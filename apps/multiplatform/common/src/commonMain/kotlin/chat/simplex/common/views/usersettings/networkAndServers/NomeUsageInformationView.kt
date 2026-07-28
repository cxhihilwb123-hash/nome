package chat.simplex.common.views.usersettings.networkAndServers

import SectionBottomSpacer
import SectionDividerSpaced
import SectionItemView
import SectionView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.ConditionsAcceptance
import chat.simplex.common.model.OperatorTag
import chat.simplex.common.model.ServerOperator
import chat.simplex.common.model.UserOperatorServers
import chat.simplex.common.platform.AppPlatform
import chat.simplex.common.platform.ColumnWithScrollBar
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.common.views.helpers.AppBarTitle
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

/**
 * Returns true only when every enabled managed operator is Nome. Custom server groups do not
 * represent an operator and therefore do not change which operator information screen applies.
 */
internal fun shouldShowNomeUsageInformation(userServers: List<UserOperatorServers>): Boolean {
  val enabledOperators = userServers.mapNotNull { it.operator }.filter { it.enabled }
  return enabledOperators.isNotEmpty() && enabledOperators.all { it.operatorTag == OperatorTag.Nome }
}

/**
 * Desktop is a Nome-only product surface: historical managed operators stay in storage for safe
 * migration, but they are not presented as choices. Android keeps the compatibility UI.
 */
internal fun shouldShowManagedOperator(platform: AppPlatform, operator: ServerOperator?): Boolean =
  operator != null && (!platform.isDesktop || operator.operatorTag == OperatorTag.Nome)

internal enum class OperatorInformationDestination {
  NomeLocalInformation,
  LegacyConditions,
  None,
}

/** Selects the only operator-information flow reachable from the network settings overview. */
internal fun operatorInformationDestination(
  platform: AppPlatform,
  userServers: List<UserOperatorServers>,
  hasLegacyConditionsAction: Boolean,
  anyOperatorEnabled: Boolean,
): OperatorInformationDestination = when {
  platform.isDesktop -> OperatorInformationDestination.NomeLocalInformation
  shouldShowNomeUsageInformation(userServers) -> OperatorInformationDestination.NomeLocalInformation
  hasLegacyConditionsAction && anyOperatorEnabled -> OperatorInformationDestination.LegacyConditions
  else -> OperatorInformationDestination.None
}

internal enum class OperatorEnableDestination {
  Enable,
  NomeLocalInformation,
  LegacyConditions,
  Ignore,
}

/**
 * Desktop never enables a non-Nome managed operator and never accepts conditions. If a Nome
 * operator unexpectedly requires conditions, the user only sees Nome's local operational
 * information and the operator remains disabled.
 */
internal fun operatorEnableDestination(
  platform: AppPlatform,
  operator: ServerOperator?,
): OperatorEnableDestination {
  if (operator == null) return OperatorEnableDestination.Ignore
  if (platform.isDesktop) {
    if (operator.operatorTag != OperatorTag.Nome) return OperatorEnableDestination.Ignore
    return when (operator.conditionsAcceptance) {
      is ConditionsAcceptance.Accepted -> OperatorEnableDestination.Enable
      is ConditionsAcceptance.Required -> OperatorEnableDestination.NomeLocalInformation
    }
  }
  return when (val acceptance = operator.conditionsAcceptance) {
    is ConditionsAcceptance.Accepted -> OperatorEnableDestination.Enable
    is ConditionsAcceptance.Required -> if (acceptance.deadline == null) {
      OperatorEnableDestination.LegacyConditions
    } else {
      OperatorEnableDestination.Enable
    }
  }
}

/**
 * Local operational guidance for this Nome build. This view intentionally does not load, accept,
 * or link to any third-party terms or privacy policy.
 */
@Composable
fun NomeUsageInformationView() {
  ColumnWithScrollBar(modifier = Modifier.fillMaxSize()) {
    AppBarTitle(
      stringResource(MR.strings.nome_usage_information_title),
      enableAlphaChanges = false,
    )
    Text(
      text = stringResource(MR.strings.nome_usage_information_intro),
      color = MaterialTheme.colors.secondary,
      style = MaterialTheme.typography.body1,
      lineHeight = 22.sp,
      modifier = Modifier.padding(horizontal = DEFAULT_PADDING),
    )
    SectionDividerSpaced()
    SectionView {
      NomeUsageInformationRow(
        icon = painterResource(MR.images.ic_dns),
        title = stringResource(MR.strings.nome_usage_information_routing_title),
        body = stringResource(MR.strings.nome_usage_information_routing_body),
      )
      NomeUsageInformationRow(
        icon = painterResource(MR.images.ic_upload_file),
        title = stringResource(MR.strings.nome_usage_information_endpoints_title),
        body = stringResource(MR.strings.nome_usage_information_endpoints_body),
      )
      NomeUsageInformationRow(
        icon = painterResource(MR.images.ic_database),
        title = stringResource(MR.strings.nome_usage_information_local_data_title),
        body = stringResource(MR.strings.nome_usage_information_local_data_body),
      )
      NomeUsageInformationRow(
        icon = painterResource(MR.images.ic_security),
        title = stringResource(MR.strings.nome_usage_information_connections_title),
        body = stringResource(MR.strings.nome_usage_information_connections_body),
      )
    }
    SectionBottomSpacer()
  }
}

@Composable
private fun NomeUsageInformationRow(
  icon: Painter,
  title: String,
  body: String,
) {
  SectionItemView {
    Icon(
      painter = icon,
      contentDescription = null,
      tint = MaterialTheme.colors.primary,
      modifier = Modifier.size(24.dp),
    )
    Spacer(Modifier.width(14.dp))
    Column {
      Text(
        text = title,
        color = MaterialTheme.colors.onBackground,
        style = MaterialTheme.typography.body1,
      )
      Text(
        text = body,
        color = MaterialTheme.colors.secondary,
        style = MaterialTheme.typography.body2,
        lineHeight = 20.sp,
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}
