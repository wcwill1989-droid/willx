package com.willx.ai

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

object ChatHistoryStore {
    private const val FILE_NAME = "chat_history.json"

    @Entity(tableName = "messages")
    data class Message(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val role: String,
        val content: String,
        val ts: Long,
    )

    // Room database implementation
    private var db: AppDatabase? = null

    private fun ensureDatabaseInitialized(context: Context) {
        if (db == null) {
            AppDatabase.init(context)
            db = AppDatabase.getInstance()
        }
    }

    fun getMessagesFlow(context: Context): Flow<List<Message>> {
        ensureDatabaseInitialized(context)
        return db!!.messageDao().getAllMessages()
    }

    suspend fun save(context: Context, messages: List<Message>) {
        ensureDatabaseInitialized(context)
        withContext(Dispatchers.IO) {
            db!!.messageDao().insertMessages(messages)
        }
    }

    suspend fun clear(context: Context) {
        ensureDatabaseInitialized(context)
        withContext(Dispatchers.IO) {
            db!!.messageDao().deleteAllMessages()
        }
    }
}
