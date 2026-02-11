package com.willx.ai

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException

object WikipediaSearch {
    private val client = OkHttpClient()

    data class Result(
        val title: String,
        val url: String,
    )

    @Throws(IOException::class)
    fun search(query: String, max: Int = 3): List<Result> {
        val url = "https://en.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "opensearch")
            .addQueryParameter("search", query)
            .addQueryParameter("limit", max.toString())
            .addQueryParameter("namespace", "0")
            .addQueryParameter("format", "json")
            .build()

        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("Wikipedia HTTP ${resp.code}: $body")
            }

            val arr = JSONArray(body)
            val titles = arr.getJSONArray(1)
            val urls = arr.getJSONArray(3)

            val out = ArrayList<Result>()
            val count = minOf(titles.length(), urls.length())
            for (i in 0 until count) {
                out.add(Result(titles.getString(i), urls.getString(i)))
            }
            return out
        }
    }
}
