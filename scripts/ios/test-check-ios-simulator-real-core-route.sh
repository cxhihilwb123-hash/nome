#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/check-ios-simulator-real-core-route.sh"
developer_dir="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-sim-route-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

make_destinations() {
  local file="$1"
  shift
  local arch

  : > "$file"
  for arch in "$@"; do
    printf '\t\t{ platform:iOS Simulator, arch:%s, id:SIM-%s, OS:26.5, name:iPhone Test }\n' "$arch" "$arch" >> "$file"
  done
}

make_archive() {
  local dir="$1"
  local arch="$2"
  local marker="${3:-real-core}"
  local src="$work_dir/${arch}-${marker}.c"
  local obj="$work_dir/${arch}-${marker}.o"

  mkdir -p "$dir"
  cat > "$src" <<EOF
const char *nome_route_marker = "$marker";
int nome_route_probe(void) { return 0; }
EOF

  DEVELOPER_DIR="$developer_dir" /usr/bin/xcrun --sdk iphonesimulator clang \
    -arch "$arch" \
    -mios-simulator-version-min=15.0 \
    -c "$src" \
    -o "$obj"

  /usr/bin/ar rcs "$dir/libHSsimplex-chat-test.a" "$obj"
}

run_case() {
  local name="$1"
  local expected_rc="$2"
  local expected_text="$3"
  shift 3
  local log="$work_dir/$name.log"
  local rc=0

  /usr/bin/env \
    SKIP_SIMCTL=1 \
    RUN_X86_SDK_PROBE=0 \
    MIN_REAL_CORE_LIB_BYTES=1 \
    "$@" \
    "$script" > "$log" 2>&1 || rc=$?

  if [ "$rc" -ne "$expected_rc" ]; then
    sed -n '1,220p' "$log" >&2
    fail "$name expected exit $expected_rc, got $rc"
  fi

  if ! grep -Fq "$expected_text" "$log"; then
    sed -n '1,220p' "$log" >&2
    fail "$name missing expected text: $expected_text"
  fi

  pass "$name"
}

arm64_only_dest="$work_dir/arm64.destinations"
arm64_x86_dest="$work_dir/arm64-x86.destinations"
make_destinations "$arm64_only_dest" "arm64"
make_destinations "$arm64_x86_dest" "arm64" "x86_64"

arm64_real="$work_dir/arm64-real"
x86_real_downloads="$work_dir/x86-real-downloads"
mkdir -p "$x86_real_downloads/pkg-ios-x86_64-swift-json"
make_archive "$arm64_real" "arm64"
make_archive "$x86_real_downloads/pkg-ios-x86_64-swift-json" "x86_64"

arm64_preview="$work_dir/arm64-preview"
make_archive "$arm64_preview" "arm64" "preview-token"

run_case \
  "current_arm64_route_usable" \
  0 \
  "Current arm64 simulator real-core route is usable" \
  DESTINATIONS_FILE="$arm64_only_dest" \
  SIM_LIB_DIR="$arm64_real" \
  DOWNLOADS_DIR="$work_dir/missing-downloads" \
  REQUIRED_SIM_ARCH="arm64"

run_case \
  "x86_route_usable_when_destination_exists" \
  0 \
  "x86_64 simulator real-core route is usable" \
  DESTINATIONS_FILE="$arm64_x86_dest" \
  SIM_LIB_DIR="$arm64_preview" \
  DOWNLOADS_DIR="$x86_real_downloads" \
  REQUIRED_SIM_ARCH="arm64"

run_case \
  "x86_route_blocked_without_destination" \
  1 \
  "x86_64 iOS Simulator destination is unavailable" \
  DESTINATIONS_FILE="$arm64_only_dest" \
  SIM_LIB_DIR="$arm64_preview" \
  DOWNLOADS_DIR="$x86_real_downloads" \
  REQUIRED_SIM_ARCH="arm64"

echo "[PASS] simulator real-core route tests passed"
