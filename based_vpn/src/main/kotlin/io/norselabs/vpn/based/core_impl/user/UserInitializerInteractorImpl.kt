package io.norselabs.vpn.based.core_impl.user

import arrow.core.Either
import io.norselabs.vpn.based.app_config.AppConfig
import io.norselabs.vpn.based.network.DnsRequests
import io.norselabs.vpn.common.utils.VersionComparator
import io.norselabs.vpn.common_network.AppRepository
import io.norselabs.vpn.common_network.models.DataObj
import io.norselabs.vpn.common_network.models.TokenModel
import io.norselabs.vpn.core_vpn.user.Token
import io.norselabs.vpn.core_vpn.user.UserInitializerInteractor
import retrofit2.HttpException

class UserInitializerInteractorImpl(
  private val repository: AppRepository,
  private val config: AppConfig,
  private val dnsRequests: DnsRequests,
) : UserInitializerInteractor {

  override suspend fun checkVersion(): Either<Exception, Boolean> {
    val minVersion = repository.getVersion(dnsRequests.version).getOrNull() ?: "0.0.0"
    val appVersion = config.getAppVersion()
    return Either.Right(
      VersionComparator.compare(appVersion, minVersion) != -1,
    )
  }

  override suspend fun registerDevice(): Either<Exception, Token> {
    return repository.registerDevice(config.getAppToken()).map(::parseToken)
  }

  override suspend fun getSession(): Either<Exception, Token> {
    return repository.getSession().map(::parseToken)
  }

  private fun parseToken(model: DataObj<TokenModel>): Token {
    val data = model.data
    return Token(
      id = data.id,
      token = data.token,
      isBanned = data.isBanned,
      isEnrolled = data.isEnrolled,
    )
  }

  override fun parseHttpCode(exception: Exception): Int? {
    return (exception as? HttpException)?.response()?.code()
  }
}
