package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {

    // Workflow Templates
    @Query("SELECT * FROM workflow_templates")
    fun getAllWorkflowTemplates(): Flow<List<WorkflowTemplateEntity>>

    @Query("SELECT COUNT(*) FROM workflow_templates")
    suspend fun getWorkflowTemplatesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflowTemplate(template: WorkflowTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflowTemplates(templates: List<WorkflowTemplateEntity>)

    @Query("UPDATE workflow_templates SET isActive = NOT isActive WHERE id = :templateId")
    suspend fun toggleWorkflowActive(templateId: String)

    @Query("UPDATE workflow_templates SET totalExecutions = totalExecutions + 1 WHERE id = :templateId")
    suspend fun incrementTemplateExecution(templateId: String)

    // Workflow Triggers
    @Query("SELECT * FROM workflow_triggers")
    fun getAllWorkflowTriggers(): Flow<List<WorkflowTriggerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflowTrigger(trigger: WorkflowTriggerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflowTriggers(triggers: List<WorkflowTriggerEntity>)

    // Approval Items
    @Query("SELECT * FROM approval_items ORDER BY id DESC")
    fun getAllApprovalItems(): Flow<List<ApprovalItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApprovalItem(item: ApprovalItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApprovalItems(items: List<ApprovalItemEntity>)

    @Query("UPDATE approval_items SET status = :status, reviewedBy = :reviewedBy, reviewNotes = :reviewNotes WHERE id = :id")
    suspend fun updateApprovalStatus(id: String, status: String, reviewedBy: String?, reviewNotes: String?)

    // AI Action Logs
    @Query("SELECT * FROM ai_action_logs ORDER BY id DESC")
    fun getAllAiActionLogs(): Flow<List<AiActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiActionLog(log: AiActionLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiActionLogs(logs: List<AiActionLogEntity>)

    // Agent Responsibilities
    @Query("SELECT * FROM agent_responsibilities")
    fun getAllAgentResponsibilities(): Flow<List<AgentResponsibilityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgentResponsibility(agent: AgentResponsibilityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgentResponsibilities(agents: List<AgentResponsibilityEntity>)

    @Query("UPDATE agent_responsibilities SET autonomyLevel = :autonomyLevel WHERE id = :agentId")
    suspend fun updateAgentAutonomy(agentId: String, autonomyLevel: String)

    // Business Workflow Configuration
    @Query("SELECT * FROM business_workflow_configs WHERE id = 1 LIMIT 1")
    fun getBusinessWorkflowConfig(): Flow<BusinessWorkflowConfigEntity?>

    @Query("SELECT * FROM business_workflow_configs WHERE id = 1 LIMIT 1")
    suspend fun getBusinessWorkflowConfigDirect(): BusinessWorkflowConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBusinessWorkflowConfig(config: BusinessWorkflowConfigEntity)
}
