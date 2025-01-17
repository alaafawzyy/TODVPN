package io.norselabs.vpn.core_vpn.vpn.destination

import android.content.SharedPreferences
import com.google.gson.Gson
import io.norselabs.vpn.common.preferences.delegate
import io.norselabs.vpn.common.preferences.getValue
import io.norselabs.vpn.common.preferences.setValue
import io.norselabs.vpn.core_vpn.vpn.Destination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class DestinationStorage(
  private val gson: Gson,
  prefs: SharedPreferences,
) {

  private val destinationDelegate = prefs.delegate("selected_destination", "")
  private var destinationPref: String by destinationDelegate

  fun storeDestination(destination: Destination) {
    val selectedDestinationJson = gson.toJson(destination)
    destinationPref = selectedDestinationJson
  }

  fun observe(): Flow<Destination?> {
    return destinationDelegate.observe.map(::parse)
  }

  fun getLast(): Destination? {
    return parse(destinationPref)
  }

  private fun parse(json: String): Destination? {
    return try {
      val isServer = json.contains("server")
      val clazz = when {
        isServer -> Destination.Server::class.java
        else -> Destination.City::class.java
      }
      gson.fromJson(json, clazz)
    } catch (e: Exception) {
      Timber.d("Destination parsing failed")
      null
    }
  }
}
