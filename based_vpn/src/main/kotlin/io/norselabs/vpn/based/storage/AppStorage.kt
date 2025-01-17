package io.norselabs.vpn.based.storage

import android.content.SharedPreferences
import io.norselabs.vpn.common.preferences.delegate
import io.norselabs.vpn.common.preferences.getValue
import io.norselabs.vpn.common.preferences.setValue
import javax.inject.Inject

class AppStorage
@Inject constructor(prefs: SharedPreferences) {

  private var onboardingPref by prefs.delegate("onboarding_shown", false)

  private var ratingPref by prefs.delegate("rating_status", RatingStatus.New.name)

  fun isOnboardingShown() = onboardingPref

  fun onOnboardingShown() {
    onboardingPref = true
  }

  fun setRatingStatus(status: RatingStatus) {
    ratingPref = status.name
  }

  fun getRatingStatus(): RatingStatus {
    return try {
      RatingStatus.valueOf(ratingPref)
    } catch (e: IllegalArgumentException) {
      RatingStatus.New
    }
  }
}
