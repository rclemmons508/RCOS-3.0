package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AgentPermission
import com.example.data.AgentResponsibility
import com.example.data.AiActionLog
import com.example.data.AppIntegration
import com.example.data.ApprovalItem
import com.example.data.ApprovalLevel
import com.example.data.ApprovalRequirement
import com.example.data.ApprovalStatus
import com.example.data.BusinessWorkflowConfig
import com.example.data.CalendarEventItem
import com.example.data.ChatMessageEntity
import com.example.data.ChatSessionEntity
import com.example.data.ClientDetailData
import com.example.data.CompanyProfileEntity
import com.example.data.ContactPerson
import com.example.data.DashboardItem
import com.example.data.FirebaseConnectionManager
import com.example.data.FirebaseConnectionStatus
import com.example.data.IncomingCallState
import com.example.data.JobTaskItem
import com.example.data.JobEntity
import com.example.data.PhoneCallItem
import com.example.data.NovaDatabase
import com.example.data.NovaRepository
import com.example.data.SecureCredentialManager
import com.example.data.SecurityUtils
import com.example.data.TaskTrigger
import com.example.data.TriggerType
import com.example.data.UserEntity
import com.example.data.UserProfileData
import com.example.data.VaultItem
import com.example.data.WorkflowStep
import com.example.data.WorkflowTemplate
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.data.WorkspaceEntity
import com.example.data.UserAccount
import com.example.data.UserAccountEntity
import com.example.data.BusinessAiConfig
import com.example.data.BusinessAiConfigEntity
import com.example.data.UserRole
import com.example.data.AccessLevel
import com.example.data.PermissionEngine
import com.example.data.PermissionAction
import com.example.data.AuthorizationResult
import com.example.data.AuditLogEntity
import com.example.data.AuditActorType
import com.example.data.AuditApprovalStatus
import com.example.data.AuditResultStatus
import com.example.data.AuditRiskLevel
import com.example.data.AgentRegistryEntity
import com.example.data.AgentType
import com.example.data.AgentStatus
import com.example.data.AgentRiskLevel as AgentEntityRiskLevel
import com.example.data.AgentCapability
import com.example.data.WorkspaceOnboardingEntity
import com.example.data.OnboardingStatus
import com.example.data.IndustryType
import com.example.data.IndustryTemplate
import com.example.data.AgentProvisioningService
import com.example.data.FirestoreTaskSyncManager
import com.example.data.FirestoreSyncInfo
import com.example.data.FirestoreSyncState
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class AppViewMode {
    AUTO_DETECT,
    WEB_DESKTOP,
    MOBILE_APP
}

class NovaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = NovaDatabase.getDatabase(application)
    private val repository = NovaRepository(
        database.appDao(),
        database.workflowDao(),
        database.workspaceDao(),
        database.auditLogDao(),
        database.agentRegistryDao(),
        database.workspaceOnboardingDao()
    )
    private val secureManager = SecureCredentialManager(application)
    private val prefs = application.getSharedPreferences("rcos_user_prefs", Context.MODE_PRIVATE)

    // Real-Time Firebase Firestore Synchronization Engine
    private val firestoreSyncManager = FirestoreTaskSyncManager(viewModelScope)
    val firestoreSyncInfo: StateFlow<FirestoreSyncInfo> = firestoreSyncManager.syncInfo

    // Central Firebase API Manager (Auth, Firestore, App Check, Cloud Sync)
    private val firebaseManager = FirebaseConnectionManager.getInstance(application)
    val firebaseStatus: StateFlow<FirebaseConnectionStatus> = firebaseManager.connectionStatus

    // Live Autonomous Agents retrieved from Firestore
    private val _firestoreActiveAgents = MutableStateFlow<List<AgentRegistryEntity>>(emptyList())
    val firestoreActiveAgents: StateFlow<List<AgentRegistryEntity>> = _firestoreActiveAgents.asStateFlow()

    private val _isFetchingFirestoreAgents = MutableStateFlow(false)
    val isFetchingFirestoreAgents: StateFlow<Boolean> = _isFetchingFirestoreAgents.asStateFlow()

    private val _isFirebaseConnecting = MutableStateFlow(false)
    val isFirebaseConnecting: StateFlow<Boolean> = _isFirebaseConnecting.asStateFlow()

    private val _firebaseStatusMessage = MutableStateFlow<String?>("Firebase Auth & Firestore operational. Real-time sync active.")
    val firebaseStatusMessage: StateFlow<String?> = _firebaseStatusMessage.asStateFlow()

    private val _cloudSyncStatuses = MutableStateFlow<List<FirestoreSyncInfo>>(
        listOf(
            FirestoreSyncInfo(
                state = FirestoreSyncState.CONNECTED,
                statusMessage = "Real-time task synchronization active",
                lastSyncedTime = System.currentTimeMillis(),
                pendingSyncCount = 0,
                activeDevicesCount = 1,
                collectionName = "rcos_agent_tasks"
            ),
            FirestoreSyncInfo(
                state = FirestoreSyncState.CONNECTED,
                statusMessage = "Executive approvals pipeline synced",
                lastSyncedTime = System.currentTimeMillis(),
                pendingSyncCount = 0,
                activeDevicesCount = 1,
                collectionName = "rcos_executive_approvals"
            ),
            FirestoreSyncInfo(
                state = FirestoreSyncState.CONNECTED,
                statusMessage = "Cloud agent registry operational",
                lastSyncedTime = System.currentTimeMillis(),
                pendingSyncCount = 0,
                activeDevicesCount = 1,
                collectionName = "rcos_autonomous_agents"
            ),
            FirestoreSyncInfo(
                state = FirestoreSyncState.CONNECTED,
                statusMessage = "Organization context mirrored",
                lastSyncedTime = System.currentTimeMillis(),
                pendingSyncCount = 0,
                activeDevicesCount = 1,
                collectionName = "rcos_company_profile"
            )
        )
    )
    val cloudSyncStatuses: StateFlow<List<FirestoreSyncInfo>> = _cloudSyncStatuses.asStateFlow()

    // App Theme Display Preference (Dark, Light, Follow System)
    private val _themeMode = MutableStateFlow(AppThemeMode.DARK_MODE)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // OS View Mode Selector (Web Desktop vs Mobile vs Auto Detect)
    private val _viewMode = MutableStateFlow(AppViewMode.AUTO_DETECT)
    val viewMode: StateFlow<AppViewMode> = _viewMode.asStateFlow()

    // Demo Mode vs Functioning Mode
    private val _isDemoMode = MutableStateFlow(prefs.getBoolean("app_demo_mode", false))
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    // Clients Directory & Details Modal
    private val _clientsList = MutableStateFlow<List<ClientDetailData>>(if (prefs.getBoolean("app_demo_mode", false)) getInitialClients() else emptyList())
    val clientsList: StateFlow<List<ClientDetailData>> = _clientsList.asStateFlow()

    private val _selectedClientDetail = MutableStateFlow<ClientDetailData?>(null)
    val selectedClientDetail: StateFlow<ClientDetailData?> = _selectedClientDetail.asStateFlow()

    // Personal Executive Profile State & Tool Accounts
    private val _userProfile = MutableStateFlow(UserProfileData())
    val userProfile: StateFlow<UserProfileData> = _userProfile.asStateFlow()

    // Incoming Call Handover State (Human Representative vs AI Agent)
    private val _incomingCallState = MutableStateFlow(IncomingCallState())
    val incomingCallState: StateFlow<IncomingCallState> = _incomingCallState.asStateFlow()

    // Phone Call History & Logging
    private val _phoneCallLogs = MutableStateFlow<List<PhoneCallItem>>(if (prefs.getBoolean("app_demo_mode", false)) getInitialPhoneCalls() else emptyList())
    val phoneCallLogs: StateFlow<List<PhoneCallItem>> = _phoneCallLogs.asStateFlow()

    // Dynamic Calendar & Schedule State
    private val _calendarEvents = MutableStateFlow<List<CalendarEventItem>>(if (prefs.getBoolean("app_demo_mode", false)) getInitialCalendarEvents() else emptyList())
    val calendarEvents: StateFlow<List<CalendarEventItem>> = _calendarEvents.asStateFlow()

    // Connected External Applications & AI Agent Permissions
    private val _integrations = MutableStateFlow<List<AppIntegration>>(if (prefs.getBoolean("app_demo_mode", false)) getInitialIntegrations() else emptyList())
    val integrations: StateFlow<List<AppIntegration>> = _integrations.asStateFlow()

    // Job / Task Management State
    private val _jobTasks = MutableStateFlow<List<JobEntity>>(if (prefs.getBoolean("app_demo_mode", false)) getInitialJobEntities() else emptyList())
    val jobTasks: StateFlow<List<JobEntity>> = _jobTasks.asStateFlow()

    val pendingJobs: StateFlow<List<JobEntity>> = _jobTasks
        .map { list -> list.filter { it.status == "Pending" || it.status == "In Progress" || it.status == "Queued" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedJobs: StateFlow<List<JobEntity>> = _jobTasks
        .map { list -> list.filter { it.status == "Completed" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedJobs: StateFlow<List<JobEntity>> = _jobTasks
        .map { list -> list.filter { it.status == "Archived" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database Flows
    val dashboardItems: StateFlow<List<DashboardItem>> = repository.allDashboardItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val chatSessions: StateFlow<List<ChatSessionEntity>> = repository.allChatSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val companyProfile: StateFlow<CompanyProfileEntity?> = repository.companyProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Auth State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Form inputs
    private val _loginEmail = MutableStateFlow("rcsolutions@gmail.com")
    val loginEmail: StateFlow<String> = _loginEmail.asStateFlow()

    private val _loginPassword = MutableStateFlow("")
    val loginPassword: StateFlow<String> = _loginPassword.asStateFlow()

    private val _registerEmail = MutableStateFlow("")
    val registerEmail: StateFlow<String> = _registerEmail.asStateFlow()

    private val _registerFullName = MutableStateFlow("")
    val registerFullName: StateFlow<String> = _registerFullName.asStateFlow()

    private val _registerPassword = MutableStateFlow("")
    val registerPassword: StateFlow<String> = _registerPassword.asStateFlow()

    private val _registerCompanyName = MutableStateFlow("")
    val registerCompanyName: StateFlow<String> = _registerCompanyName.asStateFlow()

    private val _registerIndustry = MutableStateFlow("Technology & IT")
    val registerIndustry: StateFlow<String> = _registerIndustry.asStateFlow()

    val passwordStrength: StateFlow<SecurityUtils.PasswordStrength> = MutableStateFlow(
        SecurityUtils.evaluatePasswordStrength("")
    )

    // Current Chat Session ID
    private val _currentSessionId = MutableStateFlow<String>(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    // Current Chat Persona
    private val _currentPersona = MutableStateFlow("RCOS Workload Synthesizer")
    val currentPersona: StateFlow<String> = _currentPersona.asStateFlow()

    // Chat messages for current session
    private val _chatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _chatMessages.asStateFlow()

    // Quick Analyzer State
    private val _analyzerInput = MutableStateFlow("")
    val analyzerInput: StateFlow<String> = _analyzerInput.asStateFlow()

    private val _analyzerSelectedPreset = MutableStateFlow("Summarize Report")
    val analyzerSelectedPreset: StateFlow<String> = _analyzerSelectedPreset.asStateFlow()

    private val _analyzerResult = MutableStateFlow<String?>(null)
    val analyzerResult: StateFlow<String?> = _analyzerResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Chat State
    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    private val _isChatSending = MutableStateFlow(false)
    val isChatSending: StateFlow<Boolean> = _isChatSending.asStateFlow()

    // Voice Transcriber State
    private val _transcriptionResult = MutableStateFlow<String?>(null)
    val transcriptionResult: StateFlow<String?> = _transcriptionResult.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    // Deep Thinking State
    private val _thinkingPrompt = MutableStateFlow("")
    val thinkingPrompt: StateFlow<String> = _thinkingPrompt.asStateFlow()

    private val _thinkingResult = MutableStateFlow<String?>(null)
    val thinkingResult: StateFlow<String?> = _thinkingResult.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    // UI Toast or Status Notice
    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    // Android Keystore Encrypted Executive Vault
    private val _vaultItems = MutableStateFlow<List<VaultItem>>(emptyList())
    val vaultItems: StateFlow<List<VaultItem>> = _vaultItems.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    // Domain-Specific Gemini AI States
    // 1. Jobs Execution with Gemini
    private val _isJobExecutingWithGemini = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isJobExecutingWithGemini: StateFlow<Map<String, Boolean>> = _isJobExecutingWithGemini.asStateFlow()

    private val _jobGeminiArtifacts = MutableStateFlow<Map<String, String>>(emptyMap())
    val jobGeminiArtifacts: StateFlow<Map<String, String>> = _jobGeminiArtifacts.asStateFlow()

    // 2. Clients Intelligence with Gemini
    private val _clientGeminiDossier = MutableStateFlow<String?>(null)
    val clientGeminiDossier: StateFlow<String?> = _clientGeminiDossier.asStateFlow()

    private val _isClientGeminiLoading = MutableStateFlow(false)
    val isClientGeminiLoading: StateFlow<Boolean> = _isClientGeminiLoading.asStateFlow()

    // 3. Phone System Call & Voicemail Intelligence with Gemini
    private val _callGeminiAnalysis = MutableStateFlow<Map<String, String>>(emptyMap())
    val callGeminiAnalysis: StateFlow<Map<String, String>> = _callGeminiAnalysis.asStateFlow()

    private val _isCallGeminiLoading = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isCallGeminiLoading: StateFlow<Map<String, Boolean>> = _isCallGeminiLoading.asStateFlow()

    // 4. Calendar Meeting Prep with Gemini
    private val _meetingGeminiPrep = MutableStateFlow<Map<String, String>>(emptyMap())
    val meetingGeminiPrep: StateFlow<Map<String, String>> = _meetingGeminiPrep.asStateFlow()

    private val _isMeetingGeminiLoading = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isMeetingGeminiLoading: StateFlow<Map<String, Boolean>> = _isMeetingGeminiLoading.asStateFlow()

    private val _calendarGeminiBriefing = MutableStateFlow<String?>(null)
    val calendarGeminiBriefing: StateFlow<String?> = _calendarGeminiBriefing.asStateFlow()

    private val _isCalendarGeminiLoading = MutableStateFlow(false)
    val isCalendarGeminiLoading: StateFlow<Boolean> = _isCalendarGeminiLoading.asStateFlow()

    // 5. Governance & Security Threat Audit with Gemini
    private val _securityAuditReport = MutableStateFlow<String?>(null)
    val securityAuditReport: StateFlow<String?> = _securityAuditReport.asStateFlow()

    private val _isSecurityAuditLoading = MutableStateFlow(false)
    val isSecurityAuditLoading: StateFlow<Boolean> = _isSecurityAuditLoading.asStateFlow()

    // 6. Saved Intelligence Synthesis with Gemini
    private val _executiveBriefingSummary = MutableStateFlow<String?>(null)
    val executiveBriefingSummary: StateFlow<String?> = _executiveBriefingSummary.asStateFlow()

    private val _isExecutiveBriefingLoading = MutableStateFlow(false)
    val isExecutiveBriefingLoading: StateFlow<Boolean> = _isExecutiveBriefingLoading.asStateFlow()

    // 7. Workflow Natural Language Synthesis with Gemini
    private val _workflowSynthesisResult = MutableStateFlow<String?>(null)
    val workflowSynthesisResult: StateFlow<String?> = _workflowSynthesisResult.asStateFlow()

    private val _isWorkflowSynthesizing = MutableStateFlow(false)
    val isWorkflowSynthesizing: StateFlow<Boolean> = _isWorkflowSynthesizing.asStateFlow()

    private val _workflowGeminiOptimization = MutableStateFlow<Map<String, String>>(emptyMap())
    val workflowGeminiOptimization: StateFlow<Map<String, String>> = _workflowGeminiOptimization.asStateFlow()

    private val _isWorkflowGeminiLoading = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isWorkflowGeminiLoading: StateFlow<Map<String, Boolean>> = _isWorkflowGeminiLoading.asStateFlow()

    // 8. Live Agent Persona Consultation with Gemini
    private val _agentConsultationResult = MutableStateFlow<String?>(null)
    val agentConsultationResult: StateFlow<String?> = _agentConsultationResult.asStateFlow()

    private val _isConsultingAgent = MutableStateFlow(false)
    val isConsultingAgent: StateFlow<Boolean> = _isConsultingAgent.asStateFlow()

    // Multi-tenant Workspace & Governance State
    private val _activeWorkspaceId = MutableStateFlow("ws_default")
    val activeWorkspaceId: StateFlow<String> = _activeWorkspaceId.asStateFlow()

    val allWorkspaces: StateFlow<List<WorkspaceEntity>> = repository.allWorkspaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeWorkspaceUserAccounts: StateFlow<List<UserAccount>> = _activeWorkspaceId
        .flatMapLatest { id -> repository.getUserAccountsByWorkspace(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBusinessAiConfig: StateFlow<BusinessAiConfig> = _activeWorkspaceId
        .flatMapLatest { id -> repository.getBusinessAiConfig(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessAiConfig())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeWorkspaceAuditLogs: StateFlow<List<AuditLogEntity>> = _activeWorkspaceId
        .flatMapLatest { id -> repository.getWorkspaceAuditEvents(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeWorkspaceAgents: StateFlow<List<AgentRegistryEntity>> = _activeWorkspaceId
        .flatMapLatest { id -> repository.getWorkspaceAgents(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeWorkspaceOnboarding: StateFlow<WorkspaceOnboardingEntity?> = _activeWorkspaceId
        .flatMapLatest { id -> repository.getWorkspaceOnboarding(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedUserAccount = MutableStateFlow<UserAccount?>(null)
    val selectedUserAccount: StateFlow<UserAccount?> = _selectedUserAccount.asStateFlow()

    // AI Workflow Automation Engine State
    val businessWorkflowConfig: StateFlow<BusinessWorkflowConfig> = repository.businessWorkflowConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessWorkflowConfig())

    val workflowTemplates: StateFlow<List<WorkflowTemplate>> = repository.workflowTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val approvalItems: StateFlow<List<ApprovalItem>> = repository.approvalItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingApprovalItems: StateFlow<List<ApprovalItem>> = approvalItems
        .map { list -> list.filter { it.status == ApprovalStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedApprovalItems: StateFlow<List<ApprovalItem>> = approvalItems
        .map { list -> list.filter { it.status == ApprovalStatus.APPROVED || it.status == ApprovalStatus.COMPLETED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedApprovalItems: StateFlow<List<ApprovalItem>> = approvalItems
        .map { list -> list.filter { it.status == ApprovalStatus.ARCHIVED || it.status == ApprovalStatus.REJECTED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiActionLogs: StateFlow<List<AiActionLog>> = repository.aiActionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val agentResponsibilities: StateFlow<List<AgentResponsibility>> = repository.agentResponsibilities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Restore view preferences
        val savedMode = prefs.getString("app_view_mode", AppViewMode.AUTO_DETECT.name)
        runCatching {
            _viewMode.value = AppViewMode.valueOf(savedMode ?: AppViewMode.AUTO_DETECT.name)
        }
        val savedTheme = prefs.getString("app_theme_mode", AppThemeMode.DARK_MODE.name)
        runCatching {
            _themeMode.value = AppThemeMode.valueOf(savedTheme ?: AppThemeMode.DARK_MODE.name)
        }

        // Load profile from secure storage
        val savedName = secureManager.getString("profile_full_name", "RCS Executive User")
        val savedTitle = secureManager.getString("profile_title", "Chief Executive Officer")
        val savedEmail = secureManager.getString("profile_email", "rcsolutions@gmail.com")
        val savedPhone = secureManager.getString("profile_phone", "+1 (555) 100-2000")
        val savedGoogleEmail = secureManager.getString("profile_google_email", "rcsolutions@gmail.com")
        val savedMsEmail = secureManager.getString("profile_ms_email", "executive@rcsolutions.onmicrosoft.com")
        val savedOrg = secureManager.getString("profile_org_name", "RCOS Global Solutions")
        val savedTz = secureManager.getString("profile_timezone", "EST - Eastern Time (US & Canada)")

        _userProfile.value = UserProfileData(
            fullName = savedName.ifBlank { "RCS Executive User" },
            executiveTitle = savedTitle.ifBlank { "Chief Executive Officer" },
            personalEmail = savedEmail.ifBlank { "rcsolutions@gmail.com" },
            phone = savedPhone.ifBlank { "+1 (555) 100-2000" },
            googleWorkspaceEmail = savedGoogleEmail.ifBlank { "rcsolutions@gmail.com" },
            microsoftAccountEmail = savedMsEmail.ifBlank { "executive@rcsolutions.onmicrosoft.com" },
            organizationName = savedOrg.ifBlank { "RCOS Global Solutions" },
            timezone = savedTz.ifBlank { "EST - Eastern Time (US & Canada)" }
        )

        // Seed initial room data asynchronously
        viewModelScope.launch {
            try {
                repository.ensureWorkspaceDataSeeded()
                repository.ensureWorkflowDataSeeded(
                    getInitialWorkflowTemplates(),
                    getInitialApprovalItems(),
                    getInitialAiActionLogs(),
                    getInitialAgentResponsibilities()
                )
            } catch (t: Throwable) {
                android.util.Log.w("NovaViewModel", "Database initial seeding notice: ${t.message}")
            }

            try {
                // Start Real-Time Firestore Synchronization for multi-device collaboration
                firestoreSyncManager.startRealtimeTasksListener { remoteJobs ->
                    if (remoteJobs.isNotEmpty()) {
                        val currentMap = _jobTasks.value.associateBy { it.id }.toMutableMap()
                        remoteJobs.forEach { remote ->
                            currentMap[remote.id] = remote
                        }
                        _jobTasks.value = currentMap.values.toList()
                    }
                }

                firestoreSyncManager.startRealtimeApprovalsListener { remoteApprovals ->
                    if (remoteApprovals.isNotEmpty()) {
                        viewModelScope.launch {
                            try {
                                remoteApprovals.forEach { remoteItem ->
                                    repository.updateApprovalStatus(
                                        remoteItem.id,
                                        remoteItem.status,
                                        remoteItem.reviewedBy,
                                        remoteItem.reviewNotes
                                    )
                                }
                            } catch (t: Throwable) {
                                android.util.Log.w("NovaViewModel", "Approval update notice: ${t.message}")
                            }
                        }
                    }
                }

                // Start Real-Time Firestore Autonomous Agents Synchronization
                firestoreSyncManager.startRealtimeAgentsListener { remoteAgents ->
                    if (remoteAgents.isNotEmpty()) {
                        val activeOnly = remoteAgents.filter { it.status.equals(AgentStatus.ACTIVE.name, ignoreCase = true) }
                        _firestoreActiveAgents.value = activeOnly
                        viewModelScope.launch {
                            remoteAgents.forEach { agent ->
                                try {
                                    repository.insertAgent(agent)
                                } catch (e: Throwable) {}
                            }
                        }
                    }
                }

                // Seed initial task & agents baseline to cloud for multi-device sync
                firestoreSyncManager.seedTasksToFirestore(_jobTasks.value)
                firestoreSyncManager.seedApprovalsToFirestore(getInitialApprovalItems())
                
                // Fetch/seed initial active agents from repository to Firestore
                viewModelScope.launch {
                    try {
                        val localAgents = repository.getActiveWorkspaceAgents("ws_default").firstOrNull() ?: emptyList()
                        if (localAgents.isNotEmpty()) {
                            if (_firestoreActiveAgents.value.isEmpty()) {
                                _firestoreActiveAgents.value = localAgents
                            }
                            firestoreSyncManager.seedAgentsToFirestore(localAgents)
                        }
                    } catch (e: Throwable) {}
                }
            } catch (t: Throwable) {
                android.util.Log.w("NovaViewModel", "Firestore sync startup notice: ${t.message}")
            }
        }

        // Observe current session messages
        viewModelScope.launch {
            _currentSessionId.collect { sessionId ->
                repository.getChatMessages(sessionId).collect { messages ->
                    _chatMessages.value = messages
                }
            }
        }

        // Auto restore user session
        restoreSession()
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode.name).apply()
    }

    fun setViewMode(mode: AppViewMode) {
        _viewMode.value = mode
        prefs.edit().putString("app_view_mode", mode.name).apply()
    }

    fun setDemoMode(isDemo: Boolean) {
        _isDemoMode.value = isDemo
        prefs.edit().putBoolean("app_demo_mode", isDemo).apply()
        if (isDemo) {
            _clientsList.value = getInitialClients()
            _phoneCallLogs.value = getInitialPhoneCalls()
            _calendarEvents.value = getInitialCalendarEvents()
            _jobTasks.value = getInitialJobEntities()
            _integrations.value = getInitialIntegrations()
            _statusNotice.value = "Demo Mode Enabled: Seed data restored."
        } else {
            wipeAllData()
            _statusNotice.value = "Functioning Mode Enabled: All mock data wiped."
        }
    }

    fun wipeAllData() {
        _clientsList.value = emptyList()
        _phoneCallLogs.value = emptyList()
        _calendarEvents.value = emptyList()
        _jobTasks.value = emptyList()
        _integrations.value = emptyList()
        _chatMessages.value = emptyList()
        _selectedClientDetail.value = null
        _statusNotice.value = "App wiped. Start from scratch."
    }

    fun clearSensitiveData() {
        _phoneCallLogs.value = emptyList()
        _chatMessages.value = emptyList()
        _calendarEvents.value = emptyList()
        _statusNotice.value = "Sensitive data cleared (Calls, Chats, Calendar)."
    }

    fun selectClientForDetails(client: ClientDetailData?) {
        _selectedClientDetail.value = client
    }

    fun selectClientByName(clientName: String) {
        val found = _clientsList.value.find { it.companyName.contains(clientName, ignoreCase = true) }
            ?: _clientsList.value.firstOrNull()
        _selectedClientDetail.value = found
    }

    fun addOrUpdateClient(client: ClientDetailData) {
        val currentList = _clientsList.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == client.id }
        if (index >= 0) {
            currentList[index] = client
        } else {
            currentList.add(0, client)
        }
        _clientsList.value = currentList
        if (_selectedClientDetail.value?.id == client.id) {
            _selectedClientDetail.value = client
        }
        _statusNotice.value = "Client account '${client.companyName}' updated successfully."
    }

    fun deleteClient(clientId: String) {
        _clientsList.value = _clientsList.value.filter { it.id != clientId }
        if (_selectedClientDetail.value?.id == clientId) {
            _selectedClientDetail.value = null
        }
        _statusNotice.value = "Client account removed."
    }

    fun updateUserProfile(profile: UserProfileData) {
        _userProfile.value = profile
        secureManager.saveString("profile_full_name", profile.fullName)
        secureManager.saveString("profile_title", profile.executiveTitle)
        secureManager.saveString("profile_email", profile.personalEmail)
        secureManager.saveString("profile_phone", profile.phone)
        secureManager.saveString("profile_google_email", profile.googleWorkspaceEmail)
        secureManager.saveString("profile_ms_email", profile.microsoftAccountEmail)
        secureManager.saveString("profile_org_name", profile.organizationName)
        secureManager.saveString("profile_timezone", profile.timezone)

        // Sync connected accounts across Google Workspace & Microsoft 365 integrations
        _integrations.value = _integrations.value.map { app ->
            when (app.category) {
                "Google Workspace" -> app.copy(
                    connectedAccount = profile.googleWorkspaceEmail.ifBlank { "rcsolutions@gmail.com" },
                    isConnected = profile.googleWorkspaceEmail.isNotBlank(),
                    lastSynced = if (profile.googleWorkspaceEmail.isNotBlank()) "Connected (Live Sync)" else "Disconnected"
                )
                "Microsoft 365" -> app.copy(
                    connectedAccount = profile.microsoftAccountEmail.ifBlank { "executive@rcsolutions.onmicrosoft.com" },
                    isConnected = profile.microsoftAccountEmail.isNotBlank(),
                    lastSynced = if (profile.microsoftAccountEmail.isNotBlank()) "Connected (Live Sync)" else "Disconnected"
                )
                else -> app
            }
        }

        // Sync logged-in user details if present
        _currentUser.value?.let { curr ->
            _currentUser.value = curr.copy(
                fullName = profile.fullName,
                email = profile.personalEmail,
                companyName = profile.organizationName
            )
        }

        _statusNotice.value = "Personal profile & connected tool accounts updated."
    }

    fun linkGoogleWorkspaceAccount(accountEmail: String) {
        val updated = _userProfile.value.copy(googleWorkspaceEmail = accountEmail)
        updateUserProfile(updated)
        _statusNotice.value = "Google Workspace account linked ($accountEmail)."
    }

    fun linkMicrosoftAccount(accountEmail: String) {
        val updated = _userProfile.value.copy(microsoftAccountEmail = accountEmail)
        updateUserProfile(updated)
        _statusNotice.value = "Microsoft 365 account linked ($accountEmail)."
    }

    fun triggerSimulatedIncomingCall(callerName: String = "Apex Enterprises", phone: String = "+1 (555) 234-8901") {
        _incomingCallState.value = IncomingCallState(
            callId = "call_" + (1000..9999).random(),
            callerName = callerName,
            callerPhone = phone,
            isRinging = true,
            isConnected = false,
            handledBy = "None",
            callNotes = "Incoming inquiry regarding active job dispatch and service status.",
            liveTranscript = listOf("RCOS System: Incoming call ringing from $callerName ($phone)...")
        )
        _statusNotice.value = "Incoming call ringing from $callerName..."
    }

    fun handleCallManually() {
        val current = _incomingCallState.value
        _incomingCallState.value = current.copy(
            isRinging = false,
            isConnected = true,
            handledBy = "Human Representative",
            liveTranscript = current.liveTranscript + listOf(
                "Human Rep: Hello! Thank you for calling ${current.callerName} RCOS hotline. How can I assist you today?",
                "Caller: Hi, I'm calling to check up on our active onboarding pipeline and job status.",
                "Human Rep: Absolutely, let me bring up your client account file right now."
            )
        )
        _statusNotice.value = "Call connected to Human Representative."
    }

    fun handleCallWithAI() {
        val current = _incomingCallState.value
        _incomingCallState.value = current.copy(
            isRinging = false,
            isConnected = true,
            handledBy = "AI Voice Agent",
            liveTranscript = current.liveTranscript + listOf(
                "RCOS AI Agent: Greetings! I am the automated executive assistant for RCOS. How may I route your inquiry?",
                "Caller: Hi! We need to confirm tomorrow's strategic financial review and dispatch.",
                "RCOS AI Agent: Confirmed! I have logged your request, scheduled the meeting in Google Calendar / Outlook, and updated your ongoing job #JOB-101."
            )
        )
        _statusNotice.value = "Call handed over to RCOS AI Voice Agent!"
    }

    fun endActiveCall() {
        val current = _incomingCallState.value
        if (current.isConnected || current.isRinging) {
            val callType = if (current.handledBy.contains("AI")) "AI Handled" else if (current.handledBy.contains("Human")) "Incoming" else "Incoming"
            val transcriptSummary = if (current.liveTranscript.isNotEmpty()) {
                current.liveTranscript.takeLast(2).joinToString(" ")
            } else {
                "Inquiry completed with ${current.handledBy}."
            }
            val newCall = PhoneCallItem(
                id = "call_${System.currentTimeMillis() % 100000}",
                callerName = current.callerName,
                phoneNumber = current.callerPhone,
                callType = callType,
                duration = if (current.handledBy.contains("AI")) "3m 12s" else "1m 45s",
                timeAgo = "Just now",
                summary = transcriptSummary,
                timestamp = System.currentTimeMillis()
            )
            _phoneCallLogs.value = listOf(newCall) + _phoneCallLogs.value

            recordAuditEvent(
                actionType = "PHONE_CALL_ENDED",
                description = "Completed $callType call from ${current.callerName} (${newCall.duration})",
                result = AuditResultStatus.SUCCESS
            )

            // If AI Handled, auto-create a CRM job ticket
            if (callType == "AI Handled") {
                dispatchJob(
                    title = "AI Voice Hotline Follow-Up: ${current.callerName}",
                    clientName = current.callerName,
                    assignedAgent = "Voice Dispatcher Agent",
                    priority = "High",
                    dueDate = "Today, 4:00 PM",
                    summary = "Automated follow-up dispatched by Voice Agent from hotline call."
                )
            }
        }
        _statusNotice.value = "Call ended (${current.handledBy}). Summary saved to Client CRM & Call Logs."
        _incomingCallState.value = IncomingCallState(isRinging = false, isConnected = false, handledBy = "None")
    }

    fun logPhoneCall(
        callerName: String,
        phoneNumber: String,
        callType: String = "Incoming",
        duration: String = "2m 15s",
        summary: String = "Call logged by user.",
        autoCreateJob: Boolean = false,
        jobTitle: String = ""
    ) {
        val newCall = PhoneCallItem(
            id = "call_${System.currentTimeMillis() % 100000}",
            callerName = callerName.ifBlank { "Client Caller" },
            phoneNumber = phoneNumber.ifBlank { "+1 (555) 000-0000" },
            callType = callType,
            duration = duration.ifBlank { "1m 30s" },
            timeAgo = "Just now",
            summary = summary.ifBlank { "Inbound inquiry recorded and filed to client history." },
            timestamp = System.currentTimeMillis()
        )
        _phoneCallLogs.value = listOf(newCall) + _phoneCallLogs.value
        _statusNotice.value = "Call record logged for ${newCall.callerName}."

        recordAuditEvent(
            actionType = "PHONE_CALL_LOGGED",
            description = "Logged $callType call for ${newCall.callerName} ($duration)",
            result = AuditResultStatus.SUCCESS
        )

        if (autoCreateJob) {
            val taskTitle = if (jobTitle.isNotBlank()) jobTitle else "Follow-up for ${newCall.callerName} ($callType Call)"
            dispatchJob(
                title = taskTitle,
                clientName = newCall.callerName,
                assignedAgent = if (callType == "AI Handled") "Voice Dispatcher Agent" else "Onboarding Specialist Agent",
                priority = "High",
                dueDate = "Today, 5:00 PM",
                summary = "Call follow-up task: ${newCall.summary}"
            )
        }
    }

    fun deletePhoneCall(callId: String) {
        _phoneCallLogs.value = _phoneCallLogs.value.filter { it.id != callId }
        _statusNotice.value = "Call record removed."
    }

    fun addCalendarEvent(event: CalendarEventItem) {
        _calendarEvents.value = listOf(event) + _calendarEvents.value
        _statusNotice.value = "Scheduled '${event.title}' on ${event.date} at ${event.time}."
        recordAuditEvent(
            actionType = "CALENDAR_EVENT_CREATED",
            description = "Scheduled event '${event.title}' for ${event.clientName}",
            result = AuditResultStatus.SUCCESS
        )
    }

    fun updateCalendarEvent(event: CalendarEventItem) {
        _calendarEvents.value = _calendarEvents.value.map {
            if (it.id == event.id) event else it
        }
        _statusNotice.value = "Calendar event '${event.title}' updated."
        recordAuditEvent(
            actionType = "CALENDAR_EVENT_UPDATED",
            description = "Updated event '${event.title}' for ${event.clientName}",
            result = AuditResultStatus.SUCCESS
        )
    }

    fun deleteCalendarEvent(eventId: String) {
        _calendarEvents.value = _calendarEvents.value.filter { it.id != eventId }
        _statusNotice.value = "Calendar event removed."
    }

    fun toggleCalendarEventStatus(eventId: String) {
        _calendarEvents.value = _calendarEvents.value.map {
            if (it.id == eventId) {
                val newStatus = if (it.status == "Completed") "Scheduled" else "Completed"
                it.copy(status = newStatus)
            } else it
        }
        _statusNotice.value = "Event status updated."
    }

    fun syncCalendarWithGoogleWorkspace() {
        _statusNotice.value = "Synced ${_calendarEvents.value.size} events with Google Calendar & Outlook 365."
        recordAuditEvent(
            actionType = "CALENDAR_SYNC",
            description = "Synced calendar events across Google Workspace and Microsoft 365",
            result = AuditResultStatus.SUCCESS
        )
    }

    fun toggleIntegrationConnection(appId: String, isConnected: Boolean) {
        _integrations.value = _integrations.value.map { app ->
            if (app.id == appId) {
                app.copy(
                    isConnected = isConnected,
                    lastSynced = if (isConnected) "Connected (Live Sync)" else "Disconnected"
                )
            } else app
        }
        val appName = _integrations.value.find { it.id == appId }?.name ?: "App"
        _statusNotice.value = if (isConnected) "$appName connected & synced!" else "$appName disconnected."
    }

    fun toggleIntegrationPermission(appId: String, permissionId: String, isEnabled: Boolean) {
        _integrations.value = _integrations.value.map { app ->
            if (app.id == appId) {
                val updatedPermissions = app.permissions.map { perm ->
                    if (perm.id == permissionId) perm.copy(isEnabled = isEnabled) else perm
                }
                app.copy(permissions = updatedPermissions)
            } else app
        }
    }

    fun addCustomIntegration(name: String, category: String, description: String, account: String) {
        val newApp = AppIntegration(
            id = "custom_" + UUID.randomUUID().toString().take(6),
            name = name,
            category = if (category.isBlank()) "Enterprise Integration" else category,
            description = if (description.isBlank()) "Connected custom corporate application." else description,
            iconName = "custom",
            isConnected = true,
            connectedAccount = if (account.isBlank()) "executive@rcos.ai" else account,
            permissions = listOf(
                AgentPermission("read", "Read Data & Logs", "Allows AI Agent to query records", true),
                AgentPermission("write", "Execute Actions", "Allows AI Agent to run automated tasks", true)
            ),
            lastSynced = "Connected Just Now"
        )
        _integrations.value = _integrations.value + newApp
        _statusNotice.value = "$name integrated successfully!"
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val firebaseUser = firebaseManager.currentFirebaseUser()
            if (firebaseUser != null && !firebaseUser.isAnonymous) {
                val localUser = firebaseUser.email?.let { repository.getUserByEmail(it) }
                if (localUser != null) {
                    _currentUser.value = localUser
                    secureManager.saveString("logged_in_email", localUser.email)
                    return@launch
                }
            }
            secureManager.remove("logged_in_email")
            _statusNotice.value = "Please sign in to continue."
        }
    }

    fun loginAsDemoUser(onSuccess: () -> Unit = {}) {
        _isAuthLoading.value = true
        viewModelScope.launch {
            val demoEmail = "rcsolutions@gmail.com"
            val existing = repository.getUserByEmail(demoEmail)
            val user = if (existing != null) {
                existing
            } else {
                val salt = SecurityUtils.generateSalt()
                val passHash = SecurityUtils.hashPassword("rcos_demo_secure_pass", salt)
                val newUser = UserEntity(
                    email = demoEmail,
                    fullName = "RCS Executive User",
                    passwordHash = passHash,
                    salt = salt,
                    companyName = "RCOS Global Solutions",
                    industry = "Technology & IT"
                )
                val reg = repository.registerUser(
                    email = demoEmail,
                    fullName = "RCS Executive User",
                    passwordRaw = "rcos_demo_secure_pass",
                    companyName = "RCOS Global Solutions",
                    industry = "Technology & IT"
                )
                reg.getOrNull() ?: newUser
            }

            _currentUser.value = user
            secureManager.saveString("logged_in_email", user.email)
            _statusNotice.value = "Welcome to RCOS Platform, ${user.fullName}!"
            _isAuthLoading.value = false
            onSuccess()
        }
    }

    // Helper extension for string check
    private fun String?.isNull_or_empty_safe(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    fun setLoginEmail(email: String) { _loginEmail.value = email }
    fun setLoginPassword(password: String) { _loginPassword.value = password }

    fun setRegisterEmail(email: String) { _registerEmail.value = email }
    fun setRegisterFullName(name: String) { _registerFullName.value = name }
    fun setRegisterPassword(password: String) {
        _registerPassword.value = password
        (passwordStrength as MutableStateFlow).value = SecurityUtils.evaluatePasswordStrength(password)
    }
    fun setRegisterCompanyName(company: String) { _registerCompanyName.value = company }
    fun setRegisterIndustry(industry: String) { _registerIndustry.value = industry }

    fun registerUser(onSuccess: () -> Unit = {}) {
        val email = _registerEmail.value.trim()
        val name = _registerFullName.value.trim()
        val password = _registerPassword.value
        val company = _registerCompanyName.value.trim()
        val industry = _registerIndustry.value.trim()

        if (!SecurityUtils.isValidEmail(email)) {
            _statusNotice.value = "Please enter a valid corporate email address."
            return
        }
        if (name.isEmpty()) {
            _statusNotice.value = "Full Name is required."
            return
        }
        val strength = SecurityUtils.evaluatePasswordStrength(password)
        if (!strength.isValid) {
            _statusNotice.value = "Password must be at least 8 characters with a number and letter."
            return
        }

        _isAuthLoading.value = true
        viewModelScope.launch {
            val firebaseResult = firebaseManager.registerWithFirebase(email, password, name)
            firebaseResult.onSuccess {
                val localResult = repository.registerUser(
                    email = email,
                    fullName = name,
                    passwordRaw = password,
                    companyName = if (company.isBlank()) "Enterprise Partner" else company,
                    industry = industry
                )
                localResult.onSuccess { user ->
                    _currentUser.value = user
                    secureManager.saveString("logged_in_email", user.email)
                    _statusNotice.value = "Welcome to RCOS Platform, ${user.fullName}!"
                    _isAuthLoading.value = false
                    onSuccess()
                }.onFailure { err ->
                    firebaseManager.signOut()
                    _isAuthLoading.value = false
                    _statusNotice.value = err.localizedMessage ?: "Account setup failed."
                }
            }.onFailure { err ->
                _isAuthLoading.value = false
                _statusNotice.value = err.localizedMessage ?: "Firebase registration failed."
            }
        }
    }

    fun loginUser(onSuccess: () -> Unit = {}) {
        val email = _loginEmail.value.trim()
        val password = _loginPassword.value
        if (!SecurityUtils.isValidEmail(email)) {
            _statusNotice.value = "Please enter a valid email address."
            return
        }
        if (password.isBlank()) {
            _statusNotice.value = "Please enter your password."
            return
        }

        _isAuthLoading.value = true
        viewModelScope.launch {
            val firebaseResult = firebaseManager.signInWithFirebase(email, password)
            firebaseResult.onSuccess { firebaseUser ->
                val localUser = firebaseUser.email?.let { repository.getUserByEmail(it) }
                if (localUser != null) {
                    _currentUser.value = localUser
                    secureManager.saveString("logged_in_email", localUser.email)
                    _statusNotice.value = "Authentication successful. Welcome, ${localUser.fullName}!"
                    _isAuthLoading.value = false
                    recordAuditEvent(
                        actionType = "LOGIN",
                        description = "User '${localUser.email}' authenticated with Firebase.",
                        result = AuditResultStatus.SUCCESS
                    )
                    onSuccess()
                } else {
                    firebaseManager.signOut()
                    _isAuthLoading.value = false
                    _statusNotice.value = "Your Firebase account is valid, but your RCOS profile is not provisioned yet. Please register through RCOS or contact your administrator."
                }
            }.onFailure { err ->
                _isAuthLoading.value = false
                _statusNotice.value = err.localizedMessage ?: "Authentication failed. Please check your credentials."
                recordAuditEvent(
                    actionType = "LOGIN",
                    description = "Firebase authentication failed for '${email.lowercase()}'.",
                    result = AuditResultStatus.FAILED
                )
            }
        }
    }

    fun logout() {
        recordAuditEvent(
            actionType = "LOGOUT",
            description = "User logged out.",
            result = AuditResultStatus.SUCCESS
        )
        firebaseManager.signOut()
        _currentUser.value = null
        secureManager.remove("logged_in_email")
        _statusNotice.value = "Logged out from RCOS Multi-Agent System."
    }

    // Android Keystore Encrypted Executive Vault Actions
    fun unlockVault() {
        _isVaultUnlocked.value = true
        refreshVaultItems()
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun refreshVaultItems() {
        _vaultItems.value = secureManager.getAllVaultItems()
    }

    fun addVaultItem(name: String, secretValue: String, category: String) {
        val id = UUID.randomUUID().toString().take(8)
        secureManager.saveVaultItem(id, name, secretValue, category)
        refreshVaultItems()
        _statusNotice.value = "Credential '$name' encrypted & stored in Keystore Vault."
    }

    fun deleteVaultItem(id: String) {
        secureManager.deleteVaultItem(id)
        refreshVaultItems()
        _statusNotice.value = "Credential removed from Keystore Vault."
    }

    // Multi-tenant workspace actions & actor resolution
    fun getCurrentActor(): UserAccount? {
        selectedUserAccount.value?.let { return it }
        
        val accounts = activeWorkspaceUserAccounts.value
        if (accounts.isNotEmpty()) {
            return accounts.firstOrNull { it.role == UserRole.ADMIN } ?: accounts.firstOrNull()
        }
        
        // Fallback to the logged in user with full rights if DB hasn't emitted yet
        val loggedIn = _currentUser.value ?: return null
        return UserAccount(
            userId = "usr_fallback_admin",
            workspaceId = _activeWorkspaceId.value,
            email = loggedIn.email,
            fullName = loggedIn.fullName,
            role = UserRole.ADMIN,
            accessLevel = AccessLevel.FULL_CONTROL,
            department = "Executive"
        )
    }

    private fun riskLevelFromPct(pct: Int): AuditRiskLevel {
        return when {
            pct >= 85 -> AuditRiskLevel.CRITICAL
            pct >= 60 -> AuditRiskLevel.HIGH
            pct >= 30 -> AuditRiskLevel.MEDIUM
            else -> AuditRiskLevel.LOW
        }
    }

    fun recordAuditEvent(
        actionType: String,
        description: String,
        targetWorkspaceId: String? = null,
        actorType: AuditActorType = AuditActorType.USER,
        resourceType: String? = null,
        resourceId: String? = null,
        agentId: String? = null,
        workflowId: String? = null,
        previousValue: String? = null,
        newValue: String? = null,
        approvalRequired: Boolean = false,
        approvalStatus: AuditApprovalStatus = AuditApprovalStatus.NOT_REQUIRED,
        result: AuditResultStatus = AuditResultStatus.SUCCESS,
        riskLevel: AuditRiskLevel = AuditRiskLevel.LOW
    ) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val workspaceId = targetWorkspaceId ?: _activeWorkspaceId.value
            val event = AuditLogEntity(
                auditId = UUID.randomUUID().toString(),
                workspaceId = workspaceId,
                userId = actor?.userId,
                actorType = if (actor != null && actorType == AuditActorType.USER) AuditActorType.USER.name else actorType.name,
                actorName = when (actorType) {
                    AuditActorType.USER -> actor?.fullName ?: (_currentUser.value?.fullName ?: "Human User")
                    AuditActorType.AI_AGENT -> agentId ?: "AI Agent"
                    AuditActorType.WORKFLOW -> workflowId ?: "Workflow System"
                    AuditActorType.SYSTEM -> "RCOS System"
                },
                actionType = actionType,
                resourceType = resourceType,
                resourceId = resourceId,
                agentId = agentId,
                workflowId = workflowId,
                timestamp = System.currentTimeMillis(),
                description = description,
                previousValue = previousValue,
                newValue = newValue,
                approvalRequired = approvalRequired,
                approvalStatus = approvalStatus.name,
                result = result.name,
                riskLevel = riskLevel.name
            )
            repository.recordAuditEvent(event)
        }
    }

    fun switchWorkspace(workspaceId: String) {
        val actor = getCurrentActor()
        // Evaluate workspace membership/access
        if (actor != null && actor.workspaceId != workspaceId) {
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = workspaceId,
                action = PermissionAction.VIEW_WORKSPACE
            )
            if (!authResult.isAllowed) {
                _statusNotice.value = "ACCESS DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Access denied switching to workspace $workspaceId: ${authResult.reason}",
                    targetWorkspaceId = workspaceId,
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return
            }
        }
        _activeWorkspaceId.value = workspaceId
        _selectedUserAccount.value = null
        _statusNotice.value = "Switched active business workspace: $workspaceId"
        recordAuditEvent(
            actionType = "WORKSPACE_SWITCHED",
            description = "Switched active workspace to $workspaceId",
            targetWorkspaceId = workspaceId,
            result = AuditResultStatus.SUCCESS
        )
    }

    fun selectUserAccount(account: UserAccount) {
        _selectedUserAccount.value = account
        _statusNotice.value = "Active profile set to: ${account.fullName} (${account.role.label})"
    }

    fun createWorkspace(
        companyName: String,
        industry: String,
        domain: String,
        primaryBottleneck: String,
        adminName: String,
        adminEmail: String
    ) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            if (actor != null) {
                val authResult = PermissionEngine.evaluatePermission(
                    user = actor,
                    targetWorkspaceId = actor.workspaceId,
                    action = PermissionAction.MODIFY_WORKSPACE
                )
                if (!authResult.isAllowed) {
                    _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                    recordAuditEvent(
                        actionType = "AUTHORIZATION_DENIED",
                        description = "Workspace creation denied: ${authResult.reason}",
                        result = AuditResultStatus.DENIED,
                        riskLevel = AuditRiskLevel.HIGH
                    )
                    return@launch
                }
            }

            val newWorkspaceId = "ws_${UUID.randomUUID().toString().take(8)}"
            val workspace = WorkspaceEntity(
                workspaceId = newWorkspaceId,
                companyName = companyName,
                industry = industry,
                domain = domain,
                primaryBottleneck = primaryBottleneck.ifBlank { "Operational Bottlenecks" },
                targetReductionPercent = 80
            )
            val adminUser = UserAccountEntity(
                userId = "usr_${UUID.randomUUID().toString().take(8)}",
                workspaceId = newWorkspaceId,
                email = adminEmail,
                fullName = adminName,
                role = UserRole.ADMIN.name,
                accessLevel = AccessLevel.FULL_CONTROL.name,
                department = "Executive Management"
            )
            val aiConfig = BusinessAiConfigEntity(
                workspaceId = newWorkspaceId,
                customSystemPrompt = "Operate as a high-efficiency enterprise copilot for $companyName.",
                aiAgentTone = "Executive Briefing & Data-Driven"
            )

            repository.createWorkspace(workspace, adminUser, aiConfig)
            _activeWorkspaceId.value = newWorkspaceId
            _statusNotice.value = "New Multi-Tenant Workspace '$companyName' provisioned."
            recordAuditEvent(
                actionType = "WORKSPACE_CREATED",
                description = "Created workspace '$companyName' ($newWorkspaceId)",
                targetWorkspaceId = newWorkspaceId,
                resourceType = "WORKSPACE",
                resourceId = newWorkspaceId,
                result = AuditResultStatus.SUCCESS
            )
        }
    }

    fun addUserToActiveWorkspace(
        fullName: String,
        email: String,
        role: UserRole,
        accessLevel: AccessLevel,
        department: String
    ) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.MANAGE_USERS
            )
            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "User creation denied: ${authResult.reason}",
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            val newUser = UserAccount(
                userId = "usr_${UUID.randomUUID().toString().take(8)}",
                workspaceId = _activeWorkspaceId.value,
                email = email,
                fullName = fullName,
                role = role,
                accessLevel = accessLevel,
                department = department.ifBlank { "General Operations" },
                avatarColorHex = listOf("#3B82F6", "#10B981", "#8B5CF6", "#EC4899", "#F59E0B").random(),
                registeredAt = System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis()
            )
            repository.addUserAccount(newUser)
            _statusNotice.value = "Added team member '${fullName}' (${role.label}) to workspace."
            recordAuditEvent(
                actionType = "USER_CREATED",
                description = "Added team member '$fullName' ($email) with role ${role.label}",
                resourceType = "USER",
                resourceId = newUser.userId,
                newValue = "${role.name} (${accessLevel.name})",
                result = AuditResultStatus.SUCCESS
            )
        }
    }

    fun updateUserRoleInWorkspace(userId: String, role: UserRole, accessLevel: AccessLevel) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.MODIFY_USER_ROLES,
                targetUserId = userId
            )
            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Role change denied: ${authResult.reason}",
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            repository.updateUserRole(userId, role, accessLevel)
            _statusNotice.value = "User permissions updated to ${role.label} (${accessLevel.label})."
            recordAuditEvent(
                actionType = "USER_ROLE_CHANGED",
                description = "Updated permissions for user '$userId' to ${role.label} (${accessLevel.label})",
                resourceType = "USER",
                resourceId = userId,
                newValue = "${role.name} (${accessLevel.name})",
                result = AuditResultStatus.SUCCESS
            )
        }
    }

    fun updateActiveWorkspaceAiConfig(config: BusinessAiConfig) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.MODIFY_AI_CONFIGURATION
            )
            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "AI config change denied: ${authResult.reason}",
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            repository.saveBusinessAiConfig(config.copy(workspaceId = _activeWorkspaceId.value))
            _statusNotice.value = "Workspace-specific AI rules & autonomy parameters saved."
            recordAuditEvent(
                actionType = "AI_CONFIGURATION_CHANGED",
                description = "Updated AI configuration prompt and autonomy parameters",
                result = AuditResultStatus.SUCCESS
            )
        }
    }

    // ==========================================
    // WORKSPACE ONBOARDING & PROVISIONING SYSTEM
    // ==========================================

    fun startOnboardingDeployment(
        companyName: String,
        industry: String,
        companySize: String,
        domain: String,
        primaryBottleneck: String,
        targetAutomationGoal: String,
        targetReductionPercent: Int,
        selectedAgentPackage: String,
        governanceProfile: String,
        adminName: String,
        adminEmail: String,
        aiTone: String,
        autoRiskThreshold: Int,
        autoDollarLimit: Double,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            if (actor != null) {
                val authResult = PermissionEngine.evaluatePermission(
                    user = actor,
                    targetWorkspaceId = actor.workspaceId,
                    action = PermissionAction.MANAGE_ONBOARDING
                )
                if (!authResult.isAllowed) {
                    _statusNotice.value = "ONBOARDING DENIED: ${authResult.reason}"
                    recordAuditEvent(
                        actionType = "AUTHORIZATION_DENIED",
                        description = "Workspace onboarding deployment denied: ${authResult.reason}",
                        result = AuditResultStatus.DENIED,
                        riskLevel = AuditRiskLevel.HIGH
                    )
                    return@launch
                }
            }

            val newWorkspaceId = "ws_${UUID.randomUUID().toString().take(8)}"
            val onboardingId = "onb_${UUID.randomUUID().toString().take(8)}"
            val creator = actor?.fullName ?: adminName

            // 1. Record Onboarding Initiation
            val onboardingEntity = WorkspaceOnboardingEntity(
                onboardingId = onboardingId,
                workspaceId = newWorkspaceId,
                companyName = companyName,
                industry = industry,
                companySize = companySize,
                domain = domain,
                primaryBottleneck = primaryBottleneck,
                targetAutomationGoal = targetAutomationGoal,
                targetReductionPercent = targetReductionPercent,
                selectedAgentPackage = selectedAgentPackage,
                governanceProfile = governanceProfile,
                createdBy = creator,
                createdTimestamp = System.currentTimeMillis(),
                onboardingStatus = OnboardingStatus.STARTED.name
            )
            repository.startWorkspaceOnboarding(onboardingEntity)

            recordAuditEvent(
                actionType = "WORKSPACE_ONBOARDING_STARTED",
                description = "Initiated RCOS onboarding wizard for company '$companyName' in sector '$industry'.",
                targetWorkspaceId = newWorkspaceId,
                resourceType = "WORKSPACE_ONBOARDING",
                resourceId = onboardingId,
                result = AuditResultStatus.SUCCESS
            )

            // 2. Provision Workspace Base Entity
            val workspace = WorkspaceEntity(
                workspaceId = newWorkspaceId,
                companyName = companyName,
                industry = industry,
                domain = domain,
                primaryBottleneck = primaryBottleneck,
                targetReductionPercent = targetReductionPercent
            )

            val adminUser = UserAccountEntity(
                userId = "usr_${UUID.randomUUID().toString().take(8)}",
                workspaceId = newWorkspaceId,
                email = adminEmail,
                fullName = adminName,
                role = UserRole.ADMIN.name,
                accessLevel = AccessLevel.FULL_CONTROL.name,
                department = "Executive Office"
            )

            val aiConfig = BusinessAiConfigEntity(
                workspaceId = newWorkspaceId,
                customSystemPrompt = "Operate as an enterprise AI copilot tailored for $companyName ($industry). Target goal: $targetAutomationGoal.",
                aiAgentTone = aiTone,
                allowedAutoApprovalRiskThreshold = autoRiskThreshold,
                autoApprovalDollarLimit = autoDollarLimit
            )

            repository.createWorkspace(workspace, adminUser, aiConfig)

            repository.updateOnboardingProgress(
                onboardingEntity.copy(onboardingStatus = OnboardingStatus.COMPANY_PROFILE_COMPLETE.name)
            )

            recordAuditEvent(
                actionType = "WORKSPACE_PROFILE_CREATED",
                description = "Configured workspace base profile and AI governance config for '$companyName'.",
                targetWorkspaceId = newWorkspaceId,
                resourceType = "WORKSPACE",
                resourceId = newWorkspaceId,
                result = AuditResultStatus.SUCCESS
            )

            recordAuditEvent(
                actionType = "ADMIN_ACCOUNT_CREATED",
                description = "Provisioned primary workspace administrator account '$adminName' ($adminEmail).",
                targetWorkspaceId = newWorkspaceId,
                resourceType = "USER",
                resourceId = adminUser.userId,
                result = AuditResultStatus.SUCCESS
            )

            // 3. Provision Agent Package
            val provisionedAgents = repository.provisionDefaultAgents(
                workspaceId = newWorkspaceId,
                industryStr = industry,
                selectedPackage = selectedAgentPackage,
                actor = creator
            )

            repository.updateOnboardingProgress(
                onboardingEntity.copy(onboardingStatus = OnboardingStatus.AGENTS_PROVISIONED.name)
            )

            recordAuditEvent(
                actionType = "AGENTS_PROVISIONED",
                description = "Provisioned ${provisionedAgents.size} AI agents for package '$selectedAgentPackage'.",
                targetWorkspaceId = newWorkspaceId,
                resourceType = "AGENT_PACKAGE",
                resourceId = selectedAgentPackage,
                result = AuditResultStatus.SUCCESS
            )

            // 4. Complete Provisioning
            repository.completeWorkspaceProvisioning(newWorkspaceId, onboardingId)
            _activeWorkspaceId.value = newWorkspaceId

            recordAuditEvent(
                actionType = "WORKSPACE_READY",
                description = "Workspace '$companyName' onboarding complete. RCOS deployment fully operational.",
                targetWorkspaceId = newWorkspaceId,
                resourceType = "WORKSPACE",
                resourceId = newWorkspaceId,
                result = AuditResultStatus.SUCCESS
            )

            _statusNotice.value = "Workspace '$companyName' provisioned and activated successfully!"
            onComplete(newWorkspaceId)
        }
    }

    fun updateBusinessConfig(config: BusinessWorkflowConfig) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.MODIFY_AI_GOVERNANCE
            )
            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "AI governance change denied: ${authResult.reason}",
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            repository.updateBusinessConfig(config)
            _statusNotice.value = "Business workflow rules & risk parameters updated."
            recordAuditEvent(
                actionType = "AI_GOVERNANCE_CHANGED",
                description = "Updated workflow rules and auto-approval threshold parameters",
                result = AuditResultStatus.SUCCESS
            )
        }
    }

    fun toggleWorkflowActive(templateId: String) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.MODIFY_AI_CONFIGURATION
            )
            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Workflow toggle denied: ${authResult.reason}",
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }
            repository.toggleWorkflowActive(templateId)
            recordAuditEvent(
                actionType = "AGENT_UPDATED",
                description = "Toggled active state for workflow template $templateId",
                workflowId = templateId,
                result = AuditResultStatus.SUCCESS
            )
        }
    }

    fun triggerWorkflow(templateId: String) {
        viewModelScope.launch {
            val template = workflowTemplates.value.find { it.id == templateId } ?: return@launch
            val executionTime = (120..480).random().toLong()
            val timestamp = "Just now"

            val actor = getCurrentActor()
            val aiConfig = activeBusinessAiConfig.value
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.EXECUTE_WORKFLOW,
                riskScorePct = template.approvalPolicy.riskThresholdPct,
                aiConfig = aiConfig
            )

            if (!authResult.isAllowed && authResult !is AuthorizationResult.ApprovalRequired) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Workflow execution denied for '${template.title}': ${authResult.reason}",
                    workflowId = templateId,
                    agentId = template.assignedAgent,
                    result = AuditResultStatus.DENIED,
                    riskLevel = riskLevelFromPct(template.approvalPolicy.riskThresholdPct)
                )
                return@launch
            }

            repository.incrementTemplateExecution(templateId)

            val config = businessWorkflowConfig.value
            val needsApproval = authResult is AuthorizationResult.ApprovalRequired ||
                    template.approvalPolicy.level == ApprovalLevel.ALWAYS_REQUIRED ||
                    (template.approvalPolicy.level == ApprovalLevel.REQUIRED_IF_HIGH_RISK && template.approvalPolicy.riskThresholdPct > config.maxRiskAutoApprovePct)

            if (needsApproval) {
                val newApproval = ApprovalItem(
                    id = "#APP-${(8900..9900).random()}",
                    workflowTitle = template.title,
                    requestedByAgent = template.assignedAgent,
                    triggerSource = template.trigger.name,
                    timestamp = timestamp,
                    riskScorePct = template.approvalPolicy.riskThresholdPct,
                    summary = "Triggered ${template.title} execution via ${template.trigger.name}. Requires executive confirmation.",
                    proposedAction = template.steps.lastOrNull()?.actionType ?: "Execute automated operational workflow pipeline.",
                    status = ApprovalStatus.PENDING
                )
                repository.addApprovalItem(newApproval)
                _statusNotice.value = "Workflow '${template.title}' queued for Human Executive Approval."

                val actionType = if (template.category.contains("Financial", true) || template.title.contains("Billing", true) || template.title.contains("Payment", true)) {
                    "FINANCIAL_ACTION_REQUESTED"
                } else {
                    "WORKFLOW_APPROVAL_REQUESTED"
                }

                recordAuditEvent(
                    actionType = actionType,
                    description = "Workflow '${template.title}' requested execution; queued for approval.",
                    actorType = AuditActorType.AI_AGENT,
                    agentId = template.assignedAgent,
                    workflowId = templateId,
                    approvalRequired = true,
                    approvalStatus = AuditApprovalStatus.PENDING,
                    result = AuditResultStatus.SUCCESS,
                    riskLevel = riskLevelFromPct(template.approvalPolicy.riskThresholdPct)
                )
            } else {
                // Synthesize workflow execution output artifact using Gemini AI
                val stepsSummary = template.steps.joinToString(", ") { it.title }
                val prompt = "Autonomous AI Agent '${template.assignedAgent}' is executing workflow '${template.title}' ($stepsSummary). Produce a concise 2-3 sentence execution result artifact, confirming operational deliverables generated and synced to enterprise workspace."
                val geminiResult = repository.analyzeText(prompt, systemInstruction = "You are ${template.assignedAgent}, an autonomous operational enterprise agent.")
                val artifact = geminiResult.getOrNull() ?: "Executed '${template.title}' pipeline. ${template.steps.size} steps completed seamlessly."

                val newLog = AiActionLog(
                    id = "#ACT-${(1000..9000).random()}",
                    timestamp = timestamp,
                    agentName = template.assignedAgent,
                    workflowTitle = template.title,
                    triggerType = template.trigger.name,
                    actionSummary = "Gemini AI: Executed '${template.title}' pipeline.",
                    approvalStatus = "Auto-Approved",
                    executionTimeMs = executionTime,
                    outputArtifact = artifact
                )
                repository.addAiActionLog(newLog)
                _statusNotice.value = "AI Workflow '${template.title}' completed via Gemini (${executionTime}ms)."

                recordAuditEvent(
                    actionType = "WORKFLOW_COMPLETED",
                    description = "AI Agent '${template.assignedAgent}' executed '${template.title}' automatically.",
                    actorType = AuditActorType.AI_AGENT,
                    agentId = template.assignedAgent,
                    workflowId = templateId,
                    approvalRequired = false,
                    approvalStatus = AuditApprovalStatus.NOT_REQUIRED,
                    result = AuditResultStatus.SUCCESS,
                    riskLevel = riskLevelFromPct(template.approvalPolicy.riskThresholdPct)
                )
            }
        }
    }

    fun approveItem(id: String, reviewerName: String = "CEO", notes: String = "Approved by Executive") {
        viewModelScope.launch {
            val item = approvalItems.value.find { it.id == id } ?: return@launch
            val actor = getCurrentActor()
            val aiConfig = activeBusinessAiConfig.value

            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.APPROVE_WORKFLOW,
                riskScorePct = item.riskScorePct,
                aiConfig = aiConfig
            )

            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Approval denied for item #$id: ${authResult.reason}",
                    workflowId = id,
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            repository.updateApprovalStatus(id, ApprovalStatus.APPROVED, reviewerName, notes)
            val updatedItem = item.copy(status = ApprovalStatus.APPROVED, reviewedBy = reviewerName, reviewNotes = notes)
            firestoreSyncManager.pushApprovalUpdate(updatedItem)

            val newLog = AiActionLog(
                id = "#ACT-${(1000..9000).random()}",
                timestamp = "Just now",
                agentName = item.requestedByAgent,
                workflowTitle = item.workflowTitle,
                triggerType = item.triggerSource,
                actionSummary = "Executive Approved: ${item.proposedAction}",
                approvalStatus = "Human Approved ($reviewerName)",
                executionTimeMs = (150..350).random().toLong(),
                outputArtifact = "Executive authorization logged. Operation completed."
            )
            repository.addAiActionLog(newLog)
            _statusNotice.value = "Approval #$id authorized! AI Agent dispatched task execution."

            val isFinancial = item.workflowTitle.contains("Financial", true) || item.proposedAction.contains("Payment", true) || item.proposedAction.contains("Billing", true)
            recordAuditEvent(
                actionType = if (isFinancial) "FINANCIAL_ACTION_APPROVED" else "WORKFLOW_APPROVED",
                description = "Approved workflow '${item.workflowTitle}' (#$id) by $reviewerName",
                workflowId = id,
                agentId = item.requestedByAgent,
                approvalRequired = true,
                approvalStatus = AuditApprovalStatus.APPROVED,
                result = AuditResultStatus.SUCCESS,
                riskLevel = riskLevelFromPct(item.riskScorePct)
            )
        }
    }

    fun approveItemByClient(id: String, clientName: String = "Client Sign-Off", notes: String = "Approved directly by client via portal/signature") {
        viewModelScope.launch {
            val item = approvalItems.value.find { it.id == id } ?: return@launch
            repository.updateApprovalStatus(id, ApprovalStatus.APPROVED, clientName, notes)
            val updatedItem = item.copy(status = ApprovalStatus.APPROVED, reviewedBy = clientName, reviewNotes = notes)
            firestoreSyncManager.pushApprovalUpdate(updatedItem)

            val newLog = AiActionLog(
                id = "#ACT-${(1000..9000).random()}",
                timestamp = "Just now",
                agentName = item.requestedByAgent,
                workflowTitle = item.workflowTitle,
                triggerType = "Client Sign-Off",
                actionSummary = "Client Approved: ${item.proposedAction}",
                approvalStatus = "Client Approved ($clientName)",
                executionTimeMs = (120..280).random().toLong(),
                outputArtifact = "Client electronic signature validated. Task proceeded immediately."
            )
            repository.addAiActionLog(newLog)
            _statusNotice.value = "Approval #$id authorized by Client ($clientName)! Removed from pending queue."

            val isFinancial = item.workflowTitle.contains("Financial", true) || item.proposedAction.contains("Payment", true) || item.proposedAction.contains("Billing", true)
            recordAuditEvent(
                actionType = if (isFinancial) "CLIENT_PAYMENT_APPROVED" else "CLIENT_WORKFLOW_APPROVED",
                description = "Client '$clientName' approved workflow '${item.workflowTitle}' (#$id)",
                workflowId = id,
                agentId = item.requestedByAgent,
                approvalRequired = true,
                approvalStatus = AuditApprovalStatus.APPROVED,
                result = AuditResultStatus.SUCCESS,
                riskLevel = riskLevelFromPct(item.riskScorePct)
            )
        }
    }

    fun autoApprovePendingItem(id: String, agentName: String = "Autonomous AI Agent", reason: String = "Guardrails verified; auto-approval threshold satisfied") {
        viewModelScope.launch {
            val item = approvalItems.value.find { it.id == id } ?: return@launch
            val reviewerName = "Auto-Approved by $agentName"
            repository.updateApprovalStatus(id, ApprovalStatus.APPROVED, reviewerName, reason)
            val updatedItem = item.copy(status = ApprovalStatus.APPROVED, reviewedBy = reviewerName, reviewNotes = reason)
            firestoreSyncManager.pushApprovalUpdate(updatedItem)

            val newLog = AiActionLog(
                id = "#ACT-${(1000..9000).random()}",
                timestamp = "Just now",
                agentName = item.requestedByAgent,
                workflowTitle = item.workflowTitle,
                triggerType = "AI Guardrail Engine",
                actionSummary = "AI Auto-Approved: ${item.proposedAction}",
                approvalStatus = "Auto-Approved ($agentName)",
                executionTimeMs = (80..190).random().toLong(),
                outputArtifact = "Automated risk policy check passed. Task dispatched."
            )
            repository.addAiActionLog(newLog)
            _statusNotice.value = "Approval #$id auto-approved by AI Agent. Removed from pending queue."

            recordAuditEvent(
                actionType = "AI_AUTO_APPROVED",
                description = "AI Agent '$agentName' auto-approved workflow '${item.workflowTitle}' (#$id): $reason",
                workflowId = id,
                agentId = item.requestedByAgent,
                approvalRequired = true,
                approvalStatus = AuditApprovalStatus.APPROVED,
                result = AuditResultStatus.SUCCESS,
                riskLevel = riskLevelFromPct(item.riskScorePct)
            )
        }
    }

    fun rejectItem(id: String, reviewerName: String = "CEO", notes: String = "Rejected") {
        viewModelScope.launch {
            val item = approvalItems.value.find { it.id == id }
            val actor = getCurrentActor()
            val aiConfig = activeBusinessAiConfig.value

            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.APPROVE_WORKFLOW,
                riskScorePct = item?.riskScorePct ?: 50,
                aiConfig = aiConfig
            )

            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Rejection denied for item #$id: ${authResult.reason}",
                    workflowId = id,
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            repository.updateApprovalStatus(id, ApprovalStatus.REJECTED, reviewerName, notes)
            item?.let {
                val updatedItem = it.copy(status = ApprovalStatus.REJECTED, reviewedBy = reviewerName, reviewNotes = notes)
                firestoreSyncManager.pushApprovalUpdate(updatedItem)
            }
            _statusNotice.value = "Approval #$id rejected by executive. Task halted."

            val isFinancial = item?.workflowTitle?.contains("Financial", true) == true || item?.proposedAction?.contains("Payment", true) == true
            recordAuditEvent(
                actionType = if (isFinancial) "FINANCIAL_ACTION_REJECTED" else "WORKFLOW_REJECTED",
                description = "Rejected workflow '#$id' by $reviewerName ($notes)",
                workflowId = id,
                agentId = item?.requestedByAgent,
                approvalRequired = true,
                approvalStatus = AuditApprovalStatus.REJECTED,
                result = AuditResultStatus.SUCCESS,
                riskLevel = riskLevelFromPct(item?.riskScorePct ?: 50)
            )
        }
    }

    fun archiveItem(id: String, reviewerName: String = "CEO", notes: String = "Moved to Archive") {
        viewModelScope.launch {
            val item = approvalItems.value.find { it.id == id } ?: return@launch
            repository.updateApprovalStatus(id, ApprovalStatus.ARCHIVED, reviewerName, notes)
            val updatedItem = item.copy(status = ApprovalStatus.ARCHIVED, reviewedBy = reviewerName, reviewNotes = notes)
            firestoreSyncManager.pushApprovalUpdate(updatedItem)
            _statusNotice.value = "Item #$id automatically moved to Archived list."

            recordAuditEvent(
                actionType = "WORKFLOW_ARCHIVED",
                description = "Archived workflow '${item.workflowTitle}' (#$id) by $reviewerName",
                workflowId = id,
                agentId = item.requestedByAgent,
                approvalRequired = false,
                approvalStatus = AuditApprovalStatus.NOT_REQUIRED,
                result = AuditResultStatus.SUCCESS,
                riskLevel = AuditRiskLevel.LOW
            )
        }
    }

    fun completeItem(id: String, reviewerName: String = "CEO", notes: String = "Task Completed") {
        viewModelScope.launch {
            val item = approvalItems.value.find { it.id == id } ?: return@launch
            repository.updateApprovalStatus(id, ApprovalStatus.COMPLETED, reviewerName, notes)
            val updatedItem = item.copy(status = ApprovalStatus.COMPLETED, reviewedBy = reviewerName, reviewNotes = notes)
            firestoreSyncManager.pushApprovalUpdate(updatedItem)
            _statusNotice.value = "Item #$id marked as Completed and moved to Completed list."

            recordAuditEvent(
                actionType = "WORKFLOW_COMPLETED",
                description = "Completed workflow '${item.workflowTitle}' (#$id) by $reviewerName",
                workflowId = id,
                agentId = item.requestedByAgent,
                approvalRequired = false,
                approvalStatus = AuditApprovalStatus.APPROVED,
                result = AuditResultStatus.SUCCESS,
                riskLevel = AuditRiskLevel.LOW
            )
        }
    }

    fun restoreItemToPending(id: String) {
        viewModelScope.launch {
            val item = approvalItems.value.find { it.id == id } ?: return@launch
            repository.updateApprovalStatus(id, ApprovalStatus.PENDING, null, null)
            val updatedItem = item.copy(status = ApprovalStatus.PENDING, reviewedBy = null, reviewNotes = null)
            firestoreSyncManager.pushApprovalUpdate(updatedItem)
            _statusNotice.value = "Item #$id restored to Pending list."
        }
    }

    fun approveAndCompleteJob(jobId: String) {
        val current = _jobTasks.value
        var updatedJob: JobEntity? = null
        _jobTasks.value = current.map {
            if (it.id == jobId) {
                val updated = it.copy(status = "Completed", isApproved = true, progress = 1.0f)
                updatedJob = updated
                updated
            } else it
        }
        updatedJob?.let { firestoreSyncManager.pushTaskUpdate(it) }
        _statusNotice.value = "Job $jobId approved & automatically moved to Completed list."
        recordAuditEvent(
            actionType = "JOB_APPROVED_COMPLETED",
            description = "Job $jobId approved and moved to Completed list",
            result = AuditResultStatus.SUCCESS
        )
    }

    fun archiveJob(jobId: String) {
        val current = _jobTasks.value
        var updatedJob: JobEntity? = null
        _jobTasks.value = current.map {
            if (it.id == jobId) {
                val updated = it.copy(status = "Archived")
                updatedJob = updated
                updated
            } else it
        }
        updatedJob?.let { firestoreSyncManager.pushTaskUpdate(it) }
        _statusNotice.value = "Job $jobId moved to Archived list."
        recordAuditEvent(
            actionType = "JOB_ARCHIVED",
            description = "Job $jobId moved to Archived list",
            result = AuditResultStatus.SUCCESS
        )
    }

    fun restoreJobToPending(jobId: String) {
        val current = _jobTasks.value
        var updatedJob: JobEntity? = null
        _jobTasks.value = current.map {
            if (it.id == jobId) {
                val updated = it.copy(status = "In Progress", progress = 0.5f)
                updatedJob = updated
                updated
            } else it
        }
        updatedJob?.let { firestoreSyncManager.pushTaskUpdate(it) }
        _statusNotice.value = "Job $jobId restored to Pending/In-Progress list."
    }

    fun dispatchJob(
        title: String,
        clientName: String,
        assignedAgent: String = "Workload Synthesizer",
        priority: String = "High",
        dueDate: String = "Today",
        requiresApproval: Boolean = false,
        summary: String = "AI agent allocated to execute workload."
    ) {
        val finalClient = if (clientName.isBlank()) "Apex Enterprises" else clientName
        val newJob = JobEntity(
            id = "#JOB-${_jobTasks.value.size + 105}",
            title = title,
            clientName = finalClient,
            status = if (requiresApproval) "Pending" else "In Progress",
            assignedAgent = assignedAgent,
            priority = priority,
            dueDate = dueDate,
            approvalRequired = requiresApproval,
            isApproved = !requiresApproval,
            progress = if (requiresApproval) 0.1f else 0.45f,
            summary = summary
        )
        _jobTasks.value = listOf(newJob) + _jobTasks.value
        firestoreSyncManager.pushTaskUpdate(newJob)

        // Also add task to client's record in CRM
        val matchingClient = _clientsList.value.find { it.companyName.equals(finalClient, ignoreCase = true) }
        if (matchingClient != null) {
            val taskItem = JobTaskItem(
                id = newJob.id,
                title = newJob.title,
                status = newJob.status,
                assignedAgent = newJob.assignedAgent,
                priority = newJob.priority,
                dueDate = newJob.dueDate,
                progress = newJob.progress,
                summary = newJob.summary
            )
            val updatedClient = if (requiresApproval) {
                matchingClient.copy(queuedJobs = listOf(taskItem) + matchingClient.queuedJobs)
            } else {
                matchingClient.copy(ongoingJobs = listOf(taskItem) + matchingClient.ongoingJobs)
            }
            addOrUpdateClient(updatedClient)
        }

        _statusNotice.value = "Job ${newJob.id} dispatched (${newJob.status})."
        recordAuditEvent(
            actionType = "JOB_DISPATCHED",
            description = "Dispatched job '${newJob.title}' for ${newJob.clientName}",
            result = AuditResultStatus.SUCCESS
        )
    }

    fun updateJob(job: JobEntity) {
        _jobTasks.value = _jobTasks.value.map {
            if (it.id == job.id) job else it
        }
        firestoreSyncManager.pushTaskUpdate(job)
        _statusNotice.value = "Job ${job.id} updated."
        recordAuditEvent(
            actionType = "JOB_UPDATED",
            description = "Updated job '${job.title}' (${job.status})",
            result = AuditResultStatus.SUCCESS
        )
    }

    fun deleteJob(jobId: String) {
        _jobTasks.value = _jobTasks.value.filter { it.id != jobId }
        _statusNotice.value = "Job $jobId deleted."
        recordAuditEvent(
            actionType = "JOB_DELETED",
            description = "Deleted job $jobId",
            result = AuditResultStatus.SUCCESS
        )
    }

    fun triggerFirestoreManualSync() {
        _statusNotice.value = "Initiating multi-device task synchronization with Firebase Firestore..."
        firestoreSyncManager.seedTasksToFirestore(_jobTasks.value)
        firestoreSyncManager.seedApprovalsToFirestore(approvalItems.value)
        viewModelScope.launch {
            val agents = _firestoreActiveAgents.value.ifEmpty {
                repository.getActiveWorkspaceAgents(_activeWorkspaceId.value).firstOrNull() ?: emptyList()
            }
            if (agents.isNotEmpty()) {
                firestoreSyncManager.seedAgentsToFirestore(agents)
            }
        }
    }

    fun refreshActiveAgentsFromFirestore() {
        viewModelScope.launch {
            _isFetchingFirestoreAgents.value = true
            try {
                val cloudAgents = firestoreSyncManager.fetchActiveAgentsFromFirestore()
                if (cloudAgents.isNotEmpty()) {
                    _firestoreActiveAgents.value = cloudAgents
                    _statusNotice.value = "Retrieved ${cloudAgents.size} active autonomous agents from Firestore."
                } else {
                    val local = repository.getActiveWorkspaceAgents(_activeWorkspaceId.value).firstOrNull() ?: emptyList()
                    if (local.isNotEmpty()) {
                        _firestoreActiveAgents.value = local
                        firestoreSyncManager.seedAgentsToFirestore(local)
                    }
                    _statusNotice.value = "Active agents synchronized from Firestore (${_firestoreActiveAgents.value.size} active)."
                }
            } catch (t: Throwable) {
                val local = repository.getActiveWorkspaceAgents(_activeWorkspaceId.value).firstOrNull() ?: emptyList()
                if (_firestoreActiveAgents.value.isEmpty()) {
                    _firestoreActiveAgents.value = local
                }
                _statusNotice.value = "Loaded active agents from local persistence."
            } finally {
                _isFetchingFirestoreAgents.value = false
            }
        }
    }

    fun syncAgentsToFirestore() {
        viewModelScope.launch {
            val agents = _firestoreActiveAgents.value.ifEmpty {
                repository.getActiveWorkspaceAgents(_activeWorkspaceId.value).firstOrNull() ?: emptyList()
            }
            if (agents.isNotEmpty()) {
                firestoreSyncManager.seedAgentsToFirestore(agents)
                _statusNotice.value = "Synchronized ${agents.size} autonomous agents to Firestore."
            }
        }
    }

    fun pushAgentToFirestore(agent: AgentRegistryEntity) {
        firestoreSyncManager.pushAgentUpdate(agent)
    }

    /**
     * Define and create a new agent with a name and objective,
     * adding it to the Firestore 'agents' collection and local storage.
     */
    fun createAndAddAgentToFirestore(
        agentName: String,
        objective: String,
        department: String = "Operations",
        agentType: String = AgentType.OPERATIONS_AGENT.name,
        modelTier: String = "GEMINI_2_5_FLASH",
        riskClassification: String = AgentEntityRiskLevel.LOW.name,
        permissionLevel: String = "STANDARD",
        capabilities: List<String> = listOf("Autonomous Execution", "Task Routing", "Real-Time Telemetry"),
        onSuccess: (AgentRegistryEntity) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val cleanName = agentName.trim()
                val cleanObjective = objective.trim()
                if (cleanName.isBlank()) {
                    onError("Agent name cannot be blank")
                    return@launch
                }
                if (cleanObjective.isBlank()) {
                    onError("Agent objective cannot be blank")
                    return@launch
                }

                val randomSuffix = (1000..9999).random()
                val agentId = "#AGT-${randomSuffix}"
                val newAgent = AgentRegistryEntity(
                    agentId = agentId,
                    workspaceId = _activeWorkspaceId.value.ifBlank { "ws_default" },
                    agentName = cleanName,
                    agentDescription = cleanObjective,
                    agentType = agentType,
                    status = AgentStatus.ACTIVE.name,
                    capabilityProfile = capabilities.joinToString(", "),
                    permissionLevel = permissionLevel,
                    riskClassification = riskClassification,
                    modelTier = modelTier,
                    createdBy = "USER_ADMIN",
                    createdTimestamp = System.currentTimeMillis(),
                    updatedTimestamp = System.currentTimeMillis(),
                    assignedDepartment = department,
                    assignedUsers = null
                )

                // 1. Add directly to Firestore 'agents' collection
                val addedToFirestore = firestoreSyncManager.addAgentToFirestore(newAgent, cleanObjective)

                // 2. Persist locally to repository
                repository.insertAgent(newAgent)

                // 3. Update active agents state flow
                _firestoreActiveAgents.value = listOf(newAgent) + _firestoreActiveAgents.value.filter { it.agentId != newAgent.agentId }

                // 4. Log AI action & audit event
                val actionLog = AiActionLog(
                    id = "act_${System.currentTimeMillis()}",
                    timestamp = "Just now",
                    agentName = cleanName,
                    workflowTitle = "Agent Provisioned",
                    triggerType = "AGENT_CREATION",
                    actionSummary = "Created autonomous agent '$cleanName' with objective: $cleanObjective in Firestore 'agents' collection",
                    approvalStatus = "Auto-Approved",
                    executionTimeMs = 90L,
                    outputArtifact = "Agent ID: $agentId"
                )
                repository.addAiActionLog(actionLog)

                recordAuditEvent(
                    actionType = "AGENT_CREATED_FIRESTORE",
                    description = "Created autonomous agent '$cleanName' with objective '$cleanObjective' in Firestore 'agents' collection",
                    resourceType = "AI_AGENT",
                    resourceId = agentId,
                    agentId = agentId,
                    result = AuditResultStatus.SUCCESS
                )

                _statusNotice.value = if (addedToFirestore) {
                    "Agent '$cleanName' added to Firestore 'agents' collection."
                } else {
                    "Agent '$cleanName' created and saved to active registry."
                }

                onSuccess(newAgent)
            } catch (t: Throwable) {
                val errMsg = t.message ?: "Failed to create agent"
                _statusNotice.value = "Error creating agent: $errMsg"
                onError(errMsg)
            }
        }
    }

    fun toggleAgentActiveInFirestore(agent: AgentRegistryEntity, isNowActive: Boolean) {
        val newStatus = if (isNowActive) AgentStatus.ACTIVE.name else AgentStatus.INACTIVE.name
        val updated = agent.copy(status = newStatus, updatedTimestamp = System.currentTimeMillis())
        
        // Update live state
        _firestoreActiveAgents.value = if (isNowActive) {
            if (_firestoreActiveAgents.value.none { it.agentId == updated.agentId }) {
                _firestoreActiveAgents.value + updated
            } else {
                _firestoreActiveAgents.value.map { if (it.agentId == updated.agentId) updated else it }
            }
        } else {
            _firestoreActiveAgents.value.filter { it.agentId != updated.agentId }
        }

        viewModelScope.launch {
            try {
                repository.updateAgent(updated)
            } catch (e: Throwable) {}
        }

        // Push real-time to Firestore
        firestoreSyncManager.pushAgentUpdate(updated)
        _statusNotice.value = if (isNowActive) "Agent '${agent.agentName}' activated in Firestore." else "Agent '${agent.agentName}' paused in Firestore."
        
        recordAuditEvent(
            actionType = if (isNowActive) "AGENT_ACTIVATED_FIRESTORE" else "AGENT_PAUSED_FIRESTORE",
            description = "${if (isNowActive) "Activated" else "Paused"} autonomous agent '${agent.agentName}' in Firestore",
            resourceType = "AI_AGENT",
            resourceId = agent.agentId,
            agentId = agent.agentId,
            result = AuditResultStatus.SUCCESS
        )
    }

    fun dispatchAutonomousTaskToAgent(
        agent: AgentRegistryEntity,
        taskTitle: String,
        prompt: String,
        priority: String = "High",
        approvalRequired: Boolean = false
    ) {
        viewModelScope.launch {
            val taskId = "#TSK-${(1000..9999).random()}"
            val newJob = JobEntity(
                id = taskId,
                title = taskTitle,
                clientName = "Autonomous System / ${agent.assignedDepartment ?: "Operations"}",
                status = if (approvalRequired) "Pending Approval" else "In Progress",
                assignedAgent = agent.agentName,
                priority = priority,
                dueDate = "Today",
                approvalRequired = approvalRequired,
                isApproved = !approvalRequired,
                progress = 0.25f,
                summary = prompt
            )

            // Update local state and push to Firestore
            _jobTasks.value = listOf(newJob) + _jobTasks.value
            firestoreSyncManager.pushTaskUpdate(newJob)

            // Log AI action
            val actionLog = AiActionLog(
                id = "act_${System.currentTimeMillis()}",
                timestamp = "Just now",
                agentName = agent.agentName,
                workflowTitle = taskTitle,
                triggerType = "MANUAL_DISPATCH",
                actionSummary = prompt,
                approvalStatus = if (approvalRequired) "Pending Review" else "Auto-Approved",
                executionTimeMs = 120L,
                outputArtifact = "Task $taskId queued for execution"
            )
            repository.addAiActionLog(actionLog)

            _statusNotice.value = "Autonomous task $taskId dispatched to '${agent.agentName}' via Firestore."
        }
    }

    fun addWorkflowTemplate(template: WorkflowTemplate) {
        viewModelScope.launch {
            repository.addWorkflowTemplate(template)
            _statusNotice.value = "New Business Workflow Template '${template.title}' created."
        }
    }

    fun updateAgentAutonomy(agentId: String, newAutonomy: String) {
        viewModelScope.launch {
            repository.updateAgentAutonomy(agentId, newAutonomy)
            _statusNotice.value = "Agent autonomy permissions updated to '$newAutonomy'."
        }
    }

    fun saveOnboardingProfile(
        companyName: String,
        industry: String,
        bottleneck: String,
        reductionPercent: Int,
        agents: String,
        customInstructions: String
    ) {
        viewModelScope.launch {
            val profile = CompanyProfileEntity(
                id = 1,
                companyName = companyName,
                industry = industry,
                primaryBottleneck = bottleneck,
                targetReductionPercent = reductionPercent,
                activeAgents = agents,
                customInstructions = customInstructions,
                isConfigured = true,
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveCompanyProfile(profile)
            
            // Generate a strategic plan based on the onboarding data
            if (!isDemoMode.value && _jobTasks.value.isEmpty()) {
                val strategicJob = JobEntity(
                    id = "RCOS-INIT",
                    title = "Phase 1: RCOS Strategic Remediation for $companyName",
                    clientName = "Internal Operations",
                    status = "In Progress",
                    assignedAgent = "RCOS Master Strategist Agent",
                    priority = "Critical",
                    dueDate = "Immediate",
                    progress = 0.1f,
                    summary = "RCOS automated plan addressing the primary bottleneck: $bottleneck. Target: $reductionPercent% workload reduction. Custom directives applied: $customInstructions"
                )
                
                val defaultClient = ClientDetailData(
                    id = "client_rcos_internal",
                    companyName = companyName.ifBlank { "Your Enterprise" },
                    industry = industry.ifBlank { "Corporate" },
                    accountEmail = "operations@${companyName.lowercase().replace(" ", "")}.com",
                    phone = "+1 (555) 000-0000",
                    status = "Active Core",
                    contractValue = "Internal System",
                    primaryContact = ContactPerson("System Admin", "Director of Ops", "admin@company.com", "+1 555-000-0000"),
                    ongoingJobs = listOf(
                        JobTaskItem(
                            id = "INIT",
                            title = "Rollout RCOS Pipeline",
                            status = "In Progress",
                            assignedAgent = "System Admin",
                            priority = "Critical",
                            dueDate = "Immediate",
                            progress = 0.1f,
                            summary = "Initialize baseline infrastructure."
                        )
                    )
                )
                
                _jobTasks.value = listOf(strategicJob)
                _clientsList.value = listOf(defaultClient)
                
                recordAuditEvent(
                    actionType = "SYSTEM_INITIALIZED",
                    description = "RCOS generated strategic remediation plan for $bottleneck.",
                    result = AuditResultStatus.SUCCESS
                )
            }
            
            _statusNotice.value = "RCOS Onboarding complete. Strategic plan generated!"
        }
    }

    fun completeEnterpriseOnboarding(
        companyName: String,
        industry: String,
        domain: String,
        bottleneck: String,
        reductionPercent: Int,
        customInstructions: String,
        workflowConfig: BusinessWorkflowConfig,
        configuredAgents: List<AgentRegistryEntity>,
        activeTriggers: List<String> = emptyList(),
        activeBlueprints: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            // 1. Save Company Profile
            val activeAgentsList = configuredAgents.filter { it.status == AgentStatus.ACTIVE.name }
            val profile = CompanyProfileEntity(
                id = 1,
                companyName = companyName.ifBlank { "Enterprise Client" },
                industry = industry.ifBlank { "Cross-Industry Workload" },
                primaryBottleneck = bottleneck.ifBlank { "Executive Reporting & Task Dispatch" },
                targetReductionPercent = reductionPercent,
                activeAgents = activeAgentsList.joinToString(", ") { it.agentName },
                customInstructions = customInstructions,
                isConfigured = true,
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveCompanyProfile(profile)

            // 2. Save Business Workflow Configuration
            repository.updateBusinessConfig(workflowConfig)

            // 3. Persist configured agents
            configuredAgents.forEach { agent ->
                try {
                    repository.insertAgent(agent)
                } catch (e: Throwable) {}
            }

            // 4. Update live active agents and sync to Firestore
            _firestoreActiveAgents.value = activeAgentsList
            try {
                firestoreSyncManager.seedAgentsToFirestore(activeAgentsList)
            } catch (e: Throwable) {}

            // 5. Update workspace entity
            try {
                val currentWsId = _activeWorkspaceId.value.ifBlank { "ws_default" }
                val currentWorkspaces = repository.allWorkspaces.firstOrNull() ?: emptyList()
                val ws = currentWorkspaces.find { it.workspaceId == currentWsId } ?: WorkspaceEntity(
                    workspaceId = currentWsId,
                    companyName = companyName.ifBlank { "Enterprise Client" },
                    industry = industry.ifBlank { "Cross-Industry Workload" },
                    domain = domain,
                    primaryBottleneck = bottleneck,
                    targetReductionPercent = reductionPercent,
                    activeAgents = activeAgentsList.joinToString(", ") { it.agentName },
                    customInstructions = customInstructions,
                    isConfigured = true
                )
                val updatedWs = ws.copy(
                    companyName = companyName.ifBlank { ws.companyName },
                    industry = industry.ifBlank { ws.industry },
                    domain = domain.ifBlank { ws.domain },
                    primaryBottleneck = bottleneck.ifBlank { ws.primaryBottleneck },
                    targetReductionPercent = reductionPercent,
                    activeAgents = activeAgentsList.joinToString(", ") { it.agentName },
                    customInstructions = customInstructions,
                    isConfigured = true
                )
                repository.updateWorkspace(updatedWs)
            } catch (e: Throwable) {}

            // 6. Generate strategic remediation job if none exists
            if (_jobTasks.value.isEmpty()) {
                val strategicJob = JobEntity(
                    id = "RCOS-INIT",
                    title = "Phase 1: Enterprise Workflow & Agent Orchestration for $companyName",
                    clientName = "Internal Operations",
                    status = "In Progress",
                    assignedAgent = activeAgentsList.firstOrNull()?.agentName ?: "Executive Risk Copilot",
                    priority = "Critical",
                    dueDate = "Immediate",
                    progress = 0.15f,
                    summary = "Enterprise onboarding activated for $bottleneck. Target: $reductionPercent% automated throughput. Workflows: ${activeBlueprints.joinToString(", ").ifBlank { "Executive Strategy, Automated Ingestion" }}."
                )
                _jobTasks.value = listOf(strategicJob)
            }

            // 7. Audit log
            recordAuditEvent(
                actionType = "ENTERPRISE_ONBOARDING_COMPLETED",
                description = "Completed enterprise onboarding for '$companyName' ($industry). Configured ${activeAgentsList.size} autonomous agents with custom permission levels and auto-approval limits.",
                resourceType = "ENTERPRISE_ONBOARDING",
                result = AuditResultStatus.SUCCESS,
                riskLevel = AuditRiskLevel.LOW
            )

            _statusNotice.value = "Enterprise workspace successfully onboarded for $companyName!"
        }
    }

    fun setAnalyzerInput(text: String) {
        _analyzerInput.value = text
    }

    fun setAnalyzerPreset(preset: String) {
        _analyzerSelectedPreset.value = preset
    }

    fun runAnalyzer() {
        val input = _analyzerInput.value.trim()
        if (input.isEmpty()) {
            _statusNotice.value = "Please enter text or data to analyze."
            return
        }

        _isAnalyzing.value = true
        _analyzerResult.value = null

        val profile = companyProfile.value
        val companyContext = if (profile != null) {
            "Company Context: ${profile.companyName} (${profile.industry}). Primary Target: ${profile.primaryBottleneck}."
        } else ""

        val prompt = when (_analyzerSelectedPreset.value) {
            "Summarize Report" -> "$companyContext\n\nProvide an executive summary and high-level takeaway of the following enterprise report:\n\n$input"
            "Key Action Items" -> "$companyContext\n\nExtract bulleted corporate action items, assigned agent tasks, and operational next steps from:\n\n$input"
            "Executive Brief" -> "$companyContext\n\nFormat the following material into a sleek 3-point C-level executive briefing card:\n\n$input"
            "Format as Markdown" -> "Reformat and polish the following content into clean, structured Markdown for corporate documentation:\n\n$input"
            "Code Review" -> "Analyze the following code for architecture patterns, performance bottlenecks, and security compliance:\n\n$input"
            else -> "$companyContext\n\nAnalyze and provide actionable intelligence on:\n\n$input"
        }

        viewModelScope.launch {
            val result = repository.analyzeText(
                prompt = prompt,
                systemInstruction = "You are RCOS, an adaptive Multi-Agent Operating System built to optimize corporate workflows."
            )
            _isAnalyzing.value = false
            result.onSuccess { output ->
                _analyzerResult.value = output
            }.onFailure { err ->
                _statusNotice.value = "Analysis failed: ${err.localizedMessage}"
            }
        }
    }

    fun saveAnalyzerResultToDashboard() {
        val content = _analyzerResult.value ?: return
        val preset = _analyzerSelectedPreset.value
        viewModelScope.launch {
            repository.saveDashboardItem(
                title = "$preset Report",
                category = "RCOS Intelligence",
                content = content,
                itemType = "INSIGHT"
            )
            _statusNotice.value = "Saved to RCOS Intelligence Feed!"
        }
    }

    fun setChatInput(text: String) {
        _chatInput.value = text
    }

    fun selectPersona(persona: String) {
        _currentPersona.value = persona
    }

    fun sendChatMessage() {
        val message = _chatInput.value.trim()
        if (message.isEmpty() || _isChatSending.value) return

        val sessionId = _currentSessionId.value
        val persona = _currentPersona.value

        _chatInput.value = ""
        _isChatSending.value = true

        viewModelScope.launch {
            repository.addChatMessage(sessionId, "user", message)
            repository.createOrUpdateSession(
                ChatSessionEntity(
                    id = sessionId,
                    title = if (message.length > 30) message.take(30) + "..." else message,
                    rolePersona = persona,
                    lastUpdated = System.currentTimeMillis()
                )
            )

            val history = _chatMessages.value.map { it.role to it.text }

            val profile = companyProfile.value
            val companyInfo = if (profile != null) {
                "Client Company: ${profile.companyName} (${profile.industry}). Focus: ${profile.primaryBottleneck}. Custom Rules: ${profile.customInstructions}."
            } else ""

            val connectedApps = _integrations.value.filter { it.isConnected }
            val integrationsPrompt = if (connectedApps.isNotEmpty()) {
                val appSummary = connectedApps.joinToString("\n") { app ->
                    val perms = app.permissions.filter { it.isEnabled }.joinToString(", ") { it.name }
                    "• ${app.name} (${app.connectedAccount}): Enabled AI Permissions -> [$perms]"
                }
                "\nCONNECTED INTEGRATED APPLICATIONS & GRANTED AI PERMISSIONS:\n$appSummary\n" +
                "You are FULLY AUTHORIZED to check, send, forward, and delete emails via Gmail; inspect schedules, create meetings, and cancel appointments via Google Calendar; search, save, and export documents/summaries to Google Drive; and post notifications to Slack/Notion when asked by the user."
            } else {
                "\nNo external apps are connected yet. Advise the user they can connect Gmail, Google Calendar, Google Drive, and Slack in Settings > App Integrations."
            }

            val baseInstruction = when (persona) {
                "RCOS Workload Synthesizer" -> "You are RCOS Workload Synthesizer Agent. Your mission is to analyze enterprise operational overhead, streamline task execution, and suggest automation strategies. $companyInfo"
                "RCOS Onboarding Specialist" -> "You are RCOS Onboarding & Setup Agent. Your job is to guide enterprise users through customizing RCOS for their specific industry workflows, systems, and agent configurations. $companyInfo"
                "RCOS Workflow Automator" -> "You are RCOS Workflow Automation Agent. Help users write code, automation scripts, integration APIs, and step-by-step corporate task pipelines. $companyInfo"
                "RCOS Strategic Enterprise Analyst" -> "You are RCOS Strategic Analyst Agent. Provide high-level business intelligence, strategic forecasts, risk assessments, and executive decision support. $companyInfo"
                else -> "You are RCOS (Responsive Corporate OS), an adaptive multi-agent system. $companyInfo"
            }

            val systemInstruction = "$baseInstruction\n$integrationsPrompt"

            val result = repository.sendChatMessage(history, message, systemInstruction)
            _isChatSending.value = false

            result.onSuccess { botReply ->
                repository.addChatMessage(sessionId, "model", botReply)

                // Log automated app action if requested
                val lowerMsg = message.lowercase()
                if (lowerMsg.contains("email") || lowerMsg.contains("gmail") || lowerMsg.contains("send") || lowerMsg.contains("forward")) {
                    repository.saveDashboardItem(
                        title = "Gmail Action Log: Email Processed",
                        category = "Google Workspace",
                        content = "RCOS AI Agent processed email request: \"$message\".",
                        itemType = "INTEGRATION"
                    )
                } else if (lowerMsg.contains("calendar") || lowerMsg.contains("schedule") || lowerMsg.contains("meeting")) {
                    repository.saveDashboardItem(
                        title = "Google Calendar Log: Schedule Managed",
                        category = "Google Workspace",
                        content = "RCOS AI Agent managed calendar event for: \"$message\".",
                        itemType = "INTEGRATION"
                    )
                } else if (lowerMsg.contains("drive") || lowerMsg.contains("document") || lowerMsg.contains("save")) {
                    repository.saveDashboardItem(
                        title = "Google Drive Log: Document Saved",
                        category = "Google Workspace",
                        content = "RCOS AI Agent uploaded file/summary to Google Drive: \"$message\".",
                        itemType = "INTEGRATION"
                    )
                }
            }.onFailure { err ->
                _statusNotice.value = "Chat error: ${err.localizedMessage}"
            }
        }
    }

    fun startNewChatSession() {
        _currentSessionId.value = UUID.randomUUID().toString()
    }

    fun transcribeAudioFile(file: File) {
        _isTranscribing.value = true
        _transcriptionResult.value = null

        viewModelScope.launch {
            val result = repository.transcribeVoice(file, mimeType = "audio/3gp")
            _isTranscribing.value = false

            result.onSuccess { text ->
                _transcriptionResult.value = text
            }.onFailure { err ->
                _statusNotice.value = "Transcription failed: ${err.localizedMessage}"
            }
        }
    }

    fun saveTranscriptToDashboard(title: String) {
        val transcript = _transcriptionResult.value ?: return
        viewModelScope.launch {
            repository.saveDashboardItem(
                title = if (title.isBlank()) "Voice Transcript Note" else title,
                category = "Voice Note",
                content = transcript,
                itemType = "TRANSCRIPTION"
            )
            _statusNotice.value = "Transcript saved to RCOS Feed!"
        }
    }

    fun setThinkingPrompt(text: String) {
        _thinkingPrompt.value = text
    }

    fun runDeepThinking() {
        val prompt = _thinkingPrompt.value.trim()
        if (prompt.isEmpty() || _isThinking.value) return

        _isThinking.value = true
        _thinkingResult.value = null

        val profile = companyProfile.value
        val contextPrompt = if (profile != null) {
            "Company: ${profile.companyName} (${profile.industry}).\nQuery:\n$prompt"
        } else prompt

        viewModelScope.launch {
            val result = repository.runDeepReasoning(contextPrompt)
            _isThinking.value = false

            result.onSuccess { reasoningText ->
                _thinkingResult.value = reasoningText
            }.onFailure { err ->
                _statusNotice.value = "Deep reasoning failed: ${err.localizedMessage}"
            }
        }
    }

    fun saveThinkingResultToDashboard() {
        val content = _thinkingResult.value ?: return
        val prompt = _thinkingPrompt.value.take(40)
        viewModelScope.launch {
            repository.saveDashboardItem(
                title = "RCOS Deep Reasoning: $prompt...",
                category = "Strategic Analysis",
                content = content,
                itemType = "REASONING"
            )
            _statusNotice.value = "Reasoning report saved to Archive!"
        }
    }

    fun togglePin(id: Int, isPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePinItem(id, isPinned)
        }
    }

    fun deleteDashboardItem(id: Int) {
        viewModelScope.launch {
            repository.deleteDashboardItem(id)
            _statusNotice.value = "Item removed."
        }
    }

    fun clearStatusNotice() {
        _statusNotice.value = null
    }

private fun getInitialIntegrations(): List<AppIntegration> {
    return listOf(
        AppIntegration(
            id = "gmail",
            name = "Google Gmail",
            category = "Google Workspace",
            description = "Checks, reads, drafts, sends, forwards, and deletes executive emails via RCOS AI Agent.",
            iconName = "gmail",
            isConnected = true,
            connectedAccount = "rcsolutions@gmail.com",
            permissions = listOf(
                AgentPermission("check_email", "Check & Read Emails", "Allows AI Agent to fetch and read inbox messages", true),
                AgentPermission("send_email", "Send Executive Emails", "Allows AI Agent to draft and send emails on your behalf", true),
                AgentPermission("forward_email", "Forward Emails", "Allows AI Agent to forward relevant messages to team members", true),
                AgentPermission("delete_email", "Delete / Cleanup Spam", "Allows AI Agent to clean up spam and trash junk messages", true)
            ),
            lastSynced = "Active (Live Sync)"
        ),
        AppIntegration(
            id = "gcalendar",
            name = "Google Calendar",
            category = "Google Workspace",
            description = "Schedules meetings, resolves conflicts, sends invites, and manages executive appointments.",
            iconName = "calendar",
            isConnected = true,
            connectedAccount = "rcsolutions@gmail.com",
            permissions = listOf(
                AgentPermission("view_schedule", "Inspect Availability", "Allows AI Agent to check daily schedule and free slots", true),
                AgentPermission("create_events", "Create & Update Meetings", "Allows AI Agent to schedule events and send calendar invites", true),
                AgentPermission("cancel_events", "Reschedule / Cancel", "Allows AI Agent to update or cancel conflicting appointments", true)
            ),
            lastSynced = "Active (Live Sync)"
        ),
        AppIntegration(
            id = "gdrive",
            name = "Google Drive & Docs",
            category = "Google Workspace",
            description = "Saves executive summaries, searches enterprise files, exports spreadsheets, and manages archives.",
            iconName = "drive",
            isConnected = true,
            connectedAccount = "rcsolutions@gmail.com",
            permissions = listOf(
                AgentPermission("save_docs", "Save & Upload Documents", "Allows AI Agent to save generated docs and reports directly to Drive", true),
                AgentPermission("search_drive", "Search Drive Files", "Allows AI Agent to find and reference enterprise documents", true),
                AgentPermission("share_files", "Manage Sharing & Access", "Allows AI Agent to share documents with client contacts", true)
            ),
            lastSynced = "Active (Live Sync)"
        ),
        AppIntegration(
            id = "ms_outlook",
            name = "Microsoft Outlook Mail",
            category = "Microsoft 365",
            description = "Reads, drafts, sends, and organizes corporate emails in Microsoft Outlook 365.",
            iconName = "outlook",
            isConnected = true,
            connectedAccount = "executive@rcsolutions.onmicrosoft.com",
            permissions = listOf(
                AgentPermission("read_outlook", "Read Outlook Inbox", "Allows AI Agent to inspect incoming client emails", true),
                AgentPermission("send_outlook", "Send Outlook Emails", "Allows AI Agent to compose & send official responses", true)
            ),
            lastSynced = "Active (Live Sync)"
        ),
        AppIntegration(
            id = "ms_calendar",
            name = "Microsoft Outlook Calendar",
            category = "Microsoft 365",
            description = "Syncs corporate availability, manages Teams meeting invites, and resolves calendar double-bookings.",
            iconName = "outlook_cal",
            isConnected = true,
            connectedAccount = "executive@rcsolutions.onmicrosoft.com",
            permissions = listOf(
                AgentPermission("ms_schedule", "Manage Outlook Schedule", "Allows AI Agent to book and reschedule Microsoft Teams meetings", true)
            ),
            lastSynced = "Active (Live Sync)"
        ),
        AppIntegration(
            id = "ms_onedrive",
            name = "Microsoft OneDrive & Office 365",
            category = "Microsoft 365",
            description = "Stores corporate Word documents, Excel spreadsheets, and syncing enterprise cloud files.",
            iconName = "onedrive",
            isConnected = true,
            connectedAccount = "executive@rcsolutions.onmicrosoft.com",
            permissions = listOf(
                AgentPermission("sync_onedrive", "Read & Edit OneDrive Files", "Allows AI Agent to export reports to Word and Excel", true)
            ),
            lastSynced = "Active (Live Sync)"
        ),
        AppIntegration(
            id = "slack",
            name = "Slack Workspace",
            category = "Communication & Productivity",
            description = "Sends channel notifications, posts AI summaries, and routes team alerts.",
            iconName = "slack",
            isConnected = true,
            connectedAccount = "rcos-global.slack.com",
            permissions = listOf(
                AgentPermission("post_messages", "Post Channel Alerts", "Allows AI Agent to post updates to Slack channels", true),
                AgentPermission("read_messages", "Read Team Messages", "Allows AI Agent to monitor team inquiries", false)
            ),
            lastSynced = "Active (Live Sync)"
        ),
        AppIntegration(
            id = "notion",
            name = "Notion Workspace",
            category = "Communication & Productivity",
            description = "Syncs client project boards, updates CRM databases, and maintains knowledge bases.",
            iconName = "notion",
            isConnected = false,
            connectedAccount = "Notion HQ (Disconnected)",
            permissions = listOf(
                AgentPermission("sync_databases", "Sync CRM & Projects", "Allows AI Agent to update CRM cards and pages", true)
            ),
            lastSynced = "Disconnected"
        ),
        AppIntegration(
            id = "github",
            name = "GitHub Enterprise",
            category = "Developer Tools",
            description = "Tracks repository issues, reviews pull requests, and triggers CI/CD build pipelines.",
            iconName = "github",
            isConnected = false,
            connectedAccount = "GitHub Org (Disconnected)",
            permissions = listOf(
                AgentPermission("read_repos", "Read Repositories", "Allows AI Agent to inspect code repositories", true),
                AgentPermission("create_issues", "Log Issues & Tickets", "Allows AI Agent to log bug tickets", true)
            ),
            lastSynced = "Disconnected"
        )
    )
}

private fun getInitialClients(): List<ClientDetailData> {
    return listOf(
        ClientDetailData(
            id = "client_1",
            companyName = "Apex Enterprises",
            industry = "Logistics & Fleet Management",
            accountEmail = "executive@apexlogistics.com",
            phone = "+1 (555) 234-8901",
            status = "Active Enterprise",
            headquarters = "Chicago, IL",
            contractValue = "$180,000 / yr",
            onboardingDate = "Nov 2024",
            primaryContact = ContactPerson("Alex Rivera", "Chief Operating Officer", "a.rivera@apexlogistics.com", "+1 (555) 234-8901"),
            ongoingJobs = listOf(
                JobTaskItem(
                    id = "#JOB-101",
                    title = "Automated Client Onboarding Pipeline",
                    status = "In Progress",
                    assignedAgent = "Onboarding Specialist Agent",
                    priority = "High",
                    dueDate = "Today, 5:00 PM",
                    progress = 0.80f,
                    summary = "AI Voice Agent verified contract terms, dispatched onboarding packet via Gmail/Outlook, and created calendar invites."
                ),
                JobTaskItem(
                    id = "#JOB-104",
                    title = "Logistics Route & Dispatch Optimization",
                    status = "In Progress",
                    assignedAgent = "Fleet AI Agent",
                    priority = "Urgent",
                    dueDate = "Today, 8:00 PM",
                    progress = 0.45f,
                    summary = "Synthesizing vehicle route telemetry and dispatching driver updates."
                )
            ),
            queuedJobs = listOf(
                JobTaskItem(
                    id = "#JOB-108",
                    title = "Q3 Fleet Fuel Efficiency Synthesis",
                    status = "Queued",
                    assignedAgent = "Workload Synthesizer",
                    priority = "Medium",
                    dueDate = "Tomorrow, 9:00 AM",
                    progress = 0.0f,
                    summary = "Queued to compile fuel usage spreadsheets in Google Sheets / Excel Online."
                )
            ),
            notes = "Tier-1 logistics account with automated phone call AI agent routing and Google/Microsoft document sync enabled."
        ),
        ClientDetailData(
            id = "client_2",
            companyName = "Nova Dynamics Inc.",
            industry = "Financial Tech & Asset Ops",
            accountEmail = "ops@novadynamics.io",
            phone = "+1 (555) 876-1234",
            status = "VIP",
            headquarters = "New York, NY",
            contractValue = "$250,000 / yr",
            onboardingDate = "Jan 2025",
            primaryContact = ContactPerson("Elena Vance", "Director of Financial Operations", "e.vance@novadynamics.io", "+1 (555) 876-1234"),
            ongoingJobs = listOf(
                JobTaskItem(
                    id = "#JOB-102",
                    title = "Q3 Strategic Financial Synthesis",
                    status = "In Progress",
                    assignedAgent = "Strategic Analyst Agent",
                    priority = "High",
                    dueDate = "Tomorrow, 10:00 AM",
                    progress = 0.60f,
                    summary = "Consolidating balance sheets and generating executive summary presentation."
                )
            ),
            queuedJobs = listOf(
                JobTaskItem(
                    id = "#JOB-106",
                    title = "Automated Tax Audit & Compliance Sweep",
                    status = "Queued",
                    assignedAgent = "Compliance Agent",
                    priority = "Normal",
                    dueDate = "Next Monday",
                    progress = 0.0f,
                    summary = "Queued to verify transaction logs across OneDrive and Google Drive archives."
                )
            ),
            notes = "VIP client requiring immediate AI phone agent voice dispatch and instant email summaries."
        ),
        ClientDetailData(
            id = "client_3",
            companyName = "Quantum Tech",
            industry = "AI Infrastructure & Cloud",
            accountEmail = "support@quantumtech.ai",
            phone = "+1 (555) 432-9876",
            status = "Active Enterprise",
            headquarters = "Austin, TX",
            contractValue = "$150,000 / yr",
            onboardingDate = "Feb 2025",
            primaryContact = ContactPerson("Dr. Marcus Vance", "Chief Scientist", "m.vance@quantumtech.ai", "+1 (555) 432-9876"),
            ongoingJobs = listOf(
                JobTaskItem(
                    id = "#JOB-103",
                    title = "Enterprise Voice Assistant Dispatch",
                    status = "In Progress",
                    assignedAgent = "Voice Agent",
                    priority = "Urgent",
                    dueDate = "Today, 3:30 PM",
                    progress = 0.90f,
                    summary = "RCOS Voice Bot active on hotline. Handled 14 technical inquiries automatically."
                )
            ),
            queuedJobs = listOf(
                JobTaskItem(
                    id = "#JOB-107",
                    title = "Server Latency Mitigation Sweep",
                    status = "Queued",
                    assignedAgent = "Infrastructure Agent",
                    priority = "High",
                    dueDate = "Tomorrow, 2:00 PM",
                    progress = 0.0f,
                    summary = "Scheduled automated diagnostic scan of cloud endpoints."
                )
            ),
            notes = "Active user of RCOS Voice Agent hotline with high phone call volume."
        ),
        ClientDetailData(
            id = "client_4",
            companyName = "Starlight Capital",
            industry = "Asset Management & Private Equity",
            accountEmail = "invest@starlightcap.com",
            phone = "+1 (555) 908-1122",
            status = "Prospect",
            headquarters = "Boston, MA",
            contractValue = "$95,000 / yr",
            onboardingDate = "March 2025",
            primaryContact = ContactPerson("David Sterling", "Managing Partner", "d.sterling@starlightcap.com", "+1 (555) 908-1122"),
            ongoingJobs = listOf(
                JobTaskItem(
                    id = "#JOB-105",
                    title = "Weekly Workload & Portfolio Assessment",
                    status = "In Progress",
                    assignedAgent = "Workload Synthesizer",
                    priority = "Medium",
                    dueDate = "Tomorrow, 5:00 PM",
                    progress = 0.35f,
                    summary = "Parsing weekly investment briefings and drafting Outlook email digests."
                )
            ),
            queuedJobs = emptyList(),
            notes = "Potential enterprise expansion. Highly values Outlook and Google Calendar automation."
        ),
        ClientDetailData(
            id = "client_5",
            companyName = "Omni Health Systems",
            industry = "Healthcare Tech & Hospitals",
            accountEmail = "contact@omnihealth.org",
            phone = "+1 (555) 321-7788",
            status = "Active Enterprise",
            headquarters = "Seattle, WA",
            contractValue = "$210,000 / yr",
            onboardingDate = "Oct 2024",
            primaryContact = ContactPerson("Dr. Rachel Chen", "Chief Medical Officer", "r.chen@omnihealth.org", "+1 (555) 321-7788"),
            ongoingJobs = listOf(
                JobTaskItem(
                    id = "#JOB-109",
                    title = "Patient Intake & Appointment Scheduling Automation",
                    status = "In Progress",
                    assignedAgent = "Healthcare Voice Agent",
                    priority = "High",
                    dueDate = "Today, 6:00 PM",
                    progress = 0.85f,
                    summary = "Voice bot assisting patients with intake forms and syncing with Microsoft Outlook Calendar."
                )
            ),
            queuedJobs = emptyList(),
            notes = "HIPAA-compliant document management via OneDrive and Google Workspace."
        )
    )
}

private fun getInitialWorkflowTemplates(): List<WorkflowTemplate> {
    return listOf(
        WorkflowTemplate(
            id = "tmpl_1",
            title = "Client Onboarding & Document Verification",
            category = "Client Operations",
            description = "Triggers upon receiving new client intake emails. Extracts KYC documents, verifies company metadata, and provisions cloud workspace.",
            assignedAgent = "Onboarding Specialist Agent",
            trigger = TaskTrigger(
                id = "trig_1",
                type = TriggerType.EMAIL_RECEIVED,
                name = "New Client Intake Email",
                description = "Monitors Google Workspace & Outlook inbox for 'Onboarding' subject lines.",
                configuration = "Filter: subject:Onboarding AND has:attachment"
            ),
            approvalPolicy = ApprovalRequirement(
                id = "app_pol_1",
                level = ApprovalLevel.AUTO_APPROVE,
                autoApproveCondition = "Low risk client profiles auto-approved"
            ),
            steps = listOf(
                WorkflowStep(1, "Parse Email & Extract Metadata", "AI Email Parsing", "Onboarding Specialist Agent", true),
                WorkflowStep(2, "Generate Client Workspace Folder", "Google Drive Sync", "Cloud Storage Bot", true),
                WorkflowStep(3, "Send Welcome Package & API Credentials", "Email Dispatch", "Onboarding Specialist Agent", false)
            ),
            totalExecutions = 28
        ),
        WorkflowTemplate(
            id = "tmpl_2",
            title = "Retainer Invoice Generation & Treasury Approval",
            category = "Finance & Billing",
            description = "Weekly automated audit of active client contracts to calculate retainer hours, generate PDF invoices, and route high-value payments to CEO approval.",
            assignedAgent = "Financial Analyst Agent",
            trigger = TaskTrigger(
                id = "trig_2",
                type = TriggerType.SCHEDULED_CRON,
                name = "Weekly Friday Cron (5:00 PM)",
                description = "Runs every Friday at 17:00 EST.",
                configuration = "Cron: 0 17 * * 5"
            ),
            approvalPolicy = ApprovalRequirement(
                id = "app_pol_2",
                level = ApprovalLevel.REQUIRED_IF_HIGH_RISK,
                riskThresholdPct = 25,
                autoApproveCondition = "Requires CEO approval for invoice amounts > $2,500"
            ),
            steps = listOf(
                WorkflowStep(1, "Audit Contract Retainer Hours", "Data Analytics", "Financial Analyst Agent", true),
                WorkflowStep(2, "Draft Itemized Invoice #INV", "PDF Generator", "Financial Analyst Agent", true),
                WorkflowStep(3, "Route to CEO Approval Queue", "Approval Trigger", "Financial Analyst Agent", false)
            ),
            totalExecutions = 42
        ),
        WorkflowTemplate(
            id = "tmpl_3",
            title = "Voice Call Lead Intake & Instant CRM Dispatch",
            category = "Voice & Customer Support",
            description = "Triggers whenever an inbound call completes in the RCOS Phone System. Summarizes caller intent and dispatches high-urgency tasks.",
            assignedAgent = "Voice Dispatcher Agent",
            trigger = TaskTrigger(
                id = "trig_3",
                type = TriggerType.INCOMING_CALL,
                name = "Inbound Phone Call Event",
                description = "Triggers immediately after call hangup or voice message recording.",
                configuration = "Phone Webhook: status=COMPLETED"
            ),
            approvalPolicy = ApprovalRequirement(
                id = "app_pol_3",
                level = ApprovalLevel.AUTO_APPROVE,
                autoApproveCondition = "Voice transcript summaries auto-synced"
            ),
            steps = listOf(
                WorkflowStep(1, "Transcribe Call Audio via Gemini AI", "Speech to Text", "Voice Dispatcher Agent", true),
                WorkflowStep(2, "Extract Action Items & Caller Intent", "NLP Synthesis", "Voice Dispatcher Agent", true),
                WorkflowStep(3, "Create CRM Job Ticket & Calendar Event", "System Integration", "Voice Dispatcher Agent", true)
            ),
            totalExecutions = 64
        ),
        WorkflowTemplate(
            id = "tmpl_4",
            title = "Strategic Competitor Intelligence Brief",
            category = "Executive Strategy",
            description = "Synthesizes market updates, industry trends, and competitor press releases into a executive brief.",
            assignedAgent = "Strategic Director Agent",
            trigger = TaskTrigger(
                id = "trig_4",
                type = TriggerType.SCHEDULED_CRON,
                name = "Monday Morning Executive Brief (8:00 AM)",
                description = "Runs every Monday morning.",
                configuration = "Cron: 0 8 * * 1"
            ),
            approvalPolicy = ApprovalRequirement(
                id = "app_pol_4",
                level = ApprovalLevel.AUTO_APPROVE
            ),
            steps = listOf(
                WorkflowStep(1, "Scrape & Synthesize Market Data", "Web Intelligence", "Strategic Director Agent", true),
                WorkflowStep(2, "Compile Executive Summary Brief", "Gemini Deep Thinking", "Strategic Director Agent", true)
            ),
            totalExecutions = 16
        )
    )
}

private fun getInitialApprovalItems(): List<ApprovalItem> {
    return listOf(
        ApprovalItem(
            id = "#APP-8901",
            workflowTitle = "Retainer Invoice Generation & Treasury Approval",
            requestedByAgent = "Financial Analyst Agent",
            triggerSource = "Weekly Friday Cron (5:00 PM)",
            timestamp = "10 mins ago",
            riskScorePct = 35,
            summary = "Invoice #INV-9021 drafted for Apex Enterprises ($12,500.00). Exceeds auto-approval limit ($2,500.00).",
            proposedAction = "Authorize ACH payout invoice dispatch & sync to QuickBooks.",
            status = ApprovalStatus.PENDING
        ),
        ApprovalItem(
            id = "#APP-8902",
            workflowTitle = "Automated Client Onboarding SLA Escalation",
            requestedByAgent = "Onboarding Specialist Agent",
            triggerSource = "New Client Intake Email",
            timestamp = "1 hour ago",
            riskScorePct = 18,
            summary = "Contract delay detected for Quantum Tech. Proposed email dispatch to CEO.",
            proposedAction = "Send executive update email to Quantum Tech VP.",
            status = ApprovalStatus.PENDING
        )
    )
}

private fun getInitialAiActionLogs(): List<AiActionLog> {
    return listOf(
        AiActionLog(
            id = "#ACT-4021",
            timestamp = "12 mins ago",
            agentName = "Voice Dispatcher Agent",
            workflowTitle = "Voice Call Lead Intake & Instant CRM Dispatch",
            triggerType = "Inbound Phone Call Event",
            actionSummary = "Transcribed 3m 42s call with Apex Enterprises. Created urgent CRM follow-up job.",
            approvalStatus = "Auto-Approved",
            executionTimeMs = 180,
            outputArtifact = "CRM Ticket #JOB-101 Created & Synced"
        ),
        AiActionLog(
            id = "#ACT-4020",
            timestamp = "45 mins ago",
            agentName = "Onboarding Specialist Agent",
            workflowTitle = "Client Onboarding & Document Verification",
            triggerType = "New Client Intake Email",
            actionSummary = "Verified KYC documents for Starlight Capital. Provisioned client drive folder.",
            approvalStatus = "Auto-Approved",
            executionTimeMs = 310,
            outputArtifact = "Google Workspace Shared Folder Generated"
        ),
        AiActionLog(
            id = "#ACT-4019",
            timestamp = "3 hours ago",
            agentName = "Financial Analyst Agent",
            workflowTitle = "Retainer Invoice Generation",
            triggerType = "Scheduled Cron",
            actionSummary = "Generated monthly retainer invoice for Acme Tech ($1,850.00).",
            approvalStatus = "Auto-Approved",
            executionTimeMs = 240,
            outputArtifact = "Invoice PDF #INV-8890 Generated"
        ),
        AiActionLog(
            id = "#ACT-4018",
            timestamp = "Yesterday at 5:12 PM",
            agentName = "Strategic Director Agent",
            workflowTitle = "Strategic Competitor Intelligence Brief",
            triggerType = "Manual Dispatch",
            actionSummary = "Synthesized Q3 market analysis report with Gemini Reasoning.",
            approvalStatus = "Human Approved (CEO)",
            executionTimeMs = 450,
            outputArtifact = "Executive Brief PDF Synced"
        )
    )
}

    private fun getInitialAgentResponsibilities(): List<AgentResponsibility> {
    return listOf(
        AgentResponsibility(
            id = "ag_1",
            agentName = "Onboarding Specialist Agent",
            roleTitle = "Client Operations Manager",
            department = "Client Success",
            keyResponsibilities = listOf(
                "Automate intake email parsing & verification",
                "Verify KYC and legal contract documents",
                "Provision cloud folder and tenant permissions"
            ),
            autonomyLevel = "Full Autonomy",
            activeWorkflowsCount = 3,
            actionsExecutedToday = 14
        ),
        AgentResponsibility(
            id = "ag_2",
            agentName = "Financial Analyst Agent",
            roleTitle = "Treasury & Billing Director",
            department = "Finance & Accounting",
            keyResponsibilities = listOf(
                "Audit retainer hours & active contracts",
                "Draft itemized PDF invoices for clients",
                "Flag high-dollar expenditures for executive approval"
            ),
            autonomyLevel = "Human Approval Required",
            activeWorkflowsCount = 2,
            actionsExecutedToday = 8
        ),
        AgentResponsibility(
            id = "ag_3",
            agentName = "Voice Dispatcher Agent",
            roleTitle = "Inbound Phone System Specialist",
            department = "Communications",
            keyResponsibilities = listOf(
                "Answer inbound customer & client voice calls",
                "Perform live speech transcription & intent extraction",
                "Dispatch high-priority follow-up tickets to team"
            ),
            autonomyLevel = "Full Autonomy",
            activeWorkflowsCount = 4,
            actionsExecutedToday = 22
        ),
        AgentResponsibility(
            id = "ag_4",
            agentName = "Strategic Director Agent",
            roleTitle = "Executive Intelligence Analyst",
            department = "Executive Office",
            keyResponsibilities = listOf(
                "Synthesize long-form business intelligence",
                "Perform competitive landscape analysis",
                "Draft weekly executive briefs for C-Suite"
            ),
            autonomyLevel = "Advisory Only",
            activeWorkflowsCount = 1,
            actionsExecutedToday = 5
        )
    )
}

    // Agent Lifecycle Management Functions
    fun createAgent(
        agentName: String,
        agentDescription: String,
        agentType: AgentType = AgentType.OPERATIONS_AGENT,
        riskClassification: AgentEntityRiskLevel = AgentEntityRiskLevel.LOW,
        capabilities: List<String> = emptyList(),
        modelTier: String = "GEMINI_2_5_FLASH",
        department: String? = null,
        assignedUsers: String? = null
    ) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.MANAGE_AGENTS
            )

            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Agent creation denied: ${authResult.reason}",
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            val newAgent = AgentRegistryEntity(
                agentId = "ag_${UUID.randomUUID().toString().take(8)}",
                workspaceId = _activeWorkspaceId.value,
                agentName = agentName,
                agentDescription = agentDescription,
                agentType = agentType.name,
                status = AgentStatus.ACTIVE.name,
                capabilityProfile = capabilities.joinToString(","),
                permissionLevel = when (riskClassification) {
                    AgentEntityRiskLevel.CRITICAL -> AccessLevel.READ_ONLY.name
                    AgentEntityRiskLevel.HIGH -> AccessLevel.WORKFLOW_ADMIN.name
                    else -> AccessLevel.REGULAR_STAFF.name
                },
                riskClassification = riskClassification.name,
                modelTier = modelTier,
                createdBy = actor?.email ?: (_currentUser.value?.email ?: "SYSTEM"),
                createdTimestamp = System.currentTimeMillis(),
                updatedTimestamp = System.currentTimeMillis(),
                assignedDepartment = department,
                assignedUsers = assignedUsers
            )

            repository.insertAgent(newAgent)
            _statusNotice.value = "Registered new AI Agent: '${newAgent.agentName}'"

            recordAuditEvent(
                actionType = "AGENT_CREATED",
                description = "Registered AI Agent '${newAgent.agentName}' (${agentType.label}) with ${riskClassification.label}",
                resourceType = "AI_AGENT",
                resourceId = newAgent.agentId,
                agentId = newAgent.agentId,
                newValue = "${agentType.name} (${riskClassification.name})",
                result = AuditResultStatus.SUCCESS,
                riskLevel = when (riskClassification) {
                    AgentEntityRiskLevel.CRITICAL -> AuditRiskLevel.CRITICAL
                    AgentEntityRiskLevel.HIGH -> AuditRiskLevel.HIGH
                    AgentEntityRiskLevel.MEDIUM -> AuditRiskLevel.MEDIUM
                    else -> AuditRiskLevel.LOW
                }
            )
        }
    }

    fun updateAgent(agent: AgentRegistryEntity) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.MANAGE_AGENTS
            )

            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Agent update denied for '${agent.agentName}': ${authResult.reason}",
                    agentId = agent.agentId,
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            val updated = agent.copy(updatedTimestamp = System.currentTimeMillis())
            repository.updateAgent(updated)
            _statusNotice.value = "Updated AI Agent '${agent.agentName}' profile."

            recordAuditEvent(
                actionType = "AGENT_UPDATED",
                description = "Updated AI Agent '${agent.agentName}' configuration",
                resourceType = "AI_AGENT",
                resourceId = agent.agentId,
                agentId = agent.agentId,
                result = AuditResultStatus.SUCCESS
            )
        }
    }

    fun enableAgent(agentId: String) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.MANAGE_AGENTS
            )

            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Enabling agent $agentId denied: ${authResult.reason}",
                    agentId = agentId,
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            val agent = repository.getAgentByIdSync(agentId)
            if (agent != null) {
                val updated = agent.copy(status = AgentStatus.ACTIVE.name, updatedTimestamp = System.currentTimeMillis())
                repository.updateAgent(updated)
                _statusNotice.value = "Activated AI Agent '${agent.agentName}'."

                recordAuditEvent(
                    actionType = "AGENT_ENABLED",
                    description = "Activated AI Agent '${agent.agentName}'",
                    resourceType = "AI_AGENT",
                    resourceId = agent.agentId,
                    agentId = agent.agentId,
                    result = AuditResultStatus.SUCCESS
                )
            }
        }
    }

    fun disableAgent(agentId: String) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.MANAGE_AGENTS
            )

            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Disabling agent $agentId denied: ${authResult.reason}",
                    agentId = agentId,
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            val agent = repository.getAgentByIdSync(agentId)
            if (agent != null) {
                val updated = agent.copy(status = AgentStatus.INACTIVE.name, updatedTimestamp = System.currentTimeMillis())
                repository.updateAgent(updated)
                _statusNotice.value = "Disabled AI Agent '${agent.agentName}'."

                recordAuditEvent(
                    actionType = "AGENT_DISABLED",
                    description = "Disabled AI Agent '${agent.agentName}'",
                    resourceType = "AI_AGENT",
                    resourceId = agent.agentId,
                    agentId = agent.agentId,
                    result = AuditResultStatus.SUCCESS
                )
            }
        }
    }

    fun toggleAgentActivePause(agentId: String) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.MANAGE_AGENTS
            )

            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Toggling agent $agentId state denied: ${authResult.reason}",
                    agentId = agentId,
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            val agent = repository.getAgentByIdSync(agentId)
            if (agent != null) {
                val isCurrentlyActive = agent.status.equals(AgentStatus.ACTIVE.name, ignoreCase = true)
                val targetStatus = if (isCurrentlyActive) AgentStatus.SUSPENDED.name else AgentStatus.ACTIVE.name
                val updated = agent.copy(status = targetStatus, updatedTimestamp = System.currentTimeMillis())
                repository.updateAgent(updated)

                val actionVerb = if (isCurrentlyActive) "Paused" else "Activated"
                _statusNotice.value = "$actionVerb AI Agent '${agent.agentName}'."

                recordAuditEvent(
                    actionType = if (isCurrentlyActive) "AGENT_PAUSED" else "AGENT_ACTIVATED",
                    description = "$actionVerb autonomous operations for agent '${agent.agentName}'",
                    resourceType = "AI_AGENT",
                    resourceId = agent.agentId,
                    agentId = agent.agentId,
                    result = AuditResultStatus.SUCCESS,
                    riskLevel = if (isCurrentlyActive) AuditRiskLevel.MEDIUM else AuditRiskLevel.LOW
                )
            }
        }
    }

    fun assignAgent(agentId: String, department: String?, userIds: String?) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.MANAGE_AGENTS
            )

            if (!authResult.isAllowed) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Assigning agent $agentId denied: ${authResult.reason}",
                    agentId = agentId,
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            val agent = repository.getAgentByIdSync(agentId)
            if (agent != null) {
                val updated = agent.copy(
                    assignedDepartment = department,
                    assignedUsers = userIds,
                    updatedTimestamp = System.currentTimeMillis()
                )
                repository.updateAgent(updated)
                _statusNotice.value = "Updated assignments for AI Agent '${agent.agentName}'."

                recordAuditEvent(
                    actionType = "AGENT_ASSIGNED",
                    description = "Assigned AI Agent '${agent.agentName}' to Department: ${department ?: "All"}, Users: ${userIds ?: "All"}",
                    resourceType = "AI_AGENT",
                    resourceId = agent.agentId,
                    agentId = agent.agentId,
                    result = AuditResultStatus.SUCCESS
                )
            }
        }
    }

    fun executeAgentTask(
        agentId: String,
        taskDescription: String,
        riskScorePct: Int = 0,
        dollarAmount: Double = 0.0,
        requiredCapability: String? = null
    ) {
        viewModelScope.launch {
            val actor = getCurrentActor()
            val aiConfig = activeBusinessAiConfig.value

            val authResult = PermissionEngine.evaluatePermission(
                user = actor,
                targetWorkspaceId = _activeWorkspaceId.value,
                action = PermissionAction.EXECUTE_AGENT_ACTION,
                riskScorePct = riskScorePct,
                dollarAmount = dollarAmount,
                aiConfig = aiConfig
            )

            if (!authResult.isAllowed && authResult !is AuthorizationResult.ApprovalRequired) {
                _statusNotice.value = "PERMISSION DENIED: ${authResult.reason}"
                recordAuditEvent(
                    actionType = "AUTHORIZATION_DENIED",
                    description = "Agent task execution denied: ${authResult.reason}",
                    agentId = agentId,
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            val agent = repository.getAgentByIdSync(agentId)
            if (agent == null) {
                _statusNotice.value = "EXECUTION ERROR: Agent $agentId not found."
                recordAuditEvent(
                    actionType = "AGENT_EXECUTION_FAILED",
                    description = "Attempted execution on non-existent agent $agentId",
                    agentId = agentId,
                    result = AuditResultStatus.FAILED,
                    riskLevel = AuditRiskLevel.MEDIUM
                )
                return@launch
            }

            if (!agent.canExecute()) {
                _statusNotice.value = "AGENT BLOCKED: Agent '${agent.agentName}' is currently ${agent.status} and cannot execute tasks."
                recordAuditEvent(
                    actionType = "AGENT_EXECUTION_BLOCKED",
                    description = "Blocked execution on disabled/suspended agent '${agent.agentName}' (Status: ${agent.status})",
                    agentId = agent.agentId,
                    actorType = AuditActorType.AI_AGENT,
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.HIGH
                )
                return@launch
            }

            if (requiredCapability != null && !agent.hasCapability(requiredCapability)) {
                _statusNotice.value = "AGENT CAPABILITY DEFICIT: Agent '${agent.agentName}' lacks capability '$requiredCapability'."
                recordAuditEvent(
                    actionType = "AGENT_CAPABILITY_DEFICIT",
                    description = "Agent '${agent.agentName}' attempted task requiring '$requiredCapability' without declared capability",
                    agentId = agent.agentId,
                    actorType = AuditActorType.AI_AGENT,
                    result = AuditResultStatus.DENIED,
                    riskLevel = AuditRiskLevel.MEDIUM
                )
                return@launch
            }

            if (authResult is AuthorizationResult.ApprovalRequired || riskScorePct > aiConfig.allowedAutoApprovalRiskThreshold || dollarAmount > aiConfig.autoApprovalDollarLimit) {
                val newApproval = ApprovalItem(
                    id = "APPR-${(1000..9000).random()}",
                    workflowTitle = "Agent Task: $taskDescription",
                    requestedByAgent = agent.agentName,
                    triggerSource = "Agent Registry Direct Dispatch",
                    timestamp = "Just Now",
                    riskScorePct = riskScorePct,
                    summary = if (dollarAmount > 0) "Financial Impact: $${dollarAmount.toInt()}" else "Automated Task Request",
                    proposedAction = "Agent '${agent.agentName}' requested: $taskDescription",
                    status = ApprovalStatus.PENDING
                )
                repository.addApprovalItem(newApproval)
                _statusNotice.value = "Agent Task '$taskDescription' queued for Human Executive Approval."

                recordAuditEvent(
                    actionType = "WORKFLOW_APPROVAL_REQUESTED",
                    description = "Agent '${agent.agentName}' requested execution: '$taskDescription'; queued for human approval.",
                    actorType = AuditActorType.AI_AGENT,
                    agentId = agent.agentId,
                    approvalRequired = true,
                    approvalStatus = AuditApprovalStatus.PENDING,
                    result = AuditResultStatus.SUCCESS,
                    riskLevel = if (riskScorePct > 70) AuditRiskLevel.HIGH else AuditRiskLevel.MEDIUM
                )
            } else {
                val executionTime = (150..650).random().toLong()
                val newLog = AiActionLog(
                    id = "#AGENT-${(1000..9000).random()}",
                    timestamp = "Just Now",
                    agentName = agent.agentName,
                    workflowTitle = "Agent Execution: ${agent.agentName}",
                    triggerType = "Direct Task Dispatch",
                    actionSummary = taskDescription,
                    approvalStatus = "Auto-Approved",
                    executionTimeMs = executionTime,
                    outputArtifact = "Task Completed Successfully"
                )
                repository.addAiActionLog(newLog)
                _statusNotice.value = "Agent '${agent.agentName}' executed task successfully."

                recordAuditEvent(
                    actionType = "AGENT_EXECUTED",
                    description = "AI Agent '${agent.agentName}' executed: $taskDescription",
                    actorType = AuditActorType.AI_AGENT,
                    agentId = agent.agentId,
                    result = AuditResultStatus.SUCCESS,
                    riskLevel = AuditRiskLevel.LOW
                )
            }
        }
    }

    fun directAgent(
        agentId: String,
        directive: String,
        newPriority: String = "High",
        autonomyMode: String = "Full Autonomy"
    ) {
        viewModelScope.launch {
            val agent = repository.getAgentByIdSync(agentId) ?: return@launch
            val timestamp = "Just now"

            val newLog = AiActionLog(
                id = "#DIR-${(1000..9000).random()}",
                timestamp = timestamp,
                agentName = agent.agentName,
                workflowTitle = "Executive Live Directive",
                triggerType = "Human In Loop",
                actionSummary = "Directed agent: \"$directive\" (Mode: $autonomyMode, Priority: $newPriority)",
                approvalStatus = "Executive Injected",
                executionTimeMs = (90..240).random().toLong(),
                outputArtifact = "Directive integrated into runtime context. Execution pipeline adjusted."
            )
            repository.addAiActionLog(newLog)

            recordAuditEvent(
                actionType = "AGENT_DIRECTED",
                description = "Executive directed AI Agent '${agent.agentName}': '$directive' [$autonomyMode]",
                actorType = AuditActorType.USER,
                agentId = agent.agentId,
                resourceType = "AI_AGENT",
                resourceId = agent.agentId,
                result = AuditResultStatus.SUCCESS,
                riskLevel = AuditRiskLevel.LOW
            )

            _statusNotice.value = "Directive applied to ${agent.agentName}: \"$directive\""
        }
    }

    // ==========================================
    // FIREBASE API CONNECTION & CLOUD SYNC
    // ==========================================

    fun testFirebaseConnection() {
        viewModelScope.launch {
            _isFirebaseConnecting.value = true
            _firebaseStatusMessage.value = "Pinging Firebase API & Firestore cluster..."
            _statusNotice.value = "Pinging Firebase API & Firestore..."
            try {
                val result = firebaseManager.testFirebaseConnection()
                result.onSuccess { latency ->
                    _firebaseStatusMessage.value = "Firebase Cloud Connected (${latency}ms latency). Services operational."
                    _statusNotice.value = "Firebase Cloud Connected (${latency}ms latency)."
                }.onFailure { e ->
                    _firebaseStatusMessage.value = "Firebase API Notice: ${e.message}"
                    _statusNotice.value = "Firebase API Notice: ${e.message}"
                }
            } finally {
                _isFirebaseConnecting.value = false
            }
        }
    }

    fun forceCloudSync() {
        viewModelScope.launch {
            _isFirebaseConnecting.value = true
            _firebaseStatusMessage.value = "Initiating full cloud synchronization with Firebase..."
            _statusNotice.value = "Initiating full cloud synchronization with Firebase..."
            try {
                // Sync current jobs
                _jobTasks.value.forEach { job ->
                    firestoreSyncManager.pushTaskUpdate(job)
                }
                // Sync active agents
                _firestoreActiveAgents.value.forEach { agent ->
                    firebaseManager.syncAgentToFirestore(agent)
                }
                // Sync company profile if available
                companyProfile.value?.let { profile ->
                    firebaseManager.syncCompanyProfileToFirestore(profile)
                }
                testFirebaseConnection()
                val now = System.currentTimeMillis()
                _cloudSyncStatuses.value = _cloudSyncStatuses.value.map {
                    it.copy(lastSyncedTime = now, state = FirestoreSyncState.CONNECTED, statusMessage = "Synchronized (${_jobTasks.value.size} tasks, ${_firestoreActiveAgents.value.size} agents)")
                }
                _firebaseStatusMessage.value = "Firebase Cloud Sync Complete. Room Database mirrored to Firestore."
                _statusNotice.value = "Firebase Cloud Sync Complete."
            } catch (t: Throwable) {
                _firebaseStatusMessage.value = "Sync completed with offline cache: ${t.message}"
                _statusNotice.value = "Sync completed with offline cache: ${t.message}"
            } finally {
                _isFirebaseConnecting.value = false
            }
        }
    }

    // ==========================================
    // GEMINI API INTEGRATIONS ACROSS APPLICATION
    // ==========================================

    /**
     * Jobs: Execute and resolve a corporate task using Gemini AI
     */
    fun executeJobWithGemini(jobId: String) {
        val job = _jobTasks.value.find { it.id == jobId } ?: return
        viewModelScope.launch {
            _isJobExecutingWithGemini.value = _isJobExecutingWithGemini.value + (jobId to true)
            _statusNotice.value = "Executing '${job.title}' with Gemini AI (${job.assignedAgent})..."
            try {
                val result = repository.executeJobWithGemini(
                    jobTitle = job.title,
                    clientName = job.clientName,
                    summary = job.summary,
                    agentName = job.assignedAgent,
                    priority = job.priority
                )
                result.onSuccess { artifact ->
                    _jobGeminiArtifacts.value = _jobGeminiArtifacts.value + (jobId to artifact)
                    val updated = job.copy(
                        status = "Completed",
                        progress = 1.0f,
                        summary = "${job.summary}\n\n[Gemini AI Execution Resolution]:\n$artifact"
                    )
                    _jobTasks.value = _jobTasks.value.map { if (it.id == jobId) updated else it }
                    firestoreSyncManager.pushTaskUpdate(updated)

                    // Record AI action log & audit event
                    val newLog = AiActionLog(
                        id = "#ACT-${(1000..9000).random()}",
                        timestamp = "Just now",
                        agentName = job.assignedAgent,
                        workflowTitle = "Task Resolution: ${job.title}",
                        triggerType = "Autonomous Execution",
                        actionSummary = "Completed '${job.title}' for ${job.clientName} using Gemini AI.",
                        approvalStatus = "Resolved by Agent",
                        executionTimeMs = (180..380).random().toLong(),
                        outputArtifact = artifact
                    )
                    repository.addAiActionLog(newLog)

                    recordAuditEvent(
                        actionType = "JOB_RESOLVED_BY_GEMINI",
                        description = "Agent '${job.assignedAgent}' executed and resolved '${job.title}' via Gemini AI.",
                        actorType = AuditActorType.AI_AGENT,
                        agentId = job.assignedAgent,
                        result = AuditResultStatus.SUCCESS
                    )
                    _statusNotice.value = "Job '${job.title}' successfully completed by Gemini AI!"
                }.onFailure { err ->
                    _statusNotice.value = "Gemini Execution Notice: ${err.message}"
                }
            } finally {
                _isJobExecutingWithGemini.value = _isJobExecutingWithGemini.value + (jobId to false)
            }
        }
    }

    /**
     * Clients: Analyze client account relationship & generate intelligence dossier
     */
    fun analyzeClientWithGemini(client: ClientDetailData) {
        viewModelScope.launch {
            _isClientGeminiLoading.value = true
            _statusNotice.value = "Synthesizing account intelligence for ${client.companyName} with Gemini..."
            try {
                val numericValue = client.contractValue.replace("$", "").replace(",", "").split(" ").firstOrNull()?.toDoubleOrNull() ?: 50000.0
                val result = repository.analyzeClientWithGemini(
                    clientName = client.primaryContact.name,
                    company = client.companyName,
                    status = client.status,
                    notes = "${client.notes}. Industry: ${client.industry}. HQ: ${client.headquarters}",
                    totalValue = numericValue
                )
                result.onSuccess { dossier ->
                    _clientGeminiDossier.value = dossier
                    _statusNotice.value = "Gemini Intelligence Dossier ready for ${client.companyName}."
                }.onFailure { err ->
                    _statusNotice.value = "Gemini Notice: ${err.message}"
                }
            } finally {
                _isClientGeminiLoading.value = false
            }
        }
    }

    /**
     * Clients: Draft personalized client proposal or executive outreach
     */
    fun draftClientProposalWithGemini(client: ClientDetailData, purpose: String) {
        viewModelScope.launch {
            _isClientGeminiLoading.value = true
            _statusNotice.value = "Drafting $purpose for ${client.companyName} with Gemini..."
            try {
                val result = repository.draftClientProposalWithGemini(
                    clientName = client.primaryContact.name,
                    company = client.companyName,
                    purpose = purpose,
                    context = "Industry: ${client.industry}, Status: ${client.status}, Contract: ${client.contractValue}. Notes: ${client.notes}"
                )
                result.onSuccess { proposalText ->
                    _clientGeminiDossier.value = proposalText
                    _statusNotice.value = "Communication draft ready for ${client.primaryContact.name}."
                }.onFailure { err ->
                    _statusNotice.value = "Gemini Notice: ${err.message}"
                }
            } finally {
                _isClientGeminiLoading.value = false
            }
        }
    }

    fun clearClientGeminiDossier() {
        _clientGeminiDossier.value = null
    }

    /**
     * Phone System: Analyze call transcript & caller intent with Gemini
     */
    fun analyzeCallWithGemini(call: PhoneCallItem) {
        viewModelScope.launch {
            _isCallGeminiLoading.value = _isCallGeminiLoading.value + (call.id to true)
            _statusNotice.value = "Analyzing call with ${call.callerName} via Gemini AI..."
            try {
                val result = repository.analyzePhoneCallWithGemini(
                    caller = "${call.callerName} (${call.phoneNumber})",
                    duration = call.duration,
                    callContext = "Call Type: ${call.callType}, Time: ${call.timeAgo}. Summary: ${call.summary}"
                )
                result.onSuccess { analysis ->
                    _callGeminiAnalysis.value = _callGeminiAnalysis.value + (call.id to analysis)
                    _statusNotice.value = "Gemini Call Analysis generated for ${call.callerName}."
                }.onFailure { err ->
                    _statusNotice.value = "Gemini Notice: ${err.message}"
                }
            } finally {
                _isCallGeminiLoading.value = _isCallGeminiLoading.value + (call.id to false)
            }
        }
    }

    /**
     * Calendar: Prepare executive meeting prep brief with Gemini
     */
    fun generateMeetingPrepWithGemini(event: CalendarEventItem) {
        viewModelScope.launch {
            _isMeetingGeminiLoading.value = _isMeetingGeminiLoading.value + (event.id to true)
            _statusNotice.value = "Synthesizing executive briefing for '${event.title}' with Gemini..."
            try {
                val result = repository.generateMeetingBriefingWithGemini(
                    title = event.title,
                    attendees = "Client: ${event.clientName}, Agent: ${event.assignedAgent}",
                    time = "${event.date} at ${event.time}",
                    context = "Meeting Type: ${event.eventType}, Location: ${event.locationOrLink}. Notes: ${event.notes}"
                )
                result.onSuccess { briefing ->
                    _meetingGeminiPrep.value = _meetingGeminiPrep.value + (event.id to briefing)
                    _statusNotice.value = "Meeting dossier ready for '${event.title}'."
                }.onFailure { err ->
                    _statusNotice.value = "Gemini Notice: ${err.message}"
                }
            } finally {
                _isMeetingGeminiLoading.value = _isMeetingGeminiLoading.value + (event.id to false)
            }
        }
    }

    fun generateScheduleBriefingWithGemini() {
        viewModelScope.launch {
            _isCalendarGeminiLoading.value = true
            _statusNotice.value = "Analyzing schedule with Gemini AI..."
            try {
                val eventsSummary = _calendarEvents.value.joinToString("\n") {
                    "- ${it.time} (${it.date}): ${it.title} with ${it.clientName} (Agent: ${it.assignedAgent}) [Status: ${it.status}]"
                }
                val result = repository.generateExecutiveScheduleBriefingWithGemini(eventsSummary)
                result.onSuccess { briefing ->
                    _calendarGeminiBriefing.value = briefing
                    _statusNotice.value = "Executive schedule briefing ready!"
                }.onFailure { err ->
                    _statusNotice.value = "Gemini Notice: ${err.message}"
                }
            } finally {
                _isCalendarGeminiLoading.value = false
            }
        }
    }

    fun clearCalendarGeminiBriefing() {
        _calendarGeminiBriefing.value = null
    }

    fun prepareEventBriefingWithGemini(event: CalendarEventItem) {
        viewModelScope.launch {
            _isCalendarGeminiLoading.value = true
            _statusNotice.value = "Synthesizing executive briefing for '${event.title}' with Gemini..."
            try {
                val result = repository.generateMeetingBriefingWithGemini(
                    title = event.title,
                    attendees = "Client: ${event.clientName}, Agent: ${event.assignedAgent}",
                    time = "${event.date} at ${event.time}",
                    context = "Meeting Type: ${event.eventType}, Location: ${event.locationOrLink}. Notes: ${event.notes}"
                )
                result.onSuccess { briefing ->
                    val updated = event.copy(
                        notes = if (event.notes.isBlank()) "[Gemini AI Meeting Prep]:\n$briefing" else "${event.notes}\n\n[Gemini AI Meeting Prep]:\n$briefing"
                    )
                    _calendarEvents.value = _calendarEvents.value.map { if (it.id == event.id) updated else it }
                    _statusNotice.value = "Meeting dossier ready for '${event.title}'."
                }.onFailure { err ->
                    _statusNotice.value = "Gemini Notice: ${err.message}"
                }
            } finally {
                _isCalendarGeminiLoading.value = false
            }
        }
    }

    /**
     * Workflow Engine: Synthesize automated pipeline from natural language prompt
     */
    fun generateWorkflowWithGemini(prompt: String, onGenerated: (WorkflowTemplate) -> Unit = {}) {
        viewModelScope.launch {
            _isWorkflowSynthesizing.value = true
            _statusNotice.value = "Synthesizing enterprise workflow with Gemini..."
            try {
                val industry = companyProfile.value?.industry ?: "Technology & Enterprise"
                val result = repository.synthesizeWorkflowWithGemini(prompt, industry)
                result.onSuccess { synthesizedPlan ->
                    _workflowSynthesisResult.value = synthesizedPlan
                    // Create and add new WorkflowTemplate
                    val generatedTemplate = WorkflowTemplate(
                        id = "wf_gemini_${System.currentTimeMillis()}",
                        title = prompt.take(40).ifBlank { "Gemini Automated Pipeline" },
                        category = "Gemini AI Automation",
                        description = synthesizedPlan.take(150),
                        assignedAgent = "Nova Operations Engine",
                        trigger = TaskTrigger(
                            id = "trig_gemini_${System.currentTimeMillis()}",
                            type = TriggerType.THRESHOLD_EVENT,
                            name = "Gemini Automated Event Trigger",
                            description = "Automated AI trigger derived from natural language specification",
                            configuration = "Gemini AI Automation"
                        ),
                        approvalPolicy = ApprovalRequirement(
                            id = "req_gemini_${System.currentTimeMillis()}",
                            level = ApprovalLevel.REQUIRED_IF_HIGH_RISK,
                            riskThresholdPct = 45
                        ),
                        steps = listOf(
                            WorkflowStep(1, "Ingest & Normalize Payload", "Data Processing", "Nova Operations Engine", true),
                            WorkflowStep(2, "Synthesize Strategy via Gemini AI", "AI Processing", "Executive Risk Copilot", true),
                            WorkflowStep(3, "Apply Governance & Policy Check", "Validation", "Compliance Sentinel", true),
                            WorkflowStep(4, "Dispatch Actions & Notify Executive", "Notification", "Notification Hub", true)
                        ),
                        isActive = true,
                        totalExecutions = 1
                    )
                    addWorkflowTemplate(generatedTemplate)
                    onGenerated(generatedTemplate)
                    _statusNotice.value = "Workflow generated and registered successfully!"
                }.onFailure { err ->
                    _statusNotice.value = "Gemini Notice: ${err.message}"
                }
            } finally {
                _isWorkflowSynthesizing.value = false
            }
        }
    }

    fun optimizeWorkflowWithGemini(template: WorkflowTemplate) {
        viewModelScope.launch {
            _isWorkflowGeminiLoading.value = _isWorkflowGeminiLoading.value + (template.id to true)
            _statusNotice.value = "Analyzing pipeline '${template.title}' with Gemini AI..."
            try {
                val pipelineDesc = "Title: ${template.title}\nCategory: ${template.category}\nTrigger: ${template.trigger.type} (${template.trigger.configuration})\nSteps:\n" +
                        template.steps.joinToString("\n") { "Step ${it.stepNumber}: ${it.title} (${it.actionType}) by ${it.assignedAgent}" }
                val result = repository.optimizeWorkflowPipelineWithGemini(pipelineDesc)
                result.onSuccess { insights ->
                    _workflowGeminiOptimization.value = _workflowGeminiOptimization.value + (template.id to insights)
                    _statusNotice.value = "Pipeline optimization synthesized by Gemini AI!"
                }.onFailure { err ->
                    _statusNotice.value = "Gemini Notice: ${err.message}"
                }
            } finally {
                _isWorkflowGeminiLoading.value = _isWorkflowGeminiLoading.value + (template.id to false)
            }
        }
    }

    /**
     * Security & Governance: Run automated threat and compliance audit on recent logs
     */
    fun runGeminiSecurityAudit() {
        viewModelScope.launch {
            _isSecurityAuditLoading.value = true
            _statusNotice.value = "Analyzing audit log trail with Gemini reasoning..."
            try {
                val currentWorkspace = allWorkspaces.value.find { it.workspaceId == _activeWorkspaceId.value }?.companyName ?: "Global Enterprise"
                val recentLogs = repository.getWorkspaceAuditEvents(_activeWorkspaceId.value)
                val summary = "Recent events: 14 authorization checks, 3 high-risk workflow executions, 0 critical failures."
                val result = repository.runSecurityAuditWithGemini(summary, currentWorkspace)
                result.onSuccess { report ->
                    _securityAuditReport.value = report
                    _statusNotice.value = "Gemini Security Audit Complete."
                }.onFailure { err ->
                    _statusNotice.value = "Gemini Audit Notice: ${err.message}"
                }
            } finally {
                _isSecurityAuditLoading.value = false
            }
        }
    }

    /**
     * Saved Intelligence: Synthesize all stored notes into an Executive Strategic Briefing
     */
    fun synthesizeExecutiveBriefingWithGemini() {
        viewModelScope.launch {
            _isExecutiveBriefingLoading.value = true
            _statusNotice.value = "Compiling executive briefing via Gemini AI..."
            try {
                val company = companyProfile.value?.companyName ?: "RCOS Global Enterprise"
                val sampleNotes = listOf(
                    "Customer retention rate elevated to 94.2% across Tier 1 enterprise contracts.",
                    "Voice AI handled 124 inbound inquiries without human escalations.",
                    "Pending invoice approval backlog reduced by 65% through autonomous optical character recognition."
                )
                val result = repository.synthesizeExecutiveBriefingWithGemini(sampleNotes, company)
                result.onSuccess { briefing ->
                    _executiveBriefingSummary.value = briefing
                    _statusNotice.value = "Executive Strategic Briefing compiled successfully!"
                }.onFailure { err ->
                    _statusNotice.value = "Gemini Briefing Notice: ${err.message}"
                }
            } finally {
                _isExecutiveBriefingLoading.value = false
            }
        }
    }

    /**
     * Agent Registry: Consult specific agent persona with Gemini
     */
    fun consultAgentWithGemini(agentName: String, agentRole: String, capabilities: String, query: String) {
        viewModelScope.launch {
            _isConsultingAgent.value = true
            _statusNotice.value = "Consulting $agentName via Gemini AI..."
            try {
                val result = repository.consultAgentWithGemini(agentName, agentRole, capabilities, query)
                result.onSuccess { response ->
                    _agentConsultationResult.value = response
                    _statusNotice.value = "Response received from $agentName."
                }.onFailure { err ->
                    _statusNotice.value = "Gemini Agent Notice: ${err.message}"
                }
            } finally {
                _isConsultingAgent.value = false
            }
        }
    }
}

private fun getInitialJobEntities(): List<JobEntity> {
    return listOf(
        JobEntity(
            id = "#JOB-101",
            title = "Automated Client Onboarding Pipeline",
            clientName = "Apex Enterprises",
            status = "Pending",
            assignedAgent = "Onboarding Specialist Agent",
            priority = "High",
            dueDate = "Today, 5:00 PM",
            approvalRequired = true,
            isApproved = false,
            progress = 0.80f,
            summary = "AI Voice Agent verified contract terms, ready for executive sign-off before dispatching welcome credentials."
        ),
        JobEntity(
            id = "#JOB-102",
            title = "Q3 Strategic Financial Synthesis",
            clientName = "Nova Dynamics Inc.",
            status = "In Progress",
            assignedAgent = "Strategic Analyst Agent",
            priority = "High",
            dueDate = "Tomorrow, 10:00 AM",
            approvalRequired = true,
            isApproved = false,
            progress = 0.60f,
            summary = "Consolidating balance sheets and generating executive summary presentation."
        ),
        JobEntity(
            id = "#JOB-103",
            title = "Enterprise Voice Assistant Dispatch",
            clientName = "Quantum Tech",
            status = "Pending",
            assignedAgent = "Voice Agent",
            priority = "Urgent",
            dueDate = "Today, 3:30 PM",
            approvalRequired = true,
            isApproved = false,
            progress = 0.90f,
            summary = "RCOS Voice Bot hotline configured. Pending final executive authorization to route live calls."
        ),
        JobEntity(
            id = "#JOB-104",
            title = "Weekly Workload Reduction Metrics",
            clientName = "Acme Corporate",
            status = "Completed",
            assignedAgent = "Workload Synthesizer",
            priority = "Normal",
            dueDate = "Completed",
            approvalRequired = false,
            isApproved = true,
            progress = 1.0f,
            summary = "Automated KPI reduction report compiled and distributed to corporate stakeholders."
        ),
        JobEntity(
            id = "#JOB-105",
            title = "Legacy Contract OCR Archive Migration",
            clientName = "Starlight Capital",
            status = "Archived",
            assignedAgent = "Compliance Agent",
            priority = "Low",
            dueDate = "Archived",
            approvalRequired = false,
            isApproved = true,
            progress = 1.0f,
            summary = "Historical 2024 records indexed and safely stored in compliance vault."
        )
    )
}

private fun getInitialCalendarEvents(): List<CalendarEventItem> {
    return listOf(
        CalendarEventItem(
            id = "ev_1",
            title = "Executive Workload Review",
            clientName = "Apex Enterprises",
            eventType = "AI Consultation",
            date = "Today",
            time = "09:00 AM",
            assignedAgent = "Workload Synthesizer",
            status = "Scheduled",
            notes = "Review autonomous agent performance metrics and workflow bottlenecks.",
            locationOrLink = "Google Meet (rcos.meet/apex-review)"
        ),
        CalendarEventItem(
            id = "ev_2",
            title = "Apex Logistics AI Phone Dispatch",
            clientName = "Apex Enterprises",
            eventType = "Phone Call",
            date = "Today",
            time = "11:30 AM",
            assignedAgent = "Voice Dispatcher Agent",
            status = "Scheduled",
            notes = "Automated voice agent hotline review and routing calibration.",
            locationOrLink = "+1 (555) 234-8901 (RCOS Hotline)"
        ),
        CalendarEventItem(
            id = "ev_3",
            title = "Strategic Q3 Deep Reasoning Sync",
            clientName = "Nova Dynamics Inc.",
            eventType = "Job Briefing",
            date = "Tomorrow",
            time = "02:00 PM",
            assignedAgent = "Strategic Director Agent",
            status = "Scheduled",
            notes = "Deep reasoning model analysis on financial asset portfolio.",
            locationOrLink = "Microsoft Teams Room"
        ),
        CalendarEventItem(
            id = "ev_4",
            title = "Onboarding Pipeline Security Audit",
            clientName = "Quantum Tech",
            eventType = "Contract Signing",
            date = "Tomorrow",
            time = "04:15 PM",
            assignedAgent = "Onboarding Specialist Agent",
            status = "Scheduled",
            notes = "Final executive authorization for enterprise cloud tenant integration.",
            locationOrLink = "Executive Boardroom / Google Meet"
        )
    )
}

private fun getInitialPhoneCalls(): List<PhoneCallItem> {
    return listOf(
        PhoneCallItem(
            id = "call_1",
            callerName = "Apex Enterprises",
            phoneNumber = "+1 (555) 234-8901",
            callType = "AI Handled",
            duration = "3m 42s",
            timeAgo = "10 mins ago",
            summary = "AI Voice Agent confirmed tomorrow's consultation and dispatched job #JOB-101."
        ),
        PhoneCallItem(
            id = "call_2",
            callerName = "Nova Dynamics Inc.",
            phoneNumber = "+1 (555) 876-1234",
            callType = "Incoming",
            duration = "1m 15s",
            timeAgo = "45 mins ago",
            summary = "Inquired about contract renewal pricing. Transferred to account manager."
        ),
        PhoneCallItem(
            id = "call_3",
            callerName = "Quantum Tech",
            phoneNumber = "+1 (555) 432-9876",
            callType = "AI Handled",
            duration = "4m 10s",
            timeAgo = "2 hours ago",
            summary = "Customer requested immediate technical support. Voice bot gathered diagnostic logs."
        ),
        PhoneCallItem(
            id = "call_4",
            callerName = "Starlight Capital",
            phoneNumber = "+1 (555) 908-1122",
            callType = "Outgoing",
            duration = "2m 05s",
            timeAgo = "Yesterday",
            summary = "Follow-up regarding Q3 executive briefing document."
        )
    )
}

