#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
project_file="${PROJECT_FILE:-$root_dir/apps/ios/SimpleX.xcodeproj/project.pbxproj}"
info_plist="${INFO_PLIST:-$root_dir/apps/ios/SimpleX--iOS--Info.plist}"
main_entitlements="${MAIN_ENTITLEMENTS:-$root_dir/apps/ios/SimpleX (iOS).entitlements}"
nse_entitlements="${NSE_ENTITLEMENTS:-$root_dir/apps/ios/SimpleX NSE/SimpleX NSE.entitlements}"
se_entitlements="${SE_ENTITLEMENTS:-$root_dir/apps/ios/SimpleX SE/SimpleX SE.entitlements}"
review_file="${REVIEW_FILE:-$root_dir/plans/20260709_nome_ios_release_gate_review.md}"
checklist="${CHECKLIST:-$root_dir/plans/20260709_nome_ios_manual_qa_checklist.md}"
output_dir="/tmp/nome-ios-release-identity-state-$(date +%Y%m%d-%H%M%S)"
release_bundle_base="${NOME_RELEASE_BUNDLE_BASE:-com.example.nome}"
release_domain="${NOME_RELEASE_DOMAIN:-nome.example}"
force=0
skip_gates=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/export-ios-release-identity-state.sh [options]

Exports a read-only release-identity state packet for the Nome iOS app. It
does not edit the Xcode project, entitlements, Info.plist files, or signing
settings.

Options:
  --output DIR   Output directory.
  --checklist FILE
                 Manual QA checklist used to anchor required decisions.
  --release-bundle-base ID
                 Candidate reverse-DNS bundle-id base. Default:
                 com.example.nome.
  --release-domain DOMAIN
                 Candidate Nome-owned associated-link domain. Default:
                 nome.example.
  --force        Replace an existing output directory.
  --skip-gates   Do not run identifier gates; write WARN rows instead.
  -h, --help     Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --output)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --output requires a directory" >&2
        exit 2
      fi
      output_dir="$1"
      ;;
    --checklist)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --checklist requires a file" >&2
        exit 2
      fi
      checklist="$1"
      ;;
    --release-bundle-base)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --release-bundle-base requires an identifier base" >&2
        exit 2
      fi
      release_bundle_base="$1"
      ;;
    --release-domain)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --release-domain requires a domain" >&2
        exit 2
      fi
      release_domain="$1"
      ;;
    --force)
      force=1
      ;;
    --skip-gates)
      skip_gates=1
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
gate_status="$output_dir/gate_status.tsv"
current_identifiers="$output_dir/current_identifiers.tsv"
proposed_identifiers="$output_dir/proposed_identifiers.tsv"
required_decisions="$output_dir/required_decisions.tsv"
next_actions="$output_dir/next_actions.tsv"

contains() {
  local file="$1"
  local token="$2"
  grep -Fq "$token" "$file" 2>/dev/null
}

project_values_for() {
  local key="$1"

  awk -F ' = ' -v key="$key" '$1 ~ "^[[:space:]]*" key "$" {gsub(/[;"]/, "", $2); print $2}' "$project_file" 2>/dev/null | sort -u
}

record_gate() {
  local status="$1"
  local name="$2"
  local log="$3"
  printf '%s\t%s\t%s\n' "$status" "$name" "$log" >> "$gate_status"
}

run_gate() {
  local status_on_fail="$1"
  local name="$2"
  local command="$3"
  local log="$output_dir/logs/$name.log"

  if [ "$skip_gates" -eq 1 ]; then
    printf 'Skipped by --skip-gates. Intended command:\n%s\n' "$command" > "$log"
    record_gate "WARN" "$name" "$log"
    return
  fi

  printf '$ %s\n\n' "$command" > "$log"
  if bash -c "$command" >> "$log" 2>&1; then
    record_gate "PASS" "$name" "$log"
  else
    record_gate "$status_on_fail" "$name" "$log"
  fi
}

identifier_status() {
  local value="$1"
  case "$value" in
    chat.simplex*|group.chat.simplex.app|'$(AppIdentifierPrefix)chat.simplex.app'|applinks:simplex.chat|applinks:www.simplex.chat|applinks:*.simplex.im|applinks:*.simplexonflux.com|chat.simplex.app.receive)
      printf 'compatibility_exception'
      ;;
    simplex)
      printf 'protocol_compatibility'
      ;;
    Nome|Nome\ Notifications|Nome\ Share)
      printf 'nome_facing'
      ;;
    *)
      printf 'review_required'
      ;;
  esac
}

write_current_identifier() {
  local surface="$1"
  local value="$2"
  local source="$3"
  local final_requirement="$4"
  local status

  status="$(identifier_status "$value")"
  printf '%s\t%s\t%s\t%s\t%s\n' "$surface" "$value" "$source" "$status" "$final_requirement" >> "$current_identifiers"
}

write_required_decision() {
  local priority="$1"
  local decision="$2"
  local current_value="$3"
  local release_requirement="$4"
  local dependency="$5"
  local anchor
  local location
  local manual_qa_line
  local manual_qa_section
  local blocker_group

  anchor="$(qa_anchor_for_decision "$decision")"
  location="$(qa_location_for_anchor "$anchor")"
  manual_qa_line="$(printf '%s\n' "$location" | cut -f1)"
  manual_qa_section="$(printf '%s\n' "$location" | cut -f2)"
  blocker_group="$(blocker_group_for_decision "$decision")"

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$priority" "$decision" "$current_value" "$release_requirement" "$dependency" "$anchor" "$manual_qa_line" "$manual_qa_section" "$blocker_group" >> "$required_decisions"
}

proposal_status() {
  case "$release_bundle_base:$release_domain" in
    com.example.nome:nome.example)
      printf 'placeholder'
      ;;
    *)
      printf 'candidate_requires_apple_team_review'
      ;;
  esac
}

write_proposed_identifier() {
  local priority="$1"
  local surface="$2"
  local current_value="$3"
  local proposed_value="$4"
  local update_targets="$5"
  local migration_dependency="$6"
  local verification="$7"

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$priority" \
    "$surface" \
    "$current_value" \
    "$proposed_value" \
    "$(proposal_status)" \
    "$update_targets" \
    "$migration_dependency" \
    "$verification" >> "$proposed_identifiers"
}

write_next_action() {
  local priority="$1"
  local decision="$2"
  local action="$3"
  local command="$4"
  local evidence="$5"
  local anchor
  local location
  local manual_qa_line
  local manual_qa_section
  local blocker_group

  anchor="$(qa_anchor_for_decision "$decision")"
  location="$(qa_location_for_anchor "$anchor")"
  manual_qa_line="$(printf '%s\n' "$location" | cut -f1)"
  manual_qa_section="$(printf '%s\n' "$location" | cut -f2)"
  blocker_group="$(blocker_group_for_decision "$decision")"

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$priority" "$decision" "$action" "$command" "$evidence" "$anchor" "$manual_qa_line" "$manual_qa_section" "$blocker_group" >> "$next_actions"
}

qa_anchor_for_decision() {
  case "$1" in
    main_app_bundle_id|extension_bundle_ids|internal_framework_and_tests)
      printf 'Nome-owned bundle identifiers are selected for the app, tests, notification service extension, share extension, and internal framework, or compatibility exceptions are explicitly approved for release.'
      ;;
    app_group|keychain_access_group)
      printf 'Nome-owned App Group and keychain access group names are selected with an explicit data-migration decision.'
      ;;
    associated_domains|background_task_id)
      printf 'Associated domains and background task identifiers are either Nome-owned or documented compatibility exceptions.'
      ;;
    url_scheme)
      printf 'Bundle id, URL schemes, app groups, notification extension, and share extension identifiers are reviewed before TestFlight.'
      ;;
    *)
      printf 'scripts/ios/check-ios-release-identifiers.sh passes for the final TestFlight or public distribution configuration.'
      ;;
  esac
}

blocker_group_for_decision() {
  case "$1" in
    main_app_bundle_id|extension_bundle_ids|internal_framework_and_tests|app_group|keychain_access_group|associated_domains|background_task_id)
      printf 'release_identifiers'
      ;;
    url_scheme)
      printf 'release_identifier_review'
      ;;
    *)
      printf 'release_followup'
      ;;
  esac
}

qa_location_for_anchor() {
  local anchor="$1"

  if [ ! -f "$checklist" ]; then
    printf '\t'
    return
  fi

  awk -v anchor="$anchor" '
    /^## / {
      section = substr($0, 4)
      next
    }
    index($0, anchor) > 0 {
      print NR "\t" section
      found = 1
      exit
    }
    END {
      if (!found) {
        print "\t"
      }
    }
  ' "$checklist"
}

printf 'status\tcheck\tlog\n' > "$gate_status"
run_gate "FAIL" "compatibility_reviewed_identifiers" "scripts/ios/check-ios-release-identifiers.sh --compatibility-reviewed"
run_gate "BLOCKED" "strict_release_identifiers" "scripts/ios/check-ios-release-identifiers.sh"

printf 'surface\tcurrent_value\tsource\tstatus\tfinal_requirement\n' > "$current_identifiers"

bundle_ids="$(project_values_for PRODUCT_BUNDLE_IDENTIFIER || true)"
if [ -n "$bundle_ids" ]; then
  while IFS= read -r bundle_id; do
    case "$bundle_id" in
      chat.simplex.app) surface="main_app_bundle_id" ;;
      chat.simplex.app.SimpleX-NSE) surface="notification_service_bundle_id" ;;
      chat.simplex.app.SimpleX-SE) surface="share_extension_bundle_id" ;;
      chat.simplex.SimpleXChat) surface="internal_framework_bundle_id" ;;
      chat.simplex.Tests-iOS) surface="ios_test_bundle_id" ;;
      *) surface="other_bundle_id" ;;
    esac
    write_current_identifier "$surface" "$bundle_id" "project.pbxproj PRODUCT_BUNDLE_IDENTIFIER" "Use a Nome-owned id or a documented release compatibility exception."
  done <<EOF
$bundle_ids
EOF
fi

if contains "$project_file" 'INFOPLIST_KEY_CFBundleDisplayName = Nome'; then
  write_current_identifier "main_app_display_name" "Nome" "project.pbxproj INFOPLIST_KEY_CFBundleDisplayName" "Must stay Nome-facing."
fi

if contains "$project_file" 'INFOPLIST_KEY_CFBundleDisplayName = "Nome Notifications"'; then
  write_current_identifier "notification_service_display_name" "Nome Notifications" "project.pbxproj INFOPLIST_KEY_CFBundleDisplayName" "Must stay Nome-facing."
fi

if contains "$project_file" 'INFOPLIST_KEY_CFBundleDisplayName = "Nome Share"'; then
  write_current_identifier "share_extension_display_name" "Nome Share" "project.pbxproj INFOPLIST_KEY_CFBundleDisplayName" "Must stay Nome-facing."
fi

if contains "$info_plist" '<string>simplex</string>'; then
  write_current_identifier "url_scheme" "simplex" "SimpleX--iOS--Info.plist CFBundleURLSchemes" "Keep only if SimpleX protocol/link compatibility remains required."
fi

if contains "$info_plist" '<string>chat.simplex.app</string>'; then
  write_current_identifier "url_type_name" "chat.simplex.app" "SimpleX--iOS--Info.plist CFBundleURLName" "Use a Nome-owned URL type name or a documented release compatibility exception."
fi

if contains "$info_plist" 'chat.simplex.app.receive'; then
  write_current_identifier "background_task_id" "chat.simplex.app.receive" "SimpleX--iOS--Info.plist BGTaskSchedulerPermittedIdentifiers" "Use a Nome-owned background task id when bundle ids migrate."
fi

for file in "$main_entitlements" "$nse_entitlements" "$se_entitlements"; do
  label="$(basename "$file")"
  if contains "$file" 'group.chat.simplex.app'; then
    write_current_identifier "app_group" "group.chat.simplex.app" "$label com.apple.security.application-groups" "Use a Nome-owned App Group or a documented data-migration exception."
  fi
  if contains "$file" '$(AppIdentifierPrefix)chat.simplex.app'; then
    write_current_identifier "keychain_access_group" '$(AppIdentifierPrefix)chat.simplex.app' "$label keychain-access-groups" "Use a Nome-owned keychain access group with an explicit migration plan."
  fi
done

for domain in \
  'applinks:simplex.chat' \
  'applinks:www.simplex.chat' \
  'applinks:*.simplex.im' \
  'applinks:*.simplexonflux.com'
do
  if contains "$main_entitlements" "$domain"; then
    write_current_identifier "associated_domain" "$domain" "SimpleX (iOS).entitlements associated domains" "Use Nome-owned domains and AASA files, or document a compatibility exception."
  fi
done

printf 'priority\tsurface\tcurrent_value\tproposed_value\tproposal_status\tupdate_targets\tmigration_dependency\tverification\n' > "$proposed_identifiers"
write_proposed_identifier 1 "main_app_bundle_id" "chat.simplex.app" "$release_bundle_base" "apps/ios/SimpleX.xcodeproj/project.pbxproj" "Apple Developer App ID, provisioning profile, existing local data/keychain migration decision" "scripts/ios/check-ios-release-identifiers.sh"
write_proposed_identifier 2 "notification_service_bundle_id" "chat.simplex.app.SimpleX-NSE" "$release_bundle_base.notification-service" "apps/ios/SimpleX.xcodeproj/project.pbxproj; apps/ios/SimpleX NSE/SimpleX NSE.entitlements" "Main app bundle id, push capability, notification filtering capability" "scripts/ios/check-ios-release-identifiers.sh"
write_proposed_identifier 3 "share_extension_bundle_id" "chat.simplex.app.SimpleX-SE" "$release_bundle_base.share-extension" "apps/ios/SimpleX.xcodeproj/project.pbxproj; apps/ios/SimpleX SE/SimpleX SE.entitlements" "Main app bundle id, share-extension provisioning, App Group migration" "scripts/ios/check-ios-release-identifiers.sh"
write_proposed_identifier 4 "internal_framework_bundle_id" "chat.simplex.SimpleXChat" "$release_bundle_base.chatcore" "apps/ios/SimpleX.xcodeproj/project.pbxproj" "Xcode target migration and build verification" "scripts/ios/check-ios-generic-device-build.sh"
write_proposed_identifier 5 "ios_test_bundle_id" "chat.simplex.Tests-iOS" "$release_bundle_base.tests-ios" "apps/ios/SimpleX.xcodeproj/project.pbxproj" "Test target migration" "DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild -project apps/ios/SimpleX.xcodeproj -scheme \"SimpleX (iOS)\" -list"
write_proposed_identifier 6 "app_group" "group.chat.simplex.app" "group.$release_bundle_base" "apps/ios/SimpleX (iOS).entitlements; apps/ios/SimpleX NSE/SimpleX NSE.entitlements; apps/ios/SimpleX SE/SimpleX SE.entitlements" "Shared container migration for app, NSE, and share extension" "scripts/ios/check-ios-device-readiness.sh"
write_proposed_identifier 7 "keychain_access_group" '$(AppIdentifierPrefix)chat.simplex.app' "\$(AppIdentifierPrefix)$release_bundle_base" "apps/ios/SimpleX (iOS).entitlements; apps/ios/SimpleX NSE/SimpleX NSE.entitlements; apps/ios/SimpleX SE/SimpleX SE.entitlements" "Credential/keychain migration or explicit reset behavior" "scripts/ios/check-ios-device-readiness.sh"
write_proposed_identifier 8 "background_task_id" "chat.simplex.app.receive" "$release_bundle_base.receive" "apps/ios/SimpleX--iOS--Info.plist" "Background delivery behavior after bundle-id migration" "scripts/ios/check-ios-release-identifiers.sh"
write_proposed_identifier 9 "url_type_name" "chat.simplex.app" "$release_bundle_base" "apps/ios/SimpleX--iOS--Info.plist" "Deep-link routing and App Store identity review" "scripts/ios/check-ios-release-identifiers.sh"
write_proposed_identifier 10 "url_scheme" "simplex" "simplex" "apps/ios/SimpleX--iOS--Info.plist" "Keep only while SimpleX-compatible invitation/contact links are a product requirement" "scripts/ios/check-ios-release-identifiers.sh --compatibility-reviewed"
write_proposed_identifier 11 "associated_domains" "simplex.chat, www.simplex.chat, *.simplex.im, *.simplexonflux.com" "applinks:$release_domain, applinks:www.$release_domain" "apps/ios/SimpleX (iOS).entitlements; public AASA files for $release_domain" "Nome-owned web domain, AASA hosting, and compatibility-domain decision" "scripts/ios/check-ios-release-identifiers.sh"

printf 'priority\tdecision\tcurrent_value\trelease_requirement\tdependency\tmanual_qa_anchor\tmanual_qa_line\tmanual_qa_section\tblocker_group\n' > "$required_decisions"
write_required_decision 1 "main_app_bundle_id" "chat.simplex.app" "Choose a Nome-owned main app bundle id or explicitly approve compatibility for release." "Apple Developer account app id and data migration decision"
write_required_decision 2 "extension_bundle_ids" "chat.simplex.app.SimpleX-NSE, chat.simplex.app.SimpleX-SE" "Choose Nome-owned extension bundle ids or explicitly approve compatibility for release." "Main app bundle id, push/share extension provisioning"
write_required_decision 3 "internal_framework_and_tests" "chat.simplex.SimpleXChat, chat.simplex.Tests-iOS" "Choose Nome-owned framework/test ids or keep as internal compatibility exceptions." "Xcode target migration"
write_required_decision 4 "app_group" "group.chat.simplex.app" "Choose a Nome-owned App Group or document why the upstream group remains required." "Extension data sharing and migration"
write_required_decision 5 "keychain_access_group" '$(AppIdentifierPrefix)chat.simplex.app' "Choose a Nome-owned keychain group with migration or document the release exception." "Existing credentials and local database access"
write_required_decision 6 "associated_domains" "simplex.chat, www.simplex.chat, *.simplex.im, *.simplexonflux.com" "Add/replace with Nome-owned domains after AASA files exist, or document compatibility." "Public Nome domain ownership and link strategy"
write_required_decision 7 "background_task_id" "chat.simplex.app.receive" "Rename with the bundle-id migration or explicitly preserve compatibility." "Background delivery behavior"
write_required_decision 8 "url_scheme" "simplex" "Keep only while SimpleX-compatible links remain a product requirement." "Invitation/contact-link compatibility"

printf 'priority\tdecision_scope\taction\tcommand\tevidence\tmanual_qa_anchor\tmanual_qa_line\tmanual_qa_section\tblocker_group\n' > "$next_actions"
write_next_action 1 "url_scheme" "Keep using --compatibility-reviewed for development passes while real-core and migration work remains open." "scripts/ios/check-ios-release-identifiers.sh --compatibility-reviewed" "$gate_status"
write_next_action 2 "main_app_bundle_id" "Before TestFlight/public distribution, choose final Nome-owned identifiers or record explicit release exceptions." "scripts/ios/check-ios-release-identifiers.sh" "$required_decisions"
write_next_action 3 "app_group" "If identifiers change, update bundle ids, App Groups, keychain groups, associated domains, and background task ids as one migration." "plans/20260709_nome_ios_release_gate_review.md" "$current_identifiers"
write_next_action 4 "main_app_bundle_id" "After Apple/team/domain approval, dry-run or apply the checked migration proposal." "scripts/ios/apply-ios-release-identifiers.sh --proposal $proposed_identifiers --output /tmp/nome-ios-release-identity-apply --force" "$proposed_identifiers"

compat_status="$(awk -F '\t' 'NR > 1 && $2 == "compatibility_reviewed_identifiers" {print $1; exit}' "$gate_status")"
strict_status="$(awk -F '\t' 'NR > 1 && $2 == "strict_release_identifiers" {print $1; exit}' "$gate_status")"
identifier_count="$(tail -n +2 "$current_identifiers" | awk 'NF {count++} END {print count + 0}')"
proposal_count="$(tail -n +2 "$proposed_identifiers" | awk 'NF {count++} END {print count + 0}')"
decision_count="$(tail -n +2 "$required_decisions" | awk 'NF {count++} END {print count + 0}')"
next_action_count="$(tail -n +2 "$next_actions" | awk 'NF {count++} END {print count + 0}')"

printf 'key\tvalue\n' > "$summary"
printf 'review_file\t%s\n' "$review_file" >> "$summary"
printf 'checklist\t%s\n' "$checklist" >> "$summary"
printf 'compatibility_reviewed_status\t%s\n' "${compat_status:-unknown}" >> "$summary"
printf 'strict_release_status\t%s\n' "${strict_status:-unknown}" >> "$summary"
printf 'release_bundle_base\t%s\n' "$release_bundle_base" >> "$summary"
printf 'release_domain\t%s\n' "$release_domain" >> "$summary"
printf 'proposal_status\t%s\n' "$(proposal_status)" >> "$summary"
printf 'identifier_rows\t%s\n' "$identifier_count" >> "$summary"
printf 'proposed_identifier_rows\t%s\n' "$proposal_count" >> "$summary"
printf 'required_decision_rows\t%s\n' "$decision_count" >> "$summary"
printf 'next_action_rows\t%s\n' "$next_action_count" >> "$summary"
printf 'current_identifiers\t%s\n' "$current_identifiers" >> "$summary"
printf 'proposed_identifiers\t%s\n' "$proposed_identifiers" >> "$summary"
printf 'required_decisions\t%s\n' "$required_decisions" >> "$summary"
printf 'next_actions\t%s\n' "$next_actions" >> "$summary"

cat > "$output_dir/README.md" <<EOF
# Nome iOS Release Identity State

This packet records the current release-sensitive identifiers and the remaining
decisions before a Nome-owned TestFlight or public release track.

Summary:

- compatibility-reviewed gate: ${compat_status:-unknown}
- strict final release gate: ${strict_status:-unknown}
- proposed identifier status: $(proposal_status)
- proposed bundle base: $release_bundle_base
- proposed associated-link domain: $release_domain
- current identifier rows: $identifier_count
- proposed identifier rows: $proposal_count
- required decision rows: $decision_count

Files:

- summary.tsv
- gate_status.tsv
- current_identifiers.tsv
- proposed_identifiers.tsv
- required_decisions.tsv
- next_actions.tsv
- logs/
EOF

echo "Nome iOS release identity state"
echo "==============================="
echo "[INFO] Output: $output_dir"
echo "[INFO] Compatibility-reviewed gate: ${compat_status:-unknown}"
echo "[INFO] Strict final gate: ${strict_status:-unknown}"
echo "[INFO] Proposed identifier status: $(proposal_status)"
echo "[INFO] Current identifier rows: $identifier_count"
echo "[INFO] Proposed identifier rows: $proposal_count"
echo "[INFO] Required decision rows: $decision_count"
sed -n '1,40p' "$next_actions"
echo "[PASS] Exported Nome iOS release identity state"

if [ "${compat_status:-FAIL}" = "FAIL" ]; then
  exit 1
fi
