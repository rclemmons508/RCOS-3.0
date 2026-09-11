package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboard_items")
data class DashboardItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val itemType: String = "INSIGHT" // INSIGHT, TRANSCRIPTION, REASONING, CHAT_SUMMARY
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String,
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val rolePersona: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val fullName: String,
    val passwordHash: String,
    val salt: String,
    val companyName: String,
    val industry: String,
    val registeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "company_profiles")
data class CompanyProfileEntity(
    @PrimaryKey val id: Int = 1,
    val companyName: String,
    val industry: String,
    val primaryBottleneck: String,
    val targetReductionPercent: Int,
    val activeAgents: String,
    val customInstructions: String,
    val isConfigured: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

