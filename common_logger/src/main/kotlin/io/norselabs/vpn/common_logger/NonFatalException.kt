package io.norselabs.vpn.common_logger

import timber.log.Timber

class NonFatalException(
  message: String,
  cause: Throwable?,
  tag: String? = null,
) : Exception(
  cause.addTagToCauses(
    tag?.let { "$it: $message" } ?: message,
  ),
) {

  companion object {

    private fun Throwable?.addTagToCauses(tag: String?): Throwable? {
      if (this == null) return null
      if (tag == null) return this
      return Throwable(
        message = "$tag: ${this.message}",
        cause = this.cause.addTagToCauses(tag),
      )
    }
  }
}

fun logNonFatal(
  message: String,
  throwable: Throwable?,
  tag: String? = null,
) {
  Timber.e(NonFatalException(message, throwable, tag))
}
