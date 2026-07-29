#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-check-real-core-arch-test.XXXXXX")"
restricted_path="/usr/bin:/bin:/usr/sbin:/sbin"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

make_fake_tooling() {
  local bin_dir="$1"

  mkdir -p "$bin_dir"
  cat > "$bin_dir/lipo" <<'SH'
#!/bin/sh

for arg do
  lib="$arg"
done

case "$lib" in
  *sim-x86_64*)
    echo "Non-fat file: $lib is architecture: x86_64"
    ;;
  *)
    echo "Non-fat file: $lib is architecture: arm64"
    ;;
esac
SH
  chmod +x "$bin_dir/lipo"

  cat > "$bin_dir/otool" <<'SH'
#!/bin/sh

for arg do
  lib="$arg"
done

case "$lib" in
  *sim-macos-platform*) echo " platform 1" ;;
  */ios/*) echo " platform 2" ;;
  *) echo " platform 7" ;;
esac
SH
  chmod +x "$bin_dir/otool"

  cat > "$bin_dir/mac2ios" <<'SH'
#!/bin/sh
exit 0
SH
  chmod +x "$bin_dir/mac2ios"
}

make_lib_dir() {
  local dir="$1"

  mkdir -p "$dir"
  printf 'real-core-placeholder\n' > "$dir/libHSsimplex-chat-test.a"
  printf 'dependency-placeholder\n' > "$dir/libffi.a"
}

make_download_artifacts() {
  local dir="$1"

  mkdir -p "$dir/pkg-ios-aarch64-swift-json"
  mkdir -p "$dir/pkg-ios-x86_64-swift-json"
}

run_case() {
  local name="$1"
  local sim_arch_dir="$2"
  local expected_rc="$3"
  local expected_text="$4"
  local target="${5:-all}"
  local sandbox="$work_dir/$name"
  local fake_bin="$sandbox/bin"
  local ios_dir="$sandbox/ios"
  local sim_dir="$sandbox/$sim_arch_dir"
  local downloads="$sandbox/downloads"
  local log="$sandbox.log"
  local rc

  mkdir -p "$sandbox"
  make_fake_tooling "$fake_bin"
  make_lib_dir "$ios_dir"
  make_lib_dir "$sim_dir"
  make_download_artifacts "$downloads"

  set +e
  PATH="$fake_bin:$restricted_path" \
    MAC2IOS="$fake_bin/mac2ios" \
    IOS_LIB_DIR="$ios_dir" \
    SIM_LIB_DIR="$sim_dir" \
    DOWNLOADS_DIR="$downloads" \
    MIN_REAL_CORE_LIB_BYTES=1 \
    REQUIRED_SIM_ARCH=arm64 \
    "$root_dir/scripts/ios/check-real-core.sh" --target "$target" > "$log" 2>&1
  rc=$?
  set -e

  if [ "$rc" -ne "$expected_rc" ]; then
    sed -n '1,200p' "$log" >&2
    fail "$name expected exit $expected_rc, got $rc"
  fi

  if ! grep -Fq "$expected_text" "$log"; then
    sed -n '1,200p' "$log" >&2
    fail "$name missing expected text: $expected_text"
  fi

  echo "[PASS] $name"
}

run_case "arm64-simulator-core" "sim-arm64" 0 "Simulator library supports required architecture arm64"
run_case "x86_64-simulator-core" "sim-x86_64" 1 "Simulator library architecture mismatch"
run_case "macos-platform-simulator-core" "sim-macos-platform" 1 "Simulator library platform mismatch"
run_case "physical-device-target" "sim-x86_64" 0 "Check target: physical-device" "physical-device"

echo "[PASS] check-real-core architecture tests passed"
