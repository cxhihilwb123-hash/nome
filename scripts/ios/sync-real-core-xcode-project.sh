#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
project_file="${PROJECT_FILE:-$root_dir/apps/ios/SimpleX.xcodeproj/project.pbxproj}"
lib_dir="${LIB_DIR:-$root_dir/apps/ios/Libraries/sim}"
ios_lib_dir="${IOS_LIB_DIR:-$root_dir/apps/ios/Libraries/ios}"
check_only=0

usage() {
  cat <<'USAGE'
Usage:
  scripts/ios/sync-real-core-xcode-project.sh [--check]

Synchronizes the explicit libHSsimplex-chat archive references in the iOS
Xcode project with the real core archive filenames currently present in
apps/ios/Libraries/sim. This is needed after preparing real iOS core artifacts,
because Hydra/Nix library filenames include the core version and hash.

Options:
  --check   Verify the project already references the current library names.

Environment:
  PROJECT_FILE   Override the Xcode project.pbxproj path.
  LIB_DIR        Override the simulator library directory.
  IOS_LIB_DIR    Override the device library directory.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --check)
      check_only=1
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

require_file() {
  if [ ! -f "$1" ]; then
    echo "[FAIL] Missing file: $1" >&2
    exit 1
  fi
}

single_match() {
  local description="$1"
  shift
  local matches
  local count

  matches="$("$@" | sort -u)"
  count="$(printf '%s\n' "$matches" | sed '/^$/d' | wc -l | tr -d '[:space:]')"
  if [ "$count" -ne 1 ]; then
    echo "[FAIL] Expected exactly one $description, found $count" >&2
    printf '%s\n' "$matches" >&2
    exit 1
  fi
  printf '%s\n' "$matches"
}

require_file "$project_file"
if [ ! -d "$lib_dir" ]; then
  echo "[FAIL] Missing simulator library directory: $lib_dir" >&2
  exit 1
fi

new_ghc="$(single_match "simulator GHC libHS archive" find "$lib_dir" -maxdepth 1 -type f -name 'libHSsimplex-chat-*-ghc9.6.3.a' -exec basename '{}' ';')"
new_plain="$(single_match "simulator plain libHS archive" find "$lib_dir" -maxdepth 1 -type f -name 'libHSsimplex-chat-*.a' ! -name '*-ghc9.6.3.a' -exec basename '{}' ';')"

if [ -d "$ios_lib_dir" ]; then
  require_file "$ios_lib_dir/$new_ghc"
  require_file "$ios_lib_dir/$new_plain"
fi

old_ghc="$(single_match "project GHC libHS reference" grep -Eoh 'libHSsimplex-chat-[^ ";/]+-ghc9\.6\.3\.a' "$project_file")"
old_plain="$(single_match "project plain libHS reference" sh -c "grep -Eoh 'libHSsimplex-chat-[^ \" ;/]+\\.a' \"\$1\" | grep -v -- '-ghc9\\.6\\.3\\.a'" sh "$project_file")"

if [ "$old_ghc" = "$new_ghc" ] && [ "$old_plain" = "$new_plain" ]; then
  echo "[PASS] Xcode project already references current real-core archives"
  echo "[INFO] $new_ghc"
  echo "[INFO] $new_plain"
  exit 0
fi

if [ "$check_only" -eq 1 ]; then
  echo "[FAIL] Xcode project library references are out of sync" >&2
  echo "[INFO] project GHC: $old_ghc" >&2
  echo "[INFO] current GHC: $new_ghc" >&2
  echo "[INFO] project plain: $old_plain" >&2
  echo "[INFO] current plain: $new_plain" >&2
  exit 1
fi

OLD_GHC="$old_ghc" NEW_GHC="$new_ghc" OLD_PLAIN="$old_plain" NEW_PLAIN="$new_plain" \
  perl -0pi -e 's/\Q$ENV{OLD_GHC}\E/$ENV{NEW_GHC}/g; s/\Q$ENV{OLD_PLAIN}\E/$ENV{NEW_PLAIN}/g' "$project_file"

echo "[PASS] Updated Xcode project real-core archive references"
echo "[INFO] $old_ghc -> $new_ghc"
echo "[INFO] $old_plain -> $new_plain"
