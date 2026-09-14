package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppIntegration
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsScreen(
    viewModel: NovaViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val integrations by viewModel.integrations.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isFirebaseConnecting by viewModel.isFirebaseConnecting.collectAsState()
    val firebaseStatusMessage by viewModel.firebaseStatusMessage.collectAsState()
    val cloudSyncStatuses by viewModel.cloudSyncStatuses.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showLinkGoogleDialog by remember { mutableStateOf(false) }
    var showLinkMicrosoftDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "Connected Only", "Google Workspace", "Microsoft 365", "Communication & Productivity", "Developer Tools")

    val filteredList = remember(integrations, selectedCategory, searchQuery) {
        integrations.filter { app ->
            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Connected Only" -> app.isConnected
                else -> app.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                    app.name.contains(searchQuery, ignoreCase = true) ||
                    app.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar & Back Header
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
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("integrations_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column {
                        Text(
                            text = "App Integrations & AI Access",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Grant AI Agents permission to perform email, calendar & drive tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_custom_app_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add App", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Primary Enterprise Suites (Google & Microsoft)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "FEATURED ENTERPRISE WORKSPACE SUITES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = RcosNeonGreen
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Google Workspace Suite
                    Card(
                        modifier = Modifier.weight(1f).testTag("card_google_workspace_suite"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEA4335).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFFEA4335), modifier = Modifier.size(24.dp))
                                Text("Google Workspace", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Text(
                                text = "Gmail • Calendar • Drive • Docs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (userProfile.googleWorkspaceEmail.isNotBlank()) "Account: ${userProfile.googleWorkspaceEmail}" else "Not connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (userProfile.googleWorkspaceEmail.isNotBlank()) RcosNeonGreen else Color.Gray
                            )
                            Button(
                                onClick = { showLinkGoogleDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth().testTag("link_google_workspace_btn")
                            ) {
                                Text(if (userProfile.googleWorkspaceEmail.isNotBlank()) "Manage Google" else "Link Google Workspace", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Microsoft 365 Suite
                    Card(
                        modifier = Modifier.weight(1f).testTag("card_microsoft_suite"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0078D4).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF0078D4), modifier = Modifier.size(24.dp))
                                Text("Microsoft 365", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Text(
                                text = "Outlook • OneDrive • Office",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (userProfile.microsoftAccountEmail.isNotBlank()) "Account: ${userProfile.microsoftAccountEmail}" else "Not connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (userProfile.microsoftAccountEmail.isNotBlank()) RcosNeonGreen else Color.Gray
                            )
                            Button(
                                onClick = { showLinkMicrosoftDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth().testTag("link_microsoft_365_btn")
                            ) {
                                Text(if (userProfile.microsoftAccountEmail.isNotBlank()) "Manage Microsoft" else "Link Microsoft 365", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Firebase Cloud Backend & Firestore Sync
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_firebase_cloud_backend"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCA28).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFFFFCA28), modifier = Modifier.size(24.dp))
                                Column {
                                    Text("Firebase Cloud & Firestore Sync", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("Auth: rcsoulutions@gmail.com • Firestore: rcos-enterprise", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = RcosNeonGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "CONNECTED",
                                    style = MaterialTheme.typography.labelSmall.copy(color = RcosNeonGreen, fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (firebaseStatusMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCA28).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = firebaseStatusMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        if (cloudSyncStatuses.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "LIVE CLOUD COLLECTIONS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                cloudSyncStatuses.forEach { info ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(RcosNeonGreen, CircleShape)
                                            )
                                            Text(
                                                text = info.collectionName,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = info.statusMessage,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.testFirebaseConnection() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCA28), contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("test_firebase_conn_btn"),
                                enabled = !isFirebaseConnecting
                            ) {
                                if (isFirebaseConnecting) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Verifying...", fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Test Connection", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.forceCloudSync() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RcosNeonGreen),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("force_firebase_sync_btn"),
                                enabled = !isFirebaseConnecting
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp), tint = RcosNeonGreen)
                                Spacer(Modifier.width(4.dp))
                                Text("Force Cloud Sync", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Gmail, Calendar, Drive, Slack...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RcosNeonGreen) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null) } }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("integrations_search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RcosNeonGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // Filter Category Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RcosNeonGreen,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // Integration Cards List
        items(filteredList, key = { it.id }) { app ->
            IntegrationCard(
                app = app,
                onToggleConnection = { isConnected ->
                    viewModel.toggleIntegrationConnection(app.id, isConnected)
                },
                onTogglePermission = { permId, isEnabled ->
                    viewModel.toggleIntegrationPermission(app.id, permId, isEnabled)
                }
            )
        }
    }

    if (showAddDialog) {
        AddCustomAppDialog(
            onDismiss = { showAddDialog = false },
            onAddApp = { name, cat, desc, account ->
                viewModel.addCustomIntegration(name, cat, desc, account)
                showAddDialog = false
            }
        )
    }

    if (showLinkGoogleDialog) {
        LinkGoogleWorkspaceDialog(
            currentEmail = userProfile.googleWorkspaceEmail,
            onDismiss = { showLinkGoogleDialog = false },
            onLinkAccount = { accountEmail ->
                viewModel.linkGoogleWorkspaceAccount(accountEmail)
                showLinkGoogleDialog = false
            }
        )
    }

    if (showLinkMicrosoftDialog) {
        LinkMicrosoft365Dialog(
            currentEmail = userProfile.microsoftAccountEmail,
            onDismiss = { showLinkMicrosoftDialog = false },
            onLinkAccount = { accountEmail ->
                viewModel.linkMicrosoftAccount(accountEmail)
                showLinkMicrosoftDialog = false
            }
        )
    }
}

@Composable
private fun IntegrationCard(
    app: AppIntegration,
    onToggleConnection: (Boolean) -> Unit,
    onTogglePermission: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(app.isConnected) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("integration_card_${app.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (app.isConnected) androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.5f)) else null
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = getAppIconColor(app.iconName).copy(alpha = 0.2f),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getAppIconVector(app.iconName),
                            contentDescription = null,
                            tint = getAppIconColor(app.iconName),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = app.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = app.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )

                    if (app.isConnected) {
                        Text(
                            text = "Account: ${app.connectedAccount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = RcosNeonGreen,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Connect Toggle Switch
                Switch(
                    checked = app.isConnected,
                    onCheckedChange = { onToggleConnection(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = RcosNeonGreen
                    ),
                    modifier = Modifier.testTag("switch_connect_${app.id}")
                )
            }

            // Expandable AI Agent Governance & Permissions
            if (app.isConnected && app.permissions.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .testTag("expand_permissions_${app.id}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = RcosNeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "AI Agent Permissions & Capabilities (${app.permissions.count { it.isEnabled }}/${app.permissions.size})",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = RcosNeonGreen
                        )
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle permissions list",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        app.permissions.forEach { perm ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = perm.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = perm.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Checkbox(
                                        checked = perm.isEnabled,
                                        onCheckedChange = { isChecked ->
                                            onTogglePermission(perm.id, isChecked)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = RcosNeonGreen, checkmarkColor = Color.Black),
                                        modifier = Modifier.testTag("checkbox_perm_${app.id}_${perm.id}")
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

@Composable
private fun LinkGoogleWorkspaceDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onLinkAccount: (String) -> Unit
) {
    var email by remember { mutableStateOf(currentEmail.ifBlank { "executive@company.com" }) }
    var allowGmail by remember { mutableStateOf(true) }
    var allowCalendar by remember { mutableStateOf(true) }
    var allowDrive by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFFEA4335))
                Text("Link Google Workspace", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Authorize RCOS AI Agent to manage Gmail messages, Google Calendar schedules, and Drive documents.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Google Workspace Email Account") },
                    placeholder = { Text("user@company.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_google_link_email")
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                Text("PERMITTED AI TOOL SCOPES", style = MaterialTheme.typography.labelSmall, color = RcosNeonGreen)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Gmail (Draft & Auto-Respond)", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = allowGmail, onCheckedChange = { allowGmail = it }, colors = SwitchDefaults.colors(checkedThumbColor = RcosNeonGreen))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Google Calendar (Sync Meetings)", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = allowCalendar, onCheckedChange = { allowCalendar = it }, colors = SwitchDefaults.colors(checkedThumbColor = RcosNeonGreen))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Google Drive (Read Contracts & Briefs)", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = allowDrive, onCheckedChange = { allowDrive = it }, colors = SwitchDefaults.colors(checkedThumbColor = RcosNeonGreen))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLinkAccount(email) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335), contentColor = Color.White),
                modifier = Modifier.testTag("confirm_google_link_btn")
            ) {
                Text("Link Google Workspace", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LinkMicrosoft365Dialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onLinkAccount: (String) -> Unit
) {
    var email by remember { mutableStateOf(currentEmail.ifBlank { "executive@company.onmicrosoft.com" }) }
    var allowOutlook by remember { mutableStateOf(true) }
    var allowOneDrive by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF0078D4))
                Text("Link Microsoft 365 Account", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Authorize RCOS AI Agent to access Microsoft Outlook emails, calendar invites, and OneDrive cloud files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Microsoft 365 Account Email") },
                    placeholder = { Text("user@org.onmicrosoft.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_ms_link_email")
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                Text("PERMITTED MICROSOFT SCOPES", style = MaterialTheme.typography.labelSmall, color = RcosNeonGreen)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Outlook Email & Calendar", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = allowOutlook, onCheckedChange = { allowOutlook = it }, colors = SwitchDefaults.colors(checkedThumbColor = RcosNeonGreen))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("OneDrive & SharePoint Documents", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = allowOneDrive, onCheckedChange = { allowOneDrive = it }, colors = SwitchDefaults.colors(checkedThumbColor = RcosNeonGreen))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLinkAccount(email) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4), contentColor = Color.White),
                modifier = Modifier.testTag("confirm_ms_link_btn")
            ) {
                Text("Link Microsoft 365", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddCustomAppDialog(
    onDismiss: () -> Unit,
    onAddApp: (name: String, category: String, description: String, account: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Enterprise Software") }
    var description by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("user@rcos.ai") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Extension, contentDescription = null, tint = RcosNeonGreen)
                Text("Integrate External Application", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add a custom web application, CRM, or service webhook for the RCOS AI Agent to access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Application Name *") },
                    placeholder = { Text("e.g. HubSpot CRM, Zendesk, QuickBooks") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_app_name")
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("e.g. CRM, Analytics, Finance") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_app_category")
                )

                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("Connected User Account / API Webhook") },
                    placeholder = { Text("e.g. executive@rcos.ai") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_app_account")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & AI Task Purpose") },
                    placeholder = { Text("How the AI Agent should interact with this app...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_app_desc")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAddApp(name, category, description, account)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                modifier = Modifier.testTag("submit_add_app_btn")
            ) {
                Text("Integrate & Grant AI Access", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getAppIconVector(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName.lowercase()) {
        "gmail" -> Icons.Default.Email
        "calendar" -> Icons.Default.CalendarMonth
        "drive" -> Icons.Default.FolderShared
        "slack" -> Icons.Default.Forum
        "notion" -> Icons.Default.Description
        "github" -> Icons.Default.Code
        else -> Icons.Default.Apps
    }
}

private fun getAppIconColor(iconName: String): Color {
    return when (iconName.lowercase()) {
        "gmail" -> Color(0xFFEA4335)
        "calendar" -> Color(0xFF4285F4)
        "drive" -> Color(0xFF34A853)
        "slack" -> Color(0xFFE01E5A)
        "notion" -> Color(0xFF000000)
        "github" -> Color(0xFF6E40C9)
        else -> RcosNeonGreen
    }
}
