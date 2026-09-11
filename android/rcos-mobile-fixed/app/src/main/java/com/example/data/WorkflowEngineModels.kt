package com.example.data

enum class TriggerType {
    INCOMING_CALL,
    EMAIL_RECEIVED,
    SCHEDULED_CRON,
    THRESHOLD_EVENT,
    MANUAL_DISPATCH
}

enum class ApprovalLevel {
    AUTO_APPROVE,
    REQUIRED_IF_HIGH_RISK,
    ALWAYS_REQUIRED
}

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    COMPLETED,
    ARCHIVED,
    REJECTED,
    EDITED
}

data class TaskTrigger(
    val id: String,
    val type: TriggerType,
    val name: String,
    val description: String,
    val configuration: String,
    val isActive: Boolean = true
)

data class ApprovalRequirement(
    val id: String,
    val level: ApprovalLevel,
    val approverRole: String = "Chief Executive Officer",
    val riskThresholdPct: Int = 25,
    val autoApproveCondition: String = "Risk score < 25% or value < $1,000"
)

data class WorkflowStep(
    val stepNumber: Int,
    val title: String,
    val actionType: String,
    val assignedAgent: String,
    val isCompleted: Boolean = false
)

data class WorkflowTemplate(
    val id: String,
    val title: String,
    val category: String, // e.g. "Client Onboarding", "Finance", "Voice Operations", "Executive Strategy"
    val description: String,
    val assignedAgent: String,
    val trigger: TaskTrigger,
    val approvalPolicy: ApprovalRequirement,
    val steps: List<WorkflowStep>,
    val isActive: Boolean = true,
    val totalExecutions: Int = 12
)

data class ApprovalItem(
    val id: String,
    val workflowTitle: String,
    val requestedByAgent: String,
    val triggerSource: String,
    val timestamp: String,
    val riskScorePct: Int, // e.g. 15% or 65%
    val summary: String,
    val proposedAction: String,
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val reviewedBy: String? = null,
    val reviewNotes: String? = null
)

data class AiActionLog(
    val id: String,
    val timestamp: String,
    val agentName: String,
    val workflowTitle: String,
    val triggerType: String,
    val actionSummary: String,
    val approvalStatus: String, // "Auto-Approved", "Human Approved", "Pending Review", "Executed"
    val executionTimeMs: Long,
    val outputArtifact: String
)

data class AgentResponsibility(
    val id: String,
    val agentName: String,
    val roleTitle: String,
    val department: String,
    val keyResponsibilities: List<String>,
    val autonomyLevel: String, // "Full Autonomy", "Human Approval Required", "Advisory Only"
    val activeWorkflowsCount: Int,
    val actionsExecutedToday: Int
)

data class BusinessWorkflowConfig(
    val companyName: String = "RCOS Global Solutions",
    val industry: String = "Enterprise Technology & Advisory",
    val operationalBottleneck: String = "Manual Client Onboarding & Billing Approvals",
    val workloadReductionGoalPct: Int = 85,
    val autoApprovalThresholdDollars: Double = 2500.0,
    val maxRiskAutoApprovePct: Int = 20,
    val webhookNotificationUrl: String = "https://api.rcos.ai/hooks/v1/workflow-events",
    val emergencyHumanOverride: Boolean = true
)
