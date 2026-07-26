#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
smoke_manifest="${SMOKE_MANIFEST:-/tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv}"
output_dir="/tmp/nome-ios-design-evidence-state-$(date +%Y%m%d-%H%M%S)"
force=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/export-nome-design-evidence-state.sh [options]

Exports a read-only evidence packet that maps approved Nome pages-v2 mockups
to the current signed iOS smoke screenshots.

Options:
  --smoke-manifest FILE  Manifest from scripts/ios/smoke-nome-ui.sh.
  --output DIR           Output directory.
  --force                Replace an existing output directory.
  -h, --help             Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --smoke-manifest)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --smoke-manifest requires a file" >&2
        exit 2
      fi
      smoke_manifest="$1"
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

if [ ! -f "$smoke_manifest" ]; then
  echo "[FAIL] Smoke manifest not found: $smoke_manifest" >&2
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

mkdir -p "$output_dir"

summary="$output_dir/summary.tsv"
coverage="$output_dir/coverage.tsv"
page_evidence="$output_dir/page_evidence.tsv"
missing="$output_dir/missing.tsv"

sha256_file() {
  shasum -a 256 "$1" | awk '{print $1}'
}

file_bytes() {
  wc -c < "$1" | tr -d '[:space:]'
}

manifest_value() {
  local label="$1"
  local column="$2"

  awk -F '\t' -v label="$label" -v column="$column" '
    NR == 1 {
      for (i = 1; i <= NF; i++) {
        if ($i == column) {
          col = i
        }
      }
      next
    }
    $1 == label && col {
      print $col
      found = 1
      exit
    }
    END {
      exit found ? 0 : 1
    }
  ' "$smoke_manifest"
}

join_with_commas() {
  awk 'NF {printf "%s%s", sep, $0; sep=","}'
}

write_coverage_row() {
  local coverage_id="$1"
  local design_artifact="$2"
  local smoke_labels_csv="$3"
  local status="$4"
  local remaining_proof="$5"
  local design_path="$root_dir/$design_artifact"
  local design_bytes=""
  local design_sha=""
  local smoke_paths=""
  local smoke_hashes=""
  local smoke_bytes=""
  local label
  local path
  local hash
  local bytes
  local missing_row=0

  if [ -f "$design_path" ]; then
    design_bytes="$(file_bytes "$design_path")"
    design_sha="$(sha256_file "$design_path")"
  else
    printf '%s\t%s\t%s\n' "$coverage_id" "design_artifact" "$design_artifact" >> "$missing"
    missing_row=1
  fi

  IFS=',' read -r -a labels <<< "$smoke_labels_csv"
  for label in "${labels[@]}"; do
    if ! path="$(manifest_value "$label" path 2>/dev/null)"; then
      printf '%s\t%s\t%s\n' "$coverage_id" "smoke_label" "$label" >> "$missing"
      missing_row=1
      continue
    fi

    if [ ! -f "$path" ]; then
      printf '%s\t%s\t%s\n' "$coverage_id" "smoke_file" "$path" >> "$missing"
      missing_row=1
    fi

    hash="$(manifest_value "$label" sha256 2>/dev/null || printf '')"
    bytes="$(manifest_value "$label" bytes 2>/dev/null || printf '')"
    smoke_paths="$(printf '%s\n%s' "$smoke_paths" "$path" | sed '/^$/d' | join_with_commas)"
    smoke_hashes="$(printf '%s\n%s' "$smoke_hashes" "$hash" | sed '/^$/d' | join_with_commas)"
    smoke_bytes="$(printf '%s\n%s' "$smoke_bytes" "$bytes" | sed '/^$/d' | join_with_commas)"
  done

  if [ "$missing_row" -eq 0 ]; then
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$coverage_id" "$design_artifact" "$design_bytes" "$design_sha" "$smoke_labels_csv" "$smoke_paths" "$smoke_hashes" "$smoke_bytes" "$status" "$remaining_proof" >> "$coverage"
  fi
}

printf 'coverage_id\tdesign_artifact\tdesign_bytes\tdesign_sha256\tsmoke_labels\tsmoke_paths\tsmoke_sha256\tsmoke_bytes\tstatus\tremaining_proof\n' > "$coverage"
printf 'coverage_id\tmissing_kind\tvalue\n' > "$missing"

write_coverage_row "COV-HOME" "design/product/pages-v2/01-home-inbox.png" "01-home,06-chat-list-existing" "visual_navigation_covered" "Real non-preview profile and live chat-list state after real-core account creation."
write_coverage_row "COV-ADD-FRIEND" "design/product/pages-v2/02-add-friend-one-time.png" "10-add-friend" "preview_visual_covered_functional_blocked" "Real one-time link generation, scannable QR, native share sheet, and second-account acceptance."
write_coverage_row "COV-JOIN-GROUP" "design/product/pages-v2/03-join-group.png" "11-join-group" "preview_visual_covered_functional_blocked" "Real group invitation preview, join success, approval and pending states, and live QR scan."
write_coverage_row "COV-PUBLIC-CONTACT" "design/product/pages-v2/04-public-contact.png" "12-public-contact" "preview_visual_covered_functional_blocked" "Real public address creation/load, copy/share actual address, settings persistence, and second-account request."
write_coverage_row "COV-IDENTITY" "design/product/pages-v2/05-identity-center.png" "09-identity-center" "visual_navigation_covered_real_data_blocked" "Multiple real profiles, switching, hidden profile unlock, mute/delete behavior with real data."
write_coverage_row "COV-CONVERSATION" "design/product/pages-v2/06-conversation.png" "07-conversation-preview,08-conversation-details" "debug_visual_covered_functional_blocked" "Real one-to-one and group messages, files, voice, delivery receipts, verified/unverified state, and disappearing-message persistence."
write_coverage_row "COV-SETTINGS" "design/product/pages-v2/07-settings-safety.png" "13-settings" "visual_navigation_covered_real_data_blocked" "Real migration/device transfer, server/Tor/private routing data, and desktop linking."

coverage_value() {
  local coverage_id="$1"
  local column="$2"

  awk -F '\t' -v coverage_id="$coverage_id" -v column="$column" '
    NR == 1 {
      for (i = 1; i <= NF; i++) {
        if ($i == column) {
          col = i
        }
      }
      next
    }
    $1 == coverage_id && col {
      print $col
      found = 1
      exit
    }
    END {
      exit found ? 0 : 1
    }
  ' "$coverage"
}

write_page_evidence_row() {
  local coverage_id="$1"
  local page_name="$2"
  local blocker_batch="$3"
  local app_store_dependency="$4"
  local next_evidence="$5"
  local design_artifact
  local smoke_labels
  local smoke_paths
  local page_status
  local remaining_proof

  design_artifact="$(coverage_value "$coverage_id" design_artifact)"
  smoke_labels="$(coverage_value "$coverage_id" smoke_labels)"
  smoke_paths="$(coverage_value "$coverage_id" smoke_paths)"
  page_status="$(coverage_value "$coverage_id" status)"
  remaining_proof="$(coverage_value "$coverage_id" remaining_proof)"

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$coverage_id" \
    "$page_name" \
    "$design_artifact" \
    "$smoke_labels" \
    "$smoke_paths" \
    "$page_status" \
    "PASS" \
    "BLOCKED" \
    "$blocker_batch" \
    "$app_store_dependency" \
    "$remaining_proof" \
    "$next_evidence" >> "$page_evidence"
}

printf 'coverage_id\tpage_name\tdesign_artifact\tsmoke_labels\tsmoke_paths\tpage_status\tvisual_acceptance\tfinal_functional_acceptance\tblocker_batch\tapp_store_dependency\tremaining_proof\tnext_evidence\n' > "$page_evidence"
write_page_evidence_row "COV-HOME" "Home and inbox" "batch-1" "draft-ready:03-home.jpg" "Run first-run real-core profile creation and live chat-list QA."
write_page_evidence_row "COV-ADD-FRIEND" "Add friend one-time link" "batch-2" "needs-real-core:05-add-friend-needs-real-core.jpg" "Capture real invite link, QR, native share sheet, and second-account acceptance."
write_page_evidence_row "COV-JOIN-GROUP" "Join group" "batch-3" "needs-real-core:07-join-group-needs-real-core.jpg" "Capture real group invite preview, join success, approval/pending, and live QR scan."
write_page_evidence_row "COV-PUBLIC-CONTACT" "Public contact address" "batch-2" "needs-real-core:06-public-contact-needs-real-core.jpg" "Capture real public address creation/load, copy/share, settings persistence, and second-account request."
write_page_evidence_row "COV-IDENTITY" "Identity center" "batch-4" "no-final-screenshot-replacement" "Run real profile switching, hidden profile unlock, mute, and delete behavior QA."
write_page_evidence_row "COV-CONVERSATION" "Conversation" "batch-3" "needs-real-core:08-conversation-needs-real-core.jpg" "Capture real one-to-one/group messages, media, safety state, and disappearing-message persistence."
write_page_evidence_row "COV-SETTINGS" "Settings and safety" "batch-4" "draft-ready:09-settings.jpg" "Run real migration/device transfer, server/Tor/private routing, and desktop linking QA."

coverage_rows="$(tail -n +2 "$coverage" | awk 'NF {count++} END {print count + 0}')"
page_evidence_rows="$(tail -n +2 "$page_evidence" | awk 'NF {count++} END {print count + 0}')"
visual_pass_rows="$(awk -F '\t' 'NR > 1 && $7 == "PASS" {count++} END {print count + 0}' "$page_evidence")"
final_blocked_rows="$(awk -F '\t' 'NR > 1 && $8 == "BLOCKED" {count++} END {print count + 0}' "$page_evidence")"
missing_rows="$(tail -n +2 "$missing" | awk 'NF {count++} END {print count + 0}')"
smoke_label_rows="$(tail -n +2 "$smoke_manifest" | awk 'NF {count++} END {print count + 0}')"

printf 'key\tvalue\n' > "$summary"
printf 'smoke_manifest\t%s\n' "$smoke_manifest" >> "$summary"
printf 'coverage_rows\t%s\n' "$coverage_rows" >> "$summary"
printf 'page_evidence_rows\t%s\n' "$page_evidence_rows" >> "$summary"
printf 'visual_pass_rows\t%s\n' "$visual_pass_rows" >> "$summary"
printf 'final_blocked_rows\t%s\n' "$final_blocked_rows" >> "$summary"
printf 'missing_rows\t%s\n' "$missing_rows" >> "$summary"
printf 'smoke_label_rows\t%s\n' "$smoke_label_rows" >> "$summary"
printf 'coverage_tsv\t%s\n' "$coverage" >> "$summary"
printf 'page_evidence_tsv\t%s\n' "$page_evidence" >> "$summary"
printf 'missing_tsv\t%s\n' "$missing" >> "$summary"

cat > "$output_dir/README.md" <<EOF
# Nome iOS Design Evidence State

This packet maps approved product mockups to signed iOS smoke screenshots.

Summary:

- smoke manifest: $smoke_manifest
- coverage rows: $coverage_rows
- page evidence rows: $page_evidence_rows
- visual pass rows: $visual_pass_rows
- final functional blocked rows: $final_blocked_rows
- missing rows: $missing_rows
- smoke manifest rows: $smoke_label_rows

Files:

- summary.tsv
- coverage.tsv
- page_evidence.tsv
- missing.tsv

This is visual/navigation evidence. It does not claim final real-core
functional acceptance for invitation, group, public-contact, messaging, or
real-data settings paths.
EOF

echo "Nome iOS design evidence state"
echo "=============================="
echo "[INFO] Output: $output_dir"
echo "[INFO] Smoke manifest: $smoke_manifest"
echo "[INFO] Coverage rows: $coverage_rows"
echo "[INFO] Page evidence rows: $page_evidence_rows"
echo "[INFO] Missing rows: $missing_rows"
sed -n '1,12p' "$coverage"

if [ "$coverage_rows" -ne 7 ] || [ "$page_evidence_rows" -ne 7 ] || [ "$missing_rows" -ne 0 ]; then
  echo "[FAIL] Design evidence packet is incomplete" >&2
  exit 1
fi

echo "[PASS] Exported Nome iOS design evidence state"
