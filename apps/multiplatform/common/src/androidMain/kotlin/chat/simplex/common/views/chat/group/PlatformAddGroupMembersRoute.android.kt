package chat.simplex.common.views.chat.group

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.ui.nome.components.NomeFullPageScaffold
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

@Composable
internal actual fun PlatformAddGroupMembersRoute(
  title: String,
  hasContacts: Boolean,
  onClose: () -> Unit,
  profileContent: @Composable () -> Unit,
  setupContent: @Composable () -> Unit,
  selectionFooterContent: @Composable () -> Unit,
  contactsContent: @Composable () -> Unit,
  emptyContent: @Composable () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val darkTheme = !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeAddGroupMembersContent(
      title = title,
      hasContacts = hasContacts,
      onClose = onClose,
      profileContent = profileContent,
      setupContent = setupContent,
      selectionFooterContent = selectionFooterContent,
      contactsContent = contactsContent,
      emptyContent = emptyContent,
    )
  }
}

@Composable
fun NomeAddGroupMembersContent(
  title: String,
  hasContacts: Boolean,
  onClose: () -> Unit,
  profileContent: @Composable () -> Unit,
  setupContent: @Composable () -> Unit,
  selectionFooterContent: @Composable () -> Unit,
  contactsContent: @Composable () -> Unit,
  emptyContent: @Composable () -> Unit,
) {
  BackHandler(onBack = onClose)
  NomeFullPageScaffold(
    title = title,
    backLabel = androidx.compose.ui.res.stringResource(R.string.nome_back),
    onClose = onClose,
  ) {
    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border = BorderStroke(1.dp, NomeTheme.colors.divider),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
      ) {
        profileContent()
      }
    }

    if (hasContacts) {
      NomeSurface(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, NomeTheme.colors.divider),
      ) {
        Column(Modifier.fillMaxWidth()) {
          setupContent()
          Divider(color = NomeTheme.colors.divider)
          Column(
            Modifier.padding(
              horizontal = 14.dp,
              vertical = 8.dp,
            ),
          ) {
            selectionFooterContent()
          }
        }
      }
      Text(
        text = stringResource(MR.strings.select_contacts),
        modifier = Modifier.semantics { heading() },
        style = NomeTheme.typography.label,
        color = NomeTheme.colors.textSecondary,
      )
      NomeSurface(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, NomeTheme.colors.divider),
      ) {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
          contactsContent()
        }
      }
    } else {
      NomeSurface(
        modifier = Modifier.fillMaxWidth(),
        color = NomeTheme.colors.surfaceSubtle,
        border = BorderStroke(1.dp, NomeTheme.colors.divider),
      ) {
        Column(
          Modifier.padding(
            horizontal = 14.dp,
            vertical = 18.dp,
          ),
        ) {
          emptyContent()
        }
      }
    }
  }
}
