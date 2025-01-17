package io.norselabs.vpn.common_network

import arrow.core.Either
import io.norselabs.vpn.common_network.api.Api
import kotlinx.coroutines.withTimeout

class DnsBasedClient(
  private val api: Api,
) {

  companion object {
    const val TIMEOUT = 5000L
  }

  private val hosts = listOf(
    "https://8.8.8.8/resolve",
    "https://8.8.4.4/resolve",
    "https://1.1.1.1/dns-query",
    "https://1.0.0.1/dns-query",
    "https://223.5.5.5/resolve",
    "https://223.6.6.6/resolve",
    "https://9.9.9.9:5053/dns-query",
  )

  suspend fun getVersion(request: String): Either<Exception, String> {
    val response = request(request, TIMEOUT)
    return response
      ?.split(";")
      ?.firstOrNull { it.startsWith("ANDROID") }
      ?.substringAfter("=")
      ?.let { Either.Right(it) }
      ?: Either.Left(DnsBasedClientException)
  }

  private suspend fun request(
    method: String,
    timeout: Long = Long.MAX_VALUE,
  ): String? {
    hosts.forEach { host ->
      val response = runCatching {
        withTimeout(timeout) {
          api.getDNS(
            url = host,
            queries = mapOf(
              "name" to method,
              "type" to "TXT",
            ),
          )
        }
      }.getOrNull()?.answer?.firstOrNull()?.data
      if (response != null) {
        return response
      }
    }
    return null
  }
}

object DnsBasedClientException : Exception("Failed to fetch data from DNS Based client")
