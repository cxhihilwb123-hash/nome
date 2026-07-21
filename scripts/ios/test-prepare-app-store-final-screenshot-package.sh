#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/prepare-app-store-final-screenshot-package.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-final-screenshot-package-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

draft_dir="$root_dir/design/app-store/ios-upload-draft-screens"
[ -d "$draft_dir" ] || fail "missing draft screenshot directory: $draft_dir"

missing_replacements="$work_dir/missing-replacements"
mkdir -p "$missing_replacements"
blocked_output="$work_dir/blocked-output"
blocked_rc=0
"$script" \
  --draft "$draft_dir" \
  --replacement-dir "$missing_replacements" \
  --output "$blocked_output" \
  --force > "$work_dir/blocked.log" 2>&1 || blocked_rc=$?

[ "$blocked_rc" -eq 1 ] || fail "missing replacement fixture should fail"
[ -f "$blocked_output/replacement_status.tsv" ] || fail "blocked output should include replacement_status.tsv"
grep -Fq $'05-add-friend-real-core\tBLOCKED' "$blocked_output/replacement_status.tsv" || fail "blocked output should name add-friend replacement"
grep -Fq $'missing_replacements\t4' "$blocked_output/summary.tsv" || fail "blocked summary should count missing replacements"
pass "missing replacements block package generation"

replacement_dir="$work_dir/replacements"
mkdir -p "$replacement_dir"
cp "$draft_dir/05-add-friend-needs-real-core.jpg" "$replacement_dir/05-add-friend-real-core.jpg"
cp "$draft_dir/06-public-contact-needs-real-core.jpg" "$replacement_dir/06-public-contact-real-core.jpg"
cp "$draft_dir/07-join-group-needs-real-core.jpg" "$replacement_dir/07-join-group-real-core.jpg"
cp "$draft_dir/08-conversation-needs-real-core.jpg" "$replacement_dir/08-conversation-real-core.jpg"

output="$work_dir/final-package"
"$script" \
  --draft "$draft_dir" \
  --replacement-dir "$replacement_dir" \
  --output "$output" \
  --force > "$work_dir/final.log" 2>&1

[ -f "$output/summary.tsv" ] || fail "missing final summary"
[ -f "$output/MANIFEST.md" ] || fail "missing final manifest"
[ -f "$output/FINAL_BLOCKERS.md" ] || fail "missing final blockers file"
[ -f "$output/package.tsv" ] || fail "missing package table"
grep -Fq $'slot_count\t10' "$output/summary.tsv" || fail "summary should count ten slots"
grep -Fq $'missing_replacements\t0' "$output/summary.tsv" || fail "summary should have no missing replacements"
grep -Fq $'strict_final_status\tPASS' "$output/summary.tsv" || fail "strict final check should pass"
grep -Fq "No final screenshot blockers are recorded" "$output/FINAL_BLOCKERS.md" || fail "final blockers should be clear"
if grep -Eiq 'needs-real-core|preview-core|debug[- ]?(only|preview)?|not a final|must be replaced|replace before public release' "$output/MANIFEST.md"; then
  fail "final manifest should not contain non-final blocker language"
fi
if find "$output" -maxdepth 1 -type f -name '*needs-real-core*' | grep -q .; then
  fail "final package should not contain needs-real-core filenames"
fi

scripts/ios/check-app-store-screenshots.sh --final --dir "$output" > "$work_dir/check-final.log" 2>&1
pass "complete replacements produce strict-final package"

echo "[PASS] prepare App Store final screenshot package tests passed"
