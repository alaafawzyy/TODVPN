package io.norselabs.vpn.based.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService

fun getVpnPermissionRequest(context: Context): Intent? {
  return VpnService.prepare(context)
}
