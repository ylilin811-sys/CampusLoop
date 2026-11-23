package com.example.homework2.data.local.dao

import androidx.room.*
import com.example.homework2.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE receiverId = :userId ORDER BY createdAt DESC")
    fun getMessagesForUser(userId: Long = 1): Flow<List<MessageEntity>>

    @Query("SELECT COUNT(*) FROM messages WHERE receiverId = :userId AND isRead = 0")
    fun getUnreadCount(userId: Long = 1): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE messages SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE messages SET isRead = 1 WHERE receiverId = :userId")
    suspend fun markAllAsRead(userId: Long = 1)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM messages WHERE receiverId = :userId")
    suspend fun deleteAllMessages(userId: Long = 1)
}
