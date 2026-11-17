package com.hoopslive.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/** Minimal JSON GET client with a small in-memory TTL cache. */
object Http {
    private class Entry(val time: Long, val body: String)

    private val cache = ConcurrentHashMap<String, Entry>()

    suspend fun getJson(rawUrl: String, maxAgeMs: Long = 0L): JSONObject = withContext(Dispatchers.IO) {
        // ESPN core-API "$ref" links are http://; Android blocks cleartext, so force https.
        val url = if (rawUrl.startsWith("http://")) "https://" + rawUrl.removePrefix("http://") else rawUrl
        val now = System.currentTimeMillis()
        val cached = cache[url]
        if (cached != null && maxAgeMs > 0 && now - cached.time < maxAgeMs) {
            return@withContext JSONObject(cached.body)
        }
        val body = try {
            fetch(url)
        } catch (e: IOException) {
            // Offline or flaky: fall back to whatever we last saw rather than an empty screen.
            cached?.body ?: throw e
        }
        cache[url] = Entry(now, body)
        JSONObject(body)
    }

    fun invalidate(prefix: String) {
        cache.keys.filter { it.startsWith(prefix) }.forEach { cache.remove(it) }
    }

    private fun fetch(url: String): String {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) HoopsLive/1.0")
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IOException(if (code == 404) "Not available yet (404)" else "Server error $code")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
