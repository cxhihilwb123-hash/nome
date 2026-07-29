package chat.simplex.common.views.newchat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.model.CreatedConnLink
import chat.simplex.common.platform.shareText
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.common.views.helpers.ModalManager

private enum class NomeInvitationLocalAction {
  NONE,
  COPIED,
  SHARED,
}

private enum class NomeScanPasteMode {
  SCAN,
  PASTE,
}

@Composable
internal actual fun PlatformNewChatRoute(
  selection: MutableState<NewChatOption>,
  invitation: CreatedConnLink,
  invitationCreating: Boolean,
  currentProfileName: String,
  onOpenProfile: (() -> Unit)?,
  pastedLink: MutableState<String>,
  showQRCodeScanner: MutableState<Boolean>,
  onRetryInvitation: () -> Unit,
  onInvitationLocalAction: () -> Unit,
  onSubmitPastedLink: (String) -> Unit,
  onScannedLink: suspend (String) -> Boolean,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val darkTheme =
    !CurrentColors.collectAsState().value.colors.isLight
  val clipboard = LocalClipboardManager.current
  val pagerState =
    rememberPagerState(
      initialPage = selection.value.ordinal,
      initialPageOffsetFraction = 0f,
    ) {
      NewChatOption.values().size
    }

  LaunchedEffect(pagerState.currentPage) {
    selection.value =
      NewChatOption.values()[pagerState.currentPage]
  }
  LaunchedEffect(selection.value) {
    val page = selection.value.ordinal
    if (pagerState.currentPage != page) {
      pagerState.scrollToPage(page)
    }
  }
  LaunchedEffect(Unit) {
    // P12 asks for camera access only after the explicit production action.
    showQRCodeScanner.value = false
  }

  NomeAndroidTheme(darkTheme = darkTheme) {
    HorizontalPager(
      state = pagerState,
      modifier =
        Modifier
          .fillMaxSize()
          .background(NomeTheme.colors.background),
      verticalAlignment = Alignment.Top,
      userScrollEnabled = true,
    ) { page ->
      when (NewChatOption.values()[page]) {
        NewChatOption.INVITE ->
          NomeOneTimeInvitationContent(
            invitation = invitation,
            invitationCreating = invitationCreating,
            currentProfileName = currentProfileName,
            onOpenProfile = onOpenProfile,
            onCopyLink = { link ->
              clipboard.setText(AnnotatedString(link))
              onInvitationLocalAction()
            },
            onShareLink = { link ->
              clipboard.shareText(link)
              onInvitationLocalAction()
            },
            onShareQr = { link ->
              clipboard.shareText(link)
              onInvitationLocalAction()
            },
            onQrImageShared = onInvitationLocalAction,
            onRetryInvitation = onRetryInvitation,
            onClose = onClose,
          )
        NewChatOption.CONNECT ->
          NomeScanPasteContent(
            pastedLink = pastedLink.value,
            onPastedLinkChange = {
              pastedLink.value = it
            },
            onSubmitPastedLink = {
              onSubmitPastedLink(pastedLink.value)
            },
            onReadClipboard = {
              clipboard.getText()?.text?.let { text ->
                pastedLink.value = text
                onSubmitPastedLink(text)
              }
            },
            scannerContent = {
              QRCodeScanner(
                showQRCodeScanner = showQRCodeScanner,
                padding = PaddingValues(0.dp),
                onBarcode = onScannedLink,
              )
            },
            onOpenScanner = {
              showQRCodeScanner.value = true
            },
            onClose = onClose,
          )
      }
    }
  }
}

@Composable
fun NomeOneTimeInvitationContent(
  invitation: CreatedConnLink,
  invitationCreating: Boolean,
  currentProfileName: String,
  onOpenProfile: (() -> Unit)?,
  onCopyLink: (String) -> Unit,
  onShareLink: (String) -> Unit,
  onShareQr: (String) -> Unit,
  onQrImageShared: () -> Unit,
  onRetryInvitation: () -> Unit,
  onClose: () -> Unit,
) {
  var localAction by rememberSaveable {
    mutableStateOf(NomeInvitationLocalAction.NONE)
  }
  val ready = invitation.connFullLink.isNotEmpty()
  val link =
    if (ready) {
      invitation.simplexChatUri(short = false)
    } else {
      ""
    }
  val linkLabel =
    stringResource(R.string.nome_p11_link_label)

  NomePageScaffold(
    title = stringResource(R.string.nome_p11_title),
    backLabel = stringResource(R.string.nome_p11_back),
    onClose = onClose,
  ) {
    if (!ready) {
      NomeInvitationPendingCard(
        creating = invitationCreating,
        onRetry = onRetryInvitation,
      )
      return@NomePageScaffold
    }

    val invitationHeaderModifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .let { modifier ->
          if (onOpenProfile == null) {
            modifier
          } else {
            modifier
              .clickable(onClick = onOpenProfile)
              .nomeTalkBackSemantics(
                label =
                  stringResource(
                    R.string.nome_p11_profile_action,
                    currentProfileName,
                  ),
                role = Role.Button,
              )
          }
        }

    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border =
        BorderStroke(
          1.dp,
          NomeTheme.colors.border,
        ),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
      ) {
        Row(
          modifier = invitationHeaderModifier,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          NomeIconTile(
            icon = Icons.Rounded.Link,
            background = NomeTheme.colors.successContainer,
            tint = NomeTheme.colors.action,
          )
          Spacer(Modifier.width(12.dp))
          Column(Modifier.weight(1f)) {
            Text(
              text =
                stringResource(
                  R.string.nome_p11_single_use,
                ),
              style = NomeTheme.typography.bodyStrong,
              color = NomeTheme.colors.textPrimary,
            )
            Text(
              text =
                stringResource(
                  R.string.nome_p11_single_use_body,
                  currentProfileName,
                ),
              style = NomeTheme.typography.supporting,
              color = NomeTheme.colors.textSecondary,
            )
          }
        }
        Spacer(Modifier.height(10.dp))
        Text(
          text = stringResource(R.string.nome_p11_link_label),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(5.dp))
        NomeSurface(
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 52.dp),
          shape = NomeTheme.shapes.compact,
          color = NomeTheme.colors.surfaceSubtle,
        ) {
          Row(
            modifier =
              Modifier.padding(
                start = 12.dp,
                end = 6.dp,
              ),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = link,
              modifier =
                Modifier
                  .weight(1f)
                  .clearAndSetSemantics {
                    contentDescription = linkLabel
                  },
              style =
                NomeTheme.typography.supporting.copy(
                  fontFamily = FontFamily.Monospace,
                ),
              color = NomeTheme.colors.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            IconButton(
              onClick = {
                onCopyLink(link)
                localAction =
                  NomeInvitationLocalAction.COPIED
              },
              modifier =
                Modifier
                  .size(
                    NomeTheme.dimensions.minimumTouchTarget,
                  )
                  .nomeTalkBackSemantics(
                    label =
                      stringResource(
                        R.string.nome_p11_copy,
                      ),
                    role = Role.Button,
                  ),
            ) {
              Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null,
                tint = NomeTheme.colors.textPrimary,
              )
            }
          }
        }
        Spacer(Modifier.height(10.dp))
        Row(
          horizontalArrangement =
            Arrangement.spacedBy(8.dp),
        ) {
          NomeButton(
            text = stringResource(R.string.nome_p11_copy),
            onClick = {
              onCopyLink(link)
              localAction =
                NomeInvitationLocalAction.COPIED
            },
            modifier = Modifier.weight(1f),
            variant = NomeButtonVariant.SECONDARY,
            shape = NomeTheme.shapes.pill,
            leadingIcon = {
              Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = null,
              )
            },
          )
          NomeButton(
            text = stringResource(R.string.nome_p11_share),
            onClick = {
              onShareLink(link)
              localAction =
                NomeInvitationLocalAction.SHARED
            },
            modifier = Modifier.weight(1f),
            shape = NomeTheme.shapes.pill,
            leadingIcon = {
              Icon(
                Icons.Rounded.Share,
                contentDescription = null,
              )
            },
          )
        }
      }
    }
    Spacer(Modifier.height(10.dp))
    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border =
        BorderStroke(
          1.dp,
          NomeTheme.colors.border,
        ),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(R.string.nome_p11_qr_title),
          style = NomeTheme.typography.bodyStrong,
          color = NomeTheme.colors.textPrimary,
        )
        Text(
          text = stringResource(R.string.nome_p11_qr_body),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
        )
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(178.dp),
          contentAlignment = Alignment.Center,
        ) {
          SimpleXCreatedLinkQRCode(
            connLink = invitation,
            short = true,
            padding = PaddingValues(0.dp),
            tintColor = NomeTheme.colors.textPrimary,
            onShare = {
              onQrImageShared()
              localAction =
                NomeInvitationLocalAction.SHARED
            },
            imageSize = 136.dp,
          )
        }
        Spacer(Modifier.height(4.dp))
        Row(
          horizontalArrangement =
            Arrangement.spacedBy(8.dp),
        ) {
          NomeButton(
            text =
              stringResource(
                R.string.nome_p11_share_invitation,
              ),
            onClick = {
              onShareQr(link)
              localAction =
                NomeInvitationLocalAction.SHARED
            },
            modifier = Modifier.weight(1f),
            shape = NomeTheme.shapes.pill,
            leadingIcon = {
              Icon(
                Icons.Rounded.Share,
                contentDescription = null,
              )
            },
          )
          NomeButton(
            text = stringResource(R.string.nome_p11_learn_more),
            onClick = {
              ModalManager.start.showModalCloseable {
                close ->
                AddContactLearnMore(close)
              }
            },
            modifier = Modifier.weight(1f),
            variant = NomeButtonVariant.SECONDARY,
            shape = NomeTheme.shapes.pill,
            leadingIcon = {
              Icon(
                Icons.Rounded.Info,
                contentDescription = null,
              )
            },
          )
        }
      }
    }
    Spacer(Modifier.height(10.dp))
    NomeTruthStrip(
      icon = Icons.Rounded.Security,
      text =
        when (localAction) {
          NomeInvitationLocalAction.NONE ->
            stringResource(
              R.string.nome_p11_waiting,
            )
          NomeInvitationLocalAction.COPIED ->
            stringResource(
              R.string.nome_p11_copied_local,
            )
          NomeInvitationLocalAction.SHARED ->
            stringResource(
              R.string.nome_p11_shared_local,
            )
        },
    )
  }
}

@Composable
fun NomeScanPasteContent(
  pastedLink: String,
  onPastedLinkChange: (String) -> Unit,
  onSubmitPastedLink: () -> Unit,
  onReadClipboard: () -> Unit,
  scannerContent: @Composable () -> Unit,
  onOpenScanner: () -> Unit,
  onClose: () -> Unit,
) {
  var mode by rememberSaveable {
    mutableStateOf(NomeScanPasteMode.SCAN)
  }
  var scannerActivated by rememberSaveable {
    mutableStateOf(false)
  }

  NomePageScaffold(
    title = stringResource(R.string.nome_p12_title),
    backLabel = stringResource(R.string.nome_p12_back),
    onClose = onClose,
  ) {
    NomeSegmentedControl(
      mode = mode,
      onModeChange = {
        mode = it
      },
    )
    Spacer(Modifier.height(10.dp))
    if (mode == NomeScanPasteMode.SCAN) {
      NomeScannerCard(
        scannerActivated = scannerActivated,
        scannerContent = scannerContent,
        onOpenScanner = {
          scannerActivated = true
          onOpenScanner()
        },
      )
      Spacer(Modifier.height(10.dp))
      NomePasteCard(
        pastedLink = pastedLink,
        onPastedLinkChange = onPastedLinkChange,
        onSubmitPastedLink = onSubmitPastedLink,
        onReadClipboard = onReadClipboard,
      )
    } else {
      NomePasteCard(
        pastedLink = pastedLink,
        onPastedLinkChange = onPastedLinkChange,
        onSubmitPastedLink = onSubmitPastedLink,
        onReadClipboard = onReadClipboard,
      )
      Spacer(Modifier.height(10.dp))
      NomeScannerCard(
        scannerActivated = scannerActivated,
        scannerContent = scannerContent,
        onOpenScanner = {
          scannerActivated = true
          onOpenScanner()
        },
      )
    }
    Spacer(Modifier.height(10.dp))
    NomeTruthStrip(
      icon = Icons.Rounded.Security,
      text =
        stringResource(
          R.string.nome_p12_plan_note,
        ),
    )
  }
}

@Composable
private fun NomeScannerCard(
  scannerActivated: Boolean,
  scannerContent: @Composable () -> Unit,
  onOpenScanner: () -> Unit,
) {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    border =
      BorderStroke(
        1.dp,
        NomeTheme.colors.border,
      ),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (scannerActivated) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .aspectRatio(1.55f)
              .background(
                color = NomeTheme.colors.textPrimary,
                shape = NomeTheme.shapes.control,
              ),
          contentAlignment = Alignment.Center,
        ) {
          scannerContent()
        }
      } else {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(198.dp)
              .background(
                color = NomeTheme.colors.textPrimary,
                shape = NomeTheme.shapes.control,
              ),
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            modifier = Modifier.size(100.dp),
            shape = NomeTheme.shapes.control,
            color = NomeTheme.colors.successContainer,
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = NomeTheme.colors.action,
              )
            }
          }
        }
      }
      Spacer(Modifier.height(10.dp))
      Text(
        text = stringResource(R.string.nome_p12_scan_heading),
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text =
          stringResource(
            R.string.nome_p12_camera_permission,
          ),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
      if (!scannerActivated) {
        Spacer(Modifier.height(10.dp))
        NomeButton(
          text = stringResource(R.string.nome_p12_open_camera),
          onClick = onOpenScanner,
          modifier = Modifier.fillMaxWidth(),
          shape = NomeTheme.shapes.pill,
          leadingIcon = {
            Icon(
              Icons.Rounded.CameraAlt,
              contentDescription = null,
            )
          },
        )
      }
    }
  }
}

@Composable
private fun NomePasteCard(
  pastedLink: String,
  onPastedLinkChange: (String) -> Unit,
  onSubmitPastedLink: () -> Unit,
  onReadClipboard: () -> Unit,
) {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    border =
      BorderStroke(
        1.dp,
        NomeTheme.colors.border,
      ),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
    ) {
      Text(
        text = stringResource(R.string.nome_p12_link_label),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
      Spacer(Modifier.height(5.dp))
      OutlinedTextField(
        value = pastedLink,
        onValueChange = onPastedLinkChange,
        modifier =
          Modifier
            .fillMaxWidth()
            .nomeTalkBackSemantics(
              label =
                stringResource(
                  R.string.nome_p12_link_input,
                ),
            ),
        singleLine = true,
        textStyle = NomeTheme.typography.body,
        placeholder = {
          Text(
            text =
              stringResource(
                R.string.nome_p12_link_placeholder,
              ),
            style = NomeTheme.typography.body,
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Rounded.Link,
            contentDescription = null,
            tint = NomeTheme.colors.textTertiary,
          )
        },
        shape = NomeTheme.shapes.control,
        colors =
          TextFieldDefaults.outlinedTextFieldColors(
            textColor = NomeTheme.colors.textPrimary,
            cursorColor = NomeTheme.colors.action,
            focusedBorderColor = NomeTheme.colors.action,
            unfocusedBorderColor = NomeTheme.colors.border,
            placeholderColor = NomeTheme.colors.textTertiary,
            backgroundColor = NomeTheme.colors.surface,
          ),
      )
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable(onClick = onReadClipboard)
            .nomeMinimumTouchTarget()
            .nomeTalkBackSemantics(
              label =
                stringResource(
                  R.string.nome_p12_clipboard_action,
                ),
              role = Role.Button,
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Rounded.ContentPaste,
          contentDescription = null,
          modifier = Modifier.size(17.dp),
          tint = NomeTheme.colors.textTertiary,
        )
        Spacer(Modifier.width(7.dp))
        Text(
          text =
            stringResource(
              R.string.nome_p12_clipboard_action,
            ),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
        )
      }
      Spacer(Modifier.height(3.dp))
      NomeButton(
        text = stringResource(R.string.nome_p12_check),
        onClick = onSubmitPastedLink,
        modifier = Modifier.fillMaxWidth(),
        variant =
          if (pastedLink.isBlank()) {
            NomeButtonVariant.SECONDARY
          } else {
            NomeButtonVariant.PRIMARY
          },
        enabled = pastedLink.isNotBlank(),
        shape = NomeTheme.shapes.pill,
        leadingIcon = {
          Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
          )
        },
      )
    }
  }
}

@Composable
private fun NomeInvitationPendingCard(
  creating: Boolean,
  onRetry: () -> Unit,
) {
  NomeSurface(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 240.dp),
    border =
      BorderStroke(
        1.dp,
        NomeTheme.colors.border,
      ),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      if (creating) {
        CircularProgressIndicator(
          color = NomeTheme.colors.action,
        )
        Spacer(Modifier.height(16.dp))
        Text(
          text =
            stringResource(
              R.string.nome_p11_creating,
            ),
          style = NomeTheme.typography.bodyStrong,
          color = NomeTheme.colors.textPrimary,
        )
      } else {
        Text(
          text =
            stringResource(
              R.string.nome_p11_create_failed,
            ),
          style = NomeTheme.typography.bodyStrong,
          color = NomeTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(16.dp))
        NomeButton(
          text = stringResource(R.string.nome_p11_retry),
          onClick = onRetry,
          shape = NomeTheme.shapes.pill,
        )
      }
    }
  }
}

@Composable
private fun NomeSegmentedControl(
  mode: NomeScanPasteMode,
  onModeChange: (NomeScanPasteMode) -> Unit,
) {
  Surface(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 44.dp),
    shape = NomeTheme.shapes.pill,
    color = NomeTheme.colors.surfaceSubtle,
  ) {
    Row(
      modifier = Modifier.padding(2.dp),
    ) {
      NomeSegment(
        text = stringResource(R.string.nome_p12_scan_tab),
        selected = mode == NomeScanPasteMode.SCAN,
        onClick = {
          onModeChange(NomeScanPasteMode.SCAN)
        },
        modifier = Modifier.weight(1f),
      )
      NomeSegment(
        text = stringResource(R.string.nome_p12_paste_tab),
        selected = mode == NomeScanPasteMode.PASTE,
        onClick = {
          onModeChange(NomeScanPasteMode.PASTE)
        },
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun NomeSegment(
  text: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier,
) {
  Surface(
    modifier =
      modifier
        .heightIn(min = 40.dp)
        .clickable(onClick = onClick)
        .nomeMinimumTouchTarget()
        .semantics {
          this.selected = selected
        }
        .nomeTalkBackSemantics(
          label = text,
          state =
            stringResource(
              if (selected) {
                R.string.nome_p12_selected
              } else {
                R.string.nome_p12_not_selected
              },
            ),
          role = Role.Tab,
        ),
    shape = NomeTheme.shapes.pill,
    color =
      if (selected) {
        NomeTheme.colors.surface
      } else {
        Color.Transparent
      },
    elevation =
      if (selected) {
        NomeTheme.elevation.low
      } else {
        NomeTheme.elevation.none
      },
  ) {
    Box(
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = text,
        style = NomeTheme.typography.label,
        color =
          if (selected) {
            NomeTheme.colors.textPrimary
          } else {
            NomeTheme.colors.textSecondary
          },
      )
    }
  }
}

@Composable
private fun NomeTruthStrip(
  icon: ImageVector,
  text: String,
) {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    color = NomeTheme.colors.successContainer,
    border =
      BorderStroke(
        1.dp,
        NomeTheme.colors.sentMessageBorder,
      ),
    shape = NomeTheme.shapes.control,
  ) {
    Row(
      modifier =
        Modifier.padding(
          horizontal = 12.dp,
          vertical = 10.dp,
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = NomeTheme.colors.onSuccessContainer,
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = text,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.onSuccessContainer,
      )
    }
  }
}

@Composable
private fun NomeIconTile(
  icon: ImageVector,
  background: Color,
  tint: Color,
) {
  Surface(
    modifier = Modifier.size(42.dp),
    shape = NomeTheme.shapes.compact,
    color = background,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = tint,
      )
    }
  }
}

@Composable
private fun NomePageScaffold(
  title: String,
  backLabel: String,
  onClose: () -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(NomeTheme.colors.background)
        .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 56.dp)
          .padding(start = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = onClose,
        modifier =
          Modifier
            .size(
              NomeTheme.dimensions.minimumTouchTarget,
            )
            .nomeTalkBackSemantics(
              label = backLabel,
              role = Role.Button,
            ),
      ) {
        Icon(
          imageVector =
            Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = null,
          tint = NomeTheme.colors.textPrimary,
        )
      }
      Spacer(Modifier.width(12.dp))
      Text(
        text = title,
        modifier = Modifier.semantics { heading() },
        style = NomeTheme.typography.title,
        color = NomeTheme.colors.textPrimary,
        fontWeight = FontWeight.SemiBold,
      )
    }
    Divider(color = NomeTheme.colors.divider)
    Column(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(
            start = NomeTheme.dimensions.screenHorizontalInset,
            top = 10.dp,
            end = NomeTheme.dimensions.screenHorizontalInset,
            bottom = NomeTheme.dimensions.space24,
          ),
      content = content,
    )
  }
}
