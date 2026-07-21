#!/bin/bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
developer_dir="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
project_path="$root_dir/apps/ios/SimpleX.xcodeproj"
scheme="SimpleX (iOS)"
configuration="Debug"
derived_data="${DERIVED_DATA_PATH:-/tmp/nome-ios-derived-smoke}"
bundle_id="chat.simplex.app"
app_path="$derived_data/Build/Products/Debug-iphonesimulator/Nome.app"
simulator_id="${SIMULATOR_ID:-}"
output_dir="${OUTPUT_DIR:-/tmp/nome-ios-smoke-$(date +%Y%m%d-%H%M%S)}"
skip_build=0
settle_seconds="${SETTLE_SECONDS:-2}"
min_screenshot_bytes="${MIN_SCREENSHOT_BYTES:-50000}"
simctl_retries="${SIMCTL_RETRIES:-3}"
expected_width=""
expected_height=""
case_count=0
screenshots=()

usage() {
  cat <<'USAGE'
Usage: scripts/ios/smoke-nome-ui.sh [--simulator UUID] [--output DIR] [--skip-build]

Builds, installs, launches, and screenshots the current Nome iOS UI smoke set:
normal home, onboarding previews, existing-chat-list preview, conversation
preview, conversation safety/details preview, identity center preview, contacts
tab preview, and primary flow previews for add friend, join group, public
contact, and settings. When ImageMagick is available, the script also writes
a contact sheet next to the manifest for quick visual review.

Environment:
  DEVELOPER_DIR       Defaults to /Applications/Xcode.app/Contents/Developer
  DERIVED_DATA_PATH   Defaults to /tmp/nome-ios-derived-smoke
  SIMULATOR_ID        Optional simulator UUID
  OUTPUT_DIR          Optional screenshot/log output directory
  SETTLE_SECONDS      Seconds to wait after launch before screenshot, default 2
  MIN_SCREENSHOT_BYTES  Minimum screenshot PNG size, default 50000
  SIMCTL_RETRIES      Retry count for transient simctl launch/screenshot errors, default 3
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --simulator)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --simulator requires a UUID" >&2
        exit 2
      fi
      simulator_id="$1"
      ;;
    --output)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --output requires a directory" >&2
        exit 2
      fi
      output_dir="$1"
      ;;
    --skip-build)
      skip_build=1
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

xcrun_cmd="$(command -v xcrun || true)"
xcodebuild_cmd="$developer_dir/usr/bin/xcodebuild"

if [ -z "$xcrun_cmd" ] || [ ! -x "$xcrun_cmd" ] || [ ! -x "$xcodebuild_cmd" ]; then
  echo "[FAIL] Xcode tools not found under DEVELOPER_DIR=$developer_dir" >&2
  exit 2
fi

if [ -z "$simulator_id" ]; then
  simulator_id="$(DEVELOPER_DIR="$developer_dir" "$xcrun_cmd" simctl list devices booted | sed -nE 's/.*\(([0-9A-F-]{36})\) \(Booted\).*/\1/p' | head -1)"
fi

if [ -z "$simulator_id" ]; then
  echo "[FAIL] No booted simulator found. Boot a simulator or pass --simulator UUID." >&2
  exit 2
fi

simctl_cmd() {
  DEVELOPER_DIR="$developer_dir" "$xcrun_cmd" simctl "$@"
}

simulator_state() {
  local line

  line="$(simctl_cmd list devices | awk -v id="$simulator_id" 'index($0, id) { print; exit }')"
  if [ -z "$line" ]; then
    echo "unknown"
  elif printf '%s\n' "$line" | grep -q '(Booted)'; then
    echo "booted"
  elif printf '%s\n' "$line" | grep -q '(Shutdown)'; then
    echo "shutdown"
  else
    echo "other"
  fi
}

ensure_simulator_booted() {
  local state

  state="$(simulator_state)"
  case "$state" in
    booted)
      return 0
      ;;
    shutdown)
      echo "[INFO] Booting simulator $simulator_id"
      simctl_cmd boot "$simulator_id" >/dev/null
      simctl_cmd bootstatus "$simulator_id" -b >/dev/null
      ;;
    *)
      echo "[FAIL] Simulator $simulator_id is not available for smoke: state=$state" >&2
      exit 2
      ;;
  esac
}

simctl_retry() {
  local description="$1"
  shift
  local attempt=1
  local output=""
  local rc=1

  while [ "$attempt" -le "$simctl_retries" ]; do
    if output="$(simctl_cmd "$@" 2>&1)"; then
      return 0
    fi

    rc=$?
    echo "[WARN] simctl $description failed on attempt $attempt/$simctl_retries" >&2
    if [ -n "$output" ]; then
      printf '%s\n' "$output" >&2
    fi

    ensure_simulator_booted
    sleep "$attempt"
    attempt=$((attempt + 1))
  done

  echo "[FAIL] simctl $description failed after $simctl_retries attempts" >&2
  return "$rc"
}

mkdir -p "$output_dir"
manifest="$output_dir/manifest.tsv"
build_log="$output_dir/build.log"
hashes_file="$output_dir/hashes.tsv"
contact_sheet="$output_dir/contact-sheet.png"

latest_crash() {
  ls -t "$HOME"/Library/Logs/DiagnosticReports/Nome-*.ips 2>/dev/null | head -1 || true
}

crash_before="$(latest_crash)"
echo "[INFO] Simulator: $simulator_id"
echo "[INFO] Output: $output_dir"
echo "[INFO] Latest Nome crash before: ${crash_before:-none}"
ensure_simulator_booted

if [ "$skip_build" -eq 0 ]; then
  echo "[INFO] Building $scheme"
  if ! DEVELOPER_DIR="$developer_dir" "$xcodebuild_cmd" \
    -quiet \
    -project "$project_path" \
    -scheme "$scheme" \
    -configuration "$configuration" \
    -destination "id=$simulator_id" \
    -derivedDataPath "$derived_data" \
    -skipPackagePluginValidation \
    -skipMacroValidation \
    build >"$build_log" 2>&1; then
    echo "[FAIL] Build failed. Last build lines:" >&2
    tail -n 80 "$build_log" >&2
    exit 1
  fi
else
  echo "[INFO] Skipping build"
fi

if [ ! -d "$app_path" ]; then
  echo "[FAIL] Built app not found: $app_path" >&2
  exit 1
fi

echo "[INFO] Installing $app_path"
simctl_retry "install" install "$simulator_id" "$app_path"

printf 'label\targs\twidth\theight\tbytes\tsha256\tpath\n' > "$manifest"
printf 'label\tsha256\n' > "$hashes_file"

run_case() {
  local label="$1"
  shift
  local screenshot="$output_dir/$label.png"
  local width
  local height
  local bytes
  local sha

  echo "[INFO] Launching $label $*"
  simctl_cmd terminate "$simulator_id" "$bundle_id" >/dev/null 2>&1 || true
  simctl_retry "launch $label" launch "$simulator_id" "$bundle_id" "$@"
  sleep "$settle_seconds"
  simctl_retry "screenshot $label" io "$simulator_id" screenshot "$screenshot"

  width="$(sips -g pixelWidth "$screenshot" 2>/dev/null | awk '/pixelWidth/{print $2}')"
  height="$(sips -g pixelHeight "$screenshot" 2>/dev/null | awk '/pixelHeight/{print $2}')"
  bytes="$(wc -c < "$screenshot" | tr -d '[:space:]')"
  sha="$(shasum -a 256 "$screenshot" | awk '{print $1}')"

  if [ -z "$width" ] || [ -z "$height" ] || [ "$width" -le 1 ] || [ "$height" -le 1 ]; then
    echo "[FAIL] Screenshot dimensions look invalid for $label: ${width:-?}x${height:-?}" >&2
    exit 1
  fi

  if [ "$bytes" -lt "$min_screenshot_bytes" ]; then
    echo "[FAIL] Screenshot looks too small for $label: ${bytes} bytes" >&2
    exit 1
  fi

  if [ -z "$expected_width" ]; then
    expected_width="$width"
    expected_height="$height"
  elif [ "$width" != "$expected_width" ] || [ "$height" != "$expected_height" ]; then
    echo "[FAIL] Screenshot size changed for $label: ${width}x${height}, expected ${expected_width}x${expected_height}" >&2
    exit 1
  fi

  case_count=$((case_count + 1))
  screenshots+=("$screenshot")
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$label" "$*" "$width" "$height" "$bytes" "$sha" "$screenshot" >> "$manifest"
  printf '%s\t%s\n' "$label" "$sha" >> "$hashes_file"
}

write_contact_sheet() {
  local montage_log="$output_dir/contact-sheet.log"
  local montage_status=0
  local sheet_width=""
  local sheet_height=""

  if [ "${#screenshots[@]}" -eq 0 ]; then
    echo "[WARN] No screenshots available for contact sheet"
    return 0
  fi

  rm -f "$contact_sheet" "$montage_log"

  if command -v magick >/dev/null 2>&1; then
    if magick montage "${screenshots[@]}" \
      -thumbnail 241x524 \
      -tile 4x \
      -geometry +18+28 \
      -background '#F6F7F8' \
      "$contact_sheet" 2>"$montage_log"; then
      montage_status=0
    else
      montage_status=$?
    fi
  elif command -v montage >/dev/null 2>&1; then
    if montage "${screenshots[@]}" \
      -thumbnail 241x524 \
      -tile 4x \
      -geometry +18+28 \
      -background '#F6F7F8' \
      "$contact_sheet" 2>"$montage_log"; then
      montage_status=0
    else
      montage_status=$?
    fi
  else
    echo "[WARN] ImageMagick not found; skipping contact sheet"
    return 0
  fi

  if [ ! -s "$contact_sheet" ]; then
    echo "[WARN] Failed to generate contact sheet: $contact_sheet"
    return 0
  fi

  sheet_width="$(sips -g pixelWidth "$contact_sheet" 2>/dev/null | awk '/pixelWidth/{print $2}')"
  sheet_height="$(sips -g pixelHeight "$contact_sheet" 2>/dev/null | awk '/pixelHeight/{print $2}')"

  if [ -z "$sheet_width" ] || [ -z "$sheet_height" ] || [ "$sheet_width" -le 1 ] || [ "$sheet_height" -le 1 ]; then
    echo "[WARN] Contact sheet dimensions look invalid: ${sheet_width:-?}x${sheet_height:-?}"
    return 0
  fi

  if [ "$montage_status" -ne 0 ]; then
    echo "[WARN] Contact sheet command reported warnings; see $montage_log"
  fi

  echo "[PASS] Contact sheet: $contact_sheet"
}

run_case "01-home"
run_case "02-onboarding-welcome" "-NomeOnboardingWelcomePreview"
run_case "03-onboarding-profile" "-NomeOnboardingProfilePreview"
run_case "04-onboarding-network" "-NomeOnboardingNetworkPreview"
run_case "05-onboarding-conditions" "-NomeOnboardingConditionsPreview"
run_case "06-chat-list-existing" "-NomeChatListPreview"
run_case "07-conversation-preview" "-NomeConversationPreview"
run_case "08-conversation-details" "-NomeConversationPreview" "-NomeConversationPreviewOpenDetails"
run_case "09-identity-center" "-NomeIdentityCenterPreview"
run_case "10-add-friend" "-NomeAddFriendPreview"
run_case "11-join-group" "-NomeJoinGroupPreview"
run_case "12-public-contact" "-NomePublicContactPreview"
run_case "13-settings" "-NomeSettingsPreview"
run_case "14-contacts" "-NomeContactsPreview"

if [ "$case_count" -ne 14 ]; then
  echo "[FAIL] Smoke captured $case_count cases, expected 14" >&2
  exit 1
fi

duplicate_hashes="$(tail -n +2 "$hashes_file" | awk -F '\t' '{print $2}' | sort | uniq -d || true)"
if [ -n "$duplicate_hashes" ]; then
  echo "[FAIL] Duplicate screenshot hashes detected:" >&2
  while IFS= read -r duplicate_hash; do
    [ -n "$duplicate_hash" ] || continue
    awk -F '\t' -v hash="$duplicate_hash" '$2 == hash {print "  " $1 "\t" $2}' "$hashes_file" >&2
  done <<< "$duplicate_hashes"
  exit 1
fi

write_contact_sheet

crash_after="$(latest_crash)"
echo "[INFO] Latest Nome crash after: ${crash_after:-none}"

if [ "${crash_before:-}" != "${crash_after:-}" ]; then
  echo "[FAIL] New Nome crash report detected: ${crash_after:-unknown}" >&2
  exit 1
fi

echo "[PASS] Nome iOS UI smoke completed"
echo "[PASS] Manifest: $manifest"
