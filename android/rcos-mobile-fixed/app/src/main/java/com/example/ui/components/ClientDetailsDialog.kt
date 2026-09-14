package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClientDetailData
import com.example.data.ContactPerson
import com.example.data.IncomingCallState
import com.example.data.JobTaskItem
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailsDialog(
    client: ClientDetailData,
    viewModel: NovaViewModel,
    onDismiss: () -> Unit
) {
    val incomingCallState by viewModel.incomingCallState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Details, 1: Edit, 2: Jobs, 3: Call Handover
    var showAddTaskDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.85f)
            .testTag("client_details_dialog"),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_client_details_btn")
            ) {
                Text("Close File", color = RcosNeonGreen, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RcosNeonGreen.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = client.companyName.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = RcosNeonGreen
                                    )
                                )
                            }
                        }

                        Column {
                            Text(
                                text = client.companyName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = client.industry,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (client.status == "VIP") Color(0xFFFFD700).copy(alpha = 0.2f) else RcosNeonGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = client.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (client.status == "VIP") Color(0xFFFFD700) else RcosNeonGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = RcosNeonGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Details", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Edit Client", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Jobs (${client.ongoingJobs.size + client.queuedJobs.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Call Handover", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> CompanyAndContactTab(client = client, viewModel = viewModel, onSwitchToEdit = { selectedTab = 1 })
                    1 -> EditClientTab(client = client, viewModel = viewModel, onDismiss = onDismiss)
                    2 -> JobsAndTasksTab(
                        client = client,
                        onAddNewTask = { showAddTaskDialog = true }
                    )
                    3 -> CallHandoverTab(
                        client = client,
                        callState = incomingCallState,
                        viewModel = viewModel
                    )
                }
            }
        }
    )

    if (showAddTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var agentType by remember { mutableStateOf("Workload Synthesizer") }
        var priority by remember { mutableStateOf("High") }
        var dueDate by remember { mutableStateOf("Today, 5:00 PM") }
        var isQueuedOnly by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Queue AI Task for ${client.companyName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter task details to allocate RCOS autonomous pipeline:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Description") },
                        placeholder = { Text("e.g. Synthesize Q3 report and send via Outlook / Gmail...") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_task_title_input")
                    )
                    OutlinedTextField(
                        value = agentType,
                        onValueChange = { agentType = it },
                        label = { Text("Assigned AI Agent") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_agent_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = priority,
                            onValueChange = { priority = it },
                            label = { Text("Priority") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Due Date") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            viewModel.dispatchJob(
                                title = taskTitle.trim(),
                                clientName = client.companyName,
                                assignedAgent = agentType.trim(),
                                priority = priority.trim(),
                                dueDate = dueDate.trim(),
                                requiresApproval = isQueuedOnly,
                                summary = "Allocated task for ${client.companyName} by executive user."
                            )
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_queue_task_btn")
                ) {
                    Text("Dispatch Task", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CompanyAndContactTab(
    client: ClientDetailData,
    viewModel: NovaViewModel,
    onSwitchToEdit: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
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
                        Text(
                            text = "CORPORATE ACCOUNT DETAILS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = RcosNeonGreen
                        )
                        TextButton(
                            onClick = onSwitchToEdit,
                            modifier = Modifier.testTag("switch_to_edit_client_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = RcosNeonGreen)
                            Spacer(Modifier.width(4.dp))
                            Text("Edit Info", fontSize = 11.sp, color = RcosNeonGreen, fontWeight = FontWeight.Bold)
                        }
                    }

                    DetailRow(icon = Icons.Default.Email, label = "Account Email", value = client.accountEmail)
                    DetailRow(icon = Icons.Default.Phone, label = "Corporate Phone", value = client.phone)
                    DetailRow(icon = Icons.Default.LocationOn, label = "Headquarters", value = client.headquarters)
                    DetailRow(icon = Icons.Default.MonetizationOn, label = "Contract Value", value = client.contractValue)
                    DetailRow(icon = Icons.Default.Event, label = "Onboarding Date", value = client.onboardingDate)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PRIMARY EXECUTIVE CONTACT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = RcosNeonGreen
                    )

                    DetailRow(icon = Icons.Default.Person, label = "Name", value = client.primaryContact.name)
                    DetailRow(icon = Icons.Default.Badge, label = "Role / Title", value = client.primaryContact.role)
                    DetailRow(icon = Icons.Default.AlternateEmail, label = "Direct Email", value = client.primaryContact.email)
                    DetailRow(icon = Icons.Default.Call, label = "Direct Phone", value = client.primaryContact.phone)
                    DetailRow(icon = Icons.Default.Tune, label = "Preferred Channel", value = client.primaryContact.preferredChannel)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Executive Account Notes", style = MaterialTheme.typography.labelSmall, color = RcosNeonGreen)
                    Text(client.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        item {
            val isClientGeminiLoading by viewModel.isClientGeminiLoading.collectAsState()
            val clientGeminiDossier by viewModel.clientGeminiDossier.collectAsState()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("GEMINI AI ACCOUNT INTELLIGENCE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD1C4E9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.analyzeClientWithGemini(client) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("gemini_analyze_client_btn"),
                        enabled = !isClientGeminiLoading
                    ) {
                        if (isClientGeminiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Synthesizing...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFD54F))
                            Spacer(Modifier.width(4.dp))
                            Text("AI Dossier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.draftClientProposalWithGemini(client, "Executive Proposal & Action Plan") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB39DDB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("gemini_draft_proposal_btn"),
                        enabled = !isClientGeminiLoading
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFD54F))
                        Spacer(Modifier.width(4.dp))
                        Text("Draft Proposal", fontSize = 11.sp)
                    }
                }

                if (clientGeminiDossier != null) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A148C).copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("gemini_client_dossier_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                                    Text("Gemini AI Executive Synthesis", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD1C4E9))
                                }
                                IconButton(
                                    onClick = { viewModel.clearClientGeminiDossier() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                text = clientGeminiDossier ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("INTEGRATED SUITE QUICK ACTIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.triggerSimulatedIncomingCall(client.companyName, client.phone) },
                        colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("quick_call_client_btn")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Simulate Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.draftClientProposalWithGemini(client, "Executive Follow-up Email") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("quick_email_client_btn")
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Draft Email", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditClientTab(
    client: ClientDetailData,
    viewModel: NovaViewModel,
    onDismiss: () -> Unit
) {
    var companyName by remember { mutableStateOf(client.companyName) }
    var industry by remember { mutableStateOf(client.industry) }
    var accountEmail by remember { mutableStateOf(client.accountEmail) }
    var phone by remember { mutableStateOf(client.phone) }
    var headquarters by remember { mutableStateOf(client.headquarters) }
    var contractValue by remember { mutableStateOf(client.contractValue) }
    var status by remember { mutableStateOf(client.status) }
    var contactName by remember { mutableStateOf(client.primaryContact.name) }
    var contactRole by remember { mutableStateOf(client.primaryContact.role) }
    var contactEmail by remember { mutableStateOf(client.primaryContact.email) }
    var contactPhone by remember { mutableStateOf(client.primaryContact.phone) }
    var preferredChannel by remember { mutableStateOf(client.primaryContact.preferredChannel) }
    var notes by remember { mutableStateOf(client.notes) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "EDIT CLIENT CORPORATE PROFILE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RcosNeonGreen
            )
        }

        item {
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Business Name") },
                modifier = Modifier.fillMaxWidth().testTag("edit_client_company_name")
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = industry,
                    onValueChange = { industry = it },
                    label = { Text("Industry") },
                    modifier = Modifier.weight(1f).testTag("edit_client_industry")
                )
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Status (Active, VIP)") },
                    modifier = Modifier.weight(1f).testTag("edit_client_status")
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = accountEmail,
                    onValueChange = { accountEmail = it },
                    label = { Text("Account Email") },
                    modifier = Modifier.weight(1f).testTag("edit_client_email")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Corporate Phone") },
                    modifier = Modifier.weight(1f).testTag("edit_client_phone")
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = headquarters,
                    onValueChange = { headquarters = it },
                    label = { Text("Headquarters") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = contractValue,
                    onValueChange = { contractValue = it },
                    label = { Text("Contract Value") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "PRIMARY EXECUTIVE CONTACT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RcosNeonGreen
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Contact Name") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = contactRole,
                    onValueChange = { contactRole = it },
                    label = { Text("Title / Role") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    label = { Text("Direct Email") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Direct Phone") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Executive Account Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val updated = client.copy(
                            companyName = companyName,
                            industry = industry,
                            accountEmail = accountEmail,
                            phone = phone,
                            headquarters = headquarters,
                            contractValue = contractValue,
                            status = status,
                            primaryContact = ContactPerson(
                                name = contactName,
                                role = contactRole,
                                email = contactEmail,
                                phone = contactPhone,
                                preferredChannel = preferredChannel
                            ),
                            notes = notes
                        )
                        viewModel.addOrUpdateClient(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("save_client_changes_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    modifier = Modifier.testTag("delete_client_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Client Account?") },
            text = { Text("Are you sure you want to remove ${client.companyName} from RCOS CRM?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClient(client.id)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun JobsAndTasksTab(
    client: ClientDetailData,
    onAddNewTask: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ONGOING JOBS & TASKS (${client.ongoingJobs.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = RcosNeonGreen
                )

                Button(
                    onClick = onAddNewTask,
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("Add AI Task", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (client.ongoingJobs.isEmpty()) {
            item {
                Text("No ongoing jobs for this client.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(client.ongoingJobs) { job ->
                JobTaskCard(job = job, isQueued = false)
            }
        }

        item {
            Text(
                text = "QUEUED JOBS & TASKS (${client.queuedJobs.size})",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (client.queuedJobs.isEmpty()) {
            item {
                Text("No queued jobs currently.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(client.queuedJobs) { job ->
                JobTaskCard(job = job, isQueued = true)
            }
        }
    }
}

@Composable
private fun JobTaskCard(job: JobTaskItem, isQueued: Boolean) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(job.id, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = RcosNeonGreen)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isQueued) Color.Gray.copy(alpha = 0.2f) else RcosNeonGreen.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = job.status,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isQueued) Color.LightGray else RcosNeonGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Text(job.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Assigned: ${job.assignedAgent}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Due: ${job.dueDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (!isQueued) {
                LinearProgressIndicator(
                    progress = { job.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = RcosNeonGreen,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }

            Text(job.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CallHandoverTab(
    client: ClientDetailData,
    callState: IncomingCallState,
    viewModel: NovaViewModel
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "MANUAL VS AI AGENT CALL HANDOVER CONTROL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = RcosNeonGreen
                    )

                    Text(
                        text = "When ${client.companyName} calls your RCOS corporate hotline, you can manually take the call yourself or hand it over to the RCOS AI Voice Agent whenever you want.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.handleCallManually() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("take_call_manually_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PhoneCallback, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Take Call Manually", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.handleCallWithAI() },
                            colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("route_call_to_ai_btn")
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Route to AI Agent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.triggerSimulatedIncomingCall(client.companyName, client.phone) },
                        modifier = Modifier.fillMaxWidth().testTag("trigger_test_call_btn")
                    ) {
                        Icon(Icons.Default.RingVolume, contentDescription = null, modifier = Modifier.size(16.dp), tint = RcosNeonGreen)
                        Spacer(Modifier.width(6.dp))
                        Text("Simulate Incoming Call From ${client.companyName}", color = RcosNeonGreen)
                    }
                }
            }
        }

        if (callState.isConnected || callState.isRinging) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen),
                    modifier = Modifier.fillMaxWidth()
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = RcosNeonGreen)
                                Text("Active Call Routing Status", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            IconButton(onClick = { viewModel.endActiveCall() }) {
                                Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.Red)
                            }
                        }

                        Text("Handled By: ${callState.handledBy}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = RcosNeonGreen)

                        Text("Live Transcript Log:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        callState.liveTranscript.forEach { line ->
                            Text("• $line", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
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
            Icon(icon, contentDescription = null, tint = RcosNeonGreen, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
    }
}
