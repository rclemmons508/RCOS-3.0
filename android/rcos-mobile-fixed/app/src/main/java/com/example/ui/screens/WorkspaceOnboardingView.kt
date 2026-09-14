package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.example.data.AgentRiskLevel
import com.example.data.IndustryTemplate
import com.example.data.IndustryType
import com.example.data.OnboardingStatus
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceOnboardingDialog(
    viewModel: NovaViewModel,
    onDismiss: () -> Unit,
    onOnboardingComplete: (String) -> Unit = {}
) {
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 1: Company Profile
    var companyName by remember { mutableStateOf("") }
    var selectedIndustryType by remember { mutableStateOf(IndustryType.FINANCE) }
    var companySize by remember { mutableStateOf("50-250 Employees") }
    var domain by remember { mutableStateOf("") }

    // Step 2: Operational Assessment
    var primaryBottleneck by remember { mutableStateOf("Manual transaction reviews and slow compliance sign-offs") }
    var targetAutomationGoal by remember { mutableStateOf("Automate 80% of routine approval workflows and financial checks") }
    var targetReductionPercent by remember { mutableFloatStateOf(80f) }

    // Step 3: AI Governance Parameters
    val template = remember(selectedIndustryType) { IndustryTemplate.getTemplate(selectedIndustryType) }
    var aiTone by remember(template) { mutableStateOf(template.defaultAiTone) }
    var autoRiskThreshold by remember(template) { mutableIntStateOf(template.defaultAutoApprovalRiskThreshold) }
    var autoDollarLimit by remember(template) { mutableDoubleStateOf(template.defaultAutoApprovalDollarLimit) }

    // Step 4: Agent Selection Package
    var selectedAgentPackage by remember { mutableStateOf("Enterprise AI Suite") }

    // Step 5: Administrator Setup
    var adminName by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }

    // Step 6: Provisioning Execution
    var isProvisioning by remember { mutableStateOf(false) }
    var provisioningStepIndex by remember { mutableIntStateOf(0) }

    val provisioningSteps = remember {
        listOf(
            "Initializing workspace tenant database isolation...",
            "Creating RBAC permissions & user account roles...",
            "Applying business AI prompt and compliance governance rules...",
            "Provisioning industry AI agent package registry entities...",
            "Generating cryptographically verifiable audit log entries...",
            "Finalizing RCOS workspace environment deployment!"
        )
    }

    BasicAlertDialog(
        onDismissRequest = { if (!isProvisioning) onDismiss() },
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.9f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .testTag("workspace_onboarding_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Dialog Header & Close Button
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
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = "Onboarding",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "RCOS Workspace Onboarding",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Guided Enterprise Deployment & Agent Provisioning",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isProvisioning) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_onboarding_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Stepper Indicator Bar
            StepperIndicator(currentStep = currentStep, totalSteps = 6)

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Step Content Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                when (currentStep) {
                    1 -> StepCompanyProfile(
                        companyName = companyName,
                        onCompanyNameChange = { companyName = it },
                        selectedIndustryType = selectedIndustryType,
                        onIndustrySelect = { selectedIndustryType = it },
                        companySize = companySize,
                        onSizeChange = { companySize = it },
                        domain = domain,
                        onDomainChange = { domain = it }
                    )

                    2 -> StepOperationalAssessment(
                        primaryBottleneck = primaryBottleneck,
                        onBottleneckChange = { primaryBottleneck = it },
                        targetGoal = targetAutomationGoal,
                        onTargetGoalChange = { targetAutomationGoal = it },
                        reductionPercent = targetReductionPercent,
                        onReductionChange = { targetReductionPercent = it },
                        template = template
                    )

                    3 -> StepAiGovernance(
                        aiTone = aiTone,
                        onToneChange = { aiTone = it },
                        autoRiskThreshold = autoRiskThreshold,
                        onRiskChange = { autoRiskThreshold = it },
                        autoDollarLimit = autoDollarLimit,
                        onLimitChange = { autoDollarLimit = it }
                    )

                    4 -> StepAgentPackageSelection(
                        template = template,
                        selectedPackage = selectedAgentPackage,
                        onPackageSelect = { selectedAgentPackage = it }
                    )

                    5 -> StepAdministratorSetup(
                        adminName = adminName,
                        onAdminNameChange = { adminName = it },
                        adminEmail = adminEmail,
                        onAdminEmailChange = { adminEmail = it }
                    )

                    6 -> StepProvisioningExecution(
                        companyName = companyName,
                        industryName = template.displayName,
                        adminEmail = adminEmail,
                        agentPackage = selectedAgentPackage,
                        isProvisioning = isProvisioning,
                        provisioningSteps = provisioningSteps,
                        currentStepIndex = provisioningStepIndex
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.height(12.dp))

            // Wizard Action Buttons Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1 && currentStep < 6 && !isProvisioning) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.testTag("onboarding_prev_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Previous")
                    }
                } else {
                    Spacer(Modifier.width(100.dp))
                }

                if (currentStep < 5) {
                    Button(
                        onClick = { currentStep++ },
                        enabled = isStepValid(currentStep, companyName, adminName, adminEmail),
                        modifier = Modifier.testTag("onboarding_next_btn")
                    ) {
                        Text("Next Step")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                } else if (currentStep == 5) {
                    Button(
                        onClick = {
                            currentStep = 6
                            isProvisioning = true
                            viewModel.startOnboardingDeployment(
                                companyName = companyName,
                                industry = selectedIndustryType.name,
                                companySize = companySize,
                                domain = domain,
                                primaryBottleneck = primaryBottleneck,
                                targetAutomationGoal = targetAutomationGoal,
                                targetReductionPercent = targetReductionPercent.toInt(),
                                selectedAgentPackage = selectedAgentPackage,
                                governanceProfile = "Enterprise Strict RBAC & Audit Trail",
                                adminName = adminName,
                                adminEmail = adminEmail,
                                aiTone = aiTone,
                                autoRiskThreshold = autoRiskThreshold,
                                autoDollarLimit = autoDollarLimit,
                                onComplete = { newWsId ->
                                    isProvisioning = false
                                    onOnboardingComplete(newWsId)
                                    onDismiss()
                                }
                            )
                        },
                        enabled = adminName.isNotBlank() && adminEmail.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("onboarding_execute_btn")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Deploy Workspace & Agents")
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        enabled = !isProvisioning,
                        modifier = Modifier.testTag("onboarding_finish_btn")
                    ) {
                        Text("Close Wizard")
                    }
                }
            }
        }
    }
}

private fun isStepValid(step: Int, companyName: String, adminName: String, adminEmail: String): Boolean {
    return when (step) {
        1 -> companyName.isNotBlank()
        5 -> adminName.isNotBlank() && adminEmail.isNotBlank()
        else -> true
    }
}

@Composable
fun StepperIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (step in 1..totalSteps) {
            val isCompleted = step < currentStep
            val isCurrent = step == currentStep

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = when {
                            isCompleted -> RcosNeonGreen
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "$step",
                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (step < totalSteps) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    color = if (step < currentStep) RcosNeonGreen else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepCompanyProfile(
    companyName: String,
    onCompanyNameChange: (String) -> Unit,
    selectedIndustryType: IndustryType,
    onIndustrySelect: (IndustryType) -> Unit,
    companySize: String,
    onSizeChange: (String) -> Unit,
    domain: String,
    onDomainChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Step 1: Enterprise Company Profile",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = companyName,
            onValueChange = onCompanyNameChange,
            label = { Text("Company / Organization Name *") },
            placeholder = { Text("e.g. Acme Financial Corp") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_company_name_input")
        )

        OutlinedTextField(
            value = domain,
            onValueChange = onDomainChange,
            label = { Text("Corporate Domain (e.g. acmefinancial.com)") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_domain_input")
        )

        Text("Select Industry Sector:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

        IndustryType.values().forEach { ind ->
            val isSelected = ind == selectedIndustryType
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIndustrySelect(ind) }
                    .testTag("industry_option_${ind.name}")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = ind.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    RadioButton(selected = isSelected, onClick = { onIndustrySelect(ind) })
                }
            }
        }
    }
}

@Composable
fun StepOperationalAssessment(
    primaryBottleneck: String,
    onBottleneckChange: (String) -> Unit,
    targetGoal: String,
    onTargetGoalChange: (String) -> Unit,
    reductionPercent: Float,
    onReductionChange: (Float) -> Unit,
    template: IndustryTemplate
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Step 2: Operational Assessment & Automation Goals",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Industry Profile: ${template.displayName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = primaryBottleneck,
            onValueChange = onBottleneckChange,
            label = { Text("Primary Operational Bottleneck") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_bottleneck_input"),
            minLines = 2
        )

        OutlinedTextField(
            value = targetGoal,
            onValueChange = onTargetGoalChange,
            label = { Text("Target Automation Goal") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_target_goal_input"),
            minLines = 2
        )

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Target Bottleneck Workload Reduction", style = MaterialTheme.typography.labelLarge)
                Text("${reductionPercent.toInt()}%", fontWeight = FontWeight.Bold, color = RcosNeonGreen)
            }
            Slider(
                value = reductionPercent,
                onValueChange = onReductionChange,
                valueRange = 10f..95f,
                steps = 17,
                modifier = Modifier.testTag("reduction_slider")
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Suggested Industry Automation Opportunities:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                template.suggestedAutomationOpportunities.forEach { opp ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RcosNeonGreen, modifier = Modifier.size(14.dp))
                        Text(opp, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StepAiGovernance(
    aiTone: String,
    onToneChange: (String) -> Unit,
    autoRiskThreshold: Int,
    onRiskChange: (Int) -> Unit,
    autoDollarLimit: Double,
    onLimitChange: (Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Step 3: AI Governance & Autonomous Limits",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = aiTone,
            onValueChange = onToneChange,
            label = { Text("AI Copilot Tone & System Behavior") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_ai_tone_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = autoRiskThreshold.toString(),
                onValueChange = { onRiskChange(it.toIntOrNull() ?: 20) },
                label = { Text("Auto-Approval Max Risk %") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("onboarding_risk_input")
            )

            OutlinedTextField(
                value = autoDollarLimit.toInt().toString(),
                onValueChange = { onLimitChange(it.toDoubleOrNull() ?: 2500.0) },
                label = { Text("Auto-Approval $ Ceiling") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("onboarding_dollar_input")
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("AI Safety & RBAC Governance Summary:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("• Operations exceeding Risk Level > $autoRiskThreshold% will automatically trigger Executive Human Sign-off.", fontSize = 12.sp)
                Text("• Financial actions exceeding $${autoDollarLimit.toInt()} require Manager or Admin approval.", fontSize = 12.sp)
                Text("• Every agent action is cryptographically recorded in the Enterprise Audit Log.", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StepAgentPackageSelection(
    template: IndustryTemplate,
    selectedPackage: String,
    onPackageSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Step 4: AI Agent Package & Registry Provisioning",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Recommended AI Agents for ${template.displayName}:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )

        template.recommendedAgents.forEach { spec ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(spec.agentName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }

                        AssistChip(
                            onClick = {},
                            label = { Text(spec.riskClassification.name, fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = when (spec.riskClassification) {
                                    AgentRiskLevel.CRITICAL -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                    AgentRiskLevel.HIGH -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    else -> Color(0xFF10B981).copy(alpha = 0.2f)
                                }
                            )
                        )
                    }

                    Text(spec.agentDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Department: ${spec.department} • Model: ${spec.modelTier}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun StepAdministratorSetup(
    adminName: String,
    onAdminNameChange: (String) -> Unit,
    adminEmail: String,
    onAdminEmailChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Step 5: Executive Administrator Account Setup",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = adminName,
            onValueChange = onAdminNameChange,
            label = { Text("Primary Admin Full Name *") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_admin_name_input")
        )

        OutlinedTextField(
            value = adminEmail,
            onValueChange = onAdminEmailChange,
            label = { Text("Primary Admin Corporate Email *") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_admin_email_input")
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Admin Account Privileges:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("• Full Control (FULL_CONTROL access level) over workspace configuration.", fontSize = 12.sp)
                Text("• Ability to invite team members and assign RBAC user roles.", fontSize = 12.sp)
                Text("• Authority to approve high-risk AI agent operations and financial transfers.", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StepProvisioningExecution(
    companyName: String,
    industryName: String,
    adminEmail: String,
    agentPackage: String,
    isProvisioning: Boolean,
    provisioningSteps: List<String>,
    currentStepIndex: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isProvisioning) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Text(
                text = "Deploying RCOS Workspace Environment...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = RcosNeonGreen,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "Workspace Environment Ready!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RcosNeonGreen
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Deployment Summary:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("• Company: $companyName", fontSize = 13.sp)
                Text("• Industry Sector: $industryName", fontSize = 13.sp)
                Text("• Workspace Admin: $adminEmail", fontSize = 13.sp)
                Text("• Agent Package: $agentPackage", fontSize = 13.sp)
                Text("• Multi-Tenant Database Isolation: VERIFIED", fontSize = 13.sp, color = RcosNeonGreen)
            }
        }
    }
}
