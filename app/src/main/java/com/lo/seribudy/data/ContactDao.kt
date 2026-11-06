package com.lo.seribudy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM streak_contacts ORDER BY hasStreak DESC, streakDays DESC")
    fun getAllContacts(): Flow<List<StreakContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<StreakContact>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: StreakContact)

    @Query("DELETE FROM streak_contacts WHERE username = :username")
    suspend fun deleteContact(username: String)

    @Query("SELECT COUNT(*) FROM streak_contacts")
    suspend fun getContactCount(): Int
}
