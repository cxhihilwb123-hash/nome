package chat.simplex.common.ui.nome.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.getTimestampText
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.views.chatlist.NomeChatKindBadge
import chat.simplex.common.views.chatlist.nomeChatKind
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.res.MR

@Composable
fun NomeSearchRouteContent(
  query: String,
  results: NomeSearchResults,
  contentState: NomeHomeContentState,
  coreState: NomeHomeCoreState,
  onQueryChange: (String) -> Unit,
  onOpenChat: (Chat) -> Unit,
  onClose: () -> Unit,
) {
  BackHandler(onBack = onClose)
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(NomeTheme.colors.background)
      .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    NomeSearchTopBar(onClose)
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(
        start = NomeTheme.dimensions.screenHorizontalInset,
        end = NomeTheme.dimensions.screenHorizontalInset,
        bottom = NomeTheme.dimensions.space24,
      ),
    ) {
      item {
        Spacer(Modifier.height(10.dp))
        NomeSearchField(
          query = query,
          onQueryChange = onQueryChange,
        )
      }

      if (coreState == NomeHomeCoreState.STOPPED) {
        item {
          Text(
            text = stringResource(R.string.nome_p09_core_stopped),
            modifier = Modifier.padding(top = 12.dp),
            style = NomeTheme.typography.supporting,
            color = NomeTheme.colors.warning,
          )
        }
      }

      when {
        query.isBlank() -> item {
          Text(
            text = stringResource(R.string.nome_p09_idle),
            modifier = Modifier.padding(top = 18.dp),
            style = NomeTheme.typography.supporting,
            color = NomeTheme.colors.textSecondary,
          )
        }
        contentState == NomeHomeContentState.LOADING -> item {
          Text(
            text = stringResource(R.string.nome_p09_loading),
            modifier = Modifier.padding(top = 18.dp),
            style = NomeTheme.typography.supporting,
            color = NomeTheme.colors.textSecondary,
          )
        }
        contentState == NomeHomeContentState.UNAVAILABLE -> item {
          Text(
            text = stringResource(R.string.nome_p09_unavailable),
            modifier = Modifier.padding(top = 18.dp),
            style = NomeTheme.typography.supporting,
            color = NomeTheme.colors.danger,
          )
        }
        contentState == NomeHomeContentState.FILTERED_NO_RESULT &&
          results.isEmpty -> item {
          NomeSearchEmpty(query)
        }
        else -> {
          item {
            Text(
              text = stringResource(R.string.nome_p09_results_heading),
              modifier = Modifier
                .padding(top = 16.dp, bottom = 4.dp)
                .semantics { heading() },
              style = NomeTheme.typography.supporting,
              color = NomeTheme.colors.textSecondary,
            )
          }
          items(
            items = results.ordered,
            key = { it.remoteHostId to it.id },
          ) { chat ->
            NomeSearchResultRow(
              chat = chat,
              enabled = coreState == NomeHomeCoreState.RUNNING &&
                chat.chatInfo.ready,
              onClick = { onOpenChat(chat) },
            )
          }
          item {
            NomeSearchUnsupportedMessageScope()
          }
          item {
            NomeSearchRecentPolicy()
          }
        }
      }
    }
  }

}

@Composable
private fun NomeSearchUnsupportedMessageScope() {
  Column(
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = stringResource(R.string.nome_p09_messages_heading),
      modifier = Modifier
        .padding(top = 16.dp, bottom = 6.dp)
        .semantics { heading() },
      style = NomeTheme.typography.supporting,
      color = NomeTheme.colors.textSecondary,
    )
    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      shape = NomeTheme.shapes.control,
      color = NomeTheme.colors.successContainer,
      border = BorderStroke(1.dp, NomeTheme.colors.sentMessageBorder),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Rounded.ChatBubbleOutline,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = NomeTheme.colors.action,
        )
        Spacer(Modifier.width(10.dp))
        Text(
          text = stringResource(R.string.nome_p09_messages_scope),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
        )
      }
    }
  }
}

@Composable
private fun NomeSearchRecentPolicy() {
  Column(
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = stringResource(R.string.nome_p09_recent_heading),
      modifier = Modifier
        .padding(top = 16.dp, bottom = 6.dp)
        .semantics { heading() },
      style = NomeTheme.typography.supporting,
      color = NomeTheme.colors.textSecondary,
    )
    NomeSurface(
      shape = NomeTheme.shapes.pill,
      color = NomeTheme.colors.background,
      border = BorderStroke(1.dp, NomeTheme.colors.border),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Rounded.Search,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = NomeTheme.colors.textSecondary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.nome_p09_recent_policy),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
        )
      }
    }
  }
}

@Composable
private fun NomeSearchTopBar(onClose: () -> Unit) {
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
          label = stringResource(R.string.nome_p09_back),
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
      text = stringResource(R.string.nome_p09_title),
      modifier = Modifier.semantics { heading() },
      style = NomeTheme.typography.title,
      color = NomeTheme.colors.textPrimary,
      fontWeight = FontWeight.SemiBold,
    )
  }
  Divider(color = NomeTheme.colors.divider)
}

@Composable
private fun NomeSearchField(
  query: String,
  onQueryChange: (String) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  NomeSurface(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp),
    shape = NomeTheme.shapes.pill,
    color = NomeTheme.colors.surfaceSubtle,
  ) {
    BasicTextField(
      value = query,
      onValueChange = onQueryChange,
      modifier = Modifier
        .fillMaxWidth()
        .nomeTalkBackSemantics(
          label = stringResource(R.string.nome_p09_search_semantics),
        ),
      textStyle = NomeTheme.typography.body.copy(
        color = NomeTheme.colors.textPrimary,
      ),
      singleLine = true,
      keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(
        onSearch = { focusManager.clearFocus() },
      ),
      cursorBrush = SolidColor(NomeTheme.colors.action),
      decorationBox = { innerTextField ->
        Row(
          modifier = Modifier.padding(start = 14.dp, end = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = NomeTheme.colors.textSecondary,
          )
          Spacer(Modifier.width(8.dp))
          Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
              Text(
                text = stringResource(R.string.nome_p09_search_placeholder),
                style = NomeTheme.typography.body,
                color = NomeTheme.colors.textSecondary,
              )
            }
            innerTextField()
          }
          if (query.isNotEmpty()) {
            IconButton(
              onClick = { onQueryChange("") },
              modifier = Modifier
                .size(NomeTheme.dimensions.minimumTouchTarget)
                .nomeTalkBackSemantics(
                  label = stringResource(R.string.nome_p09_clear),
                  role = Role.Button,
                ),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = NomeTheme.colors.textSecondary,
              )
            }
          }
        }
      },
    )
  }
}

@Composable
private fun NomeSearchResultRow(
  chat: Chat,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  val info = chat.chatInfo
  val rowEnabled = enabled && when (info) {
    is ChatInfo.Direct -> !info.contactCard
    is ChatInfo.Group,
    is ChatInfo.Local -> true
    is ChatInfo.ContactRequest,
    is ChatInfo.ContactConnection,
    is ChatInfo.InvalidJSON -> false
  }
  val title = info.chatViewName
  val body = nomeSearchResultDescription(chat)
  val chatKind = nomeChatKind(info)
  val timestamp =
    getTimestampText(chat.chatItems.lastOrNull()?.meta?.itemTs ?: info.chatTs)
  val icon =
    when (info) {
      is ChatInfo.Group ->
        if (info.groupInfo.isChannel) {
          MR.images.ic_bigtop_updates_circle_filled
        } else {
          MR.images.ic_supervised_user_circle_filled
        }
      else -> MR.images.ic_account_circle_filled
    }
  val interaction =
    if (rowEnabled) {
      Modifier
        .clickable(onClick = onClick)
        .nomeMinimumTouchTarget()
        .nomeTalkBackSemantics(
          label = stringResource(
            R.string.nome_p09_open_result,
            title,
            body,
          ),
          role = Role.Button,
        )
    } else {
      Modifier.nomeMinimumTouchTarget()
    }

  Column(modifier = interaction.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ProfileImage(
        size = 44.dp,
        image = info.image,
        icon = icon,
        color = NomeTheme.colors.action,
        backgroundColor = NomeTheme.colors.surfaceContainer,
      )
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            modifier = Modifier.weight(1f, fill = false),
            style = NomeTheme.typography.bodyStrong,
            color = NomeTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          chatKind?.let {
            Spacer(Modifier.width(6.dp))
            NomeChatKindBadge(it)
          }
        }
        Text(
          text = body,
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = timestamp,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
      Spacer(Modifier.width(4.dp))
      if (rowEnabled) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
          contentDescription = null,
          tint = NomeTheme.colors.textTertiary,
        )
      }
    }
    Divider(color = NomeTheme.colors.divider)
  }
}

@Composable
private fun nomeSearchResultDescription(chat: Chat): String =
  when (val info = chat.chatInfo) {
    is ChatInfo.Direct -> stringResource(R.string.nome_p09_type_contact)
    is ChatInfo.Group ->
      if (info.groupInfo.isChannel) {
        stringResource(R.string.nome_p09_type_channel)
      } else if (info.groupInfo.groupSummary.currentMembers > 0) {
        stringResource(
          R.string.nome_p09_type_group_members,
          info.groupInfo.groupSummary.currentMembers,
        )
      } else {
        stringResource(R.string.nome_p09_type_group)
      }
    is ChatInfo.Local -> stringResource(R.string.nome_p09_type_notes)
    is ChatInfo.ContactRequest ->
      stringResource(R.string.nome_p09_type_request)
    is ChatInfo.ContactConnection ->
      stringResource(R.string.nome_p09_type_pending)
    is ChatInfo.InvalidJSON ->
      stringResource(R.string.nome_p09_type_unavailable)
  }

@Composable
private fun NomeSearchEmpty(query: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 42.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      imageVector = Icons.Rounded.Search,
      contentDescription = null,
      modifier = Modifier.size(32.dp),
      tint = NomeTheme.colors.textTertiary,
    )
    Text(
      text = stringResource(R.string.nome_p09_no_result_title),
      style = NomeTheme.typography.bodyStrong,
      color = NomeTheme.colors.textPrimary,
    )
    Text(
      text = stringResource(R.string.nome_p09_no_result_body, query),
      style = NomeTheme.typography.supporting,
      color = NomeTheme.colors.textSecondary,
    )
  }
}
