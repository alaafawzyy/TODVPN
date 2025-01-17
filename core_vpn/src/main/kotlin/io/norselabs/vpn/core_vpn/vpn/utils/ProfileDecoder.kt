package io.norselabs.vpn.core_vpn.vpn.utils

import android.util.Base64
import io.norselabs.vpn.core_vpn.vpn.Credentials
import io.norselabs.vpn.v2ray.model.VpnProfile

object ProfileDecoder {

  fun decode(credentials: Credentials): VpnProfile? {
    return when (credentials) {
      is Credentials.Wireguard -> decodeWireguard(
        privateKey = credentials.privateKey,
        payload = credentials.payload,
      )

      is Credentials.V2Ray -> decodeVmess(
        payload = credentials.payload,
        uid = credentials.uid,
      )
    }
  }

  private fun decodeVmess(
    payload: String,
    uid: String,
  ): VpnProfile? {
    return try {
      Base64.decode(payload, Base64.DEFAULT).let { bytes ->
        if (bytes.size != 7) return null
        val address = "${bytes[0].toUByte()}.${bytes[1].toUByte()}.${bytes[2].toUByte()}.${bytes[3].toUByte()}"
        val port = bytesToUnsignedShort(bytes[4], bytes[5]).toString()
        val hex = String.format("0x%02x", (bytes[6].toInt() and 0xFF))
        val transport: String = when (hex) {
          "0x01" -> "tcp"
          "0x02" -> "mkcp"
          "0x03" -> "websocket"
          "0x04" -> "http"
          "0x05" -> "domainsocket"
          "0x06" -> "quic"
          "0x07" -> "gun"
          "0x08" -> "grpc"
          else -> ""
        }

        VpnProfile.Vmess(
          uid = uid,
          address = address,
          listenPort = port,
          transport = transport,
        )
      }
    } catch (e: Exception) {
      null
    }
  }

  private fun decodeWireguard(
    privateKey: String,
    payload: String,
  ): VpnProfile? {
    return try {
      Base64.decode(payload, Base64.DEFAULT).let { bytes ->
        val address = "${bytes[0].toUByte()}.${bytes[1].toUByte()}.${bytes[2].toUByte()}.${bytes[3].toUByte()}/32"
        val port = bytesToUnsignedShort(bytes[24], bytes[25]).toString()
        val host = "${bytes[20].toUByte()}.${bytes[21].toUByte()}.${bytes[22].toUByte()}.${bytes[23].toUByte()}"
        val pubKeyBytes = bytes.copyOfRange(26, 58)
        val peerPubKeyBase64 = String(Base64.encode(pubKeyBytes, Base64.DEFAULT)).trim()

        VpnProfile.Wireguard(
          privateKey = privateKey,
          address = address,
          host = host,
          listenPort = port,
          peerPubKeyBase64 = peerPubKeyBase64,
        )
      }
    } catch (e: Exception) {
      null
    }
  }

  private fun bytesToUnsignedShort(
    byte1: Byte,
    byte2: Byte,
  ): Int {
    return (((byte1.toInt() and 255) shl 8) or (byte2.toInt() and 255))
  }
}
