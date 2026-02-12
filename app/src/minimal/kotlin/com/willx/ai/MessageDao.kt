package com.willx.ai

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY ts DESC")
    fun getAllMessages(): Flow<List<ChatHistoryStore.Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatHistoryStore.Message>)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}
