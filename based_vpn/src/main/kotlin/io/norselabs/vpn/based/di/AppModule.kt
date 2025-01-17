package io.norselabs.vpn.based.di

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.norselabs.vpn.based.app_config.AppConfig
import io.norselabs.vpn.based.core_impl.user.UserInitializerInteractorImpl
import io.norselabs.vpn.based.core_impl.vpn.VPNConnectorInteractorImpl
import io.norselabs.vpn.based.network.DnsRequests
import io.norselabs.vpn.common_network.AppRepository
import io.norselabs.vpn.core_vpn.connectivity.NetworkStateMonitor
import io.norselabs.vpn.core_vpn.storage.CoreStorage
import io.norselabs.vpn.core_vpn.user.UserInitializer
import io.norselabs.vpn.core_vpn.user.UserInitializerInteractor
import io.norselabs.vpn.core_vpn.vpn.connector.VPNConnector
import io.norselabs.vpn.core_vpn.vpn.connector.VPNConnectorInteractor
import io.norselabs.vpn.core_vpn.vpn.destination.DestinationStorage
import io.norselabs.vpn.core_vpn.vpn.split_tunneling.SplitTunnelingConfigurator
import io.norselabs.vpn.v2ray.repo.V2RayRepository
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

  @Provides
  @Singleton
  fun provideUserInitializerInteractor(
    repository: AppRepository,
    config: AppConfig,
    dnsRequests: DnsRequests,
  ): UserInitializerInteractor {
    return UserInitializerInteractorImpl(repository, config, dnsRequests)
  }

  @Provides
  @Singleton
  fun provideUserInitializer(
    interactor: UserInitializerInteractor,
    coreStorage: CoreStorage,
  ): UserInitializer = UserInitializer(
    scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    interactor = interactor,
    coreStorage = coreStorage,
  )

  @Provides
  @Singleton
  fun provideVPNConnectorInteractor(
    repository: AppRepository,
    v2RayRepository: V2RayRepository,
  ): VPNConnectorInteractor {
    return VPNConnectorInteractorImpl(
      repository = repository,
      v2RayRepository = v2RayRepository,
    )
  }

  @Provides
  @Singleton
  fun provideVPNConnector(
    gson: Gson,
    coreStorage: CoreStorage,
    interactor: VPNConnectorInteractor,
  ): VPNConnector {
    return VPNConnector(
      gson = gson,
      coreStorage = coreStorage,
      interactor = interactor,
    )
  }

  @Provides
  @Singleton
  fun provideDestinationKeeper(
    gson: Gson,
    prefs: SharedPreferences,
  ): DestinationStorage {
    return DestinationStorage(gson, prefs)
  }

  @Provides
  @Singleton
  fun provideSplitTunnelingConfigurator(
    v2RayRepository: V2RayRepository,
  ): SplitTunnelingConfigurator {
    return SplitTunnelingConfigurator(v2RayRepository)
  }

  @Provides
  @Singleton
  fun provideNetworkStateMonitor(
    @ApplicationContext context: Context,
  ): NetworkStateMonitor {
    return NetworkStateMonitor(context)
  }
}
