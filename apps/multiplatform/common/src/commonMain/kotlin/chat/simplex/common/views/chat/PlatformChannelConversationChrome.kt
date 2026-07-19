package chat.simplex.common.views.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

internal val NomeChannelDisclosureHeight = 86.dp

/**
 * Android-only presentation for public-channel disclosure and observer state.
 *
 * The caller supplies only official group/channel facts. These functions create no relay,
 * encryption, membership, role, or send result and Desktop renders nothing.
 */
@Composable
internal expect fun PlatformChannelDisclosureBanner(
  visible: Boolean,
)

@Composable
internal expect fun PlatformChannelObserverBar(
  visible: Boolean,
)
