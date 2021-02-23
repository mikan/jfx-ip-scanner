package com.github.mikan.ipscan.domain

import java.lang.NumberFormatException

data class NetworkAdapter(val name: String, val displayName: String, val ipV4: List<IpV4>) {
    data class IpV4(val address: String, val broadcast: String, val length: Short) {
        private val uintMask = 0x0FFFFFFFF.toInt()
        fun subnetIps(): List<String> {
            val tokens = try {
                address.split(".").map { it.toInt() }
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("invalid IPv4 address: $address")
            }
            if (tokens.size != 4) {
                throw IllegalArgumentException("invalid IPv4 address: $address")
            }
            var addr = 0
            for (i in 1..4) {
                addr = addr or (tokens[i - 1] and 0xff shl 8 * (4 - i))
            }
            val netmask = (0x0FFFFFFFFL shl (32 - length.toInt())).toInt()
            val network = addr and netmask
            val broadcast = network or netmask.inv()
            val b = (broadcast and uintMask).toLong()
            val n = (network and uintMask).toLong()
            val count = b - n + -1
            val ct = (if (count < 0) 0 else count).toInt()
            if (ct == 0) {
                return listOf()
            }
            val addresses = arrayOfNulls<String>(ct)
            var add = if (b - n > 1) network + 1 else 0
            var i = 0
            while (add <= if (b - n > 1) broadcast - 1 else 0) {
                val ar = IntArray(4)
                for (j in 3 downTo 0) {
                    ar[j] = ar[j] or (add ushr 8 * (3 - j) and 0xff)
                }
                var str = ""
                for (j in ar.indices) {
                    str += ar[j]
                    if (j != ar.size - 1) {
                        str += "."
                    }
                }
                addresses[i] = str
                add++
                i++
            }
            return listOf(*addresses).map { l: String? -> l ?: "" }
        }
    }

    override fun toString(): String {
        val addr = if (ipV4.isEmpty()) "0.0.0.0/0" else "${ipV4[0].address}/${ipV4[0].length}"
        return "$addr @ $name" + if (name != displayName) " ($displayName)" else ""
    }
}
