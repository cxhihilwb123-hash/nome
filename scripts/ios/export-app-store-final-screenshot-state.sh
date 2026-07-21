#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
draft_dir="$root_dir/design/app-store/ios-upload-draft-screens"
checklist="$root_dir/plans/20260709_nome_ios_manual_qa_checklist.md"
execution_plan="$root_dir/plans/20260709_nome_ios_remaining_qa_execution_plan.md"
output_dir="/tmp/nome-ios-app-store-final-screenshot-state-$(date +%Y%m%d-%H%M%S)"
force=0
skip_gates=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/export-app-store-final-screenshot-state.sh [options]

Exports a read-only App Store screenshot finalization packet for Nome iOS. It
does not edit screenshots, manifests, or blocker ledgers.

Options:
  --dir DIR             Draft screenshot directory.
  --checklist FILE      Manual QA checklist for replacement anchors.
  --execution-plan FILE Remaining QA execution plan.
  --output DIR          Output directory.
  --force               Replace an existing output directory.
  --skip-gates          Do not run screenshot gates; write WARN rows instead.
  -h, --help            Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --dir)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --dir requires a directory" >&2
        exit 2
      fi
      draft_dir="$1"
      ;;
    --checklist)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --checklist requires a file" >&2
        exit 2
      fi
      checklist="$1"
      ;;
    --execution-plan)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --execution-plan requires a file" >&2
        exit 2
      fi
      execution_plan="$1"
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

if [ ! -d "$draft_dir" ]; then
  echo "[FAIL] Screenshot directory not found: $draft_dir" >&2
  exit 1
fi

manifest="$draft_dir/MANIFEST.md"
blockers="$draft_dir/FINAL_BLOCKERS.md"

if [ ! -f "$manifest" ]; then
  echo "[FAIL] Screenshot manifest not found: $manifest" >&2
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
screenshot_status="$output_dir/screenshot_status.tsv"
replacement_plan="$output_dir/replacement_plan.tsv"
next_actions="$output_dir/next_actions.tsv"
route_state_dir="$output_dir/real_core_route_state"

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

qa_anchor_for_file() {
  case "$1" in
    05-add-friend-needs-real-core.jpg)
      printf 'Real core creates a valid one-time link.'
      ;;
    06-public-contact-needs-real-core.jpg)
      printf 'Real core creates or loads a reusable public contact address.'
      ;;
    07-join-group-needs-real-core.jpg)
      printf 'Real group invitation links show a preview before joining.'
      ;;
    08-conversation-needs-real-core.jpg)
      printf 'A real one-to-one conversation sends and receives text.'
      ;;
    *)
      printf 'Review manual QA checklist for this screenshot.'
      ;;
  esac
}

batch_for_file() {
  case "$1" in
    05-add-friend-needs-real-core.jpg|06-public-contact-needs-real-core.jpg)
      printf 'batch-2'
      ;;
    07-join-group-needs-real-core.jpg|08-conversation-needs-real-core.jpg)
      printf 'batch-3'
      ;;
    *)
      printf 'batch-6'
      ;;
  esac
}

blocker_group_for_file() {
  case "$1" in
    05-add-friend-needs-real-core.jpg)
      printf 'real_core_functional'
      ;;
    06-public-contact-needs-real-core.jpg)
      printf 'real_core_functional'
      ;;
    07-join-group-needs-real-core.jpg)
      printf 'group_real_core'
      ;;
    08-conversation-needs-real-core.jpg)
      printf 'multi_account_real_core'
      ;;
    *)
      printf 'manual_followup'
      ;;
  esac
}

replacement_action_for_file() {
  case "$1" in
    05-add-friend-needs-real-core.jpg)
      printf 'Capture a real one-time invitation link and QR code from the real iOS core.'
      ;;
    06-public-contact-needs-real-core.jpg)
      printf 'Capture a real public contact address page with copy/share actions backed by the real iOS core.'
      ;;
    07-join-group-needs-real-core.jpg)
      printf 'Capture a real group invite preview or join path from a real group invitation link.'
      ;;
    08-conversation-needs-real-core.jpg)
      printf 'Capture a real two-account conversation after send and receive text succeeds.'
      ;;
    *)
      printf 'Replace this screenshot with real app evidence and update the manifest.'
      ;;
  esac
}

qa_location_for_anchor() {
  local anchor="$1"

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

printf 'status\tcheck\tlog\n' > "$gate_status"
printf 'slot\tfile\tsource_file\tdimensions\tstatus\tfinal_state\tnote\n' > "$screenshot_status"

awk -F '|' '
function trim(value) {
  gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
  gsub(/`/, "", value)
  return value
}
/^\|[[:space:]]*[0-9]+[[:space:]]*\|/ {
  slot = trim($2)
  file = trim($3)
  source = trim($4)
  dimensions = trim($5)
  status = trim($8)
  note = trim($9)
  final_state = "candidate_ready"
  lower = tolower(file " " status " " note)
  if (lower ~ /needs-real-core|preview|debug|replace/) {
    final_state = "final_blocked"
  }
  printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\n", slot, file, source, dimensions, status, final_state, note
}
' "$manifest" >> "$screenshot_status"

printf 'slot\tfile\tsource_candidate\tstatus\treplacement_evidence_required\tmanual_qa_anchor\tmanual_qa_line\tmanual_qa_section\tblocker_group\tbatch_id\taction\n' > "$replacement_plan"

if [ -f "$blockers" ]; then
  while IFS=$'\t' read -r slot file source_candidate status evidence; do
    [ -n "$slot" ] || continue
    anchor="$(qa_anchor_for_file "$file")"
    qa_location="$(qa_location_for_anchor "$anchor")"
    manual_qa_line="$(printf '%s\n' "$qa_location" | cut -f1)"
    manual_qa_section="$(printf '%s\n' "$qa_location" | cut -f2)"
    blocker_group="$(blocker_group_for_file "$file")"
    batch_id="$(batch_for_file "$file")"
    action="$(replacement_action_for_file "$file")"
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$slot" "$file" "$source_candidate" "$status" "$evidence" "$anchor" "$manual_qa_line" "$manual_qa_section" "$blocker_group" "$batch_id" "$action" >> "$replacement_plan"
  done < <(
    awk -F '|' '
    function trim(value) {
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      gsub(/`/, "", value)
      return value
    }
    /^\|[[:space:]]*[0-9]+[[:space:]]*\|/ {
      printf "%s\t%s\t%s\t%s\t%s\n", trim($2), trim($3), trim($4), trim($5), trim($6)
    }
    ' "$blockers"
  )
fi

replacement_count="$(tail -n +2 "$replacement_plan" | awk 'NF {count++} END {print count + 0}')"
needs_real_core_count="$(awk -F '\t' 'NR > 1 && ($2 ~ /needs-real-core/ || $5 == "needs-real-core" || $6 == "final_blocked") {count++} END {print count + 0}' "$screenshot_status")"
ready_count="$(awk -F '\t' 'NR > 1 && $5 == "ready" && $6 == "candidate_ready" {count++} END {print count + 0}' "$screenshot_status")"
total_count="$(tail -n +2 "$screenshot_status" | awk 'NF {count++} END {print count + 0}')"

route_state_log="$output_dir/logs/real_core_route_state.log"
route_ready_count="unknown"
route_state_status="WARN"

if [ "$skip_gates" -eq 1 ]; then
  printf 'Skipped by --skip-gates. Intended command:\nscripts/ios/export-real-core-route-state.sh --output %s --force\n' "$route_state_dir" > "$route_state_log"
  record_gate "WARN" "real_core_route_state" "$route_state_log"
else
  printf '$ scripts/ios/export-real-core-route-state.sh --output %s --force\n\n' "$route_state_dir" > "$route_state_log"
  if scripts/ios/export-real-core-route-state.sh --output "$route_state_dir" --force >> "$route_state_log" 2>&1; then
    route_state_status="PASS"
    record_gate "PASS" "real_core_route_state" "$route_state_log"
    if [ -f "$route_state_dir/summary.tsv" ]; then
      route_ready_count="$(awk -F '\t' 'NR > 1 && $1 == "route_ready_count" {print $2; exit}' "$route_state_dir/summary.tsv")"
      route_ready_count="${route_ready_count:-unknown}"
    fi
  else
    route_state_status="FAIL"
    record_gate "FAIL" "real_core_route_state" "$route_state_log"
  fi
fi

if [ "$replacement_count" -gt 0 ] && [ "${route_ready_count:-unknown}" = "0" ]; then
  record_gate "BLOCKED" "real_core_route_prerequisite" "$route_state_log"
elif [ "$replacement_count" -gt 0 ] && [ "${route_ready_count:-unknown}" = "unknown" ]; then
  record_gate "WARN" "real_core_route_prerequisite" "$route_state_log"
else
  record_gate "PASS" "real_core_route_prerequisite" "$route_state_log"
fi

printf 'priority\titem\taction\tcommand\n' > "$next_actions"

if [ "$replacement_count" -gt 0 ]; then
  priority=0
  if [ "${route_ready_count:-unknown}" != "1" ] && [ "${route_ready_count:-unknown}" != "2" ] && [ "${route_ready_count:-unknown}" != "3" ] && [ "${route_ready_count:-unknown}" != "4" ]; then
    priority=$((priority + 1))
    printf '%s\treal_core_route_prerequisite\t%s\t%s\n' "$priority" "Resolve Batch 0 first: the screenshot replacements require a READY real-core route. Current ready route count: ${route_ready_count:-unknown}." "scripts/ios/export-real-core-route-state.sh --output /tmp/nome-ios-real-core-route-state-current --force" >> "$next_actions"
  fi
  while IFS=$'\t' read -r slot file _source _status _evidence anchor manual_qa_line manual_qa_section blocker_group batch_id action; do
    [ -n "$slot" ] || continue
    priority=$((priority + 1))
    printf '%s\t%s\t%s\t%s\n' "$priority" "$file" "$action Evidence anchor: $anchor Checklist: line $manual_qa_line, $manual_qa_section, $blocker_group. Batch: $batch_id." "scripts/ios/check-app-store-final-blocker-plan.sh" >> "$next_actions"
  done < <(tail -n +2 "$replacement_plan")

  priority=$((priority + 1))
  printf '%s\tfinal_package_proposal\t%s\t%s\n' "$priority" "After capturing the four real-core screenshots, place them in one directory as 05-add-friend-real-core, 06-public-contact-real-core, 07-join-group-real-core, and 08-conversation-real-core, then generate a strict-final package proposal." "scripts/ios/prepare-app-store-final-screenshot-package.sh --replacement-dir /tmp/nome-ios-real-core-final-screens --output /tmp/nome-ios-app-store-final-package-current --force" >> "$next_actions"

  priority=$((priority + 1))
  printf '%s\tmanifest_cleanup\t%s\t%s\n' "$priority" "If editing the draft directory directly instead of using a proposed package, update MANIFEST.md and clear or rewrite FINAL_BLOCKERS.md so no preview/debug/needs-real-core language remains." "scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens" >> "$next_actions"
else
  printf '1\tfinal_verification\t%s\t%s\n' "Run the strict final screenshot checker and keep the package frozen for upload." "scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens" >> "$next_actions"
fi

quoted_draft_dir="$(printf '%q' "$draft_dir")"
quoted_checklist="$(printf '%q' "$checklist")"
quoted_execution_plan="$(printf '%q' "$execution_plan")"

run_gate "FAIL" "candidate_screenshot_check" "scripts/ios/check-app-store-screenshots.sh --dir $quoted_draft_dir"
run_gate "BLOCKED" "strict_final_screenshot_check" "scripts/ios/check-app-store-screenshots.sh --final --dir $quoted_draft_dir"
run_gate "FAIL" "final_blocker_plan_check" "scripts/ios/check-app-store-final-blocker-plan.sh --dir $quoted_draft_dir --checklist $quoted_checklist --execution-plan $quoted_execution_plan"

candidate_status="$(awk -F '\t' 'NR > 1 && $2 == "candidate_screenshot_check" {print $1; exit}' "$gate_status")"
strict_status="$(awk -F '\t' 'NR > 1 && $2 == "strict_final_screenshot_check" {print $1; exit}' "$gate_status")"
blocker_plan_status="$(awk -F '\t' 'NR > 1 && $2 == "final_blocker_plan_check" {print $1; exit}' "$gate_status")"

printf 'key\tvalue\n' > "$summary"
printf 'draft_dir\t%s\n' "$draft_dir" >> "$summary"
printf 'manifest\t%s\n' "$manifest" >> "$summary"
printf 'final_blockers\t%s\n' "$blockers" >> "$summary"
printf 'candidate_status\t%s\n' "${candidate_status:-unknown}" >> "$summary"
printf 'strict_final_status\t%s\n' "${strict_status:-unknown}" >> "$summary"
printf 'blocker_plan_status\t%s\n' "${blocker_plan_status:-unknown}" >> "$summary"
printf 'real_core_route_state_status\t%s\n' "${route_state_status:-unknown}" >> "$summary"
printf 'real_core_route_ready_count\t%s\n' "${route_ready_count:-unknown}" >> "$summary"
printf 'screenshot_total\t%s\n' "$total_count" >> "$summary"
printf 'ready_count\t%s\n' "$ready_count" >> "$summary"
printf 'needs_real_core_count\t%s\n' "$needs_real_core_count" >> "$summary"
printf 'replacement_rows\t%s\n' "$replacement_count" >> "$summary"
printf 'screenshot_status\t%s\n' "$screenshot_status" >> "$summary"
printf 'replacement_plan\t%s\n' "$replacement_plan" >> "$summary"
printf 'next_actions\t%s\n' "$next_actions" >> "$summary"
printf 'real_core_route_state\t%s\n' "$route_state_dir" >> "$summary"

cat > "$output_dir/README.md" <<EOF
# Nome iOS App Store Final Screenshot State

This packet records the current finalization state for the Nome iOS App Store
screenshot package.

Summary:

- candidate screenshot gate: ${candidate_status:-unknown}
- strict final screenshot gate: ${strict_status:-unknown}
- blocker-plan gate: ${blocker_plan_status:-unknown}
- real-core route state: ${route_state_status:-unknown}
- real-core ready routes: ${route_ready_count:-unknown}
- screenshot rows: $total_count
- ready rows: $ready_count
- needs-real-core rows: $needs_real_core_count
- replacement rows: $replacement_count

Files:

- summary.tsv
- gate_status.tsv
- screenshot_status.tsv
- replacement_plan.tsv
- next_actions.tsv
- logs/
EOF

echo "Nome iOS App Store final screenshot state"
echo "========================================="
echo "[INFO] Output: $output_dir"
echo "[INFO] Candidate gate: ${candidate_status:-unknown}"
echo "[INFO] Strict final gate: ${strict_status:-unknown}"
echo "[INFO] Needs-real-core rows: $needs_real_core_count"
echo "[INFO] Replacement rows: $replacement_count"
sed -n '1,40p' "$next_actions"
echo "[PASS] Exported App Store final screenshot state"

if [ "${candidate_status:-FAIL}" = "FAIL" ] || [ "${blocker_plan_status:-FAIL}" = "FAIL" ]; then
  exit 1
fi

exit 0
