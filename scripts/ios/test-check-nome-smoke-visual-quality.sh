#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/check-nome-smoke-visual-quality.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-smoke-visual-quality-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

image_one="$root_dir/design/product/pages-v2/01-home-inbox.png"
image_two="$root_dir/design/product/pages-v2/02-add-friend-one-time.png"

[ -f "$image_one" ] || fail "missing fixture image: $image_one"
[ -f "$image_two" ] || fail "missing fixture image: $image_two"

sips_value() {
  local file="$1"
  local key="$2"

  sips -g "$key" "$file" 2>/dev/null | awk -F ': ' -v key="$key" '$1 ~ key {print $2; found=1; exit} END {exit found ? 0 : 1}'
}

add_manifest_row() {
  local manifest="$1"
  local label="$2"
  local source="$3"
  local target="$4"
  local bytes_override="${5:-}"

  cp "$source" "$target"
  width="$(sips_value "$target" pixelWidth)"
  height="$(sips_value "$target" pixelHeight)"
  bytes="$(wc -c < "$target" | tr -d '[:space:]')"
  hash="$(shasum -a 256 "$target" | awk '{print $1}')"
  if [ -n "$bytes_override" ]; then
    bytes="$bytes_override"
  fi
  printf '%s\t\t%s\t%s\t%s\t%s\t%s\n' "$label" "$width" "$height" "$bytes" "$hash" "$target" >> "$manifest"
}

make_manifest() {
  local manifest="$1"
  printf 'label\targs\twidth\theight\tbytes\tsha256\tpath\n' > "$manifest"
}

good_manifest="$work_dir/good.tsv"
good_dir="$work_dir/good-images"
good_output="$work_dir/good-output"
mkdir -p "$good_dir"
make_manifest "$good_manifest"
add_manifest_row "$good_manifest" "01-home" "$image_one" "$good_dir/01-home.png"
add_manifest_row "$good_manifest" "10-add-friend" "$image_two" "$good_dir/10-add-friend.png"

"$script" --manifest "$good_manifest" --expected-count 2 --output "$good_output" --force > "$work_dir/good.log" 2>&1

[ -f "$good_output/summary.tsv" ] || fail "missing good summary"
[ -f "$good_output/images.tsv" ] || fail "missing good images table"
[ -f "$good_output/failures.tsv" ] || fail "missing good failures table"
grep -Fq $'image_rows\t2' "$good_output/summary.tsv" || fail "good summary should count two rows"
grep -Fq $'failure_count\t0' "$good_output/summary.tsv" || fail "good summary should have no failures"
grep -Fq $'01-home\tPASS' "$good_output/images.tsv" || fail "home fixture should pass"
grep -Fq $'10-add-friend\tPASS' "$good_output/images.tsv" || fail "add-friend fixture should pass"
pass "valid smoke screenshots pass"

bad_manifest="$work_dir/bad-bytes.tsv"
bad_dir="$work_dir/bad-images"
bad_output="$work_dir/bad-output"
mkdir -p "$bad_dir"
make_manifest "$bad_manifest"
add_manifest_row "$bad_manifest" "01-home" "$image_one" "$bad_dir/01-home.png" "123"

bad_rc=0
"$script" --manifest "$bad_manifest" --expected-count 1 --output "$bad_output" --force > "$work_dir/bad.log" 2>&1 || bad_rc=$?
[ "$bad_rc" -eq 1 ] || fail "bad bytes fixture should fail"
grep -Fq "byte_mismatch" "$bad_output/failures.tsv" || fail "bad bytes fixture should record byte_mismatch"
pass "manifest byte mismatch fails"

duplicate_manifest="$work_dir/duplicate.tsv"
duplicate_dir="$work_dir/duplicate-images"
duplicate_output="$work_dir/duplicate-output"
mkdir -p "$duplicate_dir"
make_manifest "$duplicate_manifest"
add_manifest_row "$duplicate_manifest" "01-home" "$image_one" "$duplicate_dir/01-home.png"
add_manifest_row "$duplicate_manifest" "02-home-copy" "$image_one" "$duplicate_dir/02-home-copy.png"

duplicate_rc=0
"$script" --manifest "$duplicate_manifest" --expected-count 2 --output "$duplicate_output" --force > "$work_dir/duplicate.log" 2>&1 || duplicate_rc=$?
[ "$duplicate_rc" -eq 1 ] || fail "duplicate hash fixture should fail"
grep -Fq "duplicate_sha256" "$duplicate_output/failures.tsv" || fail "duplicate fixture should record duplicate hash"
pass "duplicate screenshot hashes fail"

echo "[PASS] check-nome-smoke-visual-quality tests passed"
