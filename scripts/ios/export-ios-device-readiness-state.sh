#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
checklist="${CHECKLIST:-$root_dir/plans/20260709_nome_ios_manual_qa_checklist.md}"
output_dir="/tmp/nome-ios-device-readiness-state-$(date +%Y%m%d-%H%M%S)"
device_readiness_log=""
prepare_device_log=""
source_audit_log=""
xcode_sync_log=""
generic_device_build_dir=""
force=0
skip_gate=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/export-ios-device-readiness-state.sh [options]

Exports a read-only state packet for the Nome iOS physical-device path.
The packet separates Xcode/signing readiness, connected device availability,
local arm64 device artifacts, installed device libraries, and release-identifier
review so the next physical-device action is explicit.

Options:
  --output DIR                 Output directory.
  --checklist FILE             Manual QA checklist used to anchor actions.
  --device-readiness-log FILE  Parse an existing check-ios-device-readiness log.
  --prepare-device-log FILE    Parse an existing prepare-device-real-core dry-run log.
  --source-audit-log FILE      Parse an existing SOURCE_TARGET=physical-device
                               check-real-core-sources log.
  --xcode-sync-log FILE        Parse an existing check-real-core-xcode-sync log.
  --generic-device-build-dir DIR
                               Parse an existing check-ios-generic-device-build
                               output directory.
  --force                      Replace an existing output directory.
  --skip-gate                  Do not run check-ios-device-readiness.
  -h, --help                   Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --output)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --output requires a directory" >&2
        exit 2
      fi
      output_dir="$1"
      ;;
    --checklist)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --checklist requires a file" >&2
        exit 2
      fi
      checklist="$1"
      ;;
    --device-readiness-log)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --device-readiness-log requires a file" >&2
        exit 2
      fi
      device_readiness_log="$1"
      ;;
    --prepare-device-log)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --prepare-device-log requires a file" >&2
        exit 2
      fi
      prepare_device_log="$1"
      ;;
    --source-audit-log)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --source-audit-log requires a file" >&2
        exit 2
      fi
      source_audit_log="$1"
      ;;
    --xcode-sync-log)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --xcode-sync-log requires a file" >&2
        exit 2
      fi
      xcode_sync_log="$1"
      ;;
    --generic-device-build-dir)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --generic-device-build-dir requires a directory" >&2
        exit 2
      fi
      generic_device_build_dir="$1"
      ;;
    --force)
      force=1
      ;;
    --skip-gate)
      skip_gate=1
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
gate_status="$output_dir/gate_status.tsv"
device_status="$output_dir/device_status.tsv"
next_actions="$output_dir/next_actions.tsv"
log="$output_dir/logs/physical_device_readiness.log"
prepare_log="$output_dir/logs/prepare_device_real_core.log"
source_log="$output_dir/logs/physical_device_source_audit.log"
xcode_sync_state_log="$output_dir/logs/real_core_xcode_sync.log"
generic_device_build_log="$output_dir/logs/generic_device_build.log"

contains() {
  local file="$1"
  local text="$2"
  grep -Fq "$text" "$file" 2>/dev/null
}

contains_regex() {
  local file="$1"
  local pattern="$2"
  grep -Eq "$pattern" "$file" 2>/dev/null
}

record_gate() {
  local status="$1"
  local name="$2"
  local gate_log="$3"
  printf '%s\t%s\t%s\n' "$status" "$name" "$gate_log" >> "$gate_status"
}

qa_anchor_for_area() {
  case "$1" in
    xcode_environment|signing_environment|connected_device|generic_device_build)
      printf 'The app installs and launches on a physical iPhone, if available.'
      ;;
    local_device_artifact)
      printf 'Local arm64 device artifact input in `~/Downloads/pkg-ios-aarch64-swift-json` is production-sized, `arm64`, and free of preview-core markers for the physical-device path.'
      ;;
    physical_device_source_audit)
      printf 'Local arm64 device artifact input in `~/Downloads/pkg-ios-aarch64-swift-json` is production-sized, `arm64`, and free of preview-core markers for the physical-device path.'
      ;;
    installed_device_libraries|device_project_references)
      printf 'Real iOS core libraries are installed in `apps/ios/Libraries/ios` and `apps/ios/Libraries/sim`.'
      ;;
    release_identifiers)
      printf 'Nome-owned bundle identifiers are selected for the app, tests, notification service extension, share extension, and internal framework, or compatibility exceptions are explicitly approved for release.'
      ;;
    *)
      printf 'The app installs and launches on a physical iPhone, if available.'
      ;;
  esac
}

blocker_group_for_area() {
  case "$1" in
    connected_device|xcode_environment|signing_environment|generic_device_build)
      printf 'physical_device_or_camera'
      ;;
    local_device_artifact|physical_device_source_audit|installed_device_libraries|device_project_references)
      printf 'real_core_artifacts'
      ;;
    release_identifiers)
      printf 'release_identifiers'
      ;;
    *)
      printf 'physical_device_or_camera'
      ;;
  esac
}

batch_id_for_group() {
  case "$1" in
    real_core_artifacts) printf 'batch-0' ;;
    physical_device_or_camera) printf 'batch-5' ;;
    release_identifiers) printf 'batch-7' ;;
    *) printf 'unmapped' ;;
  esac
}

qa_location_for_anchor() {
  local anchor="$1"

  if [ ! -f "$checklist" ]; then
    printf '\t'
    return
  fi

  awk -v anchor="$anchor" '
    /^## / {
      section = substr($0, 4)
      next
    }
    index($0, anchor) > 0 {
      print NR "\t" section
      found = 1
      exit
    }
    END {
      if (!found) {
        print "\t"
      }
    }
  ' "$checklist"
}

write_area() {
  local area="$1"
  local status="$2"
  local evidence="$3"
  local next_step="$4"
  local anchor
  local location
  local manual_qa_line
  local manual_qa_section
  local blocker_group
  local batch_id

  anchor="$(qa_anchor_for_area "$area")"
  location="$(qa_location_for_anchor "$anchor")"
  manual_qa_line="$(printf '%s\n' "$location" | cut -f1)"
  manual_qa_section="$(printf '%s\n' "$location" | cut -f2)"
  blocker_group="$(blocker_group_for_area "$area")"
  batch_id="$(batch_id_for_group "$blocker_group")"

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$area" "$status" "$evidence" "$next_step" "$anchor" "$manual_qa_line" "$manual_qa_section" "$blocker_group" "$batch_id" >> "$device_status"
}

command_for_area() {
  case "$1" in
    xcode_environment)
      printf 'DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-ios-device-readiness.sh'
      ;;
    connected_device)
      printf 'DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-ios-device-readiness.sh'
      ;;
    local_device_artifact)
      printf 'scripts/ios/prepare-device-real-core.sh'
      ;;
    physical_device_source_audit)
      printf 'SOURCE_TARGET=physical-device CHECK_XCODE_DESTINATIONS=0 INCLUDE_KNOWN_HYDRA_REPOS=0 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-real-core-sources.sh'
      ;;
    installed_device_libraries)
      printf 'scripts/ios/prepare-device-real-core.sh --prepare'
      ;;
    device_project_references)
      printf 'scripts/ios/check-real-core-xcode-sync.sh'
      ;;
    signing_environment)
      printf 'DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-ios-device-readiness.sh'
      ;;
    generic_device_build)
      printf 'DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-ios-generic-device-build.sh --derived-data /tmp/nome-ios-derived-generic-device-current --output /tmp/nome-ios-generic-device-build-current --force'
      ;;
    release_identifiers)
      printf 'scripts/ios/export-ios-release-identity-state.sh --output /tmp/nome-ios-release-identity-state-current --force'
      ;;
    *)
      printf 'scripts/ios/export-ios-device-readiness-state.sh'
      ;;
  esac
}

write_next_action() {
  local priority="$1"
  local area="$2"
  local status="$3"
  local action="$4"
  local command="$5"
  local evidence="$6"
  local anchor
  local location
  local manual_qa_line
  local manual_qa_section
  local blocker_group
  local batch_id

  anchor="$(qa_anchor_for_area "$area")"
  location="$(qa_location_for_anchor "$anchor")"
  manual_qa_line="$(printf '%s\n' "$location" | cut -f1)"
  manual_qa_section="$(printf '%s\n' "$location" | cut -f2)"
  blocker_group="$(blocker_group_for_area "$area")"
  batch_id="$(batch_id_for_group "$blocker_group")"

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$priority" "$area" "$status" "$action" "$command" "$evidence" "$anchor" "$manual_qa_line" "$manual_qa_section" "$blocker_group" "$batch_id" >> "$next_actions"
}

printf 'status\tcheck\tlog\n' > "$gate_status"

if [ -n "$device_readiness_log" ]; then
  if [ ! -f "$device_readiness_log" ]; then
    echo "[FAIL] Supplied device readiness log does not exist: $device_readiness_log" >&2
    exit 1
  fi
  cp "$device_readiness_log" "$log"
  if grep -Eq '^\[FAIL\]|^\[BLOCKED\]' "$log"; then
    record_gate "BLOCKED" "physical_device_readiness" "$log"
  else
    record_gate "PASS" "physical_device_readiness" "$log"
  fi
elif [ "$skip_gate" -eq 1 ]; then
  printf 'Skipped by --skip-gate. Intended command:\nDEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-ios-device-readiness.sh\n' > "$log"
  record_gate "WARN" "physical_device_readiness" "$log"
else
  printf '$ DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-ios-device-readiness.sh\n\n' > "$log"
  if DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-ios-device-readiness.sh >> "$log" 2>&1; then
    record_gate "PASS" "physical_device_readiness" "$log"
  else
    record_gate "BLOCKED" "physical_device_readiness" "$log"
  fi
fi

if [ -n "$prepare_device_log" ]; then
  if [ ! -f "$prepare_device_log" ]; then
    echo "[FAIL] Supplied prepare device log does not exist: $prepare_device_log" >&2
    exit 1
  fi
  cp "$prepare_device_log" "$prepare_log"
  if grep -Eq '^\[FAIL\]' "$prepare_log"; then
    record_gate "FAIL" "prepare_device_real_core" "$prepare_log"
  elif grep -Eq '^\[WARN\]' "$prepare_log"; then
    record_gate "WARN" "prepare_device_real_core" "$prepare_log"
  else
    record_gate "PASS" "prepare_device_real_core" "$prepare_log"
  fi
elif [ "$skip_gate" -eq 1 ]; then
  printf 'Skipped by --skip-gate. Intended command:\nscripts/ios/prepare-device-real-core.sh\n' > "$prepare_log"
  record_gate "WARN" "prepare_device_real_core" "$prepare_log"
else
  printf '$ scripts/ios/prepare-device-real-core.sh\n\n' > "$prepare_log"
  if scripts/ios/prepare-device-real-core.sh >> "$prepare_log" 2>&1; then
    if grep -Eq '^\[WARN\]' "$prepare_log"; then
      record_gate "WARN" "prepare_device_real_core" "$prepare_log"
    else
      record_gate "PASS" "prepare_device_real_core" "$prepare_log"
    fi
  else
    record_gate "FAIL" "prepare_device_real_core" "$prepare_log"
  fi
fi

if [ -n "$source_audit_log" ]; then
  if [ ! -f "$source_audit_log" ]; then
    echo "[FAIL] Supplied physical-device source audit log does not exist: $source_audit_log" >&2
    exit 1
  fi
  cp "$source_audit_log" "$source_log"
  if grep -Eq '^\[PASS\].*arm64 device artifact is available for physical-device testing' "$source_log"; then
    record_gate "PASS" "physical_device_source_audit" "$source_log"
  elif grep -Eq '^\[FAIL\]|^\[BLOCKED\]' "$source_log"; then
    record_gate "BLOCKED" "physical_device_source_audit" "$source_log"
  else
    record_gate "WARN" "physical_device_source_audit" "$source_log"
  fi
elif [ "$skip_gate" -eq 1 ]; then
  printf 'Skipped by --skip-gate. Intended command:\nSOURCE_TARGET=physical-device CHECK_XCODE_DESTINATIONS=0 INCLUDE_KNOWN_HYDRA_REPOS=0 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-real-core-sources.sh\n' > "$source_log"
  record_gate "WARN" "physical_device_source_audit" "$source_log"
else
  printf '$ SOURCE_TARGET=physical-device CHECK_XCODE_DESTINATIONS=0 INCLUDE_KNOWN_HYDRA_REPOS=0 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-real-core-sources.sh\n\n' > "$source_log"
  if SOURCE_TARGET=physical-device CHECK_XCODE_DESTINATIONS=0 INCLUDE_KNOWN_HYDRA_REPOS=0 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-real-core-sources.sh >> "$source_log" 2>&1; then
    record_gate "PASS" "physical_device_source_audit" "$source_log"
  else
    record_gate "BLOCKED" "physical_device_source_audit" "$source_log"
  fi
fi

if [ -n "$xcode_sync_log" ]; then
  if [ ! -f "$xcode_sync_log" ]; then
    echo "[FAIL] Supplied Xcode sync log does not exist: $xcode_sync_log" >&2
    exit 1
  fi
  cp "$xcode_sync_log" "$xcode_sync_state_log"
  if grep -Fq '[PASS] Xcode real-core references are consistent with installed build paths' "$xcode_sync_state_log"; then
    record_gate "PASS" "real_core_xcode_sync" "$xcode_sync_state_log"
  elif grep -Eq '^\[FAIL\]' "$xcode_sync_state_log"; then
    record_gate "BLOCKED" "real_core_xcode_sync" "$xcode_sync_state_log"
  else
    record_gate "WARN" "real_core_xcode_sync" "$xcode_sync_state_log"
  fi
elif [ "$skip_gate" -eq 1 ]; then
  printf 'Skipped by --skip-gate. Intended command:\nscripts/ios/check-real-core-xcode-sync.sh\n' > "$xcode_sync_state_log"
  record_gate "WARN" "real_core_xcode_sync" "$xcode_sync_state_log"
else
  printf '$ scripts/ios/check-real-core-xcode-sync.sh\n\n' > "$xcode_sync_state_log"
  if scripts/ios/check-real-core-xcode-sync.sh >> "$xcode_sync_state_log" 2>&1; then
    record_gate "PASS" "real_core_xcode_sync" "$xcode_sync_state_log"
  else
    record_gate "BLOCKED" "real_core_xcode_sync" "$xcode_sync_state_log"
  fi
fi

if [ -n "$generic_device_build_dir" ]; then
  if [ ! -d "$generic_device_build_dir" ]; then
    echo "[FAIL] Supplied generic device build directory does not exist: $generic_device_build_dir" >&2
    exit 1
  fi

  {
    printf 'Generic device build directory: %s\n\n' "$generic_device_build_dir"
    if [ -f "$generic_device_build_dir/summary.tsv" ]; then
      printf 'summary.tsv:\n'
      cat "$generic_device_build_dir/summary.tsv"
      printf '\n'
    else
      printf 'Missing summary.tsv\n'
    fi

    if [ -f "$generic_device_build_dir/products.tsv" ]; then
      printf '\nproducts.tsv:\n'
      cat "$generic_device_build_dir/products.tsv"
      printf '\n'
    else
      printf '\nMissing products.tsv\n'
    fi
  } > "$generic_device_build_log"

  if [ -f "$generic_device_build_dir/summary.tsv" ] &&
    [ -f "$generic_device_build_dir/products.tsv" ] &&
    awk -F '\t' '$1 == "product_fail_count" && $2 == "0" {found=1} END {exit found ? 0 : 1}' "$generic_device_build_dir/summary.tsv" &&
    awk -F '\t' 'NR > 1 && $1 == "app" && $2 == "Nome.app" && $3 == "PASS" {found=1} END {exit found ? 0 : 1}' "$generic_device_build_dir/products.tsv" &&
    awk -F '\t' 'NR > 1 && $1 == "extension" && $2 == "SimpleX NSE.appex" && $3 == "PASS" {found=1} END {exit found ? 0 : 1}' "$generic_device_build_dir/products.tsv" &&
    awk -F '\t' 'NR > 1 && $1 == "extension" && $2 == "SimpleX SE.appex" && $3 == "PASS" {found=1} END {exit found ? 0 : 1}' "$generic_device_build_dir/products.tsv"; then
    record_gate "PASS" "generic_device_build_evidence" "$generic_device_build_log"
  else
    record_gate "BLOCKED" "generic_device_build_evidence" "$generic_device_build_log"
  fi
else
  printf 'No generic-device build evidence supplied. Intended command:\nDEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-ios-generic-device-build.sh --derived-data /tmp/nome-ios-derived-generic-device-current --output /tmp/nome-ios-generic-device-build-current --force\n' > "$generic_device_build_log"
  record_gate "WARN" "generic_device_build_evidence" "$generic_device_build_log"
fi

printf 'area\tstatus\tevidence\tnext_step\tmanual_qa_anchor\tmanual_qa_line\tmanual_qa_section\tblocker_group\tbatch_id\n' > "$device_status"

if contains "$log" "[OK] Full Xcode is available" &&
  contains "$log" "[OK] Found Xcode project" &&
  contains "$log" "[OK] Xcode exposes the generic Any iOS Device destination" &&
  contains "$log" "[OK] Read generic iOS build settings"; then
  write_area "xcode_environment" "PASS" "$log" "Xcode device build environment is available."
else
  write_area "xcode_environment" "BLOCKED" "$log" "Fix Xcode/project/generic-device build settings, then re-run the device readiness gate."
fi

if contains "$log" "Connected physical iOS/iPadOS device(s):"; then
  write_area "connected_device" "PASS" "$log" "Use the connected trusted device for physical-device QA."
else
  write_area "connected_device" "BLOCKED" "$log" "Connect and trust a physical iPhone or iPad, then re-run the device readiness gate."
fi

if contains "$log" "Found local arm64 device artifact directory" &&
  contains "$log" "Local device artifact core library size looks production-like" &&
  contains "$log" "Local device artifact core library has no preview-core markers" &&
  contains "$log" "Local device artifact library supports arm64"; then
  write_area "local_device_artifact" "PASS" "$log" "Local arm64 device artifact can be used as the physical-device compatibility candidate."
elif contains "$log" "Found local arm64 device artifact zip"; then
  write_area "local_device_artifact" "WARN" "$log" "Expand and audit the local arm64 device artifact zip before device testing."
else
  write_area "local_device_artifact" "BLOCKED" "$log" "Obtain or build a production-sized arm64 iOS device core artifact."
fi

if contains "$source_log" "arm64 device artifact is available for physical-device testing"; then
  write_area "physical_device_source_audit" "PASS" "$source_log" "Physical-device source audit sees an arm64 device candidate."
elif contains "$source_log" "Missing local pkg-ios-aarch64-swift-json"; then
  write_area "physical_device_source_audit" "BLOCKED" "$source_log" "Obtain or build an arm64 iOS device artifact, then re-run the physical-device source audit."
else
  write_area "physical_device_source_audit" "WARN" "$source_log" "Review physical-device source-audit output before installing device libraries."
fi

if contains "$log" "Device real-core libraries are missing"; then
  write_area "installed_device_libraries" "BLOCKED" "$log" "Install audited device libraries only when ready for physical-device testing."
elif contains "$log" "Installed device library core library size looks production-like" &&
  contains "$log" "Installed device library core library has no preview-core markers" &&
  contains_regex "$log" '^\[OK\] Installed device library( library)? supports arm64:'; then
  write_area "installed_device_libraries" "PASS" "$log" "Installed device libraries look ready for physical-device builds."
else
  write_area "installed_device_libraries" "BLOCKED" "$log" "Inspect installed device libraries or install a known-good audited device artifact."
fi

if contains "$xcode_sync_state_log" "[PASS] Xcode real-core references are consistent with installed build paths"; then
  write_area "device_project_references" "PASS" "$xcode_sync_state_log" "Xcode project references resolve to installed simulator/device build-path libraries."
elif contains "$prepare_log" "archive name differs from current Xcode project reference"; then
  write_area "device_project_references" "WARN" "$prepare_log" "Before installing this device artifact for a physical-device build, choose a matching simulator/device pair or sync Xcode project references with a deliberate compatibility decision."
elif contains "$prepare_log" "[PASS] Device real-core artifact is ready for explicit install"; then
  write_area "device_project_references" "PASS" "$prepare_log" "Device artifact archive names match current project references for physical-device preparation."
elif contains "$prepare_log" "[FAIL]"; then
  write_area "device_project_references" "BLOCKED" "$prepare_log" "Fix device artifact preparation before attempting a physical-device build."
else
  write_area "device_project_references" "WARN" "$prepare_log" "Device artifact/project-reference compatibility was not fully checked."
fi

if contains "$log" "Development team is not configured"; then
  write_area "signing_environment" "BLOCKED" "$log" "Configure the Apple development team/provisioning before device install."
elif contains "$log" "[OK] Development team is configured" &&
  contains "$log" "[OK] Code signing style is Automatic"; then
  write_area "signing_environment" "PASS" "$log" "Automatic signing is configured for the generic iOS device destination."
else
  write_area "signing_environment" "WARN" "$log" "Review signing style and provisioning before physical-device install."
fi

if contains "$generic_device_build_log" $'app\tNome.app\tPASS' &&
  contains "$generic_device_build_log" $'extension\tSimpleX NSE.appex\tPASS' &&
  contains "$generic_device_build_log" $'extension\tSimpleX SE.appex\tPASS' &&
  contains "$generic_device_build_log" $'product_fail_count\t0'; then
  write_area "generic_device_build" "PASS" "$generic_device_build_log" "Generic iOS device build already produced the app and both extensions."
elif contains "$generic_device_build_log" "No generic-device build evidence supplied"; then
  write_area "generic_device_build" "WARN" "$generic_device_build_log" "Run a generic iOS device build before physical-device install to prove the current source links against installed device libraries."
else
  write_area "generic_device_build" "BLOCKED" "$generic_device_build_log" "Fix the generic iOS device build before attempting physical-device install."
fi

if contains "$log" "Bundle identifier is still upstream-compatible"; then
  write_area "release_identifiers" "WARN" "$log" "Choose Nome-owned identifiers or explicitly approve compatibility exceptions before TestFlight/public release."
elif contains "$log" "Bundle identifier is set:"; then
  write_area "release_identifiers" "PASS" "$log" "Bundle identifier is no longer the upstream app identifier for this target."
else
  write_area "release_identifiers" "BLOCKED" "$log" "Read and decide release identifiers before distribution."
fi

printf 'priority\tarea\tarea_status\taction\tcommand\tevidence\tmanual_qa_anchor\tmanual_qa_line\tmanual_qa_section\tblocker_group\tbatch_id\n' > "$next_actions"
priority=0
while IFS=$'\t' read -r area status _evidence next_step _anchor _line _section _group _batch; do
  [ "$area" != "area" ] || continue
  if [ "$status" = "PASS" ]; then
    continue
  fi
  priority=$((priority + 1))
  write_next_action "$priority" "$area" "$status" "$next_step" "$(command_for_area "$area")" "$device_status"
done < "$device_status"

pass_count="$(awk -F '\t' 'NR > 1 && $2 == "PASS" {count++} END {print count + 0}' "$device_status")"
warn_count="$(awk -F '\t' 'NR > 1 && $2 == "WARN" {count++} END {print count + 0}' "$device_status")"
blocked_count="$(awk -F '\t' 'NR > 1 && $2 == "BLOCKED" {count++} END {print count + 0}' "$device_status")"
gate_blocked_count="$(awk -F '\t' 'NR > 1 && $1 == "BLOCKED" {count++} END {print count + 0}' "$gate_status")"
next_action_count="$(tail -n +2 "$next_actions" | awk 'NF {count++} END {print count + 0}')"

printf 'key\tvalue\n' > "$summary"
printf 'checklist\t%s\n' "$checklist" >> "$summary"
printf 'device_area_pass_count\t%s\n' "$pass_count" >> "$summary"
printf 'device_area_warn_count\t%s\n' "$warn_count" >> "$summary"
printf 'device_area_blocked_count\t%s\n' "$blocked_count" >> "$summary"
printf 'gate_blocked_count\t%s\n' "$gate_blocked_count" >> "$summary"
printf 'next_action_rows\t%s\n' "$next_action_count" >> "$summary"
printf 'device_status\t%s\n' "$device_status" >> "$summary"
printf 'next_actions\t%s\n' "$next_actions" >> "$summary"

cat > "$output_dir/README.md" <<EOF
# Nome iOS Device Readiness State

This packet records the current physical-device path for Nome iOS without
installing libraries, building the app, signing, or touching device data.

Summary:

- PASS areas: $pass_count
- WARN areas: $warn_count
- BLOCKED areas: $blocked_count
- gate blockers: $gate_blocked_count

Files:

- summary.tsv
- gate_status.tsv
- device_status.tsv
- next_actions.tsv
- logs/
EOF

echo "Nome iOS device readiness state"
echo "==============================="
echo "[INFO] Output: $output_dir"
echo "[INFO] PASS areas: $pass_count"
echo "[INFO] WARN areas: $warn_count"
echo "[INFO] BLOCKED areas: $blocked_count"
sed -n '1,80p' "$device_status"
echo "[PASS] Exported Nome iOS device readiness state"
