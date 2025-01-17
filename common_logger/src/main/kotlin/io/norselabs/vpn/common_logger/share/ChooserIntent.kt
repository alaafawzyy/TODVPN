package io.norselabs.vpn.common_logger.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.norselabs.vpn.common_logger.logNonFatal
import java.io.File

object ChooserIntent {

  fun start(
    context: Context,
    appId: String,
    file: File,
  ) {
    try {
      val intentShareFile = Intent(Intent.ACTION_SEND)
      if (file.exists()) {
        val fileUri = FileProvider.getUriForFile(context, "$appId.provider", file)
        intentShareFile.setType("text/plain")
        intentShareFile.putExtra(Intent.EXTRA_STREAM, fileUri)
        context.startActivity(Intent.createChooser(intentShareFile, "Share Logs file"))
      }
    } catch (e: Exception) {
      logNonFatal("Can't share logs", e)
    }
  }
}
