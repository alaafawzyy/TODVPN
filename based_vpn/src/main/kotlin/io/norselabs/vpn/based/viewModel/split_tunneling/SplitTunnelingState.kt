package io.norselabs.vpn.based.viewModel.split_tunneling

import android.graphics.drawable.Drawable
import io.norselabs.vpn.common.state.ViewStateHolder
import io.norselabs.vpn.core_vpn.vpn.split_tunneling.SplitTunnelingConfigurator
import io.norselabs.vpn.core_vpn.vpn.split_tunneling.SplitTunnelingStatus
import javax.inject.Inject

class SplitTunnelingScreenStateHolder
@Inject constructor(
  splitTunneling: SplitTunnelingConfigurator,
) : ViewStateHolder<SplitTunnelingScreenState, SplitTunnelingScreenEffect>(
  SplitTunnelingScreenState(
    status = splitTunneling.getStatus(),
  ),
)

data class SplitTunnelingScreenState(
  val status: SplitTunnelingStatus,
  val applications: List<NetworkApp> = emptyList(),
)

data class NetworkApp(
  val appName: String,
  val packageName: String,
  val appIcon: Drawable,
  val isSystemApp: Boolean,
  val isChecked: Boolean,
)

sealed interface SplitTunnelingScreenEffect
