@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AgentCapability
import com.example.data.AgentRegistryEntity
import com.example.data.AgentRiskLevel
import com.example.data.AgentStatus
import com.example.data.AgentType
import com.example.data.AiActionLog
import com.example.data.ApprovalStatus
import com.example.ui.NovaViewModel
import com.example.ui.components.AgentLiveTaskDirectorDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

enum class OperationalState(
    val label: String,
    val color: Color,
    val containerColor: Color,
    val icon: ImageVector,
    val description: String
) {
    OPERATIONAL("Operational", Color(0xFF10B981), Color(0xFF064E3B), Icons.Default.CheckCircle, "Healthy & Accepting Triggers"),
    EXECUTING("Executing", Color(0xFF38BDF8), Color(0xFF0C4A6E), Icons.Default.Autorenew, "Active Workflow in Progress"),
    STANDBY("Standby", Color(0xFFA78BFA), Color(0xFF4C1D95), Icons.Default.AccessTime, "Listening for Events"),
    NEEDS_APPROVAL("Needs Approval", Color(0xFFFBBF24), Color(0xFF78350F), Icons.Default.PendingActions, "Awaiting Human Sign-off"),
    PAUSED("Paused", Color(0xFF94A3B8), Color(0xFF334155), Icons.Default.PauseCircle, "Manually Suspended")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActiveAgentsScreen(
    viewModel: NovaViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeWorkspaceId by viewModel.activeWorkspaceId.collectAsState()
    val allWorkspaces by viewModel.allWorkspaces.collectAsState()
    val agents by viewModel.activeWorkspaceAgents.collectAsState()
    val aiLogs by viewModel.aiActionLogs.collectAsState()
    val responsibilities by viewModel.agentResponsibilities.collectAsState()
    val workflows by viewModel.workflowTemplates.collectAsState()
    val approvals by viewModel.approvalItems.collectAsState()

    val currentWorkspace = allWorkspaces.find { it.workspaceId == activeWorkspaceId }

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<OperationalState?>(null) }
    var selectedDepartmentFilter by remember { mutableStateOf("ALL") }

    var agentToDispatch by remember { mutableStateOf<AgentRegistryEntity?>(null) }
    var agentToInspect by remember { mutableStateOf<AgentRegistryEntity?>(null) }
    var agentToLiveDirect by remember { mutableStateOf<AgentRegistryEntity?>(null) }

    // Derive operational states for each agent
    val agentStates = remember(agents, aiLogs, approvals) {
        agents.associate { agent ->
            val opState = deriveAgentOperationalState(agent, aiLogs, approvals)
            agent.agentId to opState
        }
    }

    // Filter agents
    val filteredAgents = remember(agents, searchQuery, selectedStatusFilter, selectedDepartmentFilter, agentStates) {
        agents.filter { agent ->
            val opState = agentStates[agent.agentId] ?: OperationalState.STANDBY
            val matchesSearch = searchQuery.isBlank() ||
                    agent.agentName.contains(searchQuery, ignoreCase = true) ||
                    agent.agentDescription.contains(searchQuery, ignoreCase = true) ||
                    agent.agentType.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == null || opState == selectedStatusFilter
            val matchesDepartment = selectedDepartmentFilter == "ALL" ||
                    (agent.assignedDepartment ?: "General").equals(selectedDepartmentFilter, ignoreCase = true)

            matchesSearch && matchesStatus && matchesDepartment
        }
    }

    // Metrics summary
    val totalActive = agents.count { it.status == AgentStatus.ACTIVE.name }
    val totalExecuting = agentStates.values.count { it == OperationalState.EXECUTING }
    val totalOperational = agentStates.values.count { it == OperationalState.OPERATIONAL }
    val totalApprovalsNeeded = agentStates.values.count { it == OperationalState.NEEDS_APPROVAL }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF0F172A),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Active Agents Dashboard",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    LiveHeartbeatPulse()
                                }
                                Text(
                                    text = "${currentWorkspace?.companyName ?: "Enterprise"} • Autonomous Fleet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isWideScreen = maxWidth >= 840.dp
            val isMediumScreen = maxWidth >= 600.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("active_agents_dashboard_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. KPI Telemetry Summary Row
                item {
                    AgentsKpiSummaryRow(
                        totalActive = totalActive,
                        totalExecuting = totalExecuting,
                        totalOperational = totalOperational,
                        totalApprovalsNeeded = totalApprovalsNeeded,
                        isMediumScreen = isMediumScreen
                    )
                }

                // 2. Search and Status Filter Chips
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_agents_input"),
                            placeholder = { Text("Search agents by name, role, or capability...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        // Status Filter Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedStatusFilter == null,
                                    onClick = { selectedStatusFilter = null },
                                    label = { Text("All Statuses (${agents.size})") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                            items(OperationalState.values()) { state ->
                                val count = agentStates.values.count { it == state }
                                FilterChip(
                                    selected = selectedStatusFilter == state,
                                    onClick = {
                                        selectedStatusFilter = if (selectedStatusFilter == state) null else state
                                    },
                                    label = { Text("${state.label} ($count)") },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(state.color, CircleShape)
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Department Filter Chips
                        val departments = remember(agents) {
                            listOf("ALL") + agents.mapNotNull { it.assignedDepartment }.distinct().sorted()
                        }
                        if (departments.size > 2) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(departments) { dept ->
                                    FilterChip(
                                        selected = selectedDepartmentFilter == dept,
                                        onClick = { selectedDepartmentFilter = dept },
                                        label = { Text(if (dept == "ALL") "All Departments" else dept) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Operational Fleet (${filteredAgents.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "Auto-Synced Live",
                            style = MaterialTheme.typography.labelSmall,
                            color = RcosNeonGreen
                        )
                    }
                }

                // 4. Empty State or Active Agent Cards
                if (filteredAgents.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, Color(0xFF1E2A42)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "No active agents match your criteria.",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Try clearing the search query or changing the operational status filter.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedStatusFilter = null
                                        selectedDepartmentFilter = "ALL"
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }
                } else {
                    if (isWideScreen) {
                        // 2-column layout for wide screens
                        val chunked = filteredAgents.chunked(2)
                        items(chunked) { rowPair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                for (agent in rowPair) {
                                    val opState = agentStates[agent.agentId] ?: OperationalState.STANDBY
                                    val agentResp = responsibilities.find { it.agentName.contains(agent.agentName, true) || agent.agentName.contains(it.agentName, true) }
                                    val recentLog = aiLogs.find { it.agentName.contains(agent.agentName, true) }
                                    val linkedWorkflows = workflows.filter { it.assignedAgent.contains(agent.agentName, true) || agent.agentName.contains(it.assignedAgent, true) }

                                    ActiveAgentOperationalCard(
                                        agent = agent,
                                        operationalState = opState,
                                        responsibility = agentResp,
                                        latestLog = recentLog,
                                        linkedWorkflowsCount = linkedWorkflows.size,
                                        onLiveDirect = { agentToLiveDirect = agent },
                                        onDispatchTask = { agentToDispatch = agent },
                                        onInspect = { agentToInspect = agent },
                                        onToggleStatus = {
                                            if (agent.status == AgentStatus.ACTIVE.name) {
                                                viewModel.disableAgent(agent.agentId)
                                            } else {
                                                viewModel.enableAgent(agent.agentId)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowPair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        // Single column layout
                        items(filteredAgents, key = { it.agentId }) { agent ->
                            val opState = agentStates[agent.agentId] ?: OperationalState.STANDBY
                            val agentResp = responsibilities.find { it.agentName.contains(agent.agentName, true) || agent.agentName.contains(it.agentName, true) }
                            val recentLog = aiLogs.find { it.agentName.contains(agent.agentName, true) }
                            val linkedWorkflows = workflows.filter { it.assignedAgent.contains(agent.agentName, true) || agent.agentName.contains(it.assignedAgent, true) }

                            ActiveAgentOperationalCard(
                                agent = agent,
                                operationalState = opState,
                                responsibility = agentResp,
                                latestLog = recentLog,
                                linkedWorkflowsCount = linkedWorkflows.size,
                                onLiveDirect = { agentToLiveDirect = agent },
                                onDispatchTask = { agentToDispatch = agent },
                                onInspect = { agentToInspect = agent },
                                onToggleStatus = {
                                    if (agent.status == AgentStatus.ACTIVE.name) {
                                        viewModel.disableAgent(agent.agentId)
                                    } else {
                                        viewModel.enableAgent(agent.agentId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 5. Dispatch Task Dialog
    agentToDispatch?.let { agent ->
        DispatchTaskDialog(
            agent = agent,
            onDismiss = { agentToDispatch = null },
            onExecute = { directive, riskPct, dollarLimit ->
                viewModel.executeAgentTask(
                    agentId = agent.agentId,
                    taskDescription = directive,
                    riskScorePct = riskPct,
                    dollarAmount = dollarLimit
                )
                agentToDispatch = null
            }
        )
    }

    // 6. Agent Deep Telemetry & Details Dialog
    agentToInspect?.let { agent ->
        val opState = agentStates[agent.agentId] ?: OperationalState.STANDBY
        val agentResp = responsibilities.find { it.agentName.contains(agent.agentName, true) }
        val agentLogs = aiLogs.filter { it.agentName.contains(agent.agentName, true) }

        AgentTelemetryDialog(
            agent = agent,
            operationalState = opState,
            responsibility = agentResp,
            logs = agentLogs,
            onDismiss = { agentToInspect = null }
        )
    }

    // 7. Live Task View & Direct Agent Dialog
    agentToLiveDirect?.let { agent ->
        val opState = agentStates[agent.agentId] ?: OperationalState.STANDBY
        val agentResp = responsibilities.find { it.agentName.contains(agent.agentName, true) }
        val agentLogs = aiLogs.filter { it.agentName.contains(agent.agentName, true) }

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

// -------------------------------------------------------------
// OPERATIONAL STATUS CARD COMPONENT
// -------------------------------------------------------------
@Composable
fun ActiveAgentOperationalCard(
    agent: AgentRegistryEntity,
    operationalState: OperationalState,
    responsibility: com.example.data.AgentResponsibility?,
    latestLog: AiActionLog?,
    linkedWorkflowsCount: Int,
    onLiveDirect: () -> Unit,
    onDispatchTask: () -> Unit,
    onInspect: () -> Unit,
    onToggleStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pseudoLatency = remember(agent.agentId) { 140 + (agent.agentId.hashCode().absoluteValue % 180) }
    val executionsToday = responsibility?.actionsExecutedToday ?: (8 + (agent.agentId.hashCode().absoluteValue % 24))
    val successRate = remember(agent.agentId) {
        if (operationalState == OperationalState.NEEDS_APPROVAL) "98.2%" else "99.8%"
    }

    val agentIcon = remember(agent.agentType) {
        when {
            agent.agentType.contains("EXECUTIVE", true) -> Icons.Default.Psychology
            agent.agentType.contains("FINANCE", true) -> Icons.Default.AccountBalance
            agent.agentType.contains("SUPPORT", true) -> Icons.Default.SupportAgent
            agent.agentType.contains("VOICE", true) || agent.agentName.contains("Voice", true) -> Icons.Default.Mic
            agent.agentType.contains("COMPLIANCE", true) -> Icons.Default.Gavel
            agent.agentType.contains("ANALYTICS", true) -> Icons.Default.Analytics
            agent.agentType.contains("ONBOARDING", true) || agent.agentName.contains("Onboarding", true) -> Icons.Default.PersonAdd
            else -> Icons.Default.SmartToy
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onLiveDirect() }
            .testTag("agent_card_${agent.agentId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = BorderStroke(
            1.dp,
            if (operationalState == OperationalState.EXECUTING) RcosNeonGreen else Color(0xFF1F2937)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Row 1: Avatar, Name, Department & Operational Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = operationalState.containerColor,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = agentIcon,
                                contentDescription = null,
                                tint = operationalState.color,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = agent.agentName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = agent.assignedDepartment ?: "Operations",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = agent.modelTier.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = RcosNeonGreen
                            )
                        }
                    }
                }

                // Operational Status Badge
                OperationalStatusBadge(state = operationalState)
            }

            // Description
            Text(
                text = agent.agentDescription,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Live Performance Telemetry Grid (4 Micro-KPIs)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TelemetryStatItem(
                        label = "Executions",
                        value = "$executionsToday",
                        icon = Icons.Default.Bolt,
                        color = RcosNeonGreen
                    )
                    VerticalDivider(
                        modifier = Modifier.height(24.dp),
                        color = Color(0xFF334155)
                    )
                    TelemetryStatItem(
                        label = "Latency",
                        value = "${pseudoLatency}ms",
                        icon = Icons.Default.Speed,
                        color = Color(0xFF38BDF8)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(24.dp),
                        color = Color(0xFF334155)
                    )
                    TelemetryStatItem(
                        label = "Success",
                        value = successRate,
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF10B981)
                    )
                    VerticalDivider(
                        modifier = Modifier.height(24.dp),
                        color = Color(0xFF334155)
                    )
                    TelemetryStatItem(
                        label = "Pipelines",
                        value = "${linkedWorkflowsCount.coerceAtLeast(1)} Active",
                        icon = Icons.Default.AccountTree,
                        color = Color(0xFFA78BFA)
                    )
                }
            }

            // Latest Operational Activity Log Snippet
            if (latestLog != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Last Live Action (${latestLog.timestamp})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = latestLog.actionSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Capabilities Tags Row
            val capabilities = agent.getCapabilitiesList()
            if (capabilities.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    capabilities.take(4).forEach { capStr ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1F2937),
                            border = BorderStroke(1.dp, Color(0xFF374151))
                        ) {
                            Text(
                                text = capStr.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (capabilities.size > 4) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1F2937)
                        ) {
                            Text(
                                text = "+${capabilities.size - 4}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onLiveDirect,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(38.dp)
                        .testTag("live_direct_agent_btn_${agent.agentId}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RcosNeonGreen,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Live Task & Direct",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onDispatchTask,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("dispatch_agent_btn_${agent.agentId}"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Dispatch",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onInspect,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF1F2937), RoundedCornerShape(10.dp))
                        .testTag("inspect_agent_btn_${agent.agentId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Telemetry Details",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onToggleStatus,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF1F2937), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = if (agent.status == AgentStatus.ACTIVE.name) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle status",
                        tint = if (agent.status == AgentStatus.ACTIVE.name) Color(0xFFF87171) else Color(0xFF34D399),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// OPERATIONAL STATUS BADGE WITH ANIMATED HEARTBEAT
// -------------------------------------------------------------
@Composable
fun OperationalStatusBadge(state: OperationalState) {
    Surface(
        color = state.containerColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, state.color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (state == OperationalState.OPERATIONAL || state == OperationalState.EXECUTING) {
                PulsingStatusDot(color = state.color)
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(state.color, CircleShape)
                )
            }

            Text(
                text = state.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = state.color
            )
        }
    }
}

@Composable
fun PulsingStatusDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .background(color, CircleShape)
    )
}

@Composable
fun LiveHeartbeatPulse() {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartbeat_alpha"
    )

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF064E3B).copy(alpha = alpha),
        border = BorderStroke(1.dp, RcosNeonGreen)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(RcosNeonGreen, CircleShape)
            )
            Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp
                ),
                color = RcosNeonGreen
            )
        }
    }
}

// -------------------------------------------------------------
// TELEMETRY STAT ITEM
// -------------------------------------------------------------
@Composable
private fun TelemetryStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color(0xFF94A3B8)
        )
    }
}

// -------------------------------------------------------------
// TOP KPI METRICS SUMMARY ROW
// -------------------------------------------------------------
@Composable
fun AgentsKpiSummaryRow(
    totalActive: Int,
    totalExecuting: Int,
    totalOperational: Int,
    totalApprovalsNeeded: Int,
    isMediumScreen: Boolean
) {
    if (isMediumScreen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiMetricCard(
                title = "Active Fleet",
                count = "$totalActive Agents",
                subtitle = "Registered in workspace",
                icon = Icons.Default.SmartToy,
                color = RcosNeonGreen,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Operational",
                count = "$totalOperational Ready",
                subtitle = "Nominal telemetry",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Executing Tasks",
                count = "$totalExecuting Live",
                subtitle = "Pipelines in flight",
                icon = Icons.Default.Autorenew,
                color = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Pending Approvals",
                count = "$totalApprovalsNeeded Queued",
                subtitle = "High-risk authorizations",
                icon = Icons.Default.PendingActions,
                color = Color(0xFFFBBF24),
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    title = "Active Fleet",
                    count = "$totalActive",
                    subtitle = "Autonomous",
                    icon = Icons.Default.SmartToy,
                    color = RcosNeonGreen,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Operational",
                    count = "$totalOperational",
                    subtitle = "Nominal",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    title = "Executing Tasks",
                    count = "$totalExecuting",
                    subtitle = "In Flight",
                    icon = Icons.Default.Autorenew,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Approvals",
                    count = "$totalApprovalsNeeded",
                    subtitle = "Queued",
                    icon = Icons.Default.PendingActions,
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    count: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        border = BorderStroke(1.dp, Color(0xFF1F2937))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = count,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFE2E8F0)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// DISPATCH TASK MODAL DIALOG
// -------------------------------------------------------------
@Composable
fun DispatchTaskDialog(
    agent: AgentRegistryEntity,
    onDismiss: () -> Unit,
    onExecute: (String, Int, Double) -> Unit
) {
    var directiveText by remember { mutableStateOf("") }
    var riskPct by remember { mutableStateOf(15) }
    var dollarAmount by remember { mutableStateOf(0.0) }

    val presetTasks = remember(agent.agentType) {
        when {
            agent.agentType.contains("FINANCE", true) -> listOf(
                "Audit pending retainer invoice #INV-9021",
                "Flag high-expense line items over $1,500",
                "Reconcile QuickBooks ledger entries"
            )
            agent.agentType.contains("ONBOARDING", true) || agent.agentName.contains("Onboarding", true) -> listOf(
                "Parse incoming client intake document",
                "Verify KYC credentials and contracts",
                "Provision cloud repository and tenant"
            )
            agent.agentType.contains("VOICE", true) || agent.agentName.contains("Voice", true) -> listOf(
                "Transcribe audio recording & extract intent",
                "Create urgent client follow-up CRM job",
                "Draft calendar scheduling confirmation"
            )
            agent.agentType.contains("EXECUTIVE", true) -> listOf(
                "Synthesize weekly executive market brief",
                "Analyze workload optimization progress",
                "Draft C-Suite operational priorities"
            )
            else -> listOf(
                "Execute automated workflow step",
                "Query database records & generate summary",
                "Inspect system logs for anomalies"
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = RcosNeonGreen
                )
                Column {
                    Text(
                        text = "Dispatch Agent Task",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Target: ${agent.agentName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Directive Instructions:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                OutlinedTextField(
                    value = directiveText,
                    onValueChange = { directiveText = it },
                    placeholder = { Text("Describe specific task or workflow action for the agent to execute...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("dispatch_directive_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presetTasks.forEach { preset ->
                        Surface(
                            onClick = { directiveText = preset },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = RcosNeonGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExecute(directiveText.ifBlank { presetTasks.first() }, riskPct, dollarAmount) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = RcosNeonGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_dispatch_btn")
            ) {
                Text("Execute Task", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF111827),
        shape = RoundedCornerShape(20.dp)
    )
}

// -------------------------------------------------------------
// AGENT TELEMETRY & INSPECTOR DIALOG
// -------------------------------------------------------------
@Composable
fun AgentTelemetryDialog(
    agent: AgentRegistryEntity,
    operationalState: OperationalState,
    responsibility: com.example.data.AgentResponsibility?,
    logs: List<AiActionLog>,
    onDismiss: () -> Unit
) {
    val dateStr = remember(agent.createdTimestamp) {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(agent.createdTimestamp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = RcosNeonGreen
                    )
                    Column {
                        Text(
                            text = agent.agentName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Agent ID: ${agent.agentId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                OperationalStatusBadge(state = operationalState)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Agent Profile Details
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "GOVERNANCE & PERMISSIONS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = RcosNeonGreen
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Model Tier:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text(agent.modelTier, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Risk Level:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text(agent.riskClassification, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Access Level:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text(agent.permissionLevel, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Registered:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }

                // Assigned Capabilities
                Text(
                    text = "Active Capabilities",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    agent.getCapabilitiesList().forEach { cap ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Text(
                                text = cap.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Recent Execution Logs
                Text(
                    text = "Live Execution Logs (${logs.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                if (logs.isEmpty()) {
                    Text(
                        text = "No recent action logs recorded for this agent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        logs.take(3).forEach { log ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = log.workflowTitle,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = RcosNeonGreen
                                        )
                                        Text(
                                            text = log.timestamp,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Text(
                                        text = log.actionSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF111827),
        shape = RoundedCornerShape(20.dp)
    )
}

// -------------------------------------------------------------
// HELPER FUNCTION: DERIVE OPERATIONAL STATE
// -------------------------------------------------------------
fun deriveAgentOperationalState(
    agent: AgentRegistryEntity,
    logs: List<AiActionLog>,
    approvals: List<com.example.data.ApprovalItem>
): OperationalState {
    if (agent.status != AgentStatus.ACTIVE.name) {
        return OperationalState.PAUSED
    }

    // Check if there is a pending human approval requested by this agent
    val hasPendingApproval = approvals.any {
        it.status == ApprovalStatus.PENDING &&
                (it.requestedByAgent.contains(agent.agentName, ignoreCase = true) || agent.agentName.contains(it.requestedByAgent, ignoreCase = true))
    }
    if (hasPendingApproval) {
        return OperationalState.NEEDS_APPROVAL
    }

    // Check if agent executed actions recently
    val recentExecuting = logs.any {
        (it.agentName.contains(agent.agentName, ignoreCase = true) || agent.agentName.contains(it.agentName, ignoreCase = true)) &&
                (it.timestamp.contains("Just now", true) || it.timestamp.contains("mins ago", true))
    }
    if (recentExecuting) {
        return OperationalState.EXECUTING
    }

    // Pseudo distribution to make dashboard realistic based on agent ID hash
    val pseudoCode = (agent.agentId.hashCode().absoluteValue) % 10
    return when {
        pseudoCode < 5 -> OperationalState.OPERATIONAL
        pseudoCode < 8 -> OperationalState.EXECUTING
        else -> OperationalState.STANDBY
    }
}
