#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
checklist="$root_dir/plans/20260709_nome_ios_manual_qa_checklist.md"
output_dir="/tmp/nome-ios-qa-batches-$(date +%Y%m%d-%H%M%S)"
force=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/export-nome-qa-batches.sh [options]

Exports the current unchecked Nome iOS manual QA queue into batch-specific TSV
files and Markdown evidence templates. This is a planning/evidence helper; it
does not run the app, modify sources, or mark checklist items complete.

Options:
  --checklist FILE  Manual QA checklist to export. Default: Nome iOS checklist.
  --output DIR      Output directory. Default: /tmp/nome-ios-qa-batches-<time>.
  --force           Replace an existing output directory.
  -h, --help        Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --checklist)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --checklist requires a file" >&2
        exit 2
      fi
      checklist="$1"
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

if [ ! -f "$checklist" ]; then
  echo "[FAIL] Manual QA checklist not found: $checklist" >&2
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

mkdir -p "$output_dir/evidence"
queue="$output_dir/manual_qa_unchecked.tsv"
manual_log="$output_dir/manual_qa_status.log"

set +e
"$root_dir/scripts/ios/check-nome-manual-qa-status.sh" --file "$checklist" --write-tsv "$queue" > "$manual_log" 2>&1
manual_status="$?"
set -e

if [ "$manual_status" -gt 1 ]; then
  echo "[FAIL] Could not summarize manual QA checklist" >&2
  sed -n '1,160p' "$manual_log" >&2
  exit 1
fi

batch_id_for_group() {
  case "$1" in
    real_core_artifacts)
      printf 'batch-0'
      ;;
    first_run_real_core|network_server_real_core)
      printf 'batch-1'
      ;;
    real_core_functional|native_share_real_core|public_contact_real_core|error_state_real_core)
      printf 'batch-2'
      ;;
    multi_account_real_core|group_real_core|messaging_feature_real_core|conversation_safety_real_core)
      printf 'batch-3'
      ;;
    identity_real_data|migration_real_data)
      printf 'batch-4'
      ;;
    physical_device_or_camera)
      printf 'batch-5'
      ;;
    app_store_final_screenshots)
      printf 'batch-6'
      ;;
    release_identifiers)
      printf 'batch-7'
      ;;
    *)
      return 1
      ;;
  esac
}

batch_title_for_id() {
  case "$1" in
    batch-0) printf 'real-core artifacts' ;;
    batch-1) printf 'first-run real app setup' ;;
    batch-2) printf 'single-account connection surfaces' ;;
    batch-3) printf 'two-account messaging and groups' ;;
    batch-4) printf 'identity, migration, and data behavior' ;;
    batch-5) printf 'physical device and camera' ;;
    batch-6) printf 'final App Store screenshot package' ;;
    batch-7) printf 'release identifiers and capabilities' ;;
    *) return 1 ;;
  esac
}

dependency_for_group() {
  case "$1" in
    real_core_artifacts)
      printf 'arm64 simulator core or deliberate physical-device real-core install'
      ;;
    first_run_real_core|network_server_real_core|real_core_functional|public_contact_real_core|error_state_real_core)
      printf 'real iOS core runtime'
      ;;
    native_share_real_core)
      printf 'real iOS core runtime and native share-sheet interaction'
      ;;
    multi_account_real_core|group_real_core|messaging_feature_real_core|conversation_safety_real_core)
      printf 'real iOS core runtime plus two independent accounts/devices'
      ;;
    identity_real_data|migration_real_data)
      printf 'real local profile data and migration source data'
      ;;
    physical_device_or_camera)
      printf 'connected trusted physical iPhone and camera permission'
      ;;
    app_store_final_screenshots)
      printf 'real-core replacement screenshots and final manifest cleanup'
      ;;
    release_identifiers)
      printf 'Nome release-identity decision for bundle ids, App Groups, keychain groups, domains, and BG task ids'
      ;;
    *)
      printf 'manual follow-up'
      ;;
  esac
}

command_summary_for_batch() {
  case "$1" in
    batch-0)
      printf 'export real-core route state, audit sources, stage artifacts, then run real-core preflight'
      ;;
    batch-1)
      printf 'launch a clean real-core app and complete first-run profile/network setup'
      ;;
    batch-2)
      printf 'exercise one-account invite, QR, share, public contact, and error-state flows'
      ;;
    batch-3)
      printf 'use two accounts/devices for messaging, groups, media, and conversation safety checks'
      ;;
    batch-4)
      printf 'verify identity, hidden profile, incognito, backup, and migration behavior with real data'
      ;;
    batch-5)
      printf 'export device readiness, install on trusted iPhone/iPad, and test live camera QR scanning'
      ;;
    batch-6)
      printf 'replace needs-real-core screenshots and pass the strict final App Store screenshot gate'
      ;;
    batch-7)
      printf 'finalize release identifiers/capabilities and pass the strict identifier gate'
      ;;
    *)
      printf 'manual follow-up'
      ;;
  esac
}

evidence_summary_for_batch() {
  case "$1" in
    batch-0)
      printf 'route packet, source-audit logs, staged artifacts, prepare logs, and final real-core preflight'
      ;;
    batch-1)
      printf 'clean-launch screen recording or screenshots, profile state, and network/server evidence'
      ;;
    batch-2)
      printf 'real invite/public-address screenshots, share-sheet evidence, and failed-link error evidence'
      ;;
    batch-3)
      printf 'two-account contact, group, media, delivery, safety, and message-retention evidence'
      ;;
    batch-4)
      printf 'real profile, hidden-profile, lock/passcode, backup/export/import, and migration evidence'
      ;;
    batch-5)
      printf 'trusted device id, install/launch logs, camera permission, and live QR scan evidence'
      ;;
    batch-6)
      printf 'final screenshot package, replacement sources, manifest, blockers ledger, and final checker log'
      ;;
    batch-7)
      printf 'release identity packet, final decision record, provisioning/capability notes, and strict checker log'
      ;;
    *)
      printf 'manual evidence paths and verdict notes'
      ;;
  esac
}

commands_for_batch() {
  case "$1" in
    batch-0)
      cat <<'EOF'
scripts/ios/export-real-core-route-state.sh --output /tmp/nome-ios-real-core-route-state-current --force
scripts/ios/check-real-core-sources.sh
SOURCE_TARGET=physical-device scripts/ios/check-real-core-sources.sh
scripts/ios/prepare-device-real-core.sh
scripts/ios/run-real-core-batch0.sh --source ~/Downloads --output /tmp/nome-ios-real-core-batch0 --force
# Only run simulator --prepare after route-state/source-audit evidence proves the staged simulator artifact matches the active simulator.
scripts/ios/run-real-core-batch0.sh --source /tmp/nome-ios-real-core-batch0 --prepare
# Only run physical-device --prepare after SOURCE_TARGET=physical-device and prepare-device dry-run evidence pass, and a real device test is intended.
scripts/ios/prepare-device-real-core.sh --prepare
scripts/ios/check-real-core-xcode-sync.sh
scripts/ios/check-real-core.sh
scripts/ios/check-ios-device-readiness.sh
EOF
      ;;
    batch-1)
      cat <<'EOF'
scripts/ios/check-real-core.sh
# Launch a clean real-core simulator/device build without Debug preview arguments.
EOF
      ;;
    batch-2)
      cat <<'EOF'
scripts/ios/check-real-core.sh
# Use one real account to create invite/public-address data and exercise share/error states.
EOF
      ;;
    batch-3)
      cat <<'EOF'
scripts/ios/check-real-core.sh
# Use two independent real accounts/devices for contact, group, media, and safety-state checks.
EOF
      ;;
    batch-4)
      cat <<'EOF'
scripts/ios/check-real-core.sh
# Use real profiles and migration source data; record passcode and migration behavior.
EOF
      ;;
    batch-5)
      cat <<'EOF'
scripts/ios/export-ios-device-readiness-state.sh --output /tmp/nome-ios-device-readiness-state-current --force
SOURCE_TARGET=physical-device scripts/ios/check-real-core-sources.sh
scripts/ios/prepare-device-real-core.sh
# Install device libraries only when a trusted physical iPhone/iPad test is intended.
scripts/ios/prepare-device-real-core.sh --prepare
scripts/ios/check-real-core-xcode-sync.sh
scripts/ios/check-ios-device-readiness.sh
# Build, install, and launch on a connected trusted physical iPhone, then test live camera QR scanning.
EOF
      ;;
    batch-6)
      cat <<'EOF'
scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens
EOF
      ;;
    batch-7)
      cat <<'EOF'
scripts/ios/check-ios-release-identifiers.sh
# Update bundle ids, App Groups, keychain groups, associated domains, extension names, and BG task ids only after the release identity decision is made.
EOF
      ;;
  esac
}

expected_evidence_for_batch() {
  case "$1" in
    batch-0)
      cat <<'EOF'
- Route-state packet path, especially `summary.tsv`, `route_status.tsv`, and `next_actions.tsv`.
- Simulator source-audit log path showing the selected artifact source and simulator architecture compatibility.
- Physical-device source-audit log path from `SOURCE_TARGET=physical-device scripts/ios/check-real-core-sources.sh`.
- `scripts/ios/prepare-device-real-core.sh` dry-run log path when the physical-device fallback is considered.
- Staged artifact directory path and the command that created it.
- Evidence that simulator `--prepare` was only run after the selected route was compatible with the active simulator.
- Evidence that physical-device `--prepare` was only run after the device source audit and dry-run passed, and only for a deliberate physical-device test.
- Final `scripts/ios/check-real-core.sh` output, or physical-device fallback output from `scripts/ios/check-ios-device-readiness.sh`.
EOF
      ;;
    batch-5)
      cat <<'EOF'
- Connected trusted iPhone/iPad identifier.
- Device readiness log path.
- `SOURCE_TARGET=physical-device` source-audit log path.
- `scripts/ios/prepare-device-real-core.sh` dry-run and, if used, `--prepare` log path.
- Physical-device build/install/launch evidence.
- Live camera QR scan evidence or a clear device/camera blocker.
EOF
      ;;
    batch-6)
      cat <<'EOF'
- Final screenshot directory path.
- `FINAL_BLOCKERS.md` state after replacement.
- Final screenshot checker output.
EOF
      ;;
    batch-7)
      cat <<'EOF'
- Release identity state packet path.
- Final identifier decision record or explicit compatibility exception.
- Strict release identifier checker output.
EOF
      ;;
    *)
      cat <<'EOF'
- Tester:
- Date:
- Device or simulator:
- Build:
- Steps performed:
- Result:
- Attachments or screenshot paths:
- Notes:
EOF
      ;;
  esac
}

slugify() {
  printf '%s' "$1" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//'
}

for batch_id in batch-0 batch-1 batch-2 batch-3 batch-4 batch-5 batch-6 batch-7; do
  title="$(batch_title_for_id "$batch_id")"
  tsv="$output_dir/$batch_id.tsv"
  mkdir -p "$output_dir/evidence/$batch_id"
  printf 'line\tsection\tblocker_group\titem\tevidence_file\tstatus\tdependency\tcommand_summary\tevidence_summary\tevidence_paths\tblocker_notes\n' > "$tsv"
  {
    printf '# %s: %s\n\n' "$batch_id" "$title"
    printf 'Status: pending\n\n'
    printf 'Commands:\n\n```bash\n'
    commands_for_batch "$batch_id"
    printf '```\n\n'
    printf 'Expected evidence:\n\n'
    expected_evidence_for_batch "$batch_id"
    printf '\n'
    printf 'Items:\n\n'
  } > "$output_dir/evidence/$batch_id/README.md"
done

manifest="$output_dir/manifest.tsv"
printf 'batch_id\ttitle\tcount\ttsv\tevidence_dir\n' > "$manifest"
evidence_index="$output_dir/evidence_index.tsv"
printf 'batch_id\tbatch_title\tline\tsection\tblocker_group\titem\tevidence_file\tstatus\tdependency\tcommand_summary\tevidence_summary\tevidence_paths\tblocker_notes\n' > "$evidence_index"

tail -n +2 "$queue" | while IFS=$'\t' read -r line section blocker_group item; do
  if [ -z "${line:-}" ]; then
    continue
  fi

  if ! batch_id="$(batch_id_for_group "$blocker_group")"; then
    echo "[FAIL] Unmapped blocker group: $blocker_group" >&2
    exit 1
  fi

  item_slug="$(slugify "$item")"
  evidence_rel="evidence/$batch_id/line-$line-$item_slug.md"
  evidence_path="$output_dir/$evidence_rel"
  batch_title="$(batch_title_for_id "$batch_id")"
  dependency="$(dependency_for_group "$blocker_group")"
  command_summary="$(command_summary_for_batch "$batch_id")"
  evidence_summary="$(evidence_summary_for_batch "$batch_id")"

  printf '%s\t%s\t%s\t%s\t%s\tpending\t%s\t%s\t%s\t\t\n' "$line" "$section" "$blocker_group" "$item" "$evidence_rel" "$dependency" "$command_summary" "$evidence_summary" >> "$output_dir/$batch_id.tsv"
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\tpending\t%s\t%s\t%s\t\t\n' "$batch_id" "$batch_title" "$line" "$section" "$blocker_group" "$item" "$evidence_rel" "$dependency" "$command_summary" "$evidence_summary" >> "$evidence_index"
  printf -- '- line %s: %s\n' "$line" "$item" >> "$output_dir/evidence/$batch_id/README.md"

  cat > "$evidence_path" <<EOF
# QA evidence: line $line

Status: pending

Acceptance verdict: pending

Section: $section

Blocker group: $blocker_group

Batch: $batch_id - $batch_title

Dependency: $dependency

Command summary: $command_summary

Evidence summary: $evidence_summary

Item:

- $item

Evidence fields:

- Runtime route:
- Device or simulator:
- Build or commit:
- Account/profile setup:
- Evidence paths:
- Screenshot or video paths:
- Log paths:
- Result: pending
- Blocker notes:
- Follow-up:

Completion rule:

- Do not mark this checklist item complete until the result is pass and every required evidence path is attached or the item is explicitly moved to a documented blocker.

Expected evidence:

$(expected_evidence_for_batch "$batch_id")

Related commands:

\`\`\`bash
$(commands_for_batch "$batch_id")
\`\`\`
EOF
done

for batch_id in batch-0 batch-1 batch-2 batch-3 batch-4 batch-5 batch-6 batch-7; do
  title="$(batch_title_for_id "$batch_id")"
  count="$(tail -n +2 "$output_dir/$batch_id.tsv" | awk 'NF {count++} END {print count + 0}')"
  printf '%s\t%s\t%s\t%s\t%s\n' "$batch_id" "$title" "$count" "$batch_id.tsv" "evidence/$batch_id" >> "$manifest"
done

queue_count="$(tail -n +2 "$queue" | awk 'NF {count++} END {print count + 0}')"
batch_count="$(tail -n +2 "$manifest" | awk -F '\t' 'NF {sum += $3} END {print sum + 0}')"

if [ "$queue_count" -ne "$batch_count" ]; then
  echo "[FAIL] Batch export count mismatch: queue=$queue_count batches=$batch_count" >&2
  exit 1
fi

cat > "$output_dir/README.md" <<EOF
# Nome iOS QA batch export

Generated from:

- $checklist

Source queue:

- manual_qa_unchecked.tsv

Machine-readable evidence index:

- evidence_index.tsv

Use this export as an execution packet for the remaining Nome iOS QA batches.
Each batch has a TSV queue and Markdown evidence templates under \`evidence/\`.

Recheck commands:

\`\`\`bash
scripts/ios/check-nome-manual-qa-status.sh --write-tsv /tmp/nome-manual-qa-unchecked.tsv
scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --output /tmp/nome-ios-readiness-current
scripts/ios/check-nome-ios-goal-audit.sh --source-audit --smoke-manifest /tmp/nome-ios-smoke-contacts-tab/manifest.tsv --allow-blockers --output /tmp/nome-ios-goal-audit-current
\`\`\`
EOF

echo "Nome iOS QA batch export"
echo "========================"
echo "[INFO] Output: $output_dir"
echo "[INFO] Exported unchecked items: $batch_count"
sed -n '1,20p' "$manifest"
echo "[PASS] Exported QA batches and evidence templates"
