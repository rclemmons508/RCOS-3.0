package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditEvent(event: AuditLogEntity)

    @Query("SELECT * FROM audit_logs WHERE workspaceId = :workspaceId ORDER BY timestamp DESC")
    fun getWorkspaceAuditEvents(workspaceId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE workspaceId = :workspaceId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentWorkspaceAuditEvents(workspaceId: String, limit: Int = 100): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE workspaceId = :workspaceId AND userId = :userId ORDER BY timestamp DESC")
    fun getAuditEventsByUser(workspaceId: String, userId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE workspaceId = :workspaceId AND agentId = :agentId ORDER BY timestamp DESC")
    fun getAuditEventsByAgent(workspaceId: String, agentId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE workspaceId = :workspaceId AND workflowId = :workflowId ORDER BY timestamp DESC")
    fun getAuditEventsByWorkflow(workspaceId: String, workflowId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE workspaceId = :workspaceId AND actionType = :actionType ORDER BY timestamp DESC")
    fun getAuditEventsByActionType(workspaceId: String, actionType: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE workspaceId = :workspaceId AND approvalStatus = :approvalStatus ORDER BY timestamp DESC")
    fun getAuditEventsByApprovalStatus(workspaceId: String, approvalStatus: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE workspaceId = :workspaceId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getAuditEventsByTimeRange(workspaceId: String, startTime: Long, endTime: Long): Flow<List<AuditLogEntity>>
}
