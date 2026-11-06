package com.lo.seribudy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM scheduled_messages ORDER BY hour ASC, minute ASC")
    fun getAllMessages(): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE isActive = 1 ORDER BY hour ASC, minute ASC")
    fun getActiveMessages(): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE isActive = 1")
    suspend fun getActiveMessagesList(): List<ScheduledMessage>

    @Query("SELECT * FROM scheduled_messages WHERE id = :id")
    suspend fun getMessageById(id: Long): ScheduledMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ScheduledMessage): Long

    @Update
    suspend fun update(message: ScheduledMessage)

    @Delete
    suspend fun delete(message: ScheduledMessage)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE scheduled_messages SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)

    @Query("UPDATE scheduled_messages SET status = :status, lastSentAt = :sentAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: MessageStatus, sentAt: Long = System.currentTimeMillis())
}
