package io.norselabs.vpn.based.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {

  fun setLanguage(lang: String) {
    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(lang)
    AppCompatDelegate.setApplicationLocales(appLocale)
  }

  fun getLanguage(): String {
    return AppCompatDelegate.getApplicationLocales().toLanguageTags()
  }
}
