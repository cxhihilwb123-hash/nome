#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/export-app-store-final-screenshot-state.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-app-store-screenshot-state-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

make_fixture() {
  local dir="$1"

  mkdir -p "$dir/draft"

  cat > "$dir/draft/MANIFEST.md" <<'EOF'
| Slot | Export file | Source file | Dimensions | Bytes | SHA-256 | Status | Final-release note |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `01-onboarding-welcome.jpg` | `01-onboarding-welcome.png` | `1206x2622` | `1` | `a` | `ready` | Onboarding welcome. |
| 2 | `02-home.jpg` | `03-home.png` | `1206x2622` | `1` | `b` | `ready` | Home screen. |
| 5 | `05-add-friend-needs-real-core.jpg` | `11-add-friend-preview-core.png` | `1206x2622` | `1` | `c` | `needs-real-core` | Replace with a real one-time link and QR screenshot. |
| 6 | `06-public-contact-needs-real-core.jpg` | `05-public-contact-preview-core.png` | `1206x2622` | `1` | `d` | `needs-real-core` | Replace with a real public contact address screenshot. |
| 7 | `07-join-group-needs-real-core.jpg` | `06-join-group-preview.png` | `1206x2622` | `1` | `e` | `needs-real-core` | Replace after real group invitation preview or join test. |
| 8 | `08-conversation-needs-real-core.jpg` | `07-conversation-debug-preview.png` | `1206x2622` | `1` | `f` | `needs-real-core` | Replace with a real two-account conversation screenshot. |
EOF

  cat > "$dir/draft/FINAL_BLOCKERS.md" <<'EOF'
| Slot | Draft file | Source candidate | Status | Replacement evidence required |
| --- | --- | --- | --- | --- |
| 5 | `05-add-friend-needs-real-core.jpg` | `11-add-friend-preview-core.png` | `needs-real-core` | Real iOS core creates a one-time invitation link and QR code without preview placeholders. |
| 6 | `06-public-contact-needs-real-core.jpg` | `05-public-contact-preview-core.png` | `needs-real-core` | Real profile has a public contact address visible and copy/share actions available. |
| 7 | `07-join-group-needs-real-core.jpg` | `06-join-group-preview.png` | `needs-real-core` | Real group invite link opens a preview or join path against the real core. |
| 8 | `08-conversation-needs-real-core.jpg` | `07-conversation-debug-preview.png` | `needs-real-core` | Two-account real conversation shows sent and received messages from the real core. |
EOF

  cat > "$dir/checklist.md" <<'EOF'
## Add friend

- [ ] Real core creates a valid one-time link.

## Public contact address

- [ ] Real core creates or loads a reusable public contact address.

## Join group

- [ ] Real group invitation links show a preview before joining.

## Conversation

- [ ] A real one-to-one conversation sends and receives text.
EOF

  cat > "$dir/plan.md" <<'EOF'
## Batch 6: final App Store screenshot package

1. Replace the add-friend, public-contact, join-group, and conversation `needs-real-core` draft screenshots with real-core screenshots.
2. Verify with `scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens`.
EOF
}

fixture="$work_dir/fixture"
output="$work_dir/state"
make_fixture "$fixture"

"$script" \
  --dir "$fixture/draft" \
  --checklist "$fixture/checklist.md" \
  --execution-plan "$fixture/plan.md" \
  --output "$output" \
  --force \
  --skip-gates > "$work_dir/export.log" 2>&1

[ -f "$output/summary.tsv" ] || fail "missing summary.tsv"
[ -f "$output/gate_status.tsv" ] || fail "missing gate_status.tsv"
[ -f "$output/screenshot_status.tsv" ] || fail "missing screenshot_status.tsv"
[ -f "$output/replacement_plan.tsv" ] || fail "missing replacement_plan.tsv"
[ -f "$output/next_actions.tsv" ] || fail "missing next_actions.tsv"

grep -Fq $'screenshot_total\t6' "$output/summary.tsv" || fail "summary missing screenshot total"
grep -Fq $'ready_count\t2' "$output/summary.tsv" || fail "summary missing ready count"
grep -Fq $'needs_real_core_count\t4' "$output/summary.tsv" || fail "summary missing needs-real-core count"
grep -Fq $'replacement_rows\t4' "$output/summary.tsv" || fail "summary missing replacement rows"
grep -Fq $'real_core_route_state_status\tWARN' "$output/summary.tsv" || fail "summary missing skipped route-state warning"
grep -Fq $'real_core_route_ready_count\tunknown' "$output/summary.tsv" || fail "summary missing unknown route-ready count"
grep -Fq $'WARN\tcandidate_screenshot_check' "$output/gate_status.tsv" || fail "skip-gates should warn for candidate check"
grep -Fq $'WARN\treal_core_route_state' "$output/gate_status.tsv" || fail "skip-gates should warn for route-state export"
grep -Fq $'WARN\treal_core_route_prerequisite' "$output/gate_status.tsv" || fail "unknown route prerequisite should warn"
grep -Fq $'05-add-friend-needs-real-core.jpg\t11-add-friend-preview-core.png\t1206x2622\tneeds-real-core\tfinal_blocked' "$output/screenshot_status.tsv" || fail "missing add-friend screenshot status"
grep -Fq $'05-add-friend-needs-real-core.jpg\t11-add-friend-preview-core.png\tneeds-real-core' "$output/replacement_plan.tsv" || fail "missing add-friend replacement row"
grep -Fq "Real core creates a valid one-time link." "$output/replacement_plan.tsv" || fail "missing manual QA anchor"
grep -Fq $'batch-3\tCapture a real two-account conversation' "$output/replacement_plan.tsv" || fail "missing conversation batch/action"
grep -Fq $'1\treal_core_route_prerequisite' "$output/next_actions.tsv" || fail "missing real-core route prerequisite next action"
grep -Fq "Current ready route count: unknown" "$output/next_actions.tsv" || fail "route prerequisite should report current ready route count"
grep -Fq $'6\tfinal_package_proposal' "$output/next_actions.tsv" || fail "missing final package proposal next action"
grep -Fq "prepare-app-store-final-screenshot-package.sh" "$output/next_actions.tsv" || fail "final package proposal should point at package script"
grep -Fq $'7\tmanifest_cleanup' "$output/next_actions.tsv" || fail "missing manifest cleanup next action"

awk -F '\t' '
  NR > 1 && $2 == "05-add-friend-needs-real-core.jpg" && $7 != "" && $8 == "Add friend" && $9 == "real_core_functional" && $10 == "batch-2" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/replacement_plan.tsv" || fail "missing add-friend checklist location and blocker group"

awk -F '\t' '
  NR > 1 && $2 == "08-conversation-needs-real-core.jpg" && $7 != "" && $8 == "Conversation" && $9 == "multi_account_real_core" && $10 == "batch-3" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/replacement_plan.tsv" || fail "missing conversation checklist location and blocker group"

grep -Fq "Checklist: line" "$output/next_actions.tsv" || fail "next actions should mention checklist location"

pass "exported App Store final screenshot state fixture"
echo "[PASS] App Store final screenshot state test logs: $work_dir"
