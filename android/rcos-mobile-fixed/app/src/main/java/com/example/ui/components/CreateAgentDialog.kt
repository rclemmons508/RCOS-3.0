@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AgentRegistryEntity
import com.example.data.AgentRiskLevel
import com.example.data.AgentType
import com.example.ui.NovaViewModel
import com.example.ui.theme.RcosNeonGreen

/**
 * Production-ready CreateAgentDialog composable that allows users to define
 * a new autonomous agent with a name and objective, and then adds it directly
 * to the Firebase Firestore 'agents' collection.
 */
@Composable
fun CreateAgentDialog(
    viewModel: NovaViewModel,
    onDismiss: () -> Unit,
    onAgentCreated: (AgentRegistryEntity) -> Unit = {}
) {
    CreateAgentDialog(
        onDismiss = onDismiss,
        onConfirmCreate = { name, objective, department, agentType, modelTier, riskLevel, capabilities, onComplete, onError ->
            viewModel.createAndAddAgentToFirestore(
                agentName = name,
                objective = objective,
                department = department,
                agentType = agentType,
                modelTier = modelTier,
                riskClassification = riskLevel,
                permissionLevel = "STANDARD",
                capabilities = capabilities,
                onSuccess = { createdAgent ->
                    onComplete()
                    onAgentCreated(createdAgent)
                },
                onError = { error ->
                    onError(error)
                }
            )
        }
    )
}

/**
 * Standalone overload of CreateAgentDialog allowing custom creation handlers
 * for direct integration into any screen or preview.
 */
@Composable
fun CreateAgentDialog(
    onDismiss: () -> Unit,
    onConfirmCreate: (
        name: String,
        objective: String,
        department: String,
        agentType: String,
        modelTier: String,
        riskLevel: String,
        capabilities: List<String>,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit
) {
    var agentName by remember { mutableStateOf("") }
    var agentObjective by remember { mutableStateOf("") }
    var selectedDepartment by remember { mutableStateOf("Operations") }
    var selectedModelTier by remember { mutableStateOf("GEMINI_2_5_FLASH") }
    var selectedRiskLevel by remember { mutableStateOf(AgentRiskLevel.LOW.name) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Preselected capabilities
    val availableCapabilities = listOf(
        "Autonomous Execution",
        "Task Routing",
        "Real-Time Telemetry",
        "Firestore Sync",
        "Anomaly Detection",
        "Approval Gating",
        "Client Notification"
    )
    val selectedCapabilities = remember {
        mutableStateListOf("Autonomous Execution", "Task Routing", "Real-Time Telemetry")
    }

    val isFormValid = agentName.trim().isNotBlank() && agentObjective.trim().isNotBlank()

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_create_agent")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Title, Cloud Badge & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            RcosNeonGreen.copy(alpha = 0.25f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = RcosNeonGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Define New Agent",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(RcosNeonGreen)
                                )
                                Text(
                                    text = "Firestore 'agents' Collection",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_close_create_agent")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 0.8.dp
                )

                // Error Notice if submission fails
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let { error ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // 1. Agent Name Input Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Agent Name *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = agentName,
                        onValueChange = {
                            agentName = it
                            errorMessage = null
                        },
                        placeholder = { Text("e.g., Nova Logistics Copilot, Sentinel Audit Agent") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_agent_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // 2. Agent Objective Input Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Autonomous Objective / Instructions *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = agentObjective,
                        onValueChange = {
                            agentObjective = it
                            errorMessage = null
                        },
                        placeholder = {
                            Text("Define the primary objective, operational scope, and autonomous duties for this agent...")
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_agent_objective"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // 3. Department Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Department Assignment",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val departments = listOf("Operations", "Executive", "Finance", "Compliance", "Customer Support")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        departments.forEach { dept ->
                            val isSelected = selectedDepartment == dept
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDepartment = dept },
                                label = { Text(dept, style = MaterialTheme.typography.labelMedium) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("chip_dept_$dept")
                            )
                        }
                    }
                }

                // 4. Model Tier Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Gemini Model Tier",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val modelTiers = listOf(
                        "GEMINI_2_5_FLASH" to "Gemini 2.5 Flash (Ultra Fast)",
                        "GEMINI_1_5_PRO" to "Gemini Pro (Deep Reasoning)",
                        "GEMINI_ENTERPRISE" to "Gemini Enterprise Autonomous"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        modelTiers.forEach { (tierKey, label) ->
                            val isSelected = selectedModelTier == tierKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("option_tier_$tierKey")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedModelTier = tierKey },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Capability Profile Toggles
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Capability Profile",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableCapabilities.forEach { cap ->
                            val isChecked = selectedCapabilities.contains(cap)
                            FilterChip(
                                selected = isChecked,
                                onClick = {
                                    if (isChecked) {
                                        selectedCapabilities.remove(cap)
                                    } else {
                                        selectedCapabilities.add(cap)
                                    }
                                },
                                leadingIcon = {
                                    if (isChecked) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                },
                                label = { Text(cap, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            )
                        }
                    }
                }

                // 6. Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_cancel_create_agent")
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (isFormValid && !isSubmitting) {
                                isSubmitting = true
                                errorMessage = null
                                val agentType = when (selectedDepartment) {
                                    "Executive" -> AgentType.EXECUTIVE_AGENT.name
                                    "Finance" -> AgentType.FINANCE_AGENT.name
                                    "Compliance" -> AgentType.COMPLIANCE_AGENT.name
                                    "Customer Support" -> AgentType.CUSTOMER_SUPPORT_AGENT.name
                                    else -> AgentType.OPERATIONS_AGENT.name
                                }

                                onConfirmCreate(
                                    agentName.trim(),
                                    agentObjective.trim(),
                                    selectedDepartment,
                                    agentType,
                                    selectedModelTier,
                                    selectedRiskLevel,
                                    selectedCapabilities.toList(),
                                    {
                                        isSubmitting = false
                                        onDismiss()
                                    },
                                    { err ->
                                        isSubmitting = false
                                        errorMessage = err
                                    }
                                )
                            }
                        },
                        enabled = isFormValid && !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_create_agent_submit"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save to Cloud",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
