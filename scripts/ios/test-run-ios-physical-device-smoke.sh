#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/run-ios-physical-device-smoke.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-physical-device-smoke-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

no_device_list="$work_dir/no-device.txt"
cat > "$no_device_list" <<'EOF'
== Devices ==
Developer's Mac mini (D66468AA-3320-5C5F-A2FF-8482FB40449A)

== Devices Offline ==
Offline QA iPhone (26.5.2) (00008120-BBBBBBBBBBBBBBBB)

== Simulators ==
iPhone 17 Pro Simulator (26.5) (95CA9F4F-F85B-4AC9-ADAE-62098924E3B4)
EOF

blocked_output="$work_dir/blocked"
blocked_rc=0
"$script" \
  --device-list-file "$no_device_list" \
  --output "$blocked_output" \
  --force > "$work_dir/blocked.log" 2>&1 || blocked_rc=$?

[ "$blocked_rc" -eq 1 ] || fail "no-device fixture should exit 1"
[ -f "$blocked_output/summary.tsv" ] || fail "blocked output should include summary"
grep -Fq $'device_status\tBLOCKED' "$blocked_output/summary.tsv" || fail "blocked summary should record device_status"
grep -Fq $'BLOCKED\tconnected_device' "$blocked_output/steps.tsv" || fail "blocked steps should record connected_device"
pass "no connected device blocks before build"

device_list="$work_dir/device.txt"
cat > "$device_list" <<'EOF'
== Devices ==
Mac Pro QA iPad (26.5.2) (00008120-AAAAAAAAAAAAAAAA)
Developer's Mac mini (D66468AA-3320-5C5F-A2FF-8482FB40449A)

== Simulators ==
iPhone 17 Pro Simulator (26.5) (95CA9F4F-F85B-4AC9-ADAE-62098924E3B4)
EOF

fixture_app="$work_dir/Nome.app"
mkdir -p "$fixture_app"
cat > "$fixture_app/Info.plist" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleIdentifier</key>
  <string>chat.simplex.app</string>
</dict>
</plist>
EOF

skip_output="$work_dir/skip"
"$script" \
  --device-list-file "$device_list" \
  --app-path "$fixture_app" \
  --skip-build \
  --skip-install \
  --skip-launch \
  --output "$skip_output" \
  --force > "$work_dir/skip.log" 2>&1

[ -f "$skip_output/summary.tsv" ] || fail "skip output should include summary"
grep -Fq $'device_id\t00008120-AAAAAAAAAAAAAAAA' "$skip_output/summary.tsv" || fail "skip summary should record modern device UDID"
grep -Fq $'device_name\tMac Pro QA iPad (26.5.2)' "$skip_output/summary.tsv" || fail "skip summary should preserve device names containing Mac"
grep -Fq $'bundle_id\tchat.simplex.app' "$skip_output/summary.tsv" || fail "skip summary should read bundle id"
grep -Fq $'PASS\tconnected_device' "$skip_output/steps.tsv" || fail "skip steps should pass connected_device"
grep -Fq $'WARN\tdevice_build' "$skip_output/steps.tsv" || fail "skip steps should warn device_build"
grep -Fq $'WARN\tdevice_install' "$skip_output/steps.tsv" || fail "skip steps should warn device_install"
grep -Fq $'WARN\tdevice_launch' "$skip_output/steps.tsv" || fail "skip steps should warn device_launch"
pass "connected modern-UDID fixture records skipped build install launch"

echo "[PASS] physical-device smoke tests passed"
