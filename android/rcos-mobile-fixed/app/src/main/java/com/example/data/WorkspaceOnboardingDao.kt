package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceOnboardingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnboarding(onboarding: WorkspaceOnboardingEntity)

    @Update
    suspend fun updateOnboarding(onboarding: WorkspaceOnboardingEntity)

    @Query("SELECT * FROM workspace_onboarding WHERE workspaceId = :workspaceId ORDER BY createdTimestamp DESC LIMIT 1")
    fun getOnboardingByWorkspace(workspaceId: String): Flow<WorkspaceOnboardingEntity?>

    @Query("SELECT * FROM workspace_onboarding WHERE workspaceId = :workspaceId ORDER BY createdTimestamp DESC LIMIT 1")
    suspend fun getOnboardingByWorkspaceSync(workspaceId: String): WorkspaceOnboardingEntity?

    @Query("SELECT * FROM workspace_onboarding WHERE onboardingId = :onboardingId")
    suspend fun getOnboardingByIdSync(onboardingId: String): WorkspaceOnboardingEntity?

    @Query("SELECT * FROM workspace_onboarding ORDER BY createdTimestamp DESC")
    fun getAllOnboardings(): Flow<List<WorkspaceOnboardingEntity>>

    @Query("DELETE FROM workspace_onboarding WHERE onboardingId = :onboardingId")
    suspend fun deleteOnboarding(onboardingId: String)

    @Query("SELECT COUNT(*) FROM workspace_onboarding WHERE workspaceId = :workspaceId")
    suspend fun getOnboardingCount(workspaceId: String): Int
}
