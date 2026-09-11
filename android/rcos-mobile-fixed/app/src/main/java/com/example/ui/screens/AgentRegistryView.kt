package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.data.AgentRiskLevel as EntityRiskLevel
import com.example.ui.NovaViewModel
import com.example.ui.components.AgentLiveTaskDirectorDialog
import com.example.ui.theme.RcosNeonGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgentRegistryView(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val activeWorkspaceId by viewModel.activeWorkspaceId.collectAsState()
    val allWorkspaces by viewModel.allWorkspaces.collectAsState()
    val agents by viewModel.activeWorkspaceAgents.collectAsState()
    val accounts by viewModel.activeWorkspaceUserAccounts.collectAsState()
    val aiLogs by viewModel.aiActionLogs.collectAsState()
    val responsibilities by viewModel.agentResponsibilities.collectAsState()
    val currentActor = remember(accounts) { viewModel.getCurrentActor() }

    val currentWorkspace = allWorkspaces.find { it.workspaceId == activeWorkspaceId }
        ?: WorkspaceEntity(activeWorkspaceId, "Current Workspace", "Enterprise")

    val viewAuthResult = remember(currentActor, activeWorkspaceId) {
        PermissionEngine.evaluatePermission(
            user = currentActor,
            targetWorkspaceId = activeWorkspaceId,
            action = PermissionAction.VIEW_AGENTS
        )
    }

    val canManage = remember(currentActor, activeWorkspaceId) {
        PermissionEngine.evaluatePermission(
            user = currentActor,
            targetWorkspaceId = activeWorkspaceId,
            action = PermissionAction.MANAGE_AGENTS
        ).isAllowed
    }

    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var agentToExecuteTask by remember { mutableStateOf<AgentRegistryEntity?>(null) }
    var agentToEditAssign by remember { mutableStateOf<AgentRegistryEntity?>(null) }
    var agentToLiveDirect by remember { mutableStateOf<AgentRegistryEntity?>(null) }

    val filteredAgents = remember(agents, selectedStatusFilter, selectedTypeFilter, searchQuery) {
        agents.filter { agent ->
            val matchesStatus = selectedStatusFilter == "ALL" || agent.status == selectedStatusFilter
            val matchesType = selectedTypeFilter == "ALL" || agent.agentType == selectedTypeFilter
            val matchesSearch = searchQuery.isBlank() ||
                    agent.agentName.contains(searchQuery, ignoreCase = true) ||
                    agent.agentDescription.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesType && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Agent Registry",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Workspace: ${currentWorkspace.companyName} (${currentWorkspace.domain})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (canManage) {
                            Button(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier.testTag("create_agent_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Register Agent")
                            }
                        }
                    }

                    if (!viewAuthResult.isAllowed) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Access Restricted: ${viewAuthResult.reason}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (!viewAuthResult.isAllowed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("You do not have permission to view AI agents in this workspace.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Metrics Overview
                item {
                    AgentMetricsHeader(agents = agents)
                }

                // Search and Filters
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search agents by name or purpose...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("agent_search_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Status Filter Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Text(
                                    text = "Status:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }

                            val statuses = listOf("ALL", "ACTIVE", "INACTIVE", "SUSPENDED", "PENDING_APPROVAL")
                            items(statuses) { status ->
                                FilterChip(
                                    selected = selectedStatusFilter == status,
                                    onClick = { selectedStatusFilter = status },
                                    label = { Text(status) },
                                    modifier = Modifier.testTag("filter_status_$status")
                                )
                            }
                        }
                    }
                }

                // Agents List
                if (filteredAgents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (agents.isEmpty()) "No AI Agents registered in workspace." else "No agents match filter criteria.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredAgents, key = { it.agentId }) { agent ->
                        AgentRegistryCard(
                            agent = agent,
                            canManage = canManage,
                            onLiveDirect = { agentToLiveDirect = agent },
                            onEnable = { viewModel.enableAgent(agent.agentId) },
                            onDisable = { viewModel.disableAgent(agent.agentId) },
                            onAssign = { agentToEditAssign = agent },
                            onExecuteTask = { agentToExecuteTask = agent }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateAgentDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, type, risk, capabilities, modelTier, dept, users ->
                viewModel.createAgent(
                    agentName = name,
                    agentDescription = desc,
                    agentType = type,
                    riskClassification = risk,
                    capabilities = capabilities,
                    modelTier = modelTier,
                    department = dept,
                    assignedUsers = users
                )
                showCreateDialog = false
            }
        )
    }

    agentToExecuteTask?.let { agent ->
        ExecuteAgentTaskDialog(
            agent = agent,
            onDismiss = { agentToExecuteTask = null },
            onExecute = { taskDesc, riskScore, dollarAmt ->
                viewModel.executeAgentTask(
                    agentId = agent.agentId,
                    taskDescription = taskDesc,
                    riskScorePct = riskScore,
                    dollarAmount = dollarAmt
                )
                agentToExecuteTask = null
            }
        )
    }

    agentToEditAssign?.let { agent ->
        AssignAgentDialog(
            agent = agent,
            onDismiss = { agentToEditAssign = null },
            onSave = { dept, users ->
                viewModel.assignAgent(agent.agentId, dept, users)
                agentToEditAssign = null
            }
        )
    }

    agentToLiveDirect?.let { agent ->
        val agentResp = responsibilities.find { it.agentName.contains(agent.agentName, true) }
        val agentLogs = aiLogs.filter { it.agentName.contains(agent.agentName, true) }
        val opState = deriveAgentOperationalState(agent, aiLogs, emptyList())

        AgentLiveTaskDirectorDialog(
            agent = agent,
            operationalState = opState,
            responsibility = agentResp,
            latestLogs = agentLogs,
            viewModel = viewModel,
            onDismiss = { agentToLiveDirect = null }
        )
    }
}

@Composable
fun AgentMetricsHeader(agents: List<AgentRegistryEntity>) {
    val total = agents.size
    val active = agents.count { it.status == "ACTIVE" }
    val highRisk = agents.count { it.riskClassification == "HIGH" || it.riskClassification == "CRITICAL" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricSummaryCard(
            title = "Total Agents",
            value = total.toString(),
            icon = Icons.Default.SmartToy,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        MetricSummaryCard(
            title = "Active",
            value = active.toString(),
            icon = Icons.Default.CheckCircle,
            color = Color(0xFF10B981),
            modifier = Modifier.weight(1f)
        )
        MetricSummaryCard(
            title = "High/Critical Risk",
            value = highRisk.toString(),
            icon = Icons.Default.Warning,
            color = Color(0xFFEF4444),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MetricSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentRegistryCard(
    agent: AgentRegistryEntity,
    canManage: Boolean,
    onLiveDirect: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onAssign: () -> Unit,
    onExecuteTask: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (agent.status) {
        "ACTIVE" -> Color(0xFF10B981)
        "SUSPENDED" -> Color(0xFFEF4444)
        "PENDING_APPROVAL" -> Color(0xFFF59E0B)
        else -> Color.Gray
    }

    val riskColor = when (agent.riskClassification) {
        "CRITICAL" -> Color(0xFFDC2626)
        "HIGH" -> Color(0xFFEF4444)
        "MEDIUM" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLiveDirect() }
            .testTag("agent_card_${agent.agentId}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = agent.agentName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = agent.agentType.replace("_", " "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Pill
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = agent.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Risk Pill
                    Surface(
                        color = riskColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = agent.riskClassification,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = riskColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = agent.agentDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Capabilities Profile Badges
            val capabilities = agent.getCapabilitiesList()
            if (capabilities.isNotEmpty()) {
                Text(
                    text = "Capabilities:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    capabilities.forEach { cap ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(cap, style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onLiveDirect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RcosNeonGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("live_direct_${agent.agentId}")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Live Task & Direct", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onExecuteTask,
                        enabled = agent.canExecute(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("execute_task_${agent.agentId}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dispatch")
                    }

                    if (canManage) {
                        OutlinedButton(
                            onClick = onAssign,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Assign")
                        }
                    }
                }

                if (canManage) {
                    if (agent.status == "ACTIVE") {
                        TextButton(onClick = onDisable) {
                            Text("Disable", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        TextButton(onClick = onEnable) {
                            Text("Enable", color = Color(0xFF10B981))
                        }
                    }
                }
            }

            // Expandable details
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (expanded) "Hide Governance Details" else "View Governance Details")
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GovernanceDetailRow("Agent ID", agent.agentId)
                    GovernanceDetailRow("Model Tier", agent.modelTier)
                    GovernanceDetailRow("Permission Level", agent.permissionLevel)
                    GovernanceDetailRow("Assigned Department", agent.assignedDepartment ?: "Unassigned (Workspace Wide)")
                    GovernanceDetailRow("Assigned Users", agent.assignedUsers ?: "All Authorized Users")
                    GovernanceDetailRow("Created By", agent.createdBy)
                    GovernanceDetailRow(
                        "Created At",
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(agent.createdTimestamp))
                    )
                }
            }
        }
    }
}

@Composable
fun GovernanceDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateAgentDialog(
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        desc: String,
        type: AgentType,
        risk: EntityRiskLevel,
        capabilities: List<String>,
        modelTier: String,
        dept: String?,
        users: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AgentType.OPERATIONS_AGENT) }
    var selectedRisk by remember { mutableStateOf(EntityRiskLevel.LOW) }
    var selectedModelTier by remember { mutableStateOf("GEMINI_2_5_FLASH") }
    var department by remember { mutableStateOf("") }
    var assignedUsers by remember { mutableStateOf("") }

    val selectedCapabilities = remember { mutableStateListOf<String>() }

    // Auto-populate default capabilities when type changes
    LaunchedEffect(selectedType) {
        selectedCapabilities.clear()
        selectedCapabilities.addAll(selectedType.defaultCapabilities)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New AI Agent") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Agent Name *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_agent_name"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Purpose / Operational Description *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_agent_description"),
                        maxLines = 3
                    )
                }

                item {
                    Text("Agent Type", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AgentType.entries) { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.label) }
                            )
                        }
                    }
                }

                item {
                    Text("Risk Classification", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(EntityRiskLevel.entries) { risk ->
                            FilterChip(
                                selected = selectedRisk == risk,
                                onClick = { selectedRisk = risk },
                                label = { Text(risk.label) }
                            )
                        }
                    }
                }

                item {
                    Text("Capabilities Profile", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AgentCapability.entries.forEach { cap ->
                            val isSelected = selectedCapabilities.contains(cap.name)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedCapabilities.remove(cap.name)
                                    else selectedCapabilities.add(cap.name)
                                },
                                label = { Text(cap.label) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Assigned Department (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = assignedUsers,
                        onValueChange = { assignedUsers = it },
                        label = { Text("Assigned Users (Comma-separated emails, Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && description.isNotBlank()) {
                        onCreate(
                            name,
                            description,
                            selectedType,
                            selectedRisk,
                            selectedCapabilities,
                            selectedModelTier,
                            department.ifBlank { null },
                            assignedUsers.ifBlank { null }
                        )
                    }
                },
                enabled = name.isNotBlank() && description.isNotBlank(),
                modifier = Modifier.testTag("submit_create_agent")
            ) {
                Text("Register Agent")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExecuteAgentTaskDialog(
    agent: AgentRegistryEntity,
    onDismiss: () -> Unit,
    onExecute: (taskDesc: String, riskScore: Int, dollarAmt: Double) -> Unit
) {
    var taskDesc by remember { mutableStateOf("") }
    var riskScore by remember { mutableStateOf("15") }
    var dollarAmt by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dispatch Task to ${agent.agentName}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Agent: ${agent.agentName} (${agent.agentType})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = taskDesc,
                    onValueChange = { taskDesc = it },
                    label = { Text("Task Instructions / Payload *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_instructions_input"),
                    maxLines = 3
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = riskScore,
                        onValueChange = { riskScore = it },
                        label = { Text("Risk Score (%)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("task_risk_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dollarAmt,
                        onValueChange = { dollarAmt = it },
                        label = { Text("Dollar Impact ($)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("task_dollar_input"),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskDesc.isNotBlank()) {
                        val risk = riskScore.toIntOrNull() ?: 0
                        val amt = dollarAmt.toDoubleOrNull() ?: 0.0
                        onExecute(taskDesc, risk, amt)
                    }
                },
                enabled = taskDesc.isNotBlank(),
                modifier = Modifier.testTag("confirm_execute_task")
            ) {
                Text("Dispatch Execution")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AssignAgentDialog(
    agent: AgentRegistryEntity,
    onDismiss: () -> Unit,
    onSave: (dept: String?, users: String?) -> Unit
) {
    var department by remember { mutableStateOf(agent.assignedDepartment ?: "") }
    var assignedUsers by remember { mutableStateOf(agent.assignedUsers ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Agent '${agent.agentName}'") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Assigned Department") },
                    placeholder = { Text("e.g., Client Operations, Executive Office") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = assignedUsers,
                    onValueChange = { assignedUsers = it },
                    label = { Text("Assigned Users (Comma-separated emails/IDs)") },
                    placeholder = { Text("e.g., ceo@rcos.global, m.chen@rcos.global") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(department.ifBlank { null }, assignedUsers.ifBlank { null })
                }
            ) {
                Text("Save Assignments")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
