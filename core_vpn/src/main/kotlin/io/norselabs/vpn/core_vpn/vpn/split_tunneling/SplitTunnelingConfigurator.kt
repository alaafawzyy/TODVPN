package io.norselabs.vpn.core_vpn.vpn.split_tunneling

import io.norselabs.vpn.v2ray.repo.V2RayRepository

class SplitTunnelingConfigurator(
  private val repo: V2RayRepository,
) {

  fun getStatus(): SplitTunnelingStatus {
    return when {
      !repo.isPerAppProxyEnabled() -> SplitTunnelingStatus.Disabled
      repo.isBypassEnabled() -> SplitTunnelingStatus.Bypass
      else -> SplitTunnelingStatus.Enabled
    }
  }

  fun setStatus(status: SplitTunnelingStatus) {
    repo.enablePerAppProxy(status != SplitTunnelingStatus.Disabled)
    repo.enableBypass(status == SplitTunnelingStatus.Bypass)
  }

  fun getApps(): Set<String> {
    return repo.getPerAppProxySet()
  }

  fun setApps(apps: Set<String>) {
    repo.setPerAppProxySet(apps)
  }
}
