package com.github.mikan.ipscan.service

object ValidationService {
    private val macFinder = Regex("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})")
    private val ipV4Pattern =
        Regex("(([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])")

    fun validateIpV4Address(ip: String): Boolean = ipV4Pattern.matches(ip)
    fun extractMacAddress(src: String): String = macFinder.find(src)?.value ?: ""
}
