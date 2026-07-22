#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
project_file="${PROJECT_FILE:-$root_dir/apps/ios/SimpleX.xcodeproj/project.pbxproj}"
ios_lib_dir="${IOS_LIB_DIR:-$root_dir/apps/ios/Libraries/ios}"
min_core_bytes="${MIN_REAL_CORE_LIB_BYTES:-1000000}"
prepare=0
force=0

usage() {
  cat <<'USAGE'
Usage:
  scripts/ios/alias-device-real-core-project-libs.sh [options]

Creates local, device-only compatibility aliases in apps/ios/Libraries/ios so
the current Xcode project archive names can resolve to an installed arm64
device core artifact with different archive filenames.

By default this script is read-only. It never modifies project.pbxproj or
apps/ios/Libraries/sim.

Options:
  --prepare   Create missing project-name symlinks in apps/ios/Libraries/ios.
  --force     Replace existing project-name files/symlinks after validation.
  -h, --help  Show this help.

Environment:
  PROJECT_FILE  Override the Xcode project.pbxproj path.
  IOS_LIB_DIR   Override the installed iOS device library directory.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --prepare)
      prepare=1
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

fail() {
  printf '[FAIL] %s\n' "$1" >&2
  exit 1
}

ok() {
  printf '[OK] %s\n' "$1"
}

info() {
  printf '[INFO] %s\n' "$1"
}

single_match() {
  local description="$1"
  shift
  local matches
  local count

  matches="$("$@" | sort -u)"
  count="$(printf '%s\n' "$matches" | sed '/^$/d' | wc -l | tr -d '[:space:]')"
  if [ "$count" -ne 1 ]; then
    printf '%s\n' "$matches" | sed '/^$/d;s/^/[INFO] candidate /' >&2
    fail "Expected exactly one $description, found $count"
  fi

  printf '%s\n' "$matches"
}

project_ref() {
  local kind="$1"

  case "$kind" in
    ghc)
      single_match "project GHC libHS reference" \
        grep -Eoh 'libHSsimplex-chat-[^ ";/]+-ghc[^ ";/]+\.a' "$project_file"
      ;;
    plain)
      single_match "project plain libHS reference" \
        sh -c "grep -Eoh 'libHSsimplex-chat-[^ \" ;/]+\\.a' \"\$1\" | grep -v -- '-ghc[^ \";/]*\\.a'" sh "$project_file"
      ;;
    *)
      fail "Unknown project ref kind: $kind"
      ;;
  esac
}

installed_lib() {
  local kind="$1"
  local project_name="$2"

  if [ -f "$ios_lib_dir/$project_name" ]; then
    printf '%s\n' "$project_name"
    return
  fi

  case "$kind" in
    ghc)
      single_match "installed device GHC libHS archive that is not already the project alias" \
        find "$ios_lib_dir" -maxdepth 1 -type f -name 'libHSsimplex-chat-*-ghc*.a' ! -name "$project_name" -exec basename '{}' ';'
      ;;
    plain)
      single_match "installed device plain libHS archive that is not already the project alias" \
        find "$ios_lib_dir" -maxdepth 1 -type f -name 'libHSsimplex-chat-*.a' ! -name '*-ghc*.a' ! -name "$project_name" -exec basename '{}' ';'
      ;;
    *)
      fail "Unknown installed lib kind: $kind"
      ;;
  esac
}

library_archs() {
  local lib="$1"
  local output

  output="$(lipo -info "$lib" 2>/dev/null || true)"
  if [ -z "$output" ]; then
    return 1
  fi

  case "$output" in
    *" are: "*) printf '%s\n' "${output##* are: }" ;;
    *" is architecture: "*) printf '%s\n' "${output##* is architecture: }" ;;
    *) return 1 ;;
  esac
}

verify_source() {
  local path="$1"
  local archs
  local bytes
  local platforms

  if [ ! -f "$path" ]; then
    fail "Missing installed device library: $path"
  fi

  if ! archs="$(library_archs "$path")"; then
    fail "Could not inspect library architecture: $path"
  fi
  if ! printf ' %s ' "$archs" | grep -Fq ' arm64 '; then
    fail "Installed device library does not support arm64: $(basename "$path") [$archs]"
  fi

  bytes="$(wc -c < "$path" | tr -d ' ')"
  if [ "$bytes" -lt "$min_core_bytes" ]; then
    fail "Installed device library is too small for a real core alias: $(basename "$path") is ${bytes} bytes"
  fi

  if strings "$path" 2>/dev/null | grep -Eq 'preview-agent|preview-token|simplex:/contact#preview|nome\.local/preview'; then
    fail "Installed device library contains preview-core markers: $(basename "$path")"
  fi

  if ! command -v otool >/dev/null 2>&1; then
    fail "otool is required to verify installed device library platform metadata"
  fi
  platforms="$(otool -l "$path" 2>/dev/null | awk '$1 == "platform" { print $2 }' | sort -u)"
  if [ "$platforms" != "2" ] && [ "$platforms" != "IOS" ]; then
    fail "Installed device library must use IOS platform metadata: $(basename "$path") has [${platforms:-unknown}]"
  fi
}

ensure_alias() {
  local label="$1"
  local project_name="$2"
  local source_name="$3"
  local target="$ios_lib_dir/$project_name"
  local source="$source_name"

  verify_source "$ios_lib_dir/$source_name"

  if [ "$project_name" = "$source_name" ]; then
    ok "$label already uses the project archive name: $project_name"
    return
  fi

  if [ -L "$target" ]; then
    current_target="$(readlink "$target")"
    if [ "$current_target" = "$source" ]; then
      ok "$label alias already exists: $project_name -> $source"
      return
    fi
    if [ "$force" -ne 1 ]; then
      fail "$label alias points elsewhere: $project_name -> $current_target. Re-run with --force to replace it."
    fi
  elif [ -e "$target" ]; then
    if [ "$force" -ne 1 ]; then
      fail "$label project-name library already exists and is not a symlink: $target. Re-run with --force to replace it."
    fi
  fi

  if [ "$prepare" -ne 1 ]; then
    info "$label alias needed: $project_name -> $source"
    return
  fi

  rm -f "$target"
  (cd "$ios_lib_dir" && ln -s "$source" "$project_name")
  ok "Created $label alias: $project_name -> $source"
}

cat <<'HEADER'
Nome iOS device real-core project-name aliases
==============================================
HEADER

info "Project: $project_file"
info "Device libraries: $ios_lib_dir"
info "Prepare: $prepare"

if [ ! -f "$project_file" ]; then
  fail "Missing project file: $project_file"
fi
if [ ! -d "$ios_lib_dir" ]; then
  fail "Missing installed device library directory: $ios_lib_dir"
fi

project_ghc="$(project_ref ghc)"
project_plain="$(project_ref plain)"
source_ghc="$(installed_lib ghc "$project_ghc")"
source_plain="$(installed_lib plain "$project_plain")"

info "Project GHC archive: $project_ghc"
info "Installed GHC archive: $source_ghc"
info "Project plain archive: $project_plain"
info "Installed plain archive: $source_plain"

ensure_alias "GHC" "$project_ghc" "$source_ghc"
ensure_alias "plain" "$project_plain" "$source_plain"

if [ "$prepare" -eq 1 ]; then
  echo "[PASS] Device project-name aliases are prepared"
else
  echo "[PASS] Device project-name alias check completed"
  echo "[INFO] Dry run only. Re-run with --prepare to create aliases."
fi
