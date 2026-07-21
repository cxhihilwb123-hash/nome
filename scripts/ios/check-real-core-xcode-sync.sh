#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
project_file="${PROJECT_FILE:-$root_dir/apps/ios/SimpleX.xcodeproj/project.pbxproj}"
sim_lib_dir="${SIM_LIB_DIR:-$root_dir/apps/ios/Libraries/sim}"
ios_lib_dir="${IOS_LIB_DIR:-$root_dir/apps/ios/Libraries/ios}"
device_source_dir="${DEVICE_SOURCE_DIR:-$HOME/Downloads/pkg-ios-aarch64-swift-json}"
status=0

usage() {
  cat <<'USAGE'
Usage:
  scripts/ios/check-real-core-xcode-sync.sh [options]

Checks whether the Xcode project libHSsimplex-chat archive references match the
libraries that are actually available for the current iOS build paths. This is
read-only and does not update project.pbxproj.

Options:
  --project FILE        Xcode project.pbxproj file.
  --sim-dir DIR         Simulator library directory.
  --ios-dir DIR         Installed iOS device library directory.
  --device-source DIR   Candidate arm64 device artifact directory.
  -h, --help            Show this help.

Environment:
  PROJECT_FILE          Override the project file.
  SIM_LIB_DIR           Override the simulator library directory.
  IOS_LIB_DIR           Override the installed device library directory.
  DEVICE_SOURCE_DIR     Override the candidate device artifact directory.
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

single_match() {
  local description="$1"
  shift
  local matches
  local count

  matches="$("$@" | sort -u)"
  count="$(printf '%s\n' "$matches" | sed '/^$/d' | wc -l | tr -d '[:space:]')"
  if [ "$count" -ne 1 ]; then
    printf '[FAIL] Expected exactly one %s, found %s\n' "$description" "$count"
    printf '%s\n' "$matches" | sed '/^$/d;s/^/[INFO] candidate /'
    status=1
    return 1
  fi

  printf '%s\n' "$matches"
}

plain_lib_name_from_dir() {
  local dir="$1"
  single_match "plain libHS archive in $dir" \
    find "$dir" -maxdepth 1 -type f -name 'libHSsimplex-chat-*.a' ! -name '*-ghc9.6.3.a' -exec basename '{}' ';'
}

ghc_lib_name_from_dir() {
  local dir="$1"
  single_match "GHC libHS archive in $dir" \
    find "$dir" -maxdepth 1 -type f -name 'libHSsimplex-chat-*-ghc9.6.3.a' -exec basename '{}' ';'
}

compare_pair() {
  local label="$1"
  local ghc="$2"
  local plain="$3"
  local required_ghc="$4"
  local required_plain="$5"
  local failure_mode="${6:-fail}"
  local mismatch=0

  if [ "$ghc" = "$required_ghc" ]; then
    ok "$label GHC archive matches project reference: $ghc"
  else
    mismatch=1
    if [ "$failure_mode" = "warn" ]; then
      warn "$label GHC archive differs from project reference: $ghc vs $required_ghc"
    else
      fail "$label GHC archive differs from project reference: $ghc vs $required_ghc"
    fi
  fi

  if [ "$plain" = "$required_plain" ]; then
    ok "$label plain archive matches project reference: $plain"
  else
    mismatch=1
    if [ "$failure_mode" = "warn" ]; then
      warn "$label plain archive differs from project reference: $plain vs $required_plain"
    else
      fail "$label plain archive differs from project reference: $plain vs $required_plain"
    fi
  fi

  return "$mismatch"
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --project)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --project requires a file" >&2
        exit 2
      fi
      project_file="$1"
      ;;
    --sim-dir)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --sim-dir requires a directory" >&2
        exit 2
      fi
      sim_lib_dir="$1"
      ;;
    --ios-dir)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --ios-dir requires a directory" >&2
        exit 2
      fi
      ios_lib_dir="$1"
      ;;
    --device-source)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --device-source requires a directory" >&2
        exit 2
      fi
      device_source_dir="$1"
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
Nome iOS Xcode real-core reference check
=======================================
HEADER

info "Project: $project_file"
info "Simulator libraries: $sim_lib_dir"
info "Installed device libraries: $ios_lib_dir"
info "Candidate device source: $device_source_dir"

if [ ! -f "$project_file" ]; then
  fail "Missing Xcode project file: $project_file"
  exit "$status"
fi

project_ghc="$(single_match "project GHC libHS reference" grep -Eoh 'libHSsimplex-chat-[^ ";/]+-ghc9\.6\.3\.a' "$project_file" || true)"
project_plain="$(single_match "project plain libHS reference" sh -c "grep -Eoh 'libHSsimplex-chat-[^ \" ;/]+\\.a' \"\$1\" | grep -v -- '-ghc9\\.6\\.3\\.a'" sh "$project_file" || true)"

if [ -z "$project_ghc" ] || [ -z "$project_plain" ]; then
  exit "$status"
fi

ok "Project GHC archive reference: $project_ghc"
ok "Project plain archive reference: $project_plain"

if [ -d "$sim_lib_dir" ]; then
  sim_ghc="$(ghc_lib_name_from_dir "$sim_lib_dir" || true)"
  sim_plain="$(plain_lib_name_from_dir "$sim_lib_dir" || true)"
  if [ -n "$sim_ghc" ] && [ -n "$sim_plain" ]; then
    compare_pair "Simulator" "$sim_ghc" "$sim_plain" "$project_ghc" "$project_plain" || true
  fi
else
  fail "Missing simulator library directory: $sim_lib_dir"
fi

if [ -d "$ios_lib_dir" ]; then
  if [ -f "$ios_lib_dir/$project_ghc" ] && [ -f "$ios_lib_dir/$project_plain" ]; then
    ok "Installed device GHC archive matches project reference: $project_ghc"
    ok "Installed device plain archive matches project reference: $project_plain"

    if [ -L "$ios_lib_dir/$project_ghc" ]; then
      info "Installed device GHC archive is a local compatibility alias: $project_ghc -> $(readlink "$ios_lib_dir/$project_ghc")"
    fi
    if [ -L "$ios_lib_dir/$project_plain" ]; then
      info "Installed device plain archive is a local compatibility alias: $project_plain -> $(readlink "$ios_lib_dir/$project_plain")"
    fi
  else
    ios_ghc="$(ghc_lib_name_from_dir "$ios_lib_dir" || true)"
    ios_plain="$(plain_lib_name_from_dir "$ios_lib_dir" || true)"
    if [ -n "$ios_ghc" ] && [ -n "$ios_plain" ]; then
      compare_pair "Installed device" "$ios_ghc" "$ios_plain" "$project_ghc" "$project_plain" || true
    fi
  fi
else
  warn "Installed device library directory is missing: $ios_lib_dir"
fi

if [ -d "$device_source_dir" ]; then
  source_ghc="$(ghc_lib_name_from_dir "$device_source_dir" || true)"
  source_plain="$(plain_lib_name_from_dir "$device_source_dir" || true)"
  if [ -n "$source_ghc" ] && [ -n "$source_plain" ]; then
    compare_pair "Candidate device source" "$source_ghc" "$source_plain" "$project_ghc" "$project_plain" warn || true
    if [ "$source_ghc" != "$project_ghc" ] || [ "$source_plain" != "$project_plain" ]; then
      warn "Candidate device source mismatch is advisory until installed; sync or choose a matching pair before a device build."
    fi
  fi
else
  warn "Candidate device source directory is missing: $device_source_dir"
fi

if [ "$status" -eq 0 ]; then
  echo "[PASS] Xcode real-core references are consistent with installed build paths"
fi

exit "$status"
