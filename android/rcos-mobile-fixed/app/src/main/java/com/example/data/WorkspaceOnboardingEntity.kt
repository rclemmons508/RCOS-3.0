package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class OnboardingStatus(val label: String, val description: String) {
    STARTED("Deployment Initiated", "Workspace deployment pipeline started"),
    COMPANY_PROFILE_COMPLETE("Company Profile Configured", "Industry, domain, and operational details set"),
    ADMIN_CREATED("Administrator Account Created", "Primary workspace admin user provisioned"),
    AI_CONFIGURATION_COMPLETE("AI Governance Configured", "Model tier, system prompts, and risk thresholds set"),
    AGENTS_PROVISIONED("AI Agents Provisioned", "Recommended AI agent package deployed to registry"),
    READY("Workspace Ready", "RCOS deployment active and operational"),
    FAILED("Deployment Failed", "Onboarding process encountered an error")
}

@Entity(tableName = "workspace_onboarding")
data class WorkspaceOnboardingEntity(
    @PrimaryKey
    val onboardingId: String = "onb_${UUID.randomUUID().toString().take(8)}",
    val workspaceId: String,
    val companyName: String,
    val industry: String,
    val companySize: String,
    val domain: String,
    val primaryBottleneck: String,
    val targetAutomationGoal: String,
    val targetReductionPercent: Int,
    val selectedAgentPackage: String,
    val governanceProfile: String,
    val createdBy: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val completedTimestamp: Long? = null,
    val onboardingStatus: String = OnboardingStatus.STARTED.name
)
