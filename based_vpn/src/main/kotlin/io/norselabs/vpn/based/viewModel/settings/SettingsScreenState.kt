package io.norselabs.vpn.based.viewModel.settings

import io.norselabs.vpn.based.app_config.AppConfig
import io.norselabs.vpn.based.viewModel.settings.dto.AppLang
import io.norselabs.vpn.based.vpn.DdsConfigurator
import io.norselabs.vpn.common.state.ViewStateHolder
import io.norselabs.vpn.core_vpn.vpn.Protocol
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class SettingsScreenStateHolder
@Inject constructor(
  config: AppConfig,
) : ViewStateHolder<SettingsScreenState, SettingsScreenEffect>(
  SettingsScreenState(
    appVersion = config.getAppVersion(),
  ),
)

data class SettingsScreenState(
  val currentDns: DdsConfigurator.Dns? = null,
  val dnsOptions: ImmutableList<DdsConfigurator.Dns> = persistentListOf(
    DdsConfigurator.Dns.Cloudflare,
    DdsConfigurator.Dns.Google,
    DdsConfigurator.Dns.Handshake,
  ),
  val isDnsSelectorVisible: Boolean = false,

  val currentProtocol: Protocol? = null,
  val protocolOptions: ImmutableList<Protocol> = persistentListOf(
    Protocol.WIREGUARD,
    Protocol.V2RAY,
    Protocol.NONE,
  ),
  val isProtocolSelectorVisible: Boolean = false,

  val currentLang: AppLang? = null,
  val langOptions: ImmutableList<AppLang> = persistentListOf(),

  val appVersion: String = "",
)

sealed interface SettingsScreenEffect {
  data object OpenTelegram : SettingsScreenEffect
  data object SplitTunneling : SettingsScreenEffect
  data object ShareLogs : SettingsScreenEffect
}
