package com.example.data

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class FirestoreSyncState {
    CONNECTED,
    SYNCING,
    OFFLINE_PERSISTENCE,
    INITIALIZING,
    ERROR
}

data class FirestoreSyncInfo(
    val state: FirestoreSyncState = FirestoreSyncState.INITIALIZING,
    val statusMessage: String = "Connecting to Firestore...",
    val lastSyncedTime: Long = System.currentTimeMillis(),
    val pendingSyncCount: Int = 0,
    val activeDevicesCount: Int = 1,
    val collectionName: String = "rcos_agent_tasks"
)

class FirestoreTaskSyncManager(
    private val scope: CoroutineScope
) {
    private val tag = "FirestoreTaskSync"

    private val _syncInfo = MutableStateFlow(FirestoreSyncInfo())
    val syncInfo: StateFlow<FirestoreSyncInfo> = _syncInfo.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private var tasksListenerRegistration: ListenerRegistration? = null
    private var approvalsListenerRegistration: ListenerRegistration? = null
    private var agentsListenerRegistration: ListenerRegistration? = null

    init {
        initializeFirestore()
    }

    private fun initializeFirestore() {
        try {
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .build()
            db.firestoreSettings = settings
            firestore = db
            _syncInfo.value = FirestoreSyncInfo(
                state = FirestoreSyncState.CONNECTED,
                statusMessage = "Real-time Multi-Device Sync Active",
                lastSyncedTime = System.currentTimeMillis()
            )
            Log.d(tag, "Firebase Firestore initialized successfully with offline persistence.")
        } catch (t: Throwable) {
            Log.w(tag, "Firebase Firestore init exception (using offline cache fallback): ${t.message}")
            _syncInfo.value = FirestoreSyncInfo(
                state = FirestoreSyncState.OFFLINE_PERSISTENCE,
                statusMessage = "Offline Local Persistence Active"
            )
        }
    }

    /**
     * Start real-time snapshot listener on Firestore tasks collection.
     * When any device modifies a task status (Pending -> Completed -> Archived),
     * this listener immediately fires with the updated task list.
     */
    fun startRealtimeTasksListener(
        onTasksUpdated: (List<JobEntity>) -> Unit
    ) {
        val db = firestore ?: return
        try {
            tasksListenerRegistration?.remove()
            tasksListenerRegistration = db.collection("rcos_agent_tasks")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(tag, "Firestore tasks snapshot error: ${error.message}")
                        _syncInfo.value = _syncInfo.value.copy(
                            state = FirestoreSyncState.OFFLINE_PERSISTENCE,
                            statusMessage = "Offline Cache (Cloud stream paused)"
                        )
                        return@addSnapshotListener
                    }

                    if (snapshots != null && !snapshots.isEmpty) {
                        val updatedJobs = snapshots.documents.mapNotNull { doc ->
                            documentToJobEntity(doc)
                        }
                        _syncInfo.value = _syncInfo.value.copy(
                            state = FirestoreSyncState.CONNECTED,
                            statusMessage = "Real-time Cloud Synced (${updatedJobs.size} tasks)",
                            lastSyncedTime = System.currentTimeMillis()
                        )
                        onTasksUpdated(updatedJobs)
                    }
                }
        } catch (t: Throwable) {
            Log.e(tag, "Failed to attach real-time tasks listener", t)
        }
    }

    /**
     * Start real-time snapshot listener on Firestore approvals collection.
     */
    fun startRealtimeApprovalsListener(
        onApprovalsUpdated: (List<ApprovalItem>) -> Unit
    ) {
        val db = firestore ?: return
        try {
            approvalsListenerRegistration?.remove()
            approvalsListenerRegistration = db.collection("rcos_agent_approvals")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(tag, "Firestore approvals snapshot error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null && !snapshots.isEmpty) {
                        val updatedApprovals = snapshots.documents.mapNotNull { doc ->
                            documentToApprovalItem(doc)
                        }
                        onApprovalsUpdated(updatedApprovals)
                    }
                }
        } catch (t: Throwable) {
            Log.e(tag, "Failed to attach real-time approvals listener", t)
        }
    }

    /**
     * Push a task update (or creation) to Firestore in real time.
     */
    fun pushTaskUpdate(job: JobEntity) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                _syncInfo.value = _syncInfo.value.copy(
                    state = FirestoreSyncState.SYNCING,
                    statusMessage = "Broadcasting ${job.id} update..."
                )
                val docData = hashMapOf(
                    "id" to job.id,
                    "title" to job.title,
                    "clientName" to job.clientName,
                    "status" to job.status,
                    "assignedAgent" to job.assignedAgent,
                    "priority" to job.priority,
                    "dueDate" to job.dueDate,
                    "approvalRequired" to job.approvalRequired,
                    "isApproved" to job.isApproved,
                    "progress" to job.progress.toDouble(),
                    "summary" to job.summary,
                    "lastUpdatedTimestamp" to System.currentTimeMillis()
                )
                db.collection("rcos_agent_tasks")
                    .document(job.id.replace("#", "").replace("/", "_"))
                    .set(docData, SetOptions.merge())
                    .await()

                _syncInfo.value = _syncInfo.value.copy(
                    state = FirestoreSyncState.CONNECTED,
                    statusMessage = "Real-time Multi-Device Sync Active",
                    lastSyncedTime = System.currentTimeMillis()
                )
                Log.d(tag, "Synced task ${job.id} to Firestore successfully.")
            } catch (t: Throwable) {
                Log.w(tag, "Failed to push task update to Firestore: ${t.message}")
                _syncInfo.value = _syncInfo.value.copy(
                    state = FirestoreSyncState.OFFLINE_PERSISTENCE,
                    statusMessage = "Saved Locally (Will sync when online)"
                )
            }
        }
    }

    /**
     * Push an approval update to Firestore in real time.
     */
    fun pushApprovalUpdate(item: ApprovalItem) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val docData = hashMapOf(
                    "id" to item.id,
                    "workflowTitle" to item.workflowTitle,
                    "requestedByAgent" to item.requestedByAgent,
                    "triggerSource" to item.triggerSource,
                    "timestamp" to item.timestamp,
                    "riskScorePct" to item.riskScorePct,
                    "summary" to item.summary,
                    "proposedAction" to item.proposedAction,
                    "status" to item.status.name,
                    "reviewedBy" to (item.reviewedBy ?: ""),
                    "reviewNotes" to (item.reviewNotes ?: ""),
                    "lastUpdatedTimestamp" to System.currentTimeMillis()
                )
                db.collection("rcos_agent_approvals")
                    .document(item.id.replace("#", "").replace("/", "_"))
                    .set(docData, SetOptions.merge())
                    .await()
                Log.d(tag, "Synced approval ${item.id} to Firestore successfully.")
            } catch (t: Throwable) {
                Log.w(tag, "Failed to push approval update to Firestore: ${t.message}")
            }
        }
    }

    /**
     * Initial seed/sync of local tasks to Firestore if cloud collection is newly provisioned.
     */
    fun seedTasksToFirestore(jobs: List<JobEntity>) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val batch = db.batch()
                jobs.forEach { job ->
                    val docRef = db.collection("rcos_agent_tasks")
                        .document(job.id.replace("#", "").replace("/", "_"))
                    val docData = hashMapOf(
                        "id" to job.id,
                        "title" to job.title,
                        "clientName" to job.clientName,
                        "status" to job.status,
                        "assignedAgent" to job.assignedAgent,
                        "priority" to job.priority,
                        "dueDate" to job.dueDate,
                        "approvalRequired" to job.approvalRequired,
                        "isApproved" to job.isApproved,
                        "progress" to job.progress.toDouble(),
                        "summary" to job.summary,
                        "lastUpdatedTimestamp" to System.currentTimeMillis()
                    )
                    batch.set(docRef, docData, SetOptions.merge())
                }
                batch.commit().await()
                Log.d(tag, "Seeded ${jobs.size} initial tasks to Firestore.")
            } catch (t: Throwable) {
                Log.w(tag, "Error during initial Firestore task batch sync: ${t.message}")
            }
        }
    }

    /**
     * Initial seed/sync of approvals to Firestore.
     */
    fun seedApprovalsToFirestore(items: List<ApprovalItem>) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val batch = db.batch()
                items.forEach { item ->
                    val docRef = db.collection("rcos_agent_approvals")
                        .document(item.id.replace("#", "").replace("/", "_"))
                    val docData = hashMapOf(
                        "id" to item.id,
                        "workflowTitle" to item.workflowTitle,
                        "requestedByAgent" to item.requestedByAgent,
                        "triggerSource" to item.triggerSource,
                        "timestamp" to item.timestamp,
                        "riskScorePct" to item.riskScorePct,
                        "summary" to item.summary,
                        "proposedAction" to item.proposedAction,
                        "status" to item.status.name,
                        "reviewedBy" to (item.reviewedBy ?: ""),
                        "reviewNotes" to (item.reviewNotes ?: ""),
                        "lastUpdatedTimestamp" to System.currentTimeMillis()
                    )
                    batch.set(docRef, docData, SetOptions.merge())
                }
                batch.commit().await()
                Log.d(tag, "Seeded ${items.size} approvals to Firestore.")
            } catch (t: Throwable) {
                Log.w(tag, "Error during initial Firestore approvals batch sync: ${t.message}")
            }
        }
    }

    /**
     * Start real-time snapshot listener on Firestore autonomous agents collection.
     * When any active autonomous agent status or configuration changes in Firestore,
     * this listener immediately emits the updated agent list.
     */
    fun startRealtimeAgentsListener(
        onAgentsUpdated: (List<AgentRegistryEntity>) -> Unit
    ) {
        val db = firestore ?: return
        try {
            agentsListenerRegistration?.remove()
            agentsListenerRegistration = db.collection("rcos_autonomous_agents")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(tag, "Firestore agents snapshot error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null && !snapshots.isEmpty) {
                        val updatedAgents = snapshots.documents.mapNotNull { doc ->
                            documentToAgentEntity(doc)
                        }
                        onAgentsUpdated(updatedAgents)
                    }
                }
        } catch (t: Throwable) {
            Log.e(tag, "Failed to attach real-time agents listener", t)
        }
    }

    /**
     * Retrieve active autonomous agents directly from Firestore 'agents' collection.
     */
    suspend fun fetchActiveAgentsFromFirestore(): List<AgentRegistryEntity> {
        val db = firestore ?: return emptyList()
        return try {
            val snapshot = db.collection("agents")
                .get()
                .await()
            val agentsList = snapshot.documents.mapNotNull { documentToAgentEntity(it) }
            if (agentsList.isNotEmpty()) {
                agentsList.filter { it.status.equals("ACTIVE", ignoreCase = true) }
            } else {
                val rcosSnapshot = db.collection("rcos_autonomous_agents").get().await()
                rcosSnapshot.documents.mapNotNull { documentToAgentEntity(it) }
                    .filter { it.status.equals("ACTIVE", ignoreCase = true) }
            }
        } catch (t: Throwable) {
            Log.w(tag, "Failed to fetch active agents from Firestore: ${t.message}")
            emptyList()
        }
    }

    /**
     * Add a newly defined agent directly to the Firestore 'agents' collection.
     */
    suspend fun addAgentToFirestore(agent: AgentRegistryEntity, objective: String): Boolean {
        val db = firestore ?: return false
        return try {
            val docData = hashMapOf(
                "agentId" to agent.agentId,
                "workspaceId" to agent.workspaceId,
                "agentName" to agent.agentName,
                "agentDescription" to objective,
                "objective" to objective,
                "agentType" to agent.agentType,
                "status" to agent.status,
                "capabilityProfile" to agent.capabilityProfile,
                "permissionLevel" to agent.permissionLevel,
                "riskClassification" to agent.riskClassification,
                "modelTier" to agent.modelTier,
                "createdBy" to agent.createdBy,
                "createdTimestamp" to agent.createdTimestamp,
                "updatedTimestamp" to System.currentTimeMillis(),
                "assignedDepartment" to (agent.assignedDepartment ?: "Operations"),
                "assignedUsers" to (agent.assignedUsers ?: "")
            )
            val docId = agent.agentId.replace("#", "").replace("/", "_")
            db.collection("agents")
                .document(docId)
                .set(docData, SetOptions.merge())
                .await()
            db.collection("rcos_autonomous_agents")
                .document(docId)
                .set(docData, SetOptions.merge())
            Log.d(tag, "Added agent ${agent.agentName} with objective to Firestore 'agents' collection.")
            true
        } catch (t: Throwable) {
            Log.w(tag, "Failed to add agent to Firestore 'agents': ${t.message}")
            false
        }
    }

    /**
     * Push or update an autonomous agent document in Firestore 'agents' collection.
     */
    fun pushAgentUpdate(agent: AgentRegistryEntity) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val docData = hashMapOf(
                    "agentId" to agent.agentId,
                    "workspaceId" to agent.workspaceId,
                    "agentName" to agent.agentName,
                    "agentDescription" to agent.agentDescription,
                    "objective" to agent.agentDescription,
                    "agentType" to agent.agentType,
                    "status" to agent.status,
                    "capabilityProfile" to agent.capabilityProfile,
                    "permissionLevel" to agent.permissionLevel,
                    "riskClassification" to agent.riskClassification,
                    "modelTier" to agent.modelTier,
                    "createdBy" to agent.createdBy,
                    "createdTimestamp" to agent.createdTimestamp,
                    "updatedTimestamp" to System.currentTimeMillis(),
                    "assignedDepartment" to (agent.assignedDepartment ?: "Operations"),
                    "assignedUsers" to (agent.assignedUsers ?: "")
                )
                val docId = agent.agentId.replace("#", "").replace("/", "_")
                db.collection("agents")
                    .document(docId)
                    .set(docData, SetOptions.merge())
                    .await()
                db.collection("rcos_autonomous_agents")
                    .document(docId)
                    .set(docData, SetOptions.merge())
                Log.d(tag, "Synced agent ${agent.agentName} to Firestore successfully.")
            } catch (t: Throwable) {
                Log.w(tag, "Failed to push agent update to Firestore: ${t.message}")
            }
        }
    }

    /**
     * Seed or sync autonomous agents to Firestore in batch.
     */
    fun seedAgentsToFirestore(agents: List<AgentRegistryEntity>) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val batch = db.batch()
                agents.forEach { agent ->
                    val docId = agent.agentId.replace("#", "").replace("/", "_")
                    val docRef = db.collection("agents").document(docId)
                    val docData = hashMapOf(
                        "agentId" to agent.agentId,
                        "workspaceId" to agent.workspaceId,
                        "agentName" to agent.agentName,
                        "agentDescription" to agent.agentDescription,
                        "objective" to agent.agentDescription,
                        "agentType" to agent.agentType,
                        "status" to agent.status,
                        "capabilityProfile" to agent.capabilityProfile,
                        "permissionLevel" to agent.permissionLevel,
                        "riskClassification" to agent.riskClassification,
                        "modelTier" to agent.modelTier,
                        "createdBy" to agent.createdBy,
                        "createdTimestamp" to agent.createdTimestamp,
                        "updatedTimestamp" to System.currentTimeMillis(),
                        "assignedDepartment" to (agent.assignedDepartment ?: "Operations"),
                        "assignedUsers" to (agent.assignedUsers ?: "")
                    )
                    batch.set(docRef, docData, SetOptions.merge())
                }
                batch.commit().await()
                Log.d(tag, "Seeded ${agents.size} autonomous agents to Firestore.")
            } catch (t: Throwable) {
                Log.w(tag, "Error during Firestore agents batch sync: ${t.message}")
            }
        }
    }

    fun cleanup() {
        tasksListenerRegistration?.remove()
        approvalsListenerRegistration?.remove()
        agentsListenerRegistration?.remove()
    }

    private fun documentToJobEntity(doc: DocumentSnapshot): JobEntity? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val title = doc.getString("title") ?: return null
            val clientName = doc.getString("clientName") ?: "Client"
            val status = doc.getString("status") ?: "Pending"
            val assignedAgent = doc.getString("assignedAgent") ?: "Agent"
            val priority = doc.getString("priority") ?: "Normal"
            val dueDate = doc.getString("dueDate") ?: "Today"
            val approvalRequired = doc.getBoolean("approvalRequired") ?: false
            val isApproved = doc.getBoolean("isApproved") ?: (status == "Completed")
            val progress = doc.getDouble("progress")?.toFloat() ?: (if (status == "Completed") 1.0f else 0.5f)
            val summary = doc.getString("summary") ?: ""

            JobEntity(
                id = if (id.startsWith("#")) id else "#$id",
                title = title,
                clientName = clientName,
                status = status,
                assignedAgent = assignedAgent,
                priority = priority,
                dueDate = dueDate,
                approvalRequired = approvalRequired,
                isApproved = isApproved,
                progress = progress,
                summary = summary
            )
        } catch (e: Exception) {
            Log.w(tag, "Error parsing Firestore task document: ${e.message}")
            null
        }
    }

    private fun documentToApprovalItem(doc: DocumentSnapshot): ApprovalItem? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val workflowTitle = doc.getString("workflowTitle") ?: return null
            val triggerSource = doc.getString("triggerSource") ?: "Trigger"
            val summary = doc.getString("summary") ?: ""
            val proposedAction = doc.getString("proposedAction") ?: ""
            val riskScorePct = doc.getLong("riskScorePct")?.toInt() ?: 20
            val statusStr = doc.getString("status") ?: "PENDING"
            val status = try {
                ApprovalStatus.valueOf(statusStr)
            } catch (e: Exception) {
                ApprovalStatus.PENDING
            }
            val timestamp = doc.getString("timestamp") ?: "Just now"
            val reviewedBy = doc.getString("reviewedBy")
            val reviewNotes = doc.getString("reviewNotes")
            val requestedByAgent = doc.getString("requestedByAgent") ?: "AI Agent"

            ApprovalItem(
                id = id,
                workflowTitle = workflowTitle,
                requestedByAgent = requestedByAgent,
                triggerSource = triggerSource,
                timestamp = timestamp,
                riskScorePct = riskScorePct,
                summary = summary,
                proposedAction = proposedAction,
                status = status,
                reviewedBy = reviewedBy,
                reviewNotes = reviewNotes
            )
        } catch (e: Exception) {
            Log.w(tag, "Error parsing Firestore approval document: ${e.message}")
            null
        }
    }

    private fun documentToAgentEntity(doc: DocumentSnapshot): AgentRegistryEntity? {
        return try {
            val agentId = doc.getString("agentId") ?: doc.id
            val workspaceId = doc.getString("workspaceId") ?: "ws_default"
            val agentName = doc.getString("agentName") ?: return null
            val agentDescription = doc.getString("agentDescription") ?: "Autonomous Enterprise Agent"
            val agentType = doc.getString("agentType") ?: AgentType.OPERATIONS_AGENT.name
            val status = doc.getString("status") ?: AgentStatus.ACTIVE.name
            val capabilityProfile = doc.getString("capabilityProfile") ?: ""
            val permissionLevel = doc.getString("permissionLevel") ?: AccessLevel.READ_ONLY.name
            val riskClassification = doc.getString("riskClassification") ?: AgentRiskLevel.LOW.name
            val modelTier = doc.getString("modelTier") ?: "GEMINI_2_5_FLASH"
            val createdBy = doc.getString("createdBy") ?: "SYSTEM"
            val createdTimestamp = doc.getLong("createdTimestamp") ?: System.currentTimeMillis()
            val updatedTimestamp = doc.getLong("updatedTimestamp") ?: System.currentTimeMillis()
            val assignedDepartment = doc.getString("assignedDepartment") ?: "Operations"
            val assignedUsers = doc.getString("assignedUsers")

            AgentRegistryEntity(
                agentId = agentId,
                workspaceId = workspaceId,
                agentName = agentName,
                agentDescription = agentDescription,
                agentType = agentType,
                status = status,
                capabilityProfile = capabilityProfile,
                permissionLevel = permissionLevel,
                riskClassification = riskClassification,
                modelTier = modelTier,
                createdBy = createdBy,
                createdTimestamp = createdTimestamp,
                updatedTimestamp = updatedTimestamp,
                assignedDepartment = assignedDepartment,
                assignedUsers = assignedUsers
            )
        } catch (e: Exception) {
            Log.w(tag, "Error parsing Firestore agent document: ${e.message}")
            null
        }
    }
}
