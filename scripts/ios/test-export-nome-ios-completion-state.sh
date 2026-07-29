#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/export-nome-ios-completion-state.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-ios-completion-state-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

output="$work_dir/state"
generic_build_dir="$work_dir/generic-device-build"
mkdir -p "$generic_build_dir"

"$script" --output "$output" --force --skip-gates --generic-device-build-dir "$generic_build_dir" > "$work_dir/export.log" 2>&1

[ -f "$output/summary.tsv" ] || fail "missing summary.tsv"
[ -f "$output/completion_requirements.tsv" ] || fail "missing completion_requirements.tsv"
[ -f "$output/gate_status.tsv" ] || fail "missing gate_status.tsv"

grep -Fq $'pass_count\t0' "$output/summary.tsv" || fail "skip-gates fixture should not claim pass requirements"
grep -Fq $'warn_count\t8' "$output/summary.tsv" || fail "skip-gates fixture should emit eight warn requirements"
grep -Fq $'blocked_count\t0' "$output/summary.tsv" || fail "skip-gates fixture should not emit blockers"
grep -Fq $'generic_device_build_dir\t'"$generic_build_dir" "$output/summary.tsv" || fail "summary should record supplied generic device build dir"
grep -Fq $'planning_records\tWARN' "$output/completion_requirements.tsv" || fail "missing planning record row"
grep -Fq $'real_core_runtime\tWARN' "$output/completion_requirements.tsv" || fail "missing real-core row"
grep -Fq $'release_identifiers\tWARN' "$output/completion_requirements.tsv" || fail "missing release identifiers row"
grep -Fq $'requirement\tstatus\tevidence\tunblock' "$output/completion_requirements.tsv" || fail "completion requirements header changed"

pass "exported Nome iOS completion state fixture"
echo "[PASS] Nome iOS completion state test logs: $work_dir"
