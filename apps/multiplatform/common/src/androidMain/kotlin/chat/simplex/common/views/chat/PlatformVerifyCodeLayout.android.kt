package chat.simplex.common.views.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.R
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.components.NomeFullPageScaffold
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.common.views.newchat.QRCode

@Composable
internal actual fun PlatformVerifyCodeLayout(
  displayName: String,
  profileImage: String?,
  connectionCode: String,
  connectionVerified: Boolean,
  onScanCode: () -> Unit,
  onMarkVerified: () -> Unit,
  onClearVerification: () -> Unit,
  onShareCode: () -> Unit,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val darkTheme =
    !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeContactVerificationContent(
      displayName = displayName,
      profileImage = profileImage,
      connectionCode = connectionCode,
      connectionVerified = connectionVerified,
      onScanCode = onScanCode,
      onMarkVerified = onMarkVerified,
      onClearVerification = onClearVerification,
      onShareCode = onShareCode,
      onClose = onClose,
    )
  }
}

@Composable
fun NomeContactVerificationContent(
  displayName: String,
  profileImage: String?,
  connectionCode: String,
  connectionVerified: Boolean,
  onScanCode: () -> Unit,
  onMarkVerified: () -> Unit,
  onClearVerification: () -> Unit,
  onShareCode: () -> Unit,
  onClose: () -> Unit,
) {
  val backLabel =
    androidx.compose.ui.res.stringResource(
      R.string.nome_p19_back,
    )
  BackHandler(onBack = onClose)
  NomeFullPageScaffold(
    title =
      androidx.compose.ui.res.stringResource(
        R.string.nome_p19_title,
      ),
    backLabel = backLabel,
    onClose = onClose,
  ) {
    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border =
        BorderStroke(
          1.dp,
          NomeTheme.colors.border,
        ),
    ) {
      Row(
        modifier =
          Modifier.padding(
            horizontal = 12.dp,
            vertical = 10.dp,
          ),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ProfileImage(
          size = 58.dp,
          image = profileImage,
          color = NomeTheme.colors.textTertiary,
          backgroundColor =
            NomeTheme.colors.surfaceSubtle,
        )
        Spacer(Modifier.width(12.dp))
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement =
            Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = displayName,
            style = NomeTheme.typography.title,
            color = NomeTheme.colors.textPrimary,
            maxLines = 1,
          )
          Text(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p19_secure_session,
              ),
            style = NomeTheme.typography.supporting,
            color = NomeTheme.colors.textSecondary,
          )
        }
      }
    }

    NomeVerificationStatusStrip(
      connectionVerified = connectionVerified,
    )

    Text(
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p19_compare_intro,
          displayName,
        ),
      style = NomeTheme.typography.supporting,
      color = NomeTheme.colors.textSecondary,
    )

    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border =
        BorderStroke(
          1.dp,
          NomeTheme.colors.border,
        ),
    ) {
      Column(
          modifier =
            Modifier.padding(
            horizontal = 12.dp,
            vertical = 8.dp,
          ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement =
          Arrangement.spacedBy(5.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Spacer(
            Modifier.size(
              NomeTheme.dimensions.minimumTouchTarget,
            ),
          )
          Text(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p19_shared_code,
              ),
            modifier =
              Modifier
                .weight(1f)
                .semantics { heading() },
            style = NomeTheme.typography.bodyStrong,
            color = NomeTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
          )
          IconButton(
            onClick = onShareCode,
            modifier =
              Modifier
                .size(
                  NomeTheme.dimensions.minimumTouchTarget,
                )
                .nomeTalkBackSemantics(
                  label =
                    androidx.compose.ui.res.stringResource(
                      R.string.nome_p19_share_code,
                    ),
                  role = Role.Button,
                ),
          ) {
            Icon(
              imageVector = Icons.Rounded.Share,
              contentDescription = null,
              tint = NomeTheme.colors.action,
            )
          }
        }
        NomeSurface(
          modifier = Modifier.fillMaxWidth(),
          shape = NomeTheme.shapes.control,
          border =
            BorderStroke(
              1.dp,
              NomeTheme.colors.divider,
            ),
        ) {
          QRCode(
            connReq = connectionCode,
            small = true,
            padding =
              PaddingValues(
                horizontal = 8.dp,
                vertical = 4.dp,
              ),
            tintColor =
              if (NomeTheme.colors.isDark) {
                NomeTheme.colors.textPrimary
              } else {
                Color.Black
              },
            withLogo = false,
            imageSize = 146.dp,
          )
        }
        Text(
          text =
            formatNomeSecurityCode(
              connectionCode,
            ),
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = 2.dp),
          style =
            NomeTheme.typography.code.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              lineHeight = 14.sp,
            ),
          color = NomeTheme.colors.textPrimary,
          textAlign = TextAlign.Center,
        )
        Text(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p19_scan_hint,
            ),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
        )
      }
    }

    Text(
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p19_steps,
        ),
      modifier = Modifier.semantics { heading() },
      style = NomeTheme.typography.label,
      color = NomeTheme.colors.textSecondary,
    )

    NomeVerificationSteps(
      onMarkVerified =
        if (connectionVerified) {
          null
        } else {
          onMarkVerified
        },
    )

    if (connectionVerified) {
      NomeButton(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p19_clear_verification,
          ),
        onClick = onClearVerification,
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        variant =
          NomeButtonVariant.DESTRUCTIVE_SECONDARY,
        semanticsLabel =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p19_clear_verification,
          ),
      )
    } else {
      NomeButton(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p19_scan_action,
          ),
        onClick = onScanCode,
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = NomeTheme.shapes.pill,
        leadingIcon = {
          Icon(
            imageVector = Icons.Rounded.QrCodeScanner,
            contentDescription = null,
            modifier =
              Modifier.size(
                NomeTheme.dimensions.icon,
              ),
          )
        },
      )
    }
  }
}

@Composable
private fun NomeVerificationStatusStrip(
  connectionVerified: Boolean,
) {
  val colors = NomeTheme.colors
  val background =
    if (connectionVerified) {
      colors.successContainer
    } else {
      colors.warningContainer
    }
  val foreground =
    if (connectionVerified) {
      colors.onSuccessContainer
    } else {
      colors.onWarningContainer
    }
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    shape = NomeTheme.shapes.control,
    color = background,
    contentColor = foreground,
    border =
      BorderStroke(
        1.dp,
        foreground.copy(alpha = 0.18f),
      ),
  ) {
    Row(
      modifier =
        Modifier.padding(
          horizontal = 12.dp,
          vertical = 7.dp,
        ),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement =
        Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector =
          if (connectionVerified) {
            Icons.Rounded.Check
          } else {
            Icons.Rounded.Security
          },
        contentDescription = null,
        modifier =
          Modifier.size(
            NomeTheme.dimensions.iconSmall,
          ),
      )
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            if (connectionVerified) {
              R.string.nome_p19_verified
            } else {
              R.string.nome_p19_unverified
            },
          ),
        style = NomeTheme.typography.supporting,
      )
    }
  }
}

@Composable
private fun NomeVerificationSteps(
  onMarkVerified: (() -> Unit)?,
) {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    shape = NomeTheme.shapes.control,
    color = NomeTheme.colors.surfaceSubtle,
  ) {
    Column(
      modifier =
        Modifier.padding(
          horizontal = 12.dp,
          vertical = 8.dp,
        ),
      verticalArrangement =
        Arrangement.spacedBy(8.dp),
    ) {
      NomeVerificationStep(
        marker = {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = NomeTheme.colors.onAction,
            modifier =
              Modifier
                .size(28.dp)
                .background(
                  NomeTheme.colors.action,
                  CircleShape,
                )
                .padding(5.dp),
          )
        },
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p19_step_one_title,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p19_step_one_body,
          ),
      )
      NomeVerificationStep(
        modifier =
          if (onMarkVerified == null) {
            Modifier
          } else {
            Modifier
              .heightIn(min = 48.dp)
              .clickable(
                role = Role.Button,
                onClick = onMarkVerified,
              )
              .nomeTalkBackSemantics(
                label =
                  androidx.compose.ui.res.stringResource(
                    R.string.nome_p19_mark_verified,
                  ),
                role = Role.Button,
              )
          },
        marker = {
          Box(
            modifier =
              Modifier
                .size(28.dp)
                .background(
                  NomeTheme.colors.accent,
                  CircleShape,
                ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "2",
              style = NomeTheme.typography.label,
              color = NomeTheme.colors.onAccent,
            )
          }
        },
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p19_step_two_title,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p19_step_two_body,
          ),
      )
    }
  }
}

@Composable
private fun NomeVerificationStep(
  modifier: Modifier = Modifier,
  marker: @Composable () -> Unit,
  title: String,
  body: String,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement =
      Arrangement.spacedBy(12.dp),
  ) {
    marker()
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement =
        Arrangement.spacedBy(1.dp),
    ) {
      Text(
        text = title,
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text = body,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
    }
  }
}
