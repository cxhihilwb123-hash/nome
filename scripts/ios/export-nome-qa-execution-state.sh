#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
checklist="$root_dir/plans/20260709_nome_ios_manual_qa_checklist.md"
output_dir="/tmp/nome-ios-qa-execution-state-$(date +%Y%m%d-%H%M%S)"
force=0
skip_gates=0
run_source_audit=0
source_target=""
generic_device_build_dir=""

usage() {
  cat <<'USAGE'
Usage: scripts/ios/export-nome-qa-execution-state.sh [options]

Exports a machine-readable execution packet for the remaining Nome iOS QA
work. It does not modify source files, install libraries, run a build, or mark
checklist items complete.

Options:
  --checklist FILE  Manual QA checklist to summarize.
  --output DIR      Output directory.
  --force           Replace an existing output directory.
  --skip-gates      Do not run local readiness gates; write WARN rows instead.
  --source-audit    Include the network-dependent real-core source audit.
  --source-target TARGET
                  Optional real-core target for route-specific checks:
                  simulator or physical-device.
  --generic-device-build-dir DIR
                  Reuse an existing check-ios-generic-device-build output in
                  the physical-device readiness packet.
  -h, --help        Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --checklist)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --checklist requires a file" >&2
        exit 2
      fi
      checklist="$1"
      ;;
    --output)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --output requires a directory" >&2
        exit 2
      fi
      output_dir="$1"
      ;;
    --force)
      force=1
      ;;
    --skip-gates)
      skip_gates=1
      ;;
    --source-audit)
      run_source_audit=1
      ;;
    --source-target)
      shift
      if [ "$#" -eq 0 ] || { [ "$1" != "simulator" ] && [ "$1" != "physical-device" ]; }; then
        echo "[FAIL] --source-target requires simulator or physical-device" >&2
        exit 2
      fi
      source_target="$1"
      ;;
    --generic-device-build-dir)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --generic-device-build-dir requires a directory" >&2
        exit 2
      fi
      generic_device_build_dir="$1"
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

if [ ! -f "$checklist" ]; then
  echo "[FAIL] Manual QA checklist not found: $checklist" >&2
  exit 1
fi

if [ -n "$generic_device_build_dir" ] && [ ! -d "$generic_device_build_dir" ]; then
  echo "[FAIL] Generic device build evidence not found: $generic_device_build_dir" >&2
  exit 1
fi

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
blocker_groups="$output_dir/blocker_groups.tsv"
next_actions="$output_dir/next_actions.tsv"
queue="$output_dir/manual_qa_unchecked.tsv"
manual_log="$output_dir/logs/manual_qa_status.log"
batches_dir="$output_dir/batches"

batch_id_for_group() {
  case "$1" in
    real_core_artifacts) printf 'batch-0' ;;
    first_run_real_core|network_server_real_core) printf 'batch-1' ;;
    real_core_functional|native_share_real_core|public_contact_real_core|error_state_real_core) printf 'batch-2' ;;
    multi_account_real_core|group_real_core|messaging_feature_real_core|conversation_safety_real_core) printf 'batch-3' ;;
    identity_real_data|migration_real_data) printf 'batch-4' ;;
    physical_device_or_camera) printf 'batch-5' ;;
    app_store_final_screenshots) printf 'batch-6' ;;
    release_identifiers) printf 'batch-7' ;;
    *) printf 'unmapped' ;;
  esac
}

batch_title_for_id() {
  case "$1" in
    batch-0) printf 'real-core artifacts' ;;
    batch-1) printf 'first-run real app setup' ;;
    batch-2) printf 'single-account connection surfaces' ;;
    batch-3) printf 'two-account messaging and groups' ;;
    batch-4) printf 'identity, migration, and data behavior' ;;
    batch-5) printf 'physical device and camera' ;;
    batch-6) printf 'final App Store screenshot package' ;;
    batch-7) printf 'release identifiers and capabilities' ;;
    *) printf 'unmapped' ;;
  esac
}

dependency_for_group() {
  case "$1" in
    real_core_artifacts)
      printf 'arm64 simulator core or deliberate physical-device real-core install'
      ;;
    first_run_real_core|network_server_real_core|real_core_functional|public_contact_real_core|error_state_real_core)
      printf 'real iOS core runtime'
      ;;
    native_share_real_core)
      printf 'real iOS core runtime and native share-sheet interaction'
      ;;
    multi_account_real_core|group_real_core|messaging_feature_real_core|conversation_safety_real_core)
      printf 'real iOS core runtime plus two independent accounts/devices'
      ;;
    identity_real_data|migration_real_data)
      printf 'real local profile data and migration source data'
      ;;
    physical_device_or_camera)
      printf 'connected trusted physical iPhone and camera permission'
      ;;
    app_store_final_screenshots)
      printf 'real-core replacement screenshots and final manifest cleanup'
      ;;
    release_identifiers)
      printf 'Nome release-identity decision for bundle ids, App Groups, keychain groups, domains, and BG task ids'
      ;;
    *)
      printf 'manual follow-up'
      ;;
  esac
}

action_for_batch() {
  case "$1" in
    batch-0)
      printf 'Resolve the real-core route before claiming real invitation, group, database, or messaging behavior.'
      ;;
    batch-1)
      printf 'After real core is installed, run a clean first-launch profile and network setup pass.'
      ;;
    batch-2)
      printf 'After real core is installed, exercise one-time links, native share, public contact address, and error states.'
      ;;
    batch-3)
      printf 'Prepare two accounts/devices and verify messaging, groups, media, and conversation safety state.'
      ;;
    batch-4)
      printf 'Verify profile switching, hidden profiles, incognito wording, and migration with real data.'
      ;;
    batch-5)
      printf 'Connect and trust a physical iPhone, then verify launch and live QR scanning.'
      ;;
    batch-6)
      printf 'Replace needs-real-core App Store screenshots and make the final screenshot gate pass.'
      ;;
    batch-7)
      printf 'Finalize release identifiers and capabilities before TestFlight or public distribution.'
      ;;
    *)
      printf 'Resolve unmapped QA work.'
      ;;
  esac
}

command_for_batch() {
  local real_core_check_cmd="scripts/ios/check-real-core.sh"
  local device_readiness_cmd="scripts/ios/export-ios-device-readiness-state.sh --output /tmp/nome-ios-device-readiness-state-current --force"

  if [ "$source_target" = "physical-device" ]; then
    real_core_check_cmd="$real_core_check_cmd --target physical-device"
  elif [ "$source_target" = "simulator" ]; then
    real_core_check_cmd="$real_core_check_cmd --target simulator"
  fi

  if [ -n "$generic_device_build_dir" ]; then
    device_readiness_cmd="$device_readiness_cmd --generic-device-build-dir $(printf '%q' "$generic_device_build_dir")"
  fi

  case "$1" in
    batch-0)
      printf 'scripts/ios/export-real-core-route-state.sh --output /tmp/nome-ios-real-core-route-state-current --force'
      ;;
    batch-1)
      printf '%s' "$real_core_check_cmd"
      ;;
    batch-2)
      printf '%s' "$real_core_check_cmd"
      ;;
    batch-3)
      printf '%s' "$real_core_check_cmd"
      ;;
    batch-4)
      printf '%s' "$real_core_check_cmd"
      ;;
    batch-5)
      printf '%s' "$device_readiness_cmd"
      ;;
    batch-6)
      printf 'scripts/ios/export-app-store-final-screenshot-state.sh --output /tmp/nome-ios-app-store-final-screenshot-state-current --force'
      ;;
    batch-7)
      printf 'scripts/ios/export-ios-release-identity-state.sh --output /tmp/nome-ios-release-identity-state-current --force'
      ;;
    *)
      printf 'scripts/ios/check-nome-manual-qa-status.sh --write-tsv /tmp/nome-manual-qa-unchecked.tsv'
      ;;
  esac
}

record_gate() {
  local status="$1"
  local name="$2"
  local log="$3"
  printf '%s\t%s\t%s\n' "$status" "$name" "$log" >> "$gate_status"
}

run_gate() {
  local status_on_fail="$1"
  local name="$2"
  local command="$3"
  local log="$output_dir/logs/$name.log"

  if [ "$skip_gates" -eq 1 ]; then
    printf 'Skipped by --skip-gates. Intended command:\n%s\n' "$command" > "$log"
    record_gate "WARN" "$name" "$log"
    return
  fi

  printf '$ %s\n\n' "$command" > "$log"
  if bash -c "$command" >> "$log" 2>&1; then
    record_gate "PASS" "$name" "$log"
  else
    record_gate "$status_on_fail" "$name" "$log"
  fi
}

set +e
"$root_dir/scripts/ios/check-nome-manual-qa-status.sh" --file "$checklist" --write-tsv "$queue" > "$manual_log" 2>&1
manual_rc=$?
set -e

if [ "$manual_rc" -gt 1 ]; then
  echo "[FAIL] Could not summarize manual QA checklist" >&2
  sed -n '1,160p' "$manual_log" >&2
  exit 1
fi

"$root_dir/scripts/ios/export-nome-qa-batches.sh" --checklist "$checklist" --output "$batches_dir" --force > "$output_dir/logs/export_batches.log" 2>&1

checked_count="$(grep -Ec '^- \[x\] ' "$checklist" || true)"
unchecked_count="$(grep -Ec '^- \[ \] ' "$checklist" || true)"
total_count=$((checked_count + unchecked_count))

printf 'key\tvalue\n' > "$summary"
printf 'checklist\t%s\n' "$checklist" >> "$summary"
printf 'manual_qa_status\t%s\n' "$([ "$manual_rc" -eq 0 ] && printf PASS || printf BLOCKED)" >> "$summary"
printf 'total_items\t%s\n' "$total_count" >> "$summary"
printf 'checked_items\t%s\n' "$checked_count" >> "$summary"
printf 'unchecked_items\t%s\n' "$unchecked_count" >> "$summary"
printf 'queue\t%s\n' "$queue" >> "$summary"
printf 'batches_dir\t%s\n' "$batches_dir" >> "$summary"
printf 'evidence_index\t%s\n' "$batches_dir/evidence_index.tsv" >> "$summary"
printf 'source_target\t%s\n' "${source_target:-all}" >> "$summary"
printf 'generic_device_build_dir\t%s\n' "${generic_device_build_dir:-}" >> "$summary"

printf 'status\tcheck\tlog\n' > "$gate_status"
record_gate "$([ "$manual_rc" -eq 0 ] && printf PASS || printf BLOCKED)" "manual_qa_status" "$manual_log"
record_gate "PASS" "qa_batch_export" "$output_dir/logs/export_batches.log"
run_gate "BLOCKED" "simulator_real_core_route" "scripts/ios/check-ios-simulator-real-core-route.sh"
run_gate "FAIL" "real_core_route_state" "scripts/ios/export-real-core-route-state.sh --output '$output_dir/real_core_route_state' --force"
run_gate "BLOCKED" "physical_device_readiness" "scripts/ios/check-ios-device-readiness.sh"
run_gate "BLOCKED" "physical_device_smoke" "scripts/ios/run-ios-physical-device-smoke.sh --output '$output_dir/physical_device_smoke' --force"

device_readiness_state_cmd="scripts/ios/export-ios-device-readiness-state.sh --output '$output_dir/device_readiness_state' --force"
if [ -n "$generic_device_build_dir" ]; then
  generic_device_build_dir_q="$(printf '%q' "$generic_device_build_dir")"
  device_readiness_state_cmd="$device_readiness_state_cmd --generic-device-build-dir $generic_device_build_dir_q"
fi
run_gate "FAIL" "device_readiness_state" "$device_readiness_state_cmd"

real_core_preflight_cmd="scripts/ios/check-real-core.sh"
real_core_build_env_cmd="scripts/ios/check-real-core-build-env.sh"
if [ "$source_target" = "physical-device" ]; then
  real_core_preflight_cmd="$real_core_preflight_cmd --target physical-device"
  real_core_build_env_cmd="$real_core_build_env_cmd --target physical-device --allow-downloaded-artifacts"
elif [ "$source_target" = "simulator" ]; then
  real_core_preflight_cmd="$real_core_preflight_cmd --target simulator"
  real_core_build_env_cmd="$real_core_build_env_cmd --target simulator"
fi
run_gate "BLOCKED" "real_core_preflight" "$real_core_preflight_cmd"
run_gate "BLOCKED" "real_core_build_env" "$real_core_build_env_cmd"
run_gate "FAIL" "app_store_final_screenshot_state" "scripts/ios/export-app-store-final-screenshot-state.sh --output '$output_dir/app_store_final_screenshot_state' --force"
run_gate "BLOCKED" "app_store_final_screenshots" "scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens"
run_gate "FAIL" "release_identity_state" "scripts/ios/export-ios-release-identity-state.sh --output '$output_dir/release_identity_state' --force"
run_gate "BLOCKED" "release_identifiers" "scripts/ios/check-ios-release-identifiers.sh"
run_gate "FAIL" "release_identifiers_compatibility_review" "scripts/ios/check-ios-release-identifiers.sh --compatibility-reviewed"

if [ "$run_source_audit" -eq 1 ]; then
  source_audit_cmd="scripts/ios/check-real-core-sources.sh"
  if [ -n "$source_target" ]; then
    source_audit_cmd="SOURCE_TARGET=$source_target $source_audit_cmd"
  fi
  run_gate "BLOCKED" "real_core_source_audit" "$source_audit_cmd"
else
  log="$output_dir/logs/real_core_source_audit.log"
  printf 'Skipped. Re-run with --source-audit for network-dependent source checks.\n' > "$log"
  record_gate "WARN" "real_core_source_audit" "$log"
fi

printf 'blocker_group\tcount\tbatch_id\tbatch_title\tdependency\n' > "$blocker_groups"
tail -n +2 "$queue" | awk -F '\t' 'NF {counts[$3]++} END {for (g in counts) print g "\t" counts[g]}' | sort |
while IFS=$'\t' read -r group count; do
  batch_id="$(batch_id_for_group "$group")"
  batch_title="$(batch_title_for_id "$batch_id")"
  dependency="$(dependency_for_group "$group")"
  printf '%s\t%s\t%s\t%s\t%s\n' "$group" "$count" "$batch_id" "$batch_title" "$dependency" >> "$blocker_groups"
done

printf 'priority\tbatch_id\tbatch_title\tunchecked_count\taction\tcommand\n' > "$next_actions"
priority=0
for batch_id in batch-0 batch-1 batch-2 batch-3 batch-4 batch-5 batch-6 batch-7; do
  count="$(awk -F '\t' -v batch="$batch_id" 'NR > 1 && $3 == batch {sum += $2} END {print sum + 0}' "$blocker_groups")"
  if [ "$count" -eq 0 ]; then
    continue
  fi

  priority=$((priority + 1))
  batch_title="$(batch_title_for_id "$batch_id")"
  action="$(action_for_batch "$batch_id")"
  command="$(command_for_batch "$batch_id")"
  printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$priority" "$batch_id" "$batch_title" "$count" "$action" "$command" >> "$next_actions"
done

cat > "$output_dir/README.md" <<EOF
# Nome iOS QA Execution State

Generated from:

- $checklist

Summary:

- total items: $total_count
- checked items: $checked_count
- unchecked items: $unchecked_count
- manual QA status: $([ "$manual_rc" -eq 0 ] && printf PASS || printf BLOCKED)

Files:

- summary.tsv
- gate_status.tsv
- blocker_groups.tsv
- next_actions.tsv
- manual_qa_unchecked.tsv
- batches/
- batches/evidence_index.tsv
- logs/

The next-actions table is ordered by the current execution plan. It is not a
claim that the app is complete; it points at the next evidence needed before
completion can be claimed.
EOF

gate_fail_count="$(awk -F '\t' 'NR > 1 && $1 == "FAIL" {count++} END {print count + 0}' "$gate_status")"
gate_blocked_count="$(awk -F '\t' 'NR > 1 && $1 == "BLOCKED" {count++} END {print count + 0}' "$gate_status")"
next_action_count="$(tail -n +2 "$next_actions" | awk 'NF {count++} END {print count + 0}')"

printf 'gate_fail_count\t%s\n' "$gate_fail_count" >> "$summary"
printf 'gate_blocked_count\t%s\n' "$gate_blocked_count" >> "$summary"
printf 'next_action_count\t%s\n' "$next_action_count" >> "$summary"

echo "Nome iOS QA execution state"
echo "==========================="
echo "[INFO] Output: $output_dir"
echo "[INFO] Unchecked manual QA items: $unchecked_count"
echo "[INFO] Gate blockers: $gate_blocked_count"
echo "[INFO] Next action rows: $next_action_count"
sed -n '1,40p' "$next_actions"
echo "[PASS] Exported Nome iOS QA execution state"
