package io.norselabs.vpn.common_network.api

import io.norselabs.vpn.common_network.models.City
import io.norselabs.vpn.common_network.models.ConfigValue
import io.norselabs.vpn.common_network.models.Country
import io.norselabs.vpn.common_network.models.DataList
import io.norselabs.vpn.common_network.models.DataObj
import io.norselabs.vpn.common_network.models.DnsResponse
import io.norselabs.vpn.common_network.models.NetworkData
import io.norselabs.vpn.common_network.models.RegisterDeviceRequest
import io.norselabs.vpn.common_network.models.Server
import io.norselabs.vpn.common_network.models.TokenModel
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface Api {

  @POST("device")
  suspend fun registerDevice(
    @Body body: RegisterDeviceRequest,
  ): DataObj<TokenModel>

  @GET("device")
  suspend fun getSession(): DataObj<TokenModel>

  @GET("country")
  suspend fun getCountries(
    @Query("protocol") protocol: String?,
  ): DataList<Country>

  @GET("country/{countryId}/city")
  suspend fun getCities(
    @Path("countryId") countryId: String,
    @Query("protocol") protocol: String?,
  ): DataList<City>

  @GET("ip")
  suspend fun getIpData(): DataObj<NetworkData>

  @GET("server/{serverId}")
  suspend fun getServer(
    @Path("serverId") serverId: String,
  ): DataObj<Server>

  @GET("health")
  suspend fun checkConnection()

  @GET("config")
  suspend fun getConfig(
    @Query("app_token") appToken: String,
  ): DataList<ConfigValue>

  @GET
  suspend fun getDNS(
    @Url url: String,
    @QueryMap queries: Map<String, String>,
    @Header("Accept") value: String = "application/dns-json",
  ): DnsResponse
}
