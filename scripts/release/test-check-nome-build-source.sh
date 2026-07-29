#!/bin/sh
set -eu

root_dir=$(cd "$(dirname "$0")/../.." && pwd)
gate="$root_dir/scripts/release/check-nome-build-source.sh"
work_dir=$(mktemp -d "${TMPDIR:-/tmp}/nome-release-gate-test.XXXXXX")

fail() {
  printf '[FAIL] %s\n' "$*" >&2
  exit 1
}

make_repo() {
  name=$1
  branch=${2:-codex/nome-v656-consolidated-ios}
  repo="$work_dir/$name"
  mkdir -p "$repo"
  git -C "$repo" init -q -b "$branch"
  git -C "$repo" config user.name 'Nome Gate Test'
  git -C "$repo" config user.email 'nome-gate@example.invalid'
  printf '.cache/\n' > "$repo/.gitignore"
  printf 'source\n' > "$repo/source.txt"
  printf 'non-secret configuration\n' > "$repo/release.config"
  git -C "$repo" add .gitignore source.txt release.config
  git -C "$repo" commit -q -m base
  printf '%s\n' "$repo"
}

expect_fail() {
  name=$1
  shift
  if "$@" > "$work_dir/$name.stdout" 2> "$work_dir/$name.stderr"; then
    fail "$name unexpectedly passed"
  fi
}

clean_repo=$(make_repo clean)
git -C "$clean_repo" status --short | grep -q . && fail 'clean fixture is dirty'
(
  cd "$clean_repo"
  "$gate" --platform ios --allowed-branch codex/nome-v656-consolidated-ios --config release.config
) > "$work_dir/clean.log"

cache_repo=$(make_repo ignored-cache)
mkdir -p "$cache_repo/.cache"
printf 'cache\n' > "$cache_repo/.cache/generated.bin"
(
  cd "$cache_repo"
  "$gate" --platform ios --allowed-branch codex/nome-v656-consolidated-ios --config release.config
) > "$work_dir/ignored-cache.log"

dirty_repo=$(make_repo tracked-dirty)
printf 'changed\n' >> "$dirty_repo/source.txt"
expect_fail tracked-dirty sh -c "cd '$dirty_repo' && '$gate' --platform ios --allowed-branch codex/nome-v656-consolidated-ios --config release.config"

staged_repo=$(make_repo staged-dirty)
printf 'changed\n' >> "$staged_repo/source.txt"
git -C "$staged_repo" add source.txt
expect_fail staged-dirty sh -c "cd '$staged_repo' && '$gate' --platform ios --allowed-branch codex/nome-v656-consolidated-ios --config release.config"

untracked_repo=$(make_repo untracked-source)
printf 'untracked source\n' > "$untracked_repo/NewFeature.swift"
expect_fail untracked-source sh -c "cd '$untracked_repo' && '$gate' --platform ios --allowed-branch codex/nome-v656-consolidated-ios --config release.config"

wrong_branch_repo=$(make_repo wrong-branch main)
expect_fail wrong-branch sh -c "cd '$wrong_branch_repo' && '$gate' --platform ios --allowed-branch codex/nome-v656-consolidated-ios --config release.config"

artifact_repo=$(make_repo artifact)
printf 'artifact bytes\n' > "$work_dir/Nome-test.artifact"
(
  cd "$artifact_repo"
  "$gate" --platform ios --allowed-branch codex/nome-v656-consolidated-ios \
    --config release.config --artifact "$work_dir/Nome-test.artifact"
) > "$work_dir/artifact.log"
grep -Eq '^artifact=.* bytes=[0-9]+ sha256=[0-9a-f]{64}$' "$work_dir/artifact.log" || fail 'artifact identity was not emitted'

printf '[PASS] clean source and ignored cache accepted\n'
printf '[PASS] wrong branch, tracked, staged, and untracked states rejected\n'
printf '[PASS] artifact size and SHA-256 bound to the clean commit\n'
printf '[PASS] test evidence retained at %s\n' "$work_dir"
