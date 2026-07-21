#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
developer_dir="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
project_path="${PROJECT_PATH:-$root_dir/apps/ios/SimpleX.xcodeproj}"
scheme="${SCHEME:-SimpleX (iOS)}"
configuration="${CONFIGURATION:-Debug}"
ios_lib_dir="${IOS_LIB_DIR:-$root_dir/apps/ios/Libraries/ios}"
downloads_dir="${DOWNLOADS_DIR:-$HOME/Downloads}"
min_core_bytes="${MIN_REAL_CORE_LIB_BYTES:-1000000}"
status=0

ok() {
  printf '[OK] %s\n' "$1"
}

warn() {
  printf '[WARN] %s\n' "$1"
}

block() {
  printf '[BLOCKED] %s\n' "$1"
  status=1
}

fail() {
  printf '[FAIL] %s\n' "$1"
  status=1
}

info() {
  printf '[INFO] %s\n' "$1"
}

have_cmd() {
  command -v "$1" >/dev/null 2>&1
}

first_setting() {
  local settings="$1"
  local key="$2"

  printf '%s\n' "$settings" | awk -F ' = ' -v key="$key" '$1 ~ "^[[:space:]]*" key "$" {print $2; exit}'
}

library_archs() {
  local lib="$1"
  local output

  output="$(lipo -info "$lib" 2>/dev/null || true)"
  if [ -z "$output" ]; then
    return 1
  fi

  case "$output" in
    *" are: "*)
      printf '%s\n' "${output##* are: }"
      ;;
    *" is architecture: "*)
      printf '%s\n' "${output##* is architecture: }"
      ;;
    *)
      return 1
      ;;
  esac
}

first_core_lib() {
  find "$1" -maxdepth 1 -type f -name 'libHSsimplex-chat*.a' 2>/dev/null | sort | head -1
}

check_core_archive() {
  local label="$1"
  local dir="$2"
  local core_lib
  local core_size
  local archs
  local checked=0

  core_lib="$(first_core_lib "$dir")"
  if [ -z "$core_lib" ]; then
    block "$label is missing libHSsimplex-chat*.a in $dir"
    return
  fi

  core_size="$(wc -c < "$core_lib" | tr -d ' ')"
  if [ "$core_size" -lt "$min_core_bytes" ]; then
    block "$label core library is too small for production use: $(basename "$core_lib") is ${core_size} bytes"
  else
    ok "$label core library size looks production-like: $(basename "$core_lib") is ${core_size} bytes"
  fi

  if strings "$core_lib" 2>/dev/null | grep -Eq 'preview-agent|preview-token|simplex:/contact#preview|nome\.local/preview'; then
    block "$label core library contains preview-core markers"
  else
    ok "$label core library has no preview-core markers"
  fi

  while IFS= read -r lib; do
    checked=$((checked + 1))
    if ! archs="$(library_archs "$lib")"; then
      block "Could not inspect $label library architecture: $(basename "$lib")"
    elif printf ' %s ' "$archs" | grep -Fq ' arm64 '; then
      ok "$label library supports arm64: $(basename "$lib") [$archs]"
    else
      block "$label library does not support arm64: $(basename "$lib") [$archs]"
    fi
  done < <(find "$dir" -maxdepth 1 -type f -name '*.a' 2>/dev/null | sort)

  if [ "$checked" -eq 0 ]; then
    block "$label has no static .a libraries: $dir"
  fi
}

check_device_artifact_input() {
  local artifact_dir="$downloads_dir/pkg-ios-aarch64-swift-json"
  local artifact_zip="$downloads_dir/pkg-ios-aarch64-swift-json.zip"

  if [ -d "$artifact_dir" ]; then
    ok "Found local arm64 device artifact directory: $artifact_dir"
    check_core_archive "Local device artifact" "$artifact_dir"
  elif [ -f "$artifact_zip" ]; then
    ok "Found local arm64 device artifact zip: $artifact_zip"
    warn "Device artifact zip exists but was not expanded for architecture/content inspection"
  elif [ -d "$ios_lib_dir" ]; then
    warn "No local arm64 device artifact found in $downloads_dir; installed device libraries will be checked instead"
  else
    block "No installed device libraries or local arm64 device artifact were found"
  fi
}

check_device_libraries() {
  if [ ! -d "$ios_lib_dir" ]; then
    block "Device real-core libraries are missing: $ios_lib_dir"
    return
  fi

  check_core_archive "Installed device library" "$ios_lib_dir"
}

cat <<'HEADER'
Nome iOS physical-device readiness
==================================
HEADER

if [ -d "$developer_dir" ] && DEVELOPER_DIR="$developer_dir" /usr/bin/xcodebuild -version >/dev/null 2>&1; then
  ok "Full Xcode is available: $developer_dir"
else
  fail "Full Xcode is not available at $developer_dir"
fi

if [ -d "$project_path" ]; then
  ok "Found Xcode project: $project_path"
else
  fail "Missing Xcode project: $project_path"
fi

destinations="$(DEVELOPER_DIR="$developer_dir" /usr/bin/xcodebuild -showdestinations -project "$project_path" -scheme "$scheme" 2>/dev/null || true)"
if printf '%s\n' "$destinations" | grep -Fq 'platform:iOS, id:dvtdevice-DVTiPhonePlaceholder-iphoneos:placeholder'; then
  ok "Xcode exposes the generic Any iOS Device destination"
else
  block "Xcode does not expose a generic Any iOS Device destination for $scheme"
fi

if have_cmd xcrun; then
  xctrace_devices="$(DEVELOPER_DIR="$developer_dir" /usr/bin/xcrun xctrace list devices 2>/dev/null || true)"
  physical_devices="$(printf '%s\n' "$xctrace_devices" | awk '
    /^== Devices ==/ {in_devices=1; next}
    /^== Simulators ==/ {in_devices=0}
    in_devices && NF && $0 !~ /Mac/ {print}
  ')"

  if [ -n "$physical_devices" ]; then
    ok "Connected physical iOS/iPadOS device(s):"
    printf '%s\n' "$physical_devices" | sed 's/^/[INFO] device /'
  else
    block "No connected physical iPhone or iPad was found"
  fi
else
  fail "xcrun is not available"
fi

build_settings="$(DEVELOPER_DIR="$developer_dir" /usr/bin/xcodebuild \
  -showBuildSettings \
  -project "$project_path" \
  -scheme "$scheme" \
  -configuration "$configuration" \
  -destination 'generic/platform=iOS' 2>/dev/null || true)"

if [ -n "$build_settings" ]; then
  ok "Read generic iOS build settings for $scheme"

  product_bundle_id="$(first_setting "$build_settings" PRODUCT_BUNDLE_IDENTIFIER)"
  development_team="$(first_setting "$build_settings" DEVELOPMENT_TEAM)"
  code_sign_style="$(first_setting "$build_settings" CODE_SIGN_STYLE)"
  code_sign_identity="$(first_setting "$build_settings" CODE_SIGN_IDENTITY)"
  provisioning_required="$(first_setting "$build_settings" PROVISIONING_PROFILE_REQUIRED)"
  marketing_version="$(first_setting "$build_settings" MARKETING_VERSION)"
  current_project_version="$(first_setting "$build_settings" CURRENT_PROJECT_VERSION)"

  info "PRODUCT_BUNDLE_IDENTIFIER=${product_bundle_id:-unknown}"
  info "DEVELOPMENT_TEAM=${development_team:-unset}"
  info "CODE_SIGN_STYLE=${code_sign_style:-unknown}"
  info "CODE_SIGN_IDENTITY=${code_sign_identity:-unknown}"
  info "PROVISIONING_PROFILE_REQUIRED=${provisioning_required:-unknown}"
  info "MARKETING_VERSION=${marketing_version:-unknown}"
  info "CURRENT_PROJECT_VERSION=${current_project_version:-unknown}"

  if [ -n "$development_team" ]; then
    ok "Development team is configured"
  else
    block "Development team is not configured for generic iOS device builds"
  fi

  if [ "$code_sign_style" = "Automatic" ]; then
    ok "Code signing style is Automatic"
  else
    warn "Code signing style is ${code_sign_style:-unknown}; device install may need manual provisioning"
  fi

  if [ "$product_bundle_id" = "chat.simplex.app" ]; then
    warn "Bundle identifier is still upstream-compatible: $product_bundle_id; review release identifiers before TestFlight"
  elif [ -n "$product_bundle_id" ]; then
    ok "Bundle identifier is set: $product_bundle_id"
  else
    block "Bundle identifier could not be read"
  fi
else
  fail "Could not read generic iOS build settings"
fi

check_device_artifact_input
check_device_libraries

cat <<'NEXT'

Next supported physical-device path:
1. Connect and trust an iPhone.
2. Audit the local arm64 package:
   scripts/ios/prepare-device-real-core.sh
3. Install the arm64 package only when ready for physical-device testing:
   scripts/ios/prepare-device-real-core.sh --prepare
   Use --force only if replacing an existing apps/ios/Libraries/ios directory.
4. Re-run scripts/ios/check-ios-device-readiness.sh.
5. Build/run the app on the connected device.
6. Execute the real one-time-link, public-address, group, and messaging QA batches.

This script is read-only. It does not build, sign, install, or replace libraries.
NEXT

exit "$status"
