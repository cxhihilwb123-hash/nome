#!/usr/bin/env bash

set -euo pipefail

manifest="${SMOKE_MANIFEST:-/tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv}"
output_dir="/tmp/nome-ios-smoke-visual-quality-$(date +%Y%m%d-%H%M%S)"
expected_count=0
min_bytes=50000
force=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/check-nome-smoke-visual-quality.sh [options]

Checks that a Nome iOS smoke screenshot manifest points at real, decodable PNG
screenshots whose dimensions, byte sizes, and hashes match the manifest.

Options:
  --manifest FILE       Smoke manifest from scripts/ios/smoke-nome-ui.sh.
  --output DIR          Output directory.
  --expected-count N    Require exactly N screenshot rows. Disabled by default.
  --min-bytes N         Minimum actual file size for each screenshot. Default:
                        50000.
  --force               Replace an existing output directory.
  -h, --help            Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --manifest)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --manifest requires a file" >&2
        exit 2
      fi
      manifest="$1"
      ;;
    --output)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --output requires a directory" >&2
        exit 2
      fi
      output_dir="$1"
      ;;
    --expected-count)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --expected-count requires a number" >&2
        exit 2
      fi
      expected_count="$1"
      ;;
    --min-bytes)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --min-bytes requires a number" >&2
        exit 2
      fi
      min_bytes="$1"
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

if [ ! -f "$manifest" ]; then
  echo "[FAIL] Smoke manifest not found: $manifest" >&2
  exit 1
fi

if ! [[ "$expected_count" =~ ^[0-9]+$ ]] || ! [[ "$min_bytes" =~ ^[0-9]+$ ]]; then
  echo "[FAIL] --expected-count and --min-bytes must be non-negative integers" >&2
  exit 2
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
images="$output_dir/images.tsv"
failures="$output_dir/failures.tsv"
hashes="$output_dir/hashes.tsv"

sha256_file() {
  shasum -a 256 "$1" | awk '{print $1}'
}

file_bytes() {
  wc -c < "$1" | tr -d '[:space:]'
}

sips_value() {
  local file="$1"
  local key="$2"

  sips -g "$key" "$file" 2>/dev/null | awk -F ': ' -v key="$key" '$1 ~ key {print $2; found=1; exit} END {exit found ? 0 : 1}'
}

record_failure() {
  local label="$1"
  local issue="$2"
  local expected="$3"
  local actual="$4"
  local path="$5"

  printf '%s\t%s\t%s\t%s\t%s\n' "$label" "$issue" "$expected" "$actual" "$path" >> "$failures"
}

printf 'label\tstatus\tpath\tmanifest_width\tmanifest_height\tactual_width\tactual_height\tmanifest_bytes\tactual_bytes\tmanifest_sha256\tactual_sha256\thas_alpha\tissues\n' > "$images"
printf 'label\tissue\texpected\tactual\tpath\n' > "$failures"
printf 'sha256\tlabel\tpath\n' > "$hashes"

header="$(sed -n '1p' "$manifest")"
expected_header=$'label\targs\twidth\theight\tbytes\tsha256\tpath'
if [ "$header" != "$expected_header" ]; then
  record_failure "__manifest__" "unexpected_header" "$expected_header" "$header" "$manifest"
fi

row_count=0
while IFS= read -r line; do
  label="$(printf '%s\n' "$line" | cut -f1)"
  manifest_width="$(printf '%s\n' "$line" | cut -f3)"
  manifest_height="$(printf '%s\n' "$line" | cut -f4)"
  manifest_bytes="$(printf '%s\n' "$line" | cut -f5)"
  manifest_sha="$(printf '%s\n' "$line" | cut -f6)"
  path="$(printf '%s\n' "$line" | cut -f7)"

  [ -n "${label:-}" ] || continue

  row_count=$((row_count + 1))
  status="PASS"
  issues=""
  actual_width=""
  actual_height=""
  actual_bytes=""
  actual_sha=""
  has_alpha=""

  add_issue() {
    local issue="$1"
    local expected="$2"
    local actual="$3"

    status="FAIL"
    if [ -n "$issues" ]; then
      issues="$issues,$issue"
    else
      issues="$issue"
    fi
    record_failure "$label" "$issue" "$expected" "$actual" "$path"
  }

  if [ ! -f "$path" ]; then
    add_issue "missing_file" "existing PNG file" "missing"
  else
    actual_bytes="$(file_bytes "$path")"
    actual_sha="$(sha256_file "$path")"

    if [ "$actual_bytes" != "$manifest_bytes" ]; then
      add_issue "byte_mismatch" "$manifest_bytes" "$actual_bytes"
    fi

    if [ "$actual_sha" != "$manifest_sha" ]; then
      add_issue "sha256_mismatch" "$manifest_sha" "$actual_sha"
    fi

    if [ "$actual_bytes" -lt "$min_bytes" ]; then
      add_issue "too_small" ">=$min_bytes" "$actual_bytes"
    fi

    if ! actual_width="$(sips_value "$path" pixelWidth)"; then
      add_issue "decode_failed" "sips pixelWidth" "unreadable"
    fi

    if ! actual_height="$(sips_value "$path" pixelHeight)"; then
      add_issue "decode_failed" "sips pixelHeight" "unreadable"
    fi

    has_alpha="$(sips_value "$path" hasAlpha 2>/dev/null || printf 'unknown')"

    if [ -n "$actual_width" ] && [ "$actual_width" != "$manifest_width" ]; then
      add_issue "width_mismatch" "$manifest_width" "$actual_width"
    fi

    if [ -n "$actual_height" ] && [ "$actual_height" != "$manifest_height" ]; then
      add_issue "height_mismatch" "$manifest_height" "$actual_height"
    fi

    if [ -n "$actual_width" ] && [ -n "$actual_height" ] && [ "$actual_height" -le "$actual_width" ]; then
      add_issue "not_portrait" "height greater than width" "${actual_width}x${actual_height}"
    fi

    printf '%s\t%s\t%s\n' "$actual_sha" "$label" "$path" >> "$hashes"
  fi

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$label" \
    "$status" \
    "$path" \
    "$manifest_width" \
    "$manifest_height" \
    "$actual_width" \
    "$actual_height" \
    "$manifest_bytes" \
    "$actual_bytes" \
    "$manifest_sha" \
    "$actual_sha" \
    "$has_alpha" \
    "${issues:-none}" >> "$images"
done < <(tail -n +2 "$manifest")

if [ "$expected_count" -gt 0 ] && [ "$row_count" -ne "$expected_count" ]; then
  record_failure "__manifest__" "row_count_mismatch" "$expected_count" "$row_count" "$manifest"
fi

tail -n +2 "$hashes" | awk -F '\t' 'NF {counts[$1]++; labels[$1]=labels[$1] "," $2; paths[$1]=paths[$1] "," $3} END {for (hash in counts) if (counts[hash] > 1) print hash "\t" labels[hash] "\t" paths[hash]}' |
while IFS=$'\t' read -r duplicate_hash duplicate_labels duplicate_paths; do
  record_failure "__manifest__" "duplicate_sha256" "unique screenshot hashes" "$duplicate_hash$duplicate_labels" "$duplicate_paths"
done

image_fail_count="$(awk -F '\t' 'NR > 1 && $2 == "FAIL" {count++} END {print count + 0}' "$images")"
failure_count="$(tail -n +2 "$failures" | awk 'NF {count++} END {print count + 0}')"
pass_count="$(awk -F '\t' 'NR > 1 && $2 == "PASS" {count++} END {print count + 0}' "$images")"

printf 'key\tvalue\n' > "$summary"
printf 'manifest\t%s\n' "$manifest" >> "$summary"
printf 'expected_count\t%s\n' "$expected_count" >> "$summary"
printf 'image_rows\t%s\n' "$row_count" >> "$summary"
printf 'pass_count\t%s\n' "$pass_count" >> "$summary"
printf 'image_fail_count\t%s\n' "$image_fail_count" >> "$summary"
printf 'failure_count\t%s\n' "$failure_count" >> "$summary"
printf 'min_bytes\t%s\n' "$min_bytes" >> "$summary"
printf 'images_tsv\t%s\n' "$images" >> "$summary"
printf 'failures_tsv\t%s\n' "$failures" >> "$summary"

cat > "$output_dir/README.md" <<EOF
# Nome iOS Smoke Visual Quality

This packet validates that the smoke manifest points at decodable screenshot
PNGs and that the manifest metadata matches the files currently on disk.

Summary:

- manifest: $manifest
- expected rows: $expected_count
- image rows: $row_count
- pass rows: $pass_count
- failure rows: $failure_count

Files:

- summary.tsv
- images.tsv
- failures.tsv
- hashes.tsv
EOF

echo "Nome iOS smoke visual quality"
echo "============================="
echo "[INFO] Output: $output_dir"
echo "[INFO] Manifest: $manifest"
echo "[INFO] Image rows: $row_count"
echo "[INFO] Failure rows: $failure_count"
sed -n '1,20p' "$images"

if [ "$failure_count" -gt 0 ]; then
  echo "[FAIL] Smoke visual quality check found issues" >&2
  sed -n '1,40p' "$failures" >&2
  exit 1
fi

echo "[PASS] Smoke visual quality check passed"
