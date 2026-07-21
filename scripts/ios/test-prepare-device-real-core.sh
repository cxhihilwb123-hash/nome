#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/prepare-device-real-core.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-prepare-device-real-core-test.XXXXXX")"
restricted_path="/usr/bin:/bin:/usr/sbin:/sbin"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

make_fake_tooling() {
  local bin_dir="$1"

  mkdir -p "$bin_dir"
  cat > "$bin_dir/lipo" <<'SH'
#!/bin/sh

for arg do
  lib="$arg"
done

case "$lib" in
  *wrongarch*)
    echo "Non-fat file: $lib is architecture: x86_64"
    ;;
  *)
    echo "Non-fat file: $lib is architecture: arm64"
    ;;
esac
SH
  chmod +x "$bin_dir/lipo"
}

make_device_artifact() {
  local dir="$1"
  local marker="${2:-real-core-placeholder}"

  mkdir -p "$dir"
  printf '%s\n' "$marker" > "$dir/libHSsimplex-chat-test-ghc9.6.3.a"
  printf 'plain-real-core-placeholder\n' > "$dir/libHSsimplex-chat-test.a"
  printf 'ffi\n' > "$dir/libffi.a"
  printf 'gmp\n' > "$dir/libgmp.a"
  printf 'gmpxx\n' > "$dir/libgmpxx.a"
}

run_script() {
  local log="$1"
  shift

  PATH="$fake_bin:$restricted_path" \
    PROJECT_FILE="$work_dir/project.pbxproj" \
    MIN_REAL_CORE_LIB_BYTES=1 \
    "$script" "$@" > "$log" 2>&1
}

expect_fail() {
  local name="$1"
  local expected="$2"
  shift 2
  local log="$work_dir/$name.log"
  local rc=0

  run_script "$log" "$@" || rc=$?
  if [ "$rc" -eq 0 ]; then
    sed -n '1,160p' "$log" >&2
    fail "$name unexpectedly passed"
  fi

  if ! grep -Fq "$expected" "$log"; then
    sed -n '1,160p' "$log" >&2
    fail "$name missing expected text: $expected"
  fi

  pass "$name"
}

fake_bin="$work_dir/bin"
make_fake_tooling "$fake_bin"
cat > "$work_dir/project.pbxproj" <<'EOF'
libHSsimplex-chat-project-ghc9.6.3.a
libHSsimplex-chat-project.a
EOF

expect_fail \
  "missing_source" \
  "Source directory is missing" \
  --source "$work_dir/missing-source"

preview_source="$work_dir/preview-source"
make_device_artifact "$preview_source" "preview-token"
expect_fail \
  "preview_marker" \
  "contains preview-core markers" \
  --source "$preview_source"

wrongarch_source="$work_dir/wrongarch-source"
make_device_artifact "$wrongarch_source"
expect_fail \
  "wrong_arch" \
  "does not support arm64" \
  --source "$wrongarch_source"

valid_source="$work_dir/valid-source"
valid_target="$work_dir/valid-target"
make_device_artifact "$valid_source"
run_script "$work_dir/dry-run.log" --source "$valid_source" --target "$valid_target"

grep -Fq "Dry run only. No files were copied." "$work_dir/dry-run.log" || fail "dry run did not report read-only mode"
[ ! -e "$valid_target" ] || fail "dry run unexpectedly created target directory"
pass "dry_run_valid_source"

mkdir -p "$valid_target"
printf 'keep-me\n' > "$valid_target/existing.txt"
expect_fail \
  "target_nonempty_without_force" \
  "Target directory is not empty" \
  --source "$valid_source" \
  --target "$valid_target" \
  --prepare

grep -Fq "keep-me" "$valid_target/existing.txt" || fail "failed prepare modified target without --force"
pass "target_preserved_without_force"

run_script "$work_dir/prepare-force.log" \
  --source "$valid_source" \
  --target "$valid_target" \
  --prepare \
  --force

grep -Fq "Installed device real-core libraries" "$work_dir/prepare-force.log" || fail "force prepare did not report install"
[ -f "$valid_target/libHSsimplex-chat-test-ghc9.6.3.a" ] || fail "missing installed GHC archive"
[ -f "$valid_target/libHSsimplex-chat-test.a" ] || fail "missing installed plain archive"
[ -f "$valid_target/libffi.a" ] || fail "missing installed dependency"
pass "prepare_force_copies_libraries"

echo "[PASS] prepare-device-real-core helper tests passed"
