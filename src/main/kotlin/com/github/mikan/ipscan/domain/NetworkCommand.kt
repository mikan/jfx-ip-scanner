package com.github.mikan.ipscan.domain

import java.io.InputStream
import java.util.Scanner

enum class NetworkCommand {
    PING, ARP, TRACEROUTE;

    companion object {
        private val windows = System.getProperty("os.name").lowercase().startsWith("win")
        private val windowsSuccessResultPatterns = listOf("Approximate round trip times", "ラウンド トリップの概算時間")

        fun encodePingCommand(target: String, nPackets: Int, timeoutSec: Int): String {
            return if (windows) {
                "ping -n $nPackets -w ${timeoutSec * 1000} $target"
            } else {
                "ping -c$nPackets -t $timeoutSec $target"
            }
        }

        fun encodeArpCommand(ifAddress: String, ifName: String): String {
            return if (windows) {
                "arp -a -N $ifAddress"
            } else {
                "arp -i $ifName -a"
            }
        }

        fun encodeArpCommand(address: String): String {
            return if (windows) {
                "arp -a $address"
            } else {
                "arp $address"
            }
        }

        fun encodeTraceRouteCommand(target: String): String {
            return if (windows) {
                "tracert $target"
            } else {
                "traceroute $target"
            }
        }

        fun validatePingResult(status: Int, result: String): Boolean {
            if (windows) {
                if (windowsSuccessResultPatterns.any { s -> result.contains(s) }) {
                    return true
                }
            } else {
                if (status == 0) {
                    return true
                }
            }
            return false
        }

        fun execPing(target: String, nPackets: Int, timeoutSec: Int): Boolean {
            val p = Runtime.getRuntime().exec(encodePingCommand(target, nPackets, timeoutSec))
            val result = readAll(p.inputStream)
            p.waitFor()
            return validatePingResult(p.exitValue(), result)
        }

        private fun readAll(inputStream: InputStream): String {
            var result = ""
            Scanner(inputStream).use { scan ->
                while (scan.hasNextLine()) {
                    result += scan.nextLine() + "\n"
                }
            }
            return result
        }
    }
}
