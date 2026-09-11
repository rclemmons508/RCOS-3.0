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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogView(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val activeWorkspaceId by viewModel.activeWorkspaceId.collectAsState()
    val allWorkspaces by viewModel.allWorkspaces.collectAsState()
    val auditLogs by viewModel.activeWorkspaceAuditLogs.collectAsState()
    val accounts by viewModel.activeWorkspaceUserAccounts.collectAsState()
    val currentActor = remember(accounts) { viewModel.getCurrentActor() }

    val currentWorkspace = allWorkspaces.find { it.workspaceId == activeWorkspaceId }
        ?: WorkspaceEntity(activeWorkspaceId, "Current Business Workspace", "Enterprise")

    // RBAC Authorization Evaluation for Audit Log Viewing
    val authResult = remember(currentActor, activeWorkspaceId) {
        PermissionEngine.evaluatePermission(
            user = currentActor,
            targetWorkspaceId = activeWorkspaceId,
            action = PermissionAction.VIEW_AUDIT_LOGS
        )
    }

    var selectedActionCategory by remember { mutableStateOf("ALL") }
    var selectedRiskFilter by remember { mutableStateOf("ALL") }
    var selectedResultFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var expandedLogId by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audit_log_view_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
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
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Audit Logs",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Enterprise Audit & Accountability Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Immutable Compliance Trail • Workspace: ${currentWorkspace.companyName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RcosNeonGreen.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(RcosNeonGreen, CircleShape)
                        )
                        Text(
                            text = "APPEND-ONLY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = RcosNeonGreen
                        )
                    }
                }
            }

            // Check RBAC Authorization
            if (!authResult.isAllowed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audit_access_denied_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Access Denied",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "ACCESS DENIED — Insufficient Permissions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = authResult.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Viewing enterprise audit trails is restricted to Admin, Manager, and Auditor roles. Current role: ${currentActor?.role?.label ?: "EMPLOYEE"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Authorized View Content
                // 1. Accountability Summary Metrics
                val totalEvents = auditLogs.size
                val deniedEvents = auditLogs.count { it.result == AuditResultStatus.DENIED.name }
                val highRiskEvents = auditLogs.count { it.riskLevel == AuditRiskLevel.HIGH.name || it.riskLevel == AuditRiskLevel.CRITICAL.name }
                val pendingApprovals = auditLogs.count { it.approvalStatus == AuditApprovalStatus.PENDING.name }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBadgeCard(
                        title = "Total Events",
                        value = totalEvents.toString(),
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "Denied",
                        value = deniedEvents.toString(),
                        icon = Icons.Default.Block,
                        color = if (deniedEvents > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "High/Crit Risk",
                        value = highRiskEvents.toString(),
                        icon = Icons.Default.Warning,
                        color = if (highRiskEvents > 0) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadgeCard(
                        title = "Pending Appr.",
                        value = pendingApprovals.toString(),
                        icon = Icons.Default.HourglassTop,
                        color = if (pendingApprovals > 0) Color(0xFF3B82F6) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 2. Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audit_search_input"),
                    placeholder = { Text("Search actor, action, description, or resource ID...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // 3. Filter Chips (Category & Risk)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Filter by Action Category:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val categories = listOf("ALL", "AUTHORIZATION", "WORKFLOW", "FINANCIAL", "USER", "AI_CONFIG")
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedActionCategory == cat,
                                onClick = { selectedActionCategory = cat },
                                label = { Text(cat) },
                                modifier = Modifier.testTag("audit_filter_cat_$cat")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter by Risk Level:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("ALL", "LOW", "MEDIUM", "HIGH", "CRITICAL").forEach { risk ->
                                FilterChip(
                                    selected = selectedRiskFilter == risk,
                                    onClick = { selectedRiskFilter = risk },
                                    label = { Text(risk, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("audit_filter_risk_$risk")
                                )
                            }
                        }
                    }
                }

                // 4. Filtered Logs List
                val filteredLogs = remember(auditLogs, selectedActionCategory, selectedRiskFilter, selectedResultFilter, searchQuery) {
                    auditLogs.filter { log ->
                        val matchesCat = when (selectedActionCategory) {
                            "ALL" -> true
                            "AUTHORIZATION" -> log.actionType.contains("AUTHORIZATION") || log.actionType.contains("LOGIN") || log.actionType.contains("LOGOUT")
                            "WORKFLOW" -> log.actionType.contains("WORKFLOW") || log.actionType.contains("AGENT")
                            "FINANCIAL" -> log.actionType.contains("FINANCIAL")
                            "USER" -> log.actionType.contains("USER")
                            "AI_CONFIG" -> log.actionType.contains("AI")
                            else -> true
                        }
                        val matchesRisk = selectedRiskFilter == "ALL" || log.riskLevel.equals(selectedRiskFilter, ignoreCase = true)
                        val matchesResult = selectedResultFilter == "ALL" || log.result.equals(selectedResultFilter, ignoreCase = true)
                        val matchesSearch = searchQuery.isBlank() ||
                                log.actorName.contains(searchQuery, ignoreCase = true) ||
                                log.actionType.contains(searchQuery, ignoreCase = true) ||
                                log.description.contains(searchQuery, ignoreCase = true) ||
                                (log.resourceId?.contains(searchQuery, ignoreCase = true) == true) ||
                                (log.agentId?.contains(searchQuery, ignoreCase = true) == true)

                        matchesCat && matchesRisk && matchesResult && matchesSearch
                    }
                }

                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "No Logs",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (auditLogs.isEmpty()) "No audit events recorded for workspace '$activeWorkspaceId' yet." else "No audit events match the selected filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredLogs.forEach { log ->
                            AuditLogItemCard(
                                log = log,
                                isExpanded = expandedLogId == log.auditId,
                                onToggleExpand = {
                                    expandedLogId = if (expandedLogId == log.auditId) null else log.auditId
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBadgeCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AuditLogItemCard(
    log: AuditLogEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss • MMM dd, yyyy", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    val riskColor = when (log.riskLevel) {
        AuditRiskLevel.CRITICAL.name -> Color(0xFFEF4444)
        AuditRiskLevel.HIGH.name -> Color(0xFFF59E0B)
        AuditRiskLevel.MEDIUM.name -> Color(0xFF3B82F6)
        else -> RcosNeonGreen
    }

    val resultColor = when (log.result) {
        AuditResultStatus.DENIED.name -> Color(0xFFEF4444)
        AuditResultStatus.FAILED.name -> Color(0xFFF97316)
        else -> RcosNeonGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggleExpand() }
            .testTag("audit_item_${log.auditId}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Row: Actor, Action Badge, Risk Chip, Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ActorBadge(actorType = log.actorType, actorName = log.actorName)

                    Text(
                        text = log.actionType,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Risk Chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = riskColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = log.riskLevel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = riskColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Result Status Chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = resultColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = log.result,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = resultColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Description
            Text(
                text = log.description,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Timestamp footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (log.approvalRequired) {
                    Text(
                        text = "Approval: ${log.approvalStatus}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (log.approvalStatus == AuditApprovalStatus.APPROVED.name) RcosNeonGreen else Color(0xFFF59E0B)
                    )
                }
            }

            // Expanded Details Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "AUDIT EVENT IMMUTABLE SPECIFICATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )

                    DetailRow(label = "Audit ID:", value = log.auditId)
                    DetailRow(label = "Workspace ID:", value = log.workspaceId)
                    if (!log.userId.isNull_or_blank()) DetailRow(label = "User ID:", value = log.userId!!)
                    if (!log.resourceType.isNull_or_blank()) DetailRow(label = "Resource:", value = "${log.resourceType} (${log.resourceId ?: "N/A"})")
                    if (!log.agentId.isNull_or_blank()) DetailRow(label = "Agent ID:", value = log.agentId!!)
                    if (!log.workflowId.isNull_or_blank()) DetailRow(label = "Workflow ID:", value = log.workflowId!!)
                    if (!log.previousValue.isNull_or_blank()) DetailRow(label = "Previous Value:", value = log.previousValue!!)
                    if (!log.newValue.isNull_or_blank()) DetailRow(label = "New Value:", value = log.newValue!!)
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()

@Composable
private fun ActorBadge(actorType: String, actorName: String) {
    val (icon, color) = when (actorType) {
        AuditActorType.USER.name -> Icons.Default.Person to Color(0xFF3B82F6)
        AuditActorType.AI_AGENT.name -> Icons.Default.SmartToy to Color(0xFF8B5CF6)
        AuditActorType.WORKFLOW.name -> Icons.Default.AccountTree to Color(0xFF10B981)
        else -> Icons.Default.Computer to Color(0xFF6B7280)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = actorType,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = actorName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
