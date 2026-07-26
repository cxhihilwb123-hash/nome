#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
developer_dir="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
project_path="$root_dir/apps/ios/SimpleX.xcodeproj"
scheme="SimpleX (iOS)"
configuration="Debug"
sim_lib_dir="$root_dir/apps/ios/Libraries/sim"
x86_dir="$HOME/Downloads/pkg-ios-x86_64-swift-json"
output_dir="/tmp/nome-ios-x86_64-sim-real-core-probe-$(date +%Y%m%d-%H%M%S)"
destination="generic/platform=iOS Simulator"
simulator_id=""
force=0
skip_build=0
install_launch=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/probe-x86_64-simulator-real-core.sh [options]

Temporarily swaps apps/ios/Libraries/sim to the local x86_64 real-core
artifact, attempts an x86_64 iOS Simulator build, and restores the original
simulator libraries before exiting.

This is a reversible probe for the Apple Silicon case where the local real
simulator core is x86_64 but Xcode reports arm64 simulator destinations.

Options:
  --x86-dir DIR         Directory containing pkg-ios-x86_64-swift-json files.
  --sim-lib-dir DIR     Simulator library directory to temporarily swap.
  --output DIR          Output directory for logs and build products.
  --project PATH        Xcode project path.
  --scheme NAME         Xcode scheme.
  --configuration NAME  Xcode configuration. Default: Debug.
  --destination SPEC    xcodebuild destination. Default: generic/platform=iOS Simulator.
  --simulator UUID      Simulator UUID for optional install/launch.
  --install-launch      After a successful build, try simctl install + launch.
  --skip-build          Only exercise temporary swap and restore. For tests.
  --force              Replace an existing output directory.
  -h, --help            Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --x86-dir)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --x86-dir requires a directory" >&2
        exit 2
      fi
      x86_dir="$1"
      ;;
    --sim-lib-dir)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --sim-lib-dir requires a directory" >&2
        exit 2
      fi
      sim_lib_dir="$1"
      ;;
    --output)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --output requires a directory" >&2
        exit 2
      fi
      output_dir="$1"
      ;;
    --project)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --project requires a path" >&2
        exit 2
      fi
      project_path="$1"
      ;;
    --scheme)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --scheme requires a name" >&2
        exit 2
      fi
      scheme="$1"
      ;;
    --configuration)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --configuration requires a name" >&2
        exit 2
      fi
      configuration="$1"
      ;;
    --destination)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --destination requires a spec" >&2
        exit 2
      fi
      destination="$1"
      ;;
    --simulator)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --simulator requires a UUID" >&2
        exit 2
      fi
      simulator_id="$1"
      ;;
    --install-launch)
      install_launch=1
      ;;
    --skip-build)
      skip_build=1
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

if [ -e "$output_dir" ]; then
  if [ "$force" -ne 1 ]; then
    echo "[FAIL] Output already exists: $output_dir" >&2
    echo "[INFO] Re-run with --force to replace it." >&2
    exit 1
  fi
  rm -rf "$output_dir"
fi

mkdir -p "$output_dir/logs"

summary="$output_dir/summary.tsv"
original_sha="$output_dir/original_sim_sha256.tsv"
temporary_sha="$output_dir/temporary_x86_sim_sha256.tsv"
restored_sha="$output_dir/restored_sim_sha256.tsv"
backup_parent="$(mktemp -d "${TMPDIR:-/tmp}/nome-x86-real-core-probe-backup.XXXXXX")"
backup_dir="$backup_parent/sim"
restored=0
restore_status="NOT_RUN"

cleanup() {
  if [ "$restored" -eq 0 ] && [ -d "$backup_dir" ]; then
    rm -rf "$sim_lib_dir"
    mkdir -p "$(dirname "$sim_lib_dir")"
    cp -R "$backup_dir" "$sim_lib_dir"
    restored=1
  fi

  rm -rf "$backup_parent"
}
trap cleanup EXIT INT TERM

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

first_file() {
  find "$1" -maxdepth 1 -type f -name "$2" 2>/dev/null | sort | head -1
}

write_sha_table() {
  local dir="$1"
  local out="$2"

  printf 'file\tsha256\tbytes\n' > "$out"
  find "$dir" -maxdepth 1 -type f -name '*.a' -print 2>/dev/null | sort |
  while IFS= read -r file; do
    printf '%s\t%s\t%s\n' \
      "$(basename "$file")" \
      "$(shasum -a 256 "$file" | awk '{print $1}')" \
      "$(wc -c < "$file" | tr -d '[:space:]')" >> "$out"
  done
}

copy_source_to_target_name() {
  local source="$1"
  local target="$2"

  [ -n "$source" ] || fail "Missing source library for $(basename "$target")"
  [ -f "$source" ] || fail "Source library is not a file: $source"
  cp "$source" "$target"
  echo "[OK] x86_64 source $(basename "$source") -> $(basename "$target")"
}

restore_now() {
  cleanup

  if cmp -s "$original_sha" <(write_sha_table "$sim_lib_dir" /dev/stdout); then
    restore_status="PASS"
  else
    write_sha_table "$sim_lib_dir" "$restored_sha"
    if diff -u "$original_sha" "$restored_sha" > "$output_dir/logs/restore_diff.log" 2>&1; then
      restore_status="PASS"
    else
      restore_status="FAIL"
    fi
  fi
}

if [ ! -d "$sim_lib_dir" ]; then
  fail "Simulator library directory not found: $sim_lib_dir"
fi

if [ ! -d "$x86_dir" ]; then
  fail "x86_64 real-core artifact directory not found: $x86_dir"
fi

target_ghc="$(first_file "$sim_lib_dir" 'libHSsimplex-chat*-ghc*.a')"
target_plain="$(find "$sim_lib_dir" -maxdepth 1 -type f -name 'libHSsimplex-chat*.a' ! -name '*-ghc*.a' 2>/dev/null | sort | head -1)"
source_ghc="$(first_file "$x86_dir" 'libHSsimplex-chat*-ghc*.a')"
source_plain="$(find "$x86_dir" -maxdepth 1 -type f -name 'libHSsimplex-chat*.a' ! -name '*-ghc*.a' 2>/dev/null | sort | head -1)"

[ -n "$target_ghc" ] || fail "Missing target libHSsimplex-chat*-ghc*.a in $sim_lib_dir"
[ -n "$target_plain" ] || fail "Missing target non-ghc libHSsimplex-chat*.a in $sim_lib_dir"
[ -n "$source_ghc" ] || fail "Missing source libHSsimplex-chat*-ghc*.a in $x86_dir"
[ -n "$source_plain" ] || fail "Missing source non-ghc libHSsimplex-chat*.a in $x86_dir"

for lib in libffi.a libgmp.a libgmpxx.a; do
  [ -f "$sim_lib_dir/$lib" ] || fail "Missing target $lib in $sim_lib_dir"
  [ -f "$x86_dir/$lib" ] || fail "Missing source $lib in $x86_dir"
done

write_sha_table "$sim_lib_dir" "$original_sha"
cp -R "$sim_lib_dir" "$backup_dir"

cat <<EOF
Nome iOS x86_64 simulator real-core probe
=========================================
[INFO] Output: $output_dir
[INFO] x86 source: $x86_dir
[INFO] simulator library dir: $sim_lib_dir
[INFO] xcode destination: $destination
EOF

copy_source_to_target_name "$source_ghc" "$target_ghc"
copy_source_to_target_name "$source_plain" "$target_plain"
for lib in libffi.a libgmp.a libgmpxx.a; do
  copy_source_to_target_name "$x86_dir/$lib" "$sim_lib_dir/$lib"
done
write_sha_table "$sim_lib_dir" "$temporary_sha"

build_status="SKIPPED"
app_path=""
binary_path=""
binary_archs=""
install_status="SKIPPED"
launch_status="SKIPPED"
bundle_id=""
derived_data="$output_dir/DerivedData"

if [ "$skip_build" -eq 0 ]; then
  build_log="$output_dir/logs/xcodebuild-x86_64-simulator.log"
  set +e
  DEVELOPER_DIR="$developer_dir" /usr/bin/xcodebuild \
    -project "$project_path" \
    -scheme "$scheme" \
    -configuration "$configuration" \
    -sdk iphonesimulator \
    -destination "$destination" \
    -derivedDataPath "$derived_data" \
    ARCHS=x86_64 \
    ONLY_ACTIVE_ARCH=NO \
    EXCLUDED_ARCHS=arm64 \
    CODE_SIGNING_ALLOWED=NO \
    build > "$build_log" 2>&1
  build_rc=$?
  set -e

  if [ "$build_rc" -eq 0 ]; then
    build_status="PASS"
    app_path="$(find "$derived_data/Build/Products/${configuration}-iphonesimulator" -maxdepth 2 -type d -name '*.app' 2>/dev/null | sort | head -1)"
    if [ -n "$app_path" ] && [ -f "$app_path/Info.plist" ]; then
      bundle_id="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$app_path/Info.plist" 2>/dev/null || true)"
      executable_name="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleExecutable' "$app_path/Info.plist" 2>/dev/null || true)"
      if [ -n "$executable_name" ] && [ -f "$app_path/$executable_name" ]; then
        binary_path="$app_path/$executable_name"
        binary_archs="$(lipo -info "$binary_path" 2>/dev/null || file "$binary_path" 2>/dev/null || true)"
      fi
    fi
  else
    build_status="BLOCKED"
  fi
fi

restore_now

if [ "$install_launch" -eq 1 ] && [ "$build_status" = "PASS" ]; then
  if [ -z "$simulator_id" ]; then
    simulator_id="$(DEVELOPER_DIR="$developer_dir" /usr/bin/xcrun simctl list devices booted 2>/dev/null | sed -nE 's/.*\(([A-F0-9-]+)\) \(Booted\).*/\1/p' | head -1)"
  fi

  if [ -z "$simulator_id" ]; then
    install_status="BLOCKED"
    launch_status="BLOCKED"
    printf 'No booted simulator and no --simulator UUID supplied.\n' > "$output_dir/logs/install-launch.log"
  elif [ -z "$app_path" ] || [ ! -d "$app_path" ]; then
    install_status="BLOCKED"
    launch_status="BLOCKED"
    printf 'Build did not produce an app path.\n' > "$output_dir/logs/install-launch.log"
  elif [ -z "$bundle_id" ]; then
    install_status="BLOCKED"
    launch_status="BLOCKED"
    printf 'Build app is missing CFBundleIdentifier.\n' > "$output_dir/logs/install-launch.log"
  else
    set +e
    {
      printf '$ simctl install %s %s\n' "$simulator_id" "$app_path"
      DEVELOPER_DIR="$developer_dir" /usr/bin/xcrun simctl install "$simulator_id" "$app_path"
      install_rc=$?
      printf '\n$ simctl launch %s %s\n' "$simulator_id" "$bundle_id"
      if [ "$install_rc" -eq 0 ]; then
        DEVELOPER_DIR="$developer_dir" /usr/bin/xcrun simctl launch "$simulator_id" "$bundle_id"
        launch_rc=$?
      else
        launch_rc=1
      fi
      printf '\ninstall_rc=%s\nlaunch_rc=%s\n' "$install_rc" "$launch_rc"
    } > "$output_dir/logs/install-launch.log" 2>&1
    set -e

    [ "$install_rc" -eq 0 ] && install_status="PASS" || install_status="BLOCKED"
    [ "$launch_rc" -eq 0 ] && launch_status="PASS" || launch_status="BLOCKED"
  fi
fi

printf 'key\tvalue\n' > "$summary"
printf 'x86_dir\t%s\n' "$x86_dir" >> "$summary"
printf 'sim_lib_dir\t%s\n' "$sim_lib_dir" >> "$summary"
printf 'project\t%s\n' "$project_path" >> "$summary"
printf 'scheme\t%s\n' "$scheme" >> "$summary"
printf 'destination\t%s\n' "$destination" >> "$summary"
printf 'build_status\t%s\n' "$build_status" >> "$summary"
printf 'restore_status\t%s\n' "$restore_status" >> "$summary"
printf 'install_status\t%s\n' "$install_status" >> "$summary"
printf 'launch_status\t%s\n' "$launch_status" >> "$summary"
printf 'bundle_id\t%s\n' "$bundle_id" >> "$summary"
printf 'app_path\t%s\n' "$app_path" >> "$summary"
printf 'binary_path\t%s\n' "$binary_path" >> "$summary"
printf 'binary_archs\t%s\n' "$binary_archs" >> "$summary"
printf 'original_sha\t%s\n' "$original_sha" >> "$summary"
printf 'temporary_sha\t%s\n' "$temporary_sha" >> "$summary"
printf 'restored_sha\t%s\n' "$restored_sha" >> "$summary"

cat <<EOF

Probe summary
-------------
[INFO] Output: $output_dir
[INFO] Build status: $build_status
[INFO] Restore status: $restore_status
[INFO] Install status: $install_status
[INFO] Launch status: $launch_status
[INFO] Binary: ${binary_archs:-not built}
EOF

if [ "$restore_status" != "PASS" ]; then
  echo "[FAIL] Simulator libraries were not restored cleanly" >&2
  exit 1
fi

if [ "$build_status" = "BLOCKED" ] || [ "$install_status" = "BLOCKED" ] || [ "$launch_status" = "BLOCKED" ]; then
  exit 1
fi

exit 0
