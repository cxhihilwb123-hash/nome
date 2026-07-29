#!/bin/bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
audit_script="$root_dir/scripts/ios/check-real-core-sources.sh"
restricted_path="/usr/bin:/bin:/usr/sbin:/sbin"

if [ -n "${OUTPUT_DIR:-}" ]; then
  output_dir="$OUTPUT_DIR"
else
  output_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-real-core-source-audit-test.XXXXXX")"
fi

mkdir -p "$output_dir"

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

run_case() {
  local name="$1"
  local expected_rc="$2"
  local expected_text="$3"
  local required_sim_arch="${4:-arm64}"
  local source_target="${5:-simulator}"
  local case_dir="$output_dir/$name-artifacts"
  local log="$output_dir/$name.log"
  local rc

  mkdir -p "$case_dir"

  set +e
  PATH="$restricted_path" DOWNLOADS_DIR="$case_dir" INCLUDE_KNOWN_HYDRA_REPOS=0 CHECK_XCODE_DESTINATIONS=0 REQUIRED_SIM_ARCH="$required_sim_arch" SOURCE_TARGET="$source_target" bash "$audit_script" > "$log" 2>&1
  rc=$?
  set -e

  if [ "$rc" -ne "$expected_rc" ]; then
    sed -n '1,160p' "$log" >&2
    fail "$name expected exit $expected_rc, got $rc"
  fi

  if ! grep -Fq "$expected_text" "$log"; then
    sed -n '1,160p' "$log" >&2
    fail "$name missing expected text: $expected_text"
  fi

  pass "$name"
}

run_case "missing" 1 "No local iOS core artifacts found"

mkdir -p "$output_dir/partial-artifacts/pkg-ios-aarch64-swift-json"
touch "$output_dir/partial-artifacts/pkg-ios-aarch64-swift-json/libSimpleXChat.dylib"
run_case "partial" 1 "Only one local iOS core architecture is available"

mkdir -p "$output_dir/device-only-artifacts/pkg-ios-aarch64-swift-json"
touch "$output_dir/device-only-artifacts/pkg-ios-aarch64-swift-json/libSimpleXChat.dylib"
run_case "device-only" 0 "Local artifacts: arm64 device artifact is available for physical-device testing" "arm64" "physical-device"

mkdir -p "$output_dir/complete-arm64-mismatch-artifacts/pkg-ios-aarch64-swift-json"
mkdir -p "$output_dir/complete-arm64-mismatch-artifacts/pkg-ios-x86_64-swift-json"
touch "$output_dir/complete-arm64-mismatch-artifacts/pkg-ios-aarch64-swift-json/libSimpleXChat.dylib"
touch "$output_dir/complete-arm64-mismatch-artifacts/pkg-ios-x86_64-swift-json/libSimpleXChat.dylib"
run_case "complete-arm64-mismatch" 1 "Local artifacts: device arm64 + x86_64 simulator artifacts found, but the current simulator requires arm64"

mkdir -p "$output_dir/complete-x86_64-compatible-artifacts/pkg-ios-aarch64-swift-json"
mkdir -p "$output_dir/complete-x86_64-compatible-artifacts/pkg-ios-x86_64-swift-json"
touch "$output_dir/complete-x86_64-compatible-artifacts/pkg-ios-aarch64-swift-json/libSimpleXChat.dylib"
touch "$output_dir/complete-x86_64-compatible-artifacts/pkg-ios-x86_64-swift-json/libSimpleXChat.dylib"
run_case "complete-x86_64-compatible" 0 "Local artifacts: standard iOS core artifact pair is compatible with simulator architecture x86_64" "x86_64"

fake_bin="$output_dir/fake-bin"
mkdir -p "$fake_bin"
cat > "$fake_bin/curl" <<'SH'
#!/bin/sh

for url do
  :
done
case "$url" in
  https://hydra.complete/*)
    exit 0
    ;;
  https://hydra.partial/*aarch64-darwin.aarch64-darwin-ios:lib:simplex-chat*)
    exit 0
    ;;
  *)
    exit 22
    ;;
esac
SH
chmod +x "$fake_bin/curl"

hydra_log="$output_dir/hydra.log"
set +e
PATH="$fake_bin:$restricted_path" DOWNLOADS_DIR="$output_dir/missing-artifacts" \
  INCLUDE_KNOWN_HYDRA_REPOS=0 \
  CHECK_XCODE_DESTINATIONS=0 \
  REQUIRED_SIM_ARCH="x86_64" \
  SOURCE_TARGET="simulator" \
  HYDRA_JOB_REPOS="$(printf '%s\n%s\n' 'https://hydra.partial/job/stable' 'https://hydra.complete/job/stable')" \
  bash "$audit_script" > "$hydra_log" 2>&1
rc=$?
set -e

if [ "$rc" -ne 0 ]; then
  sed -n '1,200p' "$hydra_log" >&2
  fail "hydra expected exit 0, got $rc"
fi

if ! grep -Fq "Hydra job repository exposes a physical-device artifact but no x86_64 simulator artifact: https://hydra.partial/job/stable" "$hydra_log"; then
  sed -n '1,200p' "$hydra_log" >&2
  fail "hydra missing partial warning"
fi

if ! grep -Fq "Hydra job repository https://hydra.complete/job/stable: standard iOS core artifact pair is compatible with simulator architecture x86_64" "$hydra_log"; then
  sed -n '1,200p' "$hydra_log" >&2
  fail "hydra missing complete pass"
fi

pass "hydra"

hydra_device_log="$output_dir/hydra-device.log"
set +e
PATH="$fake_bin:$restricted_path" DOWNLOADS_DIR="$output_dir/missing-artifacts" \
  INCLUDE_KNOWN_HYDRA_REPOS=0 \
  CHECK_XCODE_DESTINATIONS=0 \
  REQUIRED_SIM_ARCH="arm64" \
  SOURCE_TARGET="physical-device" \
  HYDRA_JOB_REPOS="https://hydra.partial/job/stable" \
  bash "$audit_script" > "$hydra_device_log" 2>&1
rc=$?
set -e

if [ "$rc" -ne 0 ]; then
  sed -n '1,200p' "$hydra_device_log" >&2
  fail "hydra-device expected exit 0, got $rc"
fi

if ! grep -Fq "Hydra job repository https://hydra.partial/job/stable: arm64 device artifact is available for physical-device testing" "$hydra_device_log"; then
  sed -n '1,200p' "$hydra_device_log" >&2
  fail "hydra-device missing physical-device pass"
fi

pass "hydra-device"

hydra_arm64_log="$output_dir/hydra-arm64.log"
set +e
PATH="$fake_bin:$restricted_path" DOWNLOADS_DIR="$output_dir/missing-artifacts" \
  INCLUDE_KNOWN_HYDRA_REPOS=0 \
  CHECK_XCODE_DESTINATIONS=0 \
  REQUIRED_SIM_ARCH="arm64" \
  SOURCE_TARGET="simulator" \
  HYDRA_JOB_REPOS="https://hydra.complete/job/stable" \
  bash "$audit_script" > "$hydra_arm64_log" 2>&1
rc=$?
set -e

if [ "$rc" -ne 1 ]; then
  sed -n '1,200p' "$hydra_arm64_log" >&2
  fail "hydra-arm64 expected exit 1, got $rc"
fi

if ! grep -Fq "Hydra job repository https://hydra.complete/job/stable: device arm64 + x86_64 simulator artifacts found, but the current simulator requires arm64" "$hydra_arm64_log"; then
  sed -n '1,200p' "$hydra_arm64_log" >&2
  fail "hydra-arm64 missing simulator mismatch warning"
fi

pass "hydra-arm64"

destinations_file="$output_dir/destinations.txt"
cat > "$destinations_file" <<'EOF'
Available destinations for the "SimpleX (iOS)" scheme:
	{ platform:iOS Simulator, arch:arm64, id:SIM-ARM64, OS:26.5, name:iPhone Test }
EOF

destinations_log="$output_dir/destinations.log"
set +e
PATH="$restricted_path" DOWNLOADS_DIR="$output_dir/missing-artifacts" \
  INCLUDE_KNOWN_HYDRA_REPOS=0 \
  CHECK_XCODE_DESTINATIONS=1 \
  DESTINATIONS_FILE="$destinations_file" \
  REQUIRED_SIM_ARCH="arm64" \
  SOURCE_TARGET="simulator" \
  bash "$audit_script" > "$destinations_log" 2>&1
rc=$?
set -e

if [ "$rc" -ne 1 ]; then
  sed -n '1,200p' "$destinations_log" >&2
  fail "destinations expected exit 1, got $rc"
fi

if ! grep -Fq "Xcode iOS Simulator destination architecture(s): arm64" "$destinations_log"; then
  sed -n '1,200p' "$destinations_log" >&2
  fail "destinations missing arm64 architecture evidence"
fi

if ! grep -Fq "Standard x86_64 simulator artifacts cannot run on this machine" "$destinations_log"; then
  sed -n '1,200p' "$destinations_log" >&2
  fail "destinations missing x86_64 unavailable warning"
fi

pass "destinations"

echo "[PASS] real-core source audit unit test logs: $output_dir"
