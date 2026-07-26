#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
plan="$root_dir/plans/20260709_nome_ios_remaining_qa_execution_plan.md"
checklist="$root_dir/plans/20260709_nome_ios_manual_qa_checklist.md"

usage() {
  cat <<'USAGE'
Usage: scripts/ios/check-nome-qa-execution-plan.sh [--plan FILE] [--checklist FILE]

Verifies that the remaining Nome iOS QA execution plan covers the current
manual-QA blocker groups and names the required recheck commands.

Options:
  --plan FILE       QA execution plan to inspect.
  --checklist FILE  Manual QA checklist to summarize.
  -h, --help        Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --plan)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --plan requires a path" >&2
        exit 2
      fi
      plan="$1"
      ;;
    --checklist)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --checklist requires a path" >&2
        exit 2
      fi
      checklist="$1"
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

if [ ! -f "$plan" ]; then
  echo "[FAIL] QA execution plan not found: $plan" >&2
  exit 1
fi

if [ ! -f "$checklist" ]; then
  echo "[FAIL] Manual QA checklist not found: $checklist" >&2
  exit 1
fi

tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-qa-plan.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT

queue="$tmp_dir/manual_qa_unchecked.tsv"
manual_log="$tmp_dir/manual_qa_status.log"

set +e
"$root_dir/scripts/ios/check-nome-manual-qa-status.sh" --file "$checklist" --write-tsv "$queue" > "$manual_log" 2>&1
manual_status="$?"
set -e

if [ "$manual_status" -gt 1 ]; then
  echo "[FAIL] Could not summarize manual QA checklist" >&2
  sed -n '1,160p' "$manual_log" >&2
  exit 1
fi

missing=0

require_text() {
  local label="$1"
  local needle="$2"

  if grep -Fq -- "$needle" "$plan"; then
    printf '[PASS] %s\n' "$label"
  else
    printf '[FAIL] %s missing: %s\n' "$label" "$needle" >&2
    missing=1
  fi
}

echo "Nome iOS QA execution plan check"
echo "================================"
echo "[INFO] Plan: $plan"
echo "[INFO] Checklist: $checklist"

for heading in \
  "Batch 0" \
  "Batch 1" \
  "Batch 2" \
  "Batch 3" \
  "Batch 4" \
  "Batch 5" \
  "Batch 6" \
  "Batch 7"
do
  require_text "batch_heading" "$heading"
done

require_text "manual_qa_queue_command" "scripts/ios/check-nome-manual-qa-status.sh --write-tsv"
require_text "real_core_preflight_command" "scripts/ios/check-real-core.sh"
require_text "release_identifier_command" "scripts/ios/check-ios-release-identifiers.sh"
require_text "final_screenshot_blocker_plan_command" "scripts/ios/check-app-store-final-blocker-plan.sh"
require_text "final_screenshot_command" "scripts/ios/check-app-store-screenshots.sh --final"
require_text "release_goal_audit_command" "scripts/ios/check-nome-ios-goal-audit.sh"

if awk -F '\t' 'NR > 1 && $3 == "manual_followup" {found=1} END {exit found ? 0 : 1}' "$queue"; then
  echo "[FAIL] Manual QA queue contains unclassified manual_followup items" >&2
  awk -F '\t' 'NR > 1 && $3 == "manual_followup" {print}' "$queue" >&2
  missing=1
fi

groups="$tmp_dir/groups.txt"
awk -F '\t' 'NR > 1 && $3 != "" {print $3}' "$queue" | sort -u > "$groups"
group_count="$(wc -l < "$groups" | tr -d '[:space:]')"
echo "[INFO] Current unchecked blocker groups: $group_count"

while IFS= read -r group; do
  [ -n "$group" ] || continue
  require_text "blocker_group:$group" "$group"
done < "$groups"

if [ "$missing" -ne 0 ]; then
  echo "[FAIL] QA execution plan is incomplete" >&2
  exit 1
fi

echo "[PASS] QA execution plan covers the current manual QA blocker groups"
