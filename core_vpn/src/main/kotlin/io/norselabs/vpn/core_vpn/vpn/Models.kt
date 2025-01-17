package io.norselabs.vpn.core_vpn.vpn

import com.google.gson.annotations.SerializedName

sealed interface Credentials {

  data class V2Ray(
    val payload: String,
    val uid: String,
    override val serverId: String?,
  ) : Credentials

  data class Wireguard(
    val payload: String,
    val privateKey: String,
    override val serverId: String?,
  ) : Credentials

  val serverId: String?
}

enum class Protocol(val strValue: String) {
  WIREGUARD("WIREGUARD"),
  V2RAY("V2RAY"),
  NONE("NONE"),
  ;

  companion object {
    fun fromString(strValue: String) = entries.firstOrNull {
      it.strValue.equals(other = strValue, ignoreCase = true)
    } ?: NONE
  }
}

sealed interface Destination {

  data class City(
    @SerializedName("cityId")
    val cityId: String,
    @SerializedName("cityName")
    val cityName: String,
    @SerializedName("countryId")
    val countryId: String,
    @SerializedName("countryName")
    val countryName: String,
    @SerializedName("countryCode")
    val countryCode: String?,
  ) : Destination {
    override val isPersistable: Boolean = true
  }

  data class Server(
    @SerializedName("serverId")
    val serverId: String,
    @SerializedName("serverName")
    val serverName: String,
    @SerializedName("cityId")
    val cityId: String,
    @SerializedName("cityName")
    val cityName: String,
    @SerializedName("countryId")
    val countryId: String,
    @SerializedName("countryName")
    val countryName: String,
    @SerializedName("countryCode")
    val countryCode: String,
  ) : Destination {
    override val isPersistable: Boolean = true
  }

  data class Deeplink(
    val url: String,
  ) : Destination {
    override val isPersistable: Boolean = false
  }

  val isPersistable: Boolean
}
