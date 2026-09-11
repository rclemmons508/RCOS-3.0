package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AgentRegistryEntity
import com.example.data.CompanyProfileEntity
import com.example.data.DashboardItem
import com.example.ui.NovaViewModel
import com.example.ui.components.AgentLiveTaskDirectorDialog
import com.example.ui.components.AutonomousAgentsDashboard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardOverviewScreen(
    viewModel: NovaViewModel,
    onNavigateTab: (String) -> Unit,
    onOpenOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardItems by viewModel.dashboardItems.collectAsState()
    val chatSessions by viewModel.chatSessions.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()
    val agents by viewModel.activeWorkspaceAgents.collectAsState()
    val aiLogs by viewModel.aiActionLogs.collectAsState()
    val responsibilities by viewModel.agentResponsibilities.collectAsState()

    val analyzerInput by viewModel.analyzerInput.collectAsState()
    val analyzerPreset by viewModel.analyzerSelectedPreset.collectAsState()
    val analyzerResult by viewModel.analyzerResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val isFirebaseConnecting by viewModel.isFirebaseConnecting.collectAsState()
    val firebaseStatusMessage by viewModel.firebaseStatusMessage.collectAsState()

    var agentToLiveDirect by remember { mutableStateOf<AgentRegistryEntity?>(null) }

    val clipboardManager = LocalClipboardManager.current

    var selectedDashboardTab by remember { mutableIntStateOf(0) }

    val presets = listOf(
        "Summarize Report",
        "Key Action Items",
        "Executive Brief",
        "Format as Markdown",
        "Code Review"
    )

    // Calculate metrics
    val targetPct = companyProfile?.targetReductionPercent ?: 45
    val companyName = companyProfile?.companyName ?: "Acme Enterprise"
    val industry = companyProfile?.industry ?: "Cross-Industry Workload"
    val activeAgentsList = companyProfile?.activeAgents ?: "Workload Synthesizer, Onboarding Specialist, Workflow Automator, Strategic Analyst"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth >= 840.dp
        val isMedium = maxWidth >= 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Header & Tab Navigation Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RCOS",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = com.example.ui.theme.RcosNeonGreen,
                        letterSpacing = 2.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "RCOS MASTER APP",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                )
                Text(
                    text = "Business Operating System",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dashboard Mode Selector
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_mode_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDashboardTab == 0) com.example.ui.theme.RcosNeonGreen else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDashboardTab = 0 }
                                .testTag("tab_executive_overview")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = null,
                                    tint = if (selectedDashboardTab == 0) Color.Black else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Executive Overview",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedDashboardTab == 0) Color.Black else Color(0xFF94A3B8)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedDashboardTab == 1) com.example.ui.theme.RcosNeonGreen else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDashboardTab = 1 }
                                .testTag("tab_autonomous_agents")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = if (selectedDashboardTab == 1) Color.Black else com.example.ui.theme.RcosNeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Autonomous Agents",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedDashboardTab == 1) Color.Black else Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedDashboardTab == 1) Color.Black.copy(alpha = 0.2f) else com.example.ui.theme.RcosNeonGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${agents.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedDashboardTab == 1) Color.Black else com.example.ui.theme.RcosNeonGreen
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedDashboardTab == 1) {
                AutonomousAgentsDashboard(
                    viewModel = viewModel,
                    onNavigateToWorkflows = { onNavigateTab("workflows") },
                    onNavigateToAgents = { onNavigateTab("active_agents") },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Firebase Cloud & Gemini System Status Banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().testTag("dashboard_firebase_gemini_banner")
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                .background(com.example.ui.theme.RcosNeonGreen, CircleShape)
                                        )
                                        Text(
                                            text = "FIREBASE CLOUD & GEMINI MULTI-AGENT ENGINE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                                            color = com.example.ui.theme.RcosNeonGreen
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { viewModel.testFirebaseConnection() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCA28), contentColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            enabled = !isFirebaseConnecting,
                                            modifier = Modifier.testTag("dashboard_test_firebase_btn")
                                        ) {
                                            if (isFirebaseConnecting) {
                                                CircularProgressIndicator(modifier = Modifier.size(10.dp), color = Color.Black, strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text("Test Firebase", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.forceCloudSync() },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.ui.theme.RcosNeonGreen),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.RcosNeonGreen.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            enabled = !isFirebaseConnecting,
                                            modifier = Modifier.testTag("dashboard_sync_firebase_btn")
                                        ) {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp), tint = com.example.ui.theme.RcosNeonGreen)
                                            Spacer(Modifier.width(3.dp))
                                            Text("Sync", fontSize = 10.sp)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cloud Sync: Auth (rcsoulutions@gmail.com) • Firestore: rcos-enterprise • Gemini: 2.5 Flash / Flash Lite",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                if (firebaseStatusMessage != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = firebaseStatusMessage ?: "",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Agent Fleet Live Pulse & Direct Strip
            if (agents.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0C1322),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2A42)),
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
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(com.example.ui.theme.RcosNeonGreen, CircleShape)
                                    )
                                    Text(
                                        text = "LIVE AGENT FLEET PULSE",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        ),
                                        color = com.example.ui.theme.RcosNeonGreen
                                    )
                                }

                                TextButton(onClick = { onNavigateTab("active_agents") }) {
                                    Text(
                                        text = "View All (${agents.size})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            Text(
                                text = "Click on any agent to inspect live in-flight task execution and issue real-time steering directives:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(agents, key = { it.agentId }) { agent ->
                                    val taskSummary = when {
                                        agent.agentType.contains("FINANCE", true) -> "Auditing Retainer Invoices"
                                        agent.agentType.contains("ONBOARDING", true) || agent.agentName.contains("Onboarding", true) -> "Parsing Client Intake & KYC"
                                        agent.agentType.contains("VOICE", true) || agent.agentName.contains("Voice", true) -> "Triaging Inbound Phone Calls"
                                        agent.agentType.contains("EXECUTIVE", true) -> "Synthesizing Market Brief"
                                        else -> "Executing Workflow Pipeline"
                                    }

                                    Surface(
                                        onClick = { agentToLiveDirect = agent },
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFF131C2E),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2F4D)),
                                        modifier = Modifier
                                            .width(230.dp)
                                            .testTag("dashboard_agent_pill_${agent.agentId}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF064E3B),
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.SmartToy,
                                                            contentDescription = null,
                                                            tint = com.example.ui.theme.RcosNeonGreen,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = agent.agentName,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = agent.assignedDepartment ?: "Autonomous Ops",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = Color(0xFF94A3B8),
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF0B1322),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Bolt,
                                                        contentDescription = null,
                                                        tint = com.example.ui.theme.RcosNeonGreen,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = taskSummary,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 11.sp,
                                                            color = Color(0xFFE2E8F0)
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "⚡ Live • Tap to Direct",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = com.example.ui.theme.RcosNeonGreen
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = Color(0xFF64748B),
                                                    modifier = Modifier.size(14.dp)
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

            // 3. Core System Module Navigation Cards (Matching Screenshot 2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModuleNavigationCard(
                        title = "Active Autonomous Agents",
                        icon = Icons.Default.SmartToy,
                        onClick = { onNavigateTab("active_agents") },
                        modifier = Modifier.testTag("dashboard_nav_active_agents")
                    )
                    ModuleNavigationCard(
                        title = "AI Workflow Engine",
                        icon = Icons.Default.AutoMode,
                        onClick = { onNavigateTab("workflow") },
                        modifier = Modifier.testTag("dashboard_nav_workflow")
                    )
                    ModuleNavigationCard(
                        title = "Clients",
                        icon = Icons.Default.People,
                        onClick = { onNavigateTab("clients") },
                        modifier = Modifier.testTag("dashboard_nav_clients")
                    )
                    ModuleNavigationCard(
                        title = "Jobs",
                        icon = Icons.Default.Work,
                        onClick = { onNavigateTab("jobs") },
                        modifier = Modifier.testTag("dashboard_nav_jobs")
                    )
                    ModuleNavigationCard(
                        title = "Calendar",
                        icon = Icons.Default.CalendarToday,
                        onClick = { onNavigateTab("calendar") },
                        modifier = Modifier.testTag("dashboard_nav_calendar")
                    )
                    ModuleNavigationCard(
                        title = "Phone System",
                        icon = Icons.Default.Phone,
                        onClick = { onNavigateTab("phone") },
                        modifier = Modifier.testTag("dashboard_nav_phone")
                    )
                    ModuleNavigationCard(
                        title = "Multi-Agent Chat",
                        icon = Icons.Default.SmartToy,
                        onClick = { onNavigateTab("chat") },
                        modifier = Modifier.testTag("dashboard_nav_chat")
                    )
                    ModuleNavigationCard(
                        title = "Voice Agent",
                        icon = Icons.Default.Mic,
                        onClick = { onNavigateTab("transcribe") },
                        modifier = Modifier.testTag("dashboard_nav_voice")
                    )
                    ModuleNavigationCard(
                        title = "Deep Reasoning",
                        icon = Icons.Default.Psychology,
                        onClick = { onNavigateTab("reasoning") },
                        modifier = Modifier.testTag("dashboard_nav_reasoning")
                    )
                }
            }

            // 3. Workload Automation Meter & Onboarding Customization Banner
            item {
                if (isMedium) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WorkloadReductionCard(
                            targetPct = targetPct,
                            activeAgentsList = activeAgentsList,
                            modifier = Modifier.weight(1f)
                        )
                        OnboardingStatusCard(
                            companyName = companyName,
                            industry = industry,
                            bottleneck = companyProfile?.primaryBottleneck ?: "Executive Reporting & Task Dispatch",
                            onOpenOnboarding = onOpenOnboarding,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        WorkloadReductionCard(
                            targetPct = targetPct,
                            activeAgentsList = activeAgentsList,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OnboardingStatusCard(
                            companyName = companyName,
                            industry = industry,
                            bottleneck = companyProfile?.primaryBottleneck ?: "Executive Reporting & Task Dispatch",
                            onOpenOnboarding = onOpenOnboarding,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. Quick Action Launchers
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "RCOS Agent Command Center",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            title = "Multi-Agent Chat",
                            icon = Icons.AutoMirrored.Filled.Chat,
                            description = "Workload & Strategy Agents",
                            onClick = { onNavigateTab("chat") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("action_chat_button")
                        )
                        QuickActionButton(
                            title = "Voice Assistant",
                            icon = Icons.Default.Mic,
                            description = "Audio transcription",
                            onClick = { onNavigateTab("transcribe") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("action_voice_button")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            title = "Deep Reasoning",
                            icon = Icons.Default.Psychology,
                            description = "High-thinking analysis",
                            onClick = { onNavigateTab("reasoning") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("action_reasoning_button")
                        )
                        QuickActionButton(
                            title = "Saved Intelligence",
                            icon = Icons.Default.Bookmark,
                            description = "Corporate reports feed",
                            onClick = { onNavigateTab("saved") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("action_saved_button")
                        )
                    }
                }
            }

            // 4. Instant Workload Content Analyzer Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("content_analyzer_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(
                                    text = "RCOS Workload Analyzer",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Automate analysis of corporate documents, emails, code, or meeting notes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Input TextField
                        OutlinedTextField(
                            value = analyzerInput,
                            onValueChange = { viewModel.setAnalyzerInput(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 200.dp)
                                .testTag("analyzer_input"),
                            placeholder = { Text("Paste corporate document, report, raw data, or code to run RCOS analysis...") },
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Preset Chips Row
                        Text(
                            text = "Analysis Directive Preset:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(presets) { preset ->
                                FilterChip(
                                    selected = analyzerPreset == preset,
                                    onClick = { viewModel.setAnalyzerPreset(preset) },
                                    label = { Text(preset) },
                                    leadingIcon = if (analyzerPreset == preset) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // Run Button
                        Button(
                            onClick = { viewModel.runAnalyzer() },
                            enabled = !isAnalyzing && analyzerInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("run_analyzer_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("RCOS Multi-Agent Analysis Running...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Execute RCOS $analyzerPreset")
                            }
                        }

                        // Result Box
                        AnimatedVisibility(
                            visible = analyzerResult != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            analyzerResult?.let { resultText ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                                            Text(
                                                text = "RCOS Intelligence Output",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(resultText))
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy text",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Button(
                                                    onClick = { viewModel.saveAnalyzerResultToDashboard() },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.testTag("save_analyzer_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Bookmark,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Save Report", fontSize = 12.sp)
                                                }
                                            }
                                        }

                                        Text(
                                            text = resultText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Recent Intelligence Cards Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Intelligence Feed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    TextButton(onClick = { onNavigateTab("saved") }) {
                        Text("View Archive (${dashboardItems.size})")
                    }
                }
            }

            if (dashboardItems.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No saved corporate intelligence reports yet.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "Run RCOS Workload Analyzer, Voice Assistant, or Deep Reasoning to archive items.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(dashboardItems.take(5)) { item ->
                    DashboardItemCard(
                        item = item,
                        onTogglePin = { viewModel.togglePin(item.id, item.isPinned) },
                        onDelete = { viewModel.deleteDashboardItem(item.id) },
                        onCopy = { clipboardManager.setText(AnnotatedString(item.content)) }
                    )
                }
            }
        }
        }
    }

    agentToLiveDirect?.let { agent ->
        val agentResp = responsibilities.find { it.agentName.contains(agent.agentName, true) }
        val agentLogs = aiLogs.filter { it.agentName.contains(agent.agentName, true) }
        val opState = com.example.ui.screens.deriveAgentOperationalState(agent, aiLogs, emptyList())

        AgentLiveTaskDirectorDialog(
            agent = agent,
            operationalState = opState,
            responsibility = agentResp,
            latestLogs = agentLogs,
            viewModel = viewModel,
            onDismiss = { agentToLiveDirect = null }
        )
    }
}
}

@Composable
private fun WorkloadReductionCard(
    targetPct: Int,
    activeAgentsList: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Workload Reduction Goal",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$targetPct% Target",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { targetPct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                text = "Active RCOS Sub-Agents: $activeAgentsList",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OnboardingStatusCard(
    companyName: String,
    industry: String,
    bottleneck: String,
    onOpenOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customer Onboarding Setup",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                IconButton(
                    onClick = onOpenOnboarding,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("onboarding_gear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Customize Onboarding",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "$companyName ($industry)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Focus Bottleneck: $bottleneck",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            OutlinedButton(
                onClick = onOpenOnboarding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("customize_rcos_button"),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Customize Customer RCOS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DashboardItemCard(
    item: DashboardItem,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_item_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        border = if (item.isPinned) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))) else CardDefaults.outlinedCardBorder()
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val icon = when (item.itemType) {
                        "TRANSCRIPTION" -> Icons.Default.Mic
                        "REASONING" -> Icons.Default.Psychology
                        else -> Icons.Default.AutoAwesome
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (item.isPinned) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Pin item",
                            tint = if (item.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy text",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete item",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = item.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ModuleNavigationCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111726)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2A42))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = com.example.ui.theme.RcosNeonGreen,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            )
        }
    }
}

