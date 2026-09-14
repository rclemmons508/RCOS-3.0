package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Dashboard Items
    @Query("SELECT * FROM dashboard_items ORDER BY isPinned DESC, timestamp DESC")
    fun getAllDashboardItems(): Flow<List<DashboardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboardItem(item: DashboardItem): Long

    @Query("DELETE FROM dashboard_items WHERE id = :id")
    suspend fun deleteDashboardItemById(id: Int)

    @Query("UPDATE dashboard_items SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinnedStatus(id: Int, isPinned: Boolean)

    @Query("SELECT COUNT(*) FROM dashboard_items")
    fun getDashboardItemsCount(): Flow<Int>

    // Chat Sessions & Messages
    @Query("SELECT * FROM chat_sessions ORDER BY lastUpdated DESC")
    fun getAllChatSessions(): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSessionEntity)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteChatSession(sessionId: String)

    // User Accounts & Authentication
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    // Company Profile & RCOS Customization
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompanyProfile(profile: CompanyProfileEntity)

    @Query("SELECT * FROM company_profiles WHERE id = 1 LIMIT 1")
    fun getCompanyProfile(): Flow<CompanyProfileEntity?>

    @Query("SELECT * FROM company_profiles WHERE id = 1 LIMIT 1")
    suspend fun getCompanyProfileDirect(): CompanyProfileEntity?
}
