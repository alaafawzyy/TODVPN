package io.norselabs.vpn.core_vpn.user

enum class UserStatus(val order: Int) {
  Determining(1),
  HasToken(2),
  Enrolling(3),
  Enrolled(4),
  NotEnrolled(5),
  Banned(6),
  VersionOutdated(7),
  Failed(8),
}

data class Token(
  val id: String,
  val token: String,
  val isBanned: Boolean,
  val isEnrolled: Boolean,
)
