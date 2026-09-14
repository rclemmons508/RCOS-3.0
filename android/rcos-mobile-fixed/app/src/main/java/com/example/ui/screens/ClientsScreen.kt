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
import com.example.ui.NovaViewModel
import com.example.ui.components.ClientDetailsDialog
import com.example.ui.theme.RcosNeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    viewModel: NovaViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddClientDialog by remember { mutableStateOf(false) }

    val clientsList by viewModel.clientsList.collectAsState()
    val selectedClientDetail by viewModel.selectedClientDetail.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
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
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = RcosNeonGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Clients CRM",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
                Text(
                    text = "Tap any client account to view details, jobs & call handover controls",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FloatingActionButton(
                onClick = { showAddClientDialog = true },
                containerColor = RcosNeonGreen,
                contentColor = Color.Black,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("add_client_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Client")
            }
        }

        // Summary Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Total Accounts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${clientsList.size}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = RcosNeonGreen)
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Active Jobs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${clientsList.sumOf { it.ongoingJobs.size }}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search clients by name, industry or email...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RcosNeonGreen) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clients_search_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RcosNeonGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )

        // Clients List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(clientsList.filter {
                searchQuery.isEmpty() ||
                        it.companyName.contains(searchQuery, ignoreCase = true) ||
                        it.industry.contains(searchQuery, ignoreCase = true) ||
                        it.accountEmail.contains(searchQuery, ignoreCase = true)
            }, key = { it.id }) { client ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectClientForDetails(client) }
                        .testTag("client_card_${client.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = RcosNeonGreen.copy(alpha = 0.2f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = client.companyName.take(2).uppercase(),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                color = RcosNeonGreen
                                            )
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = client.companyName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (client.status == "VIP") Color(0xFFFFD700) else RcosNeonGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = RcosNeonGreen, modifier = Modifier.size(16.dp))
                                Text(client.accountEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF7C3AED).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        viewModel.selectClientForDetails(client)
                                        viewModel.analyzeClientWithGemini(client)
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFFD54F))
                                        Text(
                                            text = "Gemini",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFD1C4E9), fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${client.ongoingJobs.size} Active Jobs",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                                        )
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp), tint = RcosNeonGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedClientDetail?.let { client ->
        ClientDetailsDialog(
            client = client,
            viewModel = viewModel,
            onDismiss = { viewModel.selectClientForDetails(null) }
        )
    }

    if (showAddClientDialog) {
        var clientName by remember { mutableStateOf("") }
        var industry by remember { mutableStateOf("Enterprise Tech") }
        var accountEmail by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var contactName by remember { mutableStateOf("") }
        var contactRole by remember { mutableStateOf("VP of Operations") }
        var contractValue by remember { mutableStateOf("$100,000 / yr") }

        AlertDialog(
            onDismissRequest = { showAddClientDialog = false },
            title = { Text("Add Enterprise Client", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Business Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("add_client_name_input")
                    )
                    OutlinedTextField(
                        value = industry,
                        onValueChange = { industry = it },
                        label = { Text("Industry Sector") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = accountEmail,
                        onValueChange = { accountEmail = it },
                        label = { Text("Account Email") },
                        placeholder = { Text("contact@company.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Corporate Phone") },
                        placeholder = { Text("+1 (555) 000-0000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Primary Contact Person") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientName.isNotBlank()) {
                            val newClient = com.example.data.ClientDetailData(
                                id = "client_" + System.currentTimeMillis(),
                                companyName = clientName.trim(),
                                industry = industry.ifBlank { "General Business" },
                                accountEmail = accountEmail.ifBlank { "info@${clientName.lowercase().replace(" ", "")}.com" },
                                phone = phone.ifBlank { "+1 (555) 100-2000" },
                                status = "Active Enterprise",
                                contractValue = contractValue,
                                primaryContact = com.example.data.ContactPerson(
                                    name = contactName.ifBlank { "Lead Executive" },
                                    role = contactRole,
                                    email = accountEmail.ifBlank { "executive@company.com" },
                                    phone = phone.ifBlank { "+1 (555) 100-2000" }
                                )
                            )
                            viewModel.addOrUpdateClient(newClient)
                            showAddClientDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                    modifier = Modifier.testTag("save_new_client_btn")
                ) {
                    Text("Save Client Account", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClientDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

