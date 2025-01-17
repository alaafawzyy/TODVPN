package io.norselabs.vpn.common_network.models

import com.google.gson.annotations.SerializedName

data class DataObj<T>(
  @SerializedName("data")
  val data: T,
)

data class DataList<T>(
  @SerializedName("data")
  val data: List<T>,
)

data class TokenModel(
  @SerializedName("id")
  val id: String,
  @SerializedName("token")
  val token: String,
  @SerializedName("is_banned")
  val isBanned: Boolean,
  @SerializedName("is_enrolled")
  val isEnrolled: Boolean,
)

data class Country(
  @SerializedName("id")
  val id: String,
  @SerializedName("name")
  val name: String,
  @SerializedName("code")
  val code: String,
  @SerializedName("servers_available")
  val serversAvailable: Int,
)

data class City(
  @SerializedName("id")
  val id: String,
  @SerializedName("country_id")
  val countryId: String,
  @SerializedName("name")
  val name: String,
  @SerializedName("servers_available")
  val serversAvailable: Int,
)

data class NetworkData(
  @SerializedName("ip")
  val ip: String,
  @SerializedName("information")
  val info: NetworkInfo,
)

data class NetworkInfo(
  @SerializedName("latitude")
  val lat: Double,
  @SerializedName("longitude")
  val long: Double,
  @SerializedName("country_code")
  val countryCode: String,
  @SerializedName("country")
  val country: String,
  @SerializedName("city")
  val city: String,
  @SerializedName("isp")
  val provider: String,
)

data class CitiesRequest(
  @SerializedName("countryId")
  val countryId: String,
  @SerializedName("protocol")
  val protocol: String?,
)

data class RegisterDeviceRequest(
  @SerializedName("app_token")
  val appToken: String,
  @SerializedName("platform")
  val platform: String = "ANDROID",
)

data class CreateCredentials(
  @SerializedName("protocol")
  val protocol: String,
)

data class CredentialsResponse(
  @SerializedName("protocol")
  val protocol: String,
  @SerializedName("payload")
  val payload: String,
  @SerializedName(value = "private_key", alternate = ["uid"])
  val privateKey: String,
  @SerializedName("server")
  val server: ServerShort,
)

data class DnsResponse(
  @SerializedName("Answer")
  val answer: List<DnsData>,
)

data class DnsData(
  @SerializedName("data")
  val data: String,
)

data class ServerShort(
  @SerializedName("id")
  val id: String,
)

data class Server(
  @SerializedName("id")
  val id: String,
  @SerializedName("country_id")
  val countryId: String,
  @SerializedName("city_id")
  val cityId: String,
  @SerializedName("name")
  val name: String,
  @SerializedName("address")
  val address: String,
  @SerializedName("is_available")
  val isAvailable: Boolean,
  @SerializedName("load")
  val load: Double,
  @SerializedName("version")
  val version: String,
  @SerializedName("latitude")
  val latitude: Double,
  @SerializedName("longitude")
  val longitude: Double,
  @SerializedName("upload_speed")
  val uploadSpeed: Int,
  @SerializedName("download_speed")
  val downloadSpeed: Int,
  @SerializedName("remote_url")
  val remoteUrl: String,
  @SerializedName("protocol")
  val protocol: String,
)

data class ConfigValue(
  @SerializedName("key")
  val key: String,
  @SerializedName("value")
  val value: String,
)
