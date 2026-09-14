package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AccessLevel
import com.example.data.BusinessAiConfig
import com.example.data.UserAccount
import com.example.data.UserRole
import com.example.data.WorkspaceEntity
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiWorkspaceGovernanceCard(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val activeWorkspaceId by viewModel.activeWorkspaceId.collectAsState()
    val allWorkspaces by viewModel.allWorkspaces.collectAsState()
    val userAccounts by viewModel.activeWorkspaceUserAccounts.collectAsState()
    val aiConfig by viewModel.activeBusinessAiConfig.collectAsState()
    val selectedUser by viewModel.selectedUserAccount.collectAsState()

    val currentWorkspace = allWorkspaces.find { it.workspaceId == activeWorkspaceId }
        ?: allWorkspaces.firstOrNull()
        ?: WorkspaceEntity("ws_default", "RCOS Global Solutions", "Technology")

    var showAddWorkspaceDialog by remember { mutableStateOf(false) }
    var showOnboardingWizardDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showAiConfigDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("multi_workspace_governance_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header & Active Workspace Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Workspace",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Multi-Business Workspace SaaS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tenant Isolation • Roles & RBAC • Custom AI Rules",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showOnboardingWizardDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_onboarding_wizard_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = "Onboarding Wizard",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Onboarding Wizard", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showAddWorkspaceDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("provision_workspace_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Workspace",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Quick Add", fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Workspace Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Active Company Workspace",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allWorkspaces) { ws ->
                        val isSelected = ws.workspaceId == currentWorkspace.workspaceId
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.switchWorkspace(ws.workspaceId) },
                            label = {
                                Text(
                                    text = ws.companyName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.DomainVerification else Icons.Default.Domain,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("workspace_chip_${ws.workspaceId}")
                        )
                    }
                }
            }

            // Current Workspace Profile Summary
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentWorkspace.companyName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentWorkspace.industry} • ${currentWorkspace.domain.ifBlank { "Private Domain" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AssistChip(
                            onClick = { showAiConfigDialog = true },
                            label = { Text("AI Rules & Tone", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.testTag("configure_ai_rules_btn")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target Reduction: ${currentWorkspace.targetReductionPercent}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = RcosNeonGreen,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Active AI Copilots: ${currentWorkspace.activeAgents}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // User Accounts & Role Governance Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Team Member Accounts & Roles (${userAccounts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    TextButton(
                        onClick = { showAddUserDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("add_user_account_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add User",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Add Member", fontSize = 12.sp)
                    }
                }

                userAccounts.forEach { user ->
                    UserAccountRowItem(
                        user = user,
                        isSelected = selectedUser?.userId == user.userId,
                        onSelectProfile = { viewModel.selectUserAccount(user) },
                        onPromoteDemote = { newRole, newAccess ->
                            viewModel.updateUserRoleInWorkspace(user.userId, newRole, newAccess)
                        }
                    )
                }
            }

            // Business-Specific AI Configurations Summary
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Workspace AI Copilot Rules",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "System Prompt: \"${aiConfig.customSystemPrompt}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Model: ${aiConfig.modelTier}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tone: ${aiConfig.aiAgentTone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Auto-Approval Limit: ${aiConfig.autoApprovalDollarLimit.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RcosNeonGreen
                        )
                        Text(
                            text = "Max Auto Risk: ${aiConfig.allowedAutoApprovalRiskThreshold}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Enterprise Audit Logging & Compliance System
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            AuditLogView(viewModel = viewModel)
        }
    }

    // ==========================================
    // DIALOGS FOR PROVISIONING & WORKSPACE ADMIN
    // ==========================================

    if (showOnboardingWizardDialog) {
        WorkspaceOnboardingDialog(
            viewModel = viewModel,
            onDismiss = { showOnboardingWizardDialog = false },
            onOnboardingComplete = { newWsId ->
                showOnboardingWizardDialog = false
            }
        )
    }

    if (showAddWorkspaceDialog) {
        AddWorkspaceDialog(
            onDismiss = { showAddWorkspaceDialog = false },
            onCreate = { name, industry, domain, bottleneck, adminName, adminEmail ->
                viewModel.createWorkspace(name, industry, domain, bottleneck, adminName, adminEmail)
                showAddWorkspaceDialog = false
            }
        )
    }

    if (showAddUserDialog) {
        AddUserAccountDialog(
            onDismiss = { showAddUserDialog = false },
            onAdd = { name, email, role, accessLevel, dept ->
                viewModel.addUserToActiveWorkspace(name, email, role, accessLevel, dept)
                showAddUserDialog = false
            }
        )
    }

    if (showAiConfigDialog) {
        EditBusinessAiConfigDialog(
            currentConfig = aiConfig,
            onDismiss = { showAiConfigDialog = false },
            onSave = { updatedConfig ->
                viewModel.updateActiveWorkspaceAiConfig(updatedConfig)
                showAiConfigDialog = false
            }
        )
    }
}

@Composable
fun UserAccountRowItem(
    user: UserAccount,
    isSelected: Boolean,
    onSelectProfile: () -> Unit,
    onPromoteDemote: (UserRole, AccessLevel) -> Unit
) {
    var showRoleMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectProfile() }
            .testTag("user_row_${user.userId}")
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            try { Color(android.graphics.Color.parseColor(user.avatarColorHex)) }
                            catch (e: Exception) { MaterialTheme.colorScheme.primary },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.fullName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSelected) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = RcosNeonGreen,
                                modifier = Modifier
                                    .background(RcosNeonGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${user.email} • ${user.department}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                AssistChip(
                    onClick = { showRoleMenu = true },
                    label = { Text(user.role.label, fontSize = 11.sp) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("role_badge_${user.userId}")
                )

                DropdownMenu(
                    expanded = showRoleMenu,
                    onDismissRequest = { showRoleMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Workspace Admin (Full Control)") },
                        onClick = {
                            onPromoteDemote(UserRole.ADMIN, AccessLevel.FULL_CONTROL)
                            showRoleMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Operations Manager (Workflow Admin)") },
                        onClick = {
                            onPromoteDemote(UserRole.MANAGER, AccessLevel.WORKFLOW_ADMIN)
                            showRoleMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Employee Staff (Standard Access)") },
                        onClick = {
                            onPromoteDemote(UserRole.EMPLOYEE, AccessLevel.REGULAR_STAFF)
                            showRoleMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Auditor (Read-Only Access)") },
                        onClick = {
                            onPromoteDemote(UserRole.AUDITOR, AccessLevel.READ_ONLY)
                            showRoleMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddWorkspaceDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, industry: String, domain: String, bottleneck: String, adminName: String, adminEmail: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("Enterprise Technology") }
    var domain by remember { mutableStateOf("") }
    var bottleneck by remember { mutableStateOf("") }
    var adminName by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Provision New Multi-Tenant Workspace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Company Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("new_ws_name_input")
                )
                OutlinedTextField(
                    value = industry,
                    onValueChange = { industry = it },
                    label = { Text("Industry Sector") },
                    modifier = Modifier.fillMaxWidth().testTag("new_ws_industry_input")
                )
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Corporate Domain (e.g. acme.com)") },
                    modifier = Modifier.fillMaxWidth().testTag("new_ws_domain_input")
                )
                OutlinedTextField(
                    value = bottleneck,
                    onValueChange = { bottleneck = it },
                    label = { Text("Primary Operational Bottleneck") },
                    modifier = Modifier.fillMaxWidth().testTag("new_ws_bottleneck_input")
                )
                HorizontalDivider()
                Text("Initial Admin User Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = adminName,
                    onValueChange = { adminName = it },
                    label = { Text("Admin Full Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("new_ws_admin_name_input")
                )
                OutlinedTextField(
                    value = adminEmail,
                    onValueChange = { adminEmail = it },
                    label = { Text("Admin Corporate Email *") },
                    modifier = Modifier.fillMaxWidth().testTag("new_ws_admin_email_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && adminName.isNotBlank() && adminEmail.isNotBlank()) {
                        onCreate(name, industry, domain, bottleneck, adminName, adminEmail)
                    }
                },
                modifier = Modifier.testTag("confirm_create_ws_btn")
            ) {
                Text("Provision Workspace")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddUserAccountDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, email: String, role: UserRole, accessLevel: AccessLevel, dept: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("Operations") }
    var selectedRole by remember { mutableStateOf(UserRole.EMPLOYEE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add User Account to Workspace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("new_user_name_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Work Email *") },
                    modifier = Modifier.fillMaxWidth().testTag("new_user_email_input")
                )
                OutlinedTextField(
                    value = dept,
                    onValueChange = { dept = it },
                    label = { Text("Department") },
                    modifier = Modifier.fillMaxWidth().testTag("new_user_dept_input")
                )

                Text("Assigned Role & Permissions:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                UserRole.values().forEach { role ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRole = role }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role }
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(role.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(role.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        val access = when (selectedRole) {
                            UserRole.ADMIN -> AccessLevel.FULL_CONTROL
                            UserRole.MANAGER -> AccessLevel.WORKFLOW_ADMIN
                            UserRole.EMPLOYEE -> AccessLevel.REGULAR_STAFF
                            UserRole.AUDITOR -> AccessLevel.READ_ONLY
                        }
                        onAdd(name, email, selectedRole, access, dept)
                    }
                },
                modifier = Modifier.testTag("confirm_add_user_btn")
            ) {
                Text("Create Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditBusinessAiConfigDialog(
    currentConfig: BusinessAiConfig,
    onDismiss: () -> Unit,
    onSave: (BusinessAiConfig) -> Unit
) {
    var prompt by remember { mutableStateOf(currentConfig.customSystemPrompt) }
    var tone by remember { mutableStateOf(currentConfig.aiAgentTone) }
    var modelTier by remember { mutableStateOf(currentConfig.modelTier) }
    var autoRiskThreshold by remember { mutableStateOf(currentConfig.allowedAutoApprovalRiskThreshold.toString()) }
    var dollarLimit by remember { mutableStateOf(currentConfig.autoApprovalDollarLimit.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Business-Specific AI Configurations") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Custom System Prompt / Behavior") },
                    modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input"),
                    minLines = 2
                )
                OutlinedTextField(
                    value = tone,
                    onValueChange = { tone = it },
                    label = { Text("AI Communication Tone") },
                    modifier = Modifier.fillMaxWidth().testTag("ai_tone_input")
                )
                OutlinedTextField(
                    value = modelTier,
                    onValueChange = { modelTier = it },
                    label = { Text("AI Model Tier") },
                    modifier = Modifier.fillMaxWidth().testTag("ai_model_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = autoRiskThreshold,
                        onValueChange = { autoRiskThreshold = it },
                        label = { Text("Max Auto Risk %") },
                        modifier = Modifier.weight(1f).testTag("ai_risk_input")
                    )
                    OutlinedTextField(
                        value = dollarLimit,
                        onValueChange = { dollarLimit = it },
                        label = { Text("Auto $ Limit") },
                        modifier = Modifier.weight(1f).testTag("ai_dollar_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentConfig.copy(
                            customSystemPrompt = prompt,
                            aiAgentTone = tone,
                            modelTier = modelTier,
                            allowedAutoApprovalRiskThreshold = autoRiskThreshold.toIntOrNull() ?: 20,
                            autoApprovalDollarLimit = dollarLimit.toDoubleOrNull() ?: 5000.0
                        )
                    )
                },
                modifier = Modifier.testTag("save_ai_config_btn")
            ) {
                Text("Save AI Configuration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
