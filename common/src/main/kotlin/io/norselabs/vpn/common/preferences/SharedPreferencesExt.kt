@file:Suppress("unused")

package io.norselabs.vpn.common.preferences

import android.content.SharedPreferences

fun SharedPreferences.delegate(
  key: String,
  defaultValue: String,
): PreferenceDelegate<String> = StringPreference(
  preferences = this,
  key = key,
  defaultValue = defaultValue,
)

fun SharedPreferences.delegate(
  key: String,
  defaultValue: Int,
): PreferenceDelegate<Int> = IntPreference(
  preferences = this,
  key = key,
  defaultValue = defaultValue,
)

fun SharedPreferences.delegate(
  key: String,
  defaultValue: Long,
): PreferenceDelegate<Long> = LongPreference(
  preferences = this,
  key = key,
  defaultValue = defaultValue,
)

fun SharedPreferences.delegate(
  key: String,
  defaultValue: Boolean,
): PreferenceDelegate<Boolean> = BooleanPreference(
  preferences = this,
  key = key,
  defaultValue = defaultValue,
)
