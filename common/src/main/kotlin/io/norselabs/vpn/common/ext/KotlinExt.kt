package io.norselabs.vpn.common.ext

fun <T> T.oneOf(vararg list: T): Boolean {
  return this in list
}
