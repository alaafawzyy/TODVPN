package io.norselabs.vpn.based.storage

import com.google.gson.annotations.SerializedName

enum class RatingStatus {
  @SerializedName("New")
  New,

  @SerializedName("RequestOnNext")
  RequestOnNext,

  @SerializedName("Requested")
  Requested,
}
