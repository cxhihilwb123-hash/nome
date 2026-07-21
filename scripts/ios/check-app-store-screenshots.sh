#!/usr/bin/env bash

set -u

DIR="design/app-store/ios-real-screens"
MODE="candidate"

usage() {
  cat <<'USAGE'
Usage: scripts/ios/check-app-store-screenshots.sh [--final] [--dir DIR]

Checks Nome iOS screenshot candidates in PNG, JPG, or JPEG format. Candidate
mode allows planning warnings. Final mode fails when the set exceeds App Store
screenshot count limits or still contains non-final filenames or blocker notes.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --final)
      MODE="final"
      ;;
    --dir)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --dir requires a directory path" >&2
        exit 2
      fi
      DIR="$1"
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      DIR="$1"
      ;;
  esac
  shift
done

fail=0
warnings=0

fail_msg() {
  echo "[FAIL] $1"
  fail=1
}

warn_msg() {
  echo "[WARN] $1"
  warnings=$((warnings + 1))
}

pass_msg() {
  echo "[PASS] $1"
}

find_screenshot_files() {
  find "$DIR" -maxdepth 1 -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' \) | sort
}

manifest_blocker_lines() {
  grep -Ein 'needs-real-core|preview-core|debug[- ]?(only|preview)?|not a final|must be replaced|replace before public release' "$DIR/MANIFEST.md" 2>/dev/null || true
}

final_blocker_lines() {
  grep -Ein '^\|.*needs-real-core.*\||Current blocker count: [1-9][0-9]*|must be replaced' "$DIR/FINAL_BLOCKERS.md" 2>/dev/null || true
}

is_accepted_iphone_size() {
  case "$1x$2" in
    1260x2736|2736x1260|1290x2796|2796x1290|1320x2868|2868x1320|\
    1284x2778|2778x1284|1242x2688|2688x1242|\
    1179x2556|2556x1179|1206x2622|2622x1206|\
    1170x2532|2532x1170|1125x2436|2436x1125|1080x2340|2340x1080|\
    1242x2208|2208x1242|750x1334|1334x750|\
    640x1096|1096x640|640x1136|1136x640|1136x600|600x1136|\
    640x920|920x640|640x960|960x640|960x600|600x960)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

if ! command -v sips >/dev/null 2>&1; then
  echo "[FAIL] sips is required on macOS" >&2
  exit 2
fi

if ! command -v shasum >/dev/null 2>&1; then
  echo "[FAIL] shasum is required" >&2
  exit 2
fi

if [ ! -d "$DIR" ]; then
  echo "[FAIL] Screenshot directory does not exist: $DIR" >&2
  exit 2
fi

if [ ! -f "$DIR/MANIFEST.md" ]; then
  fail_msg "Missing screenshot manifest: $DIR/MANIFEST.md"
else
  manifest_blockers="$(manifest_blocker_lines)"
  if [ -n "$manifest_blockers" ]; then
    if [ "$MODE" = "final" ]; then
      fail_msg "Screenshot manifest still contains non-final preview/debug/needs-real-core status"
    else
      warn_msg "Screenshot manifest still contains non-final preview/debug/needs-real-core status"
    fi
    while IFS= read -r blocker; do
      [ -n "$blocker" ] || continue
      echo "[INFO] manifest-blocker $blocker"
    done <<< "$manifest_blockers"
  fi
fi

if [ -f "$DIR/FINAL_BLOCKERS.md" ]; then
  final_blockers="$(final_blocker_lines)"
  if [ -n "$final_blockers" ]; then
    if [ "$MODE" = "final" ]; then
      fail_msg "Final blocker ledger still records screenshot replacement requirements"
    else
      warn_msg "Final blocker ledger still records screenshot replacement requirements"
    fi
    while IFS= read -r blocker; do
      [ -n "$blocker" ] || continue
      echo "[INFO] final-blocker $blocker"
    done <<< "$final_blockers"
  fi
fi

count=$(find_screenshot_files | wc -l | tr -d '[:space:]')

if [ "$count" -eq 0 ]; then
  fail_msg "No PNG/JPG/JPEG screenshots found in $DIR"
else
  pass_msg "Found $count screenshot candidate(s)"
fi

if [ "$count" -gt 10 ]; then
  if [ "$MODE" = "final" ]; then
    fail_msg "Final App Store set has $count screenshots; maximum is 10 per localization"
  else
    warn_msg "Candidate set has $count screenshots; final App Store set must select 10 or fewer"
  fi
fi

while IFS= read -r file; do
  [ -n "$file" ] || continue

  name=$(basename "$file")
  size=$(wc -c < "$file" | tr -d '[:space:]')
  width=$(sips -g pixelWidth "$file" 2>/dev/null | awk '/pixelWidth/{print $2}')
  height=$(sips -g pixelHeight "$file" 2>/dev/null | awk '/pixelHeight/{print $2}')
  alpha=$(sips -g hasAlpha "$file" 2>/dev/null | awk '/hasAlpha/{print $2}')
  sha=$(shasum -a 256 "$file" | awk '{print $1}')
  lower_name=$(printf '%s' "$name" | tr '[:upper:]' '[:lower:]')

  case "$lower_name" in
    *.jpg|*.jpeg)
      alpha="no"
      ;;
  esac

  if [ "$size" -le 0 ]; then
    fail_msg "$name is empty"
  fi

  if [ -z "$width" ] || [ -z "$height" ]; then
    fail_msg "$name dimensions could not be read"
  elif is_accepted_iphone_size "$width" "$height"; then
    pass_msg "$name size ${width}x${height} is an accepted iPhone screenshot size"
  else
    fail_msg "$name size ${width}x${height} is not in the accepted iPhone screenshot-size list"
  fi

  if [ "$alpha" = "yes" ]; then
    warn_msg "$name has alpha; flatten final store export"
  fi

  case "$name" in
    *preview*|*debug*|*needs-real-core*)
      if [ "$MODE" = "final" ]; then
        fail_msg "$name is not final-release ready and must not be in the final App Store set"
      else
        warn_msg "$name is not final-release ready; replace before public release"
      fi
      ;;
  esac

  echo "[INFO] $name bytes=$size alpha=${alpha:-unknown} sha256=$sha"
done < <(find_screenshot_files)

if [ "$fail" -ne 0 ]; then
  echo "[FAIL] Screenshot check completed with failures"
  exit 1
fi

if [ "$warnings" -gt 0 ]; then
  echo "[PASS] Screenshot check completed with $warnings warning(s)"
else
  echo "[PASS] Screenshot check completed without warnings"
fi
