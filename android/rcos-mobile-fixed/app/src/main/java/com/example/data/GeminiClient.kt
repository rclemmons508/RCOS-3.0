package com.example.data

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    // Cloudflare Worker backend. The Gemini API key remains server-side.
    private const val BACKEND_URL =
        "https://rcos-ai-gateway.rclemmons1021.workers.dev"
    private const val DEFAULT_TEXT_MODEL = "gemini-3.7-flash"
    private const val DEFAULT_REASONING_MODEL = "gemini-3.7-flash"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()


    /**
     * Generate content for general text analysis, summarization, or query tasks.
     * Model default: gemini-3.7-flash
     */
    private suspend fun authHeaders(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                ?: return@withContext Result.failure(
                    IllegalStateException("Please sign in before using Nova AI.")
                )
            val token = user.getIdToken(false).await().token
                ?: return@withContext Result.failure(
                    IllegalStateException("Unable to obtain Firebase authentication token.")
                )
            Result.success(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to obtain Firebase auth token", e)
            Result.failure(e)
        }
    }

    private suspend fun callBackend(payload: JSONObject): Result<String> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = authHeaders()
            val token = tokenResult.getOrElse { return@withContext Result.failure(it) }

            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BACKEND_URL)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Backend API error (${response.code}): $responseBody")
                    return@withContext Result.failure(
                        Exception("Backend HTTP ${response.code}: ${parseBackendError(responseBody)}")
                    )
                }

                val json = JSONObject(responseBody)
                if (!json.optBoolean("success", false)) {
                    return@withContext Result.failure(
                        Exception(json.optString("error", "Backend request failed"))
                    )
                }

                Result.success(json.optString("text", "No response text received."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backend request failed", e)
            Result.failure(e)
        }
    }

    private fun parseBackendError(body: String): String {
        return try {
            JSONObject(body).optString("error", body)
        } catch (e: Exception) {
            body
        }
    }

    /**
     * Generate content through the RCOS backend.
     * The Gemini API key is never shipped in the Android APK.
     */
    suspend fun generateText(
        prompt: String,
        systemInstruction: String? = null,
        model: String = DEFAULT_TEXT_MODEL
    ): Result<String> {
        val payload = JSONObject().apply {
            put("operation", "generateText")
            put("prompt", prompt)
            put("model", normalizeModel(model))
            if (!systemInstruction.isNullOrBlank()) {
                put("systemInstruction", systemInstruction)
            }
        }
        return callBackend(payload)
    }

    /**
     * Multi-turn chat through the RCOS backend.
     */
    suspend fun generateChat(
        history: List<Pair<String, String>>,
        userMessage: String,
        systemInstruction: String = "You are Nova AI, an executive AI assistant.",
        model: String = DEFAULT_TEXT_MODEL
    ): Result<String> {
        val historyArray = JSONArray()
        history.forEach { (role, text) ->
            historyArray.put(JSONObject().apply {
                put("role", if (role.equals("user", true)) "user" else "model")
                put("text", text)
            })
        }

        val payload = JSONObject().apply {
            put("operation", "generateChat")
            put("history", historyArray)
            put("userMessage", userMessage)
            put("systemInstruction", systemInstruction)
            put("model", normalizeModel(model))
        }
        return callBackend(payload)
    }

    /**
     * Transcribe/analyze an audio recording through the RCOS backend.
     */
    suspend fun transcribeAudio(
        audioFile: File,
        mimeType: String = "audio/wav"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val audioBytes = audioFile.readBytes()
            val base64Data = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            val payload = JSONObject().apply {
                put("operation", "transcribeAudio")
                put("model", DEFAULT_TEXT_MODEL)
                put("prompt", "Please provide a verbatim transcription of this audio recording. If there are key points, include a brief summary below the transcript.")
                put("mimeType", mimeType)
                put("audioBase64", base64Data)
            }
            callBackend(payload)
        } catch (e: Exception) {
            Log.e(TAG, "Failed transcribeAudio", e)
            Result.failure(e)
        }
    }

    /**
     * Deep reasoning through the RCOS backend.
     */
    suspend fun deepReasoning(
        prompt: String,
        systemInstruction: String = "You are a master strategist and analytical thinker. Break down complex queries into thorough reasoning steps and clear actionable conclusions."
    ): Result<String> {
        val payload = JSONObject().apply {
            put("operation", "deepReasoning")
            put("model", DEFAULT_REASONING_MODEL)
            put("prompt", prompt)
            put("systemInstruction", systemInstruction)
        }
        return callBackend(payload)
    }

    suspend fun executeJobTask(
        jobTitle: String,
        clientName: String,
        summary: String,
        agentName: String,
        priority: String
    ): Result<String> {
        val prompt = """
            As autonomous enterprise agent '$agentName', execute and complete the following assigned corporate task:
            - Task Title: $jobTitle
            - Client / Account: $clientName
            - Priority Level: $priority
            - Current Context: $summary

            Provide an executive-level resolution artifact:
            1. Execution Summary: What concrete actions were performed.
            2. Deliverables Produced: Key assets, documents, or data synthesized.
            3. Risk & Compliance Verification: Confirm adherence to organizational guidelines.
            4. Recommended Next Steps for Executive Review.
        """.trimIndent()
        return generateText(
            prompt = prompt,
            systemInstruction = "You are $agentName, an elite enterprise autonomous AI agent executing corporate workflows with precision, rigorous compliance, and actionable deliverables.",
            model = "gemini-3.7-flash"
        )
    }

    /**
     * Analyze client relationship and synthesize account intelligence using Gemini AI.
     */
    suspend fun analyzeClientIntelligence(
        clientName: String,
        company: String,
        status: String,
        notes: String,
        totalValue: Double
    ): Result<String> {
        val prompt = """
            Perform a strategic executive intelligence evaluation for this client account:
            - Client Name: $clientName
            - Company: $company
            - Account Status: $status
            - Current Contract Value: $$totalValue
            - Operational Notes & History: $notes

            Synthesize:
            1. Account Health & Retention Score (1-100) with justification.
            2. Primary Value Bottlenecks & Expansion Opportunities.
            3. High-Probability Next Action for Account Growth.
            4. 1-Paragraph Executive Talking Points for upcoming touchpoints.
        """.trimIndent()
        return generateText(
            prompt = prompt,
            systemInstruction = "You are an Executive Account Intelligence AI specialist focusing on enterprise retention, value expansion, and client strategy.",
            model = "gemini-3.7-flash"
        )
    }

    /**
     * Draft high-impact personalized client proposal or communication using Gemini AI.
     */
    suspend fun draftClientCommunication(
        clientName: String,
        company: String,
        purpose: String,
        context: String
    ): Result<String> {
        val prompt = """
            Draft a personalized, high-caliber executive communication to:
            - Recipient: $clientName at $company
            - Communication Goal: $purpose
            - Account Context: $context

            Format the communication cleanly with:
            - Subject Line
            - Executive Greeting & Context
            - Clear Value Proposition & Action Items
            - Frictionless Call-to-Action
        """.trimIndent()
        return generateText(
            prompt = prompt,
            systemInstruction = "You are an Executive Communications Strategist drafting polished, concise, and persuasive business communications.",
            model = "gemini-3.7-flash"
        )
    }

    /**
     * Analyze Phone Call / Voicemail transcript and extract action items using Gemini AI.
     */
    suspend fun analyzePhoneCall(
        caller: String,
        duration: String,
        callContext: String
    ): Result<String> {
        val prompt = """
            Analyze the following inbound phone communication:
            - Caller: $caller
            - Duration: $duration
            - Call Notes / Transcript: $callContext

            Synthesize:
            1. Executive Call Summary (2-3 sentences).
            2. Caller Sentiment: [POSITIVE, NEUTRAL, URGENT, or AT-RISK] with explanation.
            3. Action Items & Commitments Made.
            4. Suggested Immediate Reply (SMS/Email text).
        """.trimIndent()
        return generateText(
            prompt = prompt,
            systemInstruction = "You are an AI Telephony & Communications Intelligence Specialist summarizing calls and determining urgency.",
            model = "gemini-3.7-flash"
        )
    }

    /**
     * Generate an Executive Meeting Preparation Briefing using Gemini AI.
     */
    suspend fun generateMeetingBriefing(
        title: String,
        attendees: String,
        time: String,
        meetingContext: String
    ): Result<String> {
        val prompt = """
            Prepare a comprehensive Executive Meeting Prep Dossier for:
            - Meeting Title: $title
            - Time & Date: $time
            - Key Attendees: $attendees
            - Agenda & Context: $meetingContext

            Provide:
            1. Strategic Objective of this Meeting.
            2. Recommended Agenda Allocation (15/30/45 min breakdown).
            3. Key Stakeholder Motivations & Potential Friction Points.
            4. 3 Core Talking Points & Decision Criteria to achieve alignment.
        """.trimIndent()
        return generateText(
            prompt = prompt,
            systemInstruction = "You are an Executive Chief of Staff preparing leaders for high-stakes business meetings and strategy sessions.",
            model = "gemini-3.7-flash"
        )
    }

    /**
     * Synthesize and generate an enterprise automated workflow from natural language using Gemini AI.
     */
    suspend fun synthesizeWorkflowFromPrompt(
        description: String,
        industry: String
    ): Result<String> {
        val prompt = """
            Synthesize an end-to-end autonomous business workflow pipeline for the $industry industry based on this requirement:
            "$description"

            Provide:
            1. Workflow Title & Category.
            2. Primary Trigger (e.g., Inbound Voice, Email OCR, Webhook, Schedule, Anomaly Detection).
            3. 4 Detailed Execution Steps (with step name, action type, and assigned agent type).
            4. Risk Assessment (0-100% risk score) and Recommended Human-in-the-loop Approval Policy.
            5. Expected Workload Reduction Percentage.
        """.trimIndent()
        return generateText(
            prompt = prompt,
            systemInstruction = "You are Nova Workflow Architect, an AI system architect specializing in enterprise automation and governance.",
            model = "gemini-3.1-pro-preview"
        )
    }

    /**
     * Run an Anomaly and Compliance Security Audit over logs using Gemini AI.
     */
    suspend fun runSecurityAudit(
        auditLogsSummary: String,
        workspaceName: String
    ): Result<String> {
        val prompt = """
            Analyze the following corporate audit log trail for Workspace '$workspaceName':
            $auditLogsSummary

            Provide an Executive Security & Compliance Audit:
            1. Threat & Anomaly Detection: Identify any suspicious spikes, repeated permission denials, or unusual agent behaviors.
            2. Access Governance Rating: [SECURE, ELEVATED_RISK, or CRITICAL_ATTENTION] with explanation.
            3. SOC2 / GDPR Compliance Observations.
            4. Recommended Remediation & Policy Adjustments.
        """.trimIndent()
        return generateText(
            prompt = prompt,
            systemInstruction = "You are a Chief Information Security Officer (CISO) and Compliance Auditor AI analyzing immutable system audit trails.",
            model = "gemini-3.1-pro-preview"
        )
    }

    /**
     * Synthesize multiple intelligence notes into an Executive Strategic Briefing using Gemini AI.
     */
    suspend fun synthesizeExecutiveBriefing(
        intelligenceItems: List<String>,
        companyName: String
    ): Result<String> {
        val combinedContent = intelligenceItems.joinToString("\n\n---\n\n")
        val prompt = """
            Synthesize the following collected intelligence artifacts into a unified Executive Strategic Briefing for $companyName leadership:

            $combinedContent

            Structure the briefing into:
            1. Executive Summary & Key Milestones.
            2. Strategic Risks & Operational Friction Points.
            3. Tactical Opportunities & Next Week Directives.
            4. High-Level Performance Metrics.
        """.trimIndent()
        return generateText(
            prompt = prompt,
            systemInstruction = "You are the Executive Intelligence Synthesizer delivering clear, concise, and actionable strategic briefings to C-level executives.",
            model = "gemini-3.7-flash"
        )
    }

    /**
     * Consult a specific AI Agent persona using Gemini AI.
     */
    suspend fun consultAgentPersona(
        agentName: String,
        agentRole: String,
        capabilities: String,
        userQuery: String
    ): Result<String> {
        val systemInstruction = "You are $agentName ($agentRole). Your core authorized capabilities are: $capabilities. Respond with authoritative expertise, concise execution-oriented advice, and professional composure."
        return generateText(
            prompt = userQuery,
            systemInstruction = systemInstruction,
            model = "gemini-3.7-flash"
        )
    }

    private fun normalizeModel(model: String): String {
        return when (model) {
            "gemini-3.5-flash", "gemini-2.0-flash" -> DEFAULT_TEXT_MODEL
            "gemini-3.1-pro-preview", "gemini-2.5-pro", "gemini-3.7-pro" -> DEFAULT_REASONING_MODEL
            else -> model
        }
    }

    private fun extractTextFromResponse(jsonResponse: JSONObject): String {
        val candidates = jsonResponse.optJSONArray("candidates") ?: return "No response text received."
        if (candidates.length() == 0) return "No response text received."

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return "No response content."
        val parts = content.optJSONArray("parts") ?: return "No text parts in response."

        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("text")) {
                sb.append(part.getString("text"))
            }
        }
        return sb.toString().ifEmpty { "No text content generated." }
    }
}
