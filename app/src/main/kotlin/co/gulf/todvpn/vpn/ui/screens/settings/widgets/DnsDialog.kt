package co.gulf.todvpn.vpn.ui.screens.settings.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.gulf.todvpn.vpn.R
import co.gulf.todvpn.vpn.ui.screens.settings.getLabelRes
import io.norselabs.vpn.based.viewModel.settings.SettingsScreenState
import io.norselabs.vpn.based.vpn.DdsConfigurator

@Composable
fun DnsDialog(
  state: SettingsScreenState,
  onConfirmClick: (DdsConfigurator.Dns) -> Unit,
  onDismissClick: () -> Unit,
  onDismissRequest: () -> Unit = {},
) {
  var radioState by remember { mutableStateOf(state.currentDns) }
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(stringResource(R.string.settings_dns_change_title)) },
    text = {
      Column {
        state.dnsOptions.forEach { dns ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .selectable(
                selected = dns == radioState,
                onClick = { radioState = dns },
                role = Role.RadioButton,
              )
              .padding(vertical = 8.dp),
          ) {
            RadioButton(
              selected = dns == radioState,
              onClick = null,
              modifier = Modifier.padding(end = 8.dp),
            )
            Text(
              text = stringResource(dns.getLabelRes()),
              maxLines = 1,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { radioState?.let(onConfirmClick) },
      ) { Text(stringResource(R.string.common_ok)) }
    },
    dismissButton = {
      Button(
        onClick = onDismissClick,
      ) {
        Text(stringResource(R.string.common_cancel))
      }
    },
  )
}

@Composable
@Preview
private fun DnsDialogPreview() {
  DnsDialog(
    state = SettingsScreenState(
      currentDns = DdsConfigurator.Dns.Cloudflare,
    ),
    onConfirmClick = {},
    onDismissClick = {},
    onDismissRequest = {},
  )
}
