#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/probe-x86_64-simulator-real-core.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-x86-real-core-probe-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

sim_dir="$work_dir/sim"
x86_dir="$work_dir/x86"
output="$work_dir/output"
mkdir -p "$sim_dir" "$x86_dir"

printf 'original-ghc\n' > "$sim_dir/libHSsimplex-chat-6.5.6.1-target-ghc9.6.3.a"
printf 'original-plain\n' > "$sim_dir/libHSsimplex-chat-6.5.6.1-target.a"
printf 'original-ffi\n' > "$sim_dir/libffi.a"
printf 'original-gmp\n' > "$sim_dir/libgmp.a"
printf 'original-gmpxx\n' > "$sim_dir/libgmpxx.a"

printf 'real-x86-ghc\n' > "$x86_dir/libHSsimplex-chat-6.5.5.0-source-ghc9.6.3.a"
printf 'real-x86-plain\n' > "$x86_dir/libHSsimplex-chat-6.5.5.0-source.a"
printf 'real-x86-ffi\n' > "$x86_dir/libffi.a"
printf 'real-x86-gmp\n' > "$x86_dir/libgmp.a"
printf 'real-x86-gmpxx\n' > "$x86_dir/libgmpxx.a"

"$script" \
  --sim-lib-dir "$sim_dir" \
  --x86-dir "$x86_dir" \
  --output "$output" \
  --skip-build \
  --force > "$work_dir/probe.log" 2>&1

[ -f "$output/summary.tsv" ] || fail "missing summary.tsv"
[ -f "$output/original_sim_sha256.tsv" ] || fail "missing original sha table"
[ -f "$output/temporary_x86_sim_sha256.tsv" ] || fail "missing temporary sha table"

grep -Fq $'build_status\tSKIPPED' "$output/summary.tsv" || fail "skip-build should report SKIPPED"
grep -Fq $'restore_status\tPASS' "$output/summary.tsv" || fail "restore should pass"
grep -Fq 'real-x86-ghc' "$output/temporary_x86_sim_sha256.tsv" && fail "temporary sha table should not contain raw content"

grep -Fq 'original-ghc' "$sim_dir/libHSsimplex-chat-6.5.6.1-target-ghc9.6.3.a" || fail "target ghc archive was not restored"
grep -Fq 'original-plain' "$sim_dir/libHSsimplex-chat-6.5.6.1-target.a" || fail "target plain archive was not restored"
grep -Fq 'original-ffi' "$sim_dir/libffi.a" || fail "libffi was not restored"
grep -Fq 'original-gmp' "$sim_dir/libgmp.a" || fail "libgmp was not restored"
grep -Fq 'original-gmpxx' "$sim_dir/libgmpxx.a" || fail "libgmpxx was not restored"

pass "x86_64 simulator real-core probe restores simulator libraries"
echo "[PASS] x86_64 simulator real-core probe test logs: $work_dir"
