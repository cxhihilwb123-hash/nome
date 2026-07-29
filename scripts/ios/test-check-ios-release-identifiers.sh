#!/usr/bin/env bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
script="$root_dir/scripts/ios/check-ios-release-identifiers.sh"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/nome-check-ios-release-identifiers-test.XXXXXX")"
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

  printf 'CFBundleDisplayName = "Nome Notifications";\n' > "$dir/SimpleX NSE/en.lproj/InfoPlist.strings"
  printf 'CFBundleDisplayName = "Nome Share";\n' > "$dir/SimpleX SE/en.lproj/InfoPlist.strings"
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

run_script() {
  local log="$1"
  shift
  local fixture="$1"
  shift

  PROJECT_FILE="$fixture/project.pbxproj" \
    INFO_PLIST="$fixture/Info.plist" \
    MAIN_ENTITLEMENTS="$fixture/main.entitlements" \
    NSE_ENTITLEMENTS="$fixture/nse.entitlements" \
    SE_ENTITLEMENTS="$fixture/se.entitlements" \
    "$script" "$@" > "$log" 2>&1
}

expect_rc() {
  local name="$1"
  local expected_rc="$2"
  local expected_text="$3"
  shift 3
  local log="$work_dir/$name.log"
  local rc=0

  run_script "$log" "$@" || rc=$?
  if [ "$rc" -ne "$expected_rc" ]; then
    sed -n '1,220p' "$log" >&2
    fail "$name expected exit $expected_rc, got $rc"
  fi

  if ! grep -Fq "$expected_text" "$log"; then
    sed -n '1,220p' "$log" >&2
    fail "$name missing expected text: $expected_text"
  fi

  pass "$name"
}

fixture="$work_dir/fixture"
review="$work_dir/review.md"
make_fixture "$fixture"
make_review "$review"

expect_rc \
  "strict_final_blocks_upstream_ids" \
  1 \
  "Upstream bundle identifier remains: chat.simplex.app" \
  "$fixture"

expect_rc \
  "compatibility_review_passes_documented_exceptions" \
  0 \
  "documented compatibility exception: chat.simplex.app" \
  "$fixture" \
  --compatibility-reviewed \
  --review-file "$review"

incomplete_review="$work_dir/incomplete-review.md"
printf 'chat.simplex.app\n' > "$incomplete_review"
expect_rc \
  "compatibility_review_requires_each_exception" \
  1 \
  "missing documented compatibility exception" \
  "$fixture" \
  --compatibility-reviewed \
  --review-file "$incomplete_review"

bad_display_fixture="$work_dir/bad-display-fixture"
make_fixture "$bad_display_fixture"
perl -0pi -e 's/INFOPLIST_KEY_CFBundleDisplayName = Nome;/INFOPLIST_KEY_CFBundleDisplayName = SimpleX;/g' "$bad_display_fixture/project.pbxproj"
expect_rc \
  "compatibility_review_still_requires_nome_display" \
  1 \
  "Main app display name is not configured as Nome" \
  "$bad_display_fixture" \
  --compatibility-reviewed \
  --review-file "$review"

echo "[PASS] check-ios-release-identifiers tests passed"
