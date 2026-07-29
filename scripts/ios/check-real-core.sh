#!/bin/bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
ios_dir="${IOS_LIB_DIR:-$root_dir/apps/ios/Libraries/ios}"
sim_dir="${SIM_LIB_DIR:-$root_dir/apps/ios/Libraries/sim}"
downloads_dir="${DOWNLOADS_DIR:-$HOME/Downloads}"
min_core_bytes="${MIN_REAL_CORE_LIB_BYTES:-1000000}"
required_sim_arch="${REQUIRED_SIM_ARCH:-$(uname -m)}"
status=0
core_status=0
mac2ios_bin="${MAC2IOS:-}"
target="all"
job_repo=""

usage() {
  cat <<'USAGE'
Usage: scripts/ios/check-real-core.sh [options]

Read-only preflight for installed iOS real-core libraries.

Options:
  --target TARGET       all, simulator, or physical-device. Default: all.
  --job-repo URL        Also check a Hydra job repository for iOS artifacts.
  -h, --help            Show this help.
USAGE
}

ok() {
  printf '[OK] %s\n' "$1"
}

warn() {
  printf '[WARN] %s\n' "$1"
}

fail() {
  printf '[FAIL] %s\n' "$1"
  status=1
}

fail_core() {
  fail "$1"
  core_status=1
}

info() {
  printf '[INFO] %s\n' "$1"
}

have_cmd() {
  command -v "$1" >/dev/null 2>&1
}

find_mac2ios() {
  if [ -n "$mac2ios_bin" ] && [ -x "$mac2ios_bin" ]; then
    return 0
  fi

  if have_cmd mac2ios; then
    mac2ios_bin="$(command -v mac2ios)"
    return 0
  fi

  if [ -x "$root_dir/tools/bin/mac2ios" ]; then
    mac2ios_bin="$root_dir/tools/bin/mac2ios"
    return 0
  fi

  return 1
}

first_core_lib() {
  find "$1" -maxdepth 1 -type f -name 'libHSsimplex-chat*.a' 2>/dev/null | sort | head -1
}

library_archs() {
  local lib="$1"
  local output

  if ! have_cmd lipo; then
    return 1
  fi

  output="$(lipo -info "$lib" 2>/dev/null || true)"
  if [ -z "$output" ]; then
    return 1
  fi

  case "$output" in
    *" are: "*)
      printf '%s\n' "${output##* are: }"
      ;;
    *" is architecture: "*)
      printf '%s\n' "${output##* is architecture: }"
      ;;
    *)
      return 1
      ;;
  esac
}

arch_list_contains() {
  local archs="$1"
  local required_arch="$2"

  printf ' %s ' "$archs" | grep -Fq " $required_arch "
}

library_platforms() {
  local lib="$1"

  if ! have_cmd otool; then
    return 1
  fi

  otool -l "$lib" 2>/dev/null |
    awk '$1 == "platform" { print $2 }' |
    sort -u
}

check_library_archs() {
  local label="$1"
  local dir="$2"
  local required_arch="$3"
  local required_platform="$4"
  local lib
  local archs
  local platforms
  local checked=0

  if [ ! -d "$dir" ]; then
    return
  fi

  while IFS= read -r lib; do
    checked=$((checked + 1))
    if ! archs="$(library_archs "$lib")"; then
      fail_core "$label library architecture could not be inspected with lipo: $(basename "$lib")"
    elif arch_list_contains "$archs" "$required_arch"; then
      ok "$label library supports required architecture $required_arch: $(basename "$lib") [$archs]"
    else
      fail_core "$label library architecture mismatch: $(basename "$lib") has [$archs], requires $required_arch"
    fi

    if ! platforms="$(library_platforms "$lib")" || [ -z "$platforms" ]; then
      fail_core "$label library platform metadata could not be inspected with otool: $(basename "$lib")"
    elif [ "$platforms" = "$required_platform" ]; then
      ok "$label library uses required platform $required_platform: $(basename "$lib")"
    else
      fail_core "$label library platform mismatch: $(basename "$lib") has [$platforms], requires $required_platform"
    fi
  done < <(find "$dir" -maxdepth 1 -type f -name '*.a' 2>/dev/null | sort)

  if [ "$checked" -eq 0 ]; then
    fail_core "$label has no static .a libraries to inspect in $dir"
  fi
}

check_core_dir() {
  local label="$1"
  local dir="$2"
  local required_arch="$3"
  local required_platform="$4"
  local core_lib
  local core_size

  if [ ! -d "$dir" ]; then
    fail_core "$label libraries directory is missing: $dir"
    return
  fi

  core_lib="$(first_core_lib "$dir")"
  if [ -z "$core_lib" ]; then
    fail_core "$label is missing libHSsimplex-chat*.a in $dir"
    return
  fi

  core_size="$(wc -c < "$core_lib" | tr -d ' ')"
  if [ "$core_size" -lt "$min_core_bytes" ]; then
    fail_core "$label core library is too small for a production Haskell core: $(basename "$core_lib") is ${core_size} bytes"
  else
    ok "$label core library size looks production-like: $(basename "$core_lib") is ${core_size} bytes"
  fi

  if strings "$core_lib" 2>/dev/null | grep -Eq 'preview-agent|preview-token|simplex:/contact#preview|nome\.local/preview'; then
    fail_core "$label core library contains preview-core markers"
  else
    ok "$label core library has no preview-core markers"
  fi

  check_library_archs "$label" "$dir" "$required_arch" "$required_platform"
}

check_download_artifacts() {
  local arch
  local found=0

  for arch in aarch64 x86_64; do
    if [ -d "$downloads_dir/pkg-ios-$arch-swift-json" ]; then
      ok "Found extracted artifact directory: $downloads_dir/pkg-ios-$arch-swift-json"
      found=1
    elif [ -f "$downloads_dir/pkg-ios-$arch-swift-json.zip" ]; then
      ok "Found artifact zip: $downloads_dir/pkg-ios-$arch-swift-json.zip"
      found=1
    else
      warn "Missing $downloads_dir/pkg-ios-$arch-swift-json or .zip"
    fi
  done

  if [ "$found" -eq 0 ] && [ "$core_status" -ne 0 ]; then
    fail "No local iOS core artifacts were found in $downloads_dir"
  elif [ "$found" -eq 0 ]; then
    warn "No local iOS core artifacts were found in $downloads_dir; this is OK if real libraries are already installed"
  fi
}

check_hydra_repo() {
  local job_repo="$1"
  local arch
  local url

  for arch in aarch64 x86_64; do
    url="$job_repo/$arch-darwin.$arch-darwin-ios:lib:simplex-chat/latest/download/1"
    if curl --tlsv1.2 --fail --silent --show-error --location --head "$url" >/dev/null; then
      ok "Hydra artifact is reachable for $arch: $url"
    else
      fail "Hydra artifact is not reachable for $arch: $url"
    fi
  done
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --target)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --target requires all, simulator, or physical-device" >&2
        exit 2
      fi
      target="$1"
      ;;
    --job-repo)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --job-repo requires a Hydra job repository URL" >&2
        exit 2
      fi
      job_repo="$1"
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

case "$target" in
  all|simulator|physical-device)
    ;;
  *)
    echo "[FAIL] --target must be all, simulator, or physical-device" >&2
    exit 2
    ;;
esac

cat <<'HEADER'
Nome iOS real-core preflight
============================
HEADER

info "Check target: $target"
info "Required device architecture: arm64"
info "Required simulator architecture: $required_sim_arch"

case "$target" in
  all|physical-device)
    check_core_dir "Device" "$ios_dir" "arm64" "2"
    ;;
esac

case "$target" in
  all|simulator)
    check_core_dir "Simulator" "$sim_dir" "$required_sim_arch" "7"
    ;;
esac

check_download_artifacts

if find_mac2ios; then
  ok "mac2ios is available: $mac2ios_bin"
elif [ "$core_status" -ne 0 ]; then
  fail "mac2ios is not installed; scripts/ios/prepare-x86_64.sh cannot patch iOS archives"
else
  warn "mac2ios is not installed; this is OK if real libraries are already installed"
fi

if have_cmd nix; then
  ok "nix is installed"
else
  warn "nix is not installed; local flake builds of iOS core are unavailable"
fi

if [ -n "$job_repo" ]; then
  check_hydra_repo "$job_repo"
fi

cat <<'NEXT'

Next supported paths:
1. If you have a Hydra job repository URL:
   scripts/ios/prepare-real-core.sh --job-repo <hydra-job-repo>

2. If you have downloaded artifacts in ~/Downloads:
   scripts/ios/run-real-core-batch0.sh --source ~/Downloads --output /tmp/nome-ios-real-core-batch0 --force
   scripts/ios/run-real-core-batch0.sh --source /tmp/nome-ios-real-core-batch0 --prepare

3. If you need to build locally after installing Nix:
   scripts/ios/run-real-core-batch0.sh --build-with-nix --output /tmp/nome-ios-real-core-batch0 --force
   scripts/ios/run-real-core-batch0.sh --source /tmp/nome-ios-real-core-batch0 --prepare

This script is read-only. It does not replace libraries or download artifacts.
NEXT

exit "$status"
