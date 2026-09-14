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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AgentRegistryEntity
import com.example.data.AgentRiskLevel
import com.example.data.AgentStatus
import com.example.data.AgentType
import com.example.data.FirestoreSyncState
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen
import com.example.ui.theme.RcosNeonGreenBright

/**
 * Production-ready AgentDashboard composable that retrieves and displays
 * a real-time list of active autonomous agents from Firebase Firestore.
 */
@Composable
fun AgentDashboard(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier,
    onNavigateToWorkflows: () -> Unit = {},
    onNavigateToRegistry: () -> Unit = {}
) {
    // Collect active autonomous agents from Firestore real-time flow
    val firestoreAgents by viewModel.firestoreActiveAgents.collectAsState()
    val localWorkspaceAgents by viewModel.activeWorkspaceAgents.collectAsState()
    val isFetching by viewModel.isFetchingFirestoreAgents.collectAsState()
    val firestoreSyncInfo by viewModel.firestoreSyncInfo.collectAsState()

    // Determine the active agents list: prefer live Firestore stream, fallback to workspace active agents
    val activeAgents = remember(firestoreAgents, localWorkspaceAgents) {
        if (firestoreAgents.isNotEmpty()) {
            firestoreAgents.filter { it.status.equals(AgentStatus.ACTIVE.name, ignoreCase = true) }
        } else {
            localWorkspaceAgents.filter { it.status.equals(AgentStatus.ACTIVE.name, ignoreCase = true) }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedDepartmentFilter by remember { mutableStateOf("ALL") }
    var selectedAgentForDispatch by remember { mutableStateOf<AgentRegistryEntity?>(null) }
    var selectedAgentForDirect by remember { mutableStateOf<AgentRegistryEntity?>(null) }
    var selectedAgentForInspection by remember { mutableStateOf<AgentRegistryEntity?>(null) }
    var showCreateAgentDialog by remember { mutableStateOf(false) }

    // Pulsing animation for live Firestore beacon
    val infiniteTransition = rememberInfiniteTransition(label = "firestore_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Trigger initial refresh from Firestore on launch
    LaunchedEffect(Unit) {
        viewModel.refreshActiveAgentsFromFirestore()
    }

    val filteredAgents = remember(activeAgents, searchQuery, selectedDepartmentFilter) {
        activeAgents.filter { agent ->
            val matchesSearch = searchQuery.isBlank() ||
                agent.agentName.contains(searchQuery, ignoreCase = true) ||
                agent.agentDescription.contains(searchQuery, ignoreCase = true) ||
                agent.assignedDepartment?.contains(searchQuery, ignoreCase = true) == true ||
                agent.agentType.contains(searchQuery, ignoreCase = true) ||
                agent.modelTier.contains(searchQuery, ignoreCase = true) ||
                agent.capabilityProfile.contains(searchQuery, ignoreCase = true)

            val matchesDept = when (selectedDepartmentFilter) {
                "ALL" -> true
                "OPERATIONS" -> agent.agentType.contains("OPERATIONS", ignoreCase = true) || agent.assignedDepartment?.contains("Operations", ignoreCase = true) == true
                "EXECUTIVE" -> agent.agentType.contains("EXECUTIVE", ignoreCase = true) || agent.assignedDepartment?.contains("Executive", ignoreCase = true) == true
                "FINANCE" -> agent.agentType.contains("FINANCE", ignoreCase = true) || agent.assignedDepartment?.contains("Finance", ignoreCase = true) == true
                "COMPLIANCE" -> agent.agentType.contains("COMPLIANCE", ignoreCase = true) || agent.assignedDepartment?.contains("Compliance", ignoreCase = true) == true
                "SUPPORT" -> agent.agentType.contains("SUPPORT", ignoreCase = true) || agent.assignedDepartment?.contains("Support", ignoreCase = true) == true
                "HIGH_RISK" -> agent.riskClassification.contains("HIGH", ignoreCase = true) || agent.riskClassification.contains("CRITICAL", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesDept
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateAgentDialog = true },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Define New Agent"
                    )
                },
                text = {
                    Text(
                        text = "Define Agent",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_define_new_agent")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("agent_dashboard_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Cloud Real-Time Indicator
            item {
                AgentDashboardHeader(
                    activeCount = activeAgents.size,
                    syncState = firestoreSyncInfo.state,
                    statusMessage = firestoreSyncInfo.statusMessage,
                    isFetching = isFetching,
                    pulseAlpha = pulseAlpha,
                    onRefresh = { viewModel.refreshActiveAgentsFromFirestore() },
                    onSeedCloud = { viewModel.syncAgentsToFirestore() },
                    onCreateAgent = { showCreateAgentDialog = true }
                )
            }

            // Real-Time System Metrics Ribbon
            item {
                AgentTelemetryMetricsRibbon(
                    activeAgentsCount = activeAgents.size,
                    highRiskCount = activeAgents.count { it.riskClassification.contains("HIGH", ignoreCase = true) || it.riskClassification.contains("CRITICAL", ignoreCase = true) },
                    executiveCount = activeAgents.count { it.agentType.contains("EXECUTIVE", ignoreCase = true) },
                    operationsCount = activeAgents.count { it.agentType.contains("OPERATIONS", ignoreCase = true) }
                )
            }

            // Search and Department Filter Bar
            item {
                AgentSearchAndFilterBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedFilter = selectedDepartmentFilter,
                    onSelectFilter = { selectedDepartmentFilter = it },
                    totalFiltered = filteredAgents.size
                )
            }

            // Active Agents Section Title
            item {
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
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(RcosNeonGreen.copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = "Active Autonomous Agents (${filteredAgents.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = "Source: Firestore Live Stream",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Empty State if no agents match
            if (filteredAgents.isEmpty()) {
                item {
                    AgentDashboardEmptyState(
                        isSearchActive = searchQuery.isNotBlank() || selectedDepartmentFilter != "ALL",
                        onClearFilters = {
                            searchQuery = ""
                            selectedDepartmentFilter = "ALL"
                        },
                        onSeedFirestore = {
                            viewModel.syncAgentsToFirestore()
                            viewModel.refreshActiveAgentsFromFirestore()
                        }
                    )
                }
            } else {
                // List of Active Autonomous Agent Cards
                items(filteredAgents, key = { it.agentId }) { agent ->
                    ActiveAgentFirestoreCard(
                        agent = agent,
                        pulseAlpha = pulseAlpha,
                        onDispatchTask = { selectedAgentForDispatch = agent },
                        onDirectAgent = { selectedAgentForDirect = agent },
                        onInspect = { selectedAgentForInspection = agent },
                        onToggleActive = { isActive ->
                            viewModel.toggleAgentActiveInFirestore(agent, isActive)
                        }
                    )
                }
            }

            // Bottom Navigation / Shortcuts Banner
            item {
                AgentDashboardQuickLinksFooter(
                    onNavigateToWorkflows = onNavigateToWorkflows,
                    onNavigateToRegistry = onNavigateToRegistry
                )
            }
        }
    }

    // Modal: Task Dispatcher Dialog
    selectedAgentForDispatch?.let { agent ->
        AgentTaskDispatcherModal(
            agent = agent,
            onDismiss = { selectedAgentForDispatch = null },
            onDispatch = { title, prompt, priority, approvalReq ->
                viewModel.dispatchAutonomousTaskToAgent(
                    agent = agent,
                    taskTitle = title,
                    prompt = prompt,
                    priority = priority,
                    approvalRequired = approvalReq
                )
                selectedAgentForDispatch = null
            }
        )
    }

    // Modal: Live Autonomous Director Dialog
    selectedAgentForDirect?.let { agent ->
        AgentLiveDirectorModal(
            agent = agent,
            onDismiss = { selectedAgentForDirect = null },
            onSendInstruction = { instruction ->
                viewModel.dispatchAutonomousTaskToAgent(
                    agent = agent,
                    taskTitle = "Direct Directive: ${instruction.take(30)}...",
                    prompt = instruction,
                    priority = "Urgent",
                    approvalRequired = false
                )
                selectedAgentForDirect = null
            }
        )
    }

    // Modal: Full Agent Details Inspector
    selectedAgentForInspection?.let { agent ->
        AgentInspectionModal(
            agent = agent,
            onDismiss = { selectedAgentForInspection = null },
            onDispatchTask = {
                selectedAgentForInspection = null
                selectedAgentForDispatch = agent
            }
        )
    }

    // Modal: Create and Define New Agent Dialog (Firestore 'agents' collection)
    if (showCreateAgentDialog) {
        CreateAgentDialog(
            viewModel = viewModel,
            onDismiss = { showCreateAgentDialog = false },
            onAgentCreated = {
                showCreateAgentDialog = false
            }
        )
    }
}

/**
 * Top Header Banner with Firestore sync badge, live count, and refresh actions.
 */
@Composable
private fun AgentDashboardHeader(
    activeCount: Int,
    syncState: FirestoreSyncState,
    statusMessage: String,
    isFetching: Boolean,
    pulseAlpha: Float,
    onRefresh: () -> Unit,
    onSeedCloud: () -> Unit,
    onCreateAgent: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_dashboard_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (syncState == FirestoreSyncState.CONNECTED) RcosNeonGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Autonomous Agents Dashboard",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Real-time fleet orchestration from Firebase Firestore",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_refresh_firestore_agents"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh from Firestore",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Real-Time Connection Indicator Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                border = BorderStroke(
                    1.dp,
                    if (syncState == FirestoreSyncState.CONNECTED) RcosNeonGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    when (syncState) {
                                        FirestoreSyncState.CONNECTED -> RcosNeonGreen.copy(alpha = pulseAlpha)
                                        FirestoreSyncState.SYNCING -> Color(0xFF38BDF8)
                                        FirestoreSyncState.OFFLINE_PERSISTENCE -> Color(0xFFFFB74D)
                                        else -> Color(0xFF94A3B8)
                                    }
                                )
                        )
                        Column {
                            Text(
                                text = when (syncState) {
                                    FirestoreSyncState.CONNECTED -> "Firestore Live Connection Active"
                                    FirestoreSyncState.SYNCING -> "Syncing Cloud Agents..."
                                    FirestoreSyncState.OFFLINE_PERSISTENCE -> "Offline Persistence Stream"
                                    else -> "Connecting Firestore..."
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Collection: agents",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onCreateAgent,
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_header_define_agent"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Define Agent",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Define Agent",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        FilledTonalButton(
                            onClick = onSeedCloud,
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_seed_cloud_agents"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Sync Cloud",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sync",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Metric ribbon summarizing agent fleet capacity.
 */
@Composable
private fun AgentTelemetryMetricsRibbon(
    activeAgentsCount: Int,
    highRiskCount: Int,
    executiveCount: Int,
    operationsCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricSummaryTile(
            modifier = Modifier.weight(1f),
            label = "Active Fleet",
            value = "$activeAgentsCount",
            icon = Icons.Default.SmartToy,
            tint = RcosNeonGreen
        )
        MetricSummaryTile(
            modifier = Modifier.weight(1f),
            label = "Executive",
            value = "$executiveCount",
            icon = Icons.Default.AccountBalance,
            tint = Color(0xFF38BDF8)
        )
        MetricSummaryTile(
            modifier = Modifier.weight(1f),
            label = "Operations",
            value = "$operationsCount",
            icon = Icons.Default.Engineering,
            tint = Color(0xFFA78BFA)
        )
        MetricSummaryTile(
            modifier = Modifier.weight(1f),
            label = "Guarded",
            value = "$highRiskCount",
            icon = Icons.Default.Shield,
            tint = Color(0xFFFFB74D)
        )
    }
}

@Composable
private fun MetricSummaryTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Filter and search bar for agent fleet exploration.
 */
@Composable
private fun AgentSearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    totalFiltered: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_agent_search"),
            placeholder = { Text("Search by name, department, capability, model...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        val filters = listOf(
            "ALL" to "All Active",
            "OPERATIONS" to "Operations",
            "EXECUTIVE" to "Executive",
            "FINANCE" to "Finance",
            "COMPLIANCE" to "Compliance",
            "SUPPORT" to "Support",
            "HIGH_RISK" to "Guarded / Risk"
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(filters) { (key, label) ->
                val isSelected = selectedFilter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectFilter(key) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("filter_chip_$key")
                )
            }
        }
    }
}

/**
 * Highly styled Card representing an active autonomous agent loaded from Firestore.
 */
@Composable
private fun ActiveAgentFirestoreCard(
    agent: AgentRegistryEntity,
    pulseAlpha: Float,
    onDispatchTask: () -> Unit,
    onDirectAgent: () -> Unit,
    onInspect: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    val departmentColor = when (agent.assignedDepartment?.uppercase()) {
        "EXECUTIVE" -> Color(0xFF38BDF8)
        "FINANCE" -> Color(0xFFFBBF24)
        "COMPLIANCE", "SECURITY" -> Color(0xFFFB7185)
        "CUSTOMER_SUPPORT", "SUPPORT" -> Color(0xFF34D399)
        else -> RcosNeonGreen
    }

    val riskColor = when (agent.riskClassification.uppercase()) {
        "CRITICAL", "CRITICAL_RISK" -> Color(0xFFEF4444)
        "HIGH", "HIGH_RISK" -> Color(0xFFF97316)
        "MEDIUM", "MEDIUM_RISK" -> Color(0xFFEAB308)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_card_${agent.agentId}")
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = departmentColor.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Avatar, Name, Type, and Real-Time Active Beacon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(departmentColor.copy(alpha = 0.25f), departmentColor.copy(alpha = 0.08f))
                                )
                            )
                            .border(1.dp, departmentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getAgentIcon(agent.agentType),
                            contentDescription = null,
                            tint = departmentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = agent.agentName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "${agent.assignedDepartment ?: "Operations"} • ${agent.agentType.replace("_", " ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Live Cloud Active Beacon Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RcosNeonGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(RcosNeonGreen.copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = "LIVE ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = RcosNeonGreen
                        )
                    }
                }
            }

            // Description
            Text(
                text = agent.agentDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Badges Bar: Model Tier, Risk Classification, Permission Level
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Model Tier Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = agent.modelTier,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Risk Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = riskColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, riskColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = riskColor
                        )
                        Text(
                            text = agent.riskClassification.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = riskColor
                        )
                    }
                }

                // Capabilities Count Badge
                val caps = agent.getCapabilitiesList()
                if (caps.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${caps.size} Capabilities",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Capabilities Preview
            if (agent.capabilityProfile.isNotBlank()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    agent.getCapabilitiesList().take(4).forEach { cap ->
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = cap.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                )
                            },
                            modifier = Modifier.height(26.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                    if (agent.getCapabilitiesList().size > 4) {
                        Text(
                            text = "+${agent.getCapabilitiesList().size - 4} more",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically).padding(start = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.8.dp
            )

            // Action Buttons Row: Dispatch Task, Direct Agent, Details Inspector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dispatch Autonomous Task Button
                Button(
                    onClick = onDispatchTask,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_dispatch_task_${agent.agentId}"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Dispatch",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Dispatch Task",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Live Direct Instruction Button
                OutlinedButton(
                    onClick = onDirectAgent,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_direct_agent_${agent.agentId}"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Direct",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Direct",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                // Inspector Details Icon Button
                IconButton(
                    onClick = onInspect,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("btn_inspect_agent_${agent.agentId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Inspect Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Interactive Modal for dispatching tasks directly to autonomous agents in Firestore.
 */
@Composable
private fun AgentTaskDispatcherModal(
    agent: AgentRegistryEntity,
    onDismiss: () -> Unit,
    onDispatch: (title: String, prompt: String, priority: String, approvalRequired: Boolean) -> Unit
) {
    var taskTitle by remember { mutableStateOf("") }
    var taskPrompt by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("High") }
    var requiresApproval by remember { mutableStateOf(agent.riskClassification.contains("HIGH", ignoreCase = true)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("modal_task_dispatcher")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dispatch Autonomous Task",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Assigning to ${agent.agentName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g., Audit Monthly Billing Anomaly") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_dispatch_title"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = taskPrompt,
                    onValueChange = { taskPrompt = it },
                    label = { Text("Autonomous Instructions / Execution Prompt") },
                    placeholder = { Text("Specify objective, parameters, and constraints for the autonomous agent...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_dispatch_prompt"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Normal", "High", "Urgent").forEach { prio ->
                        val isSelected = selectedPriority == prio
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPriority = prio },
                            label = { Text(prio) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Human Approval Required",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Queue for manager approval before execution",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = requiresApproval,
                        onCheckedChange = { requiresApproval = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (taskTitle.isNotBlank()) {
                                onDispatch(
                                    taskTitle.trim(),
                                    taskPrompt.ifBlank { "Execute autonomous operation for $taskTitle" },
                                    selectedPriority,
                                    requiresApproval
                                )
                            }
                        },
                        enabled = taskTitle.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_submit_dispatch"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dispatch")
                    }
                }
            }
        }
    }
}

/**
 * Quick Live Director modal to send immediate high-priority directives to agents.
 */
@Composable
private fun AgentLiveDirectorModal(
    agent: AgentRegistryEntity,
    onDismiss: () -> Unit,
    onSendInstruction: (String) -> Unit
) {
    var directiveText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Direct Agent",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Live control stream to ${agent.agentName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = directiveText,
                    onValueChange = { directiveText = it },
                    placeholder = { Text("Type instruction for immediate autonomous processing...") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Dismiss")
                    }

                    Button(
                        onClick = {
                            if (directiveText.isNotBlank()) {
                                onSendInstruction(directiveText.trim())
                            }
                        },
                        enabled = directiveText.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Direct")
                    }
                }
            }
        }
    }
}

/**
 * Inspection modal for agent capabilities, safety controls, and metadata.
 */
@Composable
private fun AgentInspectionModal(
    agent: AgentRegistryEntity,
    onDismiss: () -> Unit,
    onDispatchTask: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = agent.agentName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Agent ID: ${agent.agentId}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = agent.agentDescription,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text(
                    text = "Capabilities & Permissions",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    agent.getCapabilitiesList().forEach { cap ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(cap.replace("_", " ")) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = onDispatchTask,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Dispatch Task")
                    }
                }
            }
        }
    }
}

/**
 * Empty state when no agents match search filters or if Firestore collection is newly initialized.
 */
@Composable
private fun AgentDashboardEmptyState(
    isSearchActive: Boolean,
    onClearFilters: () -> Unit,
    onSeedFirestore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .testTag("agent_dashboard_empty_state"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (isSearchActive) "No matching agents found" else "No active autonomous agents in Firestore",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isSearchActive)
                    "Try adjusting your keyword search or selecting a different department filter."
                else
                    "Your Firestore collection 'rcos_autonomous_agents' is empty or still initializing. You can seed standard enterprise autonomous agents with one click.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            if (isSearchActive) {
                OutlinedButton(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Clear Filters")
                }
            } else {
                Button(
                    onClick = onSeedFirestore,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_seed_empty_state")
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Seed Enterprise Agents to Firestore")
                }
            }
        }
    }
}

/**
 * Footer shortcuts to Workflow Engine and Agent Registry.
 */
@Composable
private fun AgentDashboardQuickLinksFooter(
    onNavigateToWorkflows: () -> Unit,
    onNavigateToRegistry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedCard(
            onClick = onNavigateToWorkflows,
            modifier = Modifier
                .weight(1f)
                .testTag("card_nav_workflows"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Workflow Engine",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Automations & triggers",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        OutlinedCard(
            onClick = onNavigateToRegistry,
            modifier = Modifier
                .weight(1f)
                .testTag("card_nav_registry"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Agent Registry",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Permissions & roles",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Maps AgentType to visual ImageVector icon.
 */
private fun getAgentIcon(agentType: String): ImageVector {
    return when {
        agentType.contains("EXECUTIVE", ignoreCase = true) -> Icons.Default.AccountBalance
        agentType.contains("FINANCE", ignoreCase = true) -> Icons.Default.MonetizationOn
        agentType.contains("HR", ignoreCase = true) -> Icons.Default.Group
        agentType.contains("SUPPORT", ignoreCase = true) -> Icons.Default.SupportAgent
        agentType.contains("COMPLIANCE", ignoreCase = true) -> Icons.Default.Gavel
        agentType.contains("ANALYTICS", ignoreCase = true) -> Icons.Default.Analytics
        else -> Icons.Default.SmartToy
    }
}
