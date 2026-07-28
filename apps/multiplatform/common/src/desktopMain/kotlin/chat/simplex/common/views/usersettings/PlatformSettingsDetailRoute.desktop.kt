package chat.simplex.common.views.usersettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
internal actual fun PlatformSettingsDetailRoute(
  title: String,
  onClose: (() -> Unit)?,
  groupedContent: Boolean,
  legacyContent: @Composable () -> Unit,
  content: @Composable () -> Unit,
) {
  NomeDesktopSettingsShell(
    title = title,
    onClose = onClose,
  ) { palette ->
    if (groupedContent) {
      NomeDesktopSettingsCard(palette = palette) {
        content()
      }
    } else {
      content()
    }
  }
}

@Composable
internal fun NomeDesktopSettingsShell(
  title: String,
  onClose: (() -> Unit)?,
  content: @Composable ColumnScope.(NomeDesktopSettingsPalette) -> Unit,
) {
  val palette = nomeDesktopSettingsPalette()
  val backLabel = stringResource(MR.strings.nome_desktop_back)

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(palette.background),
  ) {
    Column(
      modifier =
        Modifier
          .widthIn(max = 740.dp)
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 40.dp, vertical = 30.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (onClose != null) {
          Surface(
            color = palette.surface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, palette.border),
            elevation = 0.dp,
            modifier =
              Modifier
                .semantics(mergeDescendants = true) {
                  contentDescription = backLabel
                }
                .clickable(
                  role = Role.Button,
                  onClick = onClose,
                ),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                painter = painterResource(MR.images.ic_arrow_back_ios_new),
                contentDescription = null,
                tint = palette.textPrimary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = backLabel,
                color = palette.textPrimary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
        Spacer(Modifier.weight(1f))
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Image(
            painter = painterResource(MR.images.nome_mark),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
          )
          Text(
            text = stringResource(MR.strings.app_name),
            color = palette.textPrimary,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 7.dp),
          )
        }
      }

      Text(
        text = title,
        color = palette.textPrimary,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp,
        modifier =
          Modifier
            .padding(top = 28.dp)
            .semantics { heading() },
      )
      Divider(
        color = palette.divider,
        modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
      )
      content(palette)
      Spacer(Modifier.height(32.dp))
    }
  }
}

@Composable
internal fun NomeDesktopSettingsCard(
  palette: NomeDesktopSettingsPalette,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    color = palette.surface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, palette.border),
    elevation = 0.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
      content = content,
    )
  }
}

@Immutable
internal data class NomeDesktopSettingsPalette(
  val background: Color,
  val surface: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val border: Color,
  val divider: Color,
  val accent: Color,
  val accentContainer: Color,
)

@Composable
private fun nomeDesktopSettingsPalette(): NomeDesktopSettingsPalette =
  if (MaterialTheme.colors.isLight) {
    NomeDesktopSettingsPalette(
      background = Color(0xFFF5F7FA),
      surface = Color(0xFFFFFFFF),
      textPrimary = Color(0xFF0E1B2D),
      textSecondary = Color(0xFF607084),
      border = Color(0xFFDCE3E9),
      divider = Color(0xFFE7ECF1),
      accent = Color(0xFF0A874D),
      accentContainer = Color(0xFFE8F6F0),
    )
  } else {
    NomeDesktopSettingsPalette(
      background = Color(0xFF0F151C),
      surface = Color(0xFF17212B),
      textPrimary = Color(0xFFF2F6F4),
      textSecondary = Color(0xFFADB9C4),
      border = Color(0xFF31404C),
      divider = Color(0xFF283640),
      accent = Color(0xFF45D18B),
      accentContainer = Color(0xFF193B2C),
    )
  }
