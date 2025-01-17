package io.norselabs.vpn.core_vpn.vpn.connector

import android.util.Base64
import arrow.core.Either
import arrow.core.flatMap
import com.google.gson.Gson
import io.norselabs.vpn.core_vpn.storage.CoreStorage
import io.norselabs.vpn.core_vpn.vpn.Credentials
import io.norselabs.vpn.core_vpn.vpn.Destination
import io.norselabs.vpn.core_vpn.vpn.Protocol
import timber.log.Timber

class VPNConnector(
  private val gson: Gson,
  private val coreStorage: CoreStorage,
  private val interactor: VPNConnectorInteractor,
) {

  suspend fun connect(destination: Destination): Either<Error, Unit> {
    if (interactor.isVpnConnected()) disconnect()
    return when (destination) {
      is Destination.Deeplink -> parseDeeplink(destination)
      else -> getCredentials(destination)
    }.flatMap { connectVpn(it) }
  }

  fun disconnect() {
    interactor.stopVpn()
    coreStorage.setCurrentServerId("")
  }

  private suspend fun getCredentials(destination: Destination): Either<Error, Credentials> {
    val protocol = coreStorage.getVpnProtocol()
      .takeIf { it != Protocol.NONE }
    return interactor.getCredentials(
      destination = destination,
      protocol = protocol,
    )
      .mapLeft { parseError(it) }
  }

  private fun parseDeeplink(deeplink: Destination.Deeplink): Either<Error, Credentials> {
    return deeplink.url.split('/')
      .lastOrNull { it.isNotBlank() }
      ?.let { payload -> Either.Right(payload) }
      ?.map { Base64.decode(it, Base64.DEFAULT).decodeToString() }
      ?.flatMap(::parseCredentials)
      ?: Either.Left(Error.ParseCredentials)
  }

  private fun parseCredentials(payloadJson: String): Either<Error, Credentials> {
    return try {
      val isV2ray = payloadJson.contains("uid")
      val clazz = when {
        isV2ray -> Credentials.V2Ray::class.java
        else -> Credentials.Wireguard::class.java
      }
      val credentials = gson.fromJson(payloadJson, clazz)
      Either.Right(credentials)
    } catch (e: Exception) {
      Timber.tag(TAG).d("Parsing creation failed: $e")
      Either.Left(Error.ParseCredentials)
    }
  }

  private fun parseError(exception: Exception): Error {
    Timber.tag(TAG).d("Credentials creation failed: $exception")
    val code = interactor.parseHttpCode(exception)
    return when (code) {
      401, 403, 425 -> Error.UserToken
      else -> Error.GetCredentials(exception)
    }
  }

  private suspend fun connectVpn(credentials: Credentials): Either<Error, Unit> {
    interactor.startVpn(credentials).getOrNull()
      ?: return Either.Left(Error.StartV2Ray)

    coreStorage.setCurrentServerId(credentials.serverId)

    interactor.resetConnection()

    return Either.Right(Unit)
  }

  sealed interface Error {
    data class GetCredentials(val exception: Exception) : Error
    data object ParseCredentials : Error
    data object UserToken : Error
    data object StartV2Ray : Error
  }

  companion object {
    const val TAG = "VPNConnector"
  }
}
