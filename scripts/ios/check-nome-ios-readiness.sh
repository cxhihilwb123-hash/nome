#!/bin/bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
output_dir="/tmp/nome-ios-readiness-$(date +%Y%m%d-%H%M%S)"
run_smoke=0
skip_smoke_build=0
smoke_manifest=""
run_source_audit=0
run_generic_device_build=0
generic_device_build_dir=""
allow_blockers=0
simulator_id=""
source_job_repo=""
source_target=""

usage() {
  cat <<'USAGE'
Usage: scripts/ios/check-nome-ios-readiness.sh [options]

Aggregates the current Nome iOS readiness gates into one report.

Options:
  --smoke             Run the simulator Nome UI smoke pass.
  --smoke-manifest FILE
                      Reuse an existing scripts/ios/smoke-nome-ui.sh manifest.
  --skip-smoke-build  Pass --skip-build to the smoke script.
  --simulator UUID    Simulator UUID for the smoke pass.
  --source-audit      Run the real-core source availability audit.
  --job-repo URL      Optional Hydra job repository URL for --source-audit.
  --source-target TARGET
                      Optional source target for --source-audit: simulator or
                      physical-device.
  --generic-device-build
                      Run a generic iOS device build with code signing disabled.
  --generic-device-build-dir DIR
                      Reuse an existing check-ios-generic-device-build output.
  --output DIR        Report output directory.
  --allow-blockers    Exit 0 when only known release blockers remain.
  -h, --help          Show this help.

Statuses:
  PASS     Check passed.
  WARN     Check was intentionally skipped or produced a non-blocking warning.
  BLOCKED  Known release-readiness blocker, such as missing real iOS core.
  FAIL     Unexpected regression or local check failure.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --smoke)
      run_smoke=1
      ;;
    --skip-smoke-build)
      skip_smoke_build=1
      ;;
    --smoke-manifest)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --smoke-manifest requires a file path" >&2
        exit 2
      fi
      smoke_manifest="$1"
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
    --simulator)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --simulator requires a UUID" >&2
        exit 2
      fi
      simulator_id="$1"
      ;;
    --output)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --output requires a directory" >&2
        exit 2
      fi
      output_dir="$1"
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
printf 'status\tcheck\tlog\n' > "$summary"

record() {
  local status="$1"
  local name="$2"
  local log="$3"

  printf '%s\t%s\t%s\n' "$status" "$name" "$log" >> "$summary"
  printf '[%s] %s\n' "$status" "$name"
}

run_shell_check() {
  local status_on_fail="$1"
  local name="$2"
  local command="$3"
  local log="$output_dir/$name.log"

  printf '[INFO] Running %s\n' "$name"
  printf '$ %s\n\n' "$command" > "$log"

  if bash -c "$command" >> "$log" 2>&1; then
    record "PASS" "$name" "$log"
  else
    record "$status_on_fail" "$name" "$log"
  fi
}

write_warn() {
  local name="$1"
  local message="$2"
  local log="$output_dir/$name.log"

  printf '%s\n' "$message" > "$log"
  record "WARN" "$name" "$log"
}

record_generic_device_build_evidence() {
  local dir="$1"
  local log="$output_dir/generic_device_build.log"

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
  local manifest_file="$1"
  local log="$output_dir/nome_ui_smoke.log"
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

  printf '[INFO] Checking existing nome_ui_smoke manifest\n'

  if [ ! -f "$manifest_file" ]; then
    printf 'Missing smoke manifest: %s\n' "$manifest_file" > "$log"
    record "FAIL" "nome_ui_smoke" "$log"
    return
  fi

  {
    printf 'Smoke manifest: %s\n' "$manifest_file"
    printf '\nRequired labels:\n'
    printf '%s\n' "${labels[@]}"
  } > "$log"

  case_count="$(tail -n +2 "$manifest_file" | awk -F '\t' 'NF {count++} END {print count + 0}')"
  if [ "$case_count" -ne 14 ]; then
    printf '\nExpected 14 smoke cases, found %s\n' "$case_count" >> "$log"
    missing=1
  fi

  for label in "${labels[@]}"; do
    if ! awk -F '\t' -v label="$label" 'NR > 1 && $1 == label {found=1} END {exit found ? 0 : 1}' "$manifest_file"; then
      printf '\nMissing smoke label: %s\n' "$label" >> "$log"
      missing=1
    fi
  done

  dim_count="$(tail -n +2 "$manifest_file" | awk -F '\t' 'NF {print $3 "x" $4}' | sort -u | wc -l | tr -d '[:space:]')"
  if [ "$dim_count" -ne 1 ]; then
    printf '\nSmoke screenshots do not share one consistent dimension set:\n' >> "$log"
    tail -n +2 "$manifest_file" | awk -F '\t' 'NF {print $1 "\t" $3 "x" $4}' >> "$log"
    missing=1
  fi

  small_screenshots="$(tail -n +2 "$manifest_file" | awk -F '\t' 'NF && ($5 + 0) < 50000 {print $1 "\t" $5}')"
  if [ -n "$small_screenshots" ]; then
    printf '\nSmoke screenshots below 50 KB:\n%s\n' "$small_screenshots" >> "$log"
    missing=1
  fi

  duplicate_hashes="$(tail -n +2 "$manifest_file" | awk -F '\t' 'NF {print $6}' | sort | uniq -d || true)"
  if [ -n "$duplicate_hashes" ]; then
    printf '\nDuplicate smoke screenshot hashes:\n%s\n' "$duplicate_hashes" >> "$log"
    missing=1
  fi

  if [ "$missing" -eq 0 ]; then
    printf '\nPASS: manifest contains 14 unique, consistently-sized smoke screenshots.\n' >> "$log"
    record "PASS" "nome_ui_smoke" "$log"
  else
    record "FAIL" "nome_ui_smoke" "$log"
  fi
}

script_syntax_cmd='sh -n scripts/ios/prepare.sh scripts/ios/prepare-x86_64.sh && bash -n scripts/ios/download-libs.sh scripts/ios/build-mac2ios.sh scripts/ios/prepare-real-core.sh scripts/ios/prepare-device-real-core.sh scripts/ios/alias-device-real-core-project-libs.sh scripts/ios/stage-real-core-artifacts.sh scripts/ios/run-real-core-batch0.sh scripts/ios/check-real-core.sh scripts/ios/check-real-core-xcode-sync.sh scripts/ios/check-real-core-sources.sh scripts/ios/check-real-core-build-env.sh scripts/ios/check-ios-preview-tooling.sh scripts/ios/check-ios-device-readiness.sh scripts/ios/check-ios-generic-device-build.sh scripts/ios/run-ios-physical-device-smoke.sh scripts/ios/export-ios-device-readiness-state.sh scripts/ios/check-ios-release-identifiers.sh scripts/ios/export-ios-release-identity-state.sh scripts/ios/check-ios-release-identity-proposal.sh scripts/ios/apply-ios-release-identifiers.sh scripts/ios/check-ios-simulator-real-core-route.sh scripts/ios/probe-x86_64-simulator-real-core.sh scripts/ios/export-real-core-route-state.sh scripts/ios/export-nome-ios-completion-state.sh scripts/ios/sync-real-core-xcode-project.sh scripts/ios/test-check-real-core-arch.sh scripts/ios/test-check-real-core-xcode-sync.sh scripts/ios/test-check-ios-preview-tooling.sh scripts/ios/test-check-ios-release-identifiers.sh scripts/ios/test-check-ios-release-identity-proposal.sh scripts/ios/test-apply-ios-release-identifiers.sh scripts/ios/test-check-ios-generic-device-build.sh scripts/ios/test-run-ios-physical-device-smoke.sh scripts/ios/test-export-ios-release-identity-state.sh scripts/ios/test-export-ios-device-readiness-state.sh scripts/ios/test-check-ios-simulator-real-core-route.sh scripts/ios/test-probe-x86_64-simulator-real-core.sh scripts/ios/test-export-real-core-route-state.sh scripts/ios/test-export-nome-ios-completion-state.sh scripts/ios/test-real-core-source-audit.sh scripts/ios/test-prepare-real-core-safety.sh scripts/ios/test-prepare-device-real-core.sh scripts/ios/test-stage-real-core-artifacts.sh scripts/ios/test-run-real-core-batch0.sh scripts/ios/check-app-store-screenshots.sh scripts/ios/check-app-store-final-blocker-plan.sh scripts/ios/test-app-store-final-blocker-plan.sh scripts/ios/export-app-store-final-screenshot-state.sh scripts/ios/test-export-app-store-final-screenshot-state.sh scripts/ios/prepare-app-store-final-screenshot-package.sh scripts/ios/test-prepare-app-store-final-screenshot-package.sh scripts/ios/check-nome-brand-copy.sh scripts/ios/check-nome-design-coverage.sh scripts/ios/export-nome-design-evidence-state.sh scripts/ios/test-export-nome-design-evidence-state.sh scripts/ios/check-nome-smoke-visual-quality.sh scripts/ios/test-check-nome-smoke-visual-quality.sh scripts/ios/check-nome-manual-qa-status.sh scripts/ios/check-nome-qa-execution-plan.sh scripts/ios/export-nome-qa-batches.sh scripts/ios/test-export-nome-qa-batches.sh scripts/ios/export-nome-qa-execution-state.sh scripts/ios/test-export-nome-qa-execution-state.sh scripts/ios/check-nome-ios-goal-audit.sh scripts/ios/export-app-store-screenshot-draft.sh scripts/ios/smoke-nome-ui.sh scripts/ios/check-nome-ios-readiness.sh'
accessibility_script_syntax_cmd='bash -n scripts/ios/capture-nome-accessibility-previews.sh scripts/ios/test-capture-nome-accessibility-previews.sh scripts/ios/test-check-ios-device-readiness.sh'

run_shell_check "FAIL" "script_syntax" "$script_syntax_cmd"
run_shell_check "FAIL" "accessibility_script_syntax" "$accessibility_script_syntax_cmd"
run_shell_check "FAIL" "real_core_arch_unit" "scripts/ios/test-check-real-core-arch.sh"
run_shell_check "FAIL" "real_core_xcode_sync_unit" "scripts/ios/test-check-real-core-xcode-sync.sh"
run_shell_check "FAIL" "release_identifier_unit" "scripts/ios/test-check-ios-release-identifiers.sh"
run_shell_check "FAIL" "release_identity_state_unit" "scripts/ios/test-export-ios-release-identity-state.sh"
run_shell_check "FAIL" "release_identity_proposal_unit" "scripts/ios/test-check-ios-release-identity-proposal.sh"
run_shell_check "FAIL" "release_identity_apply_unit" "scripts/ios/test-apply-ios-release-identifiers.sh"
run_shell_check "FAIL" "device_readiness_state_unit" "scripts/ios/test-export-ios-device-readiness-state.sh"
run_shell_check "FAIL" "generic_device_build_unit" "scripts/ios/test-check-ios-generic-device-build.sh"
run_shell_check "FAIL" "physical_device_smoke_unit" "scripts/ios/test-run-ios-physical-device-smoke.sh"
run_shell_check "FAIL" "ios_preview_tooling_unit" "scripts/ios/test-check-ios-preview-tooling.sh"
run_shell_check "FAIL" "accessibility_preview_capture_unit" "scripts/ios/test-capture-nome-accessibility-previews.sh"
run_shell_check "FAIL" "device_readiness_parser_unit" "scripts/ios/test-check-ios-device-readiness.sh"
run_shell_check "FAIL" "completion_state_unit" "scripts/ios/test-export-nome-ios-completion-state.sh"
run_shell_check "FAIL" "simulator_real_core_route_unit" "scripts/ios/test-check-ios-simulator-real-core-route.sh"
run_shell_check "FAIL" "x86_64_sim_real_core_probe_unit" "scripts/ios/test-probe-x86_64-simulator-real-core.sh"
run_shell_check "FAIL" "real_core_route_state_unit" "scripts/ios/test-export-real-core-route-state.sh"
run_shell_check "FAIL" "real_core_source_audit_unit" "env -u SOURCE_TARGET scripts/ios/test-real-core-source-audit.sh"
run_shell_check "FAIL" "prepare_real_core_safety_unit" "scripts/ios/test-prepare-real-core-safety.sh"
run_shell_check "FAIL" "prepare_device_real_core_unit" "scripts/ios/test-prepare-device-real-core.sh"
run_shell_check "FAIL" "stage_real_core_artifacts_unit" "scripts/ios/test-stage-real-core-artifacts.sh"
run_shell_check "FAIL" "run_real_core_batch0_unit" "scripts/ios/test-run-real-core-batch0.sh"
run_shell_check "FAIL" "app_store_final_blocker_plan_unit" "scripts/ios/test-app-store-final-blocker-plan.sh"
run_shell_check "FAIL" "app_store_final_screenshot_state_unit" "scripts/ios/test-export-app-store-final-screenshot-state.sh"
run_shell_check "FAIL" "app_store_final_package_unit" "scripts/ios/test-prepare-app-store-final-screenshot-package.sh"
run_shell_check "FAIL" "qa_batch_export_unit" "scripts/ios/test-export-nome-qa-batches.sh"
run_shell_check "FAIL" "qa_execution_state_unit" "scripts/ios/test-export-nome-qa-execution-state.sh"
run_shell_check "FAIL" "diff_hygiene" "git diff --check"
run_shell_check "FAIL" "zh_hans_localization_lint" "plutil -lint apps/ios/zh-Hans.lproj/Localizable.strings apps/ios/zh-Hans.lproj/SimpleX--iOS--InfoPlist.strings"
run_shell_check "FAIL" "ios_preview_tooling" "scripts/ios/check-ios-preview-tooling.sh"
run_shell_check "FAIL" "nome_brand_copy" "scripts/ios/check-nome-brand-copy.sh"
run_shell_check "FAIL" "nome_design_coverage" "scripts/ios/check-nome-design-coverage.sh"
run_shell_check "FAIL" "design_evidence_state_unit" "scripts/ios/test-export-nome-design-evidence-state.sh"
run_shell_check "FAIL" "smoke_visual_quality_unit" "scripts/ios/test-check-nome-smoke-visual-quality.sh"
run_shell_check "FAIL" "qa_execution_plan" "scripts/ios/check-nome-qa-execution-plan.sh"
printf -v qa_batches_dir_q '%q' "$output_dir/qa_batches"
run_shell_check "FAIL" "qa_batch_export" "scripts/ios/export-nome-qa-batches.sh --output $qa_batches_dir_q --force"
printf -v qa_execution_state_dir_q '%q' "$output_dir/qa_execution_state"
run_shell_check "FAIL" "qa_execution_state" "scripts/ios/export-nome-qa-execution-state.sh --output $qa_execution_state_dir_q --force"
printf -v manual_qa_tsv_q '%q' "$output_dir/manual_qa_unchecked.tsv"
run_shell_check "BLOCKED" "manual_qa_status" "scripts/ios/check-nome-manual-qa-status.sh --write-tsv $manual_qa_tsv_q"
run_shell_check "FAIL" "app_store_screenshot_candidates" "scripts/ios/check-app-store-screenshots.sh"
run_shell_check "FAIL" "app_store_final_blocker_plan" "scripts/ios/check-app-store-final-blocker-plan.sh"
printf -v app_store_final_screenshot_state_dir_q '%q' "$output_dir/app_store_final_screenshot_state"
run_shell_check "FAIL" "app_store_final_screenshot_state" "scripts/ios/export-app-store-final-screenshot-state.sh --output $app_store_final_screenshot_state_dir_q --force"
run_shell_check "BLOCKED" "app_store_screenshot_final" "scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens"
run_shell_check "BLOCKED" "real_core_xcode_sync" "scripts/ios/check-real-core-xcode-sync.sh"
printf -v real_core_route_state_dir_q '%q' "$output_dir/real_core_route_state"
run_shell_check "FAIL" "real_core_route_state" "scripts/ios/export-real-core-route-state.sh --output $real_core_route_state_dir_q --force"
real_core_preflight_cmd="scripts/ios/check-real-core.sh"
if [ "$source_target" = "physical-device" ]; then
  real_core_preflight_cmd="$real_core_preflight_cmd --target physical-device"
elif [ "$source_target" = "simulator" ]; then
  real_core_preflight_cmd="$real_core_preflight_cmd --target simulator"
fi
run_shell_check "BLOCKED" "real_core_preflight" "$real_core_preflight_cmd"
run_shell_check "BLOCKED" "simulator_real_core_route" "scripts/ios/check-ios-simulator-real-core-route.sh"
real_core_build_env_cmd="scripts/ios/check-real-core-build-env.sh"
if [ "$source_target" = "physical-device" ]; then
  real_core_build_env_cmd="$real_core_build_env_cmd --target physical-device --allow-downloaded-artifacts"
elif [ "$source_target" = "simulator" ]; then
  real_core_build_env_cmd="$real_core_build_env_cmd --target simulator"
fi
run_shell_check "BLOCKED" "real_core_build_env" "$real_core_build_env_cmd"
effective_generic_device_build_dir=""
if [ "$run_generic_device_build" -eq 1 ]; then
  printf -v generic_device_build_dir_q '%q' "$output_dir/generic_device_build"
  printf -v generic_device_build_derived_q '%q' "$output_dir/generic_device_derived"
  run_shell_check "BLOCKED" "generic_device_build" "scripts/ios/check-ios-generic-device-build.sh --derived-data $generic_device_build_derived_q --output $generic_device_build_dir_q --force"
  effective_generic_device_build_dir="$output_dir/generic_device_build"
elif [ -n "$generic_device_build_dir" ]; then
  record_generic_device_build_evidence "$generic_device_build_dir"
  effective_generic_device_build_dir="$generic_device_build_dir"
else
  write_warn "generic_device_build" "Skipped. Re-run with --generic-device-build to compile the generic iOS device target with code signing disabled."
fi
printf -v device_readiness_state_dir_q '%q' "$output_dir/device_readiness_state"
device_readiness_state_cmd="scripts/ios/export-ios-device-readiness-state.sh --output $device_readiness_state_dir_q --force"
if [ -n "$effective_generic_device_build_dir" ]; then
  printf -v effective_generic_device_build_dir_q '%q' "$effective_generic_device_build_dir"
  device_readiness_state_cmd="$device_readiness_state_cmd --generic-device-build-dir $effective_generic_device_build_dir_q"
fi
run_shell_check "FAIL" "device_readiness_state" "$device_readiness_state_cmd"
run_shell_check "BLOCKED" "physical_device_readiness" "scripts/ios/check-ios-device-readiness.sh"
printf -v physical_device_smoke_dir_q '%q' "$output_dir/physical_device_smoke"
run_shell_check "BLOCKED" "physical_device_smoke" "scripts/ios/run-ios-physical-device-smoke.sh --output $physical_device_smoke_dir_q --force"
run_shell_check "FAIL" "release_identifiers_compatibility_review" "scripts/ios/check-ios-release-identifiers.sh --compatibility-reviewed"
printf -v release_identity_state_dir_q '%q' "$output_dir/release_identity_state"
run_shell_check "FAIL" "release_identity_state" "scripts/ios/export-ios-release-identity-state.sh --output $release_identity_state_dir_q --force"
run_shell_check "BLOCKED" "release_identifiers" "scripts/ios/check-ios-release-identifiers.sh"

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
  run_shell_check "BLOCKED" "real_core_source_audit" "$source_audit_cmd"
else
  write_warn "real_core_source_audit" "Skipped. Re-run with --source-audit to check local, GitHub, Actions, and optional Hydra artifact sources."
fi

if [ "$run_smoke" -eq 1 ]; then
  smoke_args=(--output "$output_dir/smoke")

  if [ -n "$simulator_id" ]; then
    smoke_args+=(--simulator "$simulator_id")
  fi

  if [ "$skip_smoke_build" -eq 1 ]; then
    smoke_args+=(--skip-build)
  fi

  smoke_log="$output_dir/nome_ui_smoke.log"
  printf '[INFO] Running nome_ui_smoke\n'
  printf '$ scripts/ios/smoke-nome-ui.sh' > "$smoke_log"
  for arg in "${smoke_args[@]}"; do
    printf ' %q' "$arg" >> "$smoke_log"
  done
  printf '\n\n' >> "$smoke_log"

  if scripts/ios/smoke-nome-ui.sh "${smoke_args[@]}" >> "$smoke_log" 2>&1; then
    record "PASS" "nome_ui_smoke" "$smoke_log"
  else
    record "FAIL" "nome_ui_smoke" "$smoke_log"
  fi
elif [ -n "$smoke_manifest" ]; then
  check_smoke_manifest "$smoke_manifest"
else
  write_warn "nome_ui_smoke" "Skipped. Re-run with --smoke to capture the simulator UI smoke set, or pass --smoke-manifest FILE to reuse an existing manifest."
fi

design_evidence_manifest=""
if [ "$run_smoke" -eq 1 ] && [ -f "$output_dir/smoke/manifest.tsv" ]; then
  design_evidence_manifest="$output_dir/smoke/manifest.tsv"
elif [ -n "$smoke_manifest" ]; then
  design_evidence_manifest="$smoke_manifest"
fi

if [ -n "$design_evidence_manifest" ]; then
  printf -v design_evidence_manifest_q '%q' "$design_evidence_manifest"
  printf -v smoke_visual_quality_dir_q '%q' "$output_dir/smoke_visual_quality"
  run_shell_check "FAIL" "smoke_visual_quality" "scripts/ios/check-nome-smoke-visual-quality.sh --manifest $design_evidence_manifest_q --expected-count 14 --output $smoke_visual_quality_dir_q --force"
  printf -v design_evidence_dir_q '%q' "$output_dir/design_evidence_state"
  run_shell_check "FAIL" "design_evidence_state" "scripts/ios/export-nome-design-evidence-state.sh --smoke-manifest $design_evidence_manifest_q --output $design_evidence_dir_q --force"
else
  write_warn "smoke_visual_quality" "Skipped. Pass --smoke-manifest FILE or run --smoke to verify screenshot PNG metadata against the smoke manifest."
  write_warn "design_evidence_state" "Skipped. Pass --smoke-manifest FILE or run --smoke to export design-to-screenshot evidence."
fi

fail_count="$(awk -F '\t' 'NR > 1 && $1 == "FAIL" {count++} END {print count + 0}' "$summary")"
blocked_count="$(awk -F '\t' 'NR > 1 && $1 == "BLOCKED" {count++} END {print count + 0}' "$summary")"
warn_count="$(awk -F '\t' 'NR > 1 && $1 == "WARN" {count++} END {print count + 0}' "$summary")"
pass_count="$(awk -F '\t' 'NR > 1 && $1 == "PASS" {count++} END {print count + 0}' "$summary")"

cat <<EOF

Nome iOS readiness summary
==========================
Report: $output_dir
PASS: $pass_count
WARN: $warn_count
BLOCKED: $blocked_count
FAIL: $fail_count

EOF

sed -n '1,160p' "$summary"

if [ "$fail_count" -gt 0 ]; then
  exit 1
fi

if [ "$blocked_count" -gt 0 ] && [ "$allow_blockers" -ne 1 ]; then
  exit 1
fi

exit 0
