package io.norselabs.vpn.common_logger.logger

import android.content.Context
import io.norselabs.vpn.common_logger.NonFatalException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import timber.log.Timber

class FileLogTree(
  private val context: Context,
  private val excluded: List<String> = emptyList(),
) : Timber.Tree() {

  companion object {
    private const val ANDROID_LOG_TIME_FORMAT = "MM-dd-yy kk:mm:ss.SSS"
  }

  private val accumulatedLogs = CopyOnWriteArrayList<String>()
  private val dateFormat = SimpleDateFormat(ANDROID_LOG_TIME_FORMAT, Locale.US)

  override fun log(
    priority: Int,
    tag: String?,
    message: String,
    t: Throwable?,
  ) {
    if (t is NonFatalException) return
    val date = System.currentTimeMillis().formatDate()
    accumulatedLogs.add("--> $date: [$tag] $message\n")
  }

  fun getLogsFile(): File? {
    val file = context.createFile("Logs.txt")
    return try {
      file.apply {
        bufferedWriter().use { buffer ->
          accumulatedLogs.forEach { line ->
            val preparedLine = excluded.fold(line) { acc, ex ->
              acc.replace(ex, "(*****)")
            }
            buffer.write(preparedLine)
          }
        }
      }
    } catch (e: Exception) {
      Timber.e(e)
      null
    }
  }

  private fun Long.formatDate(): String {
    val date = Date(this)
    return dateFormat.format(date)
  }

  private fun Context.createFile(fileName: String): File {
    val file = File(cacheDir, fileName)
    if (file.exists()) {
      file.delete()
    }
    return file
  }
}
