#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/run-real-core-message-diagnostic.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-real-core-message-diagnostic-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

"$script" --help > "$work_dir/help.log"
grep -Fq -- '--lab-simulator UUID' "$work_dir/help.log" || fail "help omits Lab simulator"
grep -Fq -- '--peer-simulator UUID' "$work_dir/help.log" || fail "help omits Peer simulator"
grep -Fq 'receiver XCUITest' "$work_dir/help.log" || fail "help omits receiver test"
grep -Fq 'already waiting while the sender XCUITest sends' "$work_dir/help.log" || fail "help omits concurrent receive boundary"

if "$script" > "$work_dir/missing.log" 2>&1; then
  fail "missing required simulator arguments unexpectedly passed"
fi
grep -Fq -- '--lab-simulator must be a simulator UUID' "$work_dir/missing.log" || fail "missing arguments were not explained"

valid_a='11111111-1111-1111-1111-111111111111'
valid_b='22222222-2222-2222-2222-222222222222'

if "$script" --lab-simulator "$valid_a" --peer-simulator "$valid_a" > "$work_dir/same.log" 2>&1; then
  fail "same simulator UUID unexpectedly passed"
fi
grep -Fq 'must be different' "$work_dir/same.log" || fail "same simulator failure was not explained"

if "$script" --lab-simulator "$valid_a" --peer-simulator "$valid_b" --rounds 21 > "$work_dir/rounds.log" 2>&1; then
  fail "invalid round count unexpectedly passed"
fi
grep -Fq -- '--rounds must be between 1 and 20' "$work_dir/rounds.log" || fail "round validation failure was not explained"

if "$script" --lab-simulator "$valid_a" --peer-simulator "$valid_b" --run-id 'unsafe/id' > "$work_dir/run-id.log" 2>&1; then
  fail "unsafe run id unexpectedly passed"
fi
grep -Fq 'unsafe characters' "$work_dir/run-id.log" || fail "run id validation failure was not explained"

echo "[PASS] real-core message diagnostic helper validation tests passed"
