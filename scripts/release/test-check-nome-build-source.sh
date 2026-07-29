#!/bin/sh
set -eu

root_dir=$(cd "$(dirname "$0")/../.." && pwd)
gate="$root_dir/scripts/release/check-nome-build-source.sh"
work_dir=$(mktemp -d "${TMPDIR:-/tmp}/nome-release-gate-test.XXXXXX")
unified_branch='codex/nome-v656-unified'
android_authority_branch='codex/nome-v656-consolidated-android'
ios_authority_branch='codex/nome-v656-consolidated-ios'
private_marker='NOME_PRIVATE_SENTINEL=must-not-leak'

fail() {
  printf '[FAIL] %s\n' "$*" >&2
  exit 1
}

make_repo() {
  name=$1
  branch=${2:-$unified_branch}
  repo="$work_dir/$name"
  mkdir -p "$repo"
  git -C "$repo" init -q -b "$branch"
  git -C "$repo" config user.name 'Nome Gate Test'
  git -C "$repo" config user.email 'nome-gate@example.invalid'
  printf '.cache/\n' > "$repo/.gitignore"
  mkdir -p "$repo/.cache"
  printf 'source\n' > "$repo/source.txt"
  printf 'non-secret configuration\n' > "$repo/release.config"
  printf '%s\n' "$private_marker" > "$repo/.cache/local-release.config"
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

sha256_file() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    fail 'no SHA-256 tool is available'
  fi
}

run_gate() (
  repo=$1
  platform=$2
  shift 2

  case "$platform" in
    ios) authority_branch=$ios_authority_branch ;;
    android|core|desktop) authority_branch=$android_authority_branch ;;
    *) fail "unsupported fixture platform: $platform" ;;
  esac

  cd "$repo"
  "$gate" \
    --platform "$platform" \
    --allowed-branch "$unified_branch" \
    --allowed-branch "$authority_branch" \
    --config release.config \
    --config .cache/local-release.config \
    "$@"
)

assert_config_fingerprints() (
  log_file=$1
  repo=$2

  for config in release.config .cache/local-release.config; do
    expected=$(sha256_file "$repo/$config")
    grep -Fqx "config=$config sha256=$expected" "$log_file" ||
      fail "incorrect or missing fingerprint for $config"
  done

  if grep -Fq "$private_marker" "$log_file"; then
    fail 'configuration value leaked into gate output'
  fi
)

clean_repo=$(make_repo clean)
git -C "$clean_repo" status --short | grep -q . && fail 'clean fixture is dirty'
for platform in ios android core desktop; do
  run_gate "$clean_repo" "$platform" > "$work_dir/clean-$platform.log"
  grep -Fqx "platform=$platform" "$work_dir/clean-$platform.log" ||
    fail "$platform identity missing"
  grep -Fqx "branch=$unified_branch" "$work_dir/clean-$platform.log" ||
    fail "$platform did not bind unified branch"
  assert_config_fingerprints "$work_dir/clean-$platform.log" "$clean_repo"
done

cache_repo=$(make_repo ignored-cache)
mkdir -p "$cache_repo/.cache"
printf 'cache\n' > "$cache_repo/.cache/generated.bin"
run_gate "$cache_repo" ios > "$work_dir/ignored-cache.log"

dirty_repo=$(make_repo tracked-dirty)
printf 'changed\n' >> "$dirty_repo/source.txt"
expect_fail tracked-dirty run_gate "$dirty_repo" android

staged_repo=$(make_repo staged-dirty)
printf 'changed\n' >> "$staged_repo/source.txt"
git -C "$staged_repo" add source.txt
expect_fail staged-dirty run_gate "$staged_repo" ios

for fixture in \
  android:NewAndroidFeature.kt \
  core:NewCoreFeature.hs \
  desktop:NewDesktopFeature.kt \
  ios:NewIOSFeature.swift
do
  platform=${fixture%%:*}
  filename=${fixture#*:}
  untracked_repo=$(make_repo "untracked-$platform")
  printf 'untracked source\n' > "$untracked_repo/$filename"
  expect_fail "untracked-$platform" run_gate "$untracked_repo" "$platform"
done

wrong_branch_repo=$(make_repo wrong-branch main)
expect_fail wrong-branch run_gate "$wrong_branch_repo" android

detached_repo=$(make_repo detached)
git -C "$detached_repo" checkout -q --detach
expect_fail detached run_gate "$detached_repo" core

legacy_android_repo=$(make_repo legacy-android "$android_authority_branch")
for platform in android core desktop; do
  run_gate "$legacy_android_repo" "$platform" > "$work_dir/legacy-$platform.log"
done

legacy_ios_repo=$(make_repo legacy-ios "$ios_authority_branch")
run_gate "$legacy_ios_repo" ios > "$work_dir/legacy-ios.log"

expect_fail ios-on-android-authority run_gate "$legacy_android_repo" ios
expect_fail android-on-ios-authority run_gate "$legacy_ios_repo" android

artifact_repo=$(make_repo artifact)
artifact_file="$work_dir/Nome-test.artifact"
printf 'artifact bytes\n' > "$artifact_file"
run_gate "$artifact_repo" desktop --artifact "$artifact_file" > "$work_dir/artifact.log"
artifact_bytes=$(wc -c < "$artifact_file" | tr -d '[:space:]')
artifact_sha=$(sha256_file "$artifact_file")
grep -Fqx "artifact=$artifact_file bytes=$artifact_bytes sha256=$artifact_sha" "$work_dir/artifact.log" ||
  fail 'artifact identity did not match exact bytes and SHA-256'

printf '[PASS] unified branch accepted for iOS, Android, Core, and Desktop\n'
printf '[PASS] matching legacy authorities accepted and cross-platform misuse rejected\n'
printf '[PASS] detached, wrong-branch, tracked, staged, and platform source states rejected\n'
printf '[PASS] configuration fingerprints emitted without leaking values\n'
printf '[PASS] exact artifact size and SHA-256 bound to the clean commit\n'
printf '[PASS] test evidence retained at %s\n' "$work_dir"
