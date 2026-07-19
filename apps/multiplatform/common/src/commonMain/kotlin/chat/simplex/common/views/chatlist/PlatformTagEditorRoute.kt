package chat.simplex.common.views.chatlist

import androidx.compose.runtime.Composable

data class NomeTagListChoice(
  val id: Long,
  val emoji: String?,
  val name: String,
  val selected: Boolean,
)

/**
 * Android presentation seam for the official chat-list tag editor.
 *
 * The common owner retains tag validation and the create/update/set-tag API sequence. Android only
 * changes the full-page composition around the official input and submit callback.
 */
@Composable
internal expect fun PlatformTagEditorRoute(
  title: String,
  submitLabel: String,
  errorText: String,
  showError: Boolean,
  submitEnabled: Boolean,
  onClose: () -> Unit,
  onSubmit: () -> Unit,
  inputContent: @Composable () -> Unit,
  legacyContent: @Composable () -> Unit,
)

/**
 * Android presentation seam for choosing an existing official chat tag.
 *
 * The common owner retains the live tag list, selection state, create/edit routes, and official
 * set-tags callback. Android only changes the full-page list composition.
 */
@Composable
internal expect fun PlatformTagListRoute(
  title: String,
  createLabel: String,
  reorderMode: Boolean,
  choices: List<NomeTagListChoice>,
  saving: Boolean,
  onClose: () -> Unit,
  onCreate: () -> Unit,
  onChoice: (Long) -> Unit,
  legacyContent: @Composable () -> Unit,
)
