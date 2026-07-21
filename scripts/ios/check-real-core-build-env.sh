#!/bin/bash

set -u

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
flake_file="$root_dir/flake.nix"
full_xcode_dir="${FULL_XCODE_DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
project_path="${PROJECT_PATH:-$root_dir/apps/ios/SimpleX.xcodeproj}"
scheme="${SCHEME:-SimpleX (iOS)}"
required_sim_arch="${REQUIRED_SIM_ARCH:-$(uname -m)}"
min_free_gb="${MIN_NIX_BUILD_FREE_GB:-80}"
downloads_dir="${DOWNLOADS_DIR:-$HOME/Downloads}"
target="all"
allow_downloaded_artifacts=0
status=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/check-real-core-build-env.sh [options]

Read-only audit for local real-core build prerequisites and optional downloaded
artifact fallback.

Options:
  --target TARGET                 all, simulator, or physical-device. Default: all.
  --allow-downloaded-artifacts    Treat existing downloaded artifacts for the
                                  target as a valid fallback when nix is absent.
  -h, --help                      Show this help.
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

info() {
  printf '[INFO] %s\n' "$1"
}

have_cmd() {
  command -v "$1" >/dev/null 2>&1
}

have_ios_artifact() {
  local arch="$1"

  [ -d "$downloads_dir/pkg-ios-$arch-swift-json" ] ||
    [ -f "$downloads_dir/pkg-ios-$arch-swift-json.zip" ]
}

artifact_fallback_ready() {
  case "$target" in
    physical-device)
      have_ios_artifact aarch64
      ;;
    simulator)
      have_ios_artifact x86_64
      ;;
    all)
      have_ios_artifact aarch64 && have_ios_artifact x86_64
      ;;
    *)
      return 1
      ;;
  esac
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
    --allow-downloaded-artifacts)
      allow_downloaded_artifacts=1
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
Nome iOS real-core local build environment
==========================================
HEADER

info "Check target: $target"
info "Downloaded artifact directory: $downloads_dir"

if [ -f "$flake_file" ]; then
  ok "Found flake.nix"
else
  fail "Missing flake.nix"
fi

if [ -f "$flake_file" ]; then
  for marker in \
    'aarch64-darwin-ios:lib:simplex-chat' \
    'x86_64-darwin-ios:lib:simplex-chat' \
    'iosOverrides "pkg-ios-aarch64-swift-json"' \
    'iosOverrides "pkg-ios-x86_64-swift-json"'
  do
    if grep -Fq "$marker" "$flake_file"; then
      ok "flake.nix contains $marker"
    else
      fail "flake.nix is missing expected iOS build marker: $marker"
    fi
  done
fi

if have_cmd nix; then
  ok "nix is installed: $(nix --version 2>/dev/null || printf unknown)"
elif [ "$allow_downloaded_artifacts" -eq 1 ] && artifact_fallback_ready; then
  warn "nix is not installed; downloaded iOS core artifacts are available for target $target"
else
  fail "nix is not installed; local iOS core builds cannot run on this machine"
fi

if [ "$allow_downloaded_artifacts" -eq 1 ]; then
  if artifact_fallback_ready; then
    ok "downloaded artifact fallback is available for target $target"
  else
    fail "downloaded artifact fallback is missing required pkg-ios-* artifacts for target $target"
  fi
fi

if [ -x "$root_dir/tools/bin/mac2ios" ]; then
  ok "local mac2ios helper is available: $root_dir/tools/bin/mac2ios"
elif have_cmd mac2ios; then
  ok "mac2ios is available on PATH: $(command -v mac2ios)"
else
  fail "mac2ios is not available; run scripts/ios/build-mac2ios.sh before preparing downloaded artifacts"
fi

xcode_select_path="$(/usr/bin/xcode-select -p 2>/dev/null || true)"
if [ -n "$xcode_select_path" ]; then
  info "xcode-select path: $xcode_select_path"
else
  warn "xcode-select path could not be read"
fi

if [ -d "$full_xcode_dir" ]; then
  if DEVELOPER_DIR="$full_xcode_dir" /usr/bin/xcodebuild -version >/dev/null 2>&1; then
    ok "full Xcode is available: $full_xcode_dir"
  else
    fail "full Xcode path exists but xcodebuild failed: $full_xcode_dir"
  fi

  if DEVELOPER_DIR="$full_xcode_dir" /usr/bin/xcrun --find simctl >/dev/null 2>&1; then
    ok "simctl is available when DEVELOPER_DIR points at full Xcode"
  else
    fail "simctl is not available even with DEVELOPER_DIR=$full_xcode_dir"
  fi

  if [ -d "$project_path" ]; then
    destinations="$(DEVELOPER_DIR="$full_xcode_dir" /usr/bin/xcodebuild -showdestinations -project "$project_path" -scheme "$scheme" 2>/dev/null || true)"
    sim_arches="$(printf '%s\n' "$destinations" | sed -nE 's/.*platform:iOS Simulator, arch:([^,}]+).*/\1/p' | sort -u | tr '\n' ' ' | sed 's/[[:space:]]*$//')"

    if [ -n "$sim_arches" ]; then
      ok "available iOS Simulator destination architecture(s): $sim_arches"
      if printf ' %s ' "$sim_arches" | grep -Fq " $required_sim_arch "; then
        ok "required simulator architecture is available: $required_sim_arch"
      else
        fail "required simulator architecture is not available: $required_sim_arch"
      fi

      if ! printf ' %s ' "$sim_arches" | grep -Fq " x86_64 "; then
        warn "no x86_64 iOS Simulator destination is available; x86_64 simulator core archives cannot be used for the current simulator workflow"
      fi
    else
      warn "could not parse iOS Simulator destination architectures for $scheme"
    fi
  else
    warn "project path not found for simulator destination audit: $project_path"
  fi
else
  fail "full Xcode developer directory is missing: $full_xcode_dir"
fi

if [ "$xcode_select_path" != "$full_xcode_dir" ]; then
  warn "xcode-select is not pointing at full Xcode; use DEVELOPER_DIR=$full_xcode_dir for iOS build commands"
fi

if free_gb="$(df -g "$root_dir" 2>/dev/null | awk 'NR == 2 {print $4 + 0}')"; then
  if [ -n "$free_gb" ] && [ "$free_gb" -gt 0 ]; then
    if [ "$free_gb" -lt "$min_free_gb" ]; then
      warn "free disk space is ${free_gb}GB; local Nix iOS core builds may need more than ${min_free_gb}GB"
    else
      ok "free disk space is ${free_gb}GB"
    fi
  else
    warn "free disk space could not be parsed"
  fi
else
  warn "free disk space could not be checked"
fi

cat <<EOF

Supported local build commands once nix is available:
  scripts/ios/run-real-core-batch0.sh --build-with-nix --output /tmp/nome-ios-real-core-batch0 --force

Then prepare the staged artifacts explicitly:
  scripts/ios/run-real-core-batch0.sh --source /tmp/nome-ios-real-core-batch0 --prepare
  scripts/ios/check-real-core.sh

This script is read-only. It does not install nix, run nix builds, or replace libraries.
EOF

exit "$status"
