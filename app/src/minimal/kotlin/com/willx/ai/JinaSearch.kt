package com.willx.ai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object JinaSearch {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class Result(
        val title: String,
        val url: String,
        val snippet: String,
        val content: String,
    )

    @Throws(IOException::class)
    fun search(apiKey: String, query: String, max: Int = 3): List<Result> {
        val body = JSONObject()
            .put("q", query)
            .put("num", max)

        val req = Request.Builder()
            .url("https://s.jina.ai/")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException("Jina Search HTTP ${resp.code}: $text")

            val root = JSONObject(text)
            val data = root.optJSONArray("data") ?: JSONArray()
            val out = ArrayList<Result>()
            for (i in 0 until minOf(max, data.length())) {
                val o = data.getJSONObject(i)
                out.add(
                    Result(
                        title = o.optString("title"),
                        url = o.optString("url"),
                        snippet = o.optString("description"),
                        content = o.optString("content"),
                    )
                )
            }
            return out
        }
    }
}
