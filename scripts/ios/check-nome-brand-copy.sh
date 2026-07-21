#!/bin/bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
status=0

fail() {
  printf '[FAIL] %s\n' "$1"
  status=1
}

pass() {
  printf '[PASS] %s\n' "$1"
}

check_swift_forbidden_phrase() {
  local phrase="$1"
  local matches

  matches="$(rg -n --fixed-strings "$phrase" "$root_dir/apps/ios/Shared" --glob '*.swift' || true)"
  if [ -n "$matches" ]; then
    fail "Forbidden Nome-facing Swift phrase remains: $phrase"
    printf '%s\n' "$matches"
  fi
}

check_zh_value_forbidden_phrase() {
  local phrase="$1"
  local file="$root_dir/apps/ios/zh-Hans.lproj/Localizable.strings"
  local matches

  matches="$(
    awk -F ' = ' 'NF > 1 {print FILENAME ":" FNR ":" $2}' "$file" \
      | rg --fixed-strings "$phrase" || true
  )"

  if [ -n "$matches" ]; then
    fail "Forbidden Nome-facing Simplified Chinese value remains: $phrase"
    printf '%s\n' "$matches"
  fi
}

check_swift_visible_original_brand() {
  local matches

  matches="$(
    rg -n \
      'Text\("[^"\n]*SimpleX|Button\("[^"\n]*SimpleX|Label\("[^"\n]*SimpleX|title: Text\("[^"\n]*SimpleX|message: Text\("[^"\n]*SimpleX|description: "[^"\n]*SimpleX|subtitle: "[^"\n]*SimpleX|title: "[^"\n]*SimpleX|NSLocalizedString\("[^"\n]*SimpleX|msgNotAllowedView\("[^"\n]*SimpleX' \
      "$root_dir/apps/ios/Shared/Views" --glob '*.swift' || true
  )"

  if [ -n "$matches" ]; then
    fail "Original project name remains in a user-facing Swift string"
    printf '%s\n' "$matches"
  fi
}

check_zh_values_for_original_brand() {
  local file="$root_dir/apps/ios/zh-Hans.lproj/Localizable.strings"
  local matches

  matches="$(
    awk -F ' = ' 'NF > 1 {print FNR ":" $2}' "$file" \
      | rg --fixed-strings "SimpleX" || true
  )"

  if [ -n "$matches" ]; then
    fail "Original project name remains in a Simplified Chinese display value"
    printf '%s\n' "$matches"
  fi
}

if ! command -v rg >/dev/null 2>&1; then
  echo "[FAIL] rg is required" >&2
  exit 2
fi

plutil -lint \
  "$root_dir/apps/ios/zh-Hans.lproj/Localizable.strings" \
  "$root_dir/apps/ios/zh-Hans.lproj/SimpleX--iOS--InfoPlist.strings" >/dev/null

swift_forbidden_phrases=(
  "SimpleX Lock"
  "Enable SimpleX Lock"
  "Disable SimpleX Lock"
  "SimpleX encrypted message"
  "SimpleX contacts"
  "Share to SimpleX"
  "Share with SimpleX contacts"
  "Let's talk in SimpleX Chat"
  "Connect to me via SimpleX Chat"
  "Create SimpleX address"
  "SimpleX Address"
  "SimpleX address"
)

zh_value_forbidden_phrases=(
  "SimpleX 已解锁"
  "SimpleX 锁"
  "分享到 SimpleX"
  "SimpleX Chat 里聊天"
  "通过 SimpleX Chat"
  "自己的 SimpleX 地址"
  "SimpleX 短地址"
)

for phrase in "${swift_forbidden_phrases[@]}"; do
  check_swift_forbidden_phrase "$phrase"
done

for phrase in "${zh_value_forbidden_phrases[@]}"; do
  check_zh_value_forbidden_phrase "$phrase"
done

check_swift_visible_original_brand
check_zh_values_for_original_brand

if [ "$status" -eq 0 ]; then
  pass "Nome brand-copy check passed"
fi

exit "$status"
