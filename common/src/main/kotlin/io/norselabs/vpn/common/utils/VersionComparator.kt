package io.norselabs.vpn.common.utils

object VersionComparator {

  fun compare(version1: String, version2: String): Int {
    val ver1 = version1.splitVersion()
    val ver2 = version2.splitVersion()

    if (ver1.first < ver2.first) return -1
    if (ver1.first > ver2.first) return 1

    if (ver1.second < ver2.second) return -1
    if (ver1.second > ver2.second) return 1

    if (ver1.third < ver2.third) return -1
    if (ver1.third > ver2.third) return 1

    return 0
  }

  private fun String.splitVersion(): Triple<Int, Int, Int> {
    return try {
      val splitted = this.split(".")
      Triple(splitted[0].toInt(), splitted[1].toInt(), splitted[2].toInt())
    } catch (e: Exception) {
      Triple(0, 0, 0)
    }
  }
}
