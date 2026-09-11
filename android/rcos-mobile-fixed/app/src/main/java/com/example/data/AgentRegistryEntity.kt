package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class AgentType(val label: String, val defaultCapabilities: List<String>) {
    EXECUTIVE_AGENT("Executive Agent", listOf("READ_DATA", "ANALYZE_DATA", "GENERATE_REPORTS", "REQUEST_APPROVAL")),
    OPERATIONS_AGENT("Operations Agent", listOf("READ_DATA", "EXECUTE_WORKFLOW", "CREATE_WORKFLOW", "SEND_COMMUNICATION")),
    FINANCE_AGENT("Finance Agent", listOf("READ_DATA", "ACCESS_FINANCIAL_DATA", "ANALYZE_DATA", "REQUEST_APPROVAL")),
    HR_AGENT("HR Agent", listOf("READ_DATA", "MODIFY_RECORDS", "SEND_COMMUNICATION")),
    CUSTOMER_SUPPORT_AGENT("Customer Support Agent", listOf("READ_DATA", "SEND_COMMUNICATION", "MODIFY_RECORDS")),
    COMPLIANCE_AGENT("Compliance Agent", listOf("READ_DATA", "ANALYZE_DATA", "GENERATE_REPORTS")),
    ANALYTICS_AGENT("Analytics Agent", listOf("READ_DATA", "ANALYZE_DATA", "GENERATE_REPORTS")),
    CUSTOM_AGENT("Custom Agent", listOf("READ_DATA", "ANALYZE_DATA"))
}

enum class AgentStatus(val label: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended"),
    PENDING_APPROVAL("Pending Approval"),
    ARCHIVED("Archived")
}

enum class AgentRiskLevel(val label: String) {
    LOW("Low Risk"),
    MEDIUM("Medium Risk"),
    HIGH("High Risk"),
    CRITICAL("Critical Risk")
}

enum class AgentCapability(val label: String, val description: String) {
    READ_DATA("Read Data", "Access workspace records and system state"),
    ANALYZE_DATA("Analyze Data", "Perform statistical modeling and intelligence analysis"),
    GENERATE_REPORTS("Generate Reports", "Create summaries and executive dashboards"),
    CREATE_WORKFLOW("Create Workflow", "Design new automation workflow pipelines"),
    EXECUTE_WORKFLOW("Execute Workflow", "Trigger and execute workflow pipelines"),
    REQUEST_APPROVAL("Request Approval", "Queue high-risk operations for human approval"),
    SEND_COMMUNICATION("Send Communication", "Dispatch emails, notifications, or messages"),
    MODIFY_RECORDS("Modify Records", "Update user, client, or operational data"),
    ACCESS_FINANCIAL_DATA("Access Financial Data", "Inspect billing, payments, and financial logs")
}

@Entity(
    tableName = "agent_registry",
    indices = [
        Index("workspaceId"),
        Index("agentType"),
        Index("status")
    ]
)
data class AgentRegistryEntity(
    @PrimaryKey
    val agentId: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val agentName: String,
    val agentDescription: String,
    val agentType: String = AgentType.OPERATIONS_AGENT.name,
    val status: String = AgentStatus.ACTIVE.name,
    val capabilityProfile: String = "", // Comma-separated list of capabilities
    val permissionLevel: String = AccessLevel.READ_ONLY.name,
    val riskClassification: String = AgentRiskLevel.LOW.name,
    val modelTier: String = "GEMINI_2_5_FLASH",
    val createdBy: String = "SYSTEM",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val assignedDepartment: String? = null,
    val assignedUsers: String? = null // Comma-separated user IDs or emails
) {
    fun getCapabilitiesList(): List<String> {
        if (capabilityProfile.isBlank()) return emptyList()
        return capabilityProfile.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun hasCapability(capability: String): Boolean {
        return getCapabilitiesList().contains(capability)
    }

    fun canExecute(): Boolean {
        return status == AgentStatus.ACTIVE.name
    }
}
