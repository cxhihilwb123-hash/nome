#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/export-nome-qa-execution-state.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-qa-execution-state-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

checklist="$work_dir/checklist.md"
output="$work_dir/state"
generic_build="$work_dir/generic-device-build"
mkdir -p "$generic_build"

cat > "$checklist" <<'EOF'
# Test checklist

## Environment gate

- [ ] Real iOS core libraries are installed in `apps/ios/Libraries/ios` and `apps/ios/Libraries/sim`.
- [x] The app builds.

## Add friend

- [ ] Real core creates a valid one-time link.

## Physical device

- [ ] Physical iPhone launch and live QR scan pass with camera permission.

## Release gate

- [ ] Final ten-or-fewer App Store screenshot package passes `scripts/ios/check-app-store-screenshots.sh --final`.
- [ ] `scripts/ios/check-ios-release-identifiers.sh` passes for the final TestFlight or public distribution configuration.
EOF

"$script" \
  --checklist "$checklist" \
  --output "$output" \
  --force \
  --skip-gates \
  --source-target physical-device \
  --generic-device-build-dir "$generic_build" > "$work_dir/export.log" 2>&1

[ -f "$output/summary.tsv" ] || fail "missing summary.tsv"
[ -f "$output/gate_status.tsv" ] || fail "missing gate_status.tsv"
[ -f "$output/blocker_groups.tsv" ] || fail "missing blocker_groups.tsv"
[ -f "$output/next_actions.tsv" ] || fail "missing next_actions.tsv"
[ -f "$output/batches/manifest.tsv" ] || fail "missing batch manifest"
[ -f "$output/batches/evidence_index.tsv" ] || fail "missing batch evidence index"

grep -Fq $'unchecked_items\t5' "$output/summary.tsv" || fail "summary missing unchecked count"
grep -Fq $'evidence_index\t'"$output/batches/evidence_index.tsv" "$output/summary.tsv" || fail "summary missing evidence index path"
grep -Fq $'source_target\tphysical-device' "$output/summary.tsv" || fail "summary missing source target"
grep -Fq $'generic_device_build_dir\t'"$generic_build" "$output/summary.tsv" || fail "summary missing generic build dir"
grep -Fq $'real_core_artifacts\t1\tbatch-0' "$output/blocker_groups.tsv" || fail "missing real-core blocker group"
grep -Fq $'real_core_functional\t1\tbatch-2' "$output/blocker_groups.tsv" || fail "missing real-core functional blocker group"
grep -Fq $'physical_device_or_camera\t1\tbatch-5' "$output/blocker_groups.tsv" || fail "missing physical-device blocker group"
grep -Fq $'app_store_final_screenshots\t1\tbatch-6' "$output/blocker_groups.tsv" || fail "missing screenshot blocker group"
grep -Fq $'release_identifiers\t1\tbatch-7' "$output/blocker_groups.tsv" || fail "missing release blocker group"
grep -Fq $'1\tbatch-0' "$output/next_actions.tsv" || fail "missing first next action"
grep -Fq "Resolve the real-core route" "$output/next_actions.tsv" || fail "missing real-core next action"
grep -Fq "export-real-core-route-state.sh" "$output/next_actions.tsv" || fail "batch 0 should point at route-state export"
grep -Fq "scripts/ios/check-real-core.sh --target physical-device" "$output/next_actions.tsv" || fail "real-core next actions should use physical-device target"
grep -Fq -- "--generic-device-build-dir $generic_build" "$output/next_actions.tsv" || fail "device next action should reuse generic build evidence"
grep -Fq $'WARN\tsimulator_real_core_route' "$output/gate_status.tsv" || fail "skip-gates should warn for simulator route"
grep -Fq $'WARN\treal_core_route_state' "$output/gate_status.tsv" || fail "skip-gates should warn for route-state export"
grep -Fq $'WARN\tphysical_device_smoke' "$output/gate_status.tsv" || fail "skip-gates should warn for physical-device smoke"
grep -Fq $'WARN\tdevice_readiness_state' "$output/gate_status.tsv" || fail "skip-gates should warn for device-state export"
grep -Fq -- "--generic-device-build-dir $generic_build" "$output/logs/device_readiness_state.log" || fail "device readiness command should reuse generic build evidence"
grep -Fq -- "--target physical-device" "$output/logs/real_core_preflight.log" || fail "real-core preflight should use physical-device target"
grep -Fq -- "--target physical-device --allow-downloaded-artifacts" "$output/logs/real_core_build_env.log" || fail "real-core build env should allow downloaded physical-device artifacts"
grep -Fq "export-app-store-final-screenshot-state.sh" "$output/next_actions.tsv" || fail "batch 6 should point at final screenshot state export"
grep -Fq "export-ios-release-identity-state.sh" "$output/next_actions.tsv" || fail "batch 7 should point at release identity state export"
grep -Fq $'WARN\tapp_store_final_screenshot_state' "$output/gate_status.tsv" || fail "skip-gates should warn for final screenshot state export"
grep -Fq $'WARN\trelease_identity_state' "$output/gate_status.tsv" || fail "skip-gates should warn for release identity state export"
grep -Fq "real iOS core runtime" "$output/batches/evidence_index.tsv" || fail "evidence index should include runtime dependencies"
grep -Fq "pass the strict final App Store screenshot gate" "$output/batches/evidence_index.tsv" || fail "evidence index should include final screenshot command summary"

pass "exported QA execution state fixture"
echo "[PASS] QA execution state test logs: $work_dir"
