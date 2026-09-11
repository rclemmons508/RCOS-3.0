package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewMode
import com.example.ui.NovaViewModel
import com.example.ui.components.ExecutiveVaultSection
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.RcosNeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NovaViewModel,
    onNavigateToIntegrations: () -> Unit,
    onOpenOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()
    val integrations by viewModel.integrations.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }

    val connectedCount = integrations.count { it.isConnected }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RcosNeonGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = RcosNeonGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "RCOS Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = "User Profile, Google & Microsoft Tool Linking & Display Mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Executive Personal Profile & Tool Accounts Card
        item {
            MultiWorkspaceGovernanceCard(viewModel = viewModel)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_user_profile_settings"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RcosNeonGreen.copy(alpha = 0.2f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = RcosNeonGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = userProfile.fullName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${userProfile.executiveTitle} • ${userProfile.organizationName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showEditProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("edit_user_profile_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit Profile & Accounts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Linked Accounts Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "LINKED GOOGLE & MICROSOFT WORKSPACE ACCOUNTS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = RcosNeonGreen
                        )

                        // Google Workspace Row
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFFEA4335), modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("Google Workspace", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text(
                                            text = userProfile.googleWorkspaceEmail.ifBlank { "Not linked yet" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (userProfile.googleWorkspaceEmail.isNotBlank()) RcosNeonGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (userProfile.googleWorkspaceEmail.isNotBlank()) RcosNeonGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (userProfile.googleWorkspaceEmail.isNotBlank()) "Linked" else "Unlinked",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (userProfile.googleWorkspaceEmail.isNotBlank()) RcosNeonGreen else Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        // Microsoft 365 Row
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF0078D4), modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("Microsoft 365 / Outlook", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text(
                                            text = userProfile.microsoftAccountEmail.ifBlank { "Not linked yet" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (userProfile.microsoftAccountEmail.isNotBlank()) RcosNeonGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (userProfile.microsoftAccountEmail.isNotBlank()) RcosNeonGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (userProfile.microsoftAccountEmail.isNotBlank()) "Linked" else "Unlinked",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (userProfile.microsoftAccountEmail.isNotBlank()) RcosNeonGreen else Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hardware Keystore & Biometric Executive Vault
        item {
            ExecutiveVaultSection(viewModel)
        }

        // Display Theme Selection Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_theme_settings"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = RcosNeonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "App Theme & Appearance",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose your preferred display mode or match your device OS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Theme Selector Options
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemeOptionTile(
                            title = "Dark Mode",
                            description = "Executive night canvas with neon green accent highlights",
                            icon = Icons.Default.DarkMode,
                            isSelected = themeMode == AppThemeMode.DARK_MODE,
                            testTag = "theme_option_dark",
                            onClick = { viewModel.setThemeMode(AppThemeMode.DARK_MODE) }
                        )

                        ThemeOptionTile(
                            title = "Light Mode",
                            description = "High-contrast clean layout with crisp emerald accents",
                            icon = Icons.Default.LightMode,
                            isSelected = themeMode == AppThemeMode.LIGHT_MODE,
                            testTag = "theme_option_light",
                            onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT_MODE) }
                        )

                        ThemeOptionTile(
                            title = "Follow Device System Setting",
                            description = "Automatically switch between Dark & Light based on Android OS preferences",
                            icon = Icons.Default.SettingsSuggest,
                            isSelected = themeMode == AppThemeMode.SYSTEM_DEFAULT,
                            testTag = "theme_option_system",
                            onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM_DEFAULT) }
                        )
                    }
                }
            }
        }

        // External App Integrations & AI Access Card
        item {
            Card(
                onClick = onNavigateToIntegrations,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_integrations_hub"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = RcosNeonGreen.copy(alpha = 0.2f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = RcosNeonGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "App Integrations & AI Access",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RcosNeonGreen,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = "$connectedCount Active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Connect Google Workspace (Gmail, Calendar, Drive), Slack & manage AI permissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Navigate",
                        tint = RcosNeonGreen
                    )
                }
            }
        }

        // View Mode / Screen Layout Preference Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_view_mode_settings"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = RcosNeonGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "OS View & Display Density",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Switch between Web Desktop, Mobile OS, or Responsive Auto-Detect",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ViewModeChip(
                            label = "Auto Detect",
                            icon = Icons.Default.AspectRatio,
                            isSelected = viewMode == AppViewMode.AUTO_DETECT,
                            testTag = "view_mode_auto",
                            onClick = { viewModel.setViewMode(AppViewMode.AUTO_DETECT) },
                            modifier = Modifier.weight(1f)
                        )
                        ViewModeChip(
                            label = "Web Desktop",
                            icon = Icons.Default.Computer,
                            isSelected = viewMode == AppViewMode.WEB_DESKTOP,
                            testTag = "view_mode_desktop",
                            onClick = { viewModel.setViewMode(AppViewMode.WEB_DESKTOP) },
                            modifier = Modifier.weight(1f)
                        )
                        ViewModeChip(
                            label = "Mobile OS",
                            icon = Icons.Default.Smartphone,
                            isSelected = viewMode == AppViewMode.MOBILE_APP,
                            testTag = "view_mode_mobile",
                            onClick = { viewModel.setViewMode(AppViewMode.MOBILE_APP) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Account Profile & Onboarding Wizard Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enterprise Organization Profile",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = RcosNeonGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = companyProfile?.companyName ?: "Acme Enterprise",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Industry: ${companyProfile?.industry ?: "Technology"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Executive Account: ${currentUser?.email ?: "executive@rcos.ai"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = RcosNeonGreen
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenOnboarding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reopen_onboarding_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reconfigure Organization & Agent Rules")
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        var fullName by remember { mutableStateOf(userProfile.fullName) }
        var executiveTitle by remember { mutableStateOf(userProfile.executiveTitle) }
        var personalEmail by remember { mutableStateOf(userProfile.personalEmail) }
        var phone by remember { mutableStateOf(userProfile.phone) }
        var googleEmail by remember { mutableStateOf(userProfile.googleWorkspaceEmail) }
        var msEmail by remember { mutableStateOf(userProfile.microsoftAccountEmail) }
        var orgName by remember { mutableStateOf(userProfile.organizationName) }
        var timezone by remember { mutableStateOf(userProfile.timezone) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AccountBox, contentDescription = null, tint = RcosNeonGreen)
                    Text("Executive Profile & Tool Accounts", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Add your personal information so RCOS can access your Google Workspace and Microsoft tools.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Your Full Name") },
                        modifier = Modifier.fillMaxWidth().testTag("input_profile_name")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = executiveTitle,
                            onValueChange = { executiveTitle = it },
                            label = { Text("Executive Title") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = orgName,
                            onValueChange = { orgName = it },
                            label = { Text("Organization Name") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = personalEmail,
                            onValueChange = { personalEmail = it },
                            label = { Text("Personal Email") },
                            modifier = Modifier.weight(1f).testTag("input_profile_personal_email")
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                    Text("TOOL ACCOUNT LINKING CREDENTIALS", style = MaterialTheme.typography.labelSmall, color = RcosNeonGreen)

                    OutlinedTextField(
                        value = googleEmail,
                        onValueChange = { googleEmail = it },
                        label = { Text("Google Workspace Email (Gmail, Calendar, Drive)") },
                        placeholder = { Text("you@company.com or rcsolutions@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = Color(0xFFEA4335)) },
                        modifier = Modifier.fillMaxWidth().testTag("input_profile_google_email")
                    )

                    OutlinedTextField(
                        value = msEmail,
                        onValueChange = { msEmail = it },
                        label = { Text("Microsoft 365 Account Email (Outlook, OneDrive)") },
                        placeholder = { Text("you@company.onmicrosoft.com") },
                        leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF0078D4)) },
                        modifier = Modifier.fillMaxWidth().testTag("input_profile_ms_email")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = userProfile.copy(
                            fullName = fullName.trim(),
                            executiveTitle = executiveTitle.trim(),
                            personalEmail = personalEmail.trim(),
                            phone = phone.trim(),
                            googleWorkspaceEmail = googleEmail.trim(),
                            microsoftAccountEmail = msEmail.trim(),
                            organizationName = orgName.trim(),
                            timezone = timezone.trim()
                        )
                        viewModel.updateUserProfile(updated)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RcosNeonGreen, contentColor = Color.Black),
                    modifier = Modifier.testTag("save_profile_settings_btn")
                ) {
                    Text("Save Profile & Link Accounts", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionTile(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, RcosNeonGreen) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) RcosNeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = RcosNeonGreen)
            )
        }
    }
}

@Composable
private fun ViewModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) RcosNeonGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, RcosNeonGreen) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) RcosNeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                ),
                color = if (isSelected) RcosNeonGreen else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
