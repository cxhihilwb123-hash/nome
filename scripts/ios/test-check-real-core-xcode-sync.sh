#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/check-real-core-xcode-sync.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-check-real-core-xcode-sync-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

make_project() {
  local file="$1"
  local stem="$2"
  local ghc_version="${3:-9.6.3}"

  cat > "$file" <<EOF
64C8299F /* libHSsimplex-chat-$stem-ghc$ghc_version.a in Frameworks */;
64C829A0 /* libHSsimplex-chat-$stem.a in Frameworks */;
64C8299A /* libHSsimplex-chat-$stem-ghc$ghc_version.a */;
64C8299B /* libHSsimplex-chat-$stem.a */;
EOF
}

make_lib_pair() {
  local dir="$1"
  local stem="$2"
  local ghc_version="${3:-9.6.3}"

  mkdir -p "$dir"
  printf 'ghc\n' > "$dir/libHSsimplex-chat-$stem-ghc$ghc_version.a"
  printf 'plain\n' > "$dir/libHSsimplex-chat-$stem.a"
}

run_case() {
  local name="$1"
  local expected_rc="$2"
  local expected_text="$3"
  shift 3
  local log="$work_dir/$name.log"
  local rc=0

  "$script" "$@" > "$log" 2>&1 || rc=$?
  if [ "$rc" -ne "$expected_rc" ]; then
    sed -n '1,180p' "$log" >&2
    fail "$name expected exit $expected_rc, got $rc"
  fi

  if ! grep -Fq "$expected_text" "$log"; then
    sed -n '1,180p' "$log" >&2
    fail "$name missing expected text: $expected_text"
  fi

  pass "$name"
}

project="$work_dir/project.pbxproj"
make_project "$project" "project"

matching_sim="$work_dir/matching-sim"
matching_ios="$work_dir/matching-ios"
source_mismatch="$work_dir/source-mismatch"
make_lib_pair "$matching_sim" "project"
make_lib_pair "$matching_ios" "project"
make_lib_pair "$source_mismatch" "candidate"

run_case \
  "candidate_mismatch_warns_but_passes" \
  0 \
  "Candidate device source mismatch is advisory until installed" \
  --project "$project" \
  --sim-dir "$matching_sim" \
  --ios-dir "$matching_ios" \
  --device-source "$source_mismatch"

mismatched_sim="$work_dir/mismatched-sim"
make_lib_pair "$mismatched_sim" "other"
run_case \
  "simulator_mismatch_fails" \
  1 \
  "Simulator GHC archive differs from project reference" \
  --project "$project" \
  --sim-dir "$mismatched_sim" \
  --ios-dir "$matching_ios" \
  --device-source "$source_mismatch"

mismatched_ios="$work_dir/mismatched-ios"
make_lib_pair "$mismatched_ios" "other"
run_case \
  "installed_device_mismatch_fails" \
  1 \
  "Installed device GHC archive differs from project reference" \
  --project "$project" \
  --sim-dir "$matching_sim" \
  --ios-dir "$mismatched_ios" \
  --device-source "$source_mismatch"

aliased_ios="$work_dir/aliased-ios"
make_lib_pair "$aliased_ios" "other"
ln -s "libHSsimplex-chat-other-ghc9.6.3.a" "$aliased_ios/libHSsimplex-chat-project-ghc9.6.3.a"
ln -s "libHSsimplex-chat-other.a" "$aliased_ios/libHSsimplex-chat-project.a"
run_case \
  "installed_device_alias_passes" \
  0 \
  "Installed device GHC archive is a local compatibility alias" \
  --project "$project" \
  --sim-dir "$matching_sim" \
  --ios-dir "$aliased_ios" \
  --device-source "$source_mismatch"

run_case \
  "missing_installed_device_warns" \
  0 \
  "Installed device library directory is missing" \
  --project "$project" \
  --sim-dir "$matching_sim" \
  --ios-dir "$work_dir/missing-ios" \
  --device-source "$source_mismatch"

ghc810_project="$work_dir/project-ghc810.pbxproj"
ghc810_sim="$work_dir/matching-sim-ghc810"
ghc810_ios="$work_dir/matching-ios-ghc810"
make_project "$ghc810_project" "project810" "8.10.7"
make_lib_pair "$ghc810_sim" "project810" "8.10.7"
make_lib_pair "$ghc810_ios" "project810" "8.10.7"
run_case \
  "ghc_8_10_7_pair_passes" \
  0 \
  "Xcode real-core references are consistent with installed build paths" \
  --project "$ghc810_project" \
  --sim-dir "$ghc810_sim" \
  --ios-dir "$ghc810_ios" \
  --device-source "$ghc810_ios"

echo "[PASS] check-real-core-xcode-sync tests passed"
