package io.norselabs.vpn.common.ext

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

fun Context.goToGooglePlay() {
  try {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
  } catch (e: ActivityNotFoundException) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
  }
}

fun Context.openWeb(url: String) {
  val uri = url.parseUrl() ?: return
  val intent = Intent(Intent.ACTION_VIEW, uri)
  startActivitySafe(intent)
}

fun Context.mailTo(email: String) {
  val uri = "mailto:".parseUrl() ?: return
  val intent = Intent(Intent.ACTION_SENDTO)
  intent.setData(uri)
  intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
  startActivitySafe(intent)
}

fun Context.startActivitySafe(
  intent: Intent,
  onActivityNotStarted: ((Intent, Exception) -> Unit)? = null,
) {
  try {
    startActivity(intent)
  } catch (ex: ActivityNotFoundException) {
    onActivityNotStarted?.invoke(intent, ex)
  } catch (ex: SecurityException) {
    onActivityNotStarted?.invoke(intent, ex)
  }
}

fun Context.openCamera(): Boolean {
  return try {
    startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
    true
  } catch (e: Exception) {
    false
  }
}
