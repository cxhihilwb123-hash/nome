package chat.simplex.common.views.usersettings.networkAndServers

import androidx.compose.runtime.Composable
import chat.simplex.common.model.UserNetworkInfo

/**
 * Returns authoritative platform connectivity when Android has observed it.
 *
 * Desktop retains the established model value. Android returns null before its first platform
 * observation so the legacy optimistic model default cannot become a visible connectivity fact.
 */
@Composable
internal expect fun PlatformObservedNetworkInfo(
  legacyInfo: UserNetworkInfo,
): UserNetworkInfo?
