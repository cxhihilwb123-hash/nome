#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/export-ios-device-readiness-state.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-device-readiness-state-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

log="$work_dir/device.log"
prepare_log="$work_dir/prepare.log"
source_log="$work_dir/source-audit.log"
xcode_sync_warn_log="$work_dir/xcode-sync-warn.log"
xcode_sync_pass_log="$work_dir/xcode-sync-pass.log"
generic_build_dir="$work_dir/generic-device-build"
checklist="$work_dir/checklist.md"
output="$work_dir/state"

cat > "$checklist" <<'EOF'
## Environment gate

- [ ] Real iOS core libraries are installed in `apps/ios/Libraries/ios` and `apps/ios/Libraries/sim`.
- [x] Local arm64 device artifact input in `~/Downloads/pkg-ios-aarch64-swift-json` is production-sized, `arm64`, and free of preview-core markers for the physical-device path.
- [ ] The app installs and launches on a physical iPhone, if available.

## Release gate

- [ ] Nome-owned bundle identifiers are selected for the app, tests, notification service extension, share extension, and internal framework, or compatibility exceptions are explicitly approved for release.
EOF

cat > "$log" <<'EOF'
Nome iOS physical-device readiness
==================================
[OK] Full Xcode is available: /Applications/Xcode.app/Contents/Developer
[OK] Found Xcode project: /repo/apps/ios/SimpleX.xcodeproj
[OK] Xcode exposes the generic Any iOS Device destination
[BLOCKED] No connected physical iPhone or iPad was found
[OK] Read generic iOS build settings for SimpleX (iOS)
[INFO] PRODUCT_BUNDLE_IDENTIFIER=chat.simplex.app
[INFO] DEVELOPMENT_TEAM=ABCDE12345
[INFO] CODE_SIGN_STYLE=Automatic
[OK] Development team is configured
[OK] Code signing style is Automatic
[WARN] Bundle identifier is still upstream-compatible: chat.simplex.app; review release identifiers before TestFlight
[OK] Found local arm64 device artifact directory: /Users/example/Downloads/pkg-ios-aarch64-swift-json
[OK] Local device artifact core library size looks production-like: libHSsimplex-chat.a is 293139304 bytes
[OK] Local device artifact core library has no preview-core markers
[OK] Local device artifact library supports arm64: libHSsimplex-chat.a [arm64]
[BLOCKED] Device real-core libraries are missing: /repo/apps/ios/Libraries/ios
EOF

cat > "$prepare_log" <<'EOF'
Nome iOS device real-core prepare
=================================
[INFO] Source: /Users/example/Downloads/pkg-ios-aarch64-swift-json
[INFO] Target: /repo/apps/ios/Libraries/ios
[INFO] Prepare: 0
[OK] Device core library size looks production-like: libHSsimplex-chat-6.5.5.0-ghc9.6.3.a is 293139304 bytes
[OK] Device core library has no preview-core markers: libHSsimplex-chat-6.5.5.0-ghc9.6.3.a
[OK] Device library supports arm64: libHSsimplex-chat-6.5.5.0-ghc9.6.3.a [arm64]
[WARN] Device GHC archive name differs from current Xcode project reference: libHSsimplex-chat-6.5.5.0-ghc9.6.3.a vs libHSsimplex-chat-6.5.6.1-ghc9.6.3.a
[WARN] Device plain archive name differs from current Xcode project reference: libHSsimplex-chat-6.5.5.0.a vs libHSsimplex-chat-6.5.6.1.a
[WARN] Before a physical-device build, use a matching simulator/device core pair or sync the Xcode project references with a deliberate release decision.

[PASS] Device real-core artifact is ready for explicit install
[INFO] Dry run only. No files were copied.
EOF

cat > "$source_log" <<'EOF'
Nome iOS real-core source audit
===============================
[INFO] Required simulator architecture: arm64
[INFO] Standard SimpleX simulator artifact architecture: x86_64
[INFO] Source target: physical-device
[INFO] Checking local artifacts in /Users/example/Downloads
[INFO] Found local extracted artifact: /Users/example/Downloads/pkg-ios-aarch64-swift-json
[WARN] Missing local pkg-ios-x86_64-swift-json directory or zip
[PASS] Local artifacts: arm64 device artifact is available for physical-device testing

This script is read-only. It does not download, unzip, or replace libraries.
EOF

cat > "$xcode_sync_warn_log" <<'EOF'
Nome iOS Xcode real-core reference check
=======================================
[WARN] Xcode project reference check was advisory in this fixture.
EOF

cat > "$xcode_sync_pass_log" <<'EOF'
Nome iOS Xcode real-core reference check
=======================================
[OK] Installed device GHC archive matches project reference: libHSsimplex-chat-6.5.6.1-ghc9.6.3.a
[INFO] Installed device GHC archive is a local compatibility alias: libHSsimplex-chat-6.5.6.1-ghc9.6.3.a -> libHSsimplex-chat-6.5.5.0-ghc9.6.3.a
[PASS] Xcode real-core references are consistent with installed build paths
EOF

mkdir -p "$generic_build_dir"
cat > "$generic_build_dir/summary.tsv" <<'EOF'
key	value
product_pass_count	3
product_fail_count	0
EOF
cat > "$generic_build_dir/products.tsv" <<'EOF'
kind	name	status	path	size
app	Nome.app	PASS	/tmp/DerivedData/Build/Products/Debug-iphoneos/Nome.app	252356K
extension	SimpleX NSE.appex	PASS	/tmp/DerivedData/Build/Products/Debug-iphoneos/SimpleX NSE.appex	2324K
extension	SimpleX SE.appex	PASS	/tmp/DerivedData/Build/Products/Debug-iphoneos/SimpleX SE.appex	4480K
EOF

"$script" \
  --output "$output" \
  --checklist "$checklist" \
  --device-readiness-log "$log" \
  --prepare-device-log "$prepare_log" \
  --source-audit-log "$source_log" \
  --xcode-sync-log "$xcode_sync_warn_log" \
  --force > "$work_dir/export.log" 2>&1

[ -f "$output/summary.tsv" ] || fail "missing summary.tsv"
[ -f "$output/gate_status.tsv" ] || fail "missing gate_status.tsv"
[ -f "$output/device_status.tsv" ] || fail "missing device_status.tsv"
[ -f "$output/next_actions.tsv" ] || fail "missing next_actions.tsv"

grep -Fq $'device_area_pass_count\t4' "$output/summary.tsv" || fail "pass area count should be 4"
grep -Fq $'device_area_warn_count\t3' "$output/summary.tsv" || fail "warn area count should be 3"
grep -Fq $'device_area_blocked_count\t2' "$output/summary.tsv" || fail "blocked area count should be 2"
grep -Fq $'next_action_rows\t5' "$output/summary.tsv" || fail "next action count should be 5"
grep -Fq $'connected_device\tBLOCKED' "$output/device_status.tsv" || fail "missing connected-device blocker"
grep -Fq $'local_device_artifact\tPASS' "$output/device_status.tsv" || fail "missing local artifact pass"
grep -Fq $'physical_device_source_audit\tPASS' "$output/device_status.tsv" || fail "missing physical-device source-audit pass"
grep -Fq $'installed_device_libraries\tBLOCKED' "$output/device_status.tsv" || fail "missing installed device libraries blocker"
grep -Fq $'device_project_references\tWARN' "$output/device_status.tsv" || fail "missing device project reference warning"
grep -Fq $'generic_device_build\tWARN' "$output/device_status.tsv" || fail "missing generic device build warning"
grep -Fq $'release_identifiers\tWARN' "$output/device_status.tsv" || fail "missing release identifier warning"
grep -Fq $'WARN\tprepare_device_real_core' "$output/gate_status.tsv" || fail "prepare dry-run warning should be recorded"
grep -Fq $'PASS\tphysical_device_source_audit' "$output/gate_status.tsv" || fail "source audit pass should be recorded"
grep -Fq $'WARN\tgeneric_device_build_evidence' "$output/gate_status.tsv" || fail "missing generic build evidence warning"
grep -Fq $'priority\tarea\tarea_status\taction\tcommand\tevidence\tmanual_qa_anchor\tmanual_qa_line\tmanual_qa_section\tblocker_group\tbatch_id' "$output/next_actions.tsv" || fail "next actions should expose QA mapping columns"

awk -F '\t' '
  NR > 1 && $2 == "connected_device" && $3 == "BLOCKED" && $8 != "" && $9 == "Environment gate" && $10 == "physical_device_or_camera" && $11 == "batch-5" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/next_actions.tsv" || fail "missing connected-device QA mapping"

awk -F '\t' '
  NR > 1 && $1 == "physical_device_source_audit" && $2 == "PASS" && $6 != "" && $7 == "Environment gate" && $8 == "real_core_artifacts" && $9 == "batch-0" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/device_status.tsv" || fail "missing physical-device source-audit QA mapping"

awk -F '\t' '
  NR > 1 && $2 == "installed_device_libraries" && $3 == "BLOCKED" && $8 != "" && $9 == "Environment gate" && $10 == "real_core_artifacts" && $11 == "batch-0" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/next_actions.tsv" || fail "missing installed device libraries QA mapping"

awk -F '\t' '
  NR > 1 && $2 == "device_project_references" && $3 == "WARN" && $8 != "" && $9 == "Environment gate" && $10 == "real_core_artifacts" && $11 == "batch-0" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/next_actions.tsv" || fail "missing device project reference QA mapping"

awk -F '\t' '
  NR > 1 && $2 == "release_identifiers" && $3 == "WARN" && $8 != "" && $9 == "Release gate" && $10 == "release_identifiers" && $11 == "batch-7" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/next_actions.tsv" || fail "missing release identifier QA mapping"

installed_log="$work_dir/device-installed.log"
installed_output="$work_dir/state-installed"

grep -Fv '[BLOCKED] Device real-core libraries are missing' "$log" > "$installed_log"
cat >> "$installed_log" <<'EOF'
[OK] Installed device library core library size looks production-like: libHSsimplex-chat-6.5.5.0-ghc9.6.3.a is 293139304 bytes
[OK] Installed device library core library has no preview-core markers
[OK] Installed device library library supports arm64: libHSsimplex-chat-6.5.5.0-ghc9.6.3.a [arm64]
EOF

"$script" \
  --output "$installed_output" \
  --checklist "$checklist" \
  --device-readiness-log "$installed_log" \
  --prepare-device-log "$prepare_log" \
  --source-audit-log "$source_log" \
  --xcode-sync-log "$xcode_sync_warn_log" \
  --force > "$work_dir/export-installed.log" 2>&1

grep -Fq $'device_area_pass_count\t5' "$installed_output/summary.tsv" || fail "installed fixture pass area count should be 5"
grep -Fq $'device_area_warn_count\t3' "$installed_output/summary.tsv" || fail "installed fixture warn area count should be 3"
grep -Fq $'device_area_blocked_count\t1' "$installed_output/summary.tsv" || fail "installed fixture blocked area count should be 1"
grep -Fq $'next_action_rows\t4' "$installed_output/summary.tsv" || fail "installed fixture next action count should be 4"
grep -Fq $'installed_device_libraries\tPASS' "$installed_output/device_status.tsv" || fail "installed device libraries should pass after install evidence"

if awk -F '\t' 'NR > 1 && $2 == "installed_device_libraries" {found = 1} END {exit found ? 0 : 1}' "$installed_output/next_actions.tsv"; then
  fail "installed device libraries should not remain a next action after pass evidence"
fi

aliased_output="$work_dir/state-aliased"
"$script" \
  --output "$aliased_output" \
  --checklist "$checklist" \
  --device-readiness-log "$installed_log" \
  --prepare-device-log "$prepare_log" \
  --source-audit-log "$source_log" \
  --xcode-sync-log "$xcode_sync_pass_log" \
  --force > "$work_dir/export-aliased.log" 2>&1

grep -Fq $'device_area_pass_count\t6' "$aliased_output/summary.tsv" || fail "aliased fixture pass area count should be 6"
grep -Fq $'device_area_warn_count\t2' "$aliased_output/summary.tsv" || fail "aliased fixture warn area count should be 2"
grep -Fq $'device_area_blocked_count\t1' "$aliased_output/summary.tsv" || fail "aliased fixture blocked area count should be 1"
grep -Fq $'next_action_rows\t3' "$aliased_output/summary.tsv" || fail "aliased fixture next action count should be 3"
grep -Fq $'device_project_references\tPASS' "$aliased_output/device_status.tsv" || fail "device project references should pass after alias sync evidence"
grep -Fq $'PASS\treal_core_xcode_sync' "$aliased_output/gate_status.tsv" || fail "xcode sync pass should be recorded"

if awk -F '\t' 'NR > 1 && $2 == "device_project_references" {found = 1} END {exit found ? 0 : 1}' "$aliased_output/next_actions.tsv"; then
  fail "device project references should not remain a next action after alias sync evidence"
fi

generic_output="$work_dir/state-generic-build"
"$script" \
  --output "$generic_output" \
  --checklist "$checklist" \
  --device-readiness-log "$installed_log" \
  --prepare-device-log "$prepare_log" \
  --source-audit-log "$source_log" \
  --xcode-sync-log "$xcode_sync_pass_log" \
  --generic-device-build-dir "$generic_build_dir" \
  --force > "$work_dir/export-generic-build.log" 2>&1

grep -Fq $'device_area_pass_count\t7' "$generic_output/summary.tsv" || fail "generic-build fixture pass area count should be 7"
grep -Fq $'device_area_warn_count\t1' "$generic_output/summary.tsv" || fail "generic-build fixture warn area count should be 1"
grep -Fq $'device_area_blocked_count\t1' "$generic_output/summary.tsv" || fail "generic-build fixture blocked area count should be 1"
grep -Fq $'next_action_rows\t2' "$generic_output/summary.tsv" || fail "generic-build fixture next action count should be 2"
grep -Fq $'generic_device_build\tPASS' "$generic_output/device_status.tsv" || fail "generic device build should pass with build evidence"
grep -Fq $'PASS\tgeneric_device_build_evidence' "$generic_output/gate_status.tsv" || fail "generic build evidence pass should be recorded"

if awk -F '\t' 'NR > 1 && $2 == "generic_device_build" {found = 1} END {exit found ? 0 : 1}' "$generic_output/next_actions.tsv"; then
  fail "generic device build should not remain a next action after pass evidence"
fi

pass "exported iOS device readiness state fixture"
echo "[PASS] device readiness state test logs: $work_dir"
