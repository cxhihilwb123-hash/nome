#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
checklist="${CHECKLIST:-$root_dir/plans/20260709_nome_ios_manual_qa_checklist.md}"
output_dir="/tmp/nome-ios-real-core-route-state-$(date +%Y%m%d-%H%M%S)"
x86_probe_dir="/tmp/nome-ios-x86_64-sim-real-core-probe-current"
simulator_route_log=""
device_readiness_log=""
device_state_dir=""
build_env_log=""
force=0
skip_gates=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/export-real-core-route-state.sh [options]

Exports a read-only state packet for the current Nome iOS real-core routes:
arm64 simulator, x86_64 simulator probe, physical-device, and local build.

Options:
  --output DIR                 Output directory.
  --checklist FILE             Manual QA checklist used to anchor route actions.
  --force                      Replace an existing output directory.
  --skip-gates                 Do not run route gates; use supplied logs or WARN rows.
  --x86-probe-dir DIR          Existing x86_64 probe packet to summarize.
  --simulator-route-log FILE   Parse an existing simulator-route log.
  --device-readiness-log FILE  Parse an existing physical-device log.
  --device-state-dir DIR       Parse an existing export-ios-device-readiness-state packet.
  --build-env-log FILE         Parse an existing local-build-env log.
  -h, --help                   Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --output)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --output requires a directory" >&2
        exit 2
      fi
      output_dir="$1"
      ;;
    --checklist)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --checklist requires a file" >&2
        exit 2
      fi
      checklist="$1"
      ;;
    --force)
      force=1
      ;;
    --skip-gates)
      skip_gates=1
      ;;
    --x86-probe-dir)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --x86-probe-dir requires a directory" >&2
        exit 2
      fi
      x86_probe_dir="$1"
      ;;
    --simulator-route-log)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --simulator-route-log requires a file" >&2
        exit 2
      fi
      simulator_route_log="$1"
      ;;
    --device-readiness-log)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --device-readiness-log requires a file" >&2
        exit 2
      fi
      device_readiness_log="$1"
      ;;
    --device-state-dir)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --device-state-dir requires a directory" >&2
        exit 2
      fi
      device_state_dir="$1"
      ;;
    --build-env-log)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --build-env-log requires a file" >&2
        exit 2
      fi
      build_env_log="$1"
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

if [ -e "$output_dir" ]; then
  if [ "$force" -ne 1 ]; then
    echo "[FAIL] Output already exists: $output_dir" >&2
    echo "[INFO] Re-run with --force to replace it." >&2
    exit 1
  fi
  rm -rf "$output_dir"
fi

mkdir -p "$output_dir/logs"

summary="$output_dir/summary.tsv"
gate_status="$output_dir/gate_status.tsv"
route_status="$output_dir/route_status.tsv"
next_actions="$output_dir/next_actions.tsv"

record_gate() {
  local status="$1"
  local name="$2"
  local log="$3"
  printf '%s\t%s\t%s\n' "$status" "$name" "$log" >> "$gate_status"
}

run_or_copy_gate() {
  local status_on_fail="$1"
  local name="$2"
  local command="$3"
  local supplied_log="$4"
  local log="$output_dir/logs/$name.log"

  if [ -n "$supplied_log" ]; then
    if [ ! -f "$supplied_log" ]; then
      echo "[FAIL] Supplied log does not exist for $name: $supplied_log" >&2
      exit 1
    fi
    cp "$supplied_log" "$log"
    if grep -Eq '^\[FAIL\]|^\[BLOCKED\]' "$log"; then
      record_gate "$status_on_fail" "$name" "$log"
    else
      record_gate "PASS" "$name" "$log"
    fi
    return
  fi

  if [ "$skip_gates" -eq 1 ]; then
    printf 'Skipped by --skip-gates. Intended command:\n%s\n' "$command" > "$log"
    record_gate "WARN" "$name" "$log"
    return
  fi

  printf '$ %s\n\n' "$command" > "$log"
  if bash -c "$command" >> "$log" 2>&1; then
    record_gate "PASS" "$name" "$log"
  else
    record_gate "$status_on_fail" "$name" "$log"
  fi
}

value_from_tsv() {
  local file="$1"
  local key="$2"
  awk -F '\t' -v key="$key" 'NR > 1 && $1 == key {print $2; exit}' "$file" 2>/dev/null
}

device_area_status() {
  local area="$1"
  local file="$2"
  awk -F '\t' -v area="$area" 'NR > 1 && $1 == area {print $2; exit}' "$file" 2>/dev/null
}

contains() {
  local file="$1"
  local text="$2"
  grep -Fq "$text" "$file" 2>/dev/null
}

write_route() {
  local route="$1"
  local status="$2"
  local evidence="$3"
  local next_step="$4"
  printf '%s\t%s\t%s\t%s\n' "$route" "$status" "$evidence" "$next_step" >> "$route_status"
}

batch_id_for_group() {
  case "$1" in
    real_core_artifacts) printf 'batch-0' ;;
    physical_device_or_camera) printf 'batch-5' ;;
    *) printf 'unmapped' ;;
  esac
}

qa_anchor_for_route() {
  case "$1" in
    arm64_simulator|x86_64_simulator|local_nix_build)
      printf 'Real iOS core libraries are installed in `apps/ios/Libraries/ios` and `apps/ios/Libraries/sim`.'
      ;;
    physical_device)
      printf 'The app installs and launches on a physical iPhone, if available.'
      ;;
    *)
      printf 'Current real-core route readiness can be exported with `scripts/ios/export-real-core-route-state.sh`; it records arm64 simulator blocked, x86_64 simulator build-only, physical device blocked, and local Nix build blocked as separate routes.'
      ;;
  esac
}

blocker_group_for_route() {
  case "$1" in
    physical_device)
      printf 'physical_device_or_camera'
      ;;
    *)
      printf 'real_core_artifacts'
      ;;
  esac
}

qa_location_for_anchor() {
  local anchor="$1"

  if [ ! -f "$checklist" ]; then
    printf '\t'
    return
  fi

  awk -v anchor="$anchor" '
    /^## / {
      section = substr($0, 4)
      next
    }
    index($0, anchor) > 0 {
      print NR "\t" section
      found = 1
      exit
    }
    END {
      if (!found) {
        print "\t"
      }
    }
  ' "$checklist"
}

command_for_route() {
  local route="$1"
  local status="$2"

  if [ "$status" = "READY" ]; then
    printf 'scripts/ios/check-real-core.sh'
    return
  fi

  case "$route" in
    arm64_simulator)
      printf 'scripts/ios/check-ios-simulator-real-core-route.sh'
      ;;
    x86_64_simulator)
      printf 'scripts/ios/probe-x86_64-simulator-real-core.sh --output /tmp/nome-ios-x86_64-sim-real-core-probe-current --force'
      ;;
    physical_device)
      printf 'scripts/ios/export-ios-device-readiness-state.sh --output /tmp/nome-ios-device-readiness-state-current --force'
      ;;
    local_nix_build)
      printf 'scripts/ios/check-real-core-build-env.sh'
      ;;
    *)
      printf 'scripts/ios/export-real-core-route-state.sh'
      ;;
  esac
}

write_next_action() {
  local priority="$1"
  local route="$2"
  local route_state="$3"
  local action="$4"
  local command="$5"
  local evidence="$6"
  local anchor
  local location
  local manual_qa_line
  local manual_qa_section
  local blocker_group
  local batch_id

  anchor="$(qa_anchor_for_route "$route")"
  location="$(qa_location_for_anchor "$anchor")"
  manual_qa_line="$(printf '%s\n' "$location" | cut -f1)"
  manual_qa_section="$(printf '%s\n' "$location" | cut -f2)"
  blocker_group="$(blocker_group_for_route "$route")"
  batch_id="$(batch_id_for_group "$blocker_group")"

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$priority" "$route" "$route_state" "$action" "$command" "$evidence" "$anchor" "$manual_qa_line" "$manual_qa_section" "$blocker_group" "$batch_id" >> "$next_actions"
}

printf 'status\tcheck\tlog\n' > "$gate_status"

run_or_copy_gate "BLOCKED" "simulator_real_core_route" \
  "DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-ios-simulator-real-core-route.sh" \
  "$simulator_route_log"
run_or_copy_gate "BLOCKED" "real_core_build_env" \
  "DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-real-core-build-env.sh" \
  "$build_env_log"

sim_log="$output_dir/logs/simulator_real_core_route.log"
device_log="$output_dir/logs/physical_device_readiness.log"
generated_device_state_dir="$output_dir/device_readiness_state"
build_log="$output_dir/logs/real_core_build_env.log"
device_status_file=""

if [ -n "$device_state_dir" ]; then
  if [ ! -f "$device_state_dir/device_status.tsv" ]; then
    echo "[FAIL] Supplied device state packet is missing device_status.tsv: $device_state_dir" >&2
    exit 1
  fi
  device_status_file="$device_state_dir/device_status.tsv"
  {
    printf 'Using supplied device readiness state packet: %s\n\n' "$device_state_dir"
    sed -n '1,120p' "$device_status_file"
  } > "$device_log"
  if awk -F '\t' 'NR > 1 && $2 == "BLOCKED" {found=1} END {exit found ? 0 : 1}' "$device_status_file"; then
    record_gate "BLOCKED" "physical_device_readiness" "$device_log"
  else
    record_gate "PASS" "physical_device_readiness" "$device_log"
  fi
elif [ -n "$device_readiness_log" ]; then
  run_or_copy_gate "BLOCKED" "physical_device_readiness" \
    "DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-ios-device-readiness.sh" \
    "$device_readiness_log"
elif [ "$skip_gates" -eq 1 ]; then
  printf 'Skipped by --skip-gates. Intended command:\nscripts/ios/export-ios-device-readiness-state.sh --output %s --force\n' "$generated_device_state_dir" > "$device_log"
  record_gate "WARN" "physical_device_readiness" "$device_log"
else
  printf '$ scripts/ios/export-ios-device-readiness-state.sh --output %s --force\n\n' "$generated_device_state_dir" > "$device_log"
  if scripts/ios/export-ios-device-readiness-state.sh --output "$generated_device_state_dir" --force >> "$device_log" 2>&1; then
    if [ -f "$generated_device_state_dir/device_status.tsv" ]; then
      device_status_file="$generated_device_state_dir/device_status.tsv"
      if awk -F '\t' 'NR > 1 && $2 == "BLOCKED" {found=1} END {exit found ? 0 : 1}' "$device_status_file"; then
        record_gate "BLOCKED" "physical_device_readiness" "$device_log"
      else
        record_gate "PASS" "physical_device_readiness" "$device_log"
      fi
    else
      record_gate "BLOCKED" "physical_device_readiness" "$device_log"
    fi
  else
    record_gate "BLOCKED" "physical_device_readiness" "$device_log"
  fi
fi

printf 'route\tstatus\tevidence\tnext_step\n' > "$route_status"

current_arm64="$(awk -F '=' '/^current_arm64_route_usable=/{print $2; exit}' "$sim_log" 2>/dev/null)"
x86_route="$(awk -F '=' '/^x86_64_route_usable=/{print $2; exit}' "$sim_log" 2>/dev/null)"

if [ "$current_arm64" = "1" ]; then
  write_route "arm64_simulator" "READY" "Installed arm64 simulator libraries are real-core artifacts." "Run real-core functional simulator QA batches."
elif contains "$sim_log" "preview-core markers"; then
  write_route "arm64_simulator" "BLOCKED" "Installed arm64 simulator libraries still contain preview-core markers." "Obtain or build arm64 simulator real-core artifacts, then replace apps/ios/Libraries/sim."
else
  write_route "arm64_simulator" "BLOCKED" "arm64 simulator route did not pass the route gate." "Inspect $sim_log."
fi

if [ -f "$x86_probe_dir/summary.tsv" ]; then
  x86_build="$(value_from_tsv "$x86_probe_dir/summary.tsv" build_status)"
  x86_install="$(value_from_tsv "$x86_probe_dir/summary.tsv" install_status)"
  x86_launch="$(value_from_tsv "$x86_probe_dir/summary.tsv" launch_status)"
  x86_binary="$(value_from_tsv "$x86_probe_dir/summary.tsv" binary_archs)"
  x86_install_note=""

  for install_log in "$x86_probe_dir/logs/manual-install-launch.log" "$x86_probe_dir/logs/rosetta-simctl-install.log" "$x86_probe_dir/logs/install-launch.log"; do
    if contains "$install_log" "Failed to find matching arch"; then
      x86_install="BLOCKED"
      x86_launch="BLOCKED"
      x86_install_note="Current simulator runtime rejected the x86_64 app: Failed to find matching arch."
      break
    fi
  done

  if [ "$x86_build" = "PASS" ] && [ "$x86_install" = "PASS" ] && [ "$x86_launch" = "PASS" ]; then
    write_route "x86_64_simulator" "READY" "Existing x86_64 probe built, installed, and launched. ${x86_binary:-}" "Run functional QA against the x86_64 simulator app."
  elif [ "$x86_build" = "PASS" ]; then
    write_route "x86_64_simulator" "BUILD_ONLY" "Existing x86_64 probe built successfully, but install/launch status is ${x86_install:-unknown}/${x86_launch:-unknown}. ${x86_install_note:-} ${x86_binary:-}" "Use a simulator/runtime that accepts x86_64 apps, or switch to arm64 simulator/device route."
  else
    write_route "x86_64_simulator" "BLOCKED" "Existing x86_64 probe build status is ${x86_build:-unknown}." "Re-run scripts/ios/probe-x86_64-simulator-real-core.sh and inspect its logs."
  fi
elif [ "$x86_route" = "1" ]; then
  write_route "x86_64_simulator" "READY" "Route gate says x86_64 simulator route is usable." "Run x86_64 real-core functional QA."
else
  write_route "x86_64_simulator" "BLOCKED" "No completed x86_64 probe packet was found, and route gate did not report x86_64 usable." "Run scripts/ios/probe-x86_64-simulator-real-core.sh if an x86_64-compatible simulator runtime becomes available."
fi

if [ -n "$device_status_file" ]; then
  connected_status="$(device_area_status connected_device "$device_status_file")"
  installed_status="$(device_area_status installed_device_libraries "$device_status_file")"
  source_status="$(device_area_status physical_device_source_audit "$device_status_file")"
  project_status="$(device_area_status device_project_references "$device_status_file")"

  if [ "$connected_status" = "PASS" ] && [ "$installed_status" = "PASS" ] && [ "${source_status:-PASS}" = "PASS" ]; then
    write_route "physical_device" "READY" "Physical-device readiness state has connected device, installed device libraries, and physical-device source audit PASS." "Build/run on the connected iPhone and execute real-core functional QA."
  elif [ "$source_status" = "PASS" ] && [ "$connected_status" = "BLOCKED" ] && [ "$installed_status" = "BLOCKED" ]; then
    write_route "physical_device" "BLOCKED" "Physical-device source audit passed for the local arm64 artifact, but no connected trusted iPhone/iPad was found, and apps/ios/Libraries/ios is missing.${project_status:+ Project references: $project_status.}" "Install audited device libraries only when ready for device testing, then connect and trust a physical iPhone."
  elif [ "$source_status" = "PASS" ] && [ "$connected_status" = "BLOCKED" ]; then
    write_route "physical_device" "BLOCKED" "Physical-device source audit passed, but no connected trusted iPhone/iPad was found.${project_status:+ Project references: $project_status.}" "Connect and trust a physical iPhone, then re-export device readiness state."
  elif [ "$source_status" = "PASS" ] && [ "$installed_status" = "BLOCKED" ]; then
    write_route "physical_device" "BLOCKED" "Physical-device source audit passed, but apps/ios/Libraries/ios is missing.${project_status:+ Project references: $project_status.}" "Run scripts/ios/prepare-device-real-core.sh, then --prepare only when ready for device testing."
  elif grep -Eq $'\tBLOCKED\t' "$device_status_file"; then
    write_route "physical_device" "BLOCKED" "Physical-device readiness state still has blockers." "Inspect $device_status_file."
  else
    write_route "physical_device" "BLOCKED" "Physical-device readiness state is not fully ready." "Inspect $device_status_file."
  fi
else
  if contains "$device_log" "No connected physical iPhone or iPad" && contains "$device_log" "Device real-core libraries are missing"; then
    write_route "physical_device" "BLOCKED" "No connected trusted physical iPhone/iPad was found, and apps/ios/Libraries/ios is missing." "Export device readiness state, install audited device libraries only when ready for device testing, then connect and trust a physical iPhone."
  elif contains "$device_log" "No connected physical iPhone or iPad"; then
    write_route "physical_device" "BLOCKED" "No connected trusted physical iPhone/iPad was found." "Export device readiness state, then connect and trust a physical iPhone."
  elif contains "$device_log" "Device real-core libraries are missing"; then
    write_route "physical_device" "BLOCKED" "Local arm64 device artifact exists, but apps/ios/Libraries/ios is missing." "Run scripts/ios/prepare-device-real-core.sh, then --prepare only when ready for device testing."
  elif grep -Eq '^\[FAIL\]|^\[BLOCKED\]' "$device_log"; then
    write_route "physical_device" "BLOCKED" "Physical-device gate still has blockers." "Inspect $device_log."
  else
    write_route "physical_device" "READY" "Physical-device readiness gate passed." "Build/run on the connected iPhone and execute real-core functional QA."
  fi
fi

if contains "$build_log" "nix is not installed"; then
  write_route "local_nix_build" "BLOCKED" "Nix is not installed, so local iOS core builds cannot run." "Install/configure Nix or obtain a prebuilt arm64 simulator artifact."
elif grep -Eq '^\[FAIL\]' "$build_log"; then
  write_route "local_nix_build" "BLOCKED" "Local build environment gate has failures." "Inspect $build_log."
else
  write_route "local_nix_build" "READY" "Local build environment gate did not report failures." "Run scripts/ios/run-real-core-batch0.sh --build-with-nix when ready."
fi

printf 'priority\troute\troute_status\taction\tcommand\tevidence\tmanual_qa_anchor\tmanual_qa_line\tmanual_qa_section\tblocker_group\tbatch_id\n' > "$next_actions"
priority=0
while IFS=$'\t' read -r route status _evidence next_step; do
  [ "$route" != "route" ] || continue
  priority=$((priority + 1))
  command="$(command_for_route "$route" "$status")"
  write_next_action "$priority" "$route" "$status" "$next_step" "$command" "$route_status"
done < "$route_status"

if [ "$priority" -eq 0 ]; then
  priority=1
  write_next_action "$priority" "arm64_simulator" "BLOCKED" "Obtain/build an arm64 simulator real-core artifact, or use the physical-device path." "scripts/ios/check-ios-simulator-real-core-route.sh" "$route_status"
fi

ready_count="$(awk -F '\t' 'NR > 1 && $2 == "READY" {count++} END {print count + 0}' "$route_status")"
blocked_count="$(awk -F '\t' 'NR > 1 && $2 == "BLOCKED" {count++} END {print count + 0}' "$route_status")"
build_only_count="$(awk -F '\t' 'NR > 1 && $2 == "BUILD_ONLY" {count++} END {print count + 0}' "$route_status")"
gate_fail_count="$(awk -F '\t' 'NR > 1 && $1 == "FAIL" {count++} END {print count + 0}' "$gate_status")"
gate_blocked_count="$(awk -F '\t' 'NR > 1 && $1 == "BLOCKED" {count++} END {print count + 0}' "$gate_status")"
next_action_count="$(tail -n +2 "$next_actions" | awk 'NF {count++} END {print count + 0}')"

printf 'key\tvalue\n' > "$summary"
printf 'checklist\t%s\n' "$checklist" >> "$summary"
printf 'route_ready_count\t%s\n' "$ready_count" >> "$summary"
printf 'route_blocked_count\t%s\n' "$blocked_count" >> "$summary"
printf 'route_build_only_count\t%s\n' "$build_only_count" >> "$summary"
printf 'gate_fail_count\t%s\n' "$gate_fail_count" >> "$summary"
printf 'gate_blocked_count\t%s\n' "$gate_blocked_count" >> "$summary"
printf 'next_action_rows\t%s\n' "$next_action_count" >> "$summary"
printf 'x86_probe_dir\t%s\n' "$x86_probe_dir" >> "$summary"
printf 'route_status\t%s\n' "$route_status" >> "$summary"
printf 'next_actions\t%s\n' "$next_actions" >> "$summary"

cat > "$output_dir/README.md" <<EOF
# Nome iOS Real-Core Route State

This packet records which real-core route can currently support functional QA.

Summary:

- ready routes: $ready_count
- blocked routes: $blocked_count
- build-only routes: $build_only_count
- gate failures: $gate_fail_count
- gate blockers: $gate_blocked_count

Files:

- summary.tsv
- gate_status.tsv
- route_status.tsv
- next_actions.tsv
- logs/
EOF

echo "Nome iOS real-core route state"
echo "=============================="
echo "[INFO] Output: $output_dir"
echo "[INFO] Ready routes: $ready_count"
echo "[INFO] Build-only routes: $build_only_count"
echo "[INFO] Blocked routes: $blocked_count"
sed -n '1,40p' "$route_status"
echo "[PASS] Exported Nome iOS real-core route state"

if [ "$gate_fail_count" -gt 0 ]; then
  exit 1
fi

exit 0
