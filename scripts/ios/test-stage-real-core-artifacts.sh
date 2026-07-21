#!/bin/bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
stage_script="$root_dir/scripts/ios/stage-real-core-artifacts.sh"
output_dir="${OUTPUT_DIR:-}"

if [ -z "$output_dir" ]; then
  output_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-stage-real-core-artifacts-test.XXXXXX")"
fi

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

make_zip_fixture() {
  local dir="$1"
  local arch="$2"

  mkdir -p "$dir"
  printf 'fake-%s-artifact\n' "$arch" > "$dir/pkg-ios-$arch-swift-json.zip"
}

expect_fail() {
  local name="$1"
  shift
  local log="$output_dir/$name.log"
  local rc=0

  "$@" > "$log" 2>&1 || rc=$?
  if [ "$rc" -eq 0 ]; then
    sed -n '1,120p' "$log" >&2
    fail "$name unexpectedly passed"
  fi

  pass "$name"
}

complete_source="$output_dir/complete-source"
complete_output="$output_dir/complete-output"
mkdir -p "$complete_source/result-aarch64" "$complete_source/result-x86_64"
make_zip_fixture "$complete_source/result-aarch64" aarch64
make_zip_fixture "$complete_source/result-x86_64" x86_64

"$stage_script" --source "$complete_source" --output "$complete_output" > "$output_dir/complete.log" 2>&1

[ -f "$complete_output/pkg-ios-aarch64-swift-json.zip" ] || fail "missing staged aarch64 zip"
[ -f "$complete_output/pkg-ios-x86_64-swift-json.zip" ] || fail "missing staged x86_64 zip"
pass "complete_pair"

missing_source="$output_dir/missing-source"
mkdir -p "$missing_source/result-aarch64"
make_zip_fixture "$missing_source/result-aarch64" aarch64
expect_fail "missing_arch" "$stage_script" --source "$missing_source" --output "$output_dir/missing-output"

ambiguous_source="$output_dir/ambiguous-source"
mkdir -p "$ambiguous_source/one" "$ambiguous_source/two" "$ambiguous_source/x86"
make_zip_fixture "$ambiguous_source/one" aarch64
make_zip_fixture "$ambiguous_source/two" aarch64
make_zip_fixture "$ambiguous_source/x86" x86_64
expect_fail "ambiguous_arch" "$stage_script" --source "$ambiguous_source" --output "$output_dir/ambiguous-output"

echo "[PASS] stage-real-core-artifacts test logs: $output_dir"
