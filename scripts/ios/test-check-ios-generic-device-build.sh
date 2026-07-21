#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/check-ios-generic-device-build.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-generic-device-build-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

project="$work_dir/SimpleX.xcodeproj"
derived="$work_dir/DerivedData"
product_dir="$derived/Build/Products/Debug-iphoneos"
output="$work_dir/state"
missing_output="$work_dir/state-missing"

mkdir -p "$project" "$product_dir/Nome.app" "$product_dir/SimpleX NSE.appex" "$product_dir/SimpleX SE.appex"

"$script" \
  --skip-build \
  --project "$project" \
  --derived-data "$derived" \
  --output "$output" \
  --force > "$work_dir/pass.log" 2>&1

grep -Fq $'product_pass_count\t3' "$output/summary.tsv" || fail "expected three product passes"
grep -Fq $'product_fail_count\t0' "$output/summary.tsv" || fail "expected zero product failures"
grep -Fq $'app\tNome.app\tPASS' "$output/products.tsv" || fail "expected app product pass"
grep -Fq $'extension\tSimpleX NSE.appex\tPASS' "$output/products.tsv" || fail "expected NSE extension pass"
grep -Fq $'extension\tSimpleX SE.appex\tPASS' "$output/products.tsv" || fail "expected SE extension pass"

rm -rf "$product_dir/SimpleX SE.appex"
rc=0
"$script" \
  --skip-build \
  --project "$project" \
  --derived-data "$derived" \
  --output "$missing_output" \
  --force > "$work_dir/missing.log" 2>&1 || rc=$?

[ "$rc" -ne 0 ] || fail "missing extension fixture should fail"
grep -Fq $'product_fail_count\t1' "$missing_output/summary.tsv" || fail "expected one product failure"
grep -Fq $'extension\tSimpleX SE.appex\tFAIL' "$missing_output/products.tsv" || fail "expected missing SE extension failure"

pass "generic iOS device build product checker"
