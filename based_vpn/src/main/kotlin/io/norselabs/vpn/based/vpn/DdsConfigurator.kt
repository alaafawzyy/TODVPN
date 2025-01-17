package io.norselabs.vpn.based.vpn

import io.norselabs.vpn.v2ray.repo.V2RayRepository
import javax.inject.Inject

class DdsConfigurator @Inject constructor(
  private val v2Ray: V2RayRepository,
) {

  fun getDefaultDns(): Dns {
    val currentDns = v2Ray.getDns().joinToString(",")
    val selectedDns = Dns.entries.firstOrNull { it.address == currentDns }
    if (selectedDns == null) {
      setDns(Dns.Cloudflare)
    }
    return selectedDns ?: Dns.Cloudflare
  }

  fun setDns(dns: Dns) {
    v2Ray.updateDns(dns.address)
  }

  enum class Dns(
    val address: String,
  ) {
    Cloudflare("1.1.1.1,1.0.0.1"),
    Google("8.8.8.8,8.8.4.4"),
    Handshake("103.196.38.38,103.196.38.39,103.196.38.40"),
  }
}
