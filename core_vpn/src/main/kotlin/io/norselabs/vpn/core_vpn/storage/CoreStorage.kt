package io.norselabs.vpn.core_vpn.storage

import android.content.SharedPreferences
import io.norselabs.vpn.common.preferences.delegate
import io.norselabs.vpn.common.preferences.getValue
import io.norselabs.vpn.common.preferences.setValue
import io.norselabs.vpn.core_vpn.vpn.Protocol
import kotlinx.coroutines.flow.Flow

class CoreStorage(prefs: SharedPreferences) {

  private var tokenPref: String by prefs.delegate("device_token", "")

  private var userIdPref: String by prefs.delegate("user_id", "")

  private var protocolPref by prefs.delegate("selected_protocol", "")

  private var currentServerIdPref = prefs.delegate("last_server_id", "")

  fun setToken(token: String) {
    tokenPref = token
  }

  fun getToken(): String = tokenPref

  fun setUserId(userId: String) {
    userIdPref = userId
  }

  fun getUserId(): String = userIdPref

  fun setVpnProtocol(protocol: Protocol) {
    protocolPref = protocol.strValue
  }

  fun getVpnProtocol(): Protocol = Protocol.fromString(protocolPref)

  fun setCurrentServerId(serverId: String?) {
    currentServerIdPref.value = serverId.orEmpty()
  }

  fun getCurrentServerId(): String = currentServerIdPref.value

  fun observeCurrentServerId(): Flow<String> = currentServerIdPref.observe

  fun clearUserData() {
    tokenPref = ""
    userIdPref = ""
    protocolPref = ""
    currentServerIdPref.value = ""
  }
}
