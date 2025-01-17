package io.norselabs.vpn.core_vpn.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class NetworkStateMonitor(context: Context) {

  private val connectivityManager by lazy {
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  }

  private val _networkState = MutableSharedFlow<NetworkState>(
    replay = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )
  val networkState: SharedFlow<NetworkState> get() = _networkState

  private val callback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
      val isVpn = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
      _networkState.tryEmit(NetworkState.Connected(isVpn))
    }

    override fun onLost(network: Network) {
      _networkState.tryEmit(NetworkState.Disconnected)
    }
  }

  fun startMonitoring() {
    connectivityManager.registerDefaultNetworkCallback(callback)
  }

  fun stopMonitoring() {
    connectivityManager.unregisterNetworkCallback(callback)
  }
}

sealed interface NetworkState {
  data class Connected(val isVpn: Boolean) : NetworkState
  data object Disconnected : NetworkState
}
