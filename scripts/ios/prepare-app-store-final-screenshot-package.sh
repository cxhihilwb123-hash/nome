#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
draft_dir="$root_dir/design/app-store/ios-upload-draft-screens"
replacement_dir=""
output_dir="/tmp/nome-ios-app-store-final-package-$(date +%Y%m%d-%H%M%S)"
force=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/prepare-app-store-final-screenshot-package.sh [options]

Creates a proposed final 10-screenshot App Store package from the current draft
plus four real-core replacement screenshots. It writes to a new output
directory and does not edit design/app-store/ios-upload-draft-screens.

Required replacement files in --replacement-dir:
  05-add-friend-real-core.(png|jpg|jpeg)
  06-public-contact-real-core.(png|jpg|jpeg)
  07-join-group-real-core.(png|jpg|jpeg)
  08-conversation-real-core.(png|jpg|jpeg)

Options:
  --draft DIR           Draft screenshot directory.
  --replacement-dir DIR Directory containing the four real-core screenshots.
  --output DIR          Output proposed final package directory.
  --force               Replace an existing output directory.
  -h, --help            Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --draft)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --draft requires a directory" >&2; exit 2; }
      draft_dir="$1"
      ;;
    --replacement-dir)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --replacement-dir requires a directory" >&2; exit 2; }
      replacement_dir="$1"
      ;;
    --output)
      shift
      [ "$#" -gt 0 ] || { echo "[FAIL] --output requires a directory" >&2; exit 2; }
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

if ! command -v sips >/dev/null 2>&1; then
  echo "[FAIL] sips is required on macOS" >&2
  exit 2
fi

if ! command -v shasum >/dev/null 2>&1; then
  echo "[FAIL] shasum is required" >&2
  exit 2
fi

if [ ! -d "$draft_dir" ]; then
  echo "[FAIL] Draft screenshot directory not found: $draft_dir" >&2
  exit 1
fi

if [ -z "$replacement_dir" ]; then
  echo "[FAIL] --replacement-dir is required" >&2
  exit 2
fi

if [ ! -d "$replacement_dir" ]; then
  echo "[FAIL] Replacement directory not found: $replacement_dir" >&2
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
manifest="$output_dir/MANIFEST.md"
blockers="$output_dir/FINAL_BLOCKERS.md"
package_tsv="$output_dir/package.tsv"
replacement_status="$output_dir/replacement_status.tsv"
final_check_log="$output_dir/logs/final_screenshot_check.log"

find_replacement() {
  local stem="$1"
  local candidate

  for ext in jpg jpeg png JPG JPEG PNG; do
    candidate="$replacement_dir/$stem.$ext"
    if [ -f "$candidate" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  return 1
}

write_image() {
  local slot="$1"
  local source_path="$2"
  local output_name="$3"
  local status="$4"
  local note="$5"
  local output_path="$output_dir/$output_name"

  if [ ! -f "$source_path" ]; then
    printf '%s\t%s\t%s\t%s\n' "$slot" "$output_name" "BLOCKED" "missing source: $source_path" >> "$package_tsv"
    return 1
  fi

  sips -s format jpeg -s formatOptions 95 "$source_path" --out "$output_path" >/dev/null

  width="$(sips -g pixelWidth "$output_path" 2>/dev/null | awk '/pixelWidth/{print $2}')"
  height="$(sips -g pixelHeight "$output_path" 2>/dev/null | awk '/pixelHeight/{print $2}')"
  bytes="$(wc -c < "$output_path" | tr -d '[:space:]')"
  sha="$(shasum -a 256 "$output_path" | awk '{print $1}')"
  source_name="$(basename "$source_path")"

  printf '| %s | `%s` | `%s` | `%sx%s` | `%s` | `%s` | `%s` | %s |\n' \
    "$slot" "$output_name" "$source_name" "$width" "$height" "$bytes" "$sha" "$status" "$note" >> "$manifest"
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$slot" "$output_name" "$source_path" "$status" "${width}x${height}" "$bytes" "$sha" "$note" >> "$package_tsv"
}

printf 'slot\toutput_file\tsource_file\tstatus\tdimensions\tbytes\tsha256\tnote\n' > "$package_tsv"
printf 'slot\trequired_stem\tstatus\tsource_file\tnote\n' > "$replacement_status"

required_replacements=(
  "5|05-add-friend-real-core|05-add-friend.jpg|Real one-time invitation link and QR screenshot captured from real iOS core."
  "6|06-public-contact-real-core|06-public-contact.jpg|Real public contact address screenshot captured from real iOS core."
  "7|07-join-group-real-core|07-join-group.jpg|Real group invite preview or join path screenshot captured from real iOS core."
  "8|08-conversation-real-core|08-conversation.jpg|Real two-account conversation screenshot captured from real iOS core."
)

missing_replacements=0
for replacement in "${required_replacements[@]}"; do
  IFS='|' read -r slot stem _output_name note <<< "$replacement"
  if source_path="$(find_replacement "$stem")"; then
    printf '%s\t%s\tPASS\t%s\t%s\n' "$slot" "$stem" "$source_path" "$note" >> "$replacement_status"
  else
    printf '%s\t%s\tBLOCKED\t\tMissing required replacement image.\n' "$slot" "$stem" >> "$replacement_status"
    missing_replacements=$((missing_replacements + 1))
  fi
done

if [ "$missing_replacements" -gt 0 ]; then
  printf 'key\tvalue\n' > "$summary"
  printf 'draft_dir\t%s\n' "$draft_dir" >> "$summary"
  printf 'replacement_dir\t%s\n' "$replacement_dir" >> "$summary"
  printf 'output_dir\t%s\n' "$output_dir" >> "$summary"
  printf 'missing_replacements\t%s\n' "$missing_replacements" >> "$summary"
  printf 'replacement_status\t%s\n' "$replacement_status" >> "$summary"
  echo "[BLOCKED] Missing $missing_replacements required real-core replacement screenshot(s)"
  sed -n '1,20p' "$replacement_status"
  exit 1
fi

cat > "$manifest" <<'HEADER'
# Nome iOS App Store final screenshot package proposal

This directory is generated by:

```bash
scripts/ios/prepare-app-store-final-screenshot-package.sh
```

It is a proposed final package assembled from the current ready draft images
plus real-core replacement screenshots.

| Slot | Export file | Source file | Dimensions | Bytes | SHA-256 | Status | Final-release note |
| --- | --- | --- | --- | --- | --- | --- | --- |
HEADER

write_image 1 "$draft_dir/01-onboarding-welcome.jpg" "01-onboarding-welcome.jpg" "ready" "Onboarding welcome."
write_image 2 "$draft_dir/02-onboarding-local-identity.jpg" "02-onboarding-local-identity.jpg" "ready" "Local identity onboarding."
write_image 3 "$draft_dir/03-home.jpg" "03-home.jpg" "ready" "Nome home screen."
write_image 4 "$draft_dir/04-contacts.jpg" "04-contacts.jpg" "ready" "Contacts tab."

for replacement in "${required_replacements[@]}"; do
  IFS='|' read -r slot stem output_name note <<< "$replacement"
  source_path="$(awk -F '\t' -v stem="$stem" 'NR > 1 && $2 == stem {print $4; exit}' "$replacement_status")"
  write_image "$slot" "$source_path" "$output_name" "ready" "$note"
done

write_image 9 "$draft_dir/09-settings.jpg" "09-settings.jpg" "ready" "Settings tab."
write_image 10 "$draft_dir/10-help.jpg" "10-help.jpg" "ready" "Help and feedback."

cat > "$blockers" <<'EOF'
# Nome iOS App Store final blockers

No final screenshot blockers are recorded for this generated package.
EOF

cat > "$output_dir/README.md" <<EOF
# Nome iOS App Store Final Screenshot Package Proposal

This package contains ten flattened JPEG screenshots.

Source draft:

- $draft_dir

Replacement source:

- $replacement_dir

Validation:

\`\`\`bash
scripts/ios/check-app-store-screenshots.sh --final --dir "$output_dir"
\`\`\`
EOF

printf '$ scripts/ios/check-app-store-screenshots.sh --final --dir %q\n\n' "$output_dir" > "$final_check_log"
if scripts/ios/check-app-store-screenshots.sh --final --dir "$output_dir" >> "$final_check_log" 2>&1; then
  final_status="PASS"
else
  final_status="FAIL"
fi

slot_count="$(tail -n +2 "$package_tsv" | awk 'NF {count++} END {print count + 0}')"
fail_count="$(awk -F '\t' 'NR > 1 && $4 != "ready" {count++} END {print count + 0}' "$package_tsv")"

printf 'key\tvalue\n' > "$summary"
printf 'draft_dir\t%s\n' "$draft_dir" >> "$summary"
printf 'replacement_dir\t%s\n' "$replacement_dir" >> "$summary"
printf 'output_dir\t%s\n' "$output_dir" >> "$summary"
printf 'slot_count\t%s\n' "$slot_count" >> "$summary"
printf 'missing_replacements\t0\n' >> "$summary"
printf 'package_fail_count\t%s\n' "$fail_count" >> "$summary"
printf 'strict_final_status\t%s\n' "$final_status" >> "$summary"
printf 'package_tsv\t%s\n' "$package_tsv" >> "$summary"
printf 'replacement_status\t%s\n' "$replacement_status" >> "$summary"
printf 'manifest\t%s\n' "$manifest" >> "$summary"
printf 'final_blockers\t%s\n' "$blockers" >> "$summary"
printf 'final_check_log\t%s\n' "$final_check_log" >> "$summary"

echo "Nome iOS App Store final screenshot package"
echo "==========================================="
echo "[INFO] Output: $output_dir"
echo "[INFO] Slots: $slot_count"
echo "[INFO] Strict final check: $final_status"
sed -n '1,14p' "$package_tsv"

if [ "$final_status" != "PASS" ] || [ "$fail_count" -gt 0 ]; then
  echo "[FAIL] Proposed final screenshot package is not ready" >&2
  exit 1
fi

echo "[PASS] Proposed final screenshot package is ready"
