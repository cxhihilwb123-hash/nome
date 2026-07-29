#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/export-real-core-route-state.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-real-core-route-state-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

sim_log="$work_dir/sim.log"
device_log="$work_dir/device.log"
device_state="$work_dir/device-state"
build_log="$work_dir/build.log"
x86_probe="$work_dir/x86-probe"
output="$work_dir/state"
checklist="$work_dir/checklist.md"
mkdir -p "$x86_probe" "$device_state"

cat > "$checklist" <<'EOF'
## Environment gate

- [ ] Real iOS core libraries are installed in `apps/ios/Libraries/ios` and `apps/ios/Libraries/sim`.
- [ ] `apps/ios/Libraries/sim` libraries are production-sized artifacts, not 9.9K preview placeholders.
- [x] Current real-core route readiness can be exported with `scripts/ios/export-real-core-route-state.sh`; it records arm64 simulator blocked, x86_64 simulator build-only, physical device blocked, and local Nix build blocked as separate routes.
- [ ] The app installs and launches on a physical iPhone, if available.
EOF

cat > "$sim_log" <<'EOF'
Nome iOS simulator real-core route
==================================
[OK] available iOS Simulator destination architecture(s): arm64
[BLOCKED] Installed simulator library core library contains preview-core markers
[BLOCKED] x86_64 iOS Simulator destination is unavailable
Route summary
-------------
current_arm64_route_usable=0
x86_64_route_usable=0
EOF

cat > "$device_log" <<'EOF'
Nome iOS physical-device readiness
==================================
[BLOCKED] No connected physical iPhone or iPad was found
[OK] Local device artifact core library has no preview-core markers
[BLOCKED] Device real-core libraries are missing: apps/ios/Libraries/ios
EOF

cat > "$device_state/device_status.tsv" <<'EOF'
area	status	evidence	next_step	manual_qa_anchor	manual_qa_line	manual_qa_section	blocker_group	batch_id
xcode_environment	PASS	log	Xcode device build environment is available.	The app installs and launches on a physical iPhone, if available.	40	Environment gate	physical_device_or_camera	batch-5
connected_device	BLOCKED	log	Connect and trust a physical iPhone or iPad.	The app installs and launches on a physical iPhone, if available.	40	Environment gate	physical_device_or_camera	batch-5
local_device_artifact	PASS	log	Local arm64 device artifact can be used as the physical-device compatibility candidate.	Local arm64 device artifact input in `~/Downloads/pkg-ios-aarch64-swift-json` is production-sized, `arm64`, and free of preview-core markers for the physical-device path.	18	Environment gate	real_core_artifacts	batch-0
physical_device_source_audit	PASS	log	Physical-device source audit sees an arm64 device candidate.	Local arm64 device artifact input in `~/Downloads/pkg-ios-aarch64-swift-json` is production-sized, `arm64`, and free of preview-core markers for the physical-device path.	18	Environment gate	real_core_artifacts	batch-0
installed_device_libraries	BLOCKED	log	Install audited device libraries only when ready for physical-device testing.	Real iOS core libraries are installed in `apps/ios/Libraries/ios` and `apps/ios/Libraries/sim`.	13	Environment gate	real_core_artifacts	batch-0
device_project_references	WARN	log	Choose a matching simulator/device pair or sync Xcode project references.	Real iOS core libraries are installed in `apps/ios/Libraries/ios` and `apps/ios/Libraries/sim`.	13	Environment gate	real_core_artifacts	batch-0
signing_environment	PASS	log	Automatic signing is configured.	The app installs and launches on a physical iPhone, if available.	40	Environment gate	physical_device_or_camera	batch-5
release_identifiers	WARN	log	Choose Nome-owned identifiers or approve compatibility exceptions.	Nome-owned bundle identifiers are selected for the app, tests, notification service extension, share extension, and internal framework, or compatibility exceptions are explicitly approved for release.	160	Release gate	release_identifiers	batch-7
EOF

cat > "$build_log" <<'EOF'
Nome iOS real-core local build environment
==========================================
[FAIL] nix is not installed; local iOS core builds cannot run on this machine
[OK] full Xcode is available
EOF

cat > "$x86_probe/summary.tsv" <<'EOF'
key	value
build_status	PASS
restore_status	PASS
install_status	BLOCKED
launch_status	BLOCKED
binary_archs	Non-fat file: Nome is architecture: x86_64
EOF

"$script" \
  --output "$output" \
  --checklist "$checklist" \
  --force \
  --skip-gates \
  --simulator-route-log "$sim_log" \
  --device-state-dir "$device_state" \
  --build-env-log "$build_log" \
  --x86-probe-dir "$x86_probe" > "$work_dir/export.log" 2>&1

[ -f "$output/summary.tsv" ] || fail "missing summary.tsv"
[ -f "$output/gate_status.tsv" ] || fail "missing gate_status.tsv"
[ -f "$output/route_status.tsv" ] || fail "missing route_status.tsv"
[ -f "$output/next_actions.tsv" ] || fail "missing next_actions.tsv"

grep -Fq $'route_ready_count\t0' "$output/summary.tsv" || fail "ready route count should be 0"
grep -Fq $'route_blocked_count\t3' "$output/summary.tsv" || fail "blocked route count should be 3"
grep -Fq $'route_build_only_count\t1' "$output/summary.tsv" || fail "build-only route count should be 1"
grep -Fq $'next_action_rows\t4' "$output/summary.tsv" || fail "next action count should be 4"
grep -Fq $'arm64_simulator\tBLOCKED' "$output/route_status.tsv" || fail "missing arm64 blocked route"
grep -Fq $'x86_64_simulator\tBUILD_ONLY' "$output/route_status.tsv" || fail "missing x86 build-only route"
grep -Fq $'physical_device\tBLOCKED' "$output/route_status.tsv" || fail "missing physical device blocker"
grep -Fq 'Physical-device source audit passed for the local arm64 artifact, but no connected trusted iPhone/iPad was found, and apps/ios/Libraries/ios is missing.' "$output/route_status.tsv" || fail "physical device route should include source-audit pass plus blockers"
grep -Fq 'Project references: WARN.' "$output/route_status.tsv" || fail "physical device route should include project reference warning state"
grep -Fq $'local_nix_build\tBLOCKED' "$output/route_status.tsv" || fail "missing nix build blocker"
grep -Fq $'1\tarm64_simulator' "$output/next_actions.tsv" || fail "missing first fallback next action"
grep -Fq 'export-ios-device-readiness-state.sh' "$output/next_actions.tsv" || fail "physical device next action should point at device state export"
grep -Fq $'priority\troute\troute_status\taction\tcommand\tevidence\tmanual_qa_anchor\tmanual_qa_line\tmanual_qa_section\tblocker_group\tbatch_id' "$output/next_actions.tsv" || fail "next actions should expose route QA mapping columns"

awk -F '\t' '
  NR > 1 && $2 == "arm64_simulator" && $3 == "BLOCKED" && $8 != "" && $9 == "Environment gate" && $10 == "real_core_artifacts" && $11 == "batch-0" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/next_actions.tsv" || fail "missing arm64 simulator QA mapping"

awk -F '\t' '
  NR > 1 && $2 == "physical_device" && $3 == "BLOCKED" && $8 != "" && $9 == "Environment gate" && $10 == "physical_device_or_camera" && $11 == "batch-5" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/next_actions.tsv" || fail "missing physical device QA mapping"

pass "exported real-core route state fixture"
echo "[PASS] real-core route state test logs: $work_dir"
