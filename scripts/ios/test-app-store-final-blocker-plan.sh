#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/check-app-store-final-blocker-plan.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-app-store-final-blocker-plan-test.XXXXXX")"
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
| 5 | `05-add-friend-needs-real-core.jpg` | `11-add-friend-preview-core.png` | `1206x2622` | `1` | `a` | `needs-real-core` | Replace with a real one-time link and QR screenshot. |
| 6 | `06-public-contact-needs-real-core.jpg` | `05-public-contact-preview-core.png` | `1206x2622` | `1` | `b` | `needs-real-core` | Replace with a real public contact address screenshot. |
| 7 | `07-join-group-needs-real-core.jpg` | `06-join-group-preview.png` | `1206x2622` | `1` | `c` | `needs-real-core` | Replace after real group invitation preview or join test. |
| 8 | `08-conversation-needs-real-core.jpg` | `07-conversation-debug-preview.png` | `1206x2622` | `1` | `d` | `needs-real-core` | Replace with a real two-account conversation screenshot. |
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
- [ ] Real core creates a valid one-time link.
- [ ] Real core creates or loads a reusable public contact address.
- [ ] Real group invitation links show a preview before joining.
- [ ] A real one-to-one conversation sends and receives text.
EOF

  cat > "$dir/plan.md" <<'EOF'
## Batch 6: final App Store screenshot package

1. Replace the add-friend, public-contact, join-group, and conversation `needs-real-core` draft screenshots with real-core screenshots.
2. Verify with `scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens`.
EOF
}

run_case() {
  local name="$1"
  local expected_rc="$2"
  local expected_text="$3"
  local fixture="$4"
  local log="$work_dir/$name.log"
  local rc=0

  "$script" \
    --dir "$fixture/draft" \
    --checklist "$fixture/checklist.md" \
    --execution-plan "$fixture/plan.md" > "$log" 2>&1 || rc=$?

  if [ "$rc" -ne "$expected_rc" ]; then
    sed -n '1,220p' "$log" >&2
    fail "$name expected exit $expected_rc, got $rc"
  fi

  if ! grep -Fq "$expected_text" "$log"; then
    sed -n '1,220p' "$log" >&2
    fail "$name missing expected text: $expected_text"
  fi

  pass "$name"
}

complete="$work_dir/complete"
make_fixture "$complete"
run_case "complete_plan_passes" 0 "Final screenshot blocker plan is complete" "$complete"

missing_blocker="$work_dir/missing-blocker"
make_fixture "$missing_blocker"
perl -0pi -e 's/^\| 7 \|.*\n//m' "$missing_blocker/draft/FINAL_BLOCKERS.md"
run_case "missing_blocker_fails" 1 "Expected 4 needs-real-core screenshot blocker rows" "$missing_blocker"

missing_qa="$work_dir/missing-qa"
make_fixture "$missing_qa"
grep -Fv 'A real one-to-one conversation sends and receives text.' "$missing_qa/checklist.md" > "$missing_qa/checklist.md.tmp"
mv "$missing_qa/checklist.md.tmp" "$missing_qa/checklist.md"
run_case "missing_manual_qa_anchor_fails" 1 "Manual QA contains evidence anchor" "$missing_qa"

echo "[PASS] app-store final blocker plan tests passed"
