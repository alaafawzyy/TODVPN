package co.gulf.todvpn.vpn.ui.screens.split_tunneling

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import io.norselabs.vpn.based.viewModel.split_tunneling.NetworkApp
import io.norselabs.vpn.based.viewModel.split_tunneling.SplitTunnelingScreenState as State
import io.norselabs.vpn.based.viewModel.split_tunneling.SplitTunnelingScreenViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import co.gulf.todvpn.vpn.R
import co.gulf.todvpn.vpn.ui.theme.BasedAppColor
import co.gulf.todvpn.vpn.ui.widget.TopBar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import io.norselabs.vpn.core_vpn.vpn.split_tunneling.SplitTunnelingStatus
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged

@Composable
fun SplitTunnelingScreenScreen(
  navigateBack: () -> Unit,
) {
  val viewModel = hiltViewModel<SplitTunnelingScreenViewModel>()
  val state by viewModel.stateHolder.state.collectAsState()

  SplitTunnelingScreenStateless(
    state = state,
    navigateBack = navigateBack,
    setSplitTunnelingStatus = viewModel::setSplitTunnelingStatus,
    onAppChecked = viewModel::onAppChecked,
  )
}

@Composable
fun SplitTunnelingScreenStateless(
  state: State,
  navigateBack: () -> Unit,
  setSplitTunnelingStatus: (SplitTunnelingStatus) -> Unit,
  onAppChecked: (NetworkApp, Boolean) -> Unit,
) {
  Scaffold(
    containerColor = BasedAppColor.Background,
    topBar = {
      TopBar(
        title = stringResource(R.string.split_tunneling_title),
        navigateBack = navigateBack,
      )
    },
    content = { paddingValues ->
      LazyColumn(
        modifier = Modifier
          .padding(paddingValues)
          .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // Options Section
        item {
          SplitTunnelingOptions(
            status = state.status,
            setSplitTunnelingStatus = setSplitTunnelingStatus,
          )
        }

        // Apps Section
        items(
          items = state.applications,
          key = { it.packageName },
        ) { app ->
          val focusRequester = remember { FocusRequester() }
          SplitTunnelingAppRow(
            app = app,
            onCheck = onAppChecked,
            focusRequester = focusRequester
          )
        }
      }
    },
  )
}

@Composable
private fun SplitTunnelingOptions(
  status: SplitTunnelingStatus,
  setSplitTunnelingStatus: (SplitTunnelingStatus) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    SplitTunnelingOptionButton(
      label = stringResource(R.string.split_tunneling_disabled),
      isSelected = status == SplitTunnelingStatus.Disabled,
      onClick = { setSplitTunnelingStatus(SplitTunnelingStatus.Disabled) },
    )
    SplitTunnelingOptionButton(
      label = stringResource(R.string.split_tunneling_enabled),
      isSelected = status == SplitTunnelingStatus.Enabled,
      onClick = { setSplitTunnelingStatus(SplitTunnelingStatus.Enabled) },
    )
    SplitTunnelingOptionButton(
      label = stringResource(R.string.split_tunneling_bypass),
      isSelected = status == SplitTunnelingStatus.Bypass,
      onClick = { setSplitTunnelingStatus(SplitTunnelingStatus.Bypass) },
    )
  }
}

@Composable
private fun SplitTunnelingOptionButton(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  var isFocused by remember { mutableStateOf(false) }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(
        color = if (isSelected) BasedAppColor.ButtonPrimary.copy(alpha = 0.3f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
      )
      .focusable()
      .onFocusChanged { focusState -> isFocused = focusState.isFocused }
      .graphicsLayer(
        scaleX = if (isFocused) 1.05f else 1f,
        scaleY = if (isFocused) 1.05f else 1f,
      )
      .clickable(onClick = onClick)
      .padding(16.dp),
  ) {
    Text(
      text = label,
      fontSize = 16.sp,
      color = BasedAppColor.TextPrimary,
      modifier = Modifier.weight(1f),
    )
    Switch(
      checked = isSelected,
      onCheckedChange = null, // Handled by the button click
    )
  }
}

@Composable
private fun SplitTunnelingAppRow(
  app: NetworkApp,
  onCheck: (NetworkApp, Boolean) -> Unit,
  focusRequester: FocusRequester
) {
  var isFocused by remember { mutableStateOf(false) }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .focusable()
      .focusRequester(focusRequester)
      .onFocusChanged { focusState -> isFocused = focusState.isFocused }
      .graphicsLayer(
        scaleX = if (isFocused) 1.05f else 1f,
        scaleY = if (isFocused) 1.05f else 1f,
      )
      .background(
        color = if (isFocused) BasedAppColor.ButtonPrimary.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
      )
      .clickable { onCheck(app, !app.isChecked) }
      .padding(12.dp),
  ) {
    AsyncImage(
      model = app.appIcon,
      contentDescription = null,
      modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(4.dp)),
    )
    Spacer(modifier = Modifier.size(12.dp))
    Column(
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.weight(1f),
    ) {
      Text(
        text = app.appName,
        fontSize = 16.sp,
        color = BasedAppColor.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = app.packageName,
        fontSize = 14.sp,
        color = BasedAppColor.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Checkbox(
      checked = app.isChecked,
      onCheckedChange = { onCheck(app, it) },
    )
  }
}


@Composable
private fun SwitchRow(
  text: String,
  checked: Boolean,
  onCheckedChange: ((Boolean) -> Unit)?,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier,
  ) {
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
    )
    Spacer(modifier = Modifier.size(8.dp))
    Text(
      text = text,
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      fontSize = 18.sp,
      color = BasedAppColor.TextPrimary,
    )
  }
}
