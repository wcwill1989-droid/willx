package com.willx.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ChatHistoryStore {
    private const val FILE_NAME = "chat_history.json"

    data class Message(
        val role: String,
        val content: String,
        val ts: Long,
    )

    fun load(context: Context): List<Message> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        val text = runCatching { file.readText() }.getOrNull() ?: return emptyList()
        return runCatching {
            val arr = JSONArray(text)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Message(
                            role = o.optString("role"),
                            content = o.optString("content"),
                            ts = o.optLong("ts"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, messages: List<Message>) {
        val file = File(context.filesDir, FILE_NAME)
        val arr = JSONArray()
        for (m in messages) {
            arr.put(
                JSONObject()
                    .put("role", m.role)
                    .put("content", m.content)
                    .put("ts", m.ts)
            )
        }
        file.writeText(arr.toString())
    }

    fun clear(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.delete()
    }
}
