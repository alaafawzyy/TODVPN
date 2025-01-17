package io.norselabs.vpn.common_net_apps

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

object AppManagerUtil {

  fun getNetworkAppList(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
    val apps = mutableListOf<AppInfo>()

    for (pkg in packages) {
      if (!pkg.hasInternetPermission && pkg.packageName != "android") continue

      val applicationInfo = pkg.applicationInfo

      val appName = applicationInfo.loadLabel(packageManager).toString()
      val appIcon = applicationInfo.loadIcon(packageManager)
      val isSystemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM > 0

      val appInfo = AppInfo(appName, pkg.packageName, appIcon, isSystemApp)
      apps.add(appInfo)
    }

    return apps
  }

  private val PackageInfo.hasInternetPermission: Boolean
    get() {
      val permissions = requestedPermissions
      return permissions?.any { it == Manifest.permission.INTERNET } ?: false
    }
}

data class AppInfo(
  val appName: String,
  val packageName: String,
  val appIcon: Drawable,
  val isSystemApp: Boolean,
)
