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
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FirebaseFirestore
import io.norselabs.vpn.based.viewModel.settings.SettingsScreenViewModel
import io.norselabs.vpn.based.vpn.DdsConfigurator
import io.norselabs.vpn.core_vpn.vpn.Protocol
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString


@Composable
fun SettingsScreen(
  navigateBack: () -> Unit,
  navigateToSplitTunneling: () -> Unit,
  shareLogs: () -> Unit,
) {

  val viewModel = hiltViewModel<SettingsScreenViewModel>()
  val state by viewModel.stateHolder.state.collectAsState()
  var expirationDate by remember { mutableStateOf("") }
  val context = LocalContext.current


  val deviceId = remember {
    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
  }
  LaunchedEffect(Unit) {
    if (deviceId.isEmpty()) {
      expirationDate = "Unable to get device ID"
      return@LaunchedEffect
    }

    FirebaseFirestore.getInstance()
      .collection("codes")
      .whereEqualTo("deviceId", deviceId)
      .get()
      .addOnSuccessListener { querySnapshot ->
        if (querySnapshot.isEmpty) {
          expirationDate = "Device not registered"
          return@addOnSuccessListener
        }


        val document = querySnapshot.documents[0]
        val timestamp = document.getTimestamp("endDate")
        val activationCode = document.getString("code")

        expirationDate = if (timestamp != null) {
          val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
          dateFormat.format(timestamp.toDate())
        } else {
          "No expiration date found"
        }

      }
      .addOnFailureListener {
        expirationDate = "Error loading data"
      }
  }

  EffectHandler(viewModel.stateHolder.effects) { effect ->
    when (effect) {
      is SettingsScreenEffect.OpenTelegram -> Unit

      is SettingsScreenEffect.ShareLogs -> shareLogs()

      is SettingsScreenEffect.SplitTunneling -> navigateToSplitTunneling()
    }
  }

  SettingsScreenStateless(
    state = state,
    expirationDate = expirationDate,
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
  expirationDate: String,
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
        expirationDate = expirationDate,
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
  expirationDate: String,
  onDnsRowClick: () -> Unit,
  onDnsDialogConfirmClick: (DdsConfigurator.Dns) -> Unit,
  onDnsDialogDismissClick: () -> Unit,
  onProtocolRowClick: () -> Unit,
  onProtocolDialogConfirmClick: (Protocol) -> Unit,
  onProtocolDialogDismissClick: () -> Unit,
  onSplitTunnelingClick: () -> Unit,
  onLogsRowClick: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .padding(paddingValues)
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

      SettingsRow(
        title = stringResource(R.string.expiration_date),
        value = expirationDate,
        modifier = Modifier.clickable { /* Optional action */ }
      )
      HorizontalDivider(color = BasedAppColor.Divider)
      SettingsRow(
        title = stringResource(R.string.about_the_app),
        value = "App Version 1.0.0",
        modifier = Modifier.clickable {

        }
      )
      HorizontalDivider(color = BasedAppColor.Divider)


      val annotatedString = buildAnnotatedString {
        pushStyle(SpanStyle(color = Color.White))
        append("To buy the activation code please visit the website /n  ")
        pushStyle(SpanStyle(color = Color(0xFF00BFFF)))
        append("https://elitbahrain.rmz.gg/")
        pop()
      }

      Text(
        text = annotatedString,
        modifier = Modifier
          .padding(start = 16.dp, end = 16.dp, top = 20.dp)
          .fillMaxWidth(),
        style = MaterialTheme.typography.body1
      )
    }

    HorizontalDivider(color = BasedAppColor.Divider)

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
