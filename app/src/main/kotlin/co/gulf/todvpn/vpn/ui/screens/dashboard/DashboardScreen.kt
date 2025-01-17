package co.gulf.todvpn.vpn.ui.screens.dashboard

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import co.gulf.todvpn.vpn.R
import co.gulf.todvpn.vpn.ui.theme.BasedAppColor
import co.gulf.todvpn.vpn.ui.theme.BasedVPNTheme
import co.gulf.todvpn.vpn.ui.widget.BasedAlertDialog
import co.gulf.todvpn.vpn.ui.widget.BasedButton
import co.gulf.todvpn.vpn.ui.widget.ButtonStyle
import co.gulf.todvpn.vpn.ui.widget.ErrorScreen
import io.norselabs.vpn.based.compose.EffectHandler
import io.norselabs.vpn.based.viewModel.dashboard.DashboardScreenEffect
import io.norselabs.vpn.based.viewModel.dashboard.DashboardScreenEffect as Effect
import io.norselabs.vpn.based.viewModel.dashboard.DashboardScreenState as State
import co.gulf.todvpn.vpn.ui.theme.TextSecondaryColor
import io.norselabs.vpn.based.viewModel.dashboard.DashboardScreenViewModel
import io.norselabs.vpn.based.viewModel.dashboard.NetworkDataUi
import io.norselabs.vpn.based.viewModel.dashboard.RatingClick
import io.norselabs.vpn.based.viewModel.dashboard.VpnStatus
import io.norselabs.vpn.based.vpn.getVpnPermissionRequest
import io.norselabs.vpn.common.ext.goToGooglePlay
import io.norselabs.vpn.common.ext.mailTo
import io.norselabs.vpn.common.state.Status
import io.norselabs.vpn.common_flags.mapToFlag
import io.norselabs.vpn.common_map.WorldMap
import io.norselabs.vpn.core_vpn.user.UserStatus
import io.norselabs.vpn.core_vpn.vpn.Destination
import timber.log.Timber
import androidx.compose.foundation.border
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow


@Composable
fun DashboardScreen(
  navigateToCountries: () -> Unit,
  navigateToSettings: () -> Unit,
) {
  val viewModel = hiltViewModel<DashboardScreenViewModel>()
  val state by viewModel.stateHolder.state.collectAsState()

  val context = LocalContext.current

  val vpnPermissionRequest = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    viewModel.onPermissionsResult(result.resultCode == Activity.RESULT_OK)
  }

  EffectHandler(viewModel.stateHolder.effects) { effect ->
    when (effect) {
      is Effect.ShowAd -> viewModel.onAdShown()

      is Effect.ShowSelectServer -> navigateToCountries()

      is Effect.CheckVpnPermission -> {
        val intent = getVpnPermissionRequest(context)
        if (intent != null) {
          vpnPermissionRequest.launch(intent)
        } else {
          viewModel.onPermissionsResult(true)
        }
      }

      is Effect.ShowSettings -> navigateToSettings()

      is Effect.ShowGooglePlay -> context.goToGooglePlay()

      is Effect.EmailToSupport -> context.mailTo("hello@world.com")

      is Effect.ShowRating -> {
        Timber.tag("DashboardScreenEffect").d("ShowRating")
      }

      is DashboardScreenEffect.ShareLogs -> Unit
    }
  }

  DashboardScreenStateless(
    state = state,
    onConnectClick = viewModel::onConnectClick,
    onDisconnectClick = viewModel::onDisconnectClick,
    onQuickConnectClick = viewModel::onQuickConnectClick,
    onSelectServerClick = viewModel::onSelectServerClick,
    onSettingsClick = viewModel::onSettingsClick,
    onTryAgainClick = viewModel::onTryAgainClick,
    onUpdateClick = viewModel::onUpdateClick,
    onAlertConfirmClick = viewModel::onAlertConfirmClick,
    onAlertDismissRequest = viewModel::onAlertDismissRequest,
    onRatingClick = viewModel::onRatingClick,
  )
}

@Composable
fun DashboardScreenStateless(
  state: State,
  onConnectClick: () -> Unit,
  onDisconnectClick: () -> Unit,
  onQuickConnectClick: () -> Unit,
  onSelectServerClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onTryAgainClick: () -> Unit,
  onUpdateClick: () -> Unit,
  onAlertConfirmClick: () -> Unit,
  onAlertDismissRequest: () -> Unit,
  onRatingClick: (RatingClick) -> Unit,
) {
  when (state.status) {
    is Status.Error -> {
      when (state.userStatus) {
        UserStatus.VersionOutdated -> {
          ErrorScreen(
            title = stringResource(R.string.update_required_title),
            description = stringResource(R.string.update_required_description),
            buttonLabel = stringResource(R.string.update_required_button),
            imageResId = R.drawable.ic_update,
            onButtonClick = onUpdateClick,
          )
        }

        UserStatus.Banned -> {
          ErrorScreen(
            title = null,
            description = stringResource(R.string.error_banned_title),
            onButtonClick = null,
          )
        }

        else -> {
          ErrorScreen(
            isLoading = (state.status as Status.Error).isLoading,
            onButtonClick = onTryAgainClick,
          )
        }
      }
    }

    else -> Content(
      state = state,
      onConnectClick = onConnectClick,
      onDisconnectClick = onDisconnectClick,
      onQuickConnectClick = onQuickConnectClick,
      onSelectServerClick = onSelectServerClick,
      onSettingsClick = onSettingsClick,
      onAlertConfirmClick = onAlertConfirmClick,
      onAlertDismissRequest = onAlertDismissRequest,
      onRatingClick = onRatingClick,
    )
  }
}

@Composable
private fun Content(
  state: State,
  onConnectClick: () -> Unit,
  onDisconnectClick: () -> Unit,
  onQuickConnectClick: () -> Unit,
  onSelectServerClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onAlertConfirmClick: () -> Unit,
  onAlertDismissRequest: () -> Unit,
  onRatingClick: (RatingClick) -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize(),
  ) {
    Column {
      TopBar(
        state = state,
        onSettingsClick = onSettingsClick,
      )
      WorldMap(
        lat = state.networkData?.lat ?: 0.0,
        long = state.networkData?.long ?: 0.0,
        color = when (state.vpnStatus) {
          is VpnStatus.Connected -> BasedAppColor.Background
          else -> BasedAppColor.Background
        },
        modifier = Modifier.weight(1f),
      )
      BottomBar(
        state = state,
        onConnectClick = onConnectClick,
        onDisconnectClick = onDisconnectClick,
        onQuickConnectClick = onQuickConnectClick,
        onSelectServerClick = onSelectServerClick,
      )
    }
    if (state.status is Status.Loading) {
      LoadingOverlay()
    }
    if (state.isErrorAlertVisible) {
      BasedAlertDialog(
        title = stringResource(R.string.dashboard_error_connection_title),
        description = stringResource(R.string.dashboard_error_connection_description),
        onConfirmClick = onAlertConfirmClick,
        onDismissRequest = onAlertDismissRequest,
      )
    }
    if (state.isRatingAlertVisible) {
      BasedAlertDialog(
        title = stringResource(R.string.dashboard_rating),
        onConfirmClick = { onRatingClick(RatingClick.Positive) },
        onDismissClick = { onRatingClick(RatingClick.Negative) },
        onDismissRequest = { onRatingClick(RatingClick.Dismiss) },
      )
    }
  }
}

@Composable
private fun LoadingOverlay() {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {},
      )
      .navigationBarsPadding()
      .background(Color.Black.copy(alpha = 0.5f))
      .fillMaxSize(),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      CircularProgressIndicator(
        color = Color.White,
      )
      Spacer(modifier = Modifier.size(24.dp))
      Text(
        text = stringResource(R.string.dashboard_loading),
        fontSize = 16.sp,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp).padding(horizontal = 48.dp),
      )
    }
  }
}

@Composable
private fun TopBar(
  state: State,
  onSettingsClick: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(
      bottomStart = 16.dp,
      bottomEnd = 16.dp,
    ),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF002147)),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .padding(top = 8.dp, bottom = 24.dp)
        .fillMaxWidth(),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
      ) {
        Text(
          text = stringResource(R.string.dashboard_your_ip).uppercase(),
          color = BasedAppColor.TextPrimary,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = state.networkData?.ip.orEmpty(),
          color = BasedAppColor.TextPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          overflow = TextOverflow.Ellipsis,
          maxLines = 1,
        )
      }
      var isFocused by remember { mutableStateOf(false) }
      Button(
        colors = ButtonDefaults.buttonColors(
          containerColor = BasedAppColor.ButtonSecondary,
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        onClick = onSettingsClick,
        modifier = Modifier
          .size(44.dp)
          .align(Alignment.CenterEnd)
          .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
          }
          .scale(
            animateFloatAsState(
              targetValue = if (isFocused) 1.1f else 1f,
              animationSpec = tween(durationMillis = 300) // Smooth pulse animation
            ).value
          ),
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_settings),
          contentDescription = stringResource(R.string.dashboard_menu_settings),
          modifier = Modifier.size(24.dp),
          tint = BasedAppColor.ButtonTertiaryIcon,
        )
      }
    }
  }
}

@Composable
fun BottomBar(
  state: State,
  onConnectClick: () -> Unit,
  onDisconnectClick: () -> Unit,
  onQuickConnectClick: () -> Unit,
  onSelectServerClick: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(
      topStart = 16.dp,
      topEnd = 16.dp,
    ),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF002147)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .padding(top = 24.dp, bottom = 16.dp)
        .navigationBarsPadding()
      ,
    ) {
      val destination = state.destination as? Destination.City
      if (destination != null) {
        SelectedCityRow(
          destination = destination,
          onClick = onSelectServerClick,
        )
        Spacer(modifier = Modifier.size(16.dp))
      }
      Row {
        var isFocused by remember { mutableStateOf(false) }

        BasedButton(
          text = stringResource(
            when (state.vpnStatus) {
              is VpnStatus.Connected -> R.string.dashboard_disconnect_from_vpn
              else -> R.string.dashboard_connect_to_vpn
            },
          ),
          style = when (state.vpnStatus) {
            is VpnStatus.Connected -> ButtonStyle.Tertiary
            else -> ButtonStyle.Tertiary
          },
          isLoading = when (state.vpnStatus) {
            is VpnStatus.Connecting -> true
            else -> false
          },
          onClick = {
            if (state.vpnStatus is VpnStatus.Disconnected) {
              onConnectClick()
            } else {
              onDisconnectClick()
            }
          },
          modifier = Modifier
            .weight(1f)
            .onFocusChanged { focusState ->
              isFocused = focusState.isFocused
            }
            .scale(
              animateFloatAsState(
                targetValue = if (isFocused) 1f else 0.98f, // Scale size for focus
                animationSpec = tween(durationMillis = 300) // Smooth scaling animation
              ).value
            )
            .border(
              width = if (isFocused) (4.dp * 0.98f) else 0.dp, // Adjust border width dynamically
              brush = Brush.horizontalGradient(
                colors = listOf(
                  Color.Magenta.copy(alpha = 0.8f),
                  Color.Cyan.copy(alpha = 0.8f)
                ) // Gradient colors with transparency
              ),
              shape = RoundedCornerShape(
                size = animateFloatAsState(
                  targetValue = if (isFocused) 12.dp.value else 8.dp.value * 0.98f, // Adjust corner size dynamically
                  animationSpec = tween(durationMillis = 300) // Smooth animation for corners
                ).value.dp
              )
            )
            .padding(
              animateFloatAsState(
                targetValue = if (isFocused) 6.dp.value else 4.dp.value * 0.98f, // Adjust padding dynamically
                animationSpec = tween(durationMillis = 300)
              ).value.dp
            )

        )
        Spacer(modifier = Modifier.size(8.dp))
        if (state.vpnStatus == VpnStatus.Disconnected) {
          QuickConnectButton(onQuickConnectClick)
        }
      }
    }
  }
}

@Composable
private fun QuickConnectButton(onClick: () -> Unit) {
  var isFocused by remember { mutableStateOf(false) }

  Button(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(
      containerColor = BasedAppColor.ButtonSecondary,
      contentColor = BasedAppColor.ButtonPrimaryText,
    ),
    shape = RoundedCornerShape(8.dp),
    contentPadding = PaddingValues(),
    modifier = Modifier
      .size(60.dp)
      .onFocusChanged { focusState ->
        isFocused = focusState.isFocused
      }
      .scale(
        animateFloatAsState(
          targetValue = if (isFocused) 1f else 0.97f,
          animationSpec = tween(durationMillis = 300) // Smooth pulse animation
        ).value
      ),
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_flash),
      contentDescription = null,
      modifier = Modifier.size(32.dp),
    )
  }
}

@Composable
fun SelectedCityRow(
  destination: Destination.City,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .heightIn(min = 60.dp)
      .background(BasedAppColor.Elevation)
      .padding(16.dp)
      .fillMaxWidth(),
  ) {
    val flagRes = mapToFlag(destination.countryCode)?.res
    if (flagRes != null) {
      Image(
        painter = painterResource(flagRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(width = 36.dp, height = 24.dp)
          .clip(RoundedCornerShape(4.dp)),
      )
    } else {
      Box(
        modifier = Modifier
          .background(BasedAppColor.Divider)
          .size(width = 36.dp, height = 24.dp)
          .clip(RoundedCornerShape(4.dp)),
      )
    }
    Spacer(modifier = Modifier.size(16.dp))
    Text(
      text = buildAnnotatedString {
        withStyle(style = SpanStyle(BasedAppColor.TextSecondary)) {
          append(destination.countryName)
        }
        withStyle(style = SpanStyle(BasedAppColor.TextSecondary)) {
          append(" • ")
          append(destination.cityName)
        }
      },
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      fontSize = 18.sp,
    )
  }
}


@Preview
@Composable
fun DashboardScreenPreview() {
  BasedVPNTheme {
    DashboardScreenStateless(
      state = State(
        destination = Destination.City(
          cityId = "",
          cityName = "Manama",
          countryId = "",
          countryName = "Bahrain",
          countryCode = "BH",
        ),
        networkData = NetworkDataUi(
          ip = "91.208.132.23",
          lat = 0.0,
          long = 0.0,
        ),
      ),
      onConnectClick = {},
      onDisconnectClick = {},
      onQuickConnectClick = {},
      onSelectServerClick = {},
      onSettingsClick = {},
      onTryAgainClick = {},
      onUpdateClick = {},
      onAlertConfirmClick = {},
      onAlertDismissRequest = {},
      onRatingClick = {},
    )
  }
}
