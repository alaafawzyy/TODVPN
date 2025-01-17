package io.norselabs.vpn.core_vpn.vpn.utils

import io.norselabs.vpn.core_vpn.vpn.Protocol

fun Protocol.format(): String? {
  return when (this) {
    Protocol.WIREGUARD -> "WireGuard"
    Protocol.V2RAY -> "V2Ray"
    Protocol.NONE -> null
  }
}
