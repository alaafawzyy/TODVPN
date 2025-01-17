package io.norselabs.vpn.common.state

sealed interface Status {
  data object Loading : Status
  data object Data : Status
  data class Error(val isLoading: Boolean = false) : Status
}
