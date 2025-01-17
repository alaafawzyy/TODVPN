package co.gulf.todvpn.vpn

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.norselabs.vpn.based.vpn.VpnInitializer
import io.norselabs.vpn.common_logger.logger.FileLogTree
import io.norselabs.vpn.common_logger.logger.NonFatalReportTree
import io.norselabs.vpn.core_vpn.connectivity.NetworkStateMonitor
import io.norselabs.vpn.core_vpn.user.UserInitializer
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class App : Application() {

  @Inject
  lateinit var vpnInitializer: VpnInitializer

  @Inject
  lateinit var fileLogTree: FileLogTree

  @Inject
  lateinit var nonFatalReportTree: NonFatalReportTree

  @Inject
  lateinit var userInitializer: UserInitializer

  @Inject
  lateinit var networkMonitor: NetworkStateMonitor

  override fun onCreate() {
    super.onCreate()
    setupTimber()
    networkMonitor.startMonitoring()
    vpnInitializer.setupVPN()
    userInitializer.enroll()
  }

  private fun setupTimber() {
    Timber.plant(
      Timber.DebugTree(),
      fileLogTree,
      nonFatalReportTree,
    )
  }
}
