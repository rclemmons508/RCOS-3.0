package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowEngineScreen(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val workflowTemplates by viewModel.workflowTemplates.collectAsState()
    val approvalItems by viewModel.approvalItems.collectAsState()
    val actionLogs by viewModel.aiActionLogs.collectAsState()
    val agentResponsibilities by viewModel.agentResponsibilities.collectAsState()
    val businessConfig by viewModel.businessWorkflowConfig.collectAsState()
    val isWorkflowGeminiLoading by viewModel.isWorkflowGeminiLoading.collectAsState()
    val workflowGeminiOptimization by viewModel.workflowGeminiOptimization.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Templates & Triggers, 1: Pending Approvals, 2: AI Action History, 3: Agent Responsibilities, 4: Business Config
    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var approvalSubFilter by remember { mutableStateOf("PENDING") } // PENDING, COMPLETED, ARCHIVED, ALL

    val pendingItems = remember(approvalItems) { approvalItems.filter { it.status == ApprovalStatus.PENDING } }
    val completedItems = remember(approvalItems) { approvalItems.filter { it.status == ApprovalStatus.APPROVED || it.status == ApprovalStatus.COMPLETED } }
    val archivedItems = remember(approvalItems) { approvalItems.filter { it.status == ApprovalStatus.ARCHIVED || it.status == ApprovalStatus.REJECTED } }
    val pendingCount = pendingItems.size

    val displayedApprovals = remember(approvalItems, approvalSubFilter) {
        when (approvalSubFilter) {
            "PENDING" -> pendingItems
            "COMPLETED" -> completedItems
            "ARCHIVED" -> archivedItems
            else -> approvalItems
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Header Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(RcosNeonGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoMode,
                            contentDescription = null,
                            tint = RcosNeonGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "AI Workflow Automation Engine",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Autonomous Operations & Executive Oversight",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { showCreateTemplateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("create_workflow_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Workflow", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Operational Metrics Summary Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricBox(
                        value = "${workflowTemplates.count { it.isActive }}",
                        label = "Active Workflows",
                        icon = Icons.Default.AccountTree,
                        color = RcosNeonGreen
                    )

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    MetricBox(
                        value = "$pendingCount",
                        label = "Pending Approvals",
                        icon = Icons.Default.PendingActions,
                        color = if (pendingCount > 0) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    MetricBox(
                        value = "${businessConfig.workloadReductionGoalPct}%",
                        label = "Target Reduction",
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        color = Color(0xFF64B5F6)
                    )
                }
            }
        }

        // Section Tab Selector
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = RcosNeonGreen,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Templates & Triggers", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Pending Approvals", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                            if (pendingCount > 0) {
                                Badge(containerColor = Color(0xFFFFB74D), contentColor = Color.Black) {
                                    Text("$pendingCount", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("AI Action History", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Agent Responsibilities", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Business Config", fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // Tab Contents
        when (selectedTab) {
            0 -> {
                // Tab 0: Business Workflow Templates & Task Triggers
                items(workflowTemplates) { template ->
                    WorkflowTemplateCard(
                        template = template,
                        onToggleActive = { viewModel.toggleWorkflowActive(template.id) },
                        onTriggerExecution = { viewModel.triggerWorkflow(template.id) },
                        onOptimizeWithGemini = { viewModel.optimizeWorkflowWithGemini(template) },
                        isOptimizingWithGemini = isWorkflowGeminiLoading[template.id] == true,
                        optimizationReport = workflowGeminiOptimization[template.id]
                    )
                }
            }

            1 -> {
                // Tab 1: Approvals Queue with Live Sub-Filters
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = approvalSubFilter == "PENDING",
                                onClick = { approvalSubFilter = "PENDING" },
                                label = { Text("Pending (${pendingItems.size})", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    if (approvalSubFilter == "PENDING") {
                                        Icon(Icons.Default.PendingActions, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFB74D),
                                    selectedLabelColor = Color.Black
                                )
                            )

                            FilterChip(
                                selected = approvalSubFilter == "COMPLETED",
                                onClick = { approvalSubFilter = "COMPLETED" },
                                label = { Text("Completed (${completedItems.size})", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    if (approvalSubFilter == "COMPLETED") {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RcosNeonGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )

                            FilterChip(
                                selected = approvalSubFilter == "ARCHIVED",
                                onClick = { approvalSubFilter = "ARCHIVED" },
                                label = { Text("Archived (${archivedItems.size})") },
                                leadingIcon = {
                                    if (approvalSubFilter == "ARCHIVED") {
                                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )

                            FilterChip(
                                selected = approvalSubFilter == "ALL",
                                onClick = { approvalSubFilter = "ALL" },
                                label = { Text("All (${approvalItems.size})") }
                            )
                        }

                        val firestoreSyncInfo by viewModel.firestoreSyncInfo.collectAsState()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().testTag("workflow_firestore_sync_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (firestoreSyncInfo.state) {
                                                    com.example.data.FirestoreSyncState.CONNECTED -> RcosNeonGreen
                                                    com.example.data.FirestoreSyncState.SYNCING -> Color(0xFF38BDF8)
                                                    else -> Color(0xFFFFB74D)
                                                }
                                            )
                                    )
                                    Text(
                                        text = "⚡ Firestore Realtime Sync: ${firestoreSyncInfo.statusMessage}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.triggerFirestoreManualSync() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        tint = RcosNeonGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = RcosNeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = when (approvalSubFilter) {
                                        "PENDING" -> "Active Pending Tasks: Once approved, items are automatically moved to the Completed list and dispatched."
                                        "COMPLETED" -> "Completed & Authorized Tasks: Workflows that passed executive, client, or AI verification."
                                        "ARCHIVED" -> "Archived Tasks: Historical or rejected workflows retained for compliance and audit trail."
                                        else -> "Comprehensive task ledger displaying all active, completed, and archived items."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (displayedApprovals.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (approvalSubFilter == "PENDING") Icons.Default.CheckCircleOutline else Icons.Default.Inbox,
                                    contentDescription = null,
                                    tint = if (approvalSubFilter == "PENDING") RcosNeonGreen else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = if (approvalSubFilter == "PENDING") "No Pending Tasks" else "No $approvalSubFilter Items",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (approvalSubFilter == "PENDING")
                                        "All tasks have been approved and automatically moved to the Completed / Archived lists."
                                    else
                                        "No items found matching the selected '$approvalSubFilter' filter criteria.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(displayedApprovals, key = { it.id }) { item ->
                        ApprovalRequirementCard(
                            item = item,
                            onApprove = { notes -> viewModel.approveItem(item.id, "CEO", notes) },
                            onClientApprove = { notes -> viewModel.approveItemByClient(item.id, "Client Portal Sign-Off", notes) },
                            onAiAutoApprove = { viewModel.autoApprovePendingItem(item.id, item.requestedByAgent) },
                            onReject = { notes -> viewModel.rejectItem(item.id, "CEO", notes) },
                            onArchive = { viewModel.archiveItem(item.id, "CEO", "Archived from task list") },
                            onRestore = { viewModel.restoreItemToPending(item.id) }
                        )
                    }
                }
            }

            2 -> {
                // Tab 2: AI Action History (Audit Log)
                items(actionLogs) { log ->
                    AiActionLogCard(log = log)
                }
            }

            3 -> {
                // Tab 3: Agent Responsibilities & Autonomy Scope
                items(agentResponsibilities) { agent ->
                    AgentResponsibilityCard(
                        agent = agent,
                        onAutonomyChanged = { newAutonomy -> viewModel.updateAgentAutonomy(agent.id, newAutonomy) }
                    )
                }
            }

            4 -> {
                // Tab 4: Business-Specific Configuration
                item {
                    BusinessConfigSection(
                        config = businessConfig,
                        onSave = { updated -> viewModel.updateBusinessConfig(updated) }
                    )
                }
            }
        }
    }

    // Create New Workflow Template Dialog
    if (showCreateTemplateDialog) {
        CreateWorkflowTemplateDialog(
            onDismiss = { showCreateTemplateDialog = false },
            onCreate = { newTemplate ->
                viewModel.addWorkflowTemplate(newTemplate)
                showCreateTemplateDialog = false
            }
        )
    }
}

@Composable
private fun MetricBox(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WorkflowTemplateCard(
    template: WorkflowTemplate,
    onToggleActive: () -> Unit,
    onTriggerExecution: () -> Unit,
    onOptimizeWithGemini: () -> Unit,
    isOptimizingWithGemini: Boolean,
    optimizationReport: String?
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workflow_template_${template.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (template.isActive) RcosNeonGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RcosNeonGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = template.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = RcosNeonGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "${template.totalExecutions} Executions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = template.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = RcosNeonGreen
                    )
                )
            }

            // Title & Description
            Text(
                text = template.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Trigger & Agent Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = when (template.trigger.type) {
                            TriggerType.INCOMING_CALL -> Icons.AutoMirrored.Filled.PhoneCallback
                            TriggerType.EMAIL_RECEIVED -> Icons.Default.MarkEmailRead
                            TriggerType.SCHEDULED_CRON -> Icons.Default.Schedule
                            TriggerType.THRESHOLD_EVENT -> Icons.Default.Warning
                            TriggerType.MANUAL_DISPATCH -> Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = template.trigger.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = RcosNeonGreen, modifier = Modifier.size(16.dp))
                    Text(
                        text = template.assignedAgent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Steps & Execution Pipeline (Expandable)
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Execution Pipeline Steps:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RcosNeonGreen)

                    template.steps.forEach { step ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (step.isCompleted) RcosNeonGreen else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${step.stepNumber}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (step.isCompleted) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column {
                                Text(step.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("${step.actionType} • ${step.assignedAgent}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Trigger Config: ${template.trigger.configuration}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (optimizationReport != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF4A148C).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(14.dp))
                            Text("Gemini AI Pipeline Optimization Insights", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD1C4E9))
                        }
                        Text(
                            text = optimizationReport,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide Pipeline Steps" else "View Pipeline Steps (${template.steps.size})", fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { onOptimizeWithGemini() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB39DDB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isOptimizingWithGemini,
                        modifier = Modifier.testTag("gemini_optimize_${template.id}")
                    ) {
                        if (isOptimizingWithGemini) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color(0xFFB39DDB), strokeWidth = 2.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("Analyzing...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFD54F))
                            Spacer(Modifier.width(4.dp))
                            Text("Gemini AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { onTriggerExecution() },
                        colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen.copy(alpha = 0.2f), contentColor = RcosNeonGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("simulate_trigger_${template.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Trigger", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApprovalRequirementCard(
    item: ApprovalItem,
    onApprove: (String) -> Unit,
    onClientApprove: (String) -> Unit,
    onAiAutoApprove: () -> Unit,
    onReject: (String) -> Unit,
    onArchive: () -> Unit = {},
    onRestore: () -> Unit = {}
) {
    val isPending = item.status == ApprovalStatus.PENDING
    val isApproved = item.status == ApprovalStatus.APPROVED || item.status == ApprovalStatus.COMPLETED
    val isArchived = item.status == ApprovalStatus.ARCHIVED
    val isRejected = item.status == ApprovalStatus.REJECTED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("approval_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isApproved -> Color(0xFF064E3B).copy(alpha = 0.25f)
                isArchived -> Color(0xFF1E293B).copy(alpha = 0.5f)
                isRejected -> Color(0xFF7F1D1D).copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isApproved -> RcosNeonGreen.copy(alpha = 0.6f)
                isArchived -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                isRejected -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                else -> Color(0xFFFFB74D).copy(alpha = 0.6f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isApproved -> RcosNeonGreen.copy(alpha = 0.2f)
                            isArchived -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            isRejected -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            else -> Color(0xFFFFB74D).copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = item.id,
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isApproved -> RcosNeonGreen
                                isArchived -> MaterialTheme.colorScheme.onSurfaceVariant
                                isRejected -> MaterialTheme.colorScheme.error
                                else -> Color(0xFFFFB74D)
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = item.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        isApproved -> Color(0xFF064E3B)
                        isArchived -> Color(0xFF334155)
                        isRejected -> Color(0xFF7F1D1D)
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isApproved -> Icons.Default.CheckCircle
                                isArchived -> Icons.Default.Archive
                                isRejected -> Icons.Default.Cancel
                                else -> Icons.Default.WarningAmber
                            },
                            contentDescription = null,
                            tint = when {
                                isApproved -> RcosNeonGreen
                                isArchived -> Color.White
                                isRejected -> Color(0xFFFCA5A5)
                                else -> MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = when {
                                isApproved -> "COMPLETED & DISPATCHED"
                                isArchived -> "ARCHIVED"
                                isRejected -> "REJECTED & HALTED"
                                else -> "Risk: ${item.riskScorePct}%"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isApproved -> RcosNeonGreen
                                isArchived -> Color.White
                                isRejected -> Color(0xFFFCA5A5)
                                else -> MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(item.workflowTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(item.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Proposed AI Action:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RcosNeonGreen)
                    Text(item.proposedAction, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("Requested by: ${item.requestedByAgent} via ${item.triggerSource}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!isPending && !item.reviewedBy.isNullOrBlank()) {
                        Text(
                            text = "Reviewer: ${item.reviewedBy}${if (!item.reviewNotes.isNullOrBlank()) " (${item.reviewNotes})" else ""}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isApproved) RcosNeonGreen else Color(0xFFFCA5A5)
                        )
                    }
                }
            }

            if (isPending) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onApprove("Executive Approved") },
                            colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f).testTag("approve_${item.id}_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Executive Approve", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { onClientApprove("Client Sign-Off Approved") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.1f).testTag("client_approve_${item.id}_btn")
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Client Sign-Off", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAiAutoApprove,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f).testTag("auto_approve_${item.id}_btn")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("AI Auto-Approve", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onReject("Executive Declined") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("reject_${item.id}_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reject", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isApproved && !isArchived) {
                        OutlinedButton(
                            onClick = onArchive,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("archive_${item.id}_btn")
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Archive Task", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onRestore,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("restore_${item.id}_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restore to Pending", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiActionLogCard(log: AiActionLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(log.id, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RcosNeonGreen)
                    Text(log.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "${log.executionTimeMs} ms",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(log.actionSummary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${log.agentName} • ${log.approvalStatus}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(log.outputArtifact, style = MaterialTheme.typography.labelSmall, color = RcosNeonGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AgentResponsibilityCard(
    agent: AgentResponsibility,
    onAutonomyChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RcosNeonGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = RcosNeonGreen, modifier = Modifier.size(22.dp))
                    }

                    Column {
                        Text(agent.agentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${agent.roleTitle} (${agent.department})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RcosNeonGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = agent.autonomyLevel,
                        style = MaterialTheme.typography.labelSmall,
                        color = RcosNeonGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text("Designated Operational Responsibilities:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = RcosNeonGreen)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                agent.keyResponsibilities.forEach { resp ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = RcosNeonGreen, modifier = Modifier.size(14.dp))
                        Text(resp, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Active Workflows: ${agent.activeWorkflowsCount} | Executed Today: ${agent.actionsExecutedToday}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedButton(
                    onClick = {
                        val nextAutonomy = when (agent.autonomyLevel) {
                            "Full Autonomy" -> "Human Approval Required"
                            "Human Approval Required" -> "Advisory Only"
                            else -> "Full Autonomy"
                        }
                        onAutonomyChanged(nextAutonomy)
                    }
                ) {
                    Text("Change Autonomy", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun BusinessConfigSection(
    config: BusinessWorkflowConfig,
    onSave: (BusinessWorkflowConfig) -> Unit
) {
    var companyName by remember { mutableStateOf(config.companyName) }
    var industry by remember { mutableStateOf(config.industry) }
    var bottleneck by remember { mutableStateOf(config.operationalBottleneck) }
    var reductionGoal by remember { mutableStateOf(config.workloadReductionGoalPct.toString()) }
    var thresholdDollars by remember { mutableStateOf(config.autoApprovalThresholdDollars.toString()) }
    var maxRisk by remember { mutableStateOf(config.maxRiskAutoApprovePct.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Business Operational Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = industry,
                onValueChange = { industry = it },
                label = { Text("Industry Sector") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = bottleneck,
                onValueChange = { bottleneck = it },
                label = { Text("Primary Operational Bottleneck") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = reductionGoal,
                    onValueChange = { reductionGoal = it },
                    label = { Text("Workload Reduction %") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = thresholdDollars,
                    onValueChange = { thresholdDollars = it },
                    label = { Text("Auto-Approval Limit ($)") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = maxRisk,
                onValueChange = { maxRisk = it },
                label = { Text("Max Risk Auto-Approve Threshold (%)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onSave(
                        config.copy(
                            companyName = companyName,
                            industry = industry,
                            operationalBottleneck = bottleneck,
                            workloadReductionGoalPct = reductionGoal.toIntOrNull() ?: 85,
                            autoApprovalThresholdDollars = thresholdDollars.toDoubleOrNull() ?: 2500.0,
                            maxRiskAutoApprovePct = maxRisk.toIntOrNull() ?: 20
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().testTag("save_business_config_btn")
            ) {
                Text("Save Business Workflow Parameters", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CreateWorkflowTemplateDialog(
    onDismiss: () -> Unit,
    onCreate: (WorkflowTemplate) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Client Operations") }
    var description by remember { mutableStateOf("") }
    var agentName by remember { mutableStateOf("Onboarding Specialist Agent") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create AI Business Workflow", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Workflow Title") },
                    placeholder = { Text("e.g. Executive Contract Renewal") },
                    modifier = Modifier.fillMaxWidth().testTag("new_wf_title")
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("Finance, Operations, Sales") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Workflow Goal & Description") },
                    modifier = Modifier.fillMaxWidth().testTag("new_wf_desc")
                )

                OutlinedTextField(
                    value = agentName,
                    onValueChange = { agentName = it },
                    label = { Text("Assigned AI Agent") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(
                            WorkflowTemplate(
                                id = "tmpl_${System.currentTimeMillis() % 10000}",
                                title = title.trim(),
                                category = category.trim(),
                                description = description.ifBlank { "Custom automated operational workflow." },
                                assignedAgent = agentName.trim(),
                                trigger = TaskTrigger(
                                    id = "trig_custom",
                                    type = TriggerType.MANUAL_DISPATCH,
                                    name = "Manual Executive Trigger",
                                    description = "Dispatched on-demand by CEO.",
                                    configuration = "Direct Executive Dispatch"
                                ),
                                approvalPolicy = ApprovalRequirement(
                                    id = "app_custom",
                                    level = ApprovalLevel.REQUIRED_IF_HIGH_RISK
                                ),
                                steps = listOf(
                                    WorkflowStep(1, "Initialize Operational Data", "Data Extraction", agentName, true),
                                    WorkflowStep(2, "Synthesize Strategy via Gemini AI", "AI Processing", agentName, false)
                                )
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                modifier = Modifier.testTag("save_new_wf_btn")
            ) {
                Text("Deploy Workflow", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
