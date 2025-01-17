package io.norselabs.vpn.based.viewModel.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.flatMap
import dagger.hilt.android.lifecycle.HiltViewModel
import io.norselabs.vpn.based.storage.AppStorage
import io.norselabs.vpn.based.storage.RatingStatus
import io.norselabs.vpn.based.viewModel.dashboard.DashboardScreenEffect as Effect
import io.norselabs.vpn.common.state.Status
import io.norselabs.vpn.common_network.AppRepository
import io.norselabs.vpn.common_network.models.CitiesRequest
import io.norselabs.vpn.common_network.models.NetworkData
import io.norselabs.vpn.core_vpn.connectivity.NetworkState
import io.norselabs.vpn.core_vpn.connectivity.NetworkStateMonitor
import io.norselabs.vpn.core_vpn.storage.CoreStorage
import io.norselabs.vpn.core_vpn.user.UserInitializer
import io.norselabs.vpn.core_vpn.user.UserStatus
import io.norselabs.vpn.core_vpn.vpn.Destination
import io.norselabs.vpn.core_vpn.vpn.Protocol
import io.norselabs.vpn.core_vpn.vpn.connector.VPNConnector
import io.norselabs.vpn.core_vpn.vpn.destination.DestinationStorage
import io.norselabs.vpn.v2ray.repo.V2RayRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber

@HiltViewModel
class DashboardScreenViewModel
@Inject constructor(
  val stateHolder: DashboardScreenStateHolder,
  private val repository: AppRepository,
  private val appStorage: AppStorage,
  private val coreStorage: CoreStorage,
  private val connector: VPNConnector,
  private val vpnRepo: V2RayRepository,
  private val userInitializer: UserInitializer,
  private val destinationStorage: DestinationStorage,
  private val networkMonitor: NetworkStateMonitor,
) : ViewModel() {

  private var connectJob: Job? = null

  private val state: DashboardScreenState
    get() = stateHolder.state.value

  init {
    observeNetworkState()
    observeConnectionState()
    observeUserStatus()
    observeDestination()
  }

  private fun observeNetworkState() {
    viewModelScope.launch {
      networkMonitor.networkState.collect { state ->
        Timber.tag(TAG).d("Network State: $state")
        if (state is NetworkState.Connected) {
          updateNetworkInfo(true)
        }
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun observeConnectionState() {
    viewModelScope.launch {
      vpnRepo.isConnected
        .flatMapLatest { isConnected ->
          if (isConnected) {
            val isQuick = state.vpnStatus.isQuick()
            setVpnStatus(VpnStatus.Connecting(isQuick))
            if (checkConnection()) {
              flowOf(true)
            } else {
              stopVpn()
              flowOf(false)
            }
          } else {
            flowOf(false)
          }
        }
        .collect { isConnected ->
          val isQuick = state.vpnStatus.isQuick()
          setVpnStatus(
            when (isConnected) {
              true -> VpnStatus.Connected(isQuick)
              false -> VpnStatus.Disconnected
            },
          )
        }
    }
  }

  private fun observeUserStatus() {
    viewModelScope.launch {
      userInitializer.status.collect { userStatus ->
        Timber.tag(TAG).d("User Status: $userStatus")
        stateHolder.updateState { copy(userStatus = userStatus) }
        val screenStatus = when (userStatus) {
          UserStatus.Enrolled,
          UserStatus.Determining,
          UserStatus.HasToken,
          UserStatus.Enrolling,
          -> Status.Data

          else -> Status.Error()
        }
        stateHolder.updateState { copy(status = screenStatus) }
      }
    }
  }

  private fun observeDestination() {
    viewModelScope.launch {
      destinationStorage.observe().collect { destination ->
        val previousDestination = state.destination
        stateHolder.updateState { copy(destination = destination) }
        if (previousDestination != null && previousDestination != destination) {
          connectJob?.cancelAndJoin()
          connector.disconnect()
          val isQuick = state.vpnStatus.isQuick()
          initConnection(isQuick)
        }
      }
    }
  }

  private fun setVpnStatus(vpnStatus: VpnStatus) {
    Timber.tag(TAG).d("Set VPN Status: $vpnStatus")
    stateHolder.updateState {
      copy(vpnStatus = vpnStatus)
    }
  }

  private suspend fun checkConnection(): Boolean {
    repeat(MAX_ATTEMPTS) {
      val isConnected = try {
        withTimeout(3000) {
          repository.checkConnection()
            .isRight()
            .also { Timber.tag(TAG).d("Connection check: $it") }
        }
      } catch (e: Exception) {
        false
      }
      if (isConnected) return true
    }
    return false
  }

  private suspend fun updateNetworkInfo(isNetworkChanged: Boolean) {
    userInitializer.hasToken().firstOrNull { it }
    stateHolder.updateState { copy(retryAttempt = state.retryAttempt + 1) }
    repository.getIpData()
      .flatMap { res ->
        val currentIp = state.networkData?.ip
        if (isNetworkChanged && currentIp == res.data.ip) {
          Either.Left(Unit)
        } else {
          Either.Right(res.data)
        }
      }
      .onRight(::parseNetworkInfo)
      .onLeft { handleNetworkInfoError(isNetworkChanged) }
  }

  private fun parseNetworkInfo(networkData: NetworkData) {
    stateHolder.updateState {
      copy(
        networkData = NetworkDataUi(
          ip = networkData.ip,
          lat = networkData.info.lat,
          long = networkData.info.long,
        ),
        retryAttempt = 0,
      )
    }
  }

  private suspend fun handleNetworkInfoError(isNetworkChanged: Boolean) {
    if (state.retryAttempt < MAX_ATTEMPTS) {
      delay(1.seconds)
      updateNetworkInfo(isNetworkChanged)
    } else {
      stateHolder.updateState { copy(retryAttempt = 0) }
      displayConnectionError()
    }
  }

  private fun displayConnectionError() {
    // todo
  }

  fun onConnectClick() {
    if (state.vpnStatus is VpnStatus.Disconnected) {
      Timber.tag(TAG).d("Connect clicked")
      initConnection(isQuick = false)
    }
  }

  fun onQuickConnectClick() {
    if (state.vpnStatus is VpnStatus.Disconnected) {
      Timber.tag(TAG).d("Quick Connect clicked")
      viewModelScope.launch {
        stateHolder.updateState { copy(isDestinationLoading = true) }
        selectRandomDestination()
        stateHolder.updateState { copy(isDestinationLoading = false) }
        initConnection(isQuick = true)
      }
    }
  }

  fun onDisconnectClick() {
    if (state.vpnStatus !is VpnStatus.Disconnected) {
      Timber.tag(TAG).d("Disconnect clicked")
      stopVpn()
    }
  }

  private fun initConnection(isQuick: Boolean) {
    if (state.userStatus != UserStatus.Enrolled) {
      // todo: show error
      return
    }
    if (state.destination == null) {
      onSelectServerClick()
      return
    }
    setVpnStatus(VpnStatus.Connecting(isQuick))
    stateHolder.sendEffect(Effect.CheckVpnPermission)
  }

  fun onPermissionsResult(isSuccess: Boolean) {
    if (isSuccess) {
      stateHolder.sendEffect(Effect.ShowAd)
    } else {
      stateHolder.updateState { copy(vpnStatus = VpnStatus.Disconnected) }
    }
  }

  fun onAdShown() {
    connectVpn()
  }

  private fun connectVpn() {
    connectJob = viewModelScope.launch {
      val destination = state.destination ?: return@launch
      connector.connect(destination)
        .onLeft { setVpnStatus(VpnStatus.Disconnected) }
        .onRight { checkAppRatingRequest() }
    }
  }

  private suspend fun selectRandomDestination() {
    val protocol = coreStorage.getVpnProtocol().takeIf { it != Protocol.NONE }
    val countries = repository.getCountries(protocol = protocol?.strValue, isFresh = false).getOrNull()?.data
    val country = countries?.randomOrNull() ?: return
    val cities = repository.getCities(
      request = CitiesRequest(country.id, protocol?.strValue),
      isFresh = false,
    ).getOrNull()?.data
    val city = cities?.randomOrNull() ?: return
    destinationStorage.storeDestination(
      Destination.City(
        cityId = city.id,
        cityName = city.name,
        countryId = country.id,
        countryName = country.name,
        countryCode = country.code,
      ),
    )
  }

  private fun stopVpn() {
    connectJob?.cancel()
    connector.disconnect()
    setVpnStatus(VpnStatus.Disconnected)
    if (state.destination is Destination.Deeplink) {
      stateHolder.updateState {
        copy(destination = destinationStorage.getLast())
      }
    }
  }

  fun onTryAgainClick() {
    stateHolder.updateState {
      copy(status = Status.Error(isLoading = true))
    }
    userInitializer.enroll()
  }

  fun onSelectServerClick() {
    stateHolder.sendEffect(Effect.ShowSelectServer)
  }

  fun onSettingsClick() {
    stateHolder.sendEffect(Effect.ShowSettings)
  }

  fun onUpdateClick() {
    stateHolder.sendEffect(Effect.ShowGooglePlay)
  }

  fun onShareLogsClick() {
    stateHolder.sendEffect(Effect.ShareLogs)
  }

  fun onAlertConfirmClick() {
    stateHolder.updateState { copy(isErrorAlertVisible = false) }
  }

  fun onAlertDismissRequest() {
    stateHolder.updateState { copy(isErrorAlertVisible = false) }
  }

  private fun checkAppRatingRequest() {
    val rating = appStorage.getRatingStatus()
    when (rating) {
      RatingStatus.New -> {
        appStorage.setRatingStatus(RatingStatus.RequestOnNext)
      }

      RatingStatus.RequestOnNext -> {
        stateHolder.updateState { copy(isRatingAlertVisible = true) }
      }

      RatingStatus.Requested -> Unit
    }
  }

  fun onRatingClick(click: RatingClick) {
    when (click) {
      RatingClick.Positive -> {
        stateHolder.sendEffect(Effect.ShowRating)
        appStorage.setRatingStatus(RatingStatus.Requested)
      }

      RatingClick.Negative -> {
        stateHolder.sendEffect(Effect.EmailToSupport)
        appStorage.setRatingStatus(RatingStatus.Requested)
      }

      RatingClick.Dismiss -> {
        appStorage.setRatingStatus(RatingStatus.New)
      }
    }
    stateHolder.updateState { copy(isRatingAlertVisible = false) }
  }

  companion object {
    const val TAG = "Dashboard"
    const val MAX_ATTEMPTS = 3
  }
}
