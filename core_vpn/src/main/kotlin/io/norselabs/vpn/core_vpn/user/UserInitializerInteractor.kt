package io.norselabs.vpn.core_vpn.user

import arrow.core.Either

interface UserInitializerInteractor {

  suspend fun checkVersion(): Either<Exception, Boolean>
  suspend fun registerDevice(): Either<Exception, Token>
  suspend fun getSession(): Either<Exception, Token>

  fun parseHttpCode(exception: Exception): Int?
}
