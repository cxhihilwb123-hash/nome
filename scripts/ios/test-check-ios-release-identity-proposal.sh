#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
export_script="$root_dir/scripts/ios/export-ios-release-identity-state.sh"
check_script="$root_dir/scripts/ios/check-ios-release-identity-proposal.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-release-proposal-test.XXXXXX")"
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
<plist>
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
<plist>
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

default_output="$work_dir/default-state"
PROJECT_FILE="$fixture/project.pbxproj" \
INFO_PLIST="$fixture/Info.plist" \
MAIN_ENTITLEMENTS="$fixture/main.entitlements" \
NSE_ENTITLEMENTS="$fixture/nse.entitlements" \
SE_ENTITLEMENTS="$fixture/se.entitlements" \
REVIEW_FILE="$fixture/review.md" \
"$export_script" \
  --checklist "$fixture/checklist.md" \
  --output "$default_output" \
  --force > "$work_dir/default-export.log" 2>&1

rc=0
"$check_script" --proposal "$default_output/proposed_identifiers.tsv" > "$work_dir/default-check.log" 2>&1 || rc=$?
[ "$rc" -ne 0 ] || fail "default placeholder proposal should fail"
grep -Fq "Proposal still contains placeholder/example values" "$work_dir/default-check.log" || fail "placeholder failure should be explicit"

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
  --force > "$work_dir/candidate-export.log" 2>&1

"$check_script" --proposal "$candidate_output/proposed_identifiers.tsv" > "$work_dir/candidate-check.log" 2>&1
grep -Fq "Main bundle id is reverse-DNS-like: app.nome.secure" "$work_dir/candidate-check.log" || fail "candidate should validate main bundle id"
grep -Fq "Associated domains use a Nome candidate domain plus www" "$work_dir/candidate-check.log" || fail "candidate should validate associated domains"
grep -Fq "simplex URL scheme remains as protocol compatibility surface" "$work_dir/candidate-check.log" || fail "candidate should preserve simplex warning"

broken_proposal="$work_dir/broken-proposed-identifiers.tsv"
awk -F '\t' 'BEGIN {OFS="\t"} NR == 1 {print; next} $2 == "share_extension_bundle_id" {$4 = "app.nome.secure.bad-share"} {print}' "$candidate_output/proposed_identifiers.tsv" > "$broken_proposal"

rc=0
"$check_script" --proposal "$broken_proposal" > "$work_dir/broken-check.log" 2>&1 || rc=$?
[ "$rc" -ne 0 ] || fail "inconsistent share extension proposal should fail"
grep -Fq "Share extension bundle id should be app.nome.secure.share-extension" "$work_dir/broken-check.log" || fail "broken proposal should report share extension mismatch"

pass "release identity proposal checker"
echo "[PASS] release proposal checker test logs: $work_dir"
