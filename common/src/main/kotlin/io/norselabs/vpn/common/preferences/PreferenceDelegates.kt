package io.norselabs.vpn.common.preferences

import android.content.SharedPreferences
import kotlin.reflect.KProperty
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface PreferenceDelegate<T> {

  var value: T

  val observe: Flow<T>
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> PreferenceDelegate<T>.getValue(
  thisObj: Any?,
  property: KProperty<*>,
): T = value

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> PreferenceDelegate<T>.setValue(
  thisObj: Any?,
  property: KProperty<*>,
  value: T,
) {
  this.value = value
}

internal class StringPreference(
  preferences: SharedPreferences,
  key: String,
  defaultValue: String,
) :
  PreferenceDelegate<String> by SimpleTypePreference(
    preferences = preferences,
    key = key,
    defaultValue = defaultValue,
    getValue = SharedPreferences::getString,
    putValue = SharedPreferences.Editor::putString,
  )

internal class IntPreference(
  preferences: SharedPreferences,
  key: String,
  defaultValue: Int,
) :
  PreferenceDelegate<Int> by SimpleTypePreference(
    preferences = preferences,
    key = key,
    defaultValue = defaultValue,
    getValue = SharedPreferences::getInt,
    putValue = SharedPreferences.Editor::putInt,
  )

internal class LongPreference(
  preferences: SharedPreferences,
  key: String,
  defaultValue: Long,
) :
  PreferenceDelegate<Long> by SimpleTypePreference(
    preferences = preferences,
    key = key,
    defaultValue = defaultValue,
    getValue = SharedPreferences::getLong,
    putValue = SharedPreferences.Editor::putLong,
  )

internal class BooleanPreference(
  preferences: SharedPreferences,
  key: String,
  defaultValue: Boolean,
) :
  PreferenceDelegate<Boolean> by SimpleTypePreference(
    preferences = preferences,
    key = key,
    defaultValue = defaultValue,
    getValue = SharedPreferences::getBoolean,
    putValue = SharedPreferences.Editor::putBoolean,
  )

private class SimpleTypePreference<T : Any>(
  private val preferences: SharedPreferences,
  private val key: String,
  private val defaultValue: T,
  private val getValue: SharedPreferences.(key: String, defaultValue: T) -> T?,
  private val putValue: SharedPreferences.Editor.(key: String, value: T) -> Unit,
) : PreferenceDelegate<T> {

  override var value: T
    get() = runCatching { getValue.invoke(preferences, key, defaultValue) }.getOrNull() ?: defaultValue
    set(value) {
      with(preferences.edit()) {
        putValue.invoke(this, key, value)
        apply()
      }
    }

  override val observe: Flow<T> =
    callbackFlow {
      val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
        if (key == changedKey) {
          trySend(value)
        }
      }

      preferences.registerOnSharedPreferenceChangeListener(listener)
      trySend(value)
      awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
