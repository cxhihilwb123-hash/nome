#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
proposal_file=""
status=0

usage() {
  cat <<'USAGE'
Usage: scripts/ios/check-ios-release-identity-proposal.sh --proposal FILE

Read-only checker for proposed Nome iOS release identifiers. It validates a
proposed_identifiers.tsv file from export-ios-release-identity-state.sh.

This script checks proposal consistency only. It does not prove Apple Developer
App ID availability, domain ownership, AASA hosting, provisioning, or data
migration.

Options:
  --proposal FILE  proposed_identifiers.tsv to check.
  -h, --help       Show this help.
USAGE
}

fail() {
  printf '[FAIL] %s\n' "$1"
  status=1
}

ok() {
  printf '[OK] %s\n' "$1"
}

warn() {
  printf '[WARN] %s\n' "$1"
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --proposal)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --proposal requires a file" >&2
        exit 2
      fi
      proposal_file="$1"
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

if [ -z "$proposal_file" ]; then
  echo "[FAIL] --proposal is required" >&2
  usage >&2
  exit 2
fi

if [ ! -f "$proposal_file" ]; then
  echo "[FAIL] Proposal file not found: $proposal_file" >&2
  exit 1
fi

value_for_surface() {
  local surface="$1"
  awk -F '\t' -v surface="$surface" 'NR > 1 && $2 == surface {print $4; exit}' "$proposal_file"
}

status_for_surface() {
  local surface="$1"
  awk -F '\t' -v surface="$surface" 'NR > 1 && $2 == surface {print $5; exit}' "$proposal_file"
}

is_reverse_dns_id() {
  case "$1" in
    *..*|.*|*.) return 1 ;;
  esac
  printf '%s\n' "$1" | grep -Eq '^[A-Za-z][A-Za-z0-9-]*(\.[A-Za-z0-9-]+){2,}$'
}

is_domain_name() {
  case "$1" in
    *..*|.*|*.) return 1 ;;
  esac
  printf '%s\n' "$1" | grep -Eq '^[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)+$'
}

has_surface() {
  local surface="$1"
  awk -F '\t' -v surface="$surface" 'NR > 1 && $2 == surface {found=1} END {exit found ? 0 : 1}' "$proposal_file"
}

cat <<'HEADER'
Nome iOS release identity proposal check
========================================
HEADER
printf '[INFO] Proposal: %s\n' "$proposal_file"

header="$(sed -n '1p' "$proposal_file")"
expected_header=$'priority\tsurface\tcurrent_value\tproposed_value\tproposal_status\tupdate_targets\tmigration_dependency\tverification'
if [ "$header" = "$expected_header" ]; then
  ok "Proposal header is valid"
else
  fail "Proposal header is invalid"
fi

row_count="$(tail -n +2 "$proposal_file" | awk 'NF {count++} END {print count + 0}')"
if [ "$row_count" -eq 11 ]; then
  ok "Proposal has 11 expected rows"
else
  fail "Proposal should have 11 rows, found $row_count"
fi

for surface in \
  main_app_bundle_id \
  notification_service_bundle_id \
  share_extension_bundle_id \
  internal_framework_bundle_id \
  ios_test_bundle_id \
  app_group \
  keychain_access_group \
  background_task_id \
  url_type_name \
  url_scheme \
  associated_domains
do
  if has_surface "$surface"; then
    ok "Found proposal surface: $surface"
  else
    fail "Missing proposal surface: $surface"
  fi
done

placeholder_rows="$(awk -F '\t' 'NR > 1 && ($4 ~ /(^|[.])example([.]|$)/ || $5 == "placeholder") {print $2 "\t" $4 "\t" $5}' "$proposal_file")"
if [ -n "$placeholder_rows" ]; then
  fail "Proposal still contains placeholder/example values"
  printf '%s\n' "$placeholder_rows" | sed 's/^/[INFO] placeholder /'
else
  ok "Proposal does not contain placeholder/example values"
fi

upstream_rows="$(awk -F '\t' 'NR > 1 && ($4 ~ /^chat[.]simplex/ || $4 ~ /^group[.]chat[.]simplex/ || $4 ~ /simplex[.]chat|simplex[.]im|simplexonflux[.]com/) && $2 != "url_scheme" {print $2 "\t" $4}' "$proposal_file")"
if [ -n "$upstream_rows" ]; then
  fail "Proposal still contains upstream release identifiers"
  printf '%s\n' "$upstream_rows" | sed 's/^/[INFO] upstream /'
else
  ok "Proposal avoids upstream-owned release identifiers outside the compatibility URL scheme"
fi

main_bundle="$(value_for_surface main_app_bundle_id)"
nse_bundle="$(value_for_surface notification_service_bundle_id)"
se_bundle="$(value_for_surface share_extension_bundle_id)"
framework_bundle="$(value_for_surface internal_framework_bundle_id)"
test_bundle="$(value_for_surface ios_test_bundle_id)"
app_group="$(value_for_surface app_group)"
keychain_group="$(value_for_surface keychain_access_group)"
background_task="$(value_for_surface background_task_id)"
url_type="$(value_for_surface url_type_name)"
url_scheme="$(value_for_surface url_scheme)"
domains="$(value_for_surface associated_domains)"

if is_reverse_dns_id "$main_bundle"; then
  ok "Main bundle id is reverse-DNS-like: $main_bundle"
else
  fail "Main bundle id is not reverse-DNS-like: ${main_bundle:-missing}"
fi

if [ "$nse_bundle" = "$main_bundle.notification-service" ]; then
  ok "Notification service bundle id is derived from main bundle id"
else
  fail "Notification service bundle id should be $main_bundle.notification-service, got ${nse_bundle:-missing}"
fi

if [ "$se_bundle" = "$main_bundle.share-extension" ]; then
  ok "Share extension bundle id is derived from main bundle id"
else
  fail "Share extension bundle id should be $main_bundle.share-extension, got ${se_bundle:-missing}"
fi

if [ "$framework_bundle" = "$main_bundle.chatcore" ]; then
  ok "Internal framework bundle id is derived from main bundle id"
else
  fail "Internal framework bundle id should be $main_bundle.chatcore, got ${framework_bundle:-missing}"
fi

if [ "$test_bundle" = "$main_bundle.tests-ios" ]; then
  ok "iOS test bundle id is derived from main bundle id"
else
  fail "iOS test bundle id should be $main_bundle.tests-ios, got ${test_bundle:-missing}"
fi

if [ "$app_group" = "group.$main_bundle" ]; then
  ok "App Group is derived from main bundle id"
else
  fail "App Group should be group.$main_bundle, got ${app_group:-missing}"
fi

if [ "$keychain_group" = "\$(AppIdentifierPrefix)$main_bundle" ]; then
  ok "Keychain group is derived from main bundle id"
else
  fail "Keychain group should be \$(AppIdentifierPrefix)$main_bundle, got ${keychain_group:-missing}"
fi

if [ "$background_task" = "$main_bundle.receive" ]; then
  ok "Background task id is derived from main bundle id"
else
  fail "Background task id should be $main_bundle.receive, got ${background_task:-missing}"
fi

if [ "$url_type" = "$main_bundle" ]; then
  ok "URL type name is derived from main bundle id"
else
  fail "URL type name should be $main_bundle, got ${url_type:-missing}"
fi

if [ "$url_scheme" = "simplex" ]; then
  warn "simplex URL scheme remains as protocol compatibility surface"
else
  fail "URL scheme changed from simplex; protocol link compatibility needs explicit product review"
fi

first_domain="$(printf '%s\n' "$domains" | awk -F ', ' '{gsub(/^applinks:/, "", $1); print $1}')"
second_domain="$(printf '%s\n' "$domains" | awk -F ', ' '{gsub(/^applinks:/, "", $2); print $2}')"
if is_domain_name "$first_domain" && [ "$second_domain" = "www.$first_domain" ]; then
  ok "Associated domains use a Nome candidate domain plus www: $domains"
else
  fail "Associated domains should be applinks:<domain>, applinks:www.<domain>; got ${domains:-missing}"
fi

if awk -F '\t' 'NR > 1 && $5 != "candidate_requires_apple_team_review" {bad=1} END {exit bad ? 0 : 1}' "$proposal_file"; then
  fail "Every non-placeholder proposal row should require Apple team review"
else
  ok "All proposal rows are marked candidate_requires_apple_team_review"
fi

cat <<'NEXT'

Remaining external checks:
1. Confirm Apple Developer App ID and App Group availability.
2. Confirm associated domain ownership and AASA hosting.
3. Decide keychain/app-group migration or reset behavior.
4. Re-run release identity export and strict release identifier gate after applying the approved migration.

This script is read-only. It does not edit Xcode, entitlements, or plists.
NEXT

exit "$status"
