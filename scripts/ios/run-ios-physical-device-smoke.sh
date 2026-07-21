#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
developer_dir="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
project_path="${PROJECT_PATH:-$root_dir/apps/ios/SimpleX.xcodeproj}"
scheme="${SCHEME:-SimpleX (iOS)}"
configuration="${CONFIGURATION:-Debug}"
derived_data="${DERIVED_DATA_PATH:-/tmp/nome-ios-derived-physical-device}"
output_dir="/tmp/nome-ios-physical-device-smoke-$(date +%Y%m%d-%H%M%S)"
expected_app="${EXPECTED_APP:-Nome.app}"
device_id="${DEVICE_ID:-}"
device_name="${DEVICE_NAME:-}"
device_list_file=""
app_path=""
bundle_id=""
skip_build=0
skip_install=0
skip_launch=0
force=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/run-ios-physical-device-smoke.sh [options]

Builds, installs, and launches Nome on a connected trusted physical iPhone or
iPad, writing a machine-readable evidence packet. Without a connected device it
exits BLOCKED and does not build, install, or launch.

Options:
  --output DIR          Output evidence directory.
  --derived-data DIR    DerivedData directory. Default:
                        /tmp/nome-ios-derived-physical-device.
  --configuration NAME  Xcode configuration. Default: Debug.
  --project PATH        Xcode project. Default: apps/ios/SimpleX.xcodeproj.
  --scheme NAME         Xcode scheme. Default: SimpleX (iOS).
  --device-id UUID      Use a specific connected device UUID.
  --device-name NAME    Label to record with --device-id.
  --device-list-file FILE
                        Parse this xctrace-list-devices output instead of
                        running xcrun xctrace list devices. Intended for tests.
  --app-path PATH       Existing app bundle path, mainly for --skip-build.
  --bundle-id ID        Bundle id to launch. If omitted, read from app bundle.
  --skip-build          Do not invoke xcodebuild; require --app-path or an
                        existing DerivedData app product.
  --skip-install        Do not invoke devicectl install; record WARN.
  --skip-launch         Do not invoke devicectl launch; record WARN.
  --force               Replace an existing output directory.
  -h, --help            Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
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
    --configuration)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --configuration requires a name" >&2; exit 2; }
      configuration="$1"
      ;;
    --project)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --project requires a path" >&2; exit 2; }
      project_path="$1"
      ;;
    --scheme)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --scheme requires a name" >&2; exit 2; }
      scheme="$1"
      ;;
    --device-id)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --device-id requires a UUID" >&2; exit 2; }
      device_id="$1"
      ;;
    --device-name)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --device-name requires a name" >&2; exit 2; }
      device_name="$1"
      ;;
    --device-list-file)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --device-list-file requires a file" >&2; exit 2; }
      device_list_file="$1"
      ;;
    --app-path)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --app-path requires a path" >&2; exit 2; }
      app_path="$1"
      ;;
    --bundle-id)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --bundle-id requires an id" >&2; exit 2; }
      bundle_id="$1"
      ;;
    --skip-build)
      skip_build=1
      ;;
    --skip-install)
      skip_install=1
      ;;
    --skip-launch)
      skip_launch=1
      ;;
    --force)
      force=1
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
steps="$output_dir/steps.tsv"
commands="$output_dir/commands.tsv"
device_list_log="$output_dir/logs/device_list.log"
build_log="$output_dir/logs/xcodebuild.log"
install_log="$output_dir/logs/devicectl_install.log"
launch_log="$output_dir/logs/devicectl_launch.log"
build_settings_log="$output_dir/logs/build_settings.log"
product_dir="$derived_data/Build/Products/${configuration}-iphoneos"
status=0

record_step() {
  local step_status="$1"
  local name="$2"
  local evidence="$3"
  local detail="$4"

  printf '%s\t%s\t%s\t%s\n' "$step_status" "$name" "$evidence" "$detail" >> "$steps"
  case "$step_status" in
    FAIL|BLOCKED)
      status=1
      ;;
  esac
}

record_command() {
  local name="$1"
  local command="$2"

  printf '%s\t%s\n' "$name" "$command" >> "$commands"
}

physical_devices_from_log() {
  awk '
    /^== Devices ==/ {in_devices=1; next}
    /^== Simulators ==/ {in_devices=0}
    in_devices && NF && $0 !~ /Mac/ {
      print
    }
  ' "$1"
}

extract_device_id() {
  sed -nE 's/^.*\(([0-9A-Fa-f-]{36})\)[[:space:]]*$/\1/p' | head -1
}

extract_device_name() {
  local line="$1"

  printf '%s\n' "$line" | sed -E 's/[[:space:]]*\([0-9A-Fa-f-]{36}\)[[:space:]]*$//'
}

read_bundle_id_from_app() {
  local app="$1"
  local plist="$app/Info.plist"

  if [ -f "$plist" ]; then
    /usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$plist" 2>/dev/null || true
  fi
}

read_bundle_id_from_build_settings() {
  if [ ! -d "$developer_dir" ] || [ ! -d "$project_path" ]; then
    return
  fi

  DEVELOPER_DIR="$developer_dir" /usr/bin/xcodebuild \
    -showBuildSettings \
    -project "$project_path" \
    -scheme "$scheme" \
    -configuration "$configuration" \
    -destination 'generic/platform=iOS' > "$build_settings_log" 2>&1 || return

  awk -F ' = ' '$1 ~ /^[[:space:]]*PRODUCT_BUNDLE_IDENTIFIER$/ {print $2; exit}' "$build_settings_log"
}

printf 'status\tstep\tevidence\tdetail\n' > "$steps"
printf 'step\tcommand\n' > "$commands"

cat <<'HEADER'
Nome iOS physical-device smoke
==============================
HEADER

printf '[INFO] Output: %s\n' "$output_dir"
printf '[INFO] Project: %s\n' "$project_path"
printf '[INFO] Scheme: %s\n' "$scheme"
printf '[INFO] Configuration: %s\n' "$configuration"
printf '[INFO] DerivedData: %s\n' "$derived_data"

if [ -n "$device_id" ]; then
  printf 'Using supplied device id: %s\n' "$device_id" > "$device_list_log"
  [ -n "$device_name" ] || device_name="supplied-device"
  record_step "PASS" "connected_device" "$device_list_log" "$device_name ($device_id)"
else
  if [ -n "$device_list_file" ]; then
    if [ ! -f "$device_list_file" ]; then
      record_step "FAIL" "connected_device" "$device_list_log" "Device-list fixture missing: $device_list_file"
    else
      cp "$device_list_file" "$device_list_log"
    fi
  else
    record_command "device_list" "DEVELOPER_DIR=$developer_dir xcrun xctrace list devices"
    DEVELOPER_DIR="$developer_dir" /usr/bin/xcrun xctrace list devices > "$device_list_log" 2>&1 || true
  fi

  if [ -z "$device_id" ] && [ -f "$device_list_log" ]; then
    first_device_line="$(physical_devices_from_log "$device_list_log" | head -1 || true)"
    if [ -n "$first_device_line" ]; then
      device_id="$(printf '%s\n' "$first_device_line" | extract_device_id)"
      device_name="$(extract_device_name "$first_device_line")"
    fi
  fi

  if [ -n "$device_id" ]; then
    record_step "PASS" "connected_device" "$device_list_log" "$device_name ($device_id)"
  else
    record_step "BLOCKED" "connected_device" "$device_list_log" "Connect and trust a physical iPhone or iPad before running build/install/launch smoke."
  fi
fi

if [ -z "$device_id" ]; then
  printf 'key\tvalue\n' > "$summary"
  printf 'output\t%s\n' "$output_dir" >> "$summary"
  printf 'device_status\tBLOCKED\n' >> "$summary"
  printf 'steps\t%s\n' "$steps" >> "$summary"
  printf 'commands\t%s\n' "$commands" >> "$summary"
  echo "[BLOCKED] No connected trusted physical iPhone/iPad was found"
  exit 1
fi

if [ -z "$app_path" ]; then
  app_path="$product_dir/$expected_app"
fi

if [ "$skip_build" -eq 1 ]; then
  printf 'Skipped build. Expected app path: %s\n' "$app_path" > "$build_log"
  if [ -d "$app_path" ]; then
    record_step "WARN" "device_build" "$build_log" "Build skipped; existing app bundle found."
  else
    record_step "BLOCKED" "device_build" "$build_log" "Build skipped but app bundle is missing: $app_path"
  fi
else
  record_command "device_build" "DEVELOPER_DIR=$developer_dir xcodebuild -project $project_path -scheme $scheme -configuration $configuration -destination id=$device_id -derivedDataPath $derived_data build"
  {
    printf '$ DEVELOPER_DIR=%q xcodebuild -project %q -scheme %q -configuration %q -destination %q -derivedDataPath %q -skipPackagePluginValidation -skipMacroValidation build\n\n' \
      "$developer_dir" "$project_path" "$scheme" "$configuration" "id=$device_id" "$derived_data"
  } > "$build_log"

  if DEVELOPER_DIR="$developer_dir" /usr/bin/xcodebuild \
    -project "$project_path" \
    -scheme "$scheme" \
    -configuration "$configuration" \
    -destination "id=$device_id" \
    -derivedDataPath "$derived_data" \
    -skipPackagePluginValidation \
    -skipMacroValidation \
    build >> "$build_log" 2>&1; then
    if [ -d "$app_path" ]; then
      record_step "PASS" "device_build" "$build_log" "Built app bundle: $app_path"
    else
      record_step "FAIL" "device_build" "$build_log" "xcodebuild succeeded but app bundle is missing: $app_path"
    fi
  else
    record_step "FAIL" "device_build" "$build_log" "xcodebuild failed; inspect log."
  fi
fi

if [ -z "$bundle_id" ] && [ -d "$app_path" ]; then
  bundle_id="$(read_bundle_id_from_app "$app_path")"
fi

if [ -z "$bundle_id" ]; then
  bundle_id="$(read_bundle_id_from_build_settings || true)"
fi

if [ -n "$bundle_id" ]; then
  record_step "PASS" "bundle_id" "$build_settings_log" "$bundle_id"
else
  record_step "FAIL" "bundle_id" "$build_settings_log" "Could not resolve bundle id from app bundle or build settings."
fi

if [ "$skip_install" -eq 1 ]; then
  printf 'Skipped install. App path: %s\n' "$app_path" > "$install_log"
  record_step "WARN" "device_install" "$install_log" "Install skipped by option."
elif [ -d "$app_path" ]; then
  record_command "device_install" "DEVELOPER_DIR=$developer_dir xcrun devicectl device install app --device $device_id $app_path"
  printf '$ DEVELOPER_DIR=%q xcrun devicectl device install app --device %q %q\n\n' "$developer_dir" "$device_id" "$app_path" > "$install_log"
  if DEVELOPER_DIR="$developer_dir" /usr/bin/xcrun devicectl device install app --device "$device_id" "$app_path" >> "$install_log" 2>&1; then
    record_step "PASS" "device_install" "$install_log" "Installed app bundle on device."
  else
    record_step "FAIL" "device_install" "$install_log" "devicectl install failed; inspect log."
  fi
else
  record_step "BLOCKED" "device_install" "$install_log" "Cannot install missing app bundle: $app_path"
fi

if [ "$skip_launch" -eq 1 ]; then
  printf 'Skipped launch. Bundle id: %s\n' "$bundle_id" > "$launch_log"
  record_step "WARN" "device_launch" "$launch_log" "Launch skipped by option."
elif [ -n "$bundle_id" ]; then
  record_command "device_launch" "DEVELOPER_DIR=$developer_dir xcrun devicectl device process launch --device $device_id $bundle_id"
  printf '$ DEVELOPER_DIR=%q xcrun devicectl device process launch --device %q %q\n\n' "$developer_dir" "$device_id" "$bundle_id" > "$launch_log"
  if DEVELOPER_DIR="$developer_dir" /usr/bin/xcrun devicectl device process launch --device "$device_id" "$bundle_id" >> "$launch_log" 2>&1; then
    record_step "PASS" "device_launch" "$launch_log" "Launched $bundle_id on device."
  else
    record_step "FAIL" "device_launch" "$launch_log" "devicectl launch failed; inspect log."
  fi
else
  record_step "BLOCKED" "device_launch" "$launch_log" "Cannot launch without a bundle id."
fi

pass_count="$(awk -F '\t' 'NR > 1 && $1 == "PASS" {count++} END {print count + 0}' "$steps")"
warn_count="$(awk -F '\t' 'NR > 1 && $1 == "WARN" {count++} END {print count + 0}' "$steps")"
blocked_count="$(awk -F '\t' 'NR > 1 && $1 == "BLOCKED" {count++} END {print count + 0}' "$steps")"
fail_count="$(awk -F '\t' 'NR > 1 && $1 == "FAIL" {count++} END {print count + 0}' "$steps")"

printf 'key\tvalue\n' > "$summary"
printf 'output\t%s\n' "$output_dir" >> "$summary"
printf 'project\t%s\n' "$project_path" >> "$summary"
printf 'scheme\t%s\n' "$scheme" >> "$summary"
printf 'configuration\t%s\n' "$configuration" >> "$summary"
printf 'derived_data\t%s\n' "$derived_data" >> "$summary"
printf 'device_id\t%s\n' "$device_id" >> "$summary"
printf 'device_name\t%s\n' "$device_name" >> "$summary"
printf 'app_path\t%s\n' "$app_path" >> "$summary"
printf 'bundle_id\t%s\n' "$bundle_id" >> "$summary"
printf 'pass_count\t%s\n' "$pass_count" >> "$summary"
printf 'warn_count\t%s\n' "$warn_count" >> "$summary"
printf 'blocked_count\t%s\n' "$blocked_count" >> "$summary"
printf 'fail_count\t%s\n' "$fail_count" >> "$summary"
printf 'steps\t%s\n' "$steps" >> "$summary"
printf 'commands\t%s\n' "$commands" >> "$summary"

cat > "$output_dir/README.md" <<EOF
# Nome iOS Physical-Device Smoke

This packet records build/install/launch evidence for the Nome iOS app on a
trusted physical iPhone or iPad.

Summary:

- device: $device_name ($device_id)
- app path: $app_path
- bundle id: $bundle_id
- PASS: $pass_count
- WARN: $warn_count
- BLOCKED: $blocked_count
- FAIL: $fail_count

Files:

- summary.tsv
- steps.tsv
- commands.tsv
- logs/
EOF

printf '[INFO] PASS: %s WARN: %s BLOCKED: %s FAIL: %s\n' "$pass_count" "$warn_count" "$blocked_count" "$fail_count"
sed -n '1,80p' "$steps"

if [ "$fail_count" -gt 0 ] || [ "$blocked_count" -gt 0 ]; then
  echo "[FAIL] Physical-device smoke did not complete" >&2
  exit 1
fi

echo "[PASS] Physical-device smoke completed"
