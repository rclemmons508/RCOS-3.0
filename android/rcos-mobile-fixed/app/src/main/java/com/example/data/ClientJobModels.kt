package com.example.data

data class ContactPerson(
    val name: String,
    val role: String,
    val email: String,
    val phone: String,
    val preferredChannel: String = "Email & Phone"
)

data class JobTaskItem(
    val id: String,
    val title: String,
    val status: String, // "In Progress", "Queued", "Completed"
    val assignedAgent: String,
    val priority: String, // "High", "Urgent", "Medium", "Normal"
    val dueDate: String,
    val progress: Float = 0.65f,
    val summary: String = "AI Agent executing routine workload pipeline."
)

data class JobEntity(
    val id: String,
    val title: String,
    val clientName: String,
    val status: String, // "Pending", "In Progress", "Queued", "Completed", "Archived"
    val assignedAgent: String,
    val priority: String,
    val dueDate: String,
    val approvalRequired: Boolean = false,
    val isApproved: Boolean = false,
    val progress: Float = 0.5f,
    val summary: String = "AI Agent executing workload pipeline."
)

data class ClientDetailData(
    val id: String,
    val companyName: String,
    val industry: String,
    val accountEmail: String,
    val phone: String,
    val status: String, // "Active Enterprise", "VIP", "Prospect"
    val headquarters: String = "San Francisco, CA",
    val contractValue: String = "$120,000 / yr",
    val onboardingDate: String = "Jan 2025",
    val primaryContact: ContactPerson = ContactPerson("Sarah Jenkins", "VP of Operations", "s.jenkins@client.com", "+1 (555) 234-8901"),
    val ongoingJobs: List<JobTaskItem> = emptyList(),
    val queuedJobs: List<JobTaskItem> = emptyList(),
    val notes: String = "Key enterprise account with automated AI voice routing enabled."
)

data class IncomingCallState(
    val callId: String = "",
    val callerName: String = "Apex Enterprises",
    val callerPhone: String = "+1 (555) 234-8901",
    val isRinging: Boolean = false,
    val isConnected: Boolean = false,
    val handledBy: String = "None", // "Human Representative", "AI Voice Agent", "None"
    val callNotes: String = "",
    val liveTranscript: List<String> = emptyList()
)

data class UserProfileData(
    val fullName: String = "RCS Executive User",
    val executiveTitle: String = "Chief Executive Officer",
    val personalEmail: String = "rcsolutions@gmail.com",
    val phone: String = "+1 (555) 100-2000",
    val googleWorkspaceEmail: String = "rcsolutions@gmail.com",
    val microsoftAccountEmail: String = "executive@rcsolutions.onmicrosoft.com",
    val organizationName: String = "RCOS Global Solutions",
    val timezone: String = "EST - Eastern Time (US & Canada)"
)

data class CalendarEventItem(
    val id: String,
    val title: String,
    val clientName: String,
    val eventType: String = "AI Consultation", // "AI Consultation", "Client Meeting", "Job Briefing", "Phone Call", "Contract Signing", "Executive Review"
    val date: String = "Today",
    val time: String = "10:00 AM",
    val assignedAgent: String = "Workload Synthesizer",
    val status: String = "Scheduled", // "Scheduled", "In Progress", "Completed", "Cancelled"
    val notes: String = "",
    val locationOrLink: String = "Google Meet / Phone Hotline",
    val timestamp: Long = System.currentTimeMillis()
)

data class PhoneCallItem(
    val id: String,
    val callerName: String,
    val phoneNumber: String,
    val callType: String = "AI Handled", // "Incoming", "Outgoing", "AI Handled"
    val duration: String = "2m 30s",
    val timeAgo: String = "Just now",
    val summary: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
