package io.norselabs.vpn.based.vpn

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.norselabs.vpn.common_logger.logNonFatal
import io.norselabs.vpn.v2ray.control.V2RayInitializer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class VpnInitializer @Inject constructor(
  @ApplicationContext private val context: Context,
) {

  private val _status = MutableStateFlow(Status.None)
  val status: StateFlow<Status> = _status

  fun setupVPN() {
    V2RayInitializer.init(context)
      .onRight { _status.value = Status.Initialized }
      .onLeft {
        _status.value = Status.NotSupported
        logNonFatal("Cannot initialize v2ray library", it)
      }
  }

  enum class Status {
    None,
    Initialized,
    NotSupported,
  }
}
