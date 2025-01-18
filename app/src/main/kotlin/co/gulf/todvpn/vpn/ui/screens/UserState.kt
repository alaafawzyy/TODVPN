package co.gulf.todvpn.vpn.ui.screens

import java.util.Date

data class UserState(
  val startDate: Date?,
  val activationEndDate: Date?,
  val isTrialActive: Boolean
)

