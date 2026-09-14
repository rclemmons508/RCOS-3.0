@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AgentRegistryEntity
import com.example.data.AgentResponsibility
import com.example.data.AgentStatus
import com.example.data.AgentType
import com.example.data.AiActionLog
import com.example.ui.NovaViewModel
import com.example.ui.screens.deriveAgentOperationalState
import com.example.ui.theme.RcosNeonGreen
import com.example.ui.theme.RcosNeonGreenBright

/**
 * Main dashboard UI component in Jetpack Compose that lists active and paused autonomous agents
 * with their current status, quick toggle action buttons, and recent activity logs using a LazyColumn.
 */
@Composable
fun AutonomousAgentsDashboard(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier,
    onNavigateToWorkflows: () -> Unit = {},
    onNavigateToAgents: () -> Unit = {}
) {
    val agents by viewModel.activeWorkspaceAgents.collectAsState()
    val allLogs by viewModel.aiActionLogs.collectAsState()
    val responsibilities by viewModel.agentResponsibilities.collectAsState()
    val firestoreSyncInfo by viewModel.firestoreSyncInfo.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var agentToDirect by remember { mutableStateOf<AgentRegistryEntity?>(null) }
    var expandedAgentId by remember { mutableStateOf<String?>(null) }

    // Pulsing animation for active agent status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val activeCount = agents.count { it.status.equals(AgentStatus.ACTIVE.name, ignoreCase = true) }
    val pausedCount = agents.count { !it.status.equals(AgentStatus.ACTIVE.name, ignoreCase = true) }
    val totalExecutionsToday = allLogs.size

    val filteredAgents = remember(agents, searchQuery, selectedFilter) {
        agents.filter { agent ->
            val isActive = agent.status.equals(AgentStatus.ACTIVE.name, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "ACTIVE" -> isActive
                "PAUSED" -> !isActive
                "OPERATIONS" -> agent.agentType.contains("OPERATIONS", ignoreCase = true) || agent.assignedDepartment?.contains("Operations", ignoreCase = true) == true
                "FINANCE" -> agent.agentType.contains("FINANCE", ignoreCase = true) || agent.assignedDepartment?.contains("Finance", ignoreCase = true) == true
                "EXECUTIVE" -> agent.agentType.contains("EXECUTIVE", ignoreCase = true) || agent.assignedDepartment?.contains("Executive", ignoreCase = true) == true
                "HIGH_RISK" -> agent.riskClassification.contains("HIGH", ignoreCase = true) || agent.riskClassification.contains("CRITICAL", ignoreCase = true)
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                agent.agentName.contains(searchQuery, ignoreCase = true) ||
                        agent.agentDescription.contains(searchQuery, ignoreCase = true) ||
                        (agent.assignedDepartment?.contains(searchQuery, ignoreCase = true) == true) ||
                        agent.agentType.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("autonomous_agents_dashboard_root")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("autonomous_agents_lazy_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Dashboard Component Header & Fleet Overview Card
            item(key = "dashboard_header_card") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().testTag("fleet_overview_card")
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        RcosNeonGreen.copy(alpha = 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
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
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(RcosNeonGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = "Autonomous Agents",
                                        tint = RcosNeonGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Autonomous Agent Fleet",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (activeCount > 0) RcosNeonGreen.copy(alpha = 0.2f) else Color(0xFFFFB74D).copy(alpha = 0.2f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (activeCount > 0) RcosNeonGreen.copy(alpha = pulseAlpha) else Color(0xFFFFB74D))
                                                )
                                                Text(
                                                    text = if (activeCount > 0) "$activeCount ACTIVE" else "ALL PAUSED",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (activeCount > 0) RcosNeonGreen else Color(0xFFFFB74D)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Control live execution status, task dispatching & audit telemetry",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.triggerFirestoreManualSync() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .testTag("dashboard_sync_refresh_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh & Sync",
                                    tint = RcosNeonGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 4 Quick High-Level Metrics (Active, Paused, Actions Today, Sync)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FleetMetricChip(
                                label = "Active",
                                value = "$activeCount",
                                icon = Icons.Default.PlayCircle,
                                tint = RcosNeonGreen,
                                modifier = Modifier.weight(1f)
                            )
                            FleetMetricChip(
                                label = "Paused",
                                value = "$pausedCount",
                                icon = Icons.Default.PauseCircle,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.weight(1f)
                            )
                            FleetMetricChip(
                                label = "Actions",
                                value = "$totalExecutionsToday",
                                icon = Icons.AutoMirrored.Filled.ListAlt,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.weight(1f)
                            )
                            FleetMetricChip(
                                label = "Cloud Sync",
                                value = if (firestoreSyncInfo.state == com.example.data.FirestoreSyncState.CONNECTED) "Live" else "Saved",
                                icon = Icons.Default.CloudDone,
                                tint = if (firestoreSyncInfo.state == com.example.data.FirestoreSyncState.CONNECTED) RcosNeonGreen else Color(0xFFFFB74D),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Search & Filter Bar
            item(key = "dashboard_search_filter_bar") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("agent_search_input"),
                        placeholder = {
                            Text(
                                "Search agents by name, role, department, or capability...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = RcosNeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RcosNeonGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        singleLine = true
                    )

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filterOptions = listOf(
                            "ALL" to "All Agents (${agents.size})",
                            "ACTIVE" to "Active ($activeCount)",
                            "PAUSED" to "Paused ($pausedCount)",
                            "OPERATIONS" to "Operations",
                            "FINANCE" to "Finance",
                            "EXECUTIVE" to "Executive",
                            "HIGH_RISK" to "High Risk"
                        )
                        items(filterOptions) { (key, label) ->
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { selectedFilter = key },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                leadingIcon = if (selectedFilter == key) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (selectedFilter == key) Color.Black else RcosNeonGreen
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RcosNeonGreen,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.testTag("filter_chip_$key")
                            )
                        }
                    }
                }
            }

            // 3. Agent List Section Title & Status Quick Batch Notice
            item(key = "dashboard_list_section_title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Autonomous Agents (${filteredAgents.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    TextButton(
                        onClick = onNavigateToAgents,
                        modifier = Modifier.testTag("view_all_agents_btn")
                    ) {
                        Text(
                            text = "Fleet Registry",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF38BDF8)
                        )
                    }
                }
            }

            // 4. Empty State
            if (filteredAgents.isEmpty()) {
                item(key = "dashboard_empty_state") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().testTag("agents_empty_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "No Agents Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No autonomous agents match '$searchQuery'." else "No agents match the selected filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isNotEmpty() || selectedFilter != "ALL") {
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedFilter = "ALL"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }
                }
            } else {
                // 5. Autonomous Agent Cards with Direct Toggle Action Buttons & Activity Logs
                items(filteredAgents, key = { it.agentId }) { agent ->
                    val agentLogs = remember(allLogs, agent.agentName) {
                        allLogs.filter { log ->
                            log.agentName.contains(agent.agentName, ignoreCase = true) ||
                                    agent.agentName.contains(log.agentName, ignoreCase = true)
                        }
                    }

                    val agentResponsibility = remember(responsibilities, agent.agentName) {
                        responsibilities.find {
                            it.agentName.contains(agent.agentName, ignoreCase = true) ||
                                    agent.agentName.contains(it.agentName, ignoreCase = true)
                        }
                    }

                    val isExpanded = expandedAgentId == agent.agentId

                    AutonomousAgentCard(
                        agent = agent,
                        agentLogs = agentLogs,
                        responsibility = agentResponsibility,
                        isExpanded = isExpanded,
                        pulseAlpha = pulseAlpha,
                        onToggleExpand = {
                            expandedAgentId = if (isExpanded) null else agent.agentId
                        },
                        onDirectTask = { agentToDirect = agent },
                        onToggleStatus = {
                            viewModel.toggleAgentActivePause(agent.agentId)
                        },
                        modifier = Modifier.testTag("agent_card_${agent.agentId}")
                    )
                }
            }

            // 6. Global Recent Activity Stream Summary at the Bottom of Dashboard LazyColumn
            if (allLogs.isNotEmpty()) {
                item(key = "dashboard_global_activity_section") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth().testTag("global_activity_stream_card")
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF38BDF8))
                                    )
                                    Text(
                                        text = "LIVE SYSTEM AUDIT STREAM",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        ),
                                        color = Color(0xFF38BDF8)
                                    )
                                }

                                Text(
                                    text = "${allLogs.size} logs recorded",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            allLogs.take(4).forEach { log ->
                                ActivityLogItemRow(log = log)
                            }

                            Button(
                                onClick = onNavigateToWorkflows,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("view_all_audit_logs_btn")
                            ) {
                                Text(
                                    text = "View Workflow & Approvals Engine",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Live Task Director Dialog when executive selects an agent to direct
    agentToDirect?.let { agent ->
        val agentResp = responsibilities.find { it.agentName.contains(agent.agentName, true) }
        val agentLogs = allLogs.filter { it.agentName.contains(agent.agentName, true) }
        val opState = deriveAgentOperationalState(agent, allLogs, emptyList())

        AgentLiveTaskDirectorDialog(
            agent = agent,
            operationalState = opState,
            responsibility = agentResp,
            latestLogs = agentLogs,
            viewModel = viewModel,
            onDismiss = { agentToDirect = null }
        )
    }
}

/**
 * Individual Autonomous Agent Card displaying live status, capabilities, action buttons to toggle
 * status between Active and Paused, and recent execution activity logs.
 */
@Composable
fun AutonomousAgentCard(
    agent: AgentRegistryEntity,
    agentLogs: List<AiActionLog>,
    responsibility: AgentResponsibility?,
    isExpanded: Boolean,
    pulseAlpha: Float = 1f,
    onToggleExpand: () -> Unit,
    onDirectTask: () -> Unit,
    onToggleStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = agent.status.equals(AgentStatus.ACTIVE.name, ignoreCase = true)
    
    val statusColor by animateColorAsState(
        targetValue = if (isActive) RcosNeonGreen else Color(0xFFFFB74D),
        label = "statusColor"
    )

    val icon = when {
        agent.agentType.contains("FINANCE", true) -> Icons.Default.AccountBalance
        agent.agentType.contains("HR", true) -> Icons.Default.People
        agent.agentType.contains("COMPLIANCE", true) -> Icons.Default.Gavel
        agent.agentType.contains("CUSTOMER", true) || agent.agentType.contains("SUPPORT", true) -> Icons.Default.SupportAgent
        agent.agentType.contains("EXECUTIVE", true) || agent.agentType.contains("STRATEGY", true) -> Icons.Default.Psychology
        else -> Icons.Default.SmartToy
    }

    val riskColor = when (agent.riskClassification) {
        "LOW" -> RcosNeonGreen
        "MEDIUM" -> Color(0xFFFFB74D)
        "HIGH", "CRITICAL" -> Color(0xFFF87171)
        else -> RcosNeonGreen
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isActive) RcosNeonGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon, Agent Name, Department, Status Badge & Quick Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = agent.agentName,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = agent.agentName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${agent.assignedDepartment ?: "Autonomous Operations"} • ${agent.modelTier.replace("_", " ")}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Interactive Status Control in Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clickable Status Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable { onToggleStatus() }
                            .testTag("status_badge_${agent.agentId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) statusColor.copy(alpha = pulseAlpha) else statusColor)
                            )
                            Text(
                                text = if (isActive) "ACTIVE" else "PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            )
                        }
                    }

                    // Mini Quick Toggle Switch
                    Switch(
                        checked = isActive,
                        onCheckedChange = { onToggleStatus() },
                        modifier = Modifier
                            .scale(0.8f)
                            .testTag("agent_status_switch_${agent.agentId}"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = RcosNeonGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF334155),
                            uncheckedBorderColor = Color(0xFF64748B)
                        )
                    )
                }
            }

            // Description
            Text(
                text = agent.agentDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            // Badges Row: Autonomy Level, Risk Level, Department Runs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Autonomy Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = RcosNeonGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = responsibility?.autonomyLevel ?: "Full Autonomy",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Risk Level Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = riskColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = riskColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${agent.riskClassification} Risk",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                            color = riskColor
                        )
                    }
                }

                // Execution Counter
                responsibility?.let {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "${it.actionsExecutedToday} runs today",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Agent Capabilities
            val capabilities = agent.getCapabilitiesList()
            if (capabilities.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    capabilities.take(if (isExpanded) capabilities.size else 3).forEach { cap ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Text(
                                text = cap.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!isExpanded && capabilities.size > 3) {
                        Text(
                            text = "+${capabilities.size - 3} more",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = RcosNeonGreen,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            // Recent Activity Logs Subsection for this Agent
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = RcosNeonGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Recent Activity Logs",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = if (agentLogs.isEmpty()) "0 logs" else "${agentLogs.size} recent",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (agentLogs.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isActive) "Agent standby. Ready for incoming dispatch." else "Agent paused. Autonomous triggers suspended.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val displayLogs = if (isExpanded) agentLogs else agentLogs.take(2)
                        displayLogs.forEach { log ->
                            AgentRecentLogItem(log = log)
                        }
                    }
                }
            }

            // Action Buttons Row: [Pause / Activate Toggle Button], [Direct Task], [Details / Expand]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Dedicated Action Button to Toggle Status between Active and Paused
                if (isActive) {
                    // Button when Active -> Action to Pause
                    OutlinedButton(
                        onClick = onToggleStatus,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFFB74D).copy(alpha = 0.12f),
                            contentColor = Color(0xFFFFB74D)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_status_btn_${agent.agentId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PauseCircle,
                            contentDescription = "Pause Agent",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pause Agent",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    // Button when Paused -> Action to Activate
                    Button(
                        onClick = onToggleStatus,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RcosNeonGreen,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_status_btn_${agent.agentId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Activate Agent",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Activate Agent",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // 2. Direct Task Button
                FilledTonalButton(
                    onClick = onDirectTask,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isActive) RcosNeonGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isActive) RcosNeonGreen else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("direct_agent_btn_${agent.agentId}")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Direct Task",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // 3. Expand / Logs Button
                OutlinedButton(
                    onClick = onToggleExpand,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.testTag("expand_agent_btn_${agent.agentId}")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand details",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpanded) "Less" else "Logs",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * Clean compact log item showing execution details inside an agent card.
 */
@Composable
fun AgentRecentLogItem(log: AiActionLog) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (log.approvalStatus.contains("Approved", true) || log.approvalStatus.contains("Executed", true)) {
                                    RcosNeonGreen
                                } else {
                                    Color(0xFFFFB74D)
                                }
                            )
                    )
                    Text(
                        text = log.workflowTitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${log.executionTimeMs}ms • ${log.timestamp}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = log.actionSummary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trigger: ${log.triggerType}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color(0xFF94A3B8)
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (log.approvalStatus.contains("Auto-Approved", true)) {
                        RcosNeonGreen.copy(alpha = 0.15f)
                    } else {
                        Color(0xFF38BDF8).copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = log.approvalStatus,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (log.approvalStatus.contains("Auto-Approved", true)) RcosNeonGreen else Color(0xFF38BDF8)
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * Activity log row for the global audit stream section.
 */
@Composable
fun ActivityLogItemRow(log: AiActionLog) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(RcosNeonGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = RcosNeonGreen,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = log.agentName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = log.timestamp,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = log.actionSummary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Metric summary chip for fleet statistics.
 */
@Composable
fun FleetMetricChip(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = tint)
            )
        }
    }
}
