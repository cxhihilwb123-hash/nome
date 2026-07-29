#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/sync-real-core-xcode-project.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-sync-real-core-project-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

project="$work_dir/project.pbxproj"
sim_dir="$work_dir/sim"
ios_dir="$work_dir/ios"
mkdir -p "$sim_dir" "$ios_dir"

cat > "$project" <<'EOF'
libHSsimplex-chat-old-ghc9.6.3.a
libHSsimplex-chat-old.a
EOF

for dir in "$sim_dir" "$ios_dir"; do
  printf 'ghc\n' > "$dir/libHSsimplex-chat-new-ghc8.10.7.a"
  printf 'plain\n' > "$dir/libHSsimplex-chat-new.a"
done

PROJECT_FILE="$project" LIB_DIR="$sim_dir" IOS_LIB_DIR="$ios_dir" "$script" > "$work_dir/sync.log"
grep -Fq "libHSsimplex-chat-new-ghc8.10.7.a" "$project" || fail "GHC 8.10.7 reference was not installed"
grep -Fq "libHSsimplex-chat-new.a" "$project" || fail "plain reference was not installed"
if grep -Fq "libHSsimplex-chat-old" "$project"; then
  fail "old project references remain"
fi

PROJECT_FILE="$project" LIB_DIR="$sim_dir" IOS_LIB_DIR="$ios_dir" "$script" --check > "$work_dir/check.log"
grep -Fq "already references current real-core archives" "$work_dir/check.log" || fail "check mode did not pass"
echo "[PASS] ghc_8_10_7_project_sync"

printf 'extra\n' > "$sim_dir/libHSsimplex-chat-extra-ghc9.6.3.a"
rc=0
PROJECT_FILE="$project" LIB_DIR="$sim_dir" IOS_LIB_DIR="$ios_dir" "$script" --check > "$work_dir/multiple.log" 2>&1 || rc=$?
[ "$rc" -ne 0 ] || fail "multiple GHC archives unexpectedly passed"
grep -Fq "Expected exactly one simulator GHC libHS archive, found 2" "$work_dir/multiple.log" || fail "multiple archive failure was unclear"
echo "[PASS] multiple_ghc_archives_rejected"

echo "[PASS] sync-real-core-xcode-project tests passed"
