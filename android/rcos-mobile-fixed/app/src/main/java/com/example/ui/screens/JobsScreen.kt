package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.JobEntity
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("PENDING") } // PENDING, COMPLETED, ARCHIVED, ALL
    var showCreateDialog by remember { mutableStateOf(false) }

    val allJobs by viewModel.jobTasks.collectAsState()
    val pendingJobs by viewModel.pendingJobs.collectAsState()
    val completedJobs by viewModel.completedJobs.collectAsState()
    val archivedJobs by viewModel.archivedJobs.collectAsState()
    val isJobExecutingWithGemini by viewModel.isJobExecutingWithGemini.collectAsState()
    val jobGeminiArtifacts by viewModel.jobGeminiArtifacts.collectAsState()

    val displayedJobs = remember(allJobs, selectedFilter, searchQuery) {
        val baseList = when (selectedFilter) {
            "PENDING" -> pendingJobs
            "COMPLETED" -> completedJobs
            "ARCHIVED" -> archivedJobs
            else -> allJobs
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.clientName.contains(searchQuery, ignoreCase = true) ||
                        it.assignedAgent.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RcosNeonGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = RcosNeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Task & Job Center",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
                Text(
                    text = "Automated Workload Dispatch & Lifecycle Filtering",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = RcosNeonGreen,
                contentColor = Color.Black,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("add_job_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Job")
            }
        }

        // Real-Time Firebase Firestore Multi-Device Sync Card
        val firestoreSyncInfo by viewModel.firestoreSyncInfo.collectAsState()
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().testTag("firestore_sync_card")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (firestoreSyncInfo.state) {
                                    com.example.data.FirestoreSyncState.CONNECTED -> RcosNeonGreen
                                    com.example.data.FirestoreSyncState.SYNCING -> Color(0xFF38BDF8)
                                    com.example.data.FirestoreSyncState.OFFLINE_PERSISTENCE -> Color(0xFFFFB74D)
                                    else -> Color.Gray
                                }
                            )
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Firebase Firestore Realtime Sync",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = RcosNeonGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "MULTI-DEVICE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                                    color = RcosNeonGreen,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = firestoreSyncInfo.statusMessage,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.triggerFirestoreManualSync() },
                    modifier = Modifier.size(32.dp).testTag("trigger_firestore_sync_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync with Firestore",
                        tint = RcosNeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Job Stats Bar with dynamic counts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { selectedFilter = "PENDING" },
                color = if (selectedFilter == "PENDING") Color(0xFFFFB74D).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                border = if (selectedFilter == "PENDING") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Pending / Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${pendingJobs.size}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D))
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { selectedFilter = "COMPLETED" },
                color = if (selectedFilter == "COMPLETED") RcosNeonGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                border = if (selectedFilter == "COMPLETED") androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Completed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${completedJobs.size}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = RcosNeonGreen)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { selectedFilter = "ARCHIVED" },
                color = if (selectedFilter == "ARCHIVED") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                border = if (selectedFilter == "ARCHIVED") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Archived", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${archivedJobs.size}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedFilter == "PENDING",
                onClick = { selectedFilter = "PENDING" },
                label = { Text("Pending (${pendingJobs.size})", fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    if (selectedFilter == "PENDING") {
                        Icon(Icons.Default.PendingActions, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFB74D),
                    selectedLabelColor = Color.Black
                )
            )

            FilterChip(
                selected = selectedFilter == "COMPLETED",
                onClick = { selectedFilter = "COMPLETED" },
                label = { Text("Completed (${completedJobs.size})", fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    if (selectedFilter == "COMPLETED") {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RcosNeonGreen,
                    selectedLabelColor = Color.Black
                )
            )

            FilterChip(
                selected = selectedFilter == "ARCHIVED",
                onClick = { selectedFilter = "ARCHIVED" },
                label = { Text("Archived (${archivedJobs.size})") },
                leadingIcon = {
                    if (selectedFilter == "ARCHIVED") {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            )

            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All (${allJobs.size})") }
            )
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search tasks, clients, or agents...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RcosNeonGreen) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("jobs_search_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RcosNeonGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            singleLine = true
        )

        // Lifecycle info banner
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoMode, contentDescription = null, tint = RcosNeonGreen, modifier = Modifier.size(16.dp))
                Text(
                    text = when (selectedFilter) {
                        "PENDING" -> "Active Queue: Approving items automatically transitions them to Completed and removes them from Pending."
                        "COMPLETED" -> "Completed Archive: Authorized tasks executed successfully by assigned agents."
                        "ARCHIVED" -> "Archived Tasks: Historical jobs retained for compliance and analytics."
                        else -> "Showing all tasks across all lifecycle stages."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Jobs List
        if (displayedJobs.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (selectedFilter == "PENDING") Icons.Default.CheckCircleOutline else Icons.Default.Inbox,
                        contentDescription = null,
                        tint = if (selectedFilter == "PENDING") RcosNeonGreen else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = if (selectedFilter == "PENDING") "No Pending Tasks" else "No $selectedFilter Jobs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedFilter == "PENDING")
                            "All tasks have been approved and automatically moved to the Completed / Archived lists."
                        else
                            "No tasks match the active '$selectedFilter' filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedJobs, key = { it.id }) { job ->
                    JobItemCard(
                        job = job,
                        onApproveAndComplete = { viewModel.approveAndCompleteJob(job.id) },
                        onArchive = { viewModel.archiveJob(job.id) },
                        onRestore = { viewModel.restoreJobToPending(job.id) },
                        onClientClick = { viewModel.selectClientByName(job.clientName) },
                        onExecuteWithGemini = { viewModel.executeJobWithGemini(job.id) },
                        isExecutingWithGemini = isJobExecutingWithGemini[job.id] == true,
                        geminiArtifact = jobGeminiArtifacts[job.id]
                    )
                }
            }
        }
    }

    val selectedClientDetail by viewModel.selectedClientDetail.collectAsState()
    selectedClientDetail?.let { client ->
        com.example.ui.components.ClientDetailsDialog(
            client = client,
            viewModel = viewModel,
            onDismiss = { viewModel.selectClientForDetails(null) }
        )
    }

    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newClient by remember { mutableStateOf("") }
        var newAgent by remember { mutableStateOf("Workload Synthesizer") }
        var newPriority by remember { mutableStateOf("High") }
        var requiresApproval by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Dispatch New RCOS Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Task Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newClient,
                        onValueChange = { newClient = it },
                        label = { Text("Client Company") },
                        placeholder = { Text("Apex Enterprises") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newAgent,
                        onValueChange = { newAgent = it },
                        label = { Text("Assigned AI Agent") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Requires Approval First", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = requiresApproval,
                            onCheckedChange = { requiresApproval = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = RcosNeonGreen)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.dispatchJob(
                                title = newTitle,
                                clientName = if (newClient.isBlank()) "Apex Enterprises" else newClient,
                                assignedAgent = newAgent,
                                priority = newPriority,
                                dueDate = "Today",
                                requiresApproval = requiresApproval
                            )
                        }
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black)
                ) {
                    Text("Dispatch Task", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun JobItemCard(
    job: JobEntity,
    onApproveAndComplete: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onClientClick: () -> Unit,
    onExecuteWithGemini: () -> Unit,
    isExecutingWithGemini: Boolean,
    geminiArtifact: String?
) {
    val isPending = job.status == "Pending" || job.status == "In Progress" || job.status == "Queued"
    val isCompleted = job.status == "Completed"
    val isArchived = job.status == "Archived"
    var showGeminiArtifact by remember { mutableStateOf(geminiArtifact != null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("job_item_${job.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCompleted -> Color(0xFF064E3B).copy(alpha = 0.2f)
                isArchived -> Color(0xFF1E293B).copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isCompleted -> RcosNeonGreen.copy(alpha = 0.5f)
                isArchived -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                job.priority == "Urgent" || job.priority == "High" -> Color(0xFFFFB74D).copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isCompleted -> RcosNeonGreen.copy(alpha = 0.2f)
                            isArchived -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            else -> Color(0xFFFFB74D).copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = job.id,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                isCompleted -> RcosNeonGreen
                                isArchived -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> Color(0xFFFFB74D)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "Due: ${job.dueDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isCompleted -> RcosNeonGreen.copy(alpha = 0.2f)
                        isArchived -> MaterialTheme.colorScheme.surfaceVariant
                        job.status == "In Progress" -> Color(0xFF0284C7).copy(alpha = 0.2f)
                        else -> Color(0xFFFFB74D).copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = job.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = when {
                                isCompleted -> RcosNeonGreen
                                isArchived -> MaterialTheme.colorScheme.onSurfaceVariant
                                job.status == "In Progress" -> Color(0xFF38BDF8)
                                else -> Color(0xFFFFB74D)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Text(
                text = job.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (job.summary.isNotBlank()) {
                Text(
                    text = job.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onClientClick() }
                        .padding(vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = RcosNeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(job.clientName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = RcosNeonGreen)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(job.assignedAgent, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Gemini AI Execution Artifact preview if available
            val artifactContent = geminiArtifact ?: if (job.summary.contains("[Gemini AI Execution Resolution]")) {
                job.summary.substringAfter("[Gemini AI Execution Resolution]:").trim()
            } else null

            if (artifactContent != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF673AB7).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF673AB7).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(14.dp))
                            Text("Gemini AI Execution Artifact", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD1C4E9))
                        }
                        Text(
                            text = artifactContent,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Action buttons for state transitions & Gemini Resolution
            if (isPending) {
                Button(
                    onClick = onExecuteWithGemini,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED), contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_resolve_${job.id}"),
                    enabled = !isExecutingWithGemini
                ) {
                    if (isExecutingWithGemini) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Gemini AI Resolving Task...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFD54F))
                        Spacer(Modifier.width(6.dp))
                        Text("Execute & Resolve with Gemini AI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPending) {
                    Button(
                        onClick = onApproveAndComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("approve_complete_${job.id}")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Approve & Complete", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onArchive,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("archive_job_${job.id}")
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                } else if (isCompleted) {
                    OutlinedButton(
                        onClick = onArchive,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("archive_job_${job.id}")
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Move to Archive", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onRestore,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_job_${job.id}")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore to Pending", fontSize = 12.sp)
                    }
                } else if (isArchived) {
                    OutlinedButton(
                        onClick = onRestore,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_job_${job.id}")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore to Pending", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
