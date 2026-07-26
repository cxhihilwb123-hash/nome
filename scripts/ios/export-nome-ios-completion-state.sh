#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
output_dir="/tmp/nome-ios-completion-state-$(date +%Y%m%d-%H%M%S)"
smoke_manifest=""
generic_device_build_dir=""
force=0
skip_gates=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/export-nome-ios-completion-state.sh [options]

Exports a top-level completion packet for the Nome iOS goal. This is a read-only
audit: it does not install libraries, build artifacts, modify Xcode settings,
mark manual QA items complete, or change release identifiers.

Options:
  --output DIR             Output directory.
  --smoke-manifest FILE    Existing smoke manifest used for design evidence.
  --generic-device-build-dir DIR
                           Existing check-ios-generic-device-build output
                           directory to pass into device readiness.
  --force                  Replace an existing output directory.
  --skip-gates             Do not run local gates; write WARN fixture rows.
  -h, --help               Show this help.
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
    --smoke-manifest)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --smoke-manifest requires a file" >&2
        exit 2
      fi
      smoke_manifest="$1"
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
    --skip-gates)
      skip_gates=1
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
requirements="$output_dir/completion_requirements.tsv"
gate_status="$output_dir/gate_status.tsv"

record_gate() {
  local status="$1"
  local name="$2"
  local log="$3"
  printf '%s\t%s\t%s\n' "$status" "$name" "$log" >> "$gate_status"
}

record_requirement() {
  local requirement="$1"
  local status="$2"
  local evidence="$3"
  local unblock="$4"
  printf '%s\t%s\t%s\t%s\n' "$requirement" "$status" "$evidence" "$unblock" >> "$requirements"
}

run_gate() {
  local status_on_fail="$1"
  local name="$2"
  local command="$3"
  local log="$output_dir/logs/$name.log"

  if [ "$skip_gates" -eq 1 ]; then
    printf 'Skipped by --skip-gates. Intended command:\n%s\n' "$command" > "$log"
    record_gate "WARN" "$name" "$log"
    return 0
  fi

  printf '$ %s\n\n' "$command" > "$log"
  if bash -c "$command" >> "$log" 2>&1; then
    record_gate "PASS" "$name" "$log"
    return 0
  fi

  record_gate "$status_on_fail" "$name" "$log"
  return 1
}

value_from_tsv() {
  local file="$1"
  local key="$2"
  awk -F '\t' -v key="$key" 'NR > 1 && $1 == key {print $2; exit}' "$file" 2>/dev/null
}

printf 'status\tcheck\tlog\n' > "$gate_status"
printf 'requirement\tstatus\tevidence\tunblock\n' > "$requirements"

if [ "$skip_gates" -eq 1 ]; then
  record_requirement "planning_records" "WARN" "$requirements" "Run without --skip-gates to verify plan, QA, and design evidence files."
  record_requirement "visual_design_and_smoke" "WARN" "$requirements" "Run with --smoke-manifest FILE from scripts/ios/smoke-nome-ui.sh."
  record_requirement "manual_qa" "WARN" "$requirements" "Run scripts/ios/check-nome-manual-qa-status.sh."
  record_requirement "real_core_runtime" "WARN" "$requirements" "Run scripts/ios/check-real-core.sh and export the real-core route state."
  record_requirement "physical_device_path" "WARN" "$requirements" "Run scripts/ios/export-ios-device-readiness-state.sh."
  record_requirement "final_app_store_screenshots" "WARN" "$requirements" "Run scripts/ios/export-app-store-final-screenshot-state.sh."
  record_requirement "release_identifiers" "WARN" "$requirements" "Run scripts/ios/export-ios-release-identity-state.sh."
  record_requirement "preview_tooling" "WARN" "$requirements" "Run scripts/ios/check-ios-preview-tooling.sh."
else
  if [ -f "$root_dir/plans/20260709_nome_ios_remaining_qa_execution_plan.md" ] &&
    [ -f "$root_dir/plans/20260709_nome_ios_qa_record.md" ] &&
    [ -f "$root_dir/plans/20260709_nome_ios_manual_qa_checklist.md" ] &&
    [ -f "$root_dir/plans/20260709_nome_ios_real_core_testing_plan.md" ] &&
    [ -f "$root_dir/plans/20260709_nome_ios_design_coverage_matrix.md" ]; then
    record_requirement "planning_records" "PASS" "$root_dir/plans" "Keep records current as blockers are resolved."
  else
    record_requirement "planning_records" "FAIL" "$root_dir/plans" "Restore the Nome iOS plan, QA, checklist, real-core, and design matrix records."
  fi

  if [ -n "$smoke_manifest" ] && [ -f "$smoke_manifest" ]; then
    design_dir="$output_dir/design_evidence_state"
    if run_gate "FAIL" "design_evidence_state" "scripts/ios/export-nome-design-evidence-state.sh --smoke-manifest '$smoke_manifest' --output '$design_dir' --force"; then
      missing_rows="$(value_from_tsv "$design_dir/summary.tsv" missing_rows)"
      coverage_rows="$(value_from_tsv "$design_dir/summary.tsv" coverage_rows)"
      smoke_rows="$(value_from_tsv "$design_dir/summary.tsv" smoke_label_rows)"
      if [ "${missing_rows:-1}" = "0" ] && [ "${coverage_rows:-0}" = "7" ] && [ "${smoke_rows:-0}" = "14" ]; then
        record_requirement "visual_design_and_smoke" "PASS" "$design_dir" "Keep using the latest build-and-smoke manifest for visual acceptance."
      else
        record_requirement "visual_design_and_smoke" "FAIL" "$design_dir" "Regenerate the 14-screen smoke manifest and design evidence export."
      fi
    else
      record_requirement "visual_design_and_smoke" "FAIL" "$design_dir" "Fix the smoke manifest or design evidence mapping."
    fi
  else
    record_requirement "visual_design_and_smoke" "WARN" "${smoke_manifest:-missing}" "Run scripts/ios/smoke-nome-ui.sh and pass its manifest with --smoke-manifest."
  fi

  manual_queue="$output_dir/manual_qa_unchecked.tsv"
  if run_gate "BLOCKED" "manual_qa_status" "scripts/ios/check-nome-manual-qa-status.sh --write-tsv '$manual_queue'"; then
    record_requirement "manual_qa" "PASS" "$manual_queue" "Manual QA checklist is complete."
  else
    unchecked_count="$(tail -n +2 "$manual_queue" 2>/dev/null | awk 'NF {count++} END {print count + 0}')"
    record_requirement "manual_qa" "BLOCKED" "$manual_queue" "Complete the remaining ${unchecked_count:-unknown} manual QA items with real-core evidence."
  fi

  real_core_log="$output_dir/logs/real_core_preflight.log"
  if run_gate "BLOCKED" "real_core_preflight" "scripts/ios/check-real-core.sh"; then
    record_requirement "real_core_runtime" "PASS" "$real_core_log" "Run real first-run, connection, group, messaging, and data QA."
  else
    route_dir="$output_dir/real_core_route_state"
    run_gate "FAIL" "real_core_route_state" "scripts/ios/export-real-core-route-state.sh --output '$route_dir' --force" >/dev/null || true
    record_requirement "real_core_runtime" "BLOCKED" "$route_dir" "Resolve arm64 simulator core, x86_64 simulator runtime, physical-device path, or local Nix build."
  fi

  device_dir="$output_dir/device_readiness_state"
  device_readiness_cmd="scripts/ios/export-ios-device-readiness-state.sh --output '$device_dir' --force"
  if [ -n "$generic_device_build_dir" ]; then
    if [ ! -d "$generic_device_build_dir" ]; then
      echo "[FAIL] Supplied generic device build directory does not exist: $generic_device_build_dir" >&2
      exit 1
    fi
    printf -v generic_device_build_dir_q '%q' "$generic_device_build_dir"
    device_readiness_cmd="$device_readiness_cmd --generic-device-build-dir $generic_device_build_dir_q"
  fi

  if run_gate "FAIL" "device_readiness_state" "$device_readiness_cmd"; then
    device_blocked="$(value_from_tsv "$device_dir/summary.tsv" device_area_blocked_count)"
    device_warn="$(value_from_tsv "$device_dir/summary.tsv" device_area_warn_count)"
    if [ "${device_blocked:-1}" = "0" ]; then
      if [ "${device_warn:-0}" = "0" ]; then
        record_requirement "physical_device_path" "PASS" "$device_dir" "Run physical-device launch and camera/QR QA."
      else
        record_requirement "physical_device_path" "WARN" "$device_dir" "Review ${device_warn:-unknown} device-readiness warning(s) before physical-device launch and camera/QR QA."
      fi
    else
      record_requirement "physical_device_path" "BLOCKED" "$device_dir" "Resolve ${device_blocked:-unknown} blocked device-readiness area(s) and review ${device_warn:-0} warning area(s), including connected device, installed device libraries, and project-reference compatibility as applicable."
    fi
  else
    record_requirement "physical_device_path" "FAIL" "$device_dir" "Fix the physical-device readiness export."
  fi

  screenshots_dir="$output_dir/app_store_final_screenshot_state"
  if run_gate "FAIL" "app_store_final_screenshot_state" "scripts/ios/export-app-store-final-screenshot-state.sh --output '$screenshots_dir' --force"; then
    strict_status="$(value_from_tsv "$screenshots_dir/summary.tsv" strict_final_status)"
    needs_real_core="$(value_from_tsv "$screenshots_dir/summary.tsv" needs_real_core_count)"
    if [ "$strict_status" = "PASS" ]; then
      record_requirement "final_app_store_screenshots" "PASS" "$screenshots_dir" "Final screenshot package is upload-ready."
    else
      record_requirement "final_app_store_screenshots" "BLOCKED" "$screenshots_dir" "Replace ${needs_real_core:-unknown} needs-real-core screenshot(s), update manifest, and pass strict final screenshot gate."
    fi
  else
    record_requirement "final_app_store_screenshots" "FAIL" "$screenshots_dir" "Fix the final screenshot state export."
  fi

  release_dir="$output_dir/release_identity_state"
  if run_gate "FAIL" "release_identity_state" "scripts/ios/export-ios-release-identity-state.sh --output '$release_dir' --force"; then
    strict_release="$(value_from_tsv "$release_dir/summary.tsv" strict_release_status)"
    required_decisions="$(value_from_tsv "$release_dir/summary.tsv" required_decision_rows)"
    if [ "$strict_release" = "PASS" ]; then
      record_requirement "release_identifiers" "PASS" "$release_dir" "Release identifiers are ready for TestFlight/public distribution."
    else
      record_requirement "release_identifiers" "BLOCKED" "$release_dir" "Resolve ${required_decisions:-unknown} release identifier/capability decision row(s)."
    fi
  else
    record_requirement "release_identifiers" "FAIL" "$release_dir" "Fix the release identity state export."
  fi

  preview_log="$output_dir/logs/ios_preview_tooling.log"
  if run_gate "FAIL" "ios_preview_tooling" "scripts/ios/check-ios-preview-tooling.sh"; then
    if grep -q '^\[WARN\]' "$preview_log"; then
      record_requirement "preview_tooling" "WARN" "$preview_log" "XcodeBuildMCP may need system xcode-select switched to full Xcode; shell smoke remains available with DEVELOPER_DIR."
    else
      record_requirement "preview_tooling" "PASS" "$preview_log" "MCP and shell preview tooling are both clean."
    fi
  else
    record_requirement "preview_tooling" "FAIL" "$preview_log" "Fix Xcode/simctl preview tooling before relying on simulator automation."
  fi
fi

pass_count="$(awk -F '\t' 'NR > 1 && $2 == "PASS" {count++} END {print count + 0}' "$requirements")"
warn_count="$(awk -F '\t' 'NR > 1 && $2 == "WARN" {count++} END {print count + 0}' "$requirements")"
blocked_count="$(awk -F '\t' 'NR > 1 && $2 == "BLOCKED" {count++} END {print count + 0}' "$requirements")"
fail_count="$(awk -F '\t' 'NR > 1 && $2 == "FAIL" {count++} END {print count + 0}' "$requirements")"
gate_fail_count="$(awk -F '\t' 'NR > 1 && $1 == "FAIL" {count++} END {print count + 0}' "$gate_status")"
gate_blocked_count="$(awk -F '\t' 'NR > 1 && $1 == "BLOCKED" {count++} END {print count + 0}' "$gate_status")"

printf 'key\tvalue\n' > "$summary"
printf 'pass_count\t%s\n' "$pass_count" >> "$summary"
printf 'warn_count\t%s\n' "$warn_count" >> "$summary"
printf 'blocked_count\t%s\n' "$blocked_count" >> "$summary"
printf 'fail_count\t%s\n' "$fail_count" >> "$summary"
printf 'gate_fail_count\t%s\n' "$gate_fail_count" >> "$summary"
printf 'gate_blocked_count\t%s\n' "$gate_blocked_count" >> "$summary"
printf 'completion_requirements\t%s\n' "$requirements" >> "$summary"
printf 'gate_status\t%s\n' "$gate_status" >> "$summary"
if [ -n "$generic_device_build_dir" ]; then
  printf 'generic_device_build_dir\t%s\n' "$generic_device_build_dir" >> "$summary"
fi

cat > "$output_dir/README.md" <<EOF
# Nome iOS Completion State

This packet summarizes whether the Nome iOS goal can be claimed complete.

Summary:

- PASS requirements: $pass_count
- WARN requirements: $warn_count
- BLOCKED requirements: $blocked_count
- FAIL requirements: $fail_count
- gate blockers: $gate_blocked_count

Files:

- summary.tsv
- completion_requirements.tsv
- gate_status.tsv
- logs/
EOF

echo "Nome iOS completion state"
echo "========================="
echo "[INFO] Output: $output_dir"
echo "[INFO] PASS requirements: $pass_count"
echo "[INFO] WARN requirements: $warn_count"
echo "[INFO] BLOCKED requirements: $blocked_count"
echo "[INFO] FAIL requirements: $fail_count"
sed -n '1,80p' "$requirements"
echo "[PASS] Exported Nome iOS completion state"

if [ "$fail_count" -gt 0 ] || [ "$gate_fail_count" -gt 0 ]; then
  exit 1
fi

exit 0
