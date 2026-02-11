package com.willx.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object OpenRouterClient {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun sendChat(
        context: Context,
        userMessage: String,
        extraContext: String? = null,
        onResult: (Result<String>) -> Unit,
    ) {
        val sb = StringBuilder()
        sendChatStreaming(
            context = context,
            userMessage = userMessage,
            extraContext = extraContext,
            onDelta = { chunk -> sb.append(chunk) },
            onComplete = { res ->
                onResult(res.map { sb.toString() })
            },
        )
    }

    fun sendChatStreaming(
        context: Context,
        userMessage: String,
        extraContext: String? = null,
        onDelta: (String) -> Unit,
        onComplete: (Result<Unit>) -> Unit,
    ): Call? {
        var callRef: Call? = null

        CoroutineScope(Dispatchers.IO).launch {
            val apiKey = AppSettings.apiKeyFlow(context).first().trim()
            val modelSetting = AppSettings.modelFlow(context).first().trim()
            val model = ModelCatalog.resolve(modelSetting)
            val webSearchEnabled = AppSettings.webSearchEnabledFlow(context).first()
            val webProvider = AppSettings.webProviderFlow(context).first()
            val jinaApiKey = AppSettings.jinaApiKeyFlow(context).first().trim()

            if (apiKey.isBlank()) {
                mainHandler.post {
                    onComplete(Result.failure(IllegalStateException("Abra Configurações e informe a OpenRouter API Key")))
                }
                return@launch
            }

            val webBlock = if (webSearchEnabled) {
                runCatching {
                    when (webProvider) {
                        WebProvider.JINA_DEEPSEARCH -> {
                            ""
                        }

                        WebProvider.JINA_SEARCH -> {
                            if (jinaApiKey.isBlank()) {
                                "\n\n[Web search]\nJina API Key não configurada (Configurações)"
                            } else {
                                val results = JinaSearch.search(jinaApiKey, userMessage, max = 3)
                                if (results.isEmpty()) "" else {
                                    buildString {
                                        append("\n\n[Web search: Jina]\n")
                                        results.forEachIndexed { idx, r ->
                                            append("${idx + 1}. ${r.title} - ${r.url}\n")
                                            if (r.snippet.isNotBlank()) {
                                                append("   ${r.snippet}\n")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            val results = WikipediaSearch.search(userMessage, max = 3)
                            if (results.isEmpty()) "" else {
                                buildString {
                                    append("\n\n[Web search: Wikipedia]\n")
                                    results.forEachIndexed { idx, r ->
                                        append("${idx + 1}. ${r.title} - ${r.url}\n")
                                    }
                                }
                            }
                        }
                    }
                }.getOrElse { "" }
            } else {
                ""
            }

            val userContent = buildString {
                append(userMessage)
                if (!extraContext.isNullOrBlank()) {
                    append("\n\n[Attachment]\n")
                    append(extraContext)
                }
                if (webBlock.isNotBlank()) {
                    append(webBlock)
                }
            }

            val bodyJson = JSONObject().apply {
                put("model", model)
                put("stream", true)
                put(
                    "messages",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", userContent)
                    )
                )
            }

            val request = Request.Builder()
                .url(BuildConfig.OPENROUTER_BASE_URL + "/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val call = client.newCall(request)
            callRef = call

            try {
                call.execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val text = resp.body?.string().orEmpty()
                        mainHandler.post {
                            onComplete(Result.failure(IOException("OpenRouter HTTP ${resp.code}: $text")))
                        }
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

                        runCatching {
                            val json = JSONObject(payload)
                            val choices = json.optJSONArray("choices") ?: return@runCatching
                            if (choices.length() == 0) return@runCatching
                            val delta = choices.getJSONObject(0).optJSONObject("delta") ?: return@runCatching
                            val content = delta.optString("content")
                            if (content.isNotEmpty()) {
                                mainHandler.post { onDelta(content) }
                            }
                        }
                    }

                    mainHandler.post { onComplete(Result.success(Unit)) }
                }
            } catch (e: Throwable) {
                mainHandler.post { onComplete(Result.failure(e)) }
            }
        }

        return callRef
    }
}
