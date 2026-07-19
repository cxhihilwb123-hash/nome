package chat.simplex.common.views.chat.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
actual fun PlatformMessageActionsMenu(
  showMenu: MutableState<Boolean>,
  content: @Composable () -> Unit,
) {
  if (!showMenu.value) return

  Dialog(
    onDismissRequest = { showMenu.value = false },
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      Modifier
        .fillMaxSize()
        .background(NomeTheme.colors.scrim.copy(alpha = 0.38f))
        .clickable(
          role = Role.Button,
          onClick = { showMenu.value = false },
        ),
      contentAlignment = Alignment.BottomCenter,
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 700.dp)
          .clickable(enabled = false, onClick = {}),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = NomeTheme.colors.surfaceRaised,
        contentColor = NomeTheme.colors.textPrimary,
        elevation = 0.dp,
      ) {
        Column {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 24.dp, top = 14.dp, end = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(MR.strings.nome_message_actions),
              style = MaterialTheme.typography.h3,
              fontWeight = FontWeight.SemiBold,
              color = NomeTheme.colors.textPrimary,
            )
            IconButton(
              onClick = { showMenu.value = false },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                painter = painterResource(MR.images.ic_close),
                contentDescription = stringResource(MR.strings.icon_descr_close_button),
                tint = NomeTheme.colors.textPrimary,
              )
            }
          }
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 12.dp, vertical = 4.dp),
          ) {
            CompositionLocalProvider(LocalNomeMessageActionsSheet provides true) {
              content()
            }
          }
        }
      }
    }
  }
}
