package com.willx.ai

import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.os.Handler
import android.os.Looper

object JinaDeepSearch {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Throws(IOException::class)
    fun deepSearch(apiKey: String, query: String): String {
        val body = JSONObject()
            .put("model", "jina-deepsearch-v1")
            .put("stream", true)
            .put("reasoning_effort", "low")
            .put("max_attempts", 1)
            .put(
                "messages",
                org.json.JSONArray()
                    .put(JSONObject().put("role", "user").put("content", query))
            )

        val req = Request.Builder()
            .url("https://deepsearch.jina.ai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val text = resp.body?.string().orEmpty()
                throw IOException("Jina DeepSearch HTTP ${resp.code}: $text")
            }

            val source = resp.body?.source() ?: return ""
            val out = StringBuilder()

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue
                if (!line.startsWith("data:")) continue

                val payload = line.removePrefix("data:").trim()
                if (payload == "[DONE]") break

                try {
                    val json = JSONObject(payload)
                    val choices = json.optJSONArray("choices") ?: continue
                    if (choices.length() == 0) continue
                    val delta = choices.getJSONObject(0).optJSONObject("delta") ?: continue
                    val content = delta.optString("content")
                    if (content.isNotEmpty()) out.append(content)
                } catch (_: Throwable) {
                    // ignore malformed chunks
                }
            }

            return out.toString()
        }
    }

    fun deepSearchStreaming(
        apiKey: String,
        query: String,
        onDelta: (String) -> Unit,
        onComplete: (Result<Unit>) -> Unit,
    ): Call? {
        val body = JSONObject()
            .put("model", "jina-deepsearch-v1")
            .put("stream", true)
            .put("reasoning_effort", "low")
            .put("max_attempts", 1)
            .put(
                "messages",
                org.json.JSONArray()
                    .put(JSONObject().put("role", "user").put("content", query))
            )

        val req = Request.Builder()
            .url("https://deepsearch.jina.ai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val call = client.newCall(req)

        Thread {
            try {
                call.execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val text = resp.body?.string().orEmpty()
                        mainHandler.post { onComplete(Result.failure(IOException("Jina DeepSearch HTTP ${resp.code}: $text"))) }
                        return@use
                    }

                    val source = resp.body?.source() ?: run {
                        mainHandler.post { onComplete(Result.success(Unit)) }
                        return@use
                    }

                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.isBlank()) continue
                        if (!line.startsWith("data:")) continue

                        val payload = line.removePrefix("data:").trim()
                        if (payload == "[DONE]") break

                        try {
                            val json = JSONObject(payload)
                            val choices = json.optJSONArray("choices") ?: continue
                            if (choices.length() == 0) continue
                            val delta = choices.getJSONObject(0).optJSONObject("delta") ?: continue
                            val content = delta.optString("content")
                            if (content.isNotEmpty()) {
                                mainHandler.post { onDelta(content) }
                            }
                        } catch (_: Throwable) {
                            // ignore malformed chunks
                        }
                    }

                    mainHandler.post { onComplete(Result.success(Unit)) }
                }
            } catch (t: Throwable) {
                mainHandler.post { onComplete(Result.failure(t)) }
            }
        }.start()

        return call
    }
}
