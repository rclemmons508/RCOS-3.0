package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    // Workspaces
    @Query("SELECT * FROM workspaces ORDER BY createdAt DESC")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE workspaceId = :workspaceId LIMIT 1")
    fun getWorkspaceById(workspaceId: String): Flow<WorkspaceEntity?>

    @Query("SELECT * FROM workspaces WHERE workspaceId = :workspaceId LIMIT 1")
    suspend fun getWorkspaceByIdDirect(workspaceId: String): WorkspaceEntity?

    @Query("SELECT COUNT(*) FROM workspaces")
    suspend fun getWorkspacesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaces(workspaces: List<WorkspaceEntity>)

    // User Accounts per Workspace
    @Query("SELECT * FROM user_accounts WHERE workspaceId = :workspaceId ORDER BY registeredAt ASC")
    fun getUserAccountsByWorkspace(workspaceId: String): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM user_accounts ORDER BY registeredAt ASC")
    fun getAllUserAccounts(): Flow<List<UserAccountEntity>>

    @Query("SELECT COUNT(*) FROM user_accounts")
    suspend fun getUserAccountsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(user: UserAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccounts(users: List<UserAccountEntity>)

    @Query("UPDATE user_accounts SET role = :role, accessLevel = :accessLevel WHERE userId = :userId")
    suspend fun updateUserRole(userId: String, role: String, accessLevel: String)

    @Query("DELETE FROM user_accounts WHERE userId = :userId")
    suspend fun deleteUserAccount(userId: String)

    // Business AI Configs per Workspace
    @Query("SELECT * FROM business_ai_configs WHERE workspaceId = :workspaceId LIMIT 1")
    fun getBusinessAiConfig(workspaceId: String): Flow<BusinessAiConfigEntity?>

    @Query("SELECT * FROM business_ai_configs WHERE workspaceId = :workspaceId LIMIT 1")
    suspend fun getBusinessAiConfigDirect(workspaceId: String): BusinessAiConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBusinessAiConfig(config: BusinessAiConfigEntity)
}
