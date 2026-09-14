package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalendarEventItem
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.calendarEvents.collectAsState()
    val clientsList by viewModel.clientsList.collectAsState()
    val isCalendarGeminiLoading by viewModel.isCalendarGeminiLoading.collectAsState()
    val calendarGeminiBriefing by viewModel.calendarGeminiBriefing.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedDateFilter by remember { mutableStateOf("All") }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var selectedEventForDetails by remember { mutableStateOf<CalendarEventItem?>(null) }

    val filteredEvents = remember(events, searchQuery, selectedCategoryFilter, selectedDateFilter) {
        events.filter { ev ->
            val matchesQuery = searchQuery.isBlank() ||
                    ev.title.contains(searchQuery, ignoreCase = true) ||
                    ev.clientName.contains(searchQuery, ignoreCase = true) ||
                    ev.assignedAgent.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategoryFilter == "All" ||
                    ev.eventType.equals(selectedCategoryFilter, ignoreCase = true)

            val matchesDate = selectedDateFilter == "All" ||
                    ev.date.equals(selectedDateFilter, ignoreCase = true)

            matchesQuery && matchesCategory && matchesDate
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
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = RcosNeonGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Executive Schedule",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "Real-time AI Consultation & Dispatch Calendar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.syncCalendarWithGoogleWorkspace() },
                    modifier = Modifier.testTag("sync_calendar_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync External Calendars",
                        tint = RcosNeonGreen
                    )
                }

                FloatingActionButton(
                    onClick = { showAddEventDialog = true },
                    containerColor = RcosNeonGreen,
                    contentColor = Color.Black,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("add_calendar_event_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Schedule Event")
                }
            }
        }

        // Summary Banner with Google / Outlook Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Calendar Sync Active",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = RcosNeonGreen
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = RcosNeonGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = RcosNeonGreen
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "${events.count { it.status == "Scheduled" }} pending meetings • Connected to Google Workspace & Outlook",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.generateScheduleBriefingWithGemini() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        enabled = !isCalendarGeminiLoading,
                        modifier = Modifier.testTag("gemini_schedule_briefing_btn")
                    ) {
                        if (isCalendarGeminiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("Analyzing...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFD54F))
                            Spacer(Modifier.width(4.dp))
                            Text("AI Briefing", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.syncCalendarWithGoogleWorkspace() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = RcosNeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sync Now", fontSize = 11.sp, color = RcosNeonGreen)
                    }
                }
            }
        }

        // Gemini AI Schedule Briefing Card
        if (calendarGeminiBriefing != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4A148C).copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("gemini_schedule_briefing_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                            Text("Gemini AI Executive Schedule Synthesis", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD1C4E9))
                        }
                        IconButton(
                            onClick = { viewModel.clearCalendarGeminiBriefing() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = calendarGeminiBriefing ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Filter Row for Dates
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Today", "Tomorrow").forEach { dateLabel ->
                FilterChip(
                    selected = selectedDateFilter == dateLabel,
                    onClick = { selectedDateFilter = dateLabel },
                    label = { Text(dateLabel, fontWeight = FontWeight.Bold) },
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
            placeholder = { Text("Search meetings, clients, or assigned agents...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RcosNeonGreen) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calendar_search_input"),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RcosNeonGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            singleLine = true
        )

        // Events List
        if (filteredEvents.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(40.dp)
                    )
                    Text("No Scheduled Events Found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tap the '+' button above to schedule an AI consultation or client briefing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredEvents, key = { it.id }) { ev ->
                    val isCompleted = ev.status == "Completed"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEventForDetails = ev }
                            .testTag("calendar_event_card_${ev.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCompleted) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else RcosNeonGreen.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant else RcosNeonGreen.copy(alpha = 0.15f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = ev.time,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else RcosNeonGreen
                                        )
                                    )
                                    Text(
                                        text = ev.date,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = ev.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = RcosNeonGreen, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = ev.clientName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "Agent: ${ev.assignedAgent}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.toggleCalendarEventStatus(ev.id) },
                                modifier = Modifier.size(36.dp).testTag("toggle_event_${ev.id}")
                            ) {
                                Icon(
                                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (isCompleted) "Mark Scheduled" else "Mark Completed",
                                    tint = if (isCompleted) RcosNeonGreen else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Schedule New Event Dialog
    if (showAddEventDialog) {
        var title by remember { mutableStateOf("") }
        var clientName by remember { mutableStateOf(clientsList.firstOrNull()?.companyName ?: "Apex Enterprises") }
        var eventType by remember { mutableStateOf("AI Consultation") }
        var date by remember { mutableStateOf("Today") }
        var time by remember { mutableStateOf("10:00 AM") }
        var assignedAgent by remember { mutableStateOf("Workload Synthesizer") }
        var locationOrLink by remember { mutableStateOf("Google Meet") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("Schedule Real-World Event", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Meeting / Event Title *") },
                        placeholder = { Text("e.g. Executive Strategy Review") },
                        modifier = Modifier.fillMaxWidth().testTag("event_title_input")
                    )

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Company Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("event_client_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date") },
                            placeholder = { Text("Today / Oct 24") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Time") },
                            placeholder = { Text("2:30 PM") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = assignedAgent,
                        onValueChange = { assignedAgent = it },
                        label = { Text("Assigned AI Agent") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = locationOrLink,
                        onValueChange = { locationOrLink = it },
                        label = { Text("Location / Video Link") },
                        placeholder = { Text("Google Meet / Office Boardroom") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Agenda / Briefing Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val newEvent = CalendarEventItem(
                                id = "ev_${System.currentTimeMillis() % 100000}",
                                title = title.trim(),
                                clientName = clientName.trim().ifBlank { "Apex Enterprises" },
                                eventType = eventType,
                                date = date.trim().ifBlank { "Today" },
                                time = time.trim().ifBlank { "10:00 AM" },
                                assignedAgent = assignedAgent.trim().ifBlank { "Workload Synthesizer" },
                                status = "Scheduled",
                                notes = notes.trim(),
                                locationOrLink = locationOrLink.trim().ifBlank { "Google Meet" }
                            )
                            viewModel.addCalendarEvent(newEvent)
                            showAddEventDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_add_event_btn")
                ) {
                    Text("Schedule Event", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEventDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Event Details Modal
    selectedEventForDetails?.let { ev ->
        AlertDialog(
            onDismissRequest = { selectedEventForDetails = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = RcosNeonGreen)
                    Text(ev.title, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Client: ${ev.clientName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Date & Time: ${ev.date} at ${ev.time}", style = MaterialTheme.typography.bodySmall)
                    Text("Type: ${ev.eventType}", style = MaterialTheme.typography.bodySmall)
                    Text("Assigned Agent: ${ev.assignedAgent}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF38BDF8))
                    Text("Location/Link: ${ev.locationOrLink}", style = MaterialTheme.typography.bodySmall)
                    if (ev.notes.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (ev.notes.contains("[Gemini AI Meeting Prep]")) Color(0xFF4A148C).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (ev.notes.contains("[Gemini AI Meeting Prep]")) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (ev.notes.contains("[Gemini AI Meeting Prep]")) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(14.dp))
                                        Text("Gemini AI Meeting Prep & Briefing", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD1C4E9))
                                    }
                                }
                                Text(
                                    text = ev.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.prepareEventBriefingWithGemini(ev) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB39DDB)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("gemini_prep_event_btn"),
                        enabled = !isCalendarGeminiLoading
                    ) {
                        if (isCalendarGeminiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFFB39DDB), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Synthesizing Meeting Briefing...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFD54F))
                            Spacer(Modifier.width(6.dp))
                            Text("Prepare with Gemini AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("Status: ${ev.status}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (ev.status == "Completed") RcosNeonGreen else Color(0xFFFFB74D)))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleCalendarEventStatus(ev.id)
                        selectedEventForDetails = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black)
                ) {
                    Text(if (ev.status == "Completed") "Mark Scheduled" else "Mark Completed")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCalendarEvent(ev.id)
                        selectedEventForDetails = null
                    }
                ) {
                    Text("Delete Event", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}
