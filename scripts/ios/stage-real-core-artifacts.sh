#!/bin/bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
output_dir=""
force=0
source_dirs=()

usage() {
  cat <<'USAGE'
Usage:
  scripts/ios/stage-real-core-artifacts.sh --source DIR --output DIR [--force]

Copies a complete real iOS core artifact pair into the layout expected by:

  scripts/ios/prepare-real-core.sh --downloads-dir DIR --downloads

The source directory can be a local artifact folder, a directory containing Nix
result symlinks, or a directory containing extracted pkg-ios-* artifact folders.

Required artifacts:
  pkg-ios-aarch64-swift-json.zip or pkg-ios-aarch64-swift-json/
  pkg-ios-x86_64-swift-json.zip or pkg-ios-x86_64-swift-json/

Options:
  --source DIR   Source directory to search. Can be supplied multiple times.
  --output DIR   Output artifact directory to create or update.
  --force        Replace existing staged pkg-ios-* files/directories in output.
  -h, --help     Show this help.

This script does not run mac2ios and does not replace apps/ios/Libraries.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --source)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --source requires a directory" >&2
        exit 2
      fi
      source_dirs+=("$1")
      ;;
    --output)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --output requires a directory" >&2
        exit 2
      fi
      output_dir="$1"
      ;;
    --force)
      force=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[FAIL] Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

if [ "${#source_dirs[@]}" -eq 0 ]; then
  echo "[FAIL] At least one --source directory is required" >&2
  usage >&2
  exit 2
fi

if [ -z "$output_dir" ]; then
  echo "[FAIL] --output is required" >&2
  usage >&2
  exit 2
fi

for source_dir in "${source_dirs[@]}"; do
  if [ ! -e "$source_dir" ]; then
    echo "[FAIL] Source does not exist: $source_dir" >&2
    exit 1
  fi
done

candidate_for_arch() {
  local arch="$1"
  local artifact_name="pkg-ios-$arch-swift-json"
  local matches=""
  local source_dir
  local count

  for source_dir in "${source_dirs[@]}"; do
    matches="${matches}${matches:+
}$(find -L "$source_dir" \( -type f -name "$artifact_name.zip" -o -type d -name "$artifact_name" \) 2>/dev/null | sort)"
  done

  matches="$(printf '%s\n' "$matches" | sed '/^$/d')"
  count="$(printf '%s\n' "$matches" | sed '/^$/d' | wc -l | tr -d '[:space:]')"

  if [ "$count" -eq 0 ]; then
    echo "[FAIL] Missing $artifact_name.zip or $artifact_name/ in supplied sources" >&2
    return 1
  fi

  if [ "$count" -gt 1 ]; then
    echo "[FAIL] Multiple $artifact_name candidates found; pass a narrower --source" >&2
    printf '%s\n' "$matches" | sed 's/^/[INFO] candidate /' >&2
    return 1
  fi

  printf '%s\n' "$matches"
}

aarch64_candidate="$(candidate_for_arch aarch64)"
x86_64_candidate="$(candidate_for_arch x86_64)"

if [ -d "$output_dir" ] && [ "$(find "$output_dir" -mindepth 1 -maxdepth 1 | wc -l | tr -d '[:space:]')" -gt 0 ]; then
  if [ "$force" -ne 1 ]; then
    echo "[FAIL] Output directory is not empty: $output_dir" >&2
    echo "[INFO] Re-run with --force to replace staged pkg-ios-* artifacts only." >&2
    exit 1
  fi

  rm -rf \
    "$output_dir/pkg-ios-aarch64-swift-json" \
    "$output_dir/pkg-ios-aarch64-swift-json.zip" \
    "$output_dir/pkg-ios-x86_64-swift-json" \
    "$output_dir/pkg-ios-x86_64-swift-json.zip"
fi

mkdir -p "$output_dir"

stage_candidate() {
  local arch="$1"
  local candidate="$2"
  local base_name="pkg-ios-$arch-swift-json"

  if [ -f "$candidate" ]; then
    cp "$candidate" "$output_dir/$base_name.zip"
    echo "[OK] staged $base_name.zip from $candidate"
  elif [ -d "$candidate" ]; then
    cp -R "$candidate" "$output_dir/$base_name"
    echo "[OK] staged $base_name/ from $candidate"
  else
    echo "[FAIL] Candidate disappeared while staging: $candidate" >&2
    exit 1
  fi
}

stage_candidate aarch64 "$aarch64_candidate"
stage_candidate x86_64 "$x86_64_candidate"

cat <<EOF

[PASS] Staged complete real-core artifact pair
[INFO] Output: $output_dir

Next:
  scripts/ios/prepare-real-core.sh --downloads-dir "$output_dir" --downloads
  scripts/ios/check-real-core.sh
EOF
