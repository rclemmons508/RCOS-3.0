package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentRegistryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentRegistryEntity)

    @Update
    suspend fun updateAgent(agent: AgentRegistryEntity)

    @Query("SELECT * FROM agent_registry WHERE workspaceId = :workspaceId ORDER BY agentName ASC")
    fun getWorkspaceAgents(workspaceId: String): Flow<List<AgentRegistryEntity>>

    @Query("SELECT * FROM agent_registry WHERE workspaceId = :workspaceId AND status = 'ACTIVE' ORDER BY agentName ASC")
    fun getActiveWorkspaceAgents(workspaceId: String): Flow<List<AgentRegistryEntity>>

    @Query("SELECT * FROM agent_registry WHERE workspaceId = :workspaceId AND agentType = :agentType ORDER BY agentName ASC")
    fun getAgentsByType(workspaceId: String, agentType: String): Flow<List<AgentRegistryEntity>>

    @Query("SELECT * FROM agent_registry WHERE workspaceId = :workspaceId AND riskClassification = :riskLevel ORDER BY agentName ASC")
    fun getAgentsByRiskLevel(workspaceId: String, riskLevel: String): Flow<List<AgentRegistryEntity>>

    @Query("SELECT * FROM agent_registry WHERE agentId = :agentId")
    fun getAgentById(agentId: String): Flow<AgentRegistryEntity?>

    @Query("SELECT * FROM agent_registry WHERE agentId = :agentId")
    suspend fun getAgentByIdSync(agentId: String): AgentRegistryEntity?

    @Query("DELETE FROM agent_registry WHERE agentId = :agentId")
    suspend fun deleteAgent(agentId: String)

    @Query("SELECT COUNT(*) FROM agent_registry WHERE workspaceId = :workspaceId")
    suspend fun getAgentsCount(workspaceId: String): Int
}
