#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
developer_dir="${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}"
project_path="${PROJECT_PATH:-$root_dir/apps/ios/SimpleX.xcodeproj}"
scheme="${SCHEME:-SimpleX (iOS)}"
configuration="${CONFIGURATION:-Debug}"
derived_data="${DERIVED_DATA_PATH:-/tmp/nome-ios-derived-generic-device}"
output_dir="/tmp/nome-ios-generic-device-build-$(date +%Y%m%d-%H%M%S)"
expected_app="${EXPECTED_APP:-Nome.app}"
expected_extensions="${EXPECTED_EXTENSIONS:-SimpleX NSE.appex
SimpleX SE.appex}"
skip_build=0
force=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/check-ios-generic-device-build.sh [options]

Builds the Nome iOS app for the generic iOS device destination with code
signing disabled, then records the expected app and extension products. This is
useful before a physical iPhone is connected: it proves the iPhoneOS target can
compile and link against the installed device libraries.

Options:
  --output DIR          Output evidence directory.
  --derived-data DIR    DerivedData directory. Default:
                        /tmp/nome-ios-derived-generic-device
  --configuration NAME  Xcode configuration. Default: Debug.
  --project PATH        Xcode project. Default: apps/ios/SimpleX.xcodeproj.
  --scheme NAME         Xcode scheme. Default: SimpleX (iOS).
  --skip-build          Do not invoke xcodebuild; inspect existing products.
  --force               Replace an existing output directory.
  -h, --help            Show this help.

Environment:
  DEVELOPER_DIR         Full Xcode path. Default:
                        /Applications/Xcode.app/Contents/Developer
  DERIVED_DATA_PATH     Override default DerivedData path.
  EXPECTED_APP          Expected .app product name. Default: Nome.app
  EXPECTED_EXTENSIONS   Newline-separated expected .appex product names.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --output)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --output requires a directory" >&2; exit 2; }
      output_dir="$1"
      ;;
    --derived-data)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --derived-data requires a directory" >&2; exit 2; }
      derived_data="$1"
      ;;
    --configuration)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --configuration requires a name" >&2; exit 2; }
      configuration="$1"
      ;;
    --project)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --project requires a path" >&2; exit 2; }
      project_path="$1"
      ;;
    --scheme)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --scheme requires a name" >&2; exit 2; }
      scheme="$1"
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
products_tsv="$output_dir/products.tsv"
build_log="$output_dir/logs/xcodebuild.log"
product_dir="$derived_data/Build/Products/${configuration}-iphoneos"
status=0

record_product() {
  local kind="$1"
  local name="$2"
  local path="$3"
  local product_status="$4"
  local size="missing"

  if [ -e "$path" ]; then
    size="$(du -sk "$path" | awk '{print $1}')K"
  fi

  printf '%s\t%s\t%s\t%s\t%s\n' "$kind" "$name" "$product_status" "$path" "$size" >> "$products_tsv"
}

cat <<'HEADER'
Nome iOS generic device build
=============================
HEADER

printf 'key\tvalue\n' > "$summary"
printf 'kind\tname\tstatus\tpath\tsize\n' > "$products_tsv"

printf '[INFO] Project: %s\n' "$project_path"
printf '[INFO] Scheme: %s\n' "$scheme"
printf '[INFO] Configuration: %s\n' "$configuration"
printf '[INFO] DerivedData: %s\n' "$derived_data"
printf '[INFO] Output: %s\n' "$output_dir"
printf '[INFO] Skip build: %s\n' "$skip_build"

if [ ! -d "$developer_dir" ]; then
  echo "[FAIL] Full Xcode is missing: $developer_dir" >&2
  status=1
fi
if [ ! -d "$project_path" ]; then
  echo "[FAIL] Xcode project is missing: $project_path" >&2
  status=1
fi

if [ "$status" -eq 0 ] && [ "$skip_build" -ne 1 ]; then
  {
    printf '$ DEVELOPER_DIR=%q xcodebuild -quiet -project %q -scheme %q -configuration %q -destination generic/platform=iOS -derivedDataPath %q -skipPackagePluginValidation -skipMacroValidation CODE_SIGNING_ALLOWED=NO build\n\n' \
      "$developer_dir" "$project_path" "$scheme" "$configuration" "$derived_data"
  } > "$build_log"

  if DEVELOPER_DIR="$developer_dir" /usr/bin/xcodebuild \
    -quiet \
    -project "$project_path" \
    -scheme "$scheme" \
    -configuration "$configuration" \
    -destination 'generic/platform=iOS' \
    -derivedDataPath "$derived_data" \
    -skipPackagePluginValidation \
    -skipMacroValidation \
    CODE_SIGNING_ALLOWED=NO \
    build >> "$build_log" 2>&1; then
    echo "[OK] Generic iOS device build completed"
  else
    echo "[FAIL] Generic iOS device build failed. Tail of log:" >&2
    tail -n 80 "$build_log" >&2 || true
    status=1
  fi
else
  printf 'Skipped build. Inspecting existing product directory: %s\n' "$product_dir" > "$build_log"
fi

if [ ! -d "$product_dir" ]; then
  echo "[FAIL] Missing product directory: $product_dir" >&2
  status=1
fi

app_path="$product_dir/$expected_app"
if [ -d "$app_path" ]; then
  echo "[OK] Found app product: $app_path"
  record_product "app" "$expected_app" "$app_path" "PASS"
else
  echo "[FAIL] Missing app product: $app_path" >&2
  record_product "app" "$expected_app" "$app_path" "FAIL"
  status=1
fi

while IFS= read -r extension_name; do
  [ -n "$extension_name" ] || continue
  extension_path="$product_dir/$extension_name"
  if [ -d "$extension_path" ]; then
    echo "[OK] Found extension product: $extension_path"
    record_product "extension" "$extension_name" "$extension_path" "PASS"
  else
    echo "[FAIL] Missing extension product: $extension_path" >&2
    record_product "extension" "$extension_name" "$extension_path" "FAIL"
    status=1
  fi
done <<EOF
$expected_extensions
EOF

pass_count="$(awk -F '\t' 'NR > 1 && $3 == "PASS" {count++} END {print count + 0}' "$products_tsv")"
fail_count="$(awk -F '\t' 'NR > 1 && $3 == "FAIL" {count++} END {print count + 0}' "$products_tsv")"

printf 'project\t%s\n' "$project_path" >> "$summary"
printf 'scheme\t%s\n' "$scheme" >> "$summary"
printf 'configuration\t%s\n' "$configuration" >> "$summary"
printf 'derived_data\t%s\n' "$derived_data" >> "$summary"
printf 'product_dir\t%s\n' "$product_dir" >> "$summary"
printf 'product_pass_count\t%s\n' "$pass_count" >> "$summary"
printf 'product_fail_count\t%s\n' "$fail_count" >> "$summary"
printf 'products\t%s\n' "$products_tsv" >> "$summary"
printf 'build_log\t%s\n' "$build_log" >> "$summary"

if [ "$status" -eq 0 ]; then
  echo "[PASS] Generic iOS device build state is ready"
else
  echo "[FAIL] Generic iOS device build state is not ready" >&2
fi

exit "$status"
