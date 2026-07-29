#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/export-nome-qa-batches.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-qa-batches-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

checklist="$work_dir/checklist.md"
output="$work_dir/batches"

cat > "$checklist" <<'EOF'
# Test checklist

## Environment gate

- [ ] Real iOS core libraries are installed in `apps/ios/Libraries/ios` and `apps/ios/Libraries/sim`.
- [ ] `apps/ios/Libraries/sim` libraries are production-sized artifacts, not 9.9K preview placeholders.

## Add friend

- [ ] Real core creates a valid one-time link.

## Release gate

- [ ] Final ten-or-fewer App Store screenshot package passes `scripts/ios/check-app-store-screenshots.sh --final`.
- [ ] `scripts/ios/check-ios-release-identifiers.sh` passes for the final TestFlight or public distribution configuration.
EOF

"$script" --checklist "$checklist" --output "$output" --force > "$work_dir/export.log" 2>&1

[ -f "$output/manifest.tsv" ] || fail "missing manifest.tsv"
[ -f "$output/evidence_index.tsv" ] || fail "missing evidence_index.tsv"
[ -f "$output/manual_qa_unchecked.tsv" ] || fail "missing unchecked queue"
[ -f "$output/batch-0.tsv" ] || fail "missing batch-0.tsv"
[ -f "$output/evidence/batch-0/README.md" ] || fail "missing batch-0 README"

grep -Fq $'status\tdependency\tcommand_summary\tevidence_summary\tevidence_paths\tblocker_notes' "$output/batch-0.tsv" || fail "batch TSV should include evidence tracking columns"
grep -Fq $'batch_id\tbatch_title\tline\tsection\tblocker_group\titem\tevidence_file\tstatus\tdependency\tcommand_summary\tevidence_summary\tevidence_paths\tblocker_notes' "$output/evidence_index.tsv" || fail "evidence index should expose all evidence tracking columns"
grep -Fq $'batch-0\treal-core artifacts\t2' "$output/manifest.tsv" || fail "batch-0 count should be 2"
grep -Fq $'batch-2\tsingle-account connection surfaces\t1' "$output/manifest.tsv" || fail "batch-2 count should be 1"
grep -Fq $'batch-6\tfinal App Store screenshot package\t1' "$output/manifest.tsv" || fail "batch-6 count should be 1"
grep -Fq $'batch-7\trelease identifiers and capabilities\t1' "$output/manifest.tsv" || fail "batch-7 count should be 1"
grep -Fq "arm64 simulator core or deliberate physical-device real-core install" "$output/evidence_index.tsv" || fail "evidence index should include real-core dependency"
grep -Fq "replace needs-real-core screenshots and pass the strict final App Store screenshot gate" "$output/evidence_index.tsv" || fail "evidence index should include screenshot command summary"
grep -Fq "final screenshot package, replacement sources, manifest, blockers ledger, and final checker log" "$output/evidence_index.tsv" || fail "evidence index should include screenshot evidence summary"

grep -Fq "scripts/ios/export-real-core-route-state.sh --output /tmp/nome-ios-real-core-route-state-current --force" "$output/evidence/batch-0/README.md" || fail "batch-0 README should require route-state export"
grep -Fq "scripts/ios/check-real-core-sources.sh" "$output/evidence/batch-0/README.md" || fail "batch-0 README should require source audit"
grep -Fq "SOURCE_TARGET=physical-device scripts/ios/check-real-core-sources.sh" "$output/evidence/batch-0/README.md" || fail "batch-0 README should include physical-device source audit"
grep -Fq "scripts/ios/prepare-device-real-core.sh" "$output/evidence/batch-0/README.md" || fail "batch-0 README should include device dry-run"
grep -Fq "Only run simulator --prepare after route-state/source-audit evidence proves" "$output/evidence/batch-0/README.md" || fail "batch-0 README should guard simulator prepare"
grep -Fq "Only run physical-device --prepare after SOURCE_TARGET=physical-device" "$output/evidence/batch-0/README.md" || fail "batch-0 README should guard physical-device prepare"
grep -Fq "Route-state packet path" "$output/evidence/batch-0/README.md" || fail "batch-0 README should request route-state evidence"
grep -Fq "Physical-device source-audit log path" "$output/evidence/batch-0/README.md" || fail "batch-0 README should request physical-device source evidence"

line13_template="$(find "$output/evidence/batch-0" -maxdepth 1 -type f -name 'line-*-real-ios-core-libraries-are-installed*.md' | head -1)"
[ -n "$line13_template" ] || fail "missing line 13 evidence template"
grep -Fq "Simulator source-audit log path" "$line13_template" || fail "line 13 template should request simulator source-audit evidence"
grep -Fq "Physical-device source-audit log path" "$line13_template" || fail "line 13 template should request physical-device source-audit evidence"
grep -Fq "prepare-device-real-core.sh" "$line13_template" || fail "line 13 template should request device dry-run evidence"
grep -Fq "Final \`scripts/ios/check-real-core.sh\` output" "$line13_template" || fail "line 13 template should request final preflight evidence"
grep -Fq "Acceptance verdict: pending" "$line13_template" || fail "line 13 template should include acceptance verdict"
grep -Fq "Evidence fields:" "$line13_template" || fail "line 13 template should include structured evidence fields"
grep -Fq "Do not mark this checklist item complete until the result is pass" "$line13_template" || fail "line 13 template should include completion rule"

grep -Fq "scripts/ios/export-ios-device-readiness-state.sh --output /tmp/nome-ios-device-readiness-state-current --force" "$output/evidence/batch-5/README.md" || fail "batch-5 README should require device readiness export"
grep -Fq "SOURCE_TARGET=physical-device scripts/ios/check-real-core-sources.sh" "$output/evidence/batch-5/README.md" || fail "batch-5 README should include physical-device source audit"
grep -Fq "Live camera QR scan evidence" "$output/evidence/batch-5/README.md" || fail "batch-5 README should request camera evidence"

pass "exported QA batch templates fixture"
echo "[PASS] QA batch template test logs: $work_dir"
