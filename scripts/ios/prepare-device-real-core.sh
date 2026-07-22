#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
source_dir="${SOURCE_DIR:-$HOME/Downloads/pkg-ios-aarch64-swift-json}"
target_dir="${TARGET_DIR:-$root_dir/apps/ios/Libraries/ios}"
project_file="${PROJECT_FILE:-$root_dir/apps/ios/SimpleX.xcodeproj/project.pbxproj}"
min_core_bytes="${MIN_REAL_CORE_LIB_BYTES:-1000000}"
prepare=0
force=0
convert_darwin=0
mac2ios_bin="${MAC2IOS:-}"
staging_dir=""

usage() {
  cat <<'USAGE'
Usage:
  scripts/ios/prepare-device-real-core.sh [options]

Audits an arm64 iOS device core artifact and, only with --prepare, installs it
into apps/ios/Libraries/ios. This is the physical-device fallback path when the
current Apple Silicon simulator still lacks a compatible arm64 simulator core.

By default the script is read-only. It never modifies apps/ios/Libraries/sim.

Options:
  --source DIR    Device artifact directory. Default:
                  ~/Downloads/pkg-ios-aarch64-swift-json
  --target DIR    Device library target. Default:
                  apps/ios/Libraries/ios
  --prepare       Copy the audited device libraries into --target.
  --convert-darwin
                  Convert a Darwin arm64 archive set to IOS platform metadata
                  in temporary staging before auditing or installing it.
  --force         Allow replacing a non-empty --target. A backup is written to
                  /tmp before replacement.
  -h, --help      Show this help.

Environment:
  SOURCE_DIR                  Override the default source directory.
  TARGET_DIR                  Override the default target directory.
  PROJECT_FILE                Override the Xcode project file used for warnings.
  MIN_REAL_CORE_LIB_BYTES     Override the production-size threshold.
  MAC2IOS                     Override the mac2ios executable used with
                              --convert-darwin.
USAGE
}

ok() {
  printf '[OK] %s\n' "$1"
}

warn() {
  printf '[WARN] %s\n' "$1"
}

fail() {
  printf '[FAIL] %s\n' "$1" >&2
  exit 1
}

info() {
  printf '[INFO] %s\n' "$1"
}

have_cmd() {
  command -v "$1" >/dev/null 2>&1
}

single_match() {
  local description="$1"
  shift
  local matches
  local count

  matches="$("$@" | sort -u)"
  count="$(printf '%s\n' "$matches" | sed '/^$/d' | wc -l | tr -d '[:space:]')"
  if [ "$count" -ne 1 ]; then
    printf '%s\n' "$matches" >&2
    fail "Expected exactly one $description, found $count"
  fi

  printf '%s\n' "$matches"
}

library_archs() {
  local lib="$1"
  local output

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

assert_safe_target() {
  case "$target_dir" in
    ""|"/")
      fail "Refusing unsafe target directory: ${target_dir:-<empty>}"
      ;;
  esac
}

cleanup() {
  if [ -n "$staging_dir" ] && [ -d "$staging_dir" ]; then
    rm -rf "$staging_dir"
  fi
}

trap cleanup EXIT

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

library_platforms() {
  local lib="$1"

  otool -l "$lib" 2>/dev/null |
    awk '$1 == "platform" { print $2 }' |
    sort -u
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --source)
      shift
      if [ "$#" -eq 0 ]; then
        fail "--source requires a directory"
      fi
      source_dir="$1"
      ;;
    --target)
      shift
      if [ "$#" -eq 0 ]; then
        fail "--target requires a directory"
      fi
      target_dir="$1"
      ;;
    --prepare)
      prepare=1
      ;;
    --convert-darwin)
      convert_darwin=1
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

cat <<'HEADER'
Nome iOS device real-core prepare
=================================
HEADER

info "Source: $source_dir"
info "Target: $target_dir"
info "Prepare: $prepare"
info "Convert Darwin archives: $convert_darwin"

assert_safe_target

if ! have_cmd lipo; then
  fail "lipo is required to verify static library architecture"
fi
if ! have_cmd otool; then
  fail "otool is required to verify static library platform metadata"
fi

if [ ! -d "$source_dir" ]; then
  fail "Source directory is missing: $source_dir"
fi

ghc_lib="$(single_match "device GHC libHS archive" find "$source_dir" -maxdepth 1 -type f -name 'libHSsimplex-chat-*-ghc*.a' -exec basename '{}' ';')"
plain_lib="$(single_match "device plain libHS archive" find "$source_dir" -maxdepth 1 -type f -name 'libHSsimplex-chat-*.a' ! -name '*-ghc*.a' -exec basename '{}' ';')"

for required in "$ghc_lib" "$plain_lib" libffi.a libgmp.a libgmpxx.a; do
  if [ ! -f "$source_dir/$required" ]; then
    fail "Required device library is missing: $source_dir/$required"
  fi
  ok "Found required device library: $required"
done

audit_dir="$source_dir"
if [ "$convert_darwin" -eq 1 ]; then
  if ! find_mac2ios; then
    fail "--convert-darwin requires mac2ios via MAC2IOS, PATH, or tools/bin/mac2ios"
  fi

  staging_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-ios-device-core.XXXXXX")"
  cp -R "$source_dir"/. "$staging_dir"/
  chmod u+w "$staging_dir"/*.a
  while IFS= read -r lib; do
    "$mac2ios_bin" "$lib" >/dev/null
    ok "Converted device library to IOS platform metadata: $(basename "$lib")"
  done < <(find "$staging_dir" -maxdepth 1 -type f -name '*.a' | sort)
  audit_dir="$staging_dir"
fi

for core_lib in "$audit_dir/$ghc_lib" "$audit_dir/$plain_lib"; do
  core_size="$(wc -c < "$core_lib" | tr -d ' ')"
  if [ "$core_size" -lt "$min_core_bytes" ]; then
    fail "Device core library is too small for production use: $(basename "$core_lib") is ${core_size} bytes"
  fi
  ok "Device core library size looks production-like: $(basename "$core_lib") is ${core_size} bytes"

  if strings "$core_lib" 2>/dev/null | grep -Eq 'preview-agent|preview-token|simplex:/contact#preview|nome\.local/preview'; then
    fail "Device core library contains preview-core markers: $(basename "$core_lib")"
  fi
  ok "Device core library has no preview-core markers: $(basename "$core_lib")"
done

checked=0
while IFS= read -r lib; do
  checked=$((checked + 1))
  if ! archs="$(library_archs "$lib")"; then
    fail "Could not inspect device library architecture: $(basename "$lib")"
  fi

  if arch_list_contains "$archs" "arm64"; then
    ok "Device library supports arm64: $(basename "$lib") [$archs]"
  else
    fail "Device library does not support arm64: $(basename "$lib") [$archs]"
  fi

  platforms="$(library_platforms "$lib")"
  if [ "$platforms" = "2" ] || [ "$platforms" = "IOS" ]; then
    ok "Device library uses IOS platform metadata: $(basename "$lib") [$platforms]"
  elif [ -z "$platforms" ]; then
    fail "Could not inspect device library platform metadata: $(basename "$lib")"
  else
    fail "Device library platform metadata mismatch: $(basename "$lib") has [$platforms], requires IOS [2]. Use --convert-darwin for official Darwin archives."
  fi
done < <(find "$audit_dir" -maxdepth 1 -type f -name '*.a' | sort)

if [ "$checked" -eq 0 ]; then
  fail "No static .a libraries found in source directory: $source_dir"
fi

if [ -f "$project_file" ]; then
  project_ghc="$(grep -Eoh 'libHSsimplex-chat-[^ ";/]+-ghc[^ ";/]+\.a' "$project_file" | sort -u | head -1 || true)"
  project_plain="$(grep -Eoh 'libHSsimplex-chat-[^ ";/]+\.a' "$project_file" | grep -v -- '-ghc[^ ";/]*\.a' | sort -u | head -1 || true)"

  if [ -n "$project_ghc" ] && [ "$project_ghc" != "$ghc_lib" ]; then
    warn "Device GHC archive name differs from current Xcode project reference: $ghc_lib vs $project_ghc"
  fi
  if [ -n "$project_plain" ] && [ "$project_plain" != "$plain_lib" ]; then
    warn "Device plain archive name differs from current Xcode project reference: $plain_lib vs $project_plain"
  fi
  if { [ -n "$project_ghc" ] && [ "$project_ghc" != "$ghc_lib" ]; } || { [ -n "$project_plain" ] && [ "$project_plain" != "$plain_lib" ]; }; then
    warn "Before a physical-device build, use a matching simulator/device core pair or sync the Xcode project references with a deliberate release decision."
  fi
fi

if [ "$prepare" -ne 1 ]; then
  convert_hint=""
  if [ "$convert_darwin" -eq 1 ]; then
    convert_hint=" --convert-darwin"
  fi
  cat <<EOF

[PASS] Device real-core artifact is ready for explicit install
[INFO] Dry run only. No files were copied.
[INFO] To install the device libraries:
  scripts/ios/prepare-device-real-core.sh --source "$source_dir"$convert_hint --prepare
EOF
  exit 0
fi

if [ -d "$target_dir" ] && [ "$(find "$target_dir" -mindepth 1 -maxdepth 1 | wc -l | tr -d '[:space:]')" -gt 0 ]; then
  if [ "$force" -ne 1 ]; then
    fail "Target directory is not empty: $target_dir. Re-run with --force to replace it after backup."
  fi

  backup_dir="/tmp/nome-ios-device-libs-backup-$(date +%Y%m%d-%H%M%S)"
  cp -R "$target_dir" "$backup_dir"
  ok "Backed up existing device libraries to: $backup_dir"
fi

rm -rf "$target_dir"
mkdir -p "$target_dir"
cp -R "$audit_dir"/. "$target_dir"/

ok "Installed device real-core libraries into: $target_dir"
info "Simulator libraries were not modified: $root_dir/apps/ios/Libraries/sim"
info "Re-run scripts/ios/check-ios-device-readiness.sh before a physical-device test."
