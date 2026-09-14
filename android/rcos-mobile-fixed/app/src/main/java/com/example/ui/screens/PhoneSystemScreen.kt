package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IncomingCallState
import com.example.data.PhoneCallItem
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSystemScreen(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showLogCallDialog by remember { mutableStateOf(false) }
    var selectedCallForDetails by remember { mutableStateOf<PhoneCallItem?>(null) }

    val callLogs by viewModel.phoneCallLogs.collectAsState()
    val incomingCallState by viewModel.incomingCallState.collectAsState()
    val clientsList by viewModel.clientsList.collectAsState()
    val isCallGeminiLoading by viewModel.isCallGeminiLoading.collectAsState()
    val callGeminiAnalysis by viewModel.callGeminiAnalysis.collectAsState()

    val filteredCalls = remember(callLogs, searchQuery, selectedFilter) {
        callLogs.filter { call ->
            val matchesQuery = searchQuery.isBlank() ||
                    call.callerName.contains(searchQuery, ignoreCase = true) ||
                    call.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                    call.summary.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "AI Handled" -> call.callType.contains("AI", ignoreCase = true)
                "Incoming" -> call.callType.equals("Incoming", ignoreCase = true)
                "Outgoing" -> call.callType.equals("Outgoing", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
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
                        .clip(CircleShape)
                        .background(RcosNeonGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PhoneCallback,
                        contentDescription = null,
                        tint = RcosNeonGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Phone & Hotline System",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "AI Voice Routing, Handover & Call Intelligence",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FloatingActionButton(
                onClick = { showLogCallDialog = true },
                containerColor = RcosNeonGreen,
                contentColor = Color.Black,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("log_call_fab")
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Log Call")
            }
        }

        // Live Incoming Call Banner & Handover Controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("phone_handover_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (incomingCallState.isRinging || incomingCallState.isConnected)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (incomingCallState.isRinging || incomingCallState.isConnected) RcosNeonGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RcosNeonGreen.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = if (incomingCallState.isRinging) Icons.Default.RingVolume else Icons.AutoMirrored.Filled.PhoneCallback,
                                contentDescription = null,
                                tint = RcosNeonGreen,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (incomingCallState.isRinging)
                                    "Ringing: ${incomingCallState.callerName}"
                                else if (incomingCallState.isConnected)
                                    "Connected Call (${incomingCallState.handledBy})"
                                else
                                    "Live Hotline & AI Handover",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (incomingCallState.isRinging || incomingCallState.isConnected)
                                    incomingCallState.callerPhone
                                else
                                    "Route real-time calls to human reps or autonomous AI agents.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (incomingCallState.isConnected) {
                        Button(
                            onClick = { viewModel.endActiveCall() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("end_call_btn")
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "End Call", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("End Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.handleCallManually() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_take_call_btn")
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Take Call Manually", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.handleCallWithAI() },
                        colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_take_call_btn")
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hand Over to AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!incomingCallState.isRinging && !incomingCallState.isConnected) {
                    OutlinedButton(
                        onClick = {
                            val targetClient = clientsList.firstOrNull()?.companyName ?: "Apex Enterprises"
                            val targetPhone = clientsList.firstOrNull()?.phone ?: "+1 (555) 234-8901"
                            viewModel.triggerSimulatedIncomingCall(targetClient, targetPhone)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulate_call_btn")
                    ) {
                        Icon(Icons.Default.RingVolume, contentDescription = null, tint = RcosNeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Test Hotline: Ring Inbound Client Call", color = RcosNeonGreen, fontSize = 12.sp)
                    }
                } else if (incomingCallState.liveTranscript.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Live Transcript Feed:", style = MaterialTheme.typography.labelSmall, color = RcosNeonGreen)
                        incomingCallState.liveTranscript.forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "AI Handled", "Incoming", "Outgoing").forEach { filterLabel ->
                FilterChip(
                    selected = selectedFilter == filterLabel,
                    onClick = { selectedFilter = filterLabel },
                    label = { Text(filterLabel, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RcosNeonGreen,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search call history, phone numbers, or summaries...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RcosNeonGreen) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("phone_search_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RcosNeonGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            singleLine = true
        )

        // Call Log List
        if (filteredCalls.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PhoneMissed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(40.dp)
                    )
                    Text("No Call Logs Found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Incoming and logged calls will appear here in real-time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCalls, key = { it.id }) { call ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("call_item_${call.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    Icon(
                                        imageVector = if (call.callType.contains("AI")) Icons.Default.SmartToy else Icons.Default.Call,
                                        contentDescription = null,
                                        tint = if (call.callType.contains("AI")) RcosNeonGreen else Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = call.callerName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${call.phoneNumber} • ${call.duration}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (call.callType.contains("AI")) RcosNeonGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = call.callType,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (call.callType.contains("AI")) RcosNeonGreen else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deletePhoneCall(call.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Call", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            if (call.summary.isNotBlank()) {
                                Text(
                                    text = call.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val isLoadingThisCall = isCallGeminiLoading[call.id] == true
                            val analysis = callGeminiAnalysis[call.id]

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = call.timeAgo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )

                                OutlinedButton(
                                    onClick = { viewModel.analyzeCallWithGemini(call) },
                                    enabled = !isLoadingThisCall,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB39DDB)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("gemini_analyze_call_${call.id}")
                                ) {
                                    if (isLoadingThisCall) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color(0xFFB39DDB), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Analyzing...", fontSize = 11.sp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFFD54F))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Gemini Analysis", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (analysis != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF4A148C).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(14.dp))
                                            Text("Gemini AI Transcript & Sentiment Insights", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD1C4E9))
                                        }
                                        Text(
                                            text = analysis,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurface
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

    // Log New Call Dialog
    if (showLogCallDialog) {
        var callerName by remember { mutableStateOf("") }
        var phoneNumber by remember { mutableStateOf("") }
        var callType by remember { mutableStateOf("Incoming") }
        var duration by remember { mutableStateOf("2m 15s") }
        var summary by remember { mutableStateOf("") }
        var autoCreateJob by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showLogCallDialog = false },
            title = { Text("Log Phone Call Activity", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = callerName,
                        onValueChange = { callerName = it },
                        label = { Text("Caller / Business Name *") },
                        placeholder = { Text("e.g. Apex Enterprises") },
                        modifier = Modifier.fillMaxWidth().testTag("log_call_name_input")
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1 (555) 000-0000") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = callType,
                            onValueChange = { callType = it },
                            label = { Text("Call Type") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
                            label = { Text("Duration") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        label = { Text("Call Summary & Notes") },
                        placeholder = { Text("Inquired about project timeline and approved quote.") },
                        modifier = Modifier.fillMaxWidth().testTag("log_call_summary_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Create Follow-up CRM Job", style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = autoCreateJob,
                            onCheckedChange = { autoCreateJob = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = RcosNeonGreen)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (callerName.isNotBlank()) {
                            viewModel.logPhoneCall(
                                callerName = callerName.trim(),
                                phoneNumber = phoneNumber.trim(),
                                callType = callType.trim().ifBlank { "Incoming" },
                                duration = duration.trim().ifBlank { "1m 30s" },
                                summary = summary.trim().ifBlank { "Client call recorded by user." },
                                autoCreateJob = autoCreateJob
                            )
                            showLogCallDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_log_call_btn")
                ) {
                    Text("Save Call Log", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogCallDialog = false }) { Text("Cancel") }
            }
        )
    }
}
