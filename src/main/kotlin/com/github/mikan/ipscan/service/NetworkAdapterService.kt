package com.github.mikan.ipscan.service

import com.github.mikan.ipscan.domain.NetworkAdapter
import com.github.mikan.ipscan.repository.IpCheckerRepository
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

object NetworkAdapterService {
    private val ipCheckerRepository = IpCheckerRepository()

    fun globalIp(): String {
        return ipCheckerRepository.resolveGlobalIp()
    }

    fun hostFromIp(ip: String): String {
        return try {
            InetAddress.getByName(ip).hostName
        } catch (e: IOException) {
            ""
        }
    }

    fun listNetworkAdapters(): List<NetworkAdapter> {
        return try {
            Collections.list(NetworkInterface.getNetworkInterfaces())
                .filter { it.isUp && !it.isLoopback }
                .map {
                    NetworkAdapter(
                        it.name,
                        it.displayName,
                        it.interfaceAddresses
                            .filter { ip -> ip.broadcast != null }
                            .map { ip ->
                                NetworkAdapter.IpV4(
                                    ip.address.hostAddress,
                                    ip.broadcast.hostAddress,
                                    ip.networkPrefixLength
                                )
                            }
                    )
                }
                .filter { it.ipV4.isNotEmpty() }
        } catch (e: IOException) {
            listOf()
        }
    }
}
