package io.norselabs.vpn.based.viewModel.dashboard

import io.norselabs.vpn.common.state.Status
import io.norselabs.vpn.common.state.ViewStateHolder
import io.norselabs.vpn.core_vpn.user.UserStatus
import io.norselabs.vpn.core_vpn.vpn.Destination
import javax.inject.Inject

class DashboardScreenStateHolder
@Inject constructor() : ViewStateHolder<DashboardScreenState, DashboardScreenEffect>(
  DashboardScreenState(),
)

data class DashboardScreenState(
  val status: Status = Status.Data,
  val vpnStatus: VpnStatus = VpnStatus.Disconnected,
  val userStatus: UserStatus = UserStatus.Determining,
  val networkData: NetworkDataUi? = null,
  val destination: Destination? = null,
  val retryAttempt: Int = 0,
  val isDestinationLoading: Boolean = false,
  val isErrorAlertVisible: Boolean = false,
  val isRatingAlertVisible: Boolean = false,
)

data class NetworkDataUi(
  val ip: String,
  val lat: Double,
  val long: Double,
)

sealed interface DashboardScreenEffect {
  data object ShowAd : DashboardScreenEffect
  data object CheckVpnPermission : DashboardScreenEffect
  data object ShowSelectServer : DashboardScreenEffect
  data object ShowSettings : DashboardScreenEffect
  data object ShowGooglePlay : DashboardScreenEffect
  data object ShowRating : DashboardScreenEffect
  data object EmailToSupport : DashboardScreenEffect
  data object ShareLogs : DashboardScreenEffect
}

sealed interface VpnStatus {
  data object Disconnected : VpnStatus
  data class Connecting(override val isQuick: Boolean) : VpnStatus, Quickable
  data class Connected(override val isQuick: Boolean) : VpnStatus, Quickable
}

interface Quickable {
  val isQuick: Boolean
}

enum class RatingClick {
  Positive,
  Negative,
  Dismiss,
}

fun VpnStatus.isQuick(): Boolean {
  return (this as? Quickable)?.isQuick == true
}
