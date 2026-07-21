#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
source_dir="${DOWNLOADS_DIR:-$HOME/Downloads}"
output_dir="${OUTPUT_DIR:-/tmp/nome-ios-real-core-batch0}"
full_xcode_dir="${FULL_XCODE_DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
force=0
prepare=0
build_with_nix=0
dry_run=0
job_repo=""

usage() {
  cat <<'USAGE'
Usage:
  scripts/ios/run-real-core-batch0.sh [options]

Batch 0 helper for replacing the current preview iOS core with real iOS core
artifacts. By default this script only stages a complete artifact pair. It
does not replace apps/ios/Libraries unless --prepare is supplied.

Options:
  --source DIR       Directory containing pkg-ios-aarch64-swift-json and
                     pkg-ios-x86_64-swift-json directories or .zip files.
                     Default: DOWNLOADS_DIR or ~/Downloads.
  --job-repo URL     Hydra job repository URL. The helper audits the URL, then
                     downloads both iOS core artifacts into --output.
  --output DIR       Directory where a complete staged pair is written.
                     Default: /tmp/nome-ios-real-core-batch0.
  --build-with-nix   If local artifacts are not available, run the two Nix
                     iOS core builds and stage their result symlinks.
  --prepare          After staging, run prepare-real-core and final preflight.
  --force            Replace existing staged pkg-ios-* files in --output.
  --dry-run          Print the commands that would run.
  -h, --help         Show this help.

Examples:
  scripts/ios/run-real-core-batch0.sh --source ~/Downloads --output /tmp/nome-ios-core --force
  scripts/ios/run-real-core-batch0.sh --job-repo https://example/job/stable --output /tmp/nome-ios-core --force
  scripts/ios/run-real-core-batch0.sh --build-with-nix --output /tmp/nome-ios-core --force
  scripts/ios/run-real-core-batch0.sh --source /tmp/nome-ios-core --prepare
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
      source_dir="$1"
      ;;
    --job-repo)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --job-repo requires a URL" >&2
        exit 2
      fi
      job_repo="$1"
      ;;
    --output)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --output requires a directory" >&2
        exit 2
      fi
      output_dir="$1"
      ;;
    --build-with-nix)
      build_with_nix=1
      ;;
    --prepare)
      prepare=1
      ;;
    --force)
      force=1
      ;;
    --dry-run)
      dry_run=1
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

have_cmd() {
  command -v "$1" >/dev/null 2>&1
}

run_cmd() {
  printf '[RUN]'
  printf ' %q' "$@"
  printf '\n'

  if [ "$dry_run" -eq 0 ]; then
    "$@"
  fi
}

artifact_pair_exists_in() {
  local dir="$1"

  [ -e "$dir/pkg-ios-aarch64-swift-json" ] || [ -f "$dir/pkg-ios-aarch64-swift-json.zip" ] || return 1
  [ -e "$dir/pkg-ios-x86_64-swift-json" ] || [ -f "$dir/pkg-ios-x86_64-swift-json.zip" ] || return 1
}

download_hydra_artifacts() {
  local arch
  local url

  if [ "$dry_run" -eq 0 ]; then
    "$root_dir/scripts/ios/check-real-core-sources.sh" --job-repo "$job_repo"
  else
    run_cmd "$root_dir/scripts/ios/check-real-core-sources.sh" --job-repo "$job_repo"
  fi

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

  run_cmd mkdir -p "$output_dir"
  for arch in aarch64 x86_64; do
    url="$job_repo/$arch-darwin.$arch-darwin-ios:lib:simplex-chat/latest/download/1"
    run_cmd curl --tlsv1.2 --fail --location \
      -o "$output_dir/pkg-ios-$arch-swift-json.zip" \
      "$url"
  done
}

stage_from_source() {
  local source="$1"
  local force_args=()
  local source_abs
  local output_abs

  if [ "$force" -eq 1 ]; then
    force_args=(--force)
  fi

  source_abs="$(cd "$source" && pwd -P)"
  mkdir -p "$output_dir"
  output_abs="$(cd "$output_dir" && pwd -P)"

  if [ "$source_abs" = "$output_abs" ] && artifact_pair_exists_in "$output_abs"; then
    echo "[INFO] Source and output are the same staged artifact directory; skipping restage"
    return
  fi

  run_cmd "$root_dir/scripts/ios/stage-real-core-artifacts.sh" \
    --source "$source" \
    --output "$output_dir" \
    "${force_args[@]}"
}

cat <<'HEADER'
Nome iOS real-core Batch 0 helper
=================================
HEADER

echo "[INFO] Source: $source_dir"
if [ -n "$job_repo" ]; then
  echo "[INFO] Job repo: $job_repo"
fi
echo "[INFO] Output: $output_dir"
echo "[INFO] Prepare libraries: $prepare"
echo "[INFO] Build with Nix fallback: $build_with_nix"

if "$root_dir/scripts/ios/check-real-core.sh" >/tmp/nome-real-core-batch0-preflight.log 2>&1; then
  echo "[PASS] Real iOS core is already installed"
  sed -n '1,160p' /tmp/nome-real-core-batch0-preflight.log
  exit 0
fi

if [ -n "$job_repo" ]; then
  echo "[INFO] Downloading complete artifact pair from Hydra job repository"
  download_hydra_artifacts
elif artifact_pair_exists_in "$source_dir"; then
  echo "[INFO] Found complete artifact pair in source directory"
  stage_from_source "$source_dir"
elif [ "$build_with_nix" -eq 1 ]; then
  if ! have_cmd nix; then
    echo "[FAIL] --build-with-nix was requested, but nix is not installed" >&2
    echo "[INFO] Run scripts/ios/check-real-core-build-env.sh for the local build prerequisites." >&2
    exit 1
  fi

  nix_results="$output_dir/nix-results"
  run_cmd mkdir -p "$nix_results"
  run_cmd env "DEVELOPER_DIR=$full_xcode_dir" nix build \
    -o "$nix_results/result-aarch64" \
    '.#aarch64-darwin-ios:lib:simplex-chat'
  run_cmd env "DEVELOPER_DIR=$full_xcode_dir" nix build \
    -o "$nix_results/result-x86_64" \
    '.#x86_64-darwin-ios:lib:simplex-chat'
  stage_from_source "$nix_results"
else
  echo "[FAIL] No complete artifact pair found in $source_dir" >&2
  echo "[INFO] Supply --source DIR with both pkg-ios-* artifacts, or re-run with --build-with-nix once nix is installed." >&2
  echo "[INFO] Current preflight log:" >&2
  sed -n '1,160p' /tmp/nome-real-core-batch0-preflight.log >&2
  exit 1
fi

if [ "$dry_run" -eq 0 ] && ! artifact_pair_exists_in "$output_dir"; then
  echo "[FAIL] Batch 0 did not produce a complete staged artifact pair in $output_dir" >&2
  exit 1
fi

if [ "$prepare" -eq 1 ]; then
  run_cmd "$root_dir/scripts/ios/prepare-real-core.sh" --downloads-dir "$output_dir" --downloads
  run_cmd "$root_dir/scripts/ios/check-real-core.sh"
else
  cat <<EOF

[PASS] Batch 0 artifact staging is ready
[INFO] Staged artifacts: $output_dir
[INFO] To replace preview libraries and run final preflight:
  scripts/ios/run-real-core-batch0.sh --source "$output_dir" --prepare
EOF
fi
