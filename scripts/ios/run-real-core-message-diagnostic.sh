#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
developer_dir="${FULL_XCODE_DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
project_path="${PROJECT_PATH:-$root_dir/apps/ios/SimpleX.xcodeproj}"
scheme="${SCHEME:-SimpleX (iOS)}"
configuration="${CONFIGURATION:-Debug}"
derived_data="${DERIVED_DATA_PATH:-/tmp/nome-ios-real-core-message-diagnostic-deriveddata}"
bundle_id="${BUNDLE_ID:-chat.simplex.app}"
lab_simulator=""
peer_simulator=""
rounds=3
run_id="$(date -u +%Y%m%dT%H%M%SZ)"
output_dir=""
receiver_ready_timeout=45
receiver_pid=""
lab_initial_state=""
peer_initial_state=""
status=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/run-real-core-message-diagnostic.sh [options]

Runs real-core, bidirectional messaging diagnostics across two existing iOS
simulators. Both simulators must already contain the paired nometest/nomepeer
profiles and contact. The helper never creates contacts or stores invitation
links. It restores simulators that were initially shut down.

Required:
  --lab-simulator UUID   Simulator containing the nometest profile.
  --peer-simulator UUID  Simulator containing the nomepeer profile.

Options:
  --rounds COUNT         Messages per direction, 1-20. Default: 3.
  --run-id ID            Safe unique id used in diagnostic messages.
                         Allowed: letters, digits, hyphen and underscore.
  --output DIR           New evidence directory. Default:
                         /tmp/nome-ios-real-core-message-diagnostic-<run-id>.
  --derived-data DIR     DerivedData directory. Default:
                         /tmp/nome-ios-real-core-message-diagnostic-deriveddata.
  --receiver-ready SECS  Maximum wait for the receiver test to become ready.
                         Default: 45.
  -h, --help             Show this help.

The script builds once, then runs each direction with the receiver XCUITest
already waiting while the sender XCUITest sends. Raw logs, xcresult bundles,
screenshots and a TSV summary stay in the output directory and are not added
to Git automatically.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --lab-simulator)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --lab-simulator requires a UUID" >&2; exit 2; }
      lab_simulator="$1"
      ;;
    --peer-simulator)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --peer-simulator requires a UUID" >&2; exit 2; }
      peer_simulator="$1"
      ;;
    --rounds)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --rounds requires a count" >&2; exit 2; }
      rounds="$1"
      ;;
    --run-id)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --run-id requires a value" >&2; exit 2; }
      run_id="$1"
      ;;
    --output)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --output requires a directory" >&2; exit 2; }
      output_dir="$1"
      ;;
    --derived-data)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --derived-data requires a directory" >&2; exit 2; }
      derived_data="$1"
      ;;
    --receiver-ready)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --receiver-ready requires seconds" >&2; exit 2; }
      receiver_ready_timeout="$1"
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[FAIL] Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

fail_usage() {
  echo "[FAIL] $1" >&2
  usage >&2
  exit 2
}

[[ "$lab_simulator" =~ ^[0-9A-Fa-f-]{36}$ ]] || fail_usage "--lab-simulator must be a simulator UUID"
[[ "$peer_simulator" =~ ^[0-9A-Fa-f-]{36}$ ]] || fail_usage "--peer-simulator must be a simulator UUID"
[ "$lab_simulator" != "$peer_simulator" ] || fail_usage "Lab and Peer simulators must be different"
[[ "$rounds" =~ ^[0-9]+$ ]] && [ "$rounds" -ge 1 ] && [ "$rounds" -le 20 ] || fail_usage "--rounds must be between 1 and 20"
[[ "$receiver_ready_timeout" =~ ^[0-9]+$ ]] && [ "$receiver_ready_timeout" -ge 5 ] && [ "$receiver_ready_timeout" -le 120 ] || fail_usage "--receiver-ready must be between 5 and 120"
[[ "$run_id" =~ ^[A-Za-z0-9_-]+$ ]] || fail_usage "--run-id contains unsafe characters"
[ "${#run_id}" -le 32 ] || fail_usage "--run-id must be 32 characters or fewer"

[ -n "$output_dir" ] || output_dir="/tmp/nome-ios-real-core-message-diagnostic-$run_id"
[ ! -e "$output_dir" ] || { echo "[FAIL] Output already exists: $output_dir" >&2; exit 1; }
[ -d "$developer_dir" ] || { echo "[FAIL] Full Xcode developer directory is missing: $developer_dir" >&2; exit 1; }
[ -d "$project_path" ] || { echo "[FAIL] Xcode project is missing: $project_path" >&2; exit 1; }

xcodebuild_bin="$developer_dir/usr/bin/xcodebuild"
[ -x "$xcodebuild_bin" ] || xcodebuild_bin="/usr/bin/xcodebuild"
simctl=(/usr/bin/env "DEVELOPER_DIR=$developer_dir" /usr/bin/xcrun simctl)

mkdir -p "$output_dir/logs" "$output_dir/results" "$output_dir/screenshots"
steps_file="$output_dir/steps.tsv"
summary_file="$output_dir/summary.tsv"
timings_file="$output_dir/timings.log"
printf 'status\tstep\tevidence\tdetail\n' > "$steps_file"
: > "$timings_file"

record_step() {
  local step_status="$1"
  local step="$2"
  local evidence="$3"
  local detail="$4"

  printf '%s\t%s\t%s\t%s\n' "$step_status" "$step" "$evidence" "$detail" >> "$steps_file"
  case "$step_status" in
    FAIL|BLOCKED) status=1 ;;
  esac
}

device_line() {
  local simulator="$1"
  "${simctl[@]}" list devices | grep -F "($simulator)" | head -1
}

device_state() {
  local line="$1"

  case "$line" in
    *"(Booted)"*) printf 'Booted\n' ;;
    *"(Shutdown)"*) printf 'Shutdown\n' ;;
    *) printf 'Unavailable\n' ;;
  esac
}

cleanup() {
  local rc=$?
  set +e

  if [ -n "$receiver_pid" ] && kill -0 "$receiver_pid" >/dev/null 2>&1; then
    kill "$receiver_pid" >/dev/null 2>&1
    wait "$receiver_pid" >/dev/null 2>&1
  fi
  if [ "$lab_initial_state" = "Shutdown" ]; then
    "${simctl[@]}" shutdown "$lab_simulator" >/dev/null 2>&1
  fi
  if [ "$peer_initial_state" = "Shutdown" ]; then
    "${simctl[@]}" shutdown "$peer_simulator" >/dev/null 2>&1
  fi
  return "$rc"
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

lab_line="$(device_line "$lab_simulator" || true)"
peer_line="$(device_line "$peer_simulator" || true)"
[ -n "$lab_line" ] || { record_step "FAIL" "lab_simulator" "$steps_file" "Simulator not found: $lab_simulator"; exit 1; }
[ -n "$peer_line" ] || { record_step "FAIL" "peer_simulator" "$steps_file" "Simulator not found: $peer_simulator"; exit 1; }
lab_initial_state="$(device_state "$lab_line")"
peer_initial_state="$(device_state "$peer_line")"
[ "$lab_initial_state" != "Unavailable" ] || { record_step "FAIL" "lab_simulator" "$steps_file" "$lab_line"; exit 1; }
[ "$peer_initial_state" != "Unavailable" ] || { record_step "FAIL" "peer_simulator" "$steps_file" "$peer_line"; exit 1; }
record_step "PASS" "lab_simulator" "$steps_file" "$lab_line"
record_step "PASS" "peer_simulator" "$steps_file" "$peer_line"

boot_simulator() {
  local simulator="$1"
  local initial_state="$2"
  local label="$3"

  if [ "$initial_state" = "Shutdown" ]; then
    "${simctl[@]}" boot "$simulator"
  fi
  "${simctl[@]}" bootstatus "$simulator" -b
  record_step "PASS" "${label}_boot" "$steps_file" "Booted for diagnostic; initial state was $initial_state"
}

echo "Nome iOS real-core bidirectional message diagnostic"
echo "===================================================="
echo "[INFO] Run id: $run_id"
echo "[INFO] Rounds per direction: $rounds"
echo "[INFO] Output: $output_dir"

boot_simulator "$lab_simulator" "$lab_initial_state" "lab"
boot_simulator "$peer_simulator" "$peer_initial_state" "peer"

preflight_log="$output_dir/logs/real-core-preflight.log"
if REQUIRED_SIM_ARCH=arm64 FULL_XCODE_DEVELOPER_DIR="$developer_dir" \
  "$root_dir/scripts/ios/check-real-core.sh" --target simulator > "$preflight_log" 2>&1; then
  record_step "PASS" "real_core_preflight" "$preflight_log" "Installed arm64 simulator real-core libraries passed preflight"
else
  record_step "FAIL" "real_core_preflight" "$preflight_log" "Real-core simulator preflight failed"
  exit 1
fi

build_log="$output_dir/logs/build-for-testing.log"
if DEVELOPER_DIR="$developer_dir" "$xcodebuild_bin" -quiet \
  -project "$project_path" \
  -scheme "$scheme" \
  -configuration "$configuration" \
  -destination "platform=iOS Simulator,id=$lab_simulator" \
  -derivedDataPath "$derived_data" \
  ARCHS=arm64 \
  ONLY_ACTIVE_ARCH=YES \
  NOME_REAL_CORE_DIAGNOSTIC_RUN_ID="$run_id" \
  NOME_REAL_CORE_DIAGNOSTIC_ROUNDS="$rounds" \
  build-for-testing > "$build_log" 2>&1; then
  record_step "PASS" "build_for_testing" "$build_log" "Diagnostic app and UI tests built"
else
  record_step "FAIL" "build_for_testing" "$build_log" "xcodebuild build-for-testing failed"
  exit 1
fi

app_path="$(find "$derived_data/Build/Products" -type d -name 'Nome.app' -print | head -1)"
[ -n "$app_path" ] || { record_step "FAIL" "app_product" "$build_log" "Nome.app not found in DerivedData"; exit 1; }
record_step "PASS" "app_product" "$app_path" "Built app product found"

for simulator in "$lab_simulator" "$peer_simulator"; do
  "${simctl[@]}" install "$simulator" "$app_path"
done
record_step "PASS" "install_products" "$app_path" "Installed the same build on both simulators without erasing app data"

wait_for_receiver() {
  local log_file="$1"
  local pid="$2"
  local elapsed=0

  while [ "$elapsed" -lt "$receiver_ready_timeout" ]; do
    if grep -Fq '[NOME_DIAG] remote-receive-wait' "$log_file"; then
      return 0
    fi
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      return 1
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done
  return 1
}

test_command() {
  local simulator="$1"
  local test_name="$2"
  local result_bundle="$3"

  DEVELOPER_DIR="$developer_dir" "$xcodebuild_bin" \
    -project "$project_path" \
    -scheme "$scheme" \
    -destination "platform=iOS Simulator,id=$simulator" \
    -derivedDataPath "$derived_data" \
    ARCHS=arm64 \
    ONLY_ACTIVE_ARCH=YES \
    NOME_REAL_CORE_DIAGNOSTIC_RUN_ID="$run_id" \
    NOME_REAL_CORE_DIAGNOSTIC_ROUNDS="$rounds" \
    -parallel-testing-enabled NO \
    -collect-test-diagnostics never \
    -resultBundlePath "$result_bundle" \
    "-only-testing:Tests iOS/Tests_iOS/$test_name" \
    test-without-building
}

run_direction() {
  local label="$1"
  local sender="$2"
  local receiver="$3"
  local receive_log="$output_dir/logs/${label}-receive.log"
  local send_log="$output_dir/logs/${label}-send.log"
  local receive_result="$output_dir/results/${label}-receive.xcresult"
  local send_result="$output_dir/results/${label}-send.xcresult"
  local direction_status=0

  echo "[INFO] Starting receiver for $label"
  test_command "$receiver" "testReceiveRealCoreDiagnosticMessages" "$receive_result" > "$receive_log" 2>&1 &
  receiver_pid=$!

  if wait_for_receiver "$receive_log" "$receiver_pid"; then
    record_step "PASS" "${label}_receiver_ready" "$receive_log" "Receiver entered live wait before sender started"
  else
    record_step "FAIL" "${label}_receiver_ready" "$receive_log" "Receiver did not enter live wait within ${receiver_ready_timeout}s"
    direction_status=1
  fi

  if [ "$direction_status" -eq 0 ]; then
    echo "[INFO] Sending $rounds message(s) for $label"
    if test_command "$sender" "testSendRealCoreDiagnosticMessages" "$send_result" > "$send_log" 2>&1; then
      record_step "PASS" "${label}_send" "$send_log" "Sender UI test passed"
    else
      record_step "FAIL" "${label}_send" "$send_log" "Sender UI test failed"
      direction_status=1
    fi
  fi

  if [ "$direction_status" -ne 0 ] && kill -0 "$receiver_pid" >/dev/null 2>&1; then
    kill "$receiver_pid" >/dev/null 2>&1 || true
  fi
  if wait "$receiver_pid"; then
    if [ "$direction_status" -eq 0 ]; then
      record_step "PASS" "${label}_receive" "$receive_log" "Receiver observed all messages while live"
    fi
  else
    record_step "FAIL" "${label}_receive" "$receive_log" "Receiver UI test failed or was stopped after sender failure"
    direction_status=1
  fi
  receiver_pid=""

  grep -hF '[NOME_DIAG]' "$send_log" "$receive_log" >> "$timings_file" 2>/dev/null || true
  return "$direction_status"
}

run_direction "lab-to-peer" "$lab_simulator" "$peer_simulator" || true
run_direction "peer-to-lab" "$peer_simulator" "$lab_simulator" || true

for entry in "lab:$lab_simulator" "peer:$peer_simulator"; do
  label="${entry%%:*}"
  simulator="${entry#*:}"
  "${simctl[@]}" launch "$simulator" "$bundle_id" >/dev/null 2>&1 || true
done
sleep 3
"${simctl[@]}" io "$lab_simulator" screenshot "$output_dir/screenshots/lab-final.png" >/dev/null 2>&1 || true
"${simctl[@]}" io "$peer_simulator" screenshot "$output_dir/screenshots/peer-final.png" >/dev/null 2>&1 || true

for entry in "lab:$lab_simulator" "peer:$peer_simulator"; do
  label="${entry%%:*}"
  simulator="${entry#*:}"
  "${simctl[@]}" spawn "$simulator" log show --style compact --debug --info --last 15m \
    --predicate 'process == "Nome"' > "$output_dir/logs/${label}-nome.log" 2>&1 || true
done

final_status="PASS"
[ "$status" -eq 0 ] || final_status="FAIL"
printf 'key\tvalue\n' > "$summary_file"
printf 'status\t%s\n' "$final_status" >> "$summary_file"
printf 'run_id\t%s\n' "$run_id" >> "$summary_file"
printf 'rounds_per_direction\t%s\n' "$rounds" >> "$summary_file"
printf 'lab_simulator\t%s\n' "$lab_simulator" >> "$summary_file"
printf 'peer_simulator\t%s\n' "$peer_simulator" >> "$summary_file"
printf 'lab_initial_state\t%s\n' "$lab_initial_state" >> "$summary_file"
printf 'peer_initial_state\t%s\n' "$peer_initial_state" >> "$summary_file"
printf 'steps\t%s\n' "$steps_file" >> "$summary_file"
printf 'timings\t%s\n' "$timings_file" >> "$summary_file"

if [ "$status" -eq 0 ]; then
  echo "[PASS] Bidirectional real-core diagnostic passed"
  echo "[INFO] Evidence: $output_dir"
else
  echo "[FAIL] Bidirectional real-core diagnostic found a failure" >&2
  echo "[INFO] Evidence: $output_dir" >&2
  exit 1
fi
