package io.norselabs.vpn.common.ext

import android.net.Uri

fun String.parseUrl(): Uri? = try {
  Uri.parse(this)
} catch (e: Throwable) {
  null
}
