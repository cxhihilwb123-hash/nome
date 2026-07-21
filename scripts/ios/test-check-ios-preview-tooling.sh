#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/check-ios-preview-tooling.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-ios-preview-tooling-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

ok_log="$work_dir/ok.log"
if ! XCODE_SELECT_PATH="/Library/Developer/CommandLineTools" \
  XCODEBUILD_VERSION_CMD="printf 'Xcode 26.6\nBuild version 17F113\n'" \
  DEFAULT_SIMCTL_CMD="printf 'xcrun: error: unable to find utility \"simctl\"\\n' >&2; exit 1" \
  FULL_SIMCTL_CMD="printf '== Devices ==\n-- iOS 26.5 --\n    iPhone 17 Pro (UUID) (Booted)\n'" \
  "$script" > "$ok_log" 2>&1; then
  sed -n '1,160p' "$ok_log" >&2
  fail "preview tooling should pass when full-Xcode simctl works"
fi

grep -Fq "[WARN] Default xcode-select points at Command Line Tools" "$ok_log" || fail "missing Command Line Tools warning"
grep -Fq "[WARN] Default xcrun simctl can list booted devices" "$ok_log" || fail "missing default simctl warning"
grep -Fq "[OK] Full-Xcode DEVELOPER_DIR xcrun simctl can list booted devices" "$ok_log" || fail "missing full-Xcode simctl pass"

fail_log="$work_dir/fail.log"
if XCODE_SELECT_PATH="/Applications/Xcode.app/Contents/Developer" \
  XCODEBUILD_VERSION_CMD="printf 'Xcode 26.6\nBuild version 17F113\n'" \
  DEFAULT_SIMCTL_CMD="printf '== Devices ==\n'" \
  FULL_SIMCTL_CMD="printf 'simctl unavailable\\n' >&2; exit 1" \
  "$script" > "$fail_log" 2>&1; then
  sed -n '1,160p' "$fail_log" >&2
  fail "preview tooling should fail when full-Xcode simctl is unavailable"
fi

grep -Fq "[FAIL] Full-Xcode DEVELOPER_DIR xcrun simctl can list booted devices" "$fail_log" || fail "missing full-Xcode simctl failure"

pass "checked iOS preview tooling fixtures"
echo "[PASS] iOS preview tooling test logs: $work_dir"
