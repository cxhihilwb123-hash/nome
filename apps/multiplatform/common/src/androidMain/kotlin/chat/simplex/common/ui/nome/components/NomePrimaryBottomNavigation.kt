package chat.simplex.common.ui.nome.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.theme.NomeTheme

enum class NomePrimaryDestination {
  HOME,
  CONTACTS,
  SETTINGS,
}

private val NomePrimaryBottomNavigationHeight = 64.dp
private val NomePrimaryBottomNavigationInsetLimit = 36.dp

@Composable
fun NomePrimaryBottomNavigation(
  selected: NomePrimaryDestination,
  onDestinationSelected: (NomePrimaryDestination) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val view = LocalView.current
  val safeDrawingBottomPx = WindowInsets.safeDrawing.getBottom(density)
  val systemGestureBottomPx =
    WindowInsets.systemGestures.getBottom(density)
  val platformNavigationBottomPx =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val rootWindowInsets = view.rootWindowInsets
      maxOf(
        rootWindowInsets
          ?.getInsetsIgnoringVisibility(
            android.view.WindowInsets.Type.navigationBars(),
          )?.bottom ?: 0,
        rootWindowInsets
          ?.getInsetsIgnoringVisibility(
            android.view.WindowInsets.Type.systemGestures(),
          )?.bottom ?: 0,
      )
    } else {
      @Suppress("DEPRECATION")
      (view.rootWindowInsets?.stableInsetBottom ?: 0)
    }
  val liveBottomInsetPx =
    maxOf(
      safeDrawingBottomPx,
      systemGestureBottomPx,
      platformNavigationBottomPx,
    )
  val configuredNavigationBottomPx =
    if (liveBottomInsetPx == 0) {
      val resourceId =
        context.resources.getIdentifier(
          "navigation_bar_height",
          "dimen",
          "android",
        )
      if (resourceId != 0) {
        context.resources.getDimensionPixelSize(resourceId)
      } else {
        0
      }
    } else {
      0
    }
  val bottomSafeInset =
    with(density) {
      maxOf(
        liveBottomInsetPx,
        configuredNavigationBottomPx,
      ).toDp().coerceAtMost(
        NomePrimaryBottomNavigationInsetLimit,
      )
    }
  val items =
    listOf(
      Triple(
        NomePrimaryDestination.HOME,
        stringResource(R.string.nome_primary_nav_home),
        Icons.Rounded.Home,
      ),
      Triple(
        NomePrimaryDestination.CONTACTS,
        stringResource(R.string.nome_primary_nav_contacts),
        Icons.Rounded.People,
      ),
      Triple(
        NomePrimaryDestination.SETTINGS,
        stringResource(R.string.nome_primary_nav_settings),
        Icons.Rounded.Settings,
      ),
    )

  Column(
    modifier =
      modifier
        .fillMaxWidth(),
  ) {
    Divider(color = NomeTheme.colors.divider)
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(
            NomePrimaryBottomNavigationHeight +
              bottomSafeInset,
          )
          .background(NomeTheme.colors.surface)
          .padding(horizontal = NomeTheme.dimensions.space16)
          .padding(bottom = bottomSafeInset),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      items.forEach { (destination, label, icon) ->
        NomePrimaryNavigationItem(
          label = label,
          icon = icon,
          selected = destination == selected,
          onClick = {
            if (destination != selected) {
              onDestinationSelected(destination)
            }
          },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun NomePrimaryNavigationItem(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .selectable(
          selected = selected,
          role = Role.Tab,
          onClick = onClick,
        )
        .nomeTalkBackSemantics(
          label = label,
          state =
            if (selected) {
              stringResource(R.string.nome_primary_nav_selected)
            } else {
              null
          },
          role = Role.Tab,
        )
        .height(NomePrimaryBottomNavigationHeight)
        .widthIn(min = 72.dp)
        .nomeMinimumTouchTarget(),
  ) {
    if (selected) {
      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .width(32.dp)
            .height(3.dp)
            .clip(NomeTheme.shapes.pill)
            .background(NomeTheme.colors.success),
      )
    }
    Column(
      modifier =
        Modifier
          .align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement =
        Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(NomeTheme.dimensions.icon),
        tint =
          if (selected) {
            NomeTheme.colors.success
          } else {
          NomeTheme.colors.textSecondary
        },
      )
      Text(
        text = label,
        style = NomeTheme.typography.supporting,
        color =
          if (selected) {
            NomeTheme.colors.success
          } else {
            NomeTheme.colors.textSecondary
          },
      )
    }
  }
}
