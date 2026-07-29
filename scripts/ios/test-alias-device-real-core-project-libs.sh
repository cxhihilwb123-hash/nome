#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/alias-device-real-core-project-libs.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-alias-device-core-test.XXXXXX")"
restricted_path="/usr/bin:/bin:/usr/sbin:/sbin"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

make_fake_tooling() {
  local bin_dir="$1"

  mkdir -p "$bin_dir"
  cat > "$bin_dir/lipo" <<'SH'
#!/bin/sh

for arg do
  lib="$arg"
done
echo "Non-fat file: $lib is architecture: arm64"
SH
  chmod +x "$bin_dir/lipo"

  cat > "$bin_dir/otool" <<'SH'
#!/bin/sh

for arg do
  lib="$arg"
done
case "$lib" in
  *darwin*) echo " platform 1" ;;
  *) echo " platform 2" ;;
esac
SH
  chmod +x "$bin_dir/otool"
}

make_project() {
  local file="$1"

  cat > "$file" <<'EOF'
libHSsimplex-chat-project-ghc8.10.7.a
libHSsimplex-chat-project.a
EOF
}

make_source_pair() {
  local dir="$1"
  local stem="$2"

  mkdir -p "$dir"
  printf 'ghc-core\n' > "$dir/libHSsimplex-chat-$stem-ghc8.10.7.a"
  printf 'plain-core\n' > "$dir/libHSsimplex-chat-$stem.a"
}

run_alias() {
  local project="$1"
  local libs="$2"
  shift 2

  PATH="$fake_bin:$restricted_path" \
    PROJECT_FILE="$project" \
    IOS_LIB_DIR="$libs" \
    MIN_REAL_CORE_LIB_BYTES=1 \
    "$script" "$@"
}

fake_bin="$work_dir/bin"
project="$work_dir/project.pbxproj"
libs="$work_dir/libs"
make_fake_tooling "$fake_bin"
make_project "$project"
make_source_pair "$libs" "candidate"

run_alias "$project" "$libs" > "$work_dir/dry-run.log"
grep -Fq "GHC alias needed" "$work_dir/dry-run.log" || fail "dry run did not report GHC alias"
[ ! -e "$libs/libHSsimplex-chat-project-ghc8.10.7.a" ] || fail "dry run created GHC alias"

run_alias "$project" "$libs" --prepare > "$work_dir/prepare.log"
[ "$(readlink "$libs/libHSsimplex-chat-project-ghc8.10.7.a")" = "libHSsimplex-chat-candidate-ghc8.10.7.a" ] || fail "wrong GHC alias target"
[ "$(readlink "$libs/libHSsimplex-chat-project.a")" = "libHSsimplex-chat-candidate.a" ] || fail "wrong plain alias target"
echo "[PASS] ghc_8_10_7_aliases_created"

matching_libs="$work_dir/matching-libs"
make_source_pair "$matching_libs" "project"
run_alias "$project" "$matching_libs" > "$work_dir/matching.log"
grep -Fq "GHC already uses the project archive name" "$work_dir/matching.log" || fail "matching GHC project name was not accepted"
grep -Fq "plain already uses the project archive name" "$work_dir/matching.log" || fail "matching plain project name was not accepted"
echo "[PASS] matching_project_names_are_idempotent"

darwin_libs="$work_dir/darwin-libs"
make_source_pair "$darwin_libs" "darwin"
rc=0
run_alias "$project" "$darwin_libs" --prepare > "$work_dir/darwin.log" 2>&1 || rc=$?
[ "$rc" -ne 0 ] || fail "Darwin platform aliases unexpectedly passed"
grep -Fq "must use IOS platform metadata" "$work_dir/darwin.log" || fail "Darwin failure did not report platform mismatch"
echo "[PASS] darwin_platform_alias_rejected"

echo "[PASS] alias-device-real-core-project-libs tests passed"
