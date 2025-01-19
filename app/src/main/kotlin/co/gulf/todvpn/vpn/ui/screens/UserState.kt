package co.gulf.todvpn.vpn.ui.screens

import java.util.Date

data class UserState(
    var startDate: Date?,
    val activationEndDate: Date?,
    val isTrialActive: Boolean
)

