package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class AuditActorType(val label: String) {
    USER("Human User"),
    AI_AGENT("AI Agent"),
    SYSTEM("System Process"),
    WORKFLOW("Workflow Engine")
}

enum class AuditApprovalStatus(val label: String) {
    NOT_REQUIRED("Not Required"),
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    EXPIRED("Expired"),
    CANCELLED("Cancelled")
}

enum class AuditResultStatus(val label: String) {
    ALLOWED("Allowed"),
    DENIED("Denied"),
    SUCCESS("Success"),
    FAILED("Failed"),
    ERROR("Error")
}

enum class AuditRiskLevel(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical")
}

@Entity(
    tableName = "audit_logs",
    indices = [
        Index("workspaceId"),
        Index("userId"),
        Index("actionType")
    ]
)
data class AuditLogEntity(
    @PrimaryKey
    val auditId: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val userId: String? = null,
    val actorType: String = AuditActorType.USER.name,
    val actorName: String,
    val actionType: String,
    val resourceType: String? = null,
    val resourceId: String? = null,
    val agentId: String? = null,
    val workflowId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val previousValue: String? = null,
    val newValue: String? = null,
    val approvalRequired: Boolean = false,
    val approvalStatus: String = AuditApprovalStatus.NOT_REQUIRED.name,
    val result: String = AuditResultStatus.SUCCESS.name,
    val riskLevel: String = AuditRiskLevel.LOW.name
)
