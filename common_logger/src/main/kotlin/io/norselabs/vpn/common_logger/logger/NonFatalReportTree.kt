package io.norselabs.vpn.common_logger.logger

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.util.Log
import io.norselabs.vpn.common_logger.NonFatalException
import timber.log.Timber

class NonFatalReportTree(
  context: Context,
  private val log: (String) -> Unit,
  private val recordException: (Throwable) -> Unit,
  setCustomKeys: (Map<String, String>) -> Unit,
) : Timber.Tree() {

  private val isMainProcess = context.isMainProcess()

  init {
    if (isMainProcess) {
      setCustomKeys(
        mapOf(
          "processor-arch" to System.getProperty("os.arch").orEmpty(),
        ),
      )
    }
  }

  override fun log(
    priority: Int,
    tag: String?,
    message: String,
    t: Throwable?,
  ) {
    if (!isMainProcess) return
    if (priority != Log.ERROR) return
    val nonFatalException = t as? NonFatalException ?: return

    nonFatalException.message?.takeIfNotBlank()?.let { log(it) }
    val actualException = nonFatalException.cause ?: nonFatalException
    recordException(actualException)
  }

  private fun String.takeIfNotBlank(): String? = takeIf { it.isNotBlank() }

  private fun Context.isMainProcess(): Boolean {
    return packageName.equals(getProcessName())
  }

  private fun Context.getProcessName(): String? {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    manager?.runningAppProcesses?.forEach { process ->
      if (process.pid == Process.myPid()) {
        return process.processName
      }
    }
    return null
  }
}
