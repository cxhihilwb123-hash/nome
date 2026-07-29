#!/bin/sh
set -eu

fail() {
  printf '[FAIL] %s\n' "$*" >&2
  exit 1
}

usage() {
  cat <<'EOF'
Usage: check-nome-build-source.sh --platform NAME --allowed-branch BRANCH
       --config FILE [--config FILE ...] [--artifact FILE]

Fails unless the current checkout is an allowed branch at one clean commit.
Ignored caches and ignored local configuration may exist. Configuration values
are never printed; only paths and SHA-256 fingerprints are emitted.
EOF
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

file_size() {
  if stat -f '%z' "$1" >/dev/null 2>&1; then
    stat -f '%z' "$1"
  else
    stat -c '%s' "$1"
  fi
}

platform=''
allowed_branches=''
configs=''
artifact=''

while [ "$#" -gt 0 ]; do
  case "$1" in
    --platform)
      [ "$#" -ge 2 ] || fail '--platform requires a value'
      platform=$2
      shift 2
      ;;
    --allowed-branch)
      [ "$#" -ge 2 ] || fail '--allowed-branch requires a value'
      allowed_branches="${allowed_branches}:$2:"
      shift 2
      ;;
    --config)
      [ "$#" -ge 2 ] || fail '--config requires a value'
      if [ -z "$configs" ]; then
        configs=$2
      else
        configs="$configs
$2"
      fi
      shift 2
      ;;
    --artifact)
      [ "$#" -ge 2 ] || fail '--artifact requires a value'
      artifact=$2
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "unknown argument: $1"
      ;;
  esac
done

[ -n "$platform" ] || fail '--platform is required'
[ -n "$allowed_branches" ] || fail 'at least one --allowed-branch is required'
[ -n "$configs" ] || fail 'at least one --config is required'

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || fail 'not inside a Git worktree'
cd "$repo_root"
branch=$(git symbolic-ref --quiet --short HEAD 2>/dev/null) || fail 'detached HEAD is not a release source'
case "$allowed_branches" in
  *":$branch:"*) ;;
  *) fail "branch is not authorized for this build: $branch" ;;
esac

status=$(git status --porcelain=v1 --untracked-files=all)
if [ -n "$status" ]; then
  printf '%s\n' "$status" >&2
  fail 'worktree is not clean; tracked, staged, or untracked files are forbidden'
fi

commit=$(git rev-parse HEAD)
printf 'platform=%s\n' "$platform"
printf 'repository=%s\n' "$repo_root"
printf 'branch=%s\n' "$branch"
printf 'commit=%s\n' "$commit"
printf 'git_status=clean\n'
printf 'controlled_dirty_patch=no\n'

printf '%s\n' "$configs" | while IFS= read -r config; do
  [ -n "$config" ] || continue
  case "$config" in
    /*) config_path=$config ;;
    *) config_path="$repo_root/$config" ;;
  esac
  [ -f "$config_path" ] || fail "configuration source is not a file: $config"
  printf 'config=%s sha256=%s\n' "$config" "$(sha256_file "$config_path")"
done

if [ -n "$artifact" ]; then
  case "$artifact" in
    /*) artifact_path=$artifact ;;
    *) artifact_path="$repo_root/$artifact" ;;
  esac
  [ -f "$artifact_path" ] || fail "artifact is not a file: $artifact"
  printf 'artifact=%s bytes=%s sha256=%s\n' \
    "$artifact" "$(file_size "$artifact_path")" "$(sha256_file "$artifact_path")"
fi

[ "$(git rev-parse HEAD)" = "$commit" ] || fail 'HEAD changed while the gate was running'
[ -z "$(git status --porcelain=v1 --untracked-files=all)" ] || fail 'worktree changed while the gate was running'
printf '[PASS] Nome build source is one clean committed state\n'
