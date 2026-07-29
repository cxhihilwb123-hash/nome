#!/usr/bin/env bash

set -euo pipefail

developer_dir="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
xcrun_cmd="${XCRUN_BIN:-/usr/bin/xcrun}"
bundle_id="${BUNDLE_ID:-chat.simplex.app}"
simulator_id="${SIMULATOR_ID:-}"
output_dir="${OUTPUT_DIR:-}"
appearance="${APPEARANCE:-light}"
content_size="${CONTENT_SIZE:-accessibility-large}"
case_set="${CASE_SET:-all}"
settle_seconds="${SETTLE_SECONDS:-2}"
min_screenshot_bytes="${MIN_SCREENSHOT_BYTES:-50000}"
list_cases=0
screenshots=()
case_count=0
expected_width=""
expected_height=""

usage() {
  cat <<'USAGE'
Usage: scripts/ios/capture-nome-accessibility-previews.sh \
  --simulator UUID --output DIR [--appearance light|dark] \
  [--content-size CATEGORY] [--case-set all|critical|conversation]

Captures accessibility-size Nome preview screenshots from an already installed
simulator app. It never builds or installs the app. Simulator appearance,
content size and increased-contrast settings are restored on exit.

Use --list-cases with --case-set all|critical|conversation to print the selected
fixture set without contacting Simulator services.
USAGE
}

emit_cases() {
  case "$case_set" in
    all)
      printf '%s\t%s\n' \
        "01-home" "" \
        "02-onboarding-welcome" "-NomeOnboardingWelcomePreview" \
        "03-onboarding-profile" "-NomeOnboardingProfilePreview" \
        "04-onboarding-network" "-NomeOnboardingNetworkPreview" \
        "05-onboarding-conditions" "-NomeOnboardingConditionsPreview" \
        "06-chat-list-existing" "-NomeChatListPreview" \
        "07-conversation-preview" "-NomeConversationPreview" \
        "08-conversation-details" "-NomeConversationPreview -NomeConversationPreviewOpenDetails" \
        "09-identity-center" "-NomeIdentityCenterPreview" \
        "10-add-friend" "-NomeAddFriendPreview" \
        "11-join-group" "-NomeJoinGroupPreview" \
        "12-public-contact" "-NomePublicContactPreview" \
        "13-settings" "-NomeSettingsPreview" \
        "14-contacts" "-NomeContactsPreview"
      ;;
    critical)
      printf '%s\t%s\n' \
        "01-home" "" \
        "02-onboarding-welcome" "-NomeOnboardingWelcomePreview" \
        "06-chat-list-existing" "-NomeChatListPreview" \
        "07-conversation-preview" "-NomeConversationPreview" \
        "10-add-friend" "-NomeAddFriendPreview" \
        "12-public-contact" "-NomePublicContactPreview" \
        "13-settings" "-NomeSettingsPreview"
      ;;
    conversation)
      printf '%s\t%s\n' \
        "07-conversation-preview" "-NomeConversationPreview"
      ;;
    *)
      printf '[FAIL] Unsupported case set: %s\n' "$case_set" >&2
      return 2
      ;;
  esac
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --simulator)
      shift
      [ "$#" -gt 0 ] || { printf '[FAIL] --simulator requires a UUID\n' >&2; exit 2; }
      simulator_id="$1"
      ;;
    --output)
      shift
      [ "$#" -gt 0 ] || { printf '[FAIL] --output requires a directory\n' >&2; exit 2; }
      output_dir="$1"
      ;;
    --appearance)
      shift
      [ "$#" -gt 0 ] || { printf '[FAIL] --appearance requires light or dark\n' >&2; exit 2; }
      appearance="$1"
      ;;
    --content-size)
      shift
      [ "$#" -gt 0 ] || { printf '[FAIL] --content-size requires a category\n' >&2; exit 2; }
      content_size="$1"
      ;;
    --case-set)
      shift
      [ "$#" -gt 0 ] || { printf '[FAIL] --case-set requires all, critical or conversation\n' >&2; exit 2; }
      case_set="$1"
      ;;
    --list-cases)
      list_cases=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      printf '[FAIL] Unknown argument: %s\n' "$1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

if [ "$list_cases" -eq 1 ]; then
  emit_cases
  exit 0
fi

case "$appearance" in
  light|dark) ;;
  *) printf '[FAIL] Unsupported appearance: %s\n' "$appearance" >&2; exit 2 ;;
esac

case "$content_size" in
  extra-small|small|medium|large|extra-large|extra-extra-large|extra-extra-extra-large|accessibility-medium|accessibility-large|accessibility-extra-large|accessibility-extra-extra-large|accessibility-extra-extra-extra-large) ;;
  *) printf '[FAIL] Unsupported content-size category: %s\n' "$content_size" >&2; exit 2 ;;
esac

emit_cases >/dev/null

if [ -z "$simulator_id" ] || [ -z "$output_dir" ]; then
  printf '[FAIL] --simulator and --output are required\n' >&2
  exit 2
fi

if [ ! -x "$xcrun_cmd" ]; then
  printf '[FAIL] xcrun is unavailable: %s\n' "$xcrun_cmd" >&2
  exit 2
fi

if [ -d "$output_dir" ] && [ -n "$(find "$output_dir" -mindepth 1 -maxdepth 1 -print -quit)" ]; then
  printf '[FAIL] Output directory is not empty: %s\n' "$output_dir" >&2
  exit 2
fi

mkdir -p "$output_dir"
manifest="$output_dir/manifest.tsv"
settings_file="$output_dir/settings.tsv"
contact_sheet="$output_dir/contact-sheet.png"

simctl() {
  DEVELOPER_DIR="$developer_dir" "$xcrun_cmd" simctl "$@"
}

original_appearance="$(simctl ui "$simulator_id" appearance)"
original_content_size="$(simctl ui "$simulator_id" content_size)"
original_contrast="$(simctl ui "$simulator_id" increase_contrast)"

printf 'key\tvalue\n' > "$settings_file"
printf 'original_appearance\t%s\n' "$original_appearance" >> "$settings_file"
printf 'original_content_size\t%s\n' "$original_content_size" >> "$settings_file"
printf 'original_increase_contrast\t%s\n' "$original_contrast" >> "$settings_file"
printf 'capture_appearance\t%s\n' "$appearance" >> "$settings_file"
printf 'capture_content_size\t%s\n' "$content_size" >> "$settings_file"

restore_simulator_settings() {
  local command_status="$?"
  local restore_status=0
  local restored_appearance
  local restored_content_size
  local restored_contrast

  trap - EXIT INT TERM HUP
  set +e
  simctl ui "$simulator_id" appearance "$original_appearance" >/dev/null
  [ "$?" -eq 0 ] || restore_status=1
  simctl ui "$simulator_id" content_size "$original_content_size" >/dev/null
  [ "$?" -eq 0 ] || restore_status=1
  if [ "$original_contrast" = "enabled" ] || [ "$original_contrast" = "disabled" ]; then
    simctl ui "$simulator_id" increase_contrast "$original_contrast" >/dev/null
    [ "$?" -eq 0 ] || restore_status=1
  fi

  restored_appearance="$(simctl ui "$simulator_id" appearance 2>/dev/null)"
  restored_content_size="$(simctl ui "$simulator_id" content_size 2>/dev/null)"
  restored_contrast="$(simctl ui "$simulator_id" increase_contrast 2>/dev/null)"
  printf 'restored_appearance\t%s\n' "$restored_appearance" >> "$settings_file"
  printf 'restored_content_size\t%s\n' "$restored_content_size" >> "$settings_file"
  printf 'restored_increase_contrast\t%s\n' "$restored_contrast" >> "$settings_file"

  if [ "$restored_appearance" != "$original_appearance" ] || [ "$restored_content_size" != "$original_content_size" ] || [ "$restored_contrast" != "$original_contrast" ]; then
    restore_status=1
  fi

  if [ "$restore_status" -ne 0 ]; then
    printf '[FAIL] Simulator UI settings were not fully restored; see %s\n' "$settings_file" >&2
    command_status=1
  else
    printf '[PASS] Restored simulator UI settings\n'
  fi
  exit "$command_status"
}

trap restore_simulator_settings EXIT INT TERM HUP

if ! simctl appinfo "$simulator_id" "$bundle_id" >/dev/null; then
  printf '[FAIL] %s is not installed on simulator %s\n' "$bundle_id" "$simulator_id" >&2
  exit 1
fi

simctl ui "$simulator_id" appearance "$appearance" >/dev/null
simctl ui "$simulator_id" content_size "$content_size" >/dev/null

actual_appearance="$(simctl ui "$simulator_id" appearance)"
actual_content_size="$(simctl ui "$simulator_id" content_size)"
if [ "$actual_appearance" != "$appearance" ] || [ "$actual_content_size" != "$content_size" ]; then
  printf '[FAIL] Simulator UI settings did not apply: appearance=%s content_size=%s\n' "$actual_appearance" "$actual_content_size" >&2
  exit 1
fi

printf 'label\targs\tappearance\tcontent_size\twidth\theight\tbytes\tsha256\tpath\n' > "$manifest"

run_case() {
  local label="$1"
  shift
  local screenshot="$output_dir/$label.png"
  local width
  local height
  local bytes
  local sha

  simctl terminate "$simulator_id" "$bundle_id" >/dev/null 2>&1 || true
  simctl launch "$simulator_id" "$bundle_id" "$@" >/dev/null
  sleep "$settle_seconds"
  simctl io "$simulator_id" screenshot "$screenshot" >/dev/null

  width="$(sips -g pixelWidth "$screenshot" 2>/dev/null | awk '/pixelWidth/{print $2}')"
  height="$(sips -g pixelHeight "$screenshot" 2>/dev/null | awk '/pixelHeight/{print $2}')"
  bytes="$(wc -c < "$screenshot" | tr -d '[:space:]')"
  sha="$(shasum -a 256 "$screenshot" | awk '{print $1}')"

  if [ -z "$width" ] || [ -z "$height" ] || [ "$bytes" -lt "$min_screenshot_bytes" ]; then
    printf '[FAIL] Invalid screenshot for %s: %sx%s %s bytes\n' "$label" "${width:-?}" "${height:-?}" "$bytes" >&2
    exit 1
  fi

  if [ -z "$expected_width" ]; then
    expected_width="$width"
    expected_height="$height"
  elif [ "$width" != "$expected_width" ] || [ "$height" != "$expected_height" ]; then
    printf '[FAIL] Screenshot dimensions changed for %s: %sx%s expected %sx%s\n' "$label" "$width" "$height" "$expected_width" "$expected_height" >&2
    exit 1
  fi

  case_count=$((case_count + 1))
  screenshots+=("$screenshot")
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$label" "$*" "$appearance" "$content_size" "$width" "$height" "$bytes" "$sha" "$screenshot" >> "$manifest"
  printf '[PASS] %s %sx%s %s bytes\n' "$label" "$width" "$height" "$bytes"
}

while IFS=$'\t' read -r label args; do
  if [ -n "$args" ]; then
    launch_args=()
    read -r -a launch_args <<< "$args"
    run_case "$label" "${launch_args[@]}"
  else
    run_case "$label"
  fi
done < <(emit_cases)

expected_count="$(emit_cases | wc -l | tr -d '[:space:]')"
if [ "$case_count" -ne "$expected_count" ]; then
  printf '[FAIL] Captured %s cases, expected %s\n' "$case_count" "$expected_count" >&2
  exit 1
fi

duplicate_hashes="$(awk -F '\t' 'NR > 1 {print $8}' "$manifest" | sort | uniq -d)"
if [ -n "$duplicate_hashes" ]; then
  printf '[FAIL] Duplicate screenshot hashes detected:\n%s\n' "$duplicate_hashes" >&2
  exit 1
fi

contact_rows=$(((case_count + 3) / 4))
if command -v ffmpeg >/dev/null 2>&1; then
  if ! ffmpeg -hide_banner -loglevel error -y -pattern_type glob \
    -i "$output_dir/[0-9][0-9]-*.png" \
    -vf "scale=241:524,tile=4x${contact_rows}:padding=18:margin=28:color=0xF6F7F8" \
    -frames:v 1 "$contact_sheet"; then
    printf '[WARN] ffmpeg contact sheet generation failed\n'
  fi
elif command -v magick >/dev/null 2>&1; then
  if ! magick montage "${screenshots[@]}" -thumbnail 241x524 -tile 4x -geometry +18+28 -background '#F6F7F8' "$contact_sheet"; then
    printf '[WARN] ImageMagick contact sheet generation failed\n'
  fi
elif command -v montage >/dev/null 2>&1; then
  if ! montage "${screenshots[@]}" -thumbnail 241x524 -tile 4x -geometry +18+28 -background '#F6F7F8' "$contact_sheet"; then
    printf '[WARN] montage contact sheet generation failed\n'
  fi
else
  printf '[WARN] No contact sheet tool is available\n'
fi

printf '[PASS] Captured %s Nome accessibility preview states at %s/%s\n' "$case_count" "$appearance" "$content_size"
