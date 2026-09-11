package com.chattriggers.ctjs.api.client

import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.TimeUnit

object HypixelPublicApi {
    private val endpoints = setOf(
        "https://api.hypixel.net/v2/resources/skyblock/items",
        "https://api.hypixel.net/v2/skyblock/bazaar",
    )
    private const val MAX_BYTES = 16 * 1024 * 1024
    private val client by lazy {
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5))
            .proxy(object : ProxySelector() {
                override fun select(uri: URI): List<Proxy> = listOf(Proxy.NO_PROXY)
                override fun connectFailed(uri: URI, address: SocketAddress, error: IOException) {}
            })
            .build()
    }

    @JvmStatic
    fun read(url: String): String {
        require(url in endpoints) { "V5 Offline only permits fixed public Hypixel endpoints" }
        // A private HttpClient does not inherit JVM CookieHandler or Authenticator credentials.
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .header("User-Agent", "V5-Offline")
            .GET().build()
        val response = client.sendAsync(request,
            HttpResponse.BodyHandlers.limiting(HttpResponse.BodyHandlers.ofString(Charsets.UTF_8), MAX_BYTES.toLong()))
        try {
            val result = response.get(12, TimeUnit.SECONDS)
            if (result.statusCode() != 200) throw IOException("Hypixel response: ${result.statusCode()}")
            return result.body()
        } finally {
            if (!response.isDone) response.cancel(true)
        }
    }
}
