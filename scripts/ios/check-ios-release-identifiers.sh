#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
project_file="${PROJECT_FILE:-$root_dir/apps/ios/SimpleX.xcodeproj/project.pbxproj}"
info_plist="${INFO_PLIST:-$root_dir/apps/ios/SimpleX--iOS--Info.plist}"
main_entitlements="${MAIN_ENTITLEMENTS:-$root_dir/apps/ios/SimpleX (iOS).entitlements}"
nse_entitlements="${NSE_ENTITLEMENTS:-$root_dir/apps/ios/SimpleX NSE/SimpleX NSE.entitlements}"
se_entitlements="${SE_ENTITLEMENTS:-$root_dir/apps/ios/SimpleX SE/SimpleX SE.entitlements}"
review_file="${REVIEW_FILE:-$root_dir/plans/20260709_nome_ios_release_gate_review.md}"
nse_dir="$root_dir/apps/ios/SimpleX NSE"
se_dir="$root_dir/apps/ios/SimpleX SE"
mode="final"
status=0

usage() {
  cat <<'USAGE'
Usage:
  scripts/ios/check-ios-release-identifiers.sh [options]

Read-only release identifier gate.

Modes:
  default                  Strict final TestFlight/public-distribution mode.
                           Upstream identifiers are BLOCKED.
  --compatibility-reviewed Development-pass mode. Upstream identifiers are
                           allowed only when they are documented in the release
                           gate review as explicit compatibility exceptions.

Options:
  --compatibility-reviewed  Use documented compatibility exceptions.
  --review-file FILE        Release gate review file.
  -h, --help                Show this help.
USAGE
}

ok() {
  printf '[OK] %s\n' "$1"
}

warn() {
  printf '[WARN] %s\n' "$1"
}

block() {
  printf '[BLOCKED] %s\n' "$1"
  status=1
}

fail() {
  printf '[FAIL] %s\n' "$1"
  status=1
}

info() {
  printf '[INFO] %s\n' "$1"
}

require_file() {
  local label="$1"
  local file="$2"

  if [ -f "$file" ]; then
    ok "Found $label: $file"
  else
    fail "Missing $label: $file"
  fi
}

contains() {
  local file="$1"
  local pattern="$2"

  grep -Fq "$pattern" "$file" 2>/dev/null
}

project_values_for() {
  local key="$1"

  awk -F ' = ' -v key="$key" '$1 ~ "^[[:space:]]*" key "$" {gsub(/[;"]/, "", $2); print $2}' "$project_file" | sort -u
}

review_contains() {
  local token="$1"

  [ -f "$review_file" ] && grep -Fq "$token" "$review_file"
}

compat_or_block() {
  local message="$1"
  local token="$2"

  if [ "$mode" = "compatibility-reviewed" ]; then
    if review_contains "$token"; then
      warn "$message; documented compatibility exception: $token"
    else
      block "$message; missing documented compatibility exception in $review_file: $token"
    fi
  else
    block "$message"
  fi
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --compatibility-reviewed)
      mode="compatibility-reviewed"
      ;;
    --review-file)
      shift
      if [ "$#" -eq 0 ]; then
        echo "[FAIL] --review-file requires a file" >&2
        exit 2
      fi
      review_file="$1"
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

cat <<'HEADER'
Nome iOS release identifier readiness
=====================================
HEADER

info "Mode: $mode"
info "Review file: $review_file"

require_file "Xcode project file" "$project_file"
require_file "iOS Info.plist" "$info_plist"
require_file "main app entitlements" "$main_entitlements"
require_file "notification service extension entitlements" "$nse_entitlements"
require_file "share extension entitlements" "$se_entitlements"
if [ "$mode" = "compatibility-reviewed" ]; then
  require_file "release gate review" "$review_file"
fi

if contains "$project_file" 'INFOPLIST_KEY_CFBundleDisplayName = Nome'; then
  ok "Main app display name is configured as Nome"
else
  block "Main app display name is not configured as Nome in project settings"
fi

if contains "$project_file" 'PRODUCT_NAME = Nome'; then
  ok "Main app product name is Nome"
else
  block "Main app product name is not Nome"
fi

bundle_ids="$(project_values_for PRODUCT_BUNDLE_IDENTIFIER)"
if [ -n "$bundle_ids" ]; then
  info "Bundle identifiers found:"
  printf '%s\n' "$bundle_ids" | sed 's/^/[INFO] bundle-id /'
else
  block "No PRODUCT_BUNDLE_IDENTIFIER values found"
fi

for upstream_id in \
  'chat.simplex.app' \
  'chat.simplex.app.SimpleX-NSE' \
  'chat.simplex.app.SimpleX-SE' \
  'chat.simplex.SimpleXChat' \
  'chat.simplex.Tests-iOS'
do
  if printf '%s\n' "$bundle_ids" | grep -Fxq "$upstream_id"; then
    compat_or_block "Upstream bundle identifier remains: $upstream_id" "$upstream_id"
  fi
done

if contains "$project_file" 'PRODUCT_BUNDLE_IDENTIFIER = chat.simplex.app;'; then
  compat_or_block "Main app bundle id is still chat.simplex.app; choose a Nome-owned id before final TestFlight/public distribution" "chat.simplex.app"
fi

if contains "$project_file" 'INFOPLIST_KEY_CFBundleDisplayName = "Nome Notifications"' &&
   contains "$project_file" 'INFOPLIST_KEY_CFBundleDisplayName = "Nome Share"'; then
  ok "Extension build display names are Nome-facing"
else
  block "Extension build display names are not fully Nome-facing"
fi

if contains "$project_file" 'INFOPLIST_KEY_CFBundleDisplayName = "SimpleX NSE"' ||
   contains "$project_file" 'INFOPLIST_KEY_CFBundleDisplayName = "SimpleX SE"'; then
  block "Extension display names still use SimpleX labels"
fi

localized_extension_labels="$(
  find "$nse_dir" "$se_dir" -maxdepth 2 -name InfoPlist.strings -exec grep -HnE 'SimpleX (NSE|SE)' {} + 2>/dev/null || true
)"
if [ -n "$localized_extension_labels" ]; then
  block "Localized extension InfoPlist labels still use SimpleX labels"
  printf '%s\n' "$localized_extension_labels" | sed 's/^/[INFO] localized-extension-label /'
else
  ok "Localized extension InfoPlist labels are Nome-facing"
fi

if contains "$info_plist" 'chat.simplex.app.receive'; then
  compat_or_block "BGTaskSchedulerPermittedIdentifiers still use chat.simplex.app.receive" "chat.simplex.app.receive"
fi

if contains "$info_plist" '<string>simplex</string>'; then
  ok "simplex URL scheme is preserved for protocol/link compatibility"
else
  block "simplex URL scheme is missing"
fi

if contains "$info_plist" '<string>chat.simplex.app</string>'; then
  compat_or_block "CFBundleURLName still uses chat.simplex.app" "chat.simplex.app"
fi

for file in "$main_entitlements" "$nse_entitlements" "$se_entitlements"; do
  if contains "$file" 'group.chat.simplex.app'; then
    compat_or_block "$(basename "$file") still uses upstream App Group group.chat.simplex.app" "group.chat.simplex.app"
  fi

  if contains "$file" '$(AppIdentifierPrefix)chat.simplex.app'; then
    compat_or_block "$(basename "$file") still uses upstream keychain access group chat.simplex.app" '$(AppIdentifierPrefix)chat.simplex.app'
  fi
done

for domain in \
  'applinks:simplex.chat' \
  'applinks:www.simplex.chat' \
  'applinks:*.simplex.im' \
  'applinks:*.simplexonflux.com'
do
  if contains "$main_entitlements" "$domain"; then
    compat_or_block "Associated domain remains upstream-controlled: $domain" "$domain"
  fi
done

if contains "$main_entitlements" 'aps-environment'; then
  ok "Push notification entitlement exists"
else
  block "Push notification entitlement is missing"
fi

cat <<'NEXT'

Next release-identifier decisions:
1. Choose final Nome bundle identifiers for the app, tests, notification service, share extension, and internal framework.
2. Choose final App Group and keychain access group names.
3. Decide whether to keep, remove, or replace upstream associated domains.
4. Keep the simplex URL scheme only if protocol compatibility remains required.
5. Re-run this script before TestFlight or any public distribution.

Use --compatibility-reviewed only for the current compatibility-first
development pass. The default mode remains the final release gate.

This script is read-only. It does not edit the Xcode project, entitlements, or plists.
NEXT

exit "$status"
