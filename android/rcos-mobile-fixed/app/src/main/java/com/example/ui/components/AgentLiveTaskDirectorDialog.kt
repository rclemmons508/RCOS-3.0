@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AgentRegistryEntity
import com.example.data.AgentStatus
import com.example.data.AiActionLog
import com.example.ui.NovaViewModel
import com.example.ui.screens.OperationalState
import com.example.ui.theme.RcosNeonGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

data class LiveAgentTaskStep(
    val stepNumber: Int,
    val title: String,
    val detail: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

data class LiveTelemetryLog(
    val timestamp: String,
    val level: String,
    val message: String,
    val isUserDirective: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgentLiveTaskDirectorDialog(
    agent: AgentRegistryEntity,
    operationalState: OperationalState,
    responsibility: com.example.data.AgentResponsibility?,
    latestLogs: List<AiActionLog>,
    viewModel: NovaViewModel,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isPaused by remember { mutableStateOf(agent.status != AgentStatus.ACTIVE.name) }
    var autonomyMode by remember { mutableStateOf("Full Autonomy") }
    var executionPriority by remember { mutableStateOf("High Priority") }

    // Direct command text input
    var directiveInput by remember { mutableStateOf("") }
    var isSendingDirective by remember { mutableStateOf(false) }
    var agentAckMessage by remember { mutableStateOf<String?>(null) }

    // Derive active task based on agent profile
    val activeTaskTitle = remember(agent.agentType, agent.agentName) {
        when {
            agent.agentType.contains("FINANCE", true) -> "Auditing Retainer Invoices & Multi-Tier Treasury Calculation"
            agent.agentType.contains("ONBOARDING", true) || agent.agentName.contains("Onboarding", true) -> "Parsing Incoming Client Intake Documents & Cloud Repository Provisioning"
            agent.agentType.contains("VOICE", true) || agent.agentName.contains("Voice", true) -> "Triaging Inbound Customer Phone Audio & CRM Job Ticket Automation"
            agent.agentType.contains("EXECUTIVE", true) -> "Synthesizing C-Suite Market Intelligence Brief & Workload Metrics"
            agent.agentType.contains("SUPPORT", true) -> "Resolving High-Priority Customer SLA Support Tickets"
            agent.agentType.contains("COMPLIANCE", true) -> "Running Multi-Workspace Governance & Access Audit Verification"
            else -> "Executing Workspace Autonomous Workflow Pipeline"
        }
    }

    // Dynamic Live Steps
    var currentStepIndex by remember { mutableStateOf(2) } // Step 3 in progress
    val steps = remember(activeTaskTitle, currentStepIndex) {
        listOf(
            LiveAgentTaskStep(1, "Ingest & Validate Context", "Loaded workspace tokens & system constraints", isCompleted = currentStepIndex > 0, isCurrent = currentStepIndex == 0),
            LiveAgentTaskStep(2, "Fetch Records & Parse Metadata", "Accessed CRM data & external integrations", isCompleted = currentStepIndex > 1, isCurrent = currentStepIndex == 1),
            LiveAgentTaskStep(3, "Gemini AI Reasoning & Synthesis", "Generating action parameters & structured payload", isCompleted = currentStepIndex > 2, isCurrent = currentStepIndex == 2),
            LiveAgentTaskStep(4, "Policy & Guardrail Verification", "Checking financial thresholds & security access", isCompleted = currentStepIndex > 3, isCurrent = currentStepIndex == 3),
            LiveAgentTaskStep(5, "Commit Action & Sync Integrations", "Dispatching to Google Workspace & notifications", isCompleted = currentStepIndex > 4, isCurrent = currentStepIndex == 4)
        )
    }

    // Real-time animated progress simulation
    var progressPercent by remember { mutableStateOf(0.68f) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (isPaused) progressPercent else (progressPercent + 0.05f).coerceAtMost(0.95f),
        animationSpec = tween(1200, easing = LinearEasing),
        label = "task_progress"
    )

    // Live terminal logs
    val listState = rememberLazyListState()
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    var telemetryLogs by remember {
        mutableStateOf(
            listOf(
                LiveTelemetryLog(timeFormatter.format(Date(System.currentTimeMillis() - 12000)), "INFO", "Agent '${agent.agentName}' initialized runtime context (Model: ${agent.modelTier})"),
                LiveTelemetryLog(timeFormatter.format(Date(System.currentTimeMillis() - 9000)), "INFO", "Target pipeline: $activeTaskTitle"),
                LiveTelemetryLog(timeFormatter.format(Date(System.currentTimeMillis() - 6000)), "EXEC", "Step 2 completed: 18 entities parsed successfully"),
                LiveTelemetryLog(timeFormatter.format(Date(System.currentTimeMillis() - 3000)), "LIVE", "Step 3 executing: Gemini 2.5 active thinking loop (82 tokens/sec)...")
            )
        )
    }

    // Background live tick simulation for live view feel
    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(3500)
            val now = timeFormatter.format(Date())
            val randomLogMessages = listOf(
                "Telemetry pulse: Memory 42MB | Latency 138ms | Health 100%",
                "Querying workspace index cache: 0 cache misses",
                "Verifying schema validation on intermediate payload",
                "Gemini thinking: evaluating optimal execution path",
                "Heartbeat ping acknowledged by RCOS Orchestrator"
            )
            val nextMsg = randomLogMessages.random()
            telemetryLogs = telemetryLogs + LiveTelemetryLog(now, "LIVE", nextMsg)
            if (telemetryLogs.size > 25) {
                telemetryLogs = telemetryLogs.drop(1)
            }
        }
    }

    // Scroll to bottom when logs update
    LaunchedEffect(telemetryLogs.size) {
        if (telemetryLogs.isNotEmpty()) {
            listState.animateScrollToItem(telemetryLogs.size - 1)
        }
    }

    // Quick directive presets
    val presetDirectives = remember(agent.agentType) {
        when {
            agent.agentType.contains("FINANCE", true) -> listOf(
                "Prioritize invoices exceeding $2,500 for expedited audit",
                "Require manual executive approval before dispatching payment",
                "Generate itemized PDF breakdown and email to CFO",
                "Flag any duplicate line items or abnormal variance"
            )
            agent.agentType.contains("ONBOARDING", true) || agent.agentName.contains("Onboarding", true) -> listOf(
                "Accelerate KYC verification for VIP tier clients",
                "Generate Google Drive workspace folder structure immediately",
                "Send customized welcome email with credentials attached",
                "Pause onboarding pipeline if NDA document is unsigned"
            )
            agent.agentType.contains("VOICE", true) || agent.agentName.contains("Voice", true) -> listOf(
                "Extract high-urgency callbacks and assign to executive desk",
                "Draft calendar scheduling options for tomorrow morning",
                "Summarize key customer pain points into CRM note",
                "Route call recording audio transcript to Slack / Google Chat"
            )
            agent.agentType.contains("EXECUTIVE", true) -> listOf(
                "Focus intelligence synthesis on competitor pricing moves",
                "Format executive brief as bullet points with ROI impact",
                "Run deep reasoning verification on workload projections",
                "Schedule automatic morning digest delivery at 8:00 AM"
            )
            else -> listOf(
                "Prioritize high-impact tasks first",
                "Switch to executive Markdown summary format",
                "Require manual confirmation before finalizing writes",
                "Execute with Gemini 2.5 Deep Thinking mode"
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("agent_live_task_director_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B111E),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. TOP HEADER: Agent Identity, Live Status & Quick Action Buttons
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
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isPaused) Color(0xFF334155) else Color(0xFF064E3B),
                            border = BorderStroke(1.dp, if (isPaused) Color(0xFF64748B) else RcosNeonGreen),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.Pause else Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = if (isPaused) Color(0xFF94A3B8) else RcosNeonGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = agent.agentName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                LivePulseBadge(isPaused = isPaused)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = agent.assignedDepartment ?: "Operations",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Text("•", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                Text(
                                    text = agent.modelTier.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RcosNeonGreen
                                )
                                Text("•", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                Text(
                                    text = autonomyMode,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }

                    // Action Controls: Pause/Resume + Close
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isPaused = !isPaused
                                if (isPaused) {
                                    viewModel.disableAgent(agent.agentId)
                                    telemetryLogs = telemetryLogs + LiveTelemetryLog(
                                        timeFormatter.format(Date()),
                                        "WARN",
                                        "Execution suspended by Executive directive.",
                                        isUserDirective = true
                                    )
                                } else {
                                    viewModel.enableAgent(agent.agentId)
                                    telemetryLogs = telemetryLogs + LiveTelemetryLog(
                                        timeFormatter.format(Date()),
                                        "INFO",
                                        "Execution resumed by Executive directive.",
                                        isUserDirective = true
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .testTag("toggle_agent_live_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume Agent" else "Pause Agent",
                                tint = if (isPaused) Color(0xFF34D399) else Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .testTag("close_agent_live_dialog_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 2. SCROLLABLE CONTENT: Active Task Banner, Steps Tracker, Telemetry Terminal, Direct Controls
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // A. Current Live Task Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF111827),
                            border = BorderStroke(1.dp, if (isPaused) Color(0xFF334155) else Color(0xFF1E3A5F)),
                            modifier = Modifier.fillMaxWidth()
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
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = RcosNeonGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "ACTIVE IN-FLIGHT TASK",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp
                                            ),
                                            color = RcosNeonGreen
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isPaused) Color(0xFF334155) else Color(0xFF0C4A6E)
                                    ) {
                                        Text(
                                            text = if (isPaused) "PAUSED" else "STEP 3 OF 5 IN PROGRESS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            color = if (isPaused) Color(0xFF94A3B8) else Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = activeTaskTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )

                                // Progress bar with percentage
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isPaused) "Execution Frozen" else "Synthesizing structured payload...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = "${(animatedProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = RcosNeonGreen
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = RcosNeonGreen,
                                        trackColor = Color(0xFF1E293B)
                                    )
                                }

                                // Step Progression Horizontal Badges
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(steps) { step ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when {
                                                step.isCompleted -> Color(0xFF064E3B)
                                                step.isCurrent -> Color(0xFF0C4A6E)
                                                else -> Color(0xFF1E293B)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                when {
                                                    step.isCompleted -> Color(0xFF10B981)
                                                    step.isCurrent -> Color(0xFF38BDF8)
                                                    else -> Color(0xFF334155)
                                                }
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        step.isCompleted -> Icons.Default.Check
                                                        step.isCurrent -> Icons.Default.Autorenew
                                                        else -> Icons.Default.AccessTime
                                                    },
                                                    contentDescription = null,
                                                    tint = when {
                                                        step.isCompleted -> Color(0xFF10B981)
                                                        step.isCurrent -> Color(0xFF38BDF8)
                                                        else -> Color(0xFF94A3B8)
                                                    },
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "${step.stepNumber}. ${step.title}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = if (step.isCurrent) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    color = if (step.isCurrent) Color.White else Color(0xFFCBD5E1)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // B. Real-Time Telemetry & Terminal Logs Feed
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF050811),
                            border = BorderStroke(1.dp, Color(0xFF1E2A42)),
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Terminal,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "LIVE TELEMETRY STREAM",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = Color(0xFF38BDF8)
                                        )
                                    }

                                    Text(
                                        text = "${telemetryLogs.size} events logged",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                // Terminal Console View
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF030712),
                                    border = BorderStroke(1.dp, Color(0xFF111827)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                ) {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(telemetryLogs) { log ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "[${log.timestamp}]",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = Color(0xFF64748B)
                                                )
                                                Text(
                                                    text = "[${log.level}]",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = when (log.level) {
                                                        "WARN" -> Color(0xFFFBBF24)
                                                        "EXEC" -> Color(0xFF38BDF8)
                                                        "DIRECTIVE" -> RcosNeonGreen
                                                        else -> Color(0xFF10B981)
                                                    }
                                                )
                                                Text(
                                                    text = log.message,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = if (log.isUserDirective) RcosNeonGreen else Color(0xFFE2E8F0),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // C. DIRECT AGENT (Human-In-The-Loop Live Command Console)
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF111827),
                            border = BorderStroke(1.dp, Color(0xFF1F2937)),
                            modifier = Modifier.fillMaxWidth()
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
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = RcosNeonGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "DIRECT AGENT (LIVE INSTRUCTION)",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = Color.White
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF1E293B)
                                    ) {
                                        Text(
                                            text = "Executive Override",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Send real-time directives or instructions to immediately steer this agent's active execution pipeline:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )

                                // Direct Prompt Input Box
                                OutlinedTextField(
                                    value = directiveInput,
                                    onValueChange = { directiveInput = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("agent_directive_input"),
                                    placeholder = {
                                        Text(
                                            text = "e.g. \"Prioritize high-value invoices over $2,500 first\", \"Switch output to executive markdown table\"...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                val textToSend = directiveInput.trim()
                                                if (textToSend.isNotBlank()) {
                                                    isSendingDirective = true
                                                    coroutineScope.launch {
                                                        // 1. Log in UI immediately
                                                        val now = timeFormatter.format(Date())
                                                        telemetryLogs = telemetryLogs + LiveTelemetryLog(
                                                            now,
                                                            "DIRECTIVE",
                                                            "Executive Directive: \"$textToSend\"",
                                                            isUserDirective = true
                                                        )

                                                        // 2. Dispatch to ViewModel & DB
                                                        viewModel.directAgent(
                                                            agentId = agent.agentId,
                                                            directive = textToSend,
                                                            newPriority = executionPriority,
                                                            autonomyMode = autonomyMode
                                                        )

                                                        // 3. Agent live acknowledgment
                                                        delay(600)
                                                        agentAckMessage = "Agent '${agent.agentName}' acknowledged: \"Integrating instruction into runtime context. Adjusting execution pipeline now.\""
                                                        progressPercent = (progressPercent + 0.12f).coerceAtMost(0.92f)
                                                        telemetryLogs = telemetryLogs + LiveTelemetryLog(
                                                            timeFormatter.format(Date()),
                                                            "EXEC",
                                                            "Pipeline updated with priority directive. Executing adjusted parameters."
                                                        )
                                                        directiveInput = ""
                                                        isSendingDirective = false
                                                    }
                                                }
                                            },
                                            enabled = directiveInput.isNotBlank() && !isSendingDirective,
                                            modifier = Modifier.testTag("send_agent_directive_btn")
                                        ) {
                                            if (isSendingDirective) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    color = RcosNeonGreen,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = "Send Directive",
                                                    tint = if (directiveInput.isNotBlank()) RcosNeonGreen else Color(0xFF475569)
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF090E17),
                                        unfocusedContainerColor = Color(0xFF090E17),
                                        focusedBorderColor = RcosNeonGreen,
                                        unfocusedBorderColor = Color(0xFF1E293B)
                                    ),
                                    maxLines = 3
                                )

                                // Live Agent Acknowledgment Notice
                                AnimatedVisibility(
                                    visible = agentAckMessage != null,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    agentAckMessage?.let { ack ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF064E3B).copy(alpha = 0.7f),
                                            border = BorderStroke(1.dp, RcosNeonGreen),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = RcosNeonGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = ack,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                    color = Color.White,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { agentAckMessage = null },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Dismiss",
                                                        tint = Color(0xFF94A3B8),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Quick Directive Presets Chips
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Quick Directive Presets:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF94A3B8)
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        presetDirectives.forEach { preset ->
                                            Surface(
                                                onClick = {
                                                    directiveInput = preset
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF1E293B),
                                                border = BorderStroke(1.dp, Color(0xFF334155))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = RcosNeonGreen,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = preset,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                        color = Color(0xFFE2E8F0)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Autonomy & Policy Mode Tuning Chips
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Autonomy Mode & Risk Guardrail:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF94A3B8)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf("Full Autonomy", "Approval Required", "Advisory Only").forEach { mode ->
                                            FilterChip(
                                                selected = autonomyMode == mode,
                                                onClick = {
                                                    autonomyMode = mode
                                                    telemetryLogs = telemetryLogs + LiveTelemetryLog(
                                                        timeFormatter.format(Date()),
                                                        "WARN",
                                                        "Autonomy mode switched to: $mode",
                                                        isUserDirective = true
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = mode,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF0C4A6E),
                                                    selectedLabelColor = Color(0xFF38BDF8)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. BOTTOM FOOTER: Close & Dispatch Custom Task Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0))
                    ) {
                        Text("Close Monitor")
                    }

                    Button(
                        onClick = {
                            viewModel.executeAgentTask(
                                agentId = agent.agentId,
                                taskDescription = "Direct manual dispatch: $activeTaskTitle",
                                riskScorePct = 15
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dispatch_new_cycle_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RcosNeonGreen,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dispatch New Cycle",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePulseBadge(isPaused: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_badge")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isPaused) Color(0xFF334155) else Color(0xFF064E3B),
        border = BorderStroke(1.dp, if (isPaused) Color(0xFF64748B) else RcosNeonGreen)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(if (isPaused) 1f else scale)
                    .background(if (isPaused) Color(0xFF94A3B8) else RcosNeonGreen, CircleShape)
            )
            Text(
                text = if (isPaused) "PAUSED" else "LIVE TASK",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp
                ),
                color = if (isPaused) Color(0xFF94A3B8) else RcosNeonGreen
            )
        }
    }
}
