package com.example.data

/**
 * Enterprise Permission Action Types in RCOS.
 */
enum class PermissionAction(val label: String, val description: String) {
    VIEW_WORKSPACE("View Workspace", "Access workspace operational dashboard and metadata"),
    MODIFY_WORKSPACE("Modify Workspace", "Update company profile, domain, and workspace settings"),
    MANAGE_USERS("Manage Users", "Add or remove team accounts from the workspace"),
    MODIFY_USER_ROLES("Modify User Roles", "Change user roles and access levels"),
    MODIFY_AI_CONFIGURATION("Modify AI Configuration", "Update system prompt, model tier, and agent settings"),
    MODIFY_AI_GOVERNANCE("Modify AI Governance", "Adjust auto-approval risk thresholds and financial limits"),
    VIEW_AUDIT_LOGS("View Audit Logs", "Access compliance logs, AI action traces, and approval history"),
    EXECUTE_WORKFLOW("Execute Workflow", "Trigger operational AI workflow pipelines"),
    APPROVE_WORKFLOW("Approve Workflow", "Authorize high-risk or elevated workflows"),
    EXECUTE_AGENT_ACTION("Execute Agent Action", "Dispatch autonomous agent tasks"),
    APPROVE_AGENT_ACTION("Approve Agent Action", "Sign off on autonomous agent proposed actions"),
    VIEW_FINANCIAL_ACTION("View Financial Action", "Inspect financial approvals and transaction logs"),
    APPROVE_FINANCIAL_ACTION("Approve Financial Action", "Authorize financial actions exceeding standard thresholds"),
    MANAGE_AGENTS("Manage AI Agents", "Register, update, enable or disable AI agent entities and capabilities"),
    VIEW_AGENTS("View AI Agents", "Inspect registered workspace AI agents and capabilities"),
    MANAGE_ONBOARDING("Manage Workspace Onboarding", "Configure and execute workspace deployment and provisioning"),
    VIEW_ONBOARDING("View Workspace Onboarding", "Inspect workspace deployment history and onboarding state")
}

/**
 * Result of a permission authorization evaluation.
 */
sealed class AuthorizationResult {
    data object Allowed : AuthorizationResult() {
        override val isAllowed: Boolean = true
        override val reason: String = "Action authorized."
    }

    data class NotAuthenticated(
        override val reason: String = "No authenticated user context provided."
    ) : AuthorizationResult() {
        override val isAllowed: Boolean = false
    }

    data class NotAMember(
        val userWorkspaceId: String,
        val targetWorkspaceId: String,
        override val reason: String = "User belongs to workspace '$userWorkspaceId' and cannot access '$targetWorkspaceId'."
    ) : AuthorizationResult() {
        override val isAllowed: Boolean = false
    }

    data class InsufficientRole(
        val requiredRoles: List<UserRole>,
        val actualRole: UserRole,
        override val reason: String = "Role '${actualRole.label}' lacks permission. Required: ${requiredRoles.joinToString { it.label }}."
    ) : AuthorizationResult() {
        override val isAllowed: Boolean = false
    }

    data class InsufficientAccessLevel(
        val requiredAccess: AccessLevel,
        val actualAccess: AccessLevel,
        override val reason: String = "Access level '${actualAccess.label}' is insufficient. Required: '${requiredAccess.label}'."
    ) : AuthorizationResult() {
        override val isAllowed: Boolean = false
    }

    data class GovernanceRestriction(
        override val reason: String = "Operation violates workspace AI governance parameters or security policies."
    ) : AuthorizationResult() {
        override val isAllowed: Boolean = false
    }

    data class ApprovalRequired(
        val riskScorePct: Int,
        val dollarAmount: Double,
        override val reason: String = "Operation exceeds auto-approval limits (Risk: $riskScorePct%, Amount: $$dollarAmount) and requires executive sign-off."
    ) : AuthorizationResult() {
        override val isAllowed: Boolean = false
    }

    abstract val isAllowed: Boolean
    abstract val reason: String
}

/**
 * Production Permission Enforcement Engine for RCOS multi-tenant workspace architecture.
 */
object PermissionEngine {

    /**
     * Evaluates whether a user is authorized to perform a specific action within a target workspace.
     */
    fun evaluatePermission(
        user: UserAccount?,
        targetWorkspaceId: String,
        action: PermissionAction,
        targetUserId: String? = null,
        riskScorePct: Int = 0,
        dollarAmount: Double = 0.0,
        aiConfig: BusinessAiConfig? = null
    ): AuthorizationResult {
        // 1. Authenticated User Check
        if (user == null) {
            return AuthorizationResult.NotAuthenticated()
        }

        // 2. Workspace Isolation Check
        if (user.workspaceId != targetWorkspaceId) {
            return AuthorizationResult.NotAMember(
                userWorkspaceId = user.workspaceId,
                targetWorkspaceId = targetWorkspaceId
            )
        }

        // 3. Prevent Self-Elevation / Self-Role Modification Vulnerabilities
        if ((action == PermissionAction.MODIFY_USER_ROLES || action == PermissionAction.MANAGE_USERS) &&
            targetUserId != null && targetUserId == user.userId && user.role != UserRole.ADMIN
        ) {
            return AuthorizationResult.GovernanceRestriction(
                reason = "Non-admin users cannot modify their own permissions or user account status."
            )
        }

        // 3.5. Super Admin Override
        // Give full unrestricted authorization to the executive admin for all system features
        if (user.role == UserRole.ADMIN && user.accessLevel == AccessLevel.FULL_CONTROL) {
            return AuthorizationResult.Allowed
        }

        // 4. Role & Access Level Matrix Evaluation
        return when (action) {
            PermissionAction.VIEW_WORKSPACE -> {
                AuthorizationResult.Allowed
            }

            PermissionAction.VIEW_AUDIT_LOGS -> {
                if (user.role in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.AUDITOR)) {
                    AuthorizationResult.Allowed
                } else {
                    AuthorizationResult.InsufficientRole(
                        requiredRoles = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.AUDITOR),
                        actualRole = user.role
                    )
                }
            }

            PermissionAction.MODIFY_WORKSPACE -> {
                if (user.role == UserRole.ADMIN) {
                    if (user.accessLevel == AccessLevel.FULL_CONTROL) {
                        AuthorizationResult.Allowed
                    } else {
                        AuthorizationResult.InsufficientAccessLevel(
                            requiredAccess = AccessLevel.FULL_CONTROL,
                            actualAccess = user.accessLevel
                        )
                    }
                } else {
                    AuthorizationResult.InsufficientRole(
                        requiredRoles = listOf(UserRole.ADMIN),
                        actualRole = user.role
                    )
                }
            }

            PermissionAction.MANAGE_USERS, PermissionAction.MODIFY_USER_ROLES -> {
                if (user.role == UserRole.ADMIN) {
                    AuthorizationResult.Allowed
                } else {
                    AuthorizationResult.InsufficientRole(
                        requiredRoles = listOf(UserRole.ADMIN),
                        actualRole = user.role
                    )
                }
            }

            PermissionAction.MODIFY_AI_CONFIGURATION, PermissionAction.MODIFY_AI_GOVERNANCE -> {
                if (user.role == UserRole.ADMIN) {
                    AuthorizationResult.Allowed
                } else {
                    AuthorizationResult.InsufficientRole(
                        requiredRoles = listOf(UserRole.ADMIN),
                        actualRole = user.role
                    )
                }
            }

            PermissionAction.EXECUTE_WORKFLOW, PermissionAction.EXECUTE_AGENT_ACTION -> {
                if (user.role == UserRole.AUDITOR) {
                    return AuthorizationResult.InsufficientRole(
                        requiredRoles = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.EMPLOYEE),
                        actualRole = user.role
                    )
                }

                if (aiConfig != null) {
                    val maxRisk = aiConfig.allowedAutoApprovalRiskThreshold
                    val maxAmount = aiConfig.autoApprovalDollarLimit

                    val exceedsRisk = riskScorePct > maxRisk
                    val exceedsAmount = dollarAmount > maxAmount

                    if (exceedsRisk || exceedsAmount) {
                        if (user.role == UserRole.ADMIN || (user.role == UserRole.MANAGER && user.accessLevel == AccessLevel.WORKFLOW_ADMIN)) {
                            AuthorizationResult.Allowed
                        } else {
                            return AuthorizationResult.ApprovalRequired(
                                riskScorePct = riskScorePct,
                                dollarAmount = dollarAmount
                            )
                        }
                    } else {
                        AuthorizationResult.Allowed
                    }
                } else {
                    AuthorizationResult.Allowed
                }
            }

            PermissionAction.APPROVE_WORKFLOW, PermissionAction.APPROVE_AGENT_ACTION,
            PermissionAction.APPROVE_FINANCIAL_ACTION -> {
                when (user.role) {
                    UserRole.ADMIN -> AuthorizationResult.Allowed
                    UserRole.MANAGER -> {
                        if (user.accessLevel == AccessLevel.WORKFLOW_ADMIN || user.accessLevel == AccessLevel.FULL_CONTROL) {
                            if (aiConfig != null && dollarAmount > aiConfig.autoApprovalDollarLimit * 2 && user.role != UserRole.ADMIN) {
                                AuthorizationResult.GovernanceRestriction(
                                    reason = "Dollar amount ($$dollarAmount) exceeds Manager approval ceiling ($${aiConfig.autoApprovalDollarLimit * 2}). Requires Admin approval."
                                )
                            } else {
                                AuthorizationResult.Allowed
                            }
                        } else {
                            AuthorizationResult.InsufficientAccessLevel(
                                requiredAccess = AccessLevel.WORKFLOW_ADMIN,
                                actualAccess = user.accessLevel
                            )
                        }
                    }
                    else -> AuthorizationResult.InsufficientRole(
                        requiredRoles = listOf(UserRole.ADMIN, UserRole.MANAGER),
                        actualRole = user.role
                    )
                }
            }

            PermissionAction.VIEW_FINANCIAL_ACTION -> {
                if (user.role in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.AUDITOR)) {
                    AuthorizationResult.Allowed
                } else {
                    AuthorizationResult.InsufficientRole(
                        requiredRoles = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.AUDITOR),
                        actualRole = user.role
                    )
                }
            }

            PermissionAction.MANAGE_AGENTS, PermissionAction.MANAGE_ONBOARDING -> {
                if (user.role == UserRole.ADMIN) {
                    AuthorizationResult.Allowed
                } else {
                    AuthorizationResult.InsufficientRole(
                        requiredRoles = listOf(UserRole.ADMIN),
                        actualRole = user.role
                    )
                }
            }

            PermissionAction.VIEW_AGENTS, PermissionAction.VIEW_ONBOARDING -> {
                AuthorizationResult.Allowed
            }
        }
    }
}
