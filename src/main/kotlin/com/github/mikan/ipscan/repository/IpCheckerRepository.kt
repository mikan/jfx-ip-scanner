package com.github.mikan.ipscan.repository

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class IpCheckerRepository {
    private val checkIp = "https://checkip.amazonaws.com/"
    private val httpClient = HttpClient.newHttpClient()
    private val stringHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)

    fun resolveGlobalIp(): String {
        val request = HttpRequest.newBuilder(URI.create(checkIp)).GET().build()
        return try {
            httpClient.send(request, stringHandler).body().trim()
        } catch (e: IOException) {
            return ""
        } catch (e: InterruptedException) {
            return ""
        }
    }
}
