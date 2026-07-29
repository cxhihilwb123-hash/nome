#!/bin/bash

set -euo pipefail

root_dir="$(cd "$(dirname "$0")/../.." && pwd -P)"
coverage_file="$root_dir/plans/20260709_nome_ios_design_coverage_matrix.md"
smoke_script="$root_dir/scripts/ios/smoke-nome-ui.sh"
app_file="$root_dir/apps/ios/Shared/SimpleXApp.swift"
chat_list_file="$root_dir/apps/ios/Shared/Views/ChatList/ChatListView.swift"
new_chat_file="$root_dir/apps/ios/Shared/Views/NewChat/NewChatView.swift"
address_file="$root_dir/apps/ios/Shared/Views/UserSettings/UserAddressView.swift"
identity_file="$root_dir/apps/ios/Shared/Views/UserSettings/UserProfilesView.swift"
conversation_file="$root_dir/apps/ios/Shared/Views/Chat/ChatView.swift"
settings_file="$root_dir/apps/ios/Shared/Views/UserSettings/SettingsView.swift"
core_readiness_file="$root_dir/apps/ios/Shared/Model/SimpleXAPI.swift"
status=0

fail() {
  printf '[FAIL] %s\n' "$1"
  status=1
}

pass() {
  printf '[PASS] %s\n' "$1"
}

require_file() {
  local path="$1"

  if [ -f "$root_dir/$path" ]; then
    pass "Found $path"
  else
    fail "Missing $path"
  fi
}

require_text() {
  local file="$1"
  local text="$2"

  if grep -Fq -- "$text" "$file"; then
    pass "Found '$text' in ${file#$root_dir/}"
  else
    fail "Missing '$text' in ${file#$root_dir/}"
  fi
}

require_file "design/product/pages-v2/01-home-inbox.png"
require_file "design/product/pages-v2/02-add-friend-one-time.png"
require_file "design/product/pages-v2/03-join-group.png"
require_file "design/product/pages-v2/04-public-contact.png"
require_file "design/product/pages-v2/05-identity-center.png"
require_file "design/product/pages-v2/06-conversation.png"
require_file "design/product/pages-v2/07-settings-safety.png"
require_file "plans/20260709_nome_ios_design_coverage_matrix.md"
require_file "scripts/ios/smoke-nome-ui.sh"
require_file "apps/ios/Shared/SimpleXApp.swift"
require_file "apps/ios/Shared/Views/ChatList/ChatListView.swift"
require_file "apps/ios/Shared/Views/NewChat/NewChatView.swift"
require_file "apps/ios/Shared/Views/UserSettings/UserAddressView.swift"
require_file "apps/ios/Shared/Views/UserSettings/UserProfilesView.swift"
require_file "apps/ios/Shared/Views/Chat/ChatView.swift"
require_file "apps/ios/Shared/Views/UserSettings/SettingsView.swift"
require_file "apps/ios/Shared/Model/SimpleXAPI.swift"

for id in \
  COV-HOME \
  COV-ADD-FRIEND \
  COV-JOIN-GROUP \
  COV-PUBLIC-CONTACT \
  COV-IDENTITY \
  COV-CONVERSATION \
  COV-SETTINGS
do
  require_text "$coverage_file" "$id"
done

for label in \
  "01-home" \
  "02-onboarding-welcome" \
  "03-onboarding-profile" \
  "04-onboarding-network" \
  "05-onboarding-conditions" \
  "06-chat-list-existing" \
  "07-conversation-preview" \
  "08-conversation-details" \
  "09-identity-center" \
  "10-add-friend" \
  "11-join-group" \
  "12-public-contact" \
  "13-settings" \
  "14-contacts"
do
  require_text "$smoke_script" "$label"
done

require_text "$smoke_script" "-NomeIdentityCenterPreview"
require_text "$smoke_script" "-NomeAddFriendPreview"
require_text "$smoke_script" "-NomeJoinGroupPreview"
require_text "$smoke_script" "-NomePublicContactPreview"
require_text "$smoke_script" "-NomeSettingsPreview"
require_text "$smoke_script" "-NomeContactsPreview"
require_text "$smoke_script" "expected 14"

require_text "$app_file" "NomePrimaryFlowPreviewHost"
require_text "$app_file" "-NomeOnboardingWelcomePreview"
require_text "$app_file" "-NomeChatListPreview"
require_text "$app_file" "-NomeConversationPreview"
require_text "$app_file" "-NomeIdentityCenterPreview"
require_text "$app_file" "-NomeAddFriendPreview"
require_text "$app_file" "-NomeJoinGroupPreview"
require_text "$app_file" "-NomePublicContactPreview"
require_text "$app_file" "-NomeSettingsPreview"
require_text "$app_file" "-NomeContactsPreview"

require_text "$chat_list_file" "NomeHomeHeader"
require_text "$chat_list_file" "NomeHomeQuickActions"
require_text "$chat_list_file" "NomeHomeCompactActions"
require_text "$chat_list_file" "NomeHomeTabBar"
require_text "$chat_list_file" "NomeContactsHeader"

require_text "$new_chat_file" "NomeInviteUnavailableView"
require_text "$new_chat_file" "NomeInlineBrandMark"
require_text "$new_chat_file" "NomeFlowPageHeader"
require_text "$new_chat_file" "NomeOneTimeLinkHero"
require_text "$new_chat_file" "NomeJoinGroupHero"
require_text "$new_chat_file" "realChatCoreReadinessError()"

require_text "$address_file" "NomeAddressInlineBrandMark"
require_text "$address_file" "NomeAddressPageHeader"
require_text "$address_file" "NomePublicAddressCard"
require_text "$address_file" "NomeConfirmationCard"
require_text "$address_file" "NomeAddressManagementCard"
require_text "$address_file" "showRealCoreErrorIfNeeded()"

require_text "$identity_file" "NomeIdentityHero"
require_text "$identity_file" "NomeIdentityMetric"
require_text "$identity_file" "NomeIdentityCenterPreviewHost"

require_text "$conversation_file" "NomeConversationPreviewHost"
require_text "$conversation_file" "NomeConversationPreviewDetailsSheet"
require_text "$conversation_file" "-NomeConversationPreviewOpenDetails"

require_text "$settings_file" "embeddedInNomeTab"
require_text "$settings_file" "NomeSettingsLogoHeader"
require_text "$settings_file" "NomeSettingsTabSection"
require_text "$settings_file" "NomeBackupAndMigrationView"
require_text "$settings_file" "NomeHelpView"
require_text "$settings_file" "NomeAboutView"

require_text "$core_readiness_file" "preview-agent"
require_text "$core_readiness_file" "当前模拟器连接的是预览 core"

if [ "$status" -eq 0 ]; then
  pass "Nome design coverage check passed"
fi

exit "$status"
