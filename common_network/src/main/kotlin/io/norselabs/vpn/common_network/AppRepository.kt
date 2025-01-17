package io.norselabs.vpn.common_network

import arrow.core.Either
import arrow.core.flatMap
import io.norselabs.vpn.common_network.api.Api
import io.norselabs.vpn.common_network.api.ConnectApi
import io.norselabs.vpn.common_network.cache.CacheUnit
import io.norselabs.vpn.common_network.models.CitiesRequest
import io.norselabs.vpn.common_network.models.City
import io.norselabs.vpn.common_network.models.Country
import io.norselabs.vpn.common_network.models.CreateCredentials
import io.norselabs.vpn.common_network.models.CredentialsResponse
import io.norselabs.vpn.common_network.models.DataList
import io.norselabs.vpn.common_network.models.DataObj
import io.norselabs.vpn.common_network.models.NetworkData
import io.norselabs.vpn.common_network.models.RegisterDeviceRequest
import io.norselabs.vpn.common_network.models.Server
import io.norselabs.vpn.common_network.models.TokenModel
import okhttp3.OkHttpClient
import timber.log.Timber

class AppRepository(
  private val api: Api,
  private val connectApi: ConnectApi,
  private val client: OkHttpClient,
  private val dnsClient: DnsBasedClient,
  private val appToken: String,
) {

  private val countriesCache = CacheUnit<String?, DataList<Country>> { protocol ->
    api.getCountries(protocol)
  }

  private val citiesCache = CacheUnit<CitiesRequest, DataList<City>> { request ->
    api.getCities(request.countryId, request.protocol)
  }

  suspend fun registerDevice(appToken: String): Either<Exception, DataObj<TokenModel>> = execute {
    api.registerDevice(RegisterDeviceRequest(appToken))
  }

  suspend fun getSession(): Either<Exception, DataObj<TokenModel>> = execute(api::getSession)

  suspend fun getCountries(
    protocol: String?,
    isFresh: Boolean = false,
  ): Either<Exception, DataList<Country>> = execute { countriesCache.get(protocol, isFresh) }

  suspend fun getCities(
    request: CitiesRequest,
    isFresh: Boolean = false,
  ): Either<Exception, DataList<City>> = execute { citiesCache.get(request, isFresh) }

  suspend fun getCredentials(
    cityId: String,
    protocol: String?,
  ): Either<Exception, DataObj<CredentialsResponse>> = execute {
    connectApi.getCredentials(
      cityId = cityId,
      body = CreateCredentials(protocol.orEmpty()),
    )
  }

  suspend fun getIpData(): Either<Exception, DataObj<NetworkData>> = execute { api.getIpData() }

  suspend fun getServer(serverId: String): Either<Exception, DataObj<Server>> = execute {
    api.getServer(serverId)
  }

  suspend fun getVersion(request: String): Either<Exception, String> {
    return execute { api.getConfig(appToken) }
      .flatMap { configs ->
        configs.data
          .firstOrNull { it.key == "minimal_android_version" }?.value
          ?.let { Either.Right(it) }
          ?: Either.Left(Unit)
      }
      .fold(
        ifLeft = { dnsClient.getVersion(request) },
        ifRight = { Either.Right(it) },
      )
  }

  suspend fun checkConnection(): Either<Exception, Boolean> = execute {
    api.checkConnection()
    true
  }

  fun resetConnection() {
    client.connectionPool.evictAll()
  }
}

suspend fun <T> execute(method: suspend () -> T): Either<Exception, T> {
  return try {
    Either.Right(method.invoke())
  } catch (e: Exception) {
    Timber.e(e)
    Either.Left(e)
  }
}
