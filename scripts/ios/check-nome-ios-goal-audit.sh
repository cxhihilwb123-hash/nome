#!/usr/bin/env bash

set -u

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
output_dir="/tmp/nome-ios-goal-audit-$(date +%Y%m%d-%H%M%S)"
smoke_manifest="${SMOKE_MANIFEST:-/tmp/nome-ios-smoke-contacts-tab/manifest.tsv}"
allow_blockers=0
run_source_audit=0
run_generic_device_build=0
generic_device_build_dir=""
source_job_repo=""
source_target=""

usage() {
  cat <<'USAGE'
Usage: scripts/ios/check-nome-ios-goal-audit.sh [options]

Audits the concrete evidence for the Nome iOS goal: plan exists, approved
effect-picture pages are mapped to implementation, the previewable UI smoke set
has current screenshot evidence, and release blockers are explicit.

Options:
  --smoke-manifest FILE  Manifest from scripts/ios/smoke-nome-ui.sh.
                         Defaults to /tmp/nome-ios-smoke-contacts-tab/manifest.tsv
                         or SMOKE_MANIFEST.
  --source-audit         Run the real-core source availability audit.
  --job-repo URL         Optional Hydra job repository URL for --source-audit.
  --source-target TARGET Optional source target for --source-audit: simulator or
                         physical-device.
  --generic-device-build Run a generic iOS device build with code signing disabled.
  --generic-device-build-dir DIR
                         Reuse an existing check-ios-generic-device-build output.
  --output DIR           Report output directory.
  --allow-blockers       Exit 0 when only known blockers remain.
  -h, --help             Show this help.

Statuses:
  PASS     Evidence proves this requirement for the current UX pass.
  WARN     Evidence is intentionally advisory or outside the current machine.
  BLOCKED  Known release blocker, usually missing real iOS core or final store screenshots.
  FAIL     Missing or contradictory evidence.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --smoke-manifest)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --smoke-manifest requires a file path" >&2
        exit 2
      fi
      smoke_manifest="$1"
      ;;
    --output)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --output requires a directory path" >&2
        exit 2
      fi
      output_dir="$1"
      ;;
    --source-audit)
      run_source_audit=1
      ;;
    --generic-device-build)
      run_generic_device_build=1
      ;;
    --generic-device-build-dir)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --generic-device-build-dir requires a directory" >&2
        exit 2
      fi
      generic_device_build_dir="$1"
      ;;
    --job-repo)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --job-repo requires a URL" >&2
        exit 2
      fi
      source_job_repo="$1"
      ;;
    --source-target)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --source-target requires simulator or physical-device" >&2
        exit 2
      fi
      source_target="$1"
      ;;
    --allow-blockers)
      allow_blockers=1
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

cd "$root_dir"
mkdir -p "$output_dir"

summary="$output_dir/summary.tsv"
printf 'status\trequirement\tevidence\n' > "$summary"

record() {
  local status="$1"
  local requirement="$2"
  local evidence="$3"

  printf '%s\t%s\t%s\n' "$status" "$requirement" "$evidence" >> "$summary"
  printf '[%s] %s\n' "$status" "$requirement"
}

fail_log() {
  local name="$1"
  printf '%s/%s.log' "$output_dir" "$name"
}

require_file() {
  local requirement="$1"
  local file="$2"

  if [ -e "$file" ]; then
    record "PASS" "$requirement" "$file"
  else
    record "FAIL" "$requirement" "missing: $file"
  fi
}

require_text() {
  local requirement="$1"
  local file="$2"
  local text="$3"

  if [ ! -f "$file" ]; then
    record "FAIL" "$requirement" "missing: $file"
  elif grep -Fq -- "$text" "$file"; then
    record "PASS" "$requirement" "$file contains '$text'"
  else
    record "FAIL" "$requirement" "$file missing '$text'"
  fi
}

run_gate() {
  local status_on_fail="$1"
  local requirement="$2"
  local command="$3"
  local log

  log="$(fail_log "$requirement")"
  printf '$ %s\n\n' "$command" > "$log"

  if bash -c "$command" >> "$log" 2>&1; then
    record "PASS" "$requirement" "$log"
  else
    record "$status_on_fail" "$requirement" "$log"
  fi
}

record_generic_device_build_evidence() {
  local dir="$1"
  local log

  log="$(fail_log "generic_device_build")"
  {
    printf 'Generic device build evidence: %s\n\n' "$dir"
    if [ -f "$dir/summary.tsv" ]; then
      printf 'summary.tsv:\n'
      cat "$dir/summary.tsv"
      printf '\n'
    else
      printf 'Missing summary.tsv\n\n'
    fi
    if [ -f "$dir/products.tsv" ]; then
      printf 'products.tsv:\n'
      cat "$dir/products.tsv"
      printf '\n'
    else
      printf 'Missing products.tsv\n\n'
    fi
  } > "$log"

  if [ -f "$dir/summary.tsv" ] &&
    [ -f "$dir/products.tsv" ] &&
    awk -F '\t' '$1 == "product_fail_count" && $2 == "0" {found=1} END {exit found ? 0 : 1}' "$dir/summary.tsv" &&
    awk -F '\t' 'NR > 1 && $1 == "app" && $2 == "Nome.app" && $3 == "PASS" {found=1} END {exit found ? 0 : 1}' "$dir/products.tsv" &&
    awk -F '\t' 'NR > 1 && $1 == "extension" && $2 == "SimpleX NSE.appex" && $3 == "PASS" {found=1} END {exit found ? 0 : 1}' "$dir/products.tsv" &&
    awk -F '\t' 'NR > 1 && $1 == "extension" && $2 == "SimpleX SE.appex" && $3 == "PASS" {found=1} END {exit found ? 0 : 1}' "$dir/products.tsv"; then
    record "PASS" "generic_device_build" "$log"
  else
    record "BLOCKED" "generic_device_build" "$log"
  fi
}

check_smoke_manifest() {
  local log
  local case_count
  local dim_count
  local small_screenshots
  local duplicate_hashes
  local missing=0
  local labels=(
    "01-home"
    "02-onboarding-welcome"
    "03-onboarding-profile"
    "04-onboarding-network"
    "05-onboarding-conditions"
    "06-chat-list-existing"
    "07-conversation-preview"
    "08-conversation-details"
    "09-identity-center"
    "10-add-friend"
    "11-join-group"
    "12-public-contact"
    "13-settings"
    "14-contacts"
  )

  log="$(fail_log "ui_smoke_manifest")"

  if [ ! -f "$smoke_manifest" ]; then
    printf 'Missing smoke manifest: %s\n' "$smoke_manifest" > "$log"
    record "FAIL" "ui_smoke_manifest" "$log"
    return
  fi

  {
    printf 'Smoke manifest: %s\n' "$smoke_manifest"
    printf '\nRequired labels:\n'
    printf '%s\n' "${labels[@]}"
  } > "$log"

  case_count="$(tail -n +2 "$smoke_manifest" | awk -F '\t' 'NF {count++} END {print count + 0}')"
  if [ "$case_count" -ne 14 ]; then
    printf '\nExpected 14 smoke cases, found %s\n' "$case_count" >> "$log"
    missing=1
  fi

  for label in "${labels[@]}"; do
    if ! awk -F '\t' -v label="$label" 'NR > 1 && $1 == label {found=1} END {exit found ? 0 : 1}' "$smoke_manifest"; then
      printf '\nMissing smoke label: %s\n' "$label" >> "$log"
      missing=1
    fi
  done

  dim_count="$(tail -n +2 "$smoke_manifest" | awk -F '\t' 'NF {print $3 "x" $4}' | sort -u | wc -l | tr -d '[:space:]')"
  if [ "$dim_count" -ne 1 ]; then
    printf '\nSmoke screenshots do not share one consistent dimension set:\n' >> "$log"
    tail -n +2 "$smoke_manifest" | awk -F '\t' 'NF {print $1 "\t" $3 "x" $4}' >> "$log"
    missing=1
  fi

  small_screenshots="$(tail -n +2 "$smoke_manifest" | awk -F '\t' 'NF && ($5 + 0) < 50000 {print $1 "\t" $5}')"
  if [ -n "$small_screenshots" ]; then
    printf '\nSmoke screenshots below 50 KB:\n%s\n' "$small_screenshots" >> "$log"
    missing=1
  fi

  duplicate_hashes="$(tail -n +2 "$smoke_manifest" | awk -F '\t' 'NF {print $6}' | sort | uniq -d || true)"
  if [ -n "$duplicate_hashes" ]; then
    printf '\nDuplicate smoke screenshot hashes:\n%s\n' "$duplicate_hashes" >> "$log"
    missing=1
  fi

  if [ "$missing" -eq 0 ]; then
    printf '\nPASS: manifest contains 14 unique, consistently-sized smoke screenshots.\n' >> "$log"
    record "PASS" "ui_smoke_manifest" "$log"
  else
    record "FAIL" "ui_smoke_manifest" "$log"
  fi
}

require_file "planning_record" "plans/20260709_nome_ios_app_development_plan.md"
require_file "qa_record" "plans/20260709_nome_ios_qa_record.md"
require_file "manual_qa_checklist" "plans/20260709_nome_ios_manual_qa_checklist.md"
require_file "remaining_qa_execution_plan" "plans/20260709_nome_ios_remaining_qa_execution_plan.md"
require_file "real_core_handoff" "plans/20260709_nome_ios_real_core_testing_plan.md"
require_file "design_coverage_matrix" "plans/20260709_nome_ios_design_coverage_matrix.md"
require_text "contacts_smoke_standard" "scripts/ios/smoke-nome-ui.sh" "expected 14"
require_text "contacts_preview_launch_arg" "apps/ios/Shared/SimpleXApp.swift" "-NomeContactsPreview"

for artifact in \
  "design/product/pages-v2/01-home-inbox.png" \
  "design/product/pages-v2/02-add-friend-one-time.png" \
  "design/product/pages-v2/03-join-group.png" \
  "design/product/pages-v2/04-public-contact.png" \
  "design/product/pages-v2/05-identity-center.png" \
  "design/product/pages-v2/06-conversation.png" \
  "design/product/pages-v2/07-settings-safety.png"
do
  require_file "approved_design_artifact" "$artifact"
done

run_gate "FAIL" "design_coverage_gate" "scripts/ios/check-nome-design-coverage.sh"
run_gate "FAIL" "brand_copy_gate" "scripts/ios/check-nome-brand-copy.sh"
run_gate "FAIL" "real_core_arch_unit" "scripts/ios/test-check-real-core-arch.sh"
run_gate "FAIL" "real_core_xcode_sync_unit" "scripts/ios/test-check-real-core-xcode-sync.sh"
run_gate "FAIL" "release_identifier_unit" "scripts/ios/test-check-ios-release-identifiers.sh"
run_gate "FAIL" "release_identity_state_unit" "scripts/ios/test-export-ios-release-identity-state.sh"
run_gate "FAIL" "release_identity_proposal_unit" "scripts/ios/test-check-ios-release-identity-proposal.sh"
run_gate "FAIL" "release_identity_apply_unit" "scripts/ios/test-apply-ios-release-identifiers.sh"
run_gate "FAIL" "device_readiness_state_unit" "scripts/ios/test-export-ios-device-readiness-state.sh"
run_gate "FAIL" "generic_device_build_unit" "scripts/ios/test-check-ios-generic-device-build.sh"
run_gate "FAIL" "physical_device_smoke_unit" "scripts/ios/test-run-ios-physical-device-smoke.sh"
run_gate "FAIL" "ios_preview_tooling_unit" "scripts/ios/test-check-ios-preview-tooling.sh"
run_gate "FAIL" "completion_state_unit" "scripts/ios/test-export-nome-ios-completion-state.sh"
run_gate "FAIL" "simulator_real_core_route_unit" "scripts/ios/test-check-ios-simulator-real-core-route.sh"
run_gate "FAIL" "x86_64_sim_real_core_probe_unit" "scripts/ios/test-probe-x86_64-simulator-real-core.sh"
run_gate "FAIL" "real_core_route_state_unit" "scripts/ios/test-export-real-core-route-state.sh"
run_gate "FAIL" "prepare_device_real_core_unit" "scripts/ios/test-prepare-device-real-core.sh"
run_gate "FAIL" "real_core_staging_unit" "scripts/ios/test-stage-real-core-artifacts.sh"
run_gate "FAIL" "run_real_core_batch0_unit" "scripts/ios/test-run-real-core-batch0.sh"
run_gate "FAIL" "app_store_final_blocker_plan_unit" "scripts/ios/test-app-store-final-blocker-plan.sh"
run_gate "FAIL" "app_store_final_screenshot_state_unit" "scripts/ios/test-export-app-store-final-screenshot-state.sh"
run_gate "FAIL" "app_store_final_package_unit" "scripts/ios/test-prepare-app-store-final-screenshot-package.sh"
run_gate "FAIL" "design_evidence_state_unit" "scripts/ios/test-export-nome-design-evidence-state.sh"
run_gate "FAIL" "smoke_visual_quality_unit" "scripts/ios/test-check-nome-smoke-visual-quality.sh"
run_gate "FAIL" "qa_batch_export_unit" "scripts/ios/test-export-nome-qa-batches.sh"
run_gate "FAIL" "qa_execution_state_unit" "scripts/ios/test-export-nome-qa-execution-state.sh"
run_gate "FAIL" "qa_execution_plan_gate" "scripts/ios/check-nome-qa-execution-plan.sh"
run_gate "FAIL" "ios_preview_tooling" "scripts/ios/check-ios-preview-tooling.sh"
printf -v qa_batches_dir_q '%q' "$output_dir/qa_batches"
run_gate "FAIL" "qa_batch_export" "scripts/ios/export-nome-qa-batches.sh --output $qa_batches_dir_q --force"
printf -v qa_execution_state_dir_q '%q' "$output_dir/qa_execution_state"
run_gate "FAIL" "qa_execution_state" "scripts/ios/export-nome-qa-execution-state.sh --output $qa_execution_state_dir_q --force"
printf -v manual_qa_tsv_q '%q' "$output_dir/manual_qa_unchecked.tsv"
run_gate "BLOCKED" "manual_qa_status" "scripts/ios/check-nome-manual-qa-status.sh --write-tsv $manual_qa_tsv_q"
check_smoke_manifest
printf -v smoke_visual_quality_manifest_q '%q' "$smoke_manifest"
printf -v smoke_visual_quality_dir_q '%q' "$output_dir/smoke_visual_quality"
run_gate "FAIL" "smoke_visual_quality" "scripts/ios/check-nome-smoke-visual-quality.sh --manifest $smoke_visual_quality_manifest_q --expected-count 14 --output $smoke_visual_quality_dir_q --force"
printf -v design_evidence_smoke_manifest_q '%q' "$smoke_manifest"
printf -v design_evidence_state_dir_q '%q' "$output_dir/design_evidence_state"
run_gate "FAIL" "design_evidence_state" "scripts/ios/export-nome-design-evidence-state.sh --smoke-manifest $design_evidence_smoke_manifest_q --output $design_evidence_state_dir_q --force"
effective_generic_device_build_dir=""
if [ "$run_generic_device_build" -eq 1 ]; then
  printf -v generic_device_build_dir_q '%q' "$output_dir/generic_device_build"
  printf -v generic_device_build_derived_q '%q' "$output_dir/generic_device_derived"
  run_gate "BLOCKED" "generic_device_build" "scripts/ios/check-ios-generic-device-build.sh --derived-data $generic_device_build_derived_q --output $generic_device_build_dir_q --force"
  effective_generic_device_build_dir="$output_dir/generic_device_build"
elif [ -n "$generic_device_build_dir" ]; then
  record_generic_device_build_evidence "$generic_device_build_dir"
  effective_generic_device_build_dir="$generic_device_build_dir"
else
  record "WARN" "generic_device_build" "Skipped. Re-run with --generic-device-build to compile the generic iOS device target with code signing disabled."
fi
printf -v completion_state_smoke_manifest_q '%q' "$smoke_manifest"
printf -v completion_state_dir_q '%q' "$output_dir/completion_state"
completion_state_cmd="scripts/ios/export-nome-ios-completion-state.sh --smoke-manifest $completion_state_smoke_manifest_q --output $completion_state_dir_q --force"
if [ -n "$effective_generic_device_build_dir" ]; then
  printf -v effective_generic_device_build_dir_q '%q' "$effective_generic_device_build_dir"
  completion_state_cmd="$completion_state_cmd --generic-device-build-dir $effective_generic_device_build_dir_q"
fi
run_gate "FAIL" "completion_state" "$completion_state_cmd"
require_file "app_store_final_blocker_ledger" "design/app-store/ios-upload-draft-screens/FINAL_BLOCKERS.md"
run_gate "FAIL" "app_store_candidate_screenshots" "scripts/ios/check-app-store-screenshots.sh"
run_gate "FAIL" "app_store_final_blocker_plan" "scripts/ios/check-app-store-final-blocker-plan.sh"
printf -v app_store_final_screenshot_state_dir_q '%q' "$output_dir/app_store_final_screenshot_state"
run_gate "FAIL" "app_store_final_screenshot_state" "scripts/ios/export-app-store-final-screenshot-state.sh --output $app_store_final_screenshot_state_dir_q --force"
run_gate "BLOCKED" "app_store_final_screenshots" "scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens"
run_gate "BLOCKED" "real_core_xcode_sync" "scripts/ios/check-real-core-xcode-sync.sh"
printf -v real_core_route_state_dir_q '%q' "$output_dir/real_core_route_state"
run_gate "FAIL" "real_core_route_state" "scripts/ios/export-real-core-route-state.sh --output $real_core_route_state_dir_q --force"
real_core_preflight_cmd="scripts/ios/check-real-core.sh"
if [ "$source_target" = "physical-device" ]; then
  real_core_preflight_cmd="$real_core_preflight_cmd --target physical-device"
elif [ "$source_target" = "simulator" ]; then
  real_core_preflight_cmd="$real_core_preflight_cmd --target simulator"
fi
run_gate "BLOCKED" "real_core_preflight" "$real_core_preflight_cmd"
run_gate "BLOCKED" "simulator_real_core_route" "scripts/ios/check-ios-simulator-real-core-route.sh"
real_core_build_env_cmd="scripts/ios/check-real-core-build-env.sh"
if [ "$source_target" = "physical-device" ]; then
  real_core_build_env_cmd="$real_core_build_env_cmd --target physical-device --allow-downloaded-artifacts"
elif [ "$source_target" = "simulator" ]; then
  real_core_build_env_cmd="$real_core_build_env_cmd --target simulator"
fi
run_gate "BLOCKED" "real_core_build_env" "$real_core_build_env_cmd"
printf -v device_readiness_state_dir_q '%q' "$output_dir/device_readiness_state"
device_readiness_state_cmd="scripts/ios/export-ios-device-readiness-state.sh --output $device_readiness_state_dir_q --force"
if [ -n "$effective_generic_device_build_dir" ]; then
  printf -v effective_generic_device_build_dir_q '%q' "$effective_generic_device_build_dir"
  device_readiness_state_cmd="$device_readiness_state_cmd --generic-device-build-dir $effective_generic_device_build_dir_q"
fi
run_gate "FAIL" "device_readiness_state" "$device_readiness_state_cmd"
run_gate "BLOCKED" "physical_device_readiness" "scripts/ios/check-ios-device-readiness.sh"
printf -v physical_device_smoke_dir_q '%q' "$output_dir/physical_device_smoke"
run_gate "BLOCKED" "physical_device_smoke" "scripts/ios/run-ios-physical-device-smoke.sh --output $physical_device_smoke_dir_q --force"
run_gate "FAIL" "release_identifiers_compatibility_review" "scripts/ios/check-ios-release-identifiers.sh --compatibility-reviewed"
printf -v release_identity_state_dir_q '%q' "$output_dir/release_identity_state"
run_gate "FAIL" "release_identity_state" "scripts/ios/export-ios-release-identity-state.sh --output $release_identity_state_dir_q --force"
run_gate "BLOCKED" "release_identifiers" "scripts/ios/check-ios-release-identifiers.sh"

if [ "$run_source_audit" -eq 1 ]; then
  source_audit_cmd='scripts/ios/check-real-core-sources.sh'
  if [ -n "$source_target" ]; then
    printf -v quoted_source_target '%q' "$source_target"
    source_audit_cmd="SOURCE_TARGET=$quoted_source_target $source_audit_cmd"
  fi
  if [ -n "$source_job_repo" ]; then
    printf -v quoted_source_job_repo '%q' "$source_job_repo"
    source_audit_cmd="$source_audit_cmd --job-repo $quoted_source_job_repo"
  fi
  run_gate "BLOCKED" "real_core_source_audit" "$source_audit_cmd"
else
  record "WARN" "real_core_source_audit" "Skipped. Re-run with --source-audit to check local, GitHub, Actions, and optional Hydra artifact sources."
fi

record "WARN" "physical_device_test" "Not run here; checklist keeps this conditional on device availability."

fail_count="$(awk -F '\t' 'NR > 1 && $1 == "FAIL" {count++} END {print count + 0}' "$summary")"
blocked_count="$(awk -F '\t' 'NR > 1 && $1 == "BLOCKED" {count++} END {print count + 0}' "$summary")"
warn_count="$(awk -F '\t' 'NR > 1 && $1 == "WARN" {count++} END {print count + 0}' "$summary")"
pass_count="$(awk -F '\t' 'NR > 1 && $1 == "PASS" {count++} END {print count + 0}' "$summary")"

cat <<EOF

Nome iOS goal audit summary
===========================
Report: $output_dir
PASS: $pass_count
WARN: $warn_count
BLOCKED: $blocked_count
FAIL: $fail_count

EOF

sed -n '1,220p' "$summary"

if [ "$fail_count" -gt 0 ]; then
  exit 1
fi

if [ "$blocked_count" -gt 0 ] && [ "$allow_blockers" -ne 1 ]; then
  exit 1
fi

exit 0
