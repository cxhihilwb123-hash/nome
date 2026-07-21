#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/export-nome-design-evidence-state.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-design-evidence-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

manifest="$work_dir/manifest.tsv"
output="$work_dir/state"
smoke_dir="$work_dir/smoke"
mkdir -p "$smoke_dir"

labels=(
  "01-home"
  "06-chat-list-existing"
  "10-add-friend"
  "11-join-group"
  "12-public-contact"
  "09-identity-center"
  "07-conversation-preview"
  "08-conversation-details"
  "13-settings"
)

printf 'label\targs\twidth\theight\tbytes\tsha256\tpath\n' > "$manifest"
for label in "${labels[@]}"; do
  file="$smoke_dir/$label.png"
  printf 'fake-smoke-%s\n' "$label" > "$file"
  hash="$(shasum -a 256 "$file" | awk '{print $1}')"
  bytes="$(wc -c < "$file" | tr -d '[:space:]')"
  printf '%s\t\t1206\t2622\t%s\t%s\t%s\n' "$label" "$bytes" "$hash" "$file" >> "$manifest"
done

"$script" --smoke-manifest "$manifest" --output "$output" --force > "$work_dir/export.log" 2>&1

[ -f "$output/summary.tsv" ] || fail "missing summary.tsv"
[ -f "$output/coverage.tsv" ] || fail "missing coverage.tsv"
[ -f "$output/page_evidence.tsv" ] || fail "missing page_evidence.tsv"
[ -f "$output/missing.tsv" ] || fail "missing missing.tsv"

grep -Fq $'coverage_rows\t7' "$output/summary.tsv" || fail "summary should report seven coverage rows"
grep -Fq $'page_evidence_rows\t7' "$output/summary.tsv" || fail "summary should report seven page evidence rows"
grep -Fq $'visual_pass_rows\t7' "$output/summary.tsv" || fail "summary should report seven visual pass rows"
grep -Fq $'final_blocked_rows\t7' "$output/summary.tsv" || fail "summary should report seven functional blocked rows"
grep -Fq $'missing_rows\t0' "$output/summary.tsv" || fail "summary should report zero missing rows"
grep -Fq $'page_evidence_tsv\t'"$output/page_evidence.tsv" "$output/summary.tsv" || fail "summary should point to page evidence TSV"
grep -Fq $'COV-HOME\tdesign/product/pages-v2/01-home-inbox.png' "$output/coverage.tsv" || fail "missing home coverage row"
grep -Fq $'COV-ADD-FRIEND\tdesign/product/pages-v2/02-add-friend-one-time.png' "$output/coverage.tsv" || fail "missing add friend coverage row"
grep -Fq "10-add-friend" "$output/coverage.tsv" || fail "missing add friend smoke mapping"
grep -Fq "07-conversation-preview,08-conversation-details" "$output/coverage.tsv" || fail "missing conversation smoke mapping"
grep -Fq "preview_visual_covered_functional_blocked" "$output/coverage.tsv" || fail "missing functional-blocked status"
grep -Fq "Real one-time link generation" "$output/coverage.tsv" || fail "missing remaining proof text"
grep -Fq $'coverage_id\tpage_name\tdesign_artifact\tsmoke_labels\tsmoke_paths\tpage_status\tvisual_acceptance\tfinal_functional_acceptance\tblocker_batch\tapp_store_dependency\tremaining_proof\tnext_evidence' "$output/page_evidence.tsv" || fail "missing page evidence header"
grep -Fq $'COV-ADD-FRIEND\tAdd friend one-time link' "$output/page_evidence.tsv" || fail "missing add friend page evidence"
grep -Fq $'PASS\tBLOCKED\tbatch-2\tneeds-real-core:05-add-friend-needs-real-core.jpg' "$output/page_evidence.tsv" || fail "missing add friend acceptance/blocker mapping"
grep -Fq $'COV-CONVERSATION\tConversation' "$output/page_evidence.tsv" || fail "missing conversation page evidence"
grep -Fq $'PASS\tBLOCKED\tbatch-3\tneeds-real-core:08-conversation-needs-real-core.jpg' "$output/page_evidence.tsv" || fail "missing conversation acceptance/blocker mapping"
grep -Fq $'COV-SETTINGS\tSettings and safety' "$output/page_evidence.tsv" || fail "missing settings page evidence"
grep -Fq $'PASS\tBLOCKED\tbatch-4\tdraft-ready:09-settings.jpg' "$output/page_evidence.tsv" || fail "missing settings acceptance/blocker mapping"

pass "exported design evidence state fixture"
echo "[PASS] design evidence state test logs: $work_dir"
