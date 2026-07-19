package chat.simplex.common.views.usersettings.networkAndServers

import androidx.compose.runtime.Composable
import chat.simplex.common.model.UserNetworkInfo

@Composable
internal actual fun PlatformObservedNetworkInfo(
  legacyInfo: UserNetworkInfo,
): UserNetworkInfo? = legacyInfo
