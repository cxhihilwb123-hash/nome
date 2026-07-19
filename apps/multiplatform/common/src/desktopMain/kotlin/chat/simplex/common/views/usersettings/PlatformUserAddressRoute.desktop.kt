package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable
import chat.simplex.common.model.UserContactLinkRec

@Composable
internal actual fun PlatformUserAddressRoute(
  userAddress: UserContactLinkRec?,
  loadState: NomeUserAddressLoadState,
  onReload: suspend () -> Unit,
  onCreate: suspend () -> Boolean,
  onSetRequiresConfirmation: suspend (Boolean) -> Boolean,
  onDelete: suspend () -> Boolean,
  onAddShortLink: () -> Unit,
  onShareAddress: (String) -> Unit,
  onOpenAdvanced: () -> Unit,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
) = legacyContent()
