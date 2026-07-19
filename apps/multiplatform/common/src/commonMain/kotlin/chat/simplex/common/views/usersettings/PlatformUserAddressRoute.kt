package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable
import chat.simplex.common.model.UserContactLinkRec

enum class NomeUserAddressLoadState {
  LOADING,
  READY,
  OFF,
  FAILURE,
}

/**
 * Android presentation seam for the official public-contact-address operations.
 *
 * The caller retains lookup, create, settings, delete, and model mutation ownership. Desktop
 * renders [legacyContent] unchanged.
 */
@Composable
internal expect fun PlatformUserAddressRoute(
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
)
