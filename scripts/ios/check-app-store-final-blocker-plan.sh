#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
draft_dir="${DRAFT_DIR:-$root_dir/design/app-store/ios-upload-draft-screens}"
checklist="${CHECKLIST:-$root_dir/plans/20260709_nome_ios_manual_qa_checklist.md}"
execution_plan="${EXECUTION_PLAN:-$root_dir/plans/20260709_nome_ios_remaining_qa_execution_plan.md}"
status=0

usage() {
  cat <<'USAGE'
Usage:
  scripts/ios/check-app-store-final-blocker-plan.sh [options]

Checks that the non-final App Store screenshot draft has an explicit,
machine-checkable replacement plan tied to manual QA and the remaining QA batch
plan. It does not make the screenshots final; strict final mode remains covered
by scripts/ios/check-app-store-screenshots.sh --final.

Options:
  --dir DIR             Draft screenshot directory.
  --checklist FILE      Manual QA checklist.
  --execution-plan FILE Remaining QA execution plan.
  -h, --help            Show this help.

Environment:
  DRAFT_DIR             Override the draft screenshot directory.
  CHECKLIST             Override the manual QA checklist path.
  EXECUTION_PLAN        Override the remaining QA execution plan path.
USAGE
}

ok() {
  printf '[OK] %s\n' "$1"
}

fail() {
  printf '[FAIL] %s\n' "$1"
  status=1
}

info() {
  printf '[INFO] %s\n' "$1"
}

contains() {
  local file="$1"
  local pattern="$2"

  grep -Fq "$pattern" "$file" 2>/dev/null
}

expect_file() {
  local label="$1"
  local file="$2"

  if [ -f "$file" ]; then
    ok "Found $label: $file"
  else
    fail "Missing $label: $file"
  fi
}

expect_contains() {
  local label="$1"
  local file="$2"
  local pattern="$3"

  if contains "$file" "$pattern"; then
    ok "$label"
  else
    fail "$label missing pattern: $pattern"
  fi
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

manifest="$draft_dir/MANIFEST.md"
blockers="$draft_dir/FINAL_BLOCKERS.md"

cat <<'HEADER'
Nome iOS App Store final screenshot blocker plan
================================================
HEADER

info "Draft directory: $draft_dir"
info "Checklist: $checklist"
info "Execution plan: $execution_plan"

expect_file "draft manifest" "$manifest"
expect_file "final blocker ledger" "$blockers"
expect_file "manual QA checklist" "$checklist"
expect_file "remaining QA execution plan" "$execution_plan"

if [ "$status" -ne 0 ]; then
  exit "$status"
fi

expected_count=4
actual_count="$(grep -Ec '^\| [0-9]+ \| `[^`]+needs-real-core[^`]*` \|' "$blockers" 2>/dev/null || true)"
if [ "$actual_count" -eq "$expected_count" ]; then
  ok "Final blocker ledger records $expected_count needs-real-core screenshot replacements"
else
  fail "Expected $expected_count needs-real-core screenshot blocker rows, found $actual_count"
fi

expected_rows=(
  "05-add-friend-needs-real-core.jpg|one-time invitation link|Real core creates a valid one-time link."
  "06-public-contact-needs-real-core.jpg|public contact address|Real core creates or loads a reusable public contact address."
  "07-join-group-needs-real-core.jpg|group invite link|Real group invitation links show a preview before joining."
  "08-conversation-needs-real-core.jpg|Two-account real conversation|A real one-to-one conversation sends and receives text."
)

for row in "${expected_rows[@]}"; do
  IFS='|' read -r draft_file blocker_phrase checklist_phrase <<< "$row"
  expect_contains "Manifest marks $draft_file as needs-real-core" "$manifest" "| \`$draft_file\` |"
  expect_contains "Manifest status for $draft_file is needs-real-core" "$manifest" "| \`needs-real-core\` |"
  expect_contains "Final blocker ledger names $draft_file" "$blockers" "\`$draft_file\`"
  expect_contains "Final blocker evidence mentions $blocker_phrase" "$blockers" "$blocker_phrase"
  expect_contains "Manual QA contains evidence anchor for $draft_file" "$checklist" "$checklist_phrase"
done

expect_contains "Execution plan has Batch 6 final screenshot section" "$execution_plan" "## Batch 6: final App Store screenshot package"
expect_contains "Execution plan says to replace needs-real-core screenshots" "$execution_plan" "Replace the add-friend, public-contact, join-group, and conversation \`needs-real-core\` draft screenshots"
expect_contains "Execution plan includes strict final screenshot command" "$execution_plan" "scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens"

if [ "$status" -eq 0 ]; then
  echo "[PASS] Final screenshot blocker plan is complete and tied to manual QA evidence"
fi

exit "$status"
