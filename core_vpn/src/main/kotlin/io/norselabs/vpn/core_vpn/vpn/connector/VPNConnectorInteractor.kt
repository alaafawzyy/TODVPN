package io.norselabs.vpn.core_vpn.vpn.connector

import arrow.core.Either
import io.norselabs.vpn.core_vpn.vpn.Credentials
import io.norselabs.vpn.core_vpn.vpn.Destination
import io.norselabs.vpn.core_vpn.vpn.Protocol
import io.norselabs.vpn.v2ray.error.V2RayError

interface VPNConnectorInteractor {

  // Repository
  suspend fun getCredentials(
    destination: Destination,
    protocol: Protocol?,
  ): Either<Exception, Credentials>

  suspend fun resetConnection()

  fun parseHttpCode(exception: Exception): Int?

  // VPN
  suspend fun startVpn(credentials: Credentials): Either<V2RayError, Unit>
  fun isVpnConnected(): Boolean
  fun stopVpn()
}
