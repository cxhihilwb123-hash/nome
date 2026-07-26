#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
project_file="${PROJECT_FILE:-$root_dir/apps/ios/SimpleX.xcodeproj/project.pbxproj}"
info_plist="${INFO_PLIST:-$root_dir/apps/ios/SimpleX--iOS--Info.plist}"
main_entitlements="${MAIN_ENTITLEMENTS:-$root_dir/apps/ios/SimpleX (iOS).entitlements}"
nse_entitlements="${NSE_ENTITLEMENTS:-$root_dir/apps/ios/SimpleX NSE/SimpleX NSE.entitlements}"
se_entitlements="${SE_ENTITLEMENTS:-$root_dir/apps/ios/SimpleX SE/SimpleX SE.entitlements}"
proposal_file=""
output_dir="/tmp/nome-ios-release-identity-apply-$(date +%Y%m%d-%H%M%S)"
apply=0
force=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/apply-ios-release-identifiers.sh --proposal FILE [options]

Dry-runs or applies a checked Nome iOS release-identifier proposal. The default
mode is dry-run; it writes a change plan without editing project files.

Options:
  --proposal FILE  proposed_identifiers.tsv from export-ios-release-identity-state.sh.
  --output DIR     Output evidence directory.
  --apply          Actually edit project, plist, and entitlement files.
  --force          Replace an existing output directory.
  -h, --help       Show this help.
USAGE
}

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --proposal)
      shift
      [ "$#" -gt 0 ] || fail "--proposal requires a file"
      proposal_file="$1"
      ;;
    --output)
      shift
      [ "$#" -gt 0 ] || fail "--output requires a directory"
      output_dir="$1"
      ;;
    --apply)
      apply=1
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

[ -n "$proposal_file" ] || {
  usage >&2
  exit 2
}
[ -f "$proposal_file" ] || fail "Proposal file not found: $proposal_file"

for file in "$project_file" "$info_plist" "$main_entitlements" "$nse_entitlements" "$se_entitlements"; do
  [ -f "$file" ] || fail "Required file not found: $file"
done

if [ -e "$output_dir" ]; then
  if [ "$force" -ne 1 ]; then
    fail "Output already exists: $output_dir"
  fi
  rm -rf "$output_dir"
fi
mkdir -p "$output_dir/logs"

summary="$output_dir/summary.tsv"
changes="$output_dir/changes.tsv"
check_log="$output_dir/logs/proposal_check.log"
post_check_log="$output_dir/logs/post_check.log"

value_for_surface() {
  local surface="$1"
  awk -F '\t' -v surface="$surface" 'NR > 1 && $2 == surface {print $4; exit}' "$proposal_file"
}

require_value() {
  local surface="$1"
  local value

  value="$(value_for_surface "$surface")"
  [ -n "$value" ] || fail "Proposal is missing value for $surface"
  printf '%s' "$value"
}

contains_literal() {
  local file="$1"
  local value="$2"
  grep -Fq "$value" "$file"
}

record_change() {
  local target_file="$1"
  local surface="$2"
  local current_value="$3"
  local proposed_value="$4"
  local action="$5"

  printf '%s\t%s\t%s\t%s\t%s\n' "$target_file" "$surface" "$current_value" "$proposed_value" "$action" >> "$changes"
}

replace_literal() {
  local file="$1"
  local surface="$2"
  local current_value="$3"
  local proposed_value="$4"
  local action="missing-current-value"

  if contains_literal "$file" "$current_value"; then
    action="replace"
    if [ "$apply" -eq 1 ]; then
      CURRENT_VALUE="$current_value" PROPOSED_VALUE="$proposed_value" \
        perl -0pi -e 's/\Q$ENV{CURRENT_VALUE}\E/$ENV{PROPOSED_VALUE}/g' "$file"
    fi
  fi

  record_change "$file" "$surface" "$current_value" "$proposed_value" "$action"
}

plist_set_or_add_string() {
  local file="$1"
  local key_path="$2"
  local value="$3"

  /usr/libexec/PlistBuddy -c "Set $key_path $value" "$file" >/dev/null 2>&1 ||
    /usr/libexec/PlistBuddy -c "Add $key_path string $value" "$file" >/dev/null
}

plist_set_array_two_strings() {
  local file="$1"
  local array_key="$2"
  local value1="$3"
  local value2="$4"

  /usr/libexec/PlistBuddy -c "Delete $array_key" "$file" >/dev/null 2>&1 || true
  /usr/libexec/PlistBuddy -c "Add $array_key array" "$file" >/dev/null
  /usr/libexec/PlistBuddy -c "Add $array_key:0 string $value1" "$file" >/dev/null
  /usr/libexec/PlistBuddy -c "Add $array_key:1 string $value2" "$file" >/dev/null
}

main_bundle="$(require_value main_app_bundle_id)"
nse_bundle="$(require_value notification_service_bundle_id)"
se_bundle="$(require_value share_extension_bundle_id)"
framework_bundle="$(require_value internal_framework_bundle_id)"
test_bundle="$(require_value ios_test_bundle_id)"
app_group="$(require_value app_group)"
keychain_group="$(require_value keychain_access_group)"
background_task="$(require_value background_task_id)"
url_type="$(require_value url_type_name)"
associated_domains="$(require_value associated_domains)"
domain_1="$(printf '%s\n' "$associated_domains" | awk -F ', ' '{print $1}')"
domain_2="$(printf '%s\n' "$associated_domains" | awk -F ', ' '{print $2}')"

if ! scripts/ios/check-ios-release-identity-proposal.sh --proposal "$proposal_file" > "$check_log" 2>&1; then
  sed -n '1,220p' "$check_log" >&2
  fail "Proposal check failed"
fi

printf 'file\tsurface\tcurrent_value\tproposed_value\taction\n' > "$changes"

replace_literal "$project_file" "notification_service_bundle_id" "chat.simplex.app.SimpleX-NSE" "$nse_bundle"
replace_literal "$project_file" "share_extension_bundle_id" "chat.simplex.app.SimpleX-SE" "$se_bundle"
replace_literal "$project_file" "internal_framework_bundle_id" "chat.simplex.SimpleXChat" "$framework_bundle"
replace_literal "$project_file" "ios_test_bundle_id" "chat.simplex.Tests-iOS" "$test_bundle"
replace_literal "$project_file" "main_app_bundle_id" "chat.simplex.app" "$main_bundle"

if [ "$apply" -eq 1 ]; then
  plist_set_or_add_string "$info_plist" ":BGTaskSchedulerPermittedIdentifiers:0" "$background_task"
  plist_set_or_add_string "$info_plist" ":CFBundleURLTypes:0:CFBundleURLName" "$url_type"
fi
record_change "$info_plist" "background_task_id" "chat.simplex.app.receive" "$background_task" "$([ "$apply" -eq 1 ] && printf set || printf planned)"
record_change "$info_plist" "url_type_name" "chat.simplex.app" "$url_type" "$([ "$apply" -eq 1 ] && printf set || printf planned)"

for entitlements in "$main_entitlements" "$nse_entitlements" "$se_entitlements"; do
  if [ "$apply" -eq 1 ]; then
    plist_set_or_add_string "$entitlements" ":com.apple.security.application-groups:0" "$app_group"
    plist_set_or_add_string "$entitlements" ":keychain-access-groups:0" "$keychain_group"
  fi
  record_change "$entitlements" "app_group" "group.chat.simplex.app" "$app_group" "$([ "$apply" -eq 1 ] && printf set || printf planned)"
  record_change "$entitlements" "keychain_access_group" '$(AppIdentifierPrefix)chat.simplex.app' "$keychain_group" "$([ "$apply" -eq 1 ] && printf set || printf planned)"
done

if [ "$apply" -eq 1 ]; then
  plist_set_array_two_strings "$main_entitlements" ":com.apple.developer.associated-domains" "$domain_1" "$domain_2"
fi
record_change "$main_entitlements" "associated_domains" "upstream SimpleX domains" "$associated_domains" "$([ "$apply" -eq 1 ] && printf set || printf planned)"

if [ "$apply" -eq 1 ]; then
  if PROJECT_FILE="$project_file" \
    INFO_PLIST="$info_plist" \
    MAIN_ENTITLEMENTS="$main_entitlements" \
    NSE_ENTITLEMENTS="$nse_entitlements" \
    SE_ENTITLEMENTS="$se_entitlements" \
    scripts/ios/check-ios-release-identifiers.sh > "$post_check_log" 2>&1; then
    post_status="PASS"
  else
    post_status="FAIL"
  fi
else
  printf 'Dry-run only; post-apply strict identifier gate was not run.\n' > "$post_check_log"
  post_status="SKIPPED"
fi

change_rows="$(tail -n +2 "$changes" | awk 'NF {count++} END {print count + 0}')"
replace_rows="$(awk -F '\t' 'NR > 1 && ($5 == "replace" || $5 == "set") {count++} END {print count + 0}' "$changes")"
missing_rows="$(awk -F '\t' 'NR > 1 && $5 == "missing-current-value" {count++} END {print count + 0}' "$changes")"

printf 'key\tvalue\n' > "$summary"
printf 'mode\t%s\n' "$([ "$apply" -eq 1 ] && printf apply || printf dry-run)" >> "$summary"
printf 'proposal\t%s\n' "$proposal_file" >> "$summary"
printf 'main_bundle_id\t%s\n' "$main_bundle" >> "$summary"
printf 'associated_domains\t%s\n' "$associated_domains" >> "$summary"
printf 'change_rows\t%s\n' "$change_rows" >> "$summary"
printf 'replace_or_set_rows\t%s\n' "$replace_rows" >> "$summary"
printf 'missing_current_value_rows\t%s\n' "$missing_rows" >> "$summary"
printf 'post_apply_strict_gate\t%s\n' "$post_status" >> "$summary"
printf 'changes_tsv\t%s\n' "$changes" >> "$summary"

cat > "$output_dir/README.md" <<EOF
# Nome iOS Release Identifier Migration

Mode: $([ "$apply" -eq 1 ] && printf apply || printf dry-run)

This packet records the release identifier migration plan for:

- main bundle id: $main_bundle
- notification service: $nse_bundle
- share extension: $se_bundle
- App Group: $app_group
- keychain group: $keychain_group
- associated domains: $associated_domains

Files:

- summary.tsv
- changes.tsv
- logs/proposal_check.log
- logs/post_check.log
EOF

echo "Nome iOS release identifier migration"
echo "====================================="
echo "[INFO] Mode: $([ "$apply" -eq 1 ] && printf apply || printf dry-run)"
echo "[INFO] Output: $output_dir"
echo "[INFO] Proposed main bundle id: $main_bundle"
echo "[INFO] Change rows: $change_rows"
echo "[INFO] Missing current-value rows: $missing_rows"
echo "[INFO] Post-apply strict gate: $post_status"
sed -n '1,80p' "$changes"

if [ "$apply" -eq 1 ] && [ "$post_status" != "PASS" ]; then
  exit 1
fi

exit 0
