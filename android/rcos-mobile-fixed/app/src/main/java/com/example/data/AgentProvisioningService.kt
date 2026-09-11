package com.example.data

import java.util.UUID

object AgentProvisioningService {

    /**
     * Provisions recommended AI agents for a workspace based on the chosen industry template and package type.
     */
    suspend fun provisionAgentsForWorkspace(
        workspaceId: String,
        industryType: IndustryType,
        selectedPackageName: String,
        createdByActor: String,
        repository: NovaRepository
    ): List<AgentRegistryEntity> {
        val template = IndustryTemplate.getTemplate(industryType)
        val createdAgents = mutableListOf<AgentRegistryEntity>()

        for (spec in template.recommendedAgents) {
            val agentId = "ag_${workspaceId.replace("ws_", "")}_${UUID.randomUUID().toString().take(6)}"
            val agentEntity = AgentRegistryEntity(
                agentId = agentId,
                workspaceId = workspaceId,
                agentName = spec.agentName,
                agentDescription = spec.agentDescription,
                agentType = spec.agentType.name,
                status = AgentStatus.ACTIVE.name,
                capabilityProfile = spec.capabilityProfile,
                permissionLevel = spec.permissionLevel.name,
                riskClassification = spec.riskClassification.name,
                modelTier = spec.modelTier,
                createdBy = createdByActor,
                createdTimestamp = System.currentTimeMillis(),
                updatedTimestamp = System.currentTimeMillis(),
                assignedDepartment = spec.department,
                assignedUsers = "All Authorized Workspace Members"
            )

            // Save agent in registry
            repository.insertAgent(agentEntity)
            createdAgents.add(agentEntity)

            // Record audit event for agent creation
            val auditEvent = AuditLogEntity(
                auditId = "aud_${UUID.randomUUID().toString().take(8)}",
                workspaceId = workspaceId,
                userId = null,
                actorType = "SYSTEM",
                actorName = createdByActor,
                actionType = "AGENTS_PROVISIONED",
                resourceType = "AI_AGENT",
                resourceId = agentId,
                agentId = agentId,
                timestamp = System.currentTimeMillis(),
                description = "Provisioned agent '${spec.agentName}' (${spec.agentType.name}) for package '$selectedPackageName' with risk classification '${spec.riskClassification.name}'.",
                newValue = "AgentStatus: ACTIVE, Risk: ${spec.riskClassification.name}, Access: ${spec.permissionLevel.name}",
                approvalRequired = false,
                approvalStatus = "AUTO_APPROVED",
                result = "SUCCESS",
                riskLevel = if (spec.riskClassification == AgentRiskLevel.CRITICAL || spec.riskClassification == AgentRiskLevel.HIGH)
                    AuditRiskLevel.HIGH.name else AuditRiskLevel.LOW.name
            )
            repository.recordAuditEvent(auditEvent)
        }

        return createdAgents
    }
}
