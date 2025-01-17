package co.gulf.todvpn.vpn.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import co.sentinel.based_vpn.R as BasedR
import co.gulf.todvpn.vpn.R
import co.gulf.todvpn.vpn.ui.screens.settings.widgets.DnsDialog
import co.gulf.todvpn.vpn.ui.screens.settings.widgets.ProtocolDialog
import co.gulf.todvpn.vpn.ui.theme.BasedAppColor
import co.gulf.todvpn.vpn.ui.widget.TopBar
import io.norselabs.vpn.based.compose.EffectHandler
import io.norselabs.vpn.based.viewModel.settings.SettingsScreenEffect
import io.norselabs.vpn.based.viewModel.settings.SettingsScreenState as State
import io.norselabs.vpn.based.viewModel.settings.SettingsScreenViewModel
import io.norselabs.vpn.based.vpn.DdsConfigurator
import io.norselabs.vpn.core_vpn.vpn.Protocol

@Composable
fun SettingsScreen(
  navigateBack: () -> Unit,
  navigateToSplitTunneling: () -> Unit,
  shareLogs: () -> Unit,
) {

  val viewModel = hiltViewModel<SettingsScreenViewModel>()
  val state by viewModel.stateHolder.state.collectAsState()

  EffectHandler(viewModel.stateHolder.effects) { effect ->
    when (effect) {
      is SettingsScreenEffect.OpenTelegram -> Unit

      is SettingsScreenEffect.ShareLogs -> shareLogs()

      is SettingsScreenEffect.SplitTunneling -> navigateToSplitTunneling()
    }
  }

  SettingsScreenStateless(
    state = state,
    navigateBack = navigateBack,
    onDnsRowClick = viewModel::onDnsRowClick,
    onDnsDialogConfirmClick = viewModel::onDnsSelected,
    onDnsDialogDismissClick = viewModel::onDnsDialogDismissClick,
    onProtocolRowClick = viewModel::onProtocolRowClick,
    onProtocolDialogConfirmClick = viewModel::onProtocolSelected,
    onProtocolDialogDismissClick = viewModel::onProtocolDialogDismissClick,
    onSplitTunnelingClick = viewModel::onSplitTunnelClick,
    onLogsRowClick = viewModel::onLogsRowClick,
  )
}

@Composable
fun SettingsScreenStateless(
  state: State,
  navigateBack: () -> Unit,
  onDnsRowClick: () -> Unit,
  onDnsDialogConfirmClick: (DdsConfigurator.Dns) -> Unit,
  onDnsDialogDismissClick: () -> Unit,
  onProtocolRowClick: () -> Unit,
  onProtocolDialogConfirmClick: (Protocol) -> Unit,
  onProtocolDialogDismissClick: () -> Unit,
  onSplitTunnelingClick: () -> Unit,
  onLogsRowClick: () -> Unit,
) {
  Scaffold(
    containerColor = BasedAppColor.Background,
    topBar = {
      TopBar(
        title = stringResource(R.string.settings_title),
        navigateBack = navigateBack,
      )
    },
    content = { paddingValues ->
      Content(
        paddingValues = paddingValues,
        state = state,
        onDnsRowClick = onDnsRowClick,
        onDnsDialogConfirmClick = onDnsDialogConfirmClick,
        onDnsDialogDismissClick = onDnsDialogDismissClick,
        onProtocolRowClick = onProtocolRowClick,
        onProtocolDialogConfirmClick = onProtocolDialogConfirmClick,
        onProtocolDialogDismissClick = onProtocolDialogDismissClick,
        onSplitTunnelingClick = onSplitTunnelingClick,
        onLogsRowClick = onLogsRowClick,
      )
    },
  )
}

@Composable
fun Content(
  paddingValues: PaddingValues,
  state: State,
  onDnsRowClick: () -> Unit,
  onDnsDialogConfirmClick: (DdsConfigurator.Dns) -> Unit,
  onDnsDialogDismissClick: () -> Unit,
  onProtocolRowClick: () -> Unit,
  onProtocolDialogConfirmClick: (Protocol) -> Unit,
  onProtocolDialogDismissClick: () -> Unit,
  onSplitTunnelingClick: () -> Unit,
  onLogsRowClick: () -> Unit,
) {
  Box {
    Column(
      modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .padding(paddingValues),
    ) {
      SettingsRow(
        title = stringResource(R.string.settings_row_dns),
        value = state.currentDns
          ?.let { stringResource(it.getLabelRes()) } ?: "",
        modifier = Modifier
          .clickable(onClick = onDnsRowClick),
      )
      HorizontalDivider(color = BasedAppColor.Divider)
      SettingsRow(
        title = stringResource(R.string.settings_row_protocol),
        value = stringResource(state.currentProtocol.getLabelRes()),
        modifier = Modifier
          .clickable(onClick = onProtocolRowClick),
      )
      HorizontalDivider(color = BasedAppColor.Divider)
      SettingsRow(
        title = stringResource(R.string.settings_row_split_tunneling),
        value = "",
        modifier = Modifier
          .clickable(onClick = onSplitTunnelingClick),
      )
      HorizontalDivider(color = BasedAppColor.Divider)
      SettingsRow(
        title = stringResource(R.string.settings_row_logs),
        value = "",
        modifier = Modifier
          .clickable(onClick = onLogsRowClick),
      )
      HorizontalDivider(color = BasedAppColor.Divider)
    }
    if (state.isDnsSelectorVisible) {
      DnsDialog(
        state = state,
        onConfirmClick = onDnsDialogConfirmClick,
        onDismissClick = onDnsDialogDismissClick,
        onDismissRequest = onDnsDialogDismissClick,
      )
    }

    if (state.isProtocolSelectorVisible) {
      ProtocolDialog(
        state = state,
        onConfirmClick = onProtocolDialogConfirmClick,
        onDismissClick = onProtocolDialogDismissClick,
        onDismissRequest = onProtocolDialogDismissClick,
      )
    }
  }
}

@Composable
private fun SettingsRow(
  title: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .heightIn(min = 54.dp)
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .fillMaxWidth(),
  ) {
    Text(
      text = title,
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      fontSize = 16.sp, // Fixed fontSize
      color = BasedAppColor.TextPrimary,
      modifier = Modifier.weight(1f),
    )
    Text(
      text = value,
      textAlign = TextAlign.End,
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      fontSize = 16.sp, // Fixed fontSize
      color = BasedAppColor.TextPrimary,
      modifier = Modifier.weight(1f),
    )
  }
}

fun DdsConfigurator.Dns.getLabelRes() = when (this) {
  DdsConfigurator.Dns.Cloudflare -> R.string.settings_dns_cloudflare
  DdsConfigurator.Dns.Google -> R.string.settings_dns_google
  DdsConfigurator.Dns.Handshake -> R.string.settings_dns_handshake
}

fun Protocol?.getLabelRes() = when (this) {
  Protocol.WIREGUARD -> BasedR.string.settings_protocol_wireguard
  Protocol.V2RAY -> BasedR.string.settings_protocol_v2ray
  else -> BasedR.string.settings_protocol_any
}
