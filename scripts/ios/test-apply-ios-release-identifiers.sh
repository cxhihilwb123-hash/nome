#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
export_script="$root_dir/scripts/ios/export-ios-release-identity-state.sh"
apply_script="$root_dir/scripts/ios/apply-ios-release-identifiers.sh"
check_script="$root_dir/scripts/ios/check-ios-release-identifiers.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-release-identity-apply-test.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

fail() {
  echo "[FAIL] $1" >&2
  exit 1
}

pass() {
  echo "[PASS] $1"
}

make_fixture() {
  local dir="$1"

  mkdir -p "$dir"

  cat > "$dir/project.pbxproj" <<'EOF'
        INFOPLIST_KEY_CFBundleDisplayName = Nome;
        PRODUCT_NAME = Nome;
        PRODUCT_BUNDLE_IDENTIFIER = chat.simplex.app;
        PRODUCT_BUNDLE_IDENTIFIER = chat.simplex.app.SimpleX-NSE;
        PRODUCT_BUNDLE_IDENTIFIER = chat.simplex.app.SimpleX-SE;
        PRODUCT_BUNDLE_IDENTIFIER = chat.simplex.SimpleXChat;
        PRODUCT_BUNDLE_IDENTIFIER = chat.simplex.Tests-iOS;
        INFOPLIST_KEY_CFBundleDisplayName = "Nome Notifications";
        INFOPLIST_KEY_CFBundleDisplayName = "Nome Share";
EOF

  cat > "$dir/Info.plist" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>BGTaskSchedulerPermittedIdentifiers</key>
  <array><string>chat.simplex.app.receive</string></array>
  <key>CFBundleURLTypes</key>
  <array>
    <dict>
      <key>CFBundleURLName</key>
      <string>chat.simplex.app</string>
      <key>CFBundleURLSchemes</key>
      <array><string>simplex</string></array>
    </dict>
  </array>
</dict>
</plist>
EOF

  cat > "$dir/checklist.md" <<'EOF'
## Release gate

- [x] Bundle id, URL schemes, app groups, notification extension, and share extension identifiers are reviewed before TestFlight.
- [ ] Nome-owned bundle identifiers are selected for the app, tests, notification service extension, share extension, and internal framework, or compatibility exceptions are explicitly approved for release.
- [ ] Nome-owned App Group and keychain access group names are selected with an explicit data-migration decision.
- [ ] Associated domains and background task identifiers are either Nome-owned or documented compatibility exceptions.
EOF

  for entitlements in main.entitlements nse.entitlements se.entitlements; do
    cat > "$dir/$entitlements" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>aps-environment</key>
  <string>development</string>
  <key>com.apple.security.application-groups</key>
  <array><string>group.chat.simplex.app</string></array>
  <key>keychain-access-groups</key>
  <array><string>$(AppIdentifierPrefix)chat.simplex.app</string></array>
  <key>com.apple.developer.associated-domains</key>
  <array>
    <string>applinks:simplex.chat</string>
    <string>applinks:www.simplex.chat</string>
    <string>applinks:*.simplex.im</string>
    <string>applinks:*.simplexonflux.com</string>
  </array>
</dict>
</plist>
EOF
  done

  cat > "$dir/review.md" <<'EOF'
chat.simplex.app
chat.simplex.app.SimpleX-NSE
chat.simplex.app.SimpleX-SE
chat.simplex.SimpleXChat
chat.simplex.Tests-iOS
simplex
chat.simplex.app.receive
group.chat.simplex.app
$(AppIdentifierPrefix)chat.simplex.app
applinks:simplex.chat
applinks:www.simplex.chat
applinks:*.simplex.im
applinks:*.simplexonflux.com
EOF
}

fixture="$work_dir/fixture"
make_fixture "$fixture"

candidate_output="$work_dir/candidate-state"
PROJECT_FILE="$fixture/project.pbxproj" \
INFO_PLIST="$fixture/Info.plist" \
MAIN_ENTITLEMENTS="$fixture/main.entitlements" \
NSE_ENTITLEMENTS="$fixture/nse.entitlements" \
SE_ENTITLEMENTS="$fixture/se.entitlements" \
REVIEW_FILE="$fixture/review.md" \
"$export_script" \
  --checklist "$fixture/checklist.md" \
  --release-bundle-base "app.nome.secure" \
  --release-domain "nome.chat" \
  --output "$candidate_output" \
  --force > "$work_dir/export.log" 2>&1

before_hash="$(shasum -a 256 "$fixture/project.pbxproj" "$fixture/Info.plist" "$fixture/main.entitlements" "$fixture/nse.entitlements" "$fixture/se.entitlements")"
dry_run_output="$work_dir/dry-run"
PROJECT_FILE="$fixture/project.pbxproj" \
INFO_PLIST="$fixture/Info.plist" \
MAIN_ENTITLEMENTS="$fixture/main.entitlements" \
NSE_ENTITLEMENTS="$fixture/nse.entitlements" \
SE_ENTITLEMENTS="$fixture/se.entitlements" \
"$apply_script" \
  --proposal "$candidate_output/proposed_identifiers.tsv" \
  --output "$dry_run_output" \
  --force > "$work_dir/dry-run.log" 2>&1
after_hash="$(shasum -a 256 "$fixture/project.pbxproj" "$fixture/Info.plist" "$fixture/main.entitlements" "$fixture/nse.entitlements" "$fixture/se.entitlements")"

[ "$before_hash" = "$after_hash" ] || fail "dry-run should not edit fixture files"
grep -Fq $'mode\tdry-run' "$dry_run_output/summary.tsv" || fail "dry-run summary should record dry-run mode"
grep -Fq $'post_apply_strict_gate\tSKIPPED' "$dry_run_output/summary.tsv" || fail "dry-run should skip post-apply strict gate"

apply_output="$work_dir/apply"
PROJECT_FILE="$fixture/project.pbxproj" \
INFO_PLIST="$fixture/Info.plist" \
MAIN_ENTITLEMENTS="$fixture/main.entitlements" \
NSE_ENTITLEMENTS="$fixture/nse.entitlements" \
SE_ENTITLEMENTS="$fixture/se.entitlements" \
"$apply_script" \
  --proposal "$candidate_output/proposed_identifiers.tsv" \
  --output "$apply_output" \
  --apply \
  --force > "$work_dir/apply.log" 2>&1

grep -Fq $'mode\tapply' "$apply_output/summary.tsv" || fail "apply summary should record apply mode"
grep -Fq $'post_apply_strict_gate\tPASS' "$apply_output/summary.tsv" || fail "post-apply strict gate should pass"
grep -Fq 'PRODUCT_BUNDLE_IDENTIFIER = app.nome.secure;' "$fixture/project.pbxproj" || fail "main bundle id not applied"
grep -Fq 'PRODUCT_BUNDLE_IDENTIFIER = app.nome.secure.notification-service;' "$fixture/project.pbxproj" || fail "notification bundle id not applied"
grep -Fq 'PRODUCT_BUNDLE_IDENTIFIER = app.nome.secure.share-extension;' "$fixture/project.pbxproj" || fail "share bundle id not applied"
grep -Fq '<string>app.nome.secure.receive</string>' "$fixture/Info.plist" || fail "background task id not applied"
grep -Fq '<string>app.nome.secure</string>' "$fixture/Info.plist" || fail "URL type name not applied"
grep -Fq '<string>group.app.nome.secure</string>' "$fixture/main.entitlements" || fail "app group not applied"
grep -Fq '<string>$(AppIdentifierPrefix)app.nome.secure</string>' "$fixture/main.entitlements" || fail "keychain group not applied"
grep -Fq '<string>applinks:nome.chat</string>' "$fixture/main.entitlements" || fail "primary associated domain not applied"
grep -Fq '<string>applinks:www.nome.chat</string>' "$fixture/main.entitlements" || fail "www associated domain not applied"

PROJECT_FILE="$fixture/project.pbxproj" \
INFO_PLIST="$fixture/Info.plist" \
MAIN_ENTITLEMENTS="$fixture/main.entitlements" \
NSE_ENTITLEMENTS="$fixture/nse.entitlements" \
SE_ENTITLEMENTS="$fixture/se.entitlements" \
"$check_script" > "$work_dir/final-check.log" 2>&1

pass "release identifier migration dry-run and apply"
echo "[PASS] release identifier migration test logs: $work_dir"
