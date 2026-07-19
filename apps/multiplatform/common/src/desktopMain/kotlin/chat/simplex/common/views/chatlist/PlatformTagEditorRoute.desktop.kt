package chat.simplex.common.views.chatlist

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformTagEditorRoute(
  title: String,
  submitLabel: String,
  errorText: String,
  showError: Boolean,
  submitEnabled: Boolean,
  onClose: () -> Unit,
  onSubmit: () -> Unit,
  inputContent: @Composable () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  legacyContent()
}

@Composable
internal actual fun PlatformTagListRoute(
  title: String,
  createLabel: String,
  reorderMode: Boolean,
  choices: List<NomeTagListChoice>,
  saving: Boolean,
  onClose: () -> Unit,
  onCreate: () -> Unit,
  onChoice: (Long) -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  legacyContent()
}
