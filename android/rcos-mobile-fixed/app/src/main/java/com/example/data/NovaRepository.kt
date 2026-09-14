package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class NovaRepository(
    private val dao: AppDao,
    private val workflowDao: WorkflowDao,
    private val workspaceDao: WorkspaceDao,
    private val auditLogDao: AuditLogDao? = null,
    private val agentRegistryDao: AgentRegistryDao? = null,
    private val workspaceOnboardingDao: WorkspaceOnboardingDao? = null
) {

    val allDashboardItems: Flow<List<DashboardItem>> = dao.getAllDashboardItems()
    val dashboardItemCount: Flow<Int> = dao.getDashboardItemsCount()
    val allChatSessions: Flow<List<ChatSessionEntity>> = dao.getAllChatSessions()

    // Multi-Workspace Architecture Streams
    val allWorkspaces: Flow<List<WorkspaceEntity>> = workspaceDao.getAllWorkspaces()

    fun getUserAccountsByWorkspace(workspaceId: String): Flow<List<UserAccount>> =
        workspaceDao.getUserAccountsByWorkspace(workspaceId).map { list -> list.map { it.toDomain() } }

    fun getBusinessAiConfig(workspaceId: String): Flow<BusinessAiConfig> =
        workspaceDao.getBusinessAiConfig(workspaceId).map { it?.toDomain() ?: BusinessAiConfig(workspaceId = workspaceId) }

    // Audit Log Streams & Access
    fun getWorkspaceAuditEvents(workspaceId: String): Flow<List<AuditLogEntity>> =
        auditLogDao?.getWorkspaceAuditEvents(workspaceId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun recordAuditEvent(event: AuditLogEntity) {
        auditLogDao?.insertAuditEvent(event)
    }

    // AI Agent Registry Streams & Lifecycle Operations
    fun getWorkspaceAgents(workspaceId: String): Flow<List<AgentRegistryEntity>> =
        agentRegistryDao?.getWorkspaceAgents(workspaceId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getActiveWorkspaceAgents(workspaceId: String): Flow<List<AgentRegistryEntity>> =
        agentRegistryDao?.getActiveWorkspaceAgents(workspaceId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getAgentByIdSync(agentId: String): AgentRegistryEntity? =
        agentRegistryDao?.getAgentByIdSync(agentId)

    suspend fun insertAgent(agent: AgentRegistryEntity) {
        agentRegistryDao?.insertAgent(agent)
    }

    suspend fun updateAgent(agent: AgentRegistryEntity) {
        agentRegistryDao?.updateAgent(agent)
    }

    suspend fun deleteAgent(agentId: String) {
        agentRegistryDao?.deleteAgent(agentId)
    }

    // Workspace Onboarding Streams & Operations
    fun getWorkspaceOnboarding(workspaceId: String): Flow<WorkspaceOnboardingEntity?> =
        workspaceOnboardingDao?.getOnboardingByWorkspace(workspaceId) ?: kotlinx.coroutines.flow.flowOf(null)

    fun getAllOnboardings(): Flow<List<WorkspaceOnboardingEntity>> =
        workspaceOnboardingDao?.getAllOnboardings() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun startWorkspaceOnboarding(onboarding: WorkspaceOnboardingEntity) {
        workspaceOnboardingDao?.insertOnboarding(onboarding)
    }

    suspend fun updateOnboardingProgress(onboarding: WorkspaceOnboardingEntity) {
        workspaceOnboardingDao?.updateOnboarding(onboarding)
    }

    suspend fun getOnboardingByWorkspaceSync(workspaceId: String): WorkspaceOnboardingEntity? =
        workspaceOnboardingDao?.getOnboardingByWorkspaceSync(workspaceId)

    suspend fun completeWorkspaceProvisioning(workspaceId: String, onboardingId: String) {
        val existing = workspaceOnboardingDao?.getOnboardingByIdSync(onboardingId)
        if (existing != null) {
            val updated = existing.copy(
                completedTimestamp = System.currentTimeMillis(),
                onboardingStatus = OnboardingStatus.READY.name
            )
            workspaceOnboardingDao.updateOnboarding(updated)
        }
    }

    suspend fun provisionDefaultAgents(
        workspaceId: String,
        industryStr: String,
        selectedPackage: String,
        actor: String
    ): List<AgentRegistryEntity> {
        val industryType = try {
            IndustryType.valueOf(industryStr)
        } catch (e: Exception) {
            IndustryType.values().firstOrNull { it.displayName.contains(industryStr, ignoreCase = true) }
                ?: IndustryType.CUSTOM
        }
        return AgentProvisioningService.provisionAgentsForWorkspace(
            workspaceId = workspaceId,
            industryType = industryType,
            selectedPackageName = selectedPackage,
            createdByActor = actor,
            repository = this
        )
    }

    suspend fun ensureWorkspaceDataSeeded() {
        if (workspaceDao.getWorkspacesCount() == 0) {
            val defaultWorkspace = WorkspaceEntity(
                workspaceId = "ws_default",
                companyName = "RCOS Global Solutions",
                industry = "Enterprise Technology & Advisory",
                domain = "rcos.global",
                primaryBottleneck = "Manual Client Onboarding & Billing Approvals",
                targetReductionPercent = 85,
                activeAgents = "Nova Ops, Executive Copilot, Billing Agent"
            )

            val techCorpWorkspace = WorkspaceEntity(
                workspaceId = "ws_apex_health",
                companyName = "Apex Health Logistics",
                industry = "Healthcare & Pharmaceuticals",
                domain = "apexhealth.org",
                primaryBottleneck = "Patient Billing Verification & Regulatory Sign-offs",
                targetReductionPercent = 90,
                activeAgents = "HIPAA Audit Agent, Billing Verification Copilot"
            )

            val acmeWorkspace = WorkspaceEntity(
                workspaceId = "ws_acme_capital",
                companyName = "Acme Capital Partners",
                industry = "Private Equity & Advisory",
                domain = "acmecapital.com",
                primaryBottleneck = "Due Diligence Document Parsing & Investment Approval",
                targetReductionPercent = 75,
                activeAgents = "Dealflow Parser, Executive Risk Copilot"
            )

            workspaceDao.insertWorkspaces(listOf(defaultWorkspace, techCorpWorkspace, acmeWorkspace))

            val initialUsers = listOf(
                UserAccountEntity(
                    userId = "usr_rcos_ceo",
                    workspaceId = "ws_default",
                    email = "ceo@rcos.global",
                    fullName = "Elena Rostova",
                    role = UserRole.ADMIN.name,
                    accessLevel = AccessLevel.FULL_CONTROL.name,
                    department = "Executive Office",
                    avatarColorHex = "#3B82F6"
                ),
                UserAccountEntity(
                    userId = "usr_rcos_mgr",
                    workspaceId = "ws_default",
                    email = "m.chen@rcos.global",
                    fullName = "Marcus Chen",
                    role = UserRole.MANAGER.name,
                    accessLevel = AccessLevel.WORKFLOW_ADMIN.name,
                    department = "Client Operations",
                    avatarColorHex = "#10B981"
                ),
                UserAccountEntity(
                    userId = "usr_rcos_emp",
                    workspaceId = "ws_default",
                    email = "s.kumar@rcos.global",
                    fullName = "Siddharth Kumar",
                    role = UserRole.EMPLOYEE.name,
                    accessLevel = AccessLevel.REGULAR_STAFF.name,
                    department = "Billing & Support",
                    avatarColorHex = "#F59E0B"
                ),
                UserAccountEntity(
                    userId = "usr_rcos_auditor",
                    workspaceId = "ws_default",
                    email = "audit@pwc.com",
                    fullName = "Sarah Jenkins (Auditor)",
                    role = UserRole.AUDITOR.name,
                    accessLevel = AccessLevel.READ_ONLY.name,
                    department = "External Audit",
                    avatarColorHex = "#8B5CF6"
                ),
                UserAccountEntity(
                    userId = "usr_apex_admin",
                    workspaceId = "ws_apex_health",
                    email = "dr.vance@apexhealth.org",
                    fullName = "Dr. Robert Vance",
                    role = UserRole.ADMIN.name,
                    accessLevel = AccessLevel.FULL_CONTROL.name,
                    department = "Medical Operations",
                    avatarColorHex = "#EC4899"
                )
            )

            workspaceDao.insertUserAccounts(initialUsers)

            val initialConfigs = listOf(
                BusinessAiConfigEntity(
                    workspaceId = "ws_default",
                    customSystemPrompt = "Operate as RCOS Enterprise Copilot. Prioritize workflow speed, autonomous risk checking, and transparent audit logs.",
                    aiAgentTone = "Executive Briefing & Data-Driven",
                    allowedAutoApprovalRiskThreshold = 20,
                    autoApprovalDollarLimit = 5000.0,
                    modelTier = "Gemini Pro 1.5 Enterprise"
                ),
                BusinessAiConfigEntity(
                    workspaceId = "ws_apex_health",
                    customSystemPrompt = "Operate with strict HIPAA compliance guidelines. Flag all patient record modifications for human sign-off.",
                    aiAgentTone = "Clinical & Highly Regulated",
                    allowedAutoApprovalRiskThreshold = 10,
                    autoApprovalDollarLimit = 1000.0,
                    modelTier = "Gemini Pro Healthcare Spec"
                )
            )

            for (cfg in initialConfigs) {
                workspaceDao.saveBusinessAiConfig(cfg)
            }

            val initialAgents = listOf(
                AgentRegistryEntity(
                    agentId = "ag_rcos_exec",
                    workspaceId = "ws_default",
                    agentName = "Executive Risk Copilot",
                    agentDescription = "Evaluates strategic risks, financial authorizations, and high-impact approvals.",
                    agentType = AgentType.EXECUTIVE_AGENT.name,
                    status = AgentStatus.ACTIVE.name,
                    capabilityProfile = "READ_DATA,ANALYZE_DATA,GENERATE_REPORTS,REQUEST_APPROVAL,ACCESS_FINANCIAL_DATA",
                    permissionLevel = AccessLevel.WORKFLOW_ADMIN.name,
                    riskClassification = AgentRiskLevel.HIGH.name,
                    modelTier = "GEMINI_2_5_PRO",
                    createdBy = "SYSTEM",
                    assignedDepartment = "Executive Office"
                ),
                AgentRegistryEntity(
                    agentId = "ag_rcos_ops",
                    workspaceId = "ws_default",
                    agentName = "Nova Operations Engine",
                    agentDescription = "Automates operational workflows, dispatch tasks, and client notification loops.",
                    agentType = AgentType.OPERATIONS_AGENT.name,
                    status = AgentStatus.ACTIVE.name,
                    capabilityProfile = "READ_DATA,EXECUTE_WORKFLOW,CREATE_WORKFLOW,SEND_COMMUNICATION",
                    permissionLevel = AccessLevel.REGULAR_STAFF.name,
                    riskClassification = AgentRiskLevel.LOW.name,
                    modelTier = "GEMINI_2_5_FLASH",
                    createdBy = "SYSTEM",
                    assignedDepartment = "Client Operations"
                ),
                AgentRegistryEntity(
                    agentId = "ag_apex_hipaa",
                    workspaceId = "ws_apex_health",
                    agentName = "HIPAA Compliance Auditor Agent",
                    agentDescription = "Monitors patient record updates and enforces regulatory sign-offs.",
                    agentType = AgentType.COMPLIANCE_AGENT.name,
                    status = AgentStatus.ACTIVE.name,
                    capabilityProfile = "READ_DATA,ANALYZE_DATA,GENERATE_REPORTS,REQUEST_APPROVAL",
                    permissionLevel = AccessLevel.READ_ONLY.name,
                    riskClassification = AgentRiskLevel.CRITICAL.name,
                    modelTier = "GEMINI_2_5_PRO",
                    createdBy = "SYSTEM",
                    assignedDepartment = "Medical Operations"
                )
            )

            for (agent in initialAgents) {
                agentRegistryDao?.insertAgent(agent)
            }
        }
    }

    suspend fun createWorkspace(
        workspace: WorkspaceEntity,
        adminUser: UserAccountEntity,
        aiConfig: BusinessAiConfigEntity
    ) {
        workspaceDao.insertWorkspace(workspace)
        workspaceDao.insertUserAccount(adminUser)
        workspaceDao.saveBusinessAiConfig(aiConfig)
    }

    suspend fun updateWorkspace(workspace: WorkspaceEntity) {
        workspaceDao.insertWorkspace(workspace)
    }

    suspend fun addUserAccount(user: UserAccount) {
        workspaceDao.insertUserAccount(user.toEntity())
    }

    suspend fun updateUserRole(userId: String, role: UserRole, accessLevel: AccessLevel) {
        workspaceDao.updateUserRole(userId, role.name, accessLevel.name)
    }

    suspend fun deleteUserAccount(userId: String) {
        workspaceDao.deleteUserAccount(userId)
    }

    suspend fun saveBusinessAiConfig(config: BusinessAiConfig) {
        workspaceDao.saveBusinessAiConfig(config.toEntity())
    }

    // Persistent Workflow Engine Streams
    val workflowTemplates: Flow<List<WorkflowTemplate>> = workflowDao.getAllWorkflowTemplates()
        .map { list -> list.map { it.toDomain() } }

    val approvalItems: Flow<List<ApprovalItem>> = workflowDao.getAllApprovalItems()
        .map { list -> list.map { it.toDomain() } }

    val aiActionLogs: Flow<List<AiActionLog>> = workflowDao.getAllAiActionLogs()
        .map { list -> list.map { it.toDomain() } }

    val agentResponsibilities: Flow<List<AgentResponsibility>> = workflowDao.getAllAgentResponsibilities()
        .map { list -> list.map { it.toDomain() } }

    val businessWorkflowConfig: Flow<BusinessWorkflowConfig> = workflowDao.getBusinessWorkflowConfig()
        .map { it?.toDomain() ?: BusinessWorkflowConfig() }

    suspend fun ensureWorkflowDataSeeded(
        initialTemplates: List<WorkflowTemplate>,
        initialApprovals: List<ApprovalItem>,
        initialLogs: List<AiActionLog>,
        initialAgents: List<AgentResponsibility>
    ) {
        if (workflowDao.getWorkflowTemplatesCount() == 0) {
            workflowDao.insertWorkflowTemplates(initialTemplates.map { it.toEntity() })
            workflowDao.insertWorkflowTriggers(initialTemplates.map { it.trigger.toEntity() })
            workflowDao.insertApprovalItems(initialApprovals.map { it.toEntity() })
            workflowDao.insertAiActionLogs(initialLogs.map { it.toEntity() })
            workflowDao.insertAgentResponsibilities(initialAgents.map { it.toEntity() })
            if (workflowDao.getBusinessWorkflowConfigDirect() == null) {
                workflowDao.saveBusinessWorkflowConfig(BusinessWorkflowConfig().toEntity())
            }
        }
    }

    suspend fun updateBusinessConfig(config: BusinessWorkflowConfig) {
        workflowDao.saveBusinessWorkflowConfig(config.toEntity())
    }

    suspend fun toggleWorkflowActive(templateId: String) {
        workflowDao.toggleWorkflowActive(templateId)
    }

    suspend fun incrementTemplateExecution(templateId: String) {
        workflowDao.incrementTemplateExecution(templateId)
    }

    suspend fun addApprovalItem(item: ApprovalItem) {
        workflowDao.insertApprovalItem(item.toEntity())
    }

    suspend fun addAiActionLog(log: AiActionLog) {
        workflowDao.insertAiActionLog(log.toEntity())
    }

    suspend fun updateApprovalStatus(id: String, status: ApprovalStatus, reviewedBy: String?, notes: String?) {
        workflowDao.updateApprovalStatus(id, status.name, reviewedBy, notes)
    }

    suspend fun addWorkflowTemplate(template: WorkflowTemplate) {
        workflowDao.insertWorkflowTemplate(template.toEntity())
        workflowDao.insertWorkflowTrigger(template.trigger.toEntity())
    }

    suspend fun updateAgentAutonomy(agentId: String, newAutonomy: String) {
        workflowDao.updateAgentAutonomy(agentId, newAutonomy)
    }

    suspend fun saveDashboardItem(
        title: String,
        category: String,
        content: String,
        itemType: String = "INSIGHT"
    ): Long {
        val item = DashboardItem(
            title = title,
            category = category,
            content = content,
            itemType = itemType
        )
        return dao.insertDashboardItem(item)
    }

    suspend fun togglePinItem(id: Int, currentPinned: Boolean) {
        dao.updatePinnedStatus(id, !currentPinned)
    }

    suspend fun deleteDashboardItem(id: Int) {
        dao.deleteDashboardItemById(id)
    }

    fun getChatMessages(sessionId: String): Flow<List<ChatMessageEntity>> {
        return dao.getMessagesForSession(sessionId)
    }

    suspend fun createOrUpdateSession(session: ChatSessionEntity) {
        dao.insertChatSession(session)
    }

    suspend fun addChatMessage(sessionId: String, role: String, text: String) {
        dao.insertChatMessage(
            ChatMessageEntity(
                sessionId = sessionId,
                role = role,
                text = text
            )
        )
    }

    suspend fun deleteChatSession(sessionId: String) {
        dao.deleteMessagesForSession(sessionId)
        dao.deleteChatSession(sessionId)
    }

    val companyProfile: Flow<CompanyProfileEntity?> = dao.getCompanyProfile()

    suspend fun registerUser(
        email: String,
        fullName: String,
        passwordRaw: String,
        companyName: String,
        industry: String
    ): Result<UserEntity> {
        return try {
            val existing = dao.getUserByEmail(email.trim().lowercase())
            if (existing != null) {
                return Result.failure(Exception("An account with this email already exists."))
            }

            val salt = SecurityUtils.generateSalt()
            val hash = SecurityUtils.hashPassword(passwordRaw, salt)

            val newUser = UserEntity(
                email = email.trim().lowercase(),
                fullName = fullName.trim(),
                passwordHash = hash,
                salt = salt,
                companyName = companyName.trim(),
                industry = industry.trim()
            )

            dao.insertUser(newUser)

            // Auto-provision initial company profile if none exists
            val currentProfile = dao.getCompanyProfileDirect()
            if (currentProfile == null) {
                dao.saveCompanyProfile(
                    CompanyProfileEntity(
                        companyName = companyName.ifBlank { "Enterprise Client" },
                        industry = industry.ifBlank { "Cross-Industry Workload" },
                        primaryBottleneck = "Executive Reporting & Task Allocation",
                        targetReductionPercent = 45,
                        activeAgents = "Workload Synthesizer, Onboarding Specialist, Workflow Automator, Strategic Analyst",
                        customInstructions = "Optimize organizational throughput and automate repetitive email/document processing.",
                        isConfigured = false
                    )
                )
            }

            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, passwordRaw: String): Result<UserEntity> {
        return try {
            val user = dao.getUserByEmail(email.trim().lowercase())
                ?: return Result.failure(Exception("No user account found with this email."))

            val isValid = SecurityUtils.verifyPassword(passwordRaw, user.salt, user.passwordHash)
            if (!isValid) {
                return Result.failure(Exception("Invalid password. Please check your credentials."))
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserByEmail(email: String): UserEntity? {
        return dao.getUserByEmail(email.trim().lowercase())
    }

    suspend fun saveCompanyProfile(profile: CompanyProfileEntity) {
        dao.saveCompanyProfile(profile)
    }

    // Gemini network requests delegation
    suspend fun analyzeText(prompt: String, systemInstruction: String? = null): Result<String> {
        return GeminiClient.generateText(prompt, systemInstruction)
    }

    suspend fun sendChatMessage(
        history: List<Pair<String, String>>,
        userMessage: String,
        personaInstruction: String
    ): Result<String> {
        return GeminiClient.generateChat(history, userMessage, personaInstruction)
    }

    suspend fun transcribeVoice(audioFile: File, mimeType: String = "audio/wav"): Result<String> {
        return GeminiClient.transcribeAudio(audioFile, mimeType)
    }

    suspend fun runDeepReasoning(prompt: String): Result<String> {
        return GeminiClient.deepReasoning(prompt)
    }

    suspend fun executeJobWithGemini(jobTitle: String, clientName: String, summary: String, agentName: String, priority: String): Result<String> {
        return GeminiClient.executeJobTask(jobTitle, clientName, summary, agentName, priority)
    }

    suspend fun analyzeClientWithGemini(clientName: String, company: String, status: String, notes: String, totalValue: Double): Result<String> {
        return GeminiClient.analyzeClientIntelligence(clientName, company, status, notes, totalValue)
    }

    suspend fun draftClientProposalWithGemini(clientName: String, company: String, purpose: String, context: String): Result<String> {
        return GeminiClient.draftClientCommunication(clientName, company, purpose, context)
    }

    suspend fun analyzePhoneCallWithGemini(caller: String, duration: String, callContext: String): Result<String> {
        return GeminiClient.analyzePhoneCall(caller, duration, callContext)
    }

    suspend fun generateMeetingBriefingWithGemini(title: String, attendees: String, time: String, context: String): Result<String> {
        return GeminiClient.generateMeetingBriefing(title, attendees, time, context)
    }

    suspend fun synthesizeWorkflowWithGemini(description: String, industry: String): Result<String> {
        return GeminiClient.synthesizeWorkflowFromPrompt(description, industry)
    }

    suspend fun runSecurityAuditWithGemini(logsSummary: String, workspaceName: String): Result<String> {
        return GeminiClient.runSecurityAudit(logsSummary, workspaceName)
    }

    suspend fun synthesizeExecutiveBriefingWithGemini(items: List<String>, companyName: String): Result<String> {
        return GeminiClient.synthesizeExecutiveBriefing(items, companyName)
    }

    suspend fun consultAgentWithGemini(agentName: String, agentRole: String, capabilities: String, query: String): Result<String> {
        return GeminiClient.consultAgentPersona(agentName, agentRole, capabilities, query)
    }

    suspend fun generateExecutiveScheduleBriefingWithGemini(eventsSummary: String): Result<String> {
        return GeminiClient.generateText(
            prompt = "Provide a high-priority executive briefing for today's schedule and appointments:\n$eventsSummary\nHighlight key meeting objectives, assigned AI agents, and critical preparation steps.",
            systemInstruction = "You are an executive AI briefing assistant for enterprise business leaders. Be sharp, actionable, and structured."
        )
    }

    suspend fun optimizeWorkflowPipelineWithGemini(pipelineDescription: String): Result<String> {
        return GeminiClient.generateText(
            prompt = "Analyze this enterprise workflow pipeline and provide optimization recommendations:\n$pipelineDescription\nInclude bottlenecks, latency reductions, autonomous agent handoff enhancements, and security controls.",
            systemInstruction = "You are an enterprise autonomous workflow optimization architect. Provide high-impact recommendations."
        )
    }
}
