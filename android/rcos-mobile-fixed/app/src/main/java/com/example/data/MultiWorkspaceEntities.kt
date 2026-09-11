package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// ==========================================
// MULTI-TENANT WORKSPACE & USER ROLES DOMAIN
// ==========================================

enum class UserRole(val label: String, val description: String) {
    ADMIN("Workspace Admin", "Full control over company settings, user management, and AI rules"),
    MANAGER("Operations Manager", "Can dispatch workflows, approve high-risk items, and manage team tasks"),
    EMPLOYEE("Employee / Staff", "Can view assigned workflows, request approvals, and log activities"),
    AUDITOR("Compliance Auditor", "Read-only access to AI action logs, approval traces, and system metrics")
}

enum class AccessLevel(val label: String) {
    FULL_CONTROL("Full Administrative Control"),
    WORKFLOW_ADMIN("Workflow & Approval Admin"),
    REGULAR_STAFF("Standard Employee Access"),
    READ_ONLY("Read-Only Audit Access")
}

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val workspaceId: String,
    val companyName: String,
    val industry: String,
    val domain: String = "",
    val primaryBottleneck: String = "Manual Operations",
    val targetReductionPercent: Int = 80,
    val activeAgents: String = "Nova Ops, Executive Copilot, Billing Agent",
    val customInstructions: String = "",
    val isConfigured: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val userId: String,
    val workspaceId: String,
    val email: String,
    val fullName: String,
    val role: String, // UserRole enum name
    val accessLevel: String, // AccessLevel enum name
    val department: String = "General Operations",
    val avatarColorHex: String = "#3B82F6",
    val registeredAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "business_ai_configs")
data class BusinessAiConfigEntity(
    @PrimaryKey val workspaceId: String,
    val customSystemPrompt: String = "Operate as a high-precision enterprise automation agent.",
    val aiAgentTone: String = "Executive Briefing & Data-Driven",
    val allowedAutoApprovalRiskThreshold: Int = 20,
    val autoApprovalDollarLimit: Double = 5000.0,
    val modelTier: String = "Gemini Pro 1.5 Enterprise",
    val enableAutonomousActions: Boolean = true,
    val dataRetentionDays: Int = 90,
    val dailyApiQuota: Int = 10000
)

// ==========================================
// DOMAIN MODELS
// ==========================================

data class UserAccount(
    val userId: String,
    val workspaceId: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val accessLevel: AccessLevel,
    val department: String = "General Operations",
    val avatarColorHex: String = "#3B82F6",
    val registeredAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

data class BusinessAiConfig(
    val workspaceId: String = "ws_default",
    val customSystemPrompt: String = "Operate as a high-precision enterprise automation agent.",
    val aiAgentTone: String = "Executive Briefing & Data-Driven",
    val allowedAutoApprovalRiskThreshold: Int = 20,
    val autoApprovalDollarLimit: Double = 5000.0,
    val modelTier: String = "Gemini Pro 1.5 Enterprise",
    val enableAutonomousActions: Boolean = true,
    val dataRetentionDays: Int = 90,
    val dailyApiQuota: Int = 10000
)

// Extension Mappers
fun UserAccountEntity.toDomain(): UserAccount {
    return UserAccount(
        userId = userId,
        workspaceId = workspaceId,
        email = email,
        fullName = fullName,
        role = try { UserRole.valueOf(role) } catch (e: Exception) { UserRole.EMPLOYEE },
        accessLevel = try { AccessLevel.valueOf(accessLevel) } catch (e: Exception) { AccessLevel.REGULAR_STAFF },
        department = department,
        avatarColorHex = avatarColorHex,
        registeredAt = registeredAt,
        lastActiveAt = lastActiveAt
    )
}

fun UserAccount.toEntity(): UserAccountEntity {
    return UserAccountEntity(
        userId = userId,
        workspaceId = workspaceId,
        email = email,
        fullName = fullName,
        role = role.name,
        accessLevel = accessLevel.name,
        department = department,
        avatarColorHex = avatarColorHex,
        registeredAt = registeredAt,
        lastActiveAt = lastActiveAt
    )
}

fun BusinessAiConfigEntity.toDomain(): BusinessAiConfig {
    return BusinessAiConfig(
        workspaceId = workspaceId,
        customSystemPrompt = customSystemPrompt,
        aiAgentTone = aiAgentTone,
        allowedAutoApprovalRiskThreshold = allowedAutoApprovalRiskThreshold,
        autoApprovalDollarLimit = autoApprovalDollarLimit,
        modelTier = modelTier,
        enableAutonomousActions = enableAutonomousActions,
        dataRetentionDays = dataRetentionDays,
        dailyApiQuota = dailyApiQuota
    )
}

fun BusinessAiConfig.toEntity(): BusinessAiConfigEntity {
    return BusinessAiConfigEntity(
        workspaceId = workspaceId,
        customSystemPrompt = customSystemPrompt,
        aiAgentTone = aiAgentTone,
        allowedAutoApprovalRiskThreshold = allowedAutoApprovalRiskThreshold,
        autoApprovalDollarLimit = autoApprovalDollarLimit,
        modelTier = modelTier,
        enableAutonomousActions = enableAutonomousActions,
        dataRetentionDays = dataRetentionDays,
        dailyApiQuota = dailyApiQuota
    )
}
