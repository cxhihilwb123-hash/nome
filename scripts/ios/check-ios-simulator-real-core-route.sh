#!/usr/bin/env bash

set -u

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
developer_dir="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
project_path="${PROJECT_PATH:-$root_dir/apps/ios/SimpleX.xcodeproj}"
scheme="${SCHEME:-SimpleX (iOS)}"
sim_lib_dir="${SIM_LIB_DIR:-$root_dir/apps/ios/Libraries/sim}"
downloads_dir="${DOWNLOADS_DIR:-$HOME/Downloads}"
required_sim_arch="${REQUIRED_SIM_ARCH:-$(uname -m)}"
min_core_bytes="${MIN_REAL_CORE_LIB_BYTES:-1000000}"
destinations_file="${DESTINATIONS_FILE:-}"
skip_simctl="${SKIP_SIMCTL:-0}"
run_x86_sdk_probe="${RUN_X86_SDK_PROBE:-1}"
status=1
current_route_usable=0
x86_route_usable=0
sim_arches=""

ok() {
  printf '[OK] %s\n' "$1"
}

warn() {
  printf '[WARN] %s\n' "$1"
}

block() {
  printf '[BLOCKED] %s\n' "$1"
}

fail() {
  printf '[FAIL] %s\n' "$1"
}

info() {
  printf '[INFO] %s\n' "$1"
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

archive_set_real_for_arch() {
  local label="$1"
  local dir="$2"
  local arch="$3"
  local core_lib
  local core_size
  local lib
  local archs
  local checked=0
  local archive_status=0

  if [ ! -d "$dir" ]; then
    block "$label directory is missing: $dir"
    return 1
  fi

  core_lib="$(first_core_lib "$dir")"
  if [ -z "$core_lib" ]; then
    block "$label is missing libHSsimplex-chat*.a in $dir"
    return 1
  fi

  core_size="$(wc -c < "$core_lib" | tr -d ' ')"
  if [ "$core_size" -lt "$min_core_bytes" ]; then
    block "$label core library is too small for real-core use: $(basename "$core_lib") is ${core_size} bytes"
    archive_status=1
  else
    ok "$label core library size looks production-like: $(basename "$core_lib") is ${core_size} bytes"
  fi

  if strings "$core_lib" 2>/dev/null | grep -Eq 'preview-agent|preview-token|simplex:/contact#preview|nome\.local/preview'; then
    block "$label core library contains preview-core markers"
    archive_status=1
  else
    ok "$label core library has no preview-core markers"
  fi

  while IFS= read -r lib; do
    checked=$((checked + 1))
    if ! archs="$(library_archs "$lib")"; then
      block "Could not inspect $label architecture: $(basename "$lib")"
      archive_status=1
    elif printf ' %s ' "$archs" | grep -Fq " $arch "; then
      ok "$label supports $arch: $(basename "$lib") [$archs]"
    else
      block "$label does not support $arch: $(basename "$lib") [$archs]"
      archive_status=1
    fi
  done < <(find "$dir" -maxdepth 1 -type f -name '*.a' 2>/dev/null | sort)

  if [ "$checked" -eq 0 ]; then
    block "$label has no static .a libraries: $dir"
    archive_status=1
  fi

  return "$archive_status"
}

load_destinations() {
  local destinations

  if [ -n "$destinations_file" ]; then
    if [ -f "$destinations_file" ]; then
      cat "$destinations_file"
      return 0
    fi

    fail "DESTINATIONS_FILE does not exist: $destinations_file"
    return 1
  fi

  if [ ! -d "$project_path" ]; then
    fail "Missing Xcode project: $project_path"
    return 1
  fi

  destinations="$(DEVELOPER_DIR="$developer_dir" /usr/bin/xcodebuild -showdestinations -project "$project_path" -scheme "$scheme" 2>/dev/null || true)"
  if [ -z "$destinations" ]; then
    fail "Could not read Xcode destinations for $scheme"
    return 1
  fi

  printf '%s\n' "$destinations"
}

probe_x86_sdk_compile() {
  local tmp_dir
  local sdk_path
  local rc

  if [ "$run_x86_sdk_probe" != "1" ]; then
    info "Skipping x86_64 SDK compile probe"
    return
  fi

  tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-x86-sim-probe.XXXXXX")"
  sdk_path="$(DEVELOPER_DIR="$developer_dir" /usr/bin/xcrun --sdk iphonesimulator --show-sdk-path 2>/dev/null || true)"
  if [ -z "$sdk_path" ]; then
    warn "Could not locate iphonesimulator SDK for x86_64 compile probe"
    rm -rf "$tmp_dir"
    return
  fi

  printf 'int main(void) { return 0; }\n' > "$tmp_dir/main.c"
  DEVELOPER_DIR="$developer_dir" /usr/bin/xcrun --sdk iphonesimulator clang \
    -arch x86_64 \
    -isysroot "$sdk_path" \
    -mios-simulator-version-min=15.0 \
    "$tmp_dir/main.c" \
    -o "$tmp_dir/probe" >/dev/null 2>&1
  rc=$?

  if [ "$rc" -eq 0 ]; then
    ok "iPhoneSimulator SDK can compile an x86_64 simulator binary"
  else
    warn "iPhoneSimulator SDK x86_64 compile probe failed"
  fi

  rm -rf "$tmp_dir"
}

cat <<'HEADER'
Nome iOS simulator real-core route
==================================
HEADER

info "Required simulator architecture: $required_sim_arch"

if [ "$skip_simctl" = "1" ]; then
  info "Skipping simctl availability check"
elif DEVELOPER_DIR="$developer_dir" /usr/bin/xcrun --find simctl >/dev/null 2>&1; then
  ok "simctl is available with DEVELOPER_DIR=$developer_dir"
else
  fail "simctl is not available with DEVELOPER_DIR=$developer_dir"
fi

destinations="$(load_destinations || true)"
if [ -n "$destinations" ]; then
  sim_arches="$(printf '%s\n' "$destinations" | sed -nE 's/.*platform:iOS Simulator, arch:([^,}]+).*/\1/p' | sort -u | tr '\n' ' ' | sed 's/[[:space:]]*$//')"
  if [ -n "$sim_arches" ]; then
    ok "available iOS Simulator destination architecture(s): $sim_arches"
  else
    block "No concrete iOS Simulator destination architectures could be parsed"
  fi
else
  block "No Xcode destination evidence is available"
fi

probe_x86_sdk_compile

if printf ' %s ' "$sim_arches" | grep -Fq " $required_sim_arch "; then
  ok "Current simulator architecture is available: $required_sim_arch"
  if archive_set_real_for_arch "Installed simulator library" "$sim_lib_dir" "$required_sim_arch"; then
    ok "Current $required_sim_arch simulator real-core route is usable"
    current_route_usable=1
  else
    block "Current $required_sim_arch simulator route is not usable until installed simulator libraries are real-core artifacts"
  fi
else
  block "Current simulator architecture is not available as an Xcode destination: $required_sim_arch"
fi

x86_candidate_dir="$downloads_dir/pkg-ios-x86_64-swift-json"
if printf ' %s ' "$sim_arches" | grep -Fq ' x86_64 '; then
  ok "x86_64 iOS Simulator destination is available"
  if archive_set_real_for_arch "Local x86_64 simulator artifact" "$x86_candidate_dir" "x86_64"; then
    ok "x86_64 simulator real-core route is usable"
    x86_route_usable=1
  else
    block "x86_64 simulator destination exists, but no usable local x86_64 real-core artifact is ready"
  fi
else
  block "x86_64 iOS Simulator destination is unavailable; x86_64 simulator core artifacts cannot be used for functional simulator QA on this machine"
  if [ -d "$x86_candidate_dir" ]; then
    if archive_set_real_for_arch "Local x86_64 simulator artifact" "$x86_candidate_dir" "x86_64"; then
      warn "Local x86_64 simulator artifact looks real, but there is no x86_64 simulator destination to run it"
    fi
  else
    warn "No local x86_64 simulator artifact directory found: $x86_candidate_dir"
  fi
fi

if [ "$current_route_usable" -eq 1 ] || [ "$x86_route_usable" -eq 1 ]; then
  status=0
else
  status=1
fi

cat <<EOF

Route summary
-------------
current_${required_sim_arch}_route_usable=$current_route_usable
x86_64_route_usable=$x86_route_usable

This script is read-only. It does not download, unzip, build, or replace
apps/ios/Libraries.
EOF

exit "$status"
