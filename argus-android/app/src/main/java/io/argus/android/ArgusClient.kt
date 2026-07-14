package io.argus.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One ranked search hit. */
data class Hit(val docId: Int, val score: Double, val title: String, val body: String)

/** A search response: total match count and the returned hits. */
data class SearchResult(val total: Long, val hits: List<Hit>)

/**
 * A thin HTTP client for the Argus server's {@code GET /search} endpoint. Uses the JDK's
 * {@code HttpURLConnection} and Android's built-in {@code org.json} — no third-party networking libs.
 */
object ArgusClient {

    suspend fun search(baseUrl: String, query: String): SearchResult = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL(baseUrl.trimEnd('/') + "/search?q=" + encoded + "&k=25")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.requestMethod = "GET"
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                throw RuntimeException("HTTP $code")
            }
            parse(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(text: String): SearchResult {
        val root = JSONObject(text)
        val total = root.optLong("total", 0)
        val hitsJson = root.optJSONArray("hits")
        val hits = ArrayList<Hit>()
        if (hitsJson != null) {
            for (i in 0 until hitsJson.length()) {
                val h = hitsJson.getJSONObject(i)
                val fields = h.optJSONObject("fields") ?: JSONObject()
                hits.add(
                    Hit(
                        docId = h.optInt("docId", -1),
                        score = h.optDouble("score", 0.0),
                        title = fields.optString("title", fields.optString("id", "(document)")),
                        body = fields.optString("body", "")
                    )
                )
            }
        }
        return SearchResult(total, hits)
    }
}
