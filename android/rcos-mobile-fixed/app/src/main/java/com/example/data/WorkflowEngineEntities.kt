package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// ==========================================
// ROOM DATABASE ENTITIES
// ==========================================

@Entity(tableName = "workflow_templates")
data class WorkflowTemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val description: String,
    val assignedAgent: String,
    val triggerJson: String,
    val approvalPolicyJson: String,
    val stepsJson: String,
    val isActive: Boolean = true,
    val totalExecutions: Int = 12
)

@Entity(tableName = "workflow_triggers")
data class WorkflowTriggerEntity(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val description: String,
    val configuration: String,
    val isActive: Boolean = true
)

@Entity(tableName = "approval_items")
data class ApprovalItemEntity(
    @PrimaryKey val id: String,
    val workflowTitle: String,
    val requestedByAgent: String,
    val triggerSource: String,
    val timestamp: String,
    val riskScorePct: Int,
    val summary: String,
    val proposedAction: String,
    val status: String = "PENDING",
    val reviewedBy: String? = null,
    val reviewNotes: String? = null
)

@Entity(tableName = "ai_action_logs")
data class AiActionLogEntity(
    @PrimaryKey val id: String,
    val timestamp: String,
    val agentName: String,
    val workflowTitle: String,
    val triggerType: String,
    val actionSummary: String,
    val approvalStatus: String,
    val executionTimeMs: Long,
    val outputArtifact: String
)

@Entity(tableName = "agent_responsibilities")
data class AgentResponsibilityEntity(
    @PrimaryKey val id: String,
    val agentName: String,
    val roleTitle: String,
    val department: String,
    val keyResponsibilitiesJson: String,
    val autonomyLevel: String,
    val activeWorkflowsCount: Int,
    val actionsExecutedToday: Int
)

@Entity(tableName = "business_workflow_configs")
data class BusinessWorkflowConfigEntity(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "RCOS Global Solutions",
    val industry: String = "Enterprise Technology & Advisory",
    val operationalBottleneck: String = "Manual Client Onboarding & Billing Approvals",
    val workloadReductionGoalPct: Int = 85,
    val autoApprovalThresholdDollars: Double = 2500.0,
    val maxRiskAutoApprovePct: Int = 20,
    val webhookNotificationUrl: String = "https://api.rcos.ai/hooks/v1/workflow-events",
    val emergencyHumanOverride: Boolean = true
)

// ==========================================
// JSON CONVERTERS (MOSHI)
// ==========================================

object WorkflowConverters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val triggerAdapter = moshi.adapter(TaskTrigger::class.java)
    private val approvalPolicyAdapter = moshi.adapter(ApprovalRequirement::class.java)
    private val stepsListAdapter = moshi.adapter<List<WorkflowStep>>(
        Types.newParameterizedType(List::class.java, WorkflowStep::class.java)
    )
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    fun triggerToJson(trigger: TaskTrigger): String = triggerAdapter.toJson(trigger)
    fun triggerFromJson(json: String): TaskTrigger {
        return try {
            triggerAdapter.fromJson(json) ?: defaultTaskTrigger()
        } catch (e: Exception) {
            defaultTaskTrigger()
        }
    }

    fun approvalPolicyToJson(policy: ApprovalRequirement): String = approvalPolicyAdapter.toJson(policy)
    fun approvalPolicyFromJson(json: String): ApprovalRequirement {
        return try {
            approvalPolicyAdapter.fromJson(json) ?: ApprovalRequirement("default", ApprovalLevel.AUTO_APPROVE)
        } catch (e: Exception) {
            ApprovalRequirement("default", ApprovalLevel.AUTO_APPROVE)
        }
    }

    fun stepsToJson(steps: List<WorkflowStep>): String = stepsListAdapter.toJson(steps)
    fun stepsFromJson(json: String): List<WorkflowStep> {
        return try {
            stepsListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun stringListToJson(list: List<String>): String = stringListAdapter.toJson(list)
    fun stringListFromJson(json: String): List<String> {
        return try {
            stringListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun defaultTaskTrigger() = TaskTrigger(
        id = "trig_def",
        type = TriggerType.MANUAL_DISPATCH,
        name = "Manual Trigger",
        description = "Standard trigger",
        configuration = "Manual"
    )
}

// ==========================================
// DOMAIN <-> ENTITY EXTENSION MAPPERS
// ==========================================

fun WorkflowTemplateEntity.toDomain(): WorkflowTemplate {
    return WorkflowTemplate(
        id = id,
        title = title,
        category = category,
        description = description,
        assignedAgent = assignedAgent,
        trigger = WorkflowConverters.triggerFromJson(triggerJson),
        approvalPolicy = WorkflowConverters.approvalPolicyFromJson(approvalPolicyJson),
        steps = WorkflowConverters.stepsFromJson(stepsJson),
        isActive = isActive,
        totalExecutions = totalExecutions
    )
}

fun WorkflowTemplate.toEntity(): WorkflowTemplateEntity {
    return WorkflowTemplateEntity(
        id = id,
        title = title,
        category = category,
        description = description,
        assignedAgent = assignedAgent,
        triggerJson = WorkflowConverters.triggerToJson(trigger),
        approvalPolicyJson = WorkflowConverters.approvalPolicyToJson(approvalPolicy),
        stepsJson = WorkflowConverters.stepsToJson(steps),
        isActive = isActive,
        totalExecutions = totalExecutions
    )
}

fun WorkflowTriggerEntity.toDomain(): TaskTrigger {
    return TaskTrigger(
        id = id,
        type = try { TriggerType.valueOf(type) } catch (e: Exception) { TriggerType.MANUAL_DISPATCH },
        name = name,
        description = description,
        configuration = configuration,
        isActive = isActive
    )
}

fun TaskTrigger.toEntity(): WorkflowTriggerEntity {
    return WorkflowTriggerEntity(
        id = id,
        type = type.name,
        name = name,
        description = description,
        configuration = configuration,
        isActive = isActive
    )
}

fun ApprovalItemEntity.toDomain(): ApprovalItem {
    return ApprovalItem(
        id = id,
        workflowTitle = workflowTitle,
        requestedByAgent = requestedByAgent,
        triggerSource = triggerSource,
        timestamp = timestamp,
        riskScorePct = riskScorePct,
        summary = summary,
        proposedAction = proposedAction,
        status = try { ApprovalStatus.valueOf(status) } catch (e: Exception) { ApprovalStatus.PENDING },
        reviewedBy = reviewedBy,
        reviewNotes = reviewNotes
    )
}

fun ApprovalItem.toEntity(): ApprovalItemEntity {
    return ApprovalItemEntity(
        id = id,
        workflowTitle = workflowTitle,
        requestedByAgent = requestedByAgent,
        triggerSource = triggerSource,
        timestamp = timestamp,
        riskScorePct = riskScorePct,
        summary = summary,
        proposedAction = proposedAction,
        status = status.name,
        reviewedBy = reviewedBy,
        reviewNotes = reviewNotes
    )
}

fun AiActionLogEntity.toDomain(): AiActionLog {
    return AiActionLog(
        id = id,
        timestamp = timestamp,
        agentName = agentName,
        workflowTitle = workflowTitle,
        triggerType = triggerType,
        actionSummary = actionSummary,
        approvalStatus = approvalStatus,
        executionTimeMs = executionTimeMs,
        outputArtifact = outputArtifact
    )
}

fun AiActionLog.toEntity(): AiActionLogEntity {
    return AiActionLogEntity(
        id = id,
        timestamp = timestamp,
        agentName = agentName,
        workflowTitle = workflowTitle,
        triggerType = triggerType,
        actionSummary = actionSummary,
        approvalStatus = approvalStatus,
        executionTimeMs = executionTimeMs,
        outputArtifact = outputArtifact
    )
}

fun AgentResponsibilityEntity.toDomain(): AgentResponsibility {
    return AgentResponsibility(
        id = id,
        agentName = agentName,
        roleTitle = roleTitle,
        department = department,
        keyResponsibilities = WorkflowConverters.stringListFromJson(keyResponsibilitiesJson),
        autonomyLevel = autonomyLevel,
        activeWorkflowsCount = activeWorkflowsCount,
        actionsExecutedToday = actionsExecutedToday
    )
}

fun AgentResponsibility.toEntity(): AgentResponsibilityEntity {
    return AgentResponsibilityEntity(
        id = id,
        agentName = agentName,
        roleTitle = roleTitle,
        department = department,
        keyResponsibilitiesJson = WorkflowConverters.stringListToJson(keyResponsibilities),
        autonomyLevel = autonomyLevel,
        activeWorkflowsCount = activeWorkflowsCount,
        actionsExecutedToday = actionsExecutedToday
    )
}

fun BusinessWorkflowConfigEntity.toDomain(): BusinessWorkflowConfig {
    return BusinessWorkflowConfig(
        companyName = companyName,
        industry = industry,
        operationalBottleneck = operationalBottleneck,
        workloadReductionGoalPct = workloadReductionGoalPct,
        autoApprovalThresholdDollars = autoApprovalThresholdDollars,
        maxRiskAutoApprovePct = maxRiskAutoApprovePct,
        webhookNotificationUrl = webhookNotificationUrl,
        emergencyHumanOverride = emergencyHumanOverride
    )
}

fun BusinessWorkflowConfig.toEntity(): BusinessWorkflowConfigEntity {
    return BusinessWorkflowConfigEntity(
        id = 1,
        companyName = companyName,
        industry = industry,
        operationalBottleneck = operationalBottleneck,
        workloadReductionGoalPct = workloadReductionGoalPct,
        autoApprovalThresholdDollars = autoApprovalThresholdDollars,
        maxRiskAutoApprovePct = maxRiskAutoApprovePct,
        webhookNotificationUrl = webhookNotificationUrl,
        emergencyHumanOverride = emergencyHumanOverride
    )
}
