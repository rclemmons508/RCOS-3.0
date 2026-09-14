package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen

/**
 * Enterprise Agent Configuration state during the multi-step onboarding flow.
 */
data class OnboardingAgentConfig(
    val agentId: String,
    val agentName: String,
    val agentDescription: String,
    val agentType: AgentType,
    val assignedDepartment: String,
    var isEnabled: Boolean,
    var permissionLevel: AccessLevel,
    var riskClassification: AgentRiskLevel,
    var capabilities: Set<String>,
    var modelTier: String = "GEMINI_2_5_FLASH"
)

/**
 * Multi-Step Enterprise Onboarding Wizard.
 * Allows new enterprise users to configure organization domain, workflow preferences,
 * and agent permission levels before entering the main dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingDialog(
    currentProfile: CompanyProfileEntity?,
    viewModel: NovaViewModel? = null,
    canDismiss: Boolean = true,
    onDismiss: () -> Unit,
    onSaveProfile: (
        company: String,
        industry: String,
        bottleneck: String,
        targetPct: Int,
        agents: String,
        customInstructions: String
    ) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 4
    var isSubmitting by remember { mutableStateOf(false) }

    // ==========================================
    // STEP 1: Enterprise Profile & Domain
    // ==========================================
    var companyName by remember {
        mutableStateOf(currentProfile?.companyName?.takeIf { it.isNotBlank() } ?: "Acme Enterprise Solutions")
    }
    var domain by remember { mutableStateOf("acme-global.com") }
    var industry by remember {
        mutableStateOf(currentProfile?.industry?.takeIf { it.isNotBlank() } ?: "Technology & Software")
    }
    var bottleneck by remember {
        mutableStateOf(
            currentProfile?.primaryBottleneck?.takeIf { it.isNotBlank() }
                ?: "Executive Reporting & Task Dispatch"
        )
    }
    var targetReduction by remember {
        mutableFloatStateOf((currentProfile?.targetReductionPercent ?: 75).toFloat())
    }
    var companySize by remember { mutableStateOf("50-250 Employees") }
    var expandedIndustry by remember { mutableStateOf(false) }

    val industries = listOf(
        "Technology & Software",
        "Financial Services & Banking",
        "Healthcare & Life Sciences",
        "Logistics & Supply Chain",
        "Manufacturing & Industrial",
        "Retail & E-commerce",
        "Professional Services & Legal",
        "Cross-Industry Enterprise Workload"
    )

    val bottleneckSuggestions = listOf(
        "Executive Reporting & Task Dispatch",
        "Manual Invoice & Payment Approvals",
        "Client Intake & Document Parsing",
        "Compliance Auditing & Sign-Offs",
        "High-Volume Ticket Escalations"
    )

    // ==========================================
    // STEP 2: Workflow Preferences & Policies
    // ==========================================
    var autoRiskThreshold by remember { mutableFloatStateOf(25f) }
    var autoDollarLimit by remember { mutableDoubleStateOf(5000.0) }
    var emergencyOverride by remember { mutableStateOf(true) }

    // Trigger Channels
    var triggerVoice by remember { mutableStateOf(true) }
    var triggerEmail by remember { mutableStateOf(true) }
    var triggerCron by remember { mutableStateOf(true) }
    var triggerAnomaly by remember { mutableStateOf(true) }
    var triggerManual by remember { mutableStateOf(true) }

    // Workflow Blueprints
    var bpExecutiveStrategy by remember { mutableStateOf(true) }
    var bpDocumentIngestion by remember { mutableStateOf(true) }
    var bpFinancialAudit by remember { mutableStateOf(true) }
    var bpComplianceWatchdog by remember { mutableStateOf(true) }
    var bpTaskEscalation by remember { mutableStateOf(true) }

    // ==========================================
    // STEP 3: Agent Permission Levels & Governance Matrix
    // ==========================================
    val initialAgentConfigs = remember {
        mutableStateListOf(
            OnboardingAgentConfig(
                agentId = "ag_rcos_exec",
                agentName = "Executive Risk Copilot",
                agentDescription = "Evaluates strategic risks, financial authorizations, and high-impact approvals.",
                agentType = AgentType.EXECUTIVE_AGENT,
                assignedDepartment = "Executive Office",
                isEnabled = true,
                permissionLevel = AccessLevel.WORKFLOW_ADMIN,
                riskClassification = AgentRiskLevel.HIGH,
                capabilities = setOf("READ_DATA", "ANALYZE_DATA", "GENERATE_REPORTS", "REQUEST_APPROVAL", "ACCESS_FINANCIAL_DATA"),
                modelTier = "GEMINI_2_5_PRO"
            ),
            OnboardingAgentConfig(
                agentId = "ag_rcos_ops",
                agentName = "Nova Operations Engine",
                agentDescription = "Automates operational workflows, dispatch tasks, and client notification loops.",
                agentType = AgentType.OPERATIONS_AGENT,
                assignedDepartment = "Client Operations",
                isEnabled = true,
                permissionLevel = AccessLevel.REGULAR_STAFF,
                riskClassification = AgentRiskLevel.LOW,
                capabilities = setOf("READ_DATA", "EXECUTE_WORKFLOW", "CREATE_WORKFLOW", "SEND_COMMUNICATION"),
                modelTier = "GEMINI_2_5_FLASH"
            ),
            OnboardingAgentConfig(
                agentId = "ag_rcos_finance",
                agentName = "Financial Anomaly Analyst",
                agentDescription = "Monitors ledger discrepancies, billing limits, and transaction risk scores.",
                agentType = AgentType.FINANCE_AGENT,
                assignedDepartment = "Finance & Accounting",
                isEnabled = true,
                permissionLevel = AccessLevel.WORKFLOW_ADMIN,
                riskClassification = AgentRiskLevel.MEDIUM,
                capabilities = setOf("READ_DATA", "ACCESS_FINANCIAL_DATA", "ANALYZE_DATA", "REQUEST_APPROVAL"),
                modelTier = "GEMINI_2_5_FLASH"
            ),
            OnboardingAgentConfig(
                agentId = "ag_rcos_compliance",
                agentName = "Compliance & Security Auditor",
                agentDescription = "Enforces regulatory sign-offs, inspects audit logs, and monitors access deviations.",
                agentType = AgentType.COMPLIANCE_AGENT,
                assignedDepartment = "Legal & Compliance",
                isEnabled = true,
                permissionLevel = AccessLevel.READ_ONLY,
                riskClassification = AgentRiskLevel.CRITICAL,
                capabilities = setOf("READ_DATA", "ANALYZE_DATA", "GENERATE_REPORTS"),
                modelTier = "GEMINI_2_5_PRO"
            ),
            OnboardingAgentConfig(
                agentId = "ag_rcos_synth",
                agentName = "Workload Synthesizer Agent",
                agentDescription = "Analyzes inter-departmental workload logs, documents, and corporate email streams.",
                agentType = AgentType.ANALYTICS_AGENT,
                assignedDepartment = "Operations Support",
                isEnabled = true,
                permissionLevel = AccessLevel.REGULAR_STAFF,
                riskClassification = AgentRiskLevel.LOW,
                capabilities = setOf("READ_DATA", "ANALYZE_DATA", "GENERATE_REPORTS", "MODIFY_RECORDS"),
                modelTier = "GEMINI_2_5_FLASH"
            )
        )
    }

    // ==========================================
    // STEP 4: Review & Directives
    // ==========================================
    var customInstructions by remember {
        mutableStateOf(
            currentProfile?.customInstructions?.takeIf { it.isNotBlank() }
                ?: "Enforce strict SOC2 compliance, escalate financial anomalies over $5,000 to executive sign-off, and automate daily operations summaries."
        )
    }

    // Dialog Container
    Dialog(
        onDismissRequest = {
            if (canDismiss && !isSubmitting) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = canDismiss && !isSubmitting,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    RoundedCornerShape(24.dp)
                )
                .testTag("enterprise_onboarding_wizard"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // ==========================================
                // HEADER & WIZARD STEP INDICATOR
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "STEP $currentStep OF $totalSteps",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = when (currentStep) {
                                    1 -> "Enterprise Identity"
                                    2 -> "Workflow Preferences"
                                    3 -> "Agent Permissions"
                                    else -> "Review & Deployment"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Configure enterprise workflow governance & agent autonomy before launching",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (canDismiss) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_onboarding_dialog")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Multi-Step Progress Tracker Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (step in 1..totalSteps) {
                        val isCompleted = step < currentStep
                        val isCurrent = step == currentStep
                        val barColor = when {
                            isCompleted -> Color(0xFF10B981)
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (step <= currentStep || !canDismiss) {
                                        currentStep = step
                                    }
                                }
                        ) {
                            LinearProgressIndicator(
                                progress = { if (isCompleted || isCurrent) 1f else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (step) {
                                    1 -> "1. Profile"
                                    2 -> "2. Workflows"
                                    3 -> "3. Agents"
                                    else -> "4. Launch"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // STEP CONTENTS (SCROLLABLE AREA)
                // ==========================================
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentStep) {
                        1 -> Step1EnterpriseProfile(
                            companyName = companyName,
                            onCompanyNameChange = { companyName = it },
                            domain = domain,
                            onDomainChange = { domain = it },
                            industry = industry,
                            onIndustryChange = { industry = it },
                            expandedIndustry = expandedIndustry,
                            onExpandedIndustryChange = { expandedIndustry = it },
                            industries = industries,
                            bottleneck = bottleneck,
                            onBottleneckChange = { bottleneck = it },
                            bottleneckSuggestions = bottleneckSuggestions,
                            targetReduction = targetReduction,
                            onTargetReductionChange = { targetReduction = it },
                            companySize = companySize,
                            onCompanySizeChange = { companySize = it }
                        )

                        2 -> Step2WorkflowPreferences(
                            autoRiskThreshold = autoRiskThreshold,
                            onAutoRiskThresholdChange = { autoRiskThreshold = it },
                            autoDollarLimit = autoDollarLimit,
                            onAutoDollarLimitChange = { autoDollarLimit = it },
                            emergencyOverride = emergencyOverride,
                            onEmergencyOverrideChange = { emergencyOverride = it },
                            triggerVoice = triggerVoice,
                            onTriggerVoiceChange = { triggerVoice = it },
                            triggerEmail = triggerEmail,
                            onTriggerEmailChange = { triggerEmail = it },
                            triggerCron = triggerCron,
                            onTriggerCronChange = { triggerCron = it },
                            triggerAnomaly = triggerAnomaly,
                            onTriggerAnomalyChange = { triggerAnomaly = it },
                            triggerManual = triggerManual,
                            onTriggerManualChange = { triggerManual = it },
                            bpExecutiveStrategy = bpExecutiveStrategy,
                            onBpExecutiveStrategyChange = { bpExecutiveStrategy = it },
                            bpDocumentIngestion = bpDocumentIngestion,
                            onBpDocumentIngestionChange = { bpDocumentIngestion = it },
                            bpFinancialAudit = bpFinancialAudit,
                            onBpFinancialAuditChange = { bpFinancialAudit = it },
                            bpComplianceWatchdog = bpComplianceWatchdog,
                            onBpComplianceWatchdogChange = { bpComplianceWatchdog = it },
                            bpTaskEscalation = bpTaskEscalation,
                            onBpTaskEscalationChange = { bpTaskEscalation = it }
                        )

                        3 -> Step3AgentPermissions(
                            agentConfigs = initialAgentConfigs,
                            onAgentConfigUpdated = { index, updated ->
                                initialAgentConfigs[index] = updated
                            }
                        )

                        4 -> Step4ReviewAndLaunch(
                            companyName = companyName,
                            domain = domain,
                            industry = industry,
                            bottleneck = bottleneck,
                            targetReduction = targetReduction.toInt(),
                            autoRiskThreshold = autoRiskThreshold.toInt(),
                            autoDollarLimit = autoDollarLimit,
                            emergencyOverride = emergencyOverride,
                            enabledTriggersCount = listOf(triggerVoice, triggerEmail, triggerCron, triggerAnomaly, triggerManual).count { it },
                            agentConfigs = initialAgentConfigs,
                            customInstructions = customInstructions,
                            onCustomInstructionsChange = { customInstructions = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // BOTTOM NAVIGATION CONTROLS
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("step${currentStep}_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back")
                        }
                    } else {
                        if (canDismiss) {
                            TextButton(
                                onClick = onDismiss,
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("cancel_onboarding_button")
                            ) {
                                Text("Cancel")
                            }
                        } else {
                            // First-time user can quick-skip with Enterprise defaults
                            TextButton(
                                onClick = {
                                    isSubmitting = true
                                    applyDefaultEnterpriseConfiguration(
                                        viewModel = viewModel,
                                        companyName = companyName,
                                        industry = industry,
                                        domain = domain,
                                        bottleneck = bottleneck,
                                        targetReduction = targetReduction.toInt(),
                                        customInstructions = customInstructions,
                                        onSaveProfile = onSaveProfile,
                                        onDismiss = onDismiss
                                    )
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("skip_onboarding_button")
                            ) {
                                Text("Use Enterprise Defaults")
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentStep < totalSteps) {
                            Button(
                                onClick = { currentStep++ },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("step${currentStep}_next_button")
                            ) {
                                Text(
                                    when (currentStep) {
                                        1 -> "Next: Workflows"
                                        2 -> "Next: Agent Permissions"
                                        else -> "Next: Review & Launch"
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Button(
                                onClick = {
                                    isSubmitting = true

                                    // Build workflow config
                                    val workflowConfig = BusinessWorkflowConfig(
                                        companyName = companyName,
                                        industry = industry,
                                        operationalBottleneck = bottleneck,
                                        workloadReductionGoalPct = targetReduction.toInt(),
                                        autoApprovalThresholdDollars = autoDollarLimit,
                                        maxRiskAutoApprovePct = autoRiskThreshold.toInt(),
                                        emergencyHumanOverride = emergencyOverride
                                    )

                                    // Convert agent configs to AgentRegistryEntity list
                                    val entities = initialAgentConfigs.map { cfg ->
                                        AgentRegistryEntity(
                                            agentId = cfg.agentId,
                                            workspaceId = "ws_default",
                                            agentName = cfg.agentName,
                                            agentDescription = cfg.agentDescription,
                                            agentType = cfg.agentType.name,
                                            status = if (cfg.isEnabled) AgentStatus.ACTIVE.name else AgentStatus.INACTIVE.name,
                                            capabilityProfile = cfg.capabilities.joinToString(","),
                                            permissionLevel = cfg.permissionLevel.name,
                                            riskClassification = cfg.riskClassification.name,
                                            modelTier = cfg.modelTier,
                                            createdBy = "ENTERPRISE_ONBOARDING",
                                            assignedDepartment = cfg.assignedDepartment
                                        )
                                    }

                                    val activeTriggers = mutableListOf<String>()
                                    if (triggerVoice) activeTriggers.add("INCOMING_CALL")
                                    if (triggerEmail) activeTriggers.add("EMAIL_RECEIVED")
                                    if (triggerCron) activeTriggers.add("SCHEDULED_CRON")
                                    if (triggerAnomaly) activeTriggers.add("THRESHOLD_EVENT")
                                    if (triggerManual) activeTriggers.add("MANUAL_DISPATCH")

                                    val activeBlueprints = mutableListOf<String>()
                                    if (bpExecutiveStrategy) activeBlueprints.add("Executive Strategy")
                                    if (bpDocumentIngestion) activeBlueprints.add("Document Ingestion")
                                    if (bpFinancialAudit) activeBlueprints.add("Financial Audit")
                                    if (bpComplianceWatchdog) activeBlueprints.add("Compliance Watchdog")
                                    if (bpTaskEscalation) activeBlueprints.add("Task Escalation")

                                    if (viewModel != null) {
                                        viewModel.completeEnterpriseOnboarding(
                                            companyName = companyName,
                                            industry = industry,
                                            domain = domain,
                                            bottleneck = bottleneck,
                                            reductionPercent = targetReduction.toInt(),
                                            customInstructions = customInstructions,
                                            workflowConfig = workflowConfig,
                                            configuredAgents = entities,
                                            activeTriggers = activeTriggers,
                                            activeBlueprints = activeBlueprints
                                        )
                                    } else {
                                        val enabledAgentsStr = initialAgentConfigs
                                            .filter { it.isEnabled }
                                            .joinToString(", ") { it.agentName }
                                        onSaveProfile(
                                            companyName,
                                            industry,
                                            bottleneck,
                                            targetReduction.toInt(),
                                            enabledAgentsStr,
                                            customInstructions
                                        )
                                    }
                                    onDismiss()
                                },
                                enabled = !isSubmitting,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00C853)
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("save_onboarding_button")
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Provisioning Workspace...", color = Color.White)
                                } else {
                                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Save & Launch Workspace",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================
// STEP 1: Enterprise Profile & Domain Identity Composable
// ====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step1EnterpriseProfile(
    companyName: String,
    onCompanyNameChange: (String) -> Unit,
    domain: String,
    onDomainChange: (String) -> Unit,
    industry: String,
    onIndustryChange: (String) -> Unit,
    expandedIndustry: Boolean,
    onExpandedIndustryChange: (Boolean) -> Unit,
    industries: List<String>,
    bottleneck: String,
    onBottleneckChange: (String) -> Unit,
    bottleneckSuggestions: List<String>,
    targetReduction: Float,
    onTargetReductionChange: (Float) -> Unit,
    companySize: String,
    onCompanySizeChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Art Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = R.drawable.img_dashboard_hero,
                contentDescription = "RCOS Enterprise Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enterprise Workspace Setup",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Tailor autonomous agents & workflows to your corporate infrastructure",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Company Name Input
        OutlinedTextField(
            value = companyName,
            onValueChange = onCompanyNameChange,
            label = { Text("Enterprise / Organization Name") },
            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_company_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Domain & Scale Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = domain,
                onValueChange = onDomainChange,
                label = { Text("Corporate Domain") },
                placeholder = { Text("company.global") },
                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("onboarding_domain_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = companySize,
                onValueChange = onCompanySizeChange,
                label = { Text("Headcount Scale") },
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("onboarding_company_size_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Industry Dropdown
        ExposedDropdownMenuBox(
            expanded = expandedIndustry,
            onExpandedChange = onExpandedIndustryChange
        ) {
            OutlinedTextField(
                value = industry,
                onValueChange = {},
                readOnly = true,
                label = { Text("Target Industry / Operations Domain") },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIndustry) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .testTag("onboarding_industry_dropdown"),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = expandedIndustry,
                onDismissRequest = { onExpandedIndustryChange(false) }
            ) {
                industries.forEach { ind ->
                    DropdownMenuItem(
                        text = { Text(ind) },
                        onClick = {
                            onIndustryChange(ind)
                            onExpandedIndustryChange(false)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Bottleneck Input
        Text(
            text = "Primary Operational Bottleneck",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = bottleneck,
            onValueChange = onBottleneckChange,
            placeholder = { Text("e.g. Executive Reporting, Document Processing, Triage") },
            leadingIcon = { Icon(Icons.Default.HourglassTop, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_bottleneck_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Suggestion Chips for Bottlenecks
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(bottleneckSuggestions) { item ->
                SuggestionChip(
                    onClick = { onBottleneckChange(item) },
                    label = { Text(item, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Workload Automation Target Slider
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Workload Automation Target",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Target percentage of routine tasks delegated to autonomous agents",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${targetReduction.toInt()}% Automated",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = targetReduction,
                    onValueChange = onTargetReductionChange,
                    valueRange = 10f..95f,
                    steps = 16,
                    modifier = Modifier.testTag("onboarding_target_slider")
                )
            }
        }
    }
}

// ====================================================================
// STEP 2: Workflow Preferences & Automation Policies Composable
// ====================================================================
@Composable
private fun Step2WorkflowPreferences(
    autoRiskThreshold: Float,
    onAutoRiskThresholdChange: (Float) -> Unit,
    autoDollarLimit: Double,
    onAutoDollarLimitChange: (Double) -> Unit,
    emergencyOverride: Boolean,
    onEmergencyOverrideChange: (Boolean) -> Unit,
    triggerVoice: Boolean,
    onTriggerVoiceChange: (Boolean) -> Unit,
    triggerEmail: Boolean,
    onTriggerEmailChange: (Boolean) -> Unit,
    triggerCron: Boolean,
    onTriggerCronChange: (Boolean) -> Unit,
    triggerAnomaly: Boolean,
    onTriggerAnomalyChange: (Boolean) -> Unit,
    triggerManual: Boolean,
    onTriggerManualChange: (Boolean) -> Unit,
    bpExecutiveStrategy: Boolean,
    onBpExecutiveStrategyChange: (Boolean) -> Unit,
    bpDocumentIngestion: Boolean,
    onBpDocumentIngestionChange: (Boolean) -> Unit,
    bpFinancialAudit: Boolean,
    onBpFinancialAuditChange: (Boolean) -> Unit,
    bpComplianceWatchdog: Boolean,
    onBpComplianceWatchdogChange: (Boolean) -> Unit,
    bpTaskEscalation: Boolean,
    onBpTaskEscalationChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Section Title
        Text(
            text = "Enterprise Auto-Approval Governance",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Configure parameters for autonomous execution versus human sign-off",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Risk Score Ceiling Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-Approval Risk Score Ceiling",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "≤ ${autoRiskThreshold.toInt()}% Risk",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (autoRiskThreshold <= 20) Color(0xFF00C853) else Color(0xFFFFA000)
                        )
                    )
                }
                Text(
                    text = "Actions with risk score exceeding this threshold require human executive sign-off.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = autoRiskThreshold,
                    onValueChange = onAutoRiskThresholdChange,
                    valueRange = 5f..50f,
                    steps = 8,
                    modifier = Modifier.testTag("workflow_auto_risk_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dollar Limit Selector
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Autonomous Transaction Dollar Limit",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Financial actions under this limit execute automatically if within risk tolerance.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                val limits = listOf(1000.0, 2500.0, 5000.0, 15000.0, 50000.0)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(limits) { limit ->
                        val isSelected = autoDollarLimit == limit
                        FilterChip(
                            selected = isSelected,
                            onClick = { onAutoDollarLimitChange(limit) },
                            label = {
                                Text(
                                    text = "$${limit.toInt()}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            modifier = Modifier.testTag("workflow_dollar_limit_${limit.toInt()}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Emergency Override Switch
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Emergency Executive Kill-Switch",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Allows workspace administrators to immediately pause or cancel any autonomous agent pipeline.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = emergencyOverride,
                    onCheckedChange = onEmergencyOverrideChange,
                    modifier = Modifier.testTag("workflow_emergency_override_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Event Trigger Channels
        Text(
            text = "Active Inbound Event Trigger Channels",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Select channels that automatically dispatch autonomous workflow pipelines",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        TriggerToggleCard(
            title = "Voice Operations & Phone AI Transcripts",
            subtitle = "Transcribe incoming calls & dispatch tasks from client conversations",
            icon = Icons.Default.Phone,
            checked = triggerVoice,
            onCheckedChange = onTriggerVoiceChange
        )
        TriggerToggleCard(
            title = "Inbound Corporate Email & Document Parser",
            subtitle = "Ingest incoming attachments, invoices, and executive correspondence",
            icon = Icons.Default.Email,
            checked = triggerEmail,
            onCheckedChange = onTriggerEmailChange
        )
        TriggerToggleCard(
            title = "Scheduled Automation Cron Loops",
            subtitle = "Execute periodic balance audits, morning strategy briefs, and syncs",
            icon = Icons.Default.Schedule,
            checked = triggerCron,
            onCheckedChange = onTriggerCronChange
        )
        TriggerToggleCard(
            title = "Telemetry Threshold & Anomaly Triggers",
            subtitle = "Trigger autonomous incident resolution when risk spikes",
            icon = Icons.Default.Warning,
            checked = triggerAnomaly,
            onCheckedChange = onTriggerAnomalyChange
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Enterprise Workflow Blueprints
        Text(
            text = "Enterprise Workflow Blueprints to Activate",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        BlueprintCheckboxCard(
            title = "Daily Strategic Executive Briefing",
            category = "Strategy & Ops",
            checked = bpExecutiveStrategy,
            onCheckedChange = onBpExecutiveStrategyChange
        )
        BlueprintCheckboxCard(
            title = "Document Intake & Contract Ingestion",
            category = "Operations",
            checked = bpDocumentIngestion,
            onCheckedChange = onBpDocumentIngestionChange
        )
        BlueprintCheckboxCard(
            title = "Financial Anomaly & Billing Authorization Stream",
            category = "Finance",
            checked = bpFinancialAudit,
            onCheckedChange = onBpFinancialAuditChange
        )
        BlueprintCheckboxCard(
            title = "Continuous Compliance & Security Watchdog",
            category = "Compliance",
            checked = bpComplianceWatchdog,
            onCheckedChange = onBpComplianceWatchdogChange
        )
    }
}

// ====================================================================
// STEP 3: Agent Permission Levels & Governance Matrix Composable
// ====================================================================
@Composable
private fun Step3AgentPermissions(
    agentConfigs: List<OnboardingAgentConfig>,
    onAgentConfigUpdated: (Int, OnboardingAgentConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Autonomous Agent Access Levels & Permissions",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Customize autonomy tiers, risk classifications, and fine-grained capabilities for each agent",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        agentConfigs.forEachIndexed { index, agent ->
            AgentPermissionCard(
                agent = agent,
                onUpdate = { updated -> onAgentConfigUpdated(index, updated) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AgentPermissionCard(
    agent: OnboardingAgentConfig,
    onUpdate: (OnboardingAgentConfig) -> Unit
) {
    var expandedPermMenu by remember { mutableStateOf(false) }
    var expandedRiskMenu by remember { mutableStateOf(false) }
    var showCapabilities by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (agent.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (agent.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Checkbox, Name, Department & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agent.isEnabled,
                    onCheckedChange = { isChecked ->
                        onUpdate(agent.copy(isEnabled = isChecked))
                    },
                    modifier = Modifier.testTag("agent_enable_checkbox_${agent.agentId}")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = agent.agentName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${agent.assignedDepartment} • ${agent.agentType.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Autonomy Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (agent.permissionLevel) {
                        AccessLevel.FULL_CONTROL -> Color(0xFF00C853).copy(alpha = 0.2f)
                        AccessLevel.WORKFLOW_ADMIN -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        AccessLevel.REGULAR_STAFF -> Color(0xFFFFA000).copy(alpha = 0.2f)
                        AccessLevel.READ_ONLY -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = agent.permissionLevel.label.take(18),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when (agent.permissionLevel) {
                            AccessLevel.FULL_CONTROL -> Color(0xFF00C853)
                            AccessLevel.WORKFLOW_ADMIN -> MaterialTheme.colorScheme.primary
                            AccessLevel.REGULAR_STAFF -> Color(0xFFFFA000)
                            AccessLevel.READ_ONLY -> Color(0xFF8B5CF6)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (agent.isEnabled) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = agent.agentDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Selectors: Permission Level & Risk Classification
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Permission Level Dropdown Box
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expandedPermMenu = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("perm_dropdown_btn_${agent.agentId}")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (agent.permissionLevel) {
                                    AccessLevel.FULL_CONTROL -> "Full Autonomy"
                                    AccessLevel.WORKFLOW_ADMIN -> "Workflow Admin"
                                    AccessLevel.REGULAR_STAFF -> "Standard Staff"
                                    AccessLevel.READ_ONLY -> "Read-Only"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = expandedPermMenu,
                            onDismissRequest = { expandedPermMenu = false }
                        ) {
                            AccessLevel.entries.forEach { level ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(level.label, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                when (level) {
                                                    AccessLevel.FULL_CONTROL -> "Full autonomous execution and approval authority"
                                                    AccessLevel.WORKFLOW_ADMIN -> "Can approve & dispatch operational workflows"
                                                    AccessLevel.REGULAR_STAFF -> "Can execute standard assigned tasks"
                                                    AccessLevel.READ_ONLY -> "Audit and advisory suggestions only"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onUpdate(agent.copy(permissionLevel = level))
                                        expandedPermMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Risk Classification Dropdown Box
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expandedRiskMenu = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("risk_dropdown_btn_${agent.agentId}")
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = agent.riskClassification.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = expandedRiskMenu,
                            onDismissRequest = { expandedRiskMenu = false }
                        ) {
                            AgentRiskLevel.entries.forEach { risk ->
                                DropdownMenuItem(
                                    text = { Text(risk.label) },
                                    onClick = {
                                        onUpdate(agent.copy(riskClassification = risk))
                                        expandedRiskMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle Capabilities Details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCapabilities = !showCapabilities }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Granted Capabilities (${agent.capabilities.size})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (showCapabilities) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = showCapabilities) {
                    val allCapabilities = listOf(
                        "READ_DATA",
                        "ANALYZE_DATA",
                        "GENERATE_REPORTS",
                        "EXECUTE_WORKFLOW",
                        "CREATE_WORKFLOW",
                        "REQUEST_APPROVAL",
                        "MODIFY_RECORDS",
                        "ACCESS_FINANCIAL_DATA",
                        "SEND_COMMUNICATION"
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allCapabilities.forEach { cap ->
                            val isGranted = agent.capabilities.contains(cap)
                            FilterChip(
                                selected = isGranted,
                                onClick = {
                                    val newSet = agent.capabilities.toMutableSet()
                                    if (isGranted) newSet.remove(cap) else newSet.add(cap)
                                    onUpdate(agent.copy(capabilities = newSet))
                                },
                                label = {
                                    Text(
                                        text = cap.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                    )
                                },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================
// STEP 4: Review & Launch Execution Composable
// ====================================================================
@Composable
private fun Step4ReviewAndLaunch(
    companyName: String,
    domain: String,
    industry: String,
    bottleneck: String,
    targetReduction: Int,
    autoRiskThreshold: Int,
    autoDollarLimit: Double,
    emergencyOverride: Boolean,
    enabledTriggersCount: Int,
    agentConfigs: List<OnboardingAgentConfig>,
    customInstructions: String,
    onCustomInstructionsChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Enterprise Workspace Configuration Review",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Confirm operational directives and deploy autonomous multi-agent pipelines",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Summary Card 1: Enterprise Profile
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Enterprise Profile",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                ReviewDetailRow(label = "Company Name", value = companyName)
                ReviewDetailRow(label = "Domain", value = domain)
                ReviewDetailRow(label = "Industry", value = industry)
                ReviewDetailRow(label = "Primary Bottleneck", value = bottleneck)
                ReviewDetailRow(label = "Target Automation", value = "$targetReduction% Workload Reduction")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Summary Card 2: Workflow Preferences
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Workflow Governance",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                ReviewDetailRow(label = "Auto-Approval Risk Cap", value = "≤ $autoRiskThreshold% Risk")
                ReviewDetailRow(label = "Auto-Approval Financial Cap", value = "$${autoDollarLimit.toInt()}")
                ReviewDetailRow(label = "Emergency Override", value = if (emergencyOverride) "Enabled (Mandatory)" else "Disabled")
                ReviewDetailRow(label = "Active Inbound Triggers", value = "$enabledTriggersCount Channels Configured")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Summary Card 3: Agent Permission Matrix
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                val enabledAgents = agentConfigs.filter { it.isEnabled }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. Agent Permission Matrix (${enabledAgents.size} Active)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                enabledAgents.forEach { ag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ag.agentName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "${ag.permissionLevel.label.take(14)} (${ag.riskClassification.label})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Custom Instructions Input
        Text(
            text = "Custom Corporate Directives & Agent Rules",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Provide top-level system prompts or compliance constraints applied to all agents",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = customInstructions,
            onValueChange = onCustomInstructionsChange,
            minLines = 3,
            maxLines = 5,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_custom_instructions_input"),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

// ====================================================================
// REUSABLE HELPER UI COMPONENTS
// ====================================================================
@Composable
private fun ReviewDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TriggerToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (checked) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun BlueprintCheckboxCard(
    title: String,
    category: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                Text(text = "Category: $category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun applyDefaultEnterpriseConfiguration(
    viewModel: NovaViewModel?,
    companyName: String,
    industry: String,
    domain: String,
    bottleneck: String,
    targetReduction: Int,
    customInstructions: String,
    onSaveProfile: (String, String, String, Int, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultWorkflowConfig = BusinessWorkflowConfig(
        companyName = companyName,
        industry = industry,
        operationalBottleneck = bottleneck,
        workloadReductionGoalPct = targetReduction,
        autoApprovalThresholdDollars = 5000.0,
        maxRiskAutoApprovePct = 25,
        emergencyHumanOverride = true
    )

    val defaultAgents = listOf(
        AgentRegistryEntity(
            agentId = "ag_rcos_exec",
            workspaceId = "ws_default",
            agentName = "Executive Risk Copilot",
            agentDescription = "Evaluates strategic risks, financial authorizations, and high-impact approvals.",
            agentType = AgentType.EXECUTIVE_AGENT.name,
            status = AgentStatus.ACTIVE.name,
            capabilityProfile = "READ_DATA,ANALYZE_DATA,GENERATE_REPORTS,REQUEST_APPROVAL,ACCESS_FINANCIAL_DATA",
            permissionLevel = AccessLevel.WORKFLOW_ADMIN.name,
            riskClassification = AgentRiskLevel.HIGH.name,
            modelTier = "GEMINI_2_5_PRO",
            createdBy = "ENTERPRISE_ONBOARDING",
            assignedDepartment = "Executive Office"
        ),
        AgentRegistryEntity(
            agentId = "ag_rcos_ops",
            workspaceId = "ws_default",
            agentName = "Nova Operations Engine",
            agentDescription = "Automates operational workflows, dispatch tasks, and client notification loops.",
            agentType = AgentType.OPERATIONS_AGENT.name,
            status = AgentStatus.ACTIVE.name,
            capabilityProfile = "READ_DATA,EXECUTE_WORKFLOW,CREATE_WORKFLOW,SEND_COMMUNICATION",
            permissionLevel = AccessLevel.REGULAR_STAFF.name,
            riskClassification = AgentRiskLevel.LOW.name,
            modelTier = "GEMINI_2_5_FLASH",
            createdBy = "ENTERPRISE_ONBOARDING",
            assignedDepartment = "Client Operations"
        )
    )

    if (viewModel != null) {
        viewModel.completeEnterpriseOnboarding(
            companyName = companyName,
            industry = industry,
            domain = domain,
            bottleneck = bottleneck,
            reductionPercent = targetReduction,
            customInstructions = customInstructions,
            workflowConfig = defaultWorkflowConfig,
            configuredAgents = defaultAgents
        )
    } else {
        onSaveProfile(
            companyName,
            industry,
            bottleneck,
            targetReduction,
            "Executive Risk Copilot, Nova Operations Engine",
            customInstructions
        )
    }
    onDismiss()
}
