package com.example.data

data class AgentPermission(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean = true
)

data class AppIntegration(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val iconName: String,
    val isConnected: Boolean = true,
    val connectedAccount: String = "rcsolutions@gmail.com",
    val permissions: List<AgentPermission> = emptyList(),
    val lastSynced: String = "Active (Live Sync)"
)
