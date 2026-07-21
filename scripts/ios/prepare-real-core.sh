#!/bin/bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
downloads_dir="${DOWNLOADS_DIR:-$HOME/Downloads}"
mode=""
job_repo=""

usage() {
  cat <<'USAGE'
Usage:
  scripts/ios/prepare-real-core.sh --job-repo URL
  scripts/ios/prepare-real-core.sh --downloads

Options:
  --job-repo URL  Download iOS core artifacts from a Hydra job repository,
                  prepare them, then run the real-core preflight.
  --downloads     Use artifacts in ~/Downloads by default:
                  pkg-ios-aarch64-swift-json/
                  pkg-ios-x86_64-swift-json/
                  Also accepts matching .zip files.
  --downloads-dir DIR
                  Use artifacts from DIR instead of ~/Downloads.

Environment:
  DOWNLOADS_DIR   Override the downloads directory for --downloads.
  MAC2IOS         Override the mac2ios executable path.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --job-repo)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --job-repo requires a URL" >&2
        exit 2
      fi
      mode="job-repo"
      job_repo="$1"
      ;;
    --downloads)
      mode="downloads"
      ;;
    --downloads-dir)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --downloads-dir requires a directory" >&2
        exit 2
      fi
      downloads_dir="$1"
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

if [ -z "$mode" ]; then
  echo "[FAIL] Choose --job-repo or --downloads" >&2
  usage >&2
  exit 2
fi

ensure_mac2ios() {
  if [ -n "${MAC2IOS:-}" ] && [ -x "$MAC2IOS" ]; then
    echo "[OK] mac2ios from MAC2IOS=$MAC2IOS"
    return
  fi

  if command -v mac2ios >/dev/null 2>&1; then
    echo "[OK] mac2ios from PATH=$(command -v mac2ios)"
    return
  fi

  if [ -x "$root_dir/tools/bin/mac2ios" ]; then
    echo "[OK] mac2ios from $root_dir/tools/bin/mac2ios"
    return
  fi

  echo "[INFO] mac2ios not found; building local helper"
  "$root_dir/scripts/ios/build-mac2ios.sh"
}

staging_dir=""
cleanup() {
  if [ -n "$staging_dir" ] && [ -d "$staging_dir" ]; then
    rm -rf "$staging_dir"
  fi
}
trap cleanup EXIT

stage_download_artifacts() {
  local missing=0
  local arch
  local source_dir
  local source_zip
  local target_dir

  staging_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-ios-core-artifacts.XXXXXX")"
  for arch in aarch64 x86_64; do
    source_dir="$downloads_dir/pkg-ios-$arch-swift-json"
    source_zip="$downloads_dir/pkg-ios-$arch-swift-json.zip"
    target_dir="$staging_dir/pkg-ios-$arch-swift-json"

    if [ -d "$source_dir" ]; then
      cp -R "$source_dir" "$target_dir"
    elif [ -f "$source_zip" ]; then
      mkdir -p "$target_dir"
      unzip -oq "$source_zip" -d "$target_dir"
    else
      echo "[FAIL] Missing $source_dir or $source_zip" >&2
      missing=1
    fi
  done

  if [ "$missing" -ne 0 ]; then
    echo "[INFO] Expected extracted artifact directories or .zip files under $downloads_dir" >&2
    exit 1
  fi

  for arch in aarch64 x86_64; do
    if ! find "$staging_dir/pkg-ios-$arch-swift-json" -maxdepth 1 -type f | grep -q .; then
      echo "[FAIL] Artifact for $arch did not contain library files after staging" >&2
      exit 1
    fi
  done
}

install_staged_artifacts_to_downloads() {
  local arch

  mkdir -p "$HOME/Downloads"
  for arch in aarch64 x86_64; do
    rm -rf "$HOME/Downloads/pkg-ios-$arch-swift-json"
    cp -R "$staging_dir/pkg-ios-$arch-swift-json" "$HOME/Downloads/pkg-ios-$arch-swift-json"
  done
}

audit_staged_artifacts_before_install() {
  echo "[INFO] Auditing staged iOS core artifacts before replacing app libraries"
  IOS_LIB_DIR="$staging_dir/pkg-ios-aarch64-swift-json" \
    SIM_LIB_DIR="$staging_dir/pkg-ios-x86_64-swift-json" \
    DOWNLOADS_DIR="$downloads_dir" \
    "$root_dir/scripts/ios/check-real-core.sh"
}

ensure_mac2ios

case "$mode" in
  job-repo)
    echo "[INFO] Checking Hydra artifacts: $job_repo"
    "$root_dir/scripts/ios/check-real-core-sources.sh" --job-repo "$job_repo"
    echo "[INFO] Downloading and preparing iOS core artifacts"
    "$root_dir/scripts/ios/download-libs.sh" "$job_repo"
    ;;
  downloads)
    echo "[INFO] Preparing iOS core artifacts from $downloads_dir"
    stage_download_artifacts
    audit_staged_artifacts_before_install
    install_staged_artifacts_to_downloads
    "$root_dir/scripts/ios/prepare-x86_64.sh"
    ;;
esac

echo "[INFO] Running final real-core preflight"
"$root_dir/scripts/ios/check-real-core.sh"
