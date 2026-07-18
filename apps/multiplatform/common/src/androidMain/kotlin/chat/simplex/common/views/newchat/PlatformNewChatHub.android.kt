package chat.simplex.common.views.newchat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.PersonAddAlt
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.common.views.helpers.ProfileImage

@Composable
internal actual fun PlatformNewChatHub(
  currentProfileName: String,
  currentProfileImage: String?,
  onOpenProfile: (() -> Unit)?,
  onAddContact: () -> Unit,
  onScanOrPaste: () -> Unit,
  onCreateGroup: () -> Unit,
  onCreateChannel: () -> Unit,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val darkTheme = !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeNewChatHubContent(
      currentProfileName = currentProfileName,
      currentProfileImage = currentProfileImage,
      onOpenProfile = onOpenProfile,
      onAddContact = onAddContact,
      onScanOrPaste = onScanOrPaste,
      onCreateGroup = onCreateGroup,
      onCreateChannel = onCreateChannel,
      onClose = onClose,
    )
  }
}

@Composable
fun NomeNewChatHubContent(
  currentProfileName: String,
  currentProfileImage: String? = null,
  onOpenProfile: (() -> Unit)?,
  onAddContact: () -> Unit,
  onScanOrPaste: () -> Unit,
  onCreateGroup: () -> Unit,
  onCreateChannel: () -> Unit,
  onClose: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(NomeTheme.colors.background)
      .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    NomeNewChatTopBar(onClose)
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(
          start = NomeTheme.dimensions.screenHorizontalInset,
          end = NomeTheme.dimensions.screenHorizontalInset,
          bottom = NomeTheme.dimensions.space24,
        ),
    ) {
      Text(
        text = stringResource(R.string.nome_p10_intro),
        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
      NomeProfileCard(
        profileName = currentProfileName,
        profileImage = currentProfileImage,
        onClick = onOpenProfile,
      )
      Spacer(Modifier.height(4.dp))
      NomePrivacyStrip()
      Spacer(Modifier.height(18.dp))
      NomeNewChatAction(
        icon = Icons.Rounded.PersonAddAlt,
        iconColor = NomeTheme.colors.action,
        iconBackground = NomeTheme.colors.successContainer,
        title = stringResource(R.string.nome_p10_add_contact),
        body = stringResource(R.string.nome_p10_add_contact_body),
        onClick = onAddContact,
      )
      NomeNewChatAction(
        icon = Icons.Rounded.QrCodeScanner,
        iconColor = NomeTheme.colors.info,
        iconBackground = NomeTheme.colors.infoContainer,
        title = stringResource(R.string.nome_p10_scan_paste),
        body = stringResource(R.string.nome_p10_scan_paste_body),
        onClick = onScanOrPaste,
      )
      NomeNewChatAction(
        icon = Icons.Rounded.GroupAdd,
        iconColor = NomeTheme.colors.textPrimary,
        iconBackground = NomeTheme.colors.surfaceSubtle,
        title = stringResource(R.string.nome_p10_group),
        body = stringResource(R.string.nome_p10_group_body),
        onClick = onCreateGroup,
      )
      NomeNewChatAction(
        icon = Icons.Rounded.CellTower,
        iconColor = NomeTheme.colors.accent,
        iconBackground = NomeTheme.colors.surfaceSubtle,
        title = stringResource(R.string.nome_p10_channel),
        body = stringResource(R.string.nome_p10_channel_body),
        onClick = onCreateChannel,
        showDivider = false,
      )
    }
  }
}

@Composable
private fun NomeNewChatTopBar(onClose: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .padding(start = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(
      onClick = onClose,
      modifier = Modifier
        .size(NomeTheme.dimensions.minimumTouchTarget)
        .nomeTalkBackSemantics(
          label = stringResource(R.string.nome_p10_back),
          role = Role.Button,
        ),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = null,
        tint = NomeTheme.colors.textPrimary,
      )
    }
    Spacer(Modifier.width(12.dp))
    Text(
      text = stringResource(R.string.nome_p10_title),
      modifier = Modifier.semantics { heading() },
      style = NomeTheme.typography.title,
      color = NomeTheme.colors.textPrimary,
      fontWeight = FontWeight.SemiBold,
    )
  }
  Divider(color = NomeTheme.colors.divider)
}

@Composable
private fun NomeProfileCard(
  profileName: String,
  profileImage: String?,
  onClick: (() -> Unit)?,
) {
  val baseModifier = Modifier
    .fillMaxWidth()
    .heightIn(min = 56.dp)
  val interactionModifier =
    if (onClick == null) {
      baseModifier
    } else {
      baseModifier
        .clickable(onClick = onClick)
        .nomeTalkBackSemantics(
          label = stringResource(R.string.nome_p10_profile_action, profileName),
          role = Role.Button,
        )
    }
  NomeSurface(
    modifier = interactionModifier,
    border = BorderStroke(1.dp, NomeTheme.colors.border),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (profileImage != null) {
        ProfileImage(
          size = 38.dp,
          image = profileImage,
          color = NomeTheme.colors.action,
          backgroundColor = NomeTheme.colors.surfaceContainer,
        )
      } else {
        Surface(
          modifier = Modifier.size(38.dp),
          shape = CircleShape,
          color = NomeTheme.colors.surfaceContainer,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = profileName.firstOrNull()?.uppercase() ?: "N",
              style = NomeTheme.typography.bodyStrong,
              color = NomeTheme.colors.action,
            )
          }
        }
      }
      Spacer(Modifier.width(10.dp))
      Column(Modifier.weight(1f)) {
        Text(
          text = profileName,
          style = NomeTheme.typography.bodyStrong,
          color = NomeTheme.colors.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = stringResource(R.string.nome_p10_profile_body),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
        )
      }
      if (onClick != null) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
          contentDescription = null,
          tint = NomeTheme.colors.textTertiary,
        )
      }
    }
  }
}

@Composable
private fun NomePrivacyStrip() {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    color = NomeTheme.colors.successContainer,
    border = BorderStroke(1.dp, NomeTheme.colors.sentMessageBorder),
    shape = NomeTheme.shapes.control,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Rounded.Security,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = NomeTheme.colors.onSuccessContainer,
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = stringResource(R.string.nome_p10_privacy_note),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.onSuccessContainer,
      )
    }
  }
}

@Composable
private fun NomeNewChatAction(
  icon: ImageVector,
  iconColor: Color,
  iconBackground: Color,
  title: String,
  body: String,
  onClick: () -> Unit,
  showDivider: Boolean = true,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 64.dp)
      .clickable(onClick = onClick)
      .nomeMinimumTouchTarget()
      .nomeTalkBackSemantics(
        label = "$title. $body",
        role = Role.Button,
      )
      .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .background(iconBackground, NomeTheme.shapes.compact),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = iconColor,
      )
    }
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Text(
        text = title,
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text = body,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Icon(
      imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
      contentDescription = null,
      tint = NomeTheme.colors.textTertiary,
    )
  }
  if (showDivider) {
    Divider(
      modifier = Modifier.padding(start = 60.dp),
      color = NomeTheme.colors.divider,
    )
  }
}
