#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/capture-nome-accessibility-previews.sh"

fail() {
  printf '[FAIL] %s\n' "$1" >&2
  exit 1
}

all_cases="$("$script" --case-set all --list-cases)"
critical_cases="$("$script" --case-set critical --list-cases)"
conversation_cases="$("$script" --case-set conversation --list-cases)"

[ "$(printf '%s\n' "$all_cases" | wc -l | tr -d '[:space:]')" -eq 14 ] || fail "all case set must contain 14 rows"
[ "$(printf '%s\n' "$critical_cases" | wc -l | tr -d '[:space:]')" -eq 7 ] || fail "critical case set must contain 7 rows"
[ "$(printf '%s\n' "$conversation_cases" | wc -l | tr -d '[:space:]')" -eq 1 ] || fail "conversation case set must contain one row"
[ "$(printf '%s\n' "$conversation_cases" | cut -f1)" = "07-conversation-preview" ] || fail "conversation case set must contain only the conversation preview"

for label in 01-home 02-onboarding-welcome 06-chat-list-existing 07-conversation-preview 10-add-friend 12-public-contact 13-settings; do
  printf '%s\n' "$critical_cases" | grep -Fq "$label" || fail "critical case set is missing $label"
done

if "$script" --case-set unsupported --list-cases >/dev/null 2>&1; then
  fail "unsupported case set must fail"
fi

printf '%s\n' "$all_cases" | awk -F '\t' 'NF != 2 {exit 1}' || fail "all case rows must contain label and launch arguments"

printf '[PASS] Nome accessibility preview capture case sets\n'
