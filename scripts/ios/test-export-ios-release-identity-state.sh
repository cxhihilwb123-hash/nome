#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/export-ios-release-identity-state.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-release-identity-state-test.XXXXXX")"
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

  mkdir -p "$dir/SimpleX NSE/en.lproj" "$dir/SimpleX SE/en.lproj"

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
- [ ] `scripts/ios/check-ios-release-identifiers.sh` passes for the final TestFlight or public distribution configuration.
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
}

make_review() {
  local file="$1"

  cat > "$file" <<'EOF'
Documented compatibility exceptions:
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
review="$work_dir/review.md"
output="$work_dir/state"
make_fixture "$fixture"
make_review "$review"

PROJECT_FILE="$fixture/project.pbxproj" \
INFO_PLIST="$fixture/Info.plist" \
MAIN_ENTITLEMENTS="$fixture/main.entitlements" \
NSE_ENTITLEMENTS="$fixture/nse.entitlements" \
SE_ENTITLEMENTS="$fixture/se.entitlements" \
REVIEW_FILE="$review" \
"$script" --checklist "$fixture/checklist.md" --output "$output" --force > "$work_dir/export.log" 2>&1

[ -f "$output/summary.tsv" ] || fail "missing summary.tsv"
[ -f "$output/gate_status.tsv" ] || fail "missing gate_status.tsv"
[ -f "$output/current_identifiers.tsv" ] || fail "missing current_identifiers.tsv"
[ -f "$output/proposed_identifiers.tsv" ] || fail "missing proposed_identifiers.tsv"
[ -f "$output/required_decisions.tsv" ] || fail "missing required_decisions.tsv"
[ -f "$output/next_actions.tsv" ] || fail "missing next_actions.tsv"

grep -Fq $'compatibility_reviewed_status\tPASS' "$output/summary.tsv" || fail "compatibility gate should pass"
grep -Fq $'strict_release_status\tBLOCKED' "$output/summary.tsv" || fail "strict gate should block"
grep -Fq $'release_bundle_base\tcom.example.nome' "$output/summary.tsv" || fail "summary should record default bundle base"
grep -Fq $'release_domain\tnome.example' "$output/summary.tsv" || fail "summary should record default release domain"
grep -Fq $'proposal_status\tplaceholder' "$output/summary.tsv" || fail "default proposal should be marked as placeholder"
grep -Fq $'proposed_identifier_rows\t11' "$output/summary.tsv" || fail "summary should count proposed identifier rows"
grep -Fq $'next_action_rows\t4' "$output/summary.tsv" || fail "summary should count next action rows"
grep -Fq $'main_app_bundle_id\tchat.simplex.app' "$output/current_identifiers.tsv" || fail "missing main app bundle id"
grep -Fq $'app_group\tgroup.chat.simplex.app' "$output/current_identifiers.tsv" || fail "missing app group"
grep -Fq $'keychain_access_group\t$(AppIdentifierPrefix)chat.simplex.app' "$output/current_identifiers.tsv" || fail "missing keychain group"
grep -Fq $'associated_domain\tapplinks:simplex.chat' "$output/current_identifiers.tsv" || fail "missing associated domain"
grep -Fq $'main_app_bundle_id\tchat.simplex.app\tcom.example.nome\tplaceholder' "$output/proposed_identifiers.tsv" || fail "missing default main app proposal"
grep -Fq $'app_group\tgroup.chat.simplex.app\tgroup.com.example.nome\tplaceholder' "$output/proposed_identifiers.tsv" || fail "missing default app group proposal"
grep -Fq $'keychain_access_group\t$(AppIdentifierPrefix)chat.simplex.app\t$(AppIdentifierPrefix)com.example.nome\tplaceholder' "$output/proposed_identifiers.tsv" || fail "missing default keychain proposal"
grep -Fq $'associated_domains\tsimplex.chat, www.simplex.chat, *.simplex.im, *.simplexonflux.com\tapplinks:nome.example, applinks:www.nome.example\tplaceholder' "$output/proposed_identifiers.tsv" || fail "missing default associated-domain proposal"
grep -Fq $'1\tmain_app_bundle_id' "$output/required_decisions.tsv" || fail "missing main bundle decision"
grep -Fq "Before TestFlight/public distribution" "$output/next_actions.tsv" || fail "missing TestFlight next action"
grep -Fq "apply-ios-release-identifiers.sh --proposal" "$output/next_actions.tsv" || fail "missing release identifier apply next action"
grep -Fq $'priority\tdecision_scope\taction\tcommand\tevidence\tmanual_qa_anchor\tmanual_qa_line\tmanual_qa_section\tblocker_group' "$output/next_actions.tsv" || fail "next actions should expose manual QA mapping columns"

awk -F '\t' '
  NR > 1 && $2 == "main_app_bundle_id" && $7 != "" && $8 == "Release gate" && $9 == "release_identifiers" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/required_decisions.tsv" || fail "missing main bundle manual QA mapping"

awk -F '\t' '
  NR > 1 && $2 == "url_scheme" && $7 != "" && $8 == "Release gate" && $9 == "release_identifier_review" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/required_decisions.tsv" || fail "missing URL scheme manual QA mapping"

awk -F '\t' '
  NR > 1 && $2 == "main_app_bundle_id" && $7 != "" && $8 == "Release gate" && $9 == "release_identifiers" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/next_actions.tsv" || fail "missing next action release identifier QA mapping"

awk -F '\t' '
  NR > 1 && $2 == "url_scheme" && $7 != "" && $8 == "Release gate" && $9 == "release_identifier_review" {
    found = 1
  }
  END {
    exit found ? 0 : 1
  }
' "$output/next_actions.tsv" || fail "missing next action compatibility QA mapping"

custom_output="$work_dir/custom-state"
PROJECT_FILE="$fixture/project.pbxproj" \
INFO_PLIST="$fixture/Info.plist" \
MAIN_ENTITLEMENTS="$fixture/main.entitlements" \
NSE_ENTITLEMENTS="$fixture/nse.entitlements" \
SE_ENTITLEMENTS="$fixture/se.entitlements" \
REVIEW_FILE="$review" \
"$script" \
  --checklist "$fixture/checklist.md" \
  --output "$custom_output" \
  --release-bundle-base "app.nome.secure" \
  --release-domain "nome.chat" \
  --force > "$work_dir/custom-export.log" 2>&1

grep -Fq $'release_bundle_base\tapp.nome.secure' "$custom_output/summary.tsv" || fail "custom summary should record bundle base"
grep -Fq $'release_domain\tnome.chat' "$custom_output/summary.tsv" || fail "custom summary should record release domain"
grep -Fq $'proposal_status\tcandidate_requires_apple_team_review' "$custom_output/summary.tsv" || fail "custom proposal should require Apple team review"
grep -Fq $'main_app_bundle_id\tchat.simplex.app\tapp.nome.secure\tcandidate_requires_apple_team_review' "$custom_output/proposed_identifiers.tsv" || fail "missing custom main app proposal"
grep -Fq $'associated_domains\tsimplex.chat, www.simplex.chat, *.simplex.im, *.simplexonflux.com\tapplinks:nome.chat, applinks:www.nome.chat\tcandidate_requires_apple_team_review' "$custom_output/proposed_identifiers.tsv" || fail "missing custom associated-domain proposal"

pass "exported release identity state fixture"
echo "[PASS] release identity state test logs: $work_dir"
