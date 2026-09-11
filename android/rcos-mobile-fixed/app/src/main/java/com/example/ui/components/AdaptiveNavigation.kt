package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserEntity
import com.example.ui.AppViewMode
import com.example.ui.theme.RcosNeonGreen

data class NavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val tag: String
)

val rcosNavDestinations = listOf(
    NavDestination("dashboard", "Dashboard", Icons.Default.Speed, "nav_dashboard"),
    NavDestination("phone", "Phone System", Icons.Default.Phone, "nav_phone"),
    NavDestination("jobs", "Jobs", Icons.Default.Work, "nav_jobs"),
    NavDestination("clients", "Clients", Icons.Default.People, "nav_clients"),
    NavDestination("more", "More", Icons.Default.MoreHoriz, "nav_more")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveNavigationLayout(
    currentRoute: String,
    currentUser: UserEntity?,
    viewMode: AppViewMode,
    onSetViewMode: (AppViewMode) -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onOpenOnboarding: () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    var showModeDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp
        val isWebMode = when (viewMode) {
            AppViewMode.WEB_DESKTOP -> true
            AppViewMode.MOBILE_APP -> false
            AppViewMode.AUTO_DETECT -> isWideScreen
        }

        if (isWebMode) {
            // WEB / DESKTOP VERSION LAYOUT
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Web Browser Address & System Control Bar
                Surface(
                    color = Color(0xFF090D16),
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Browser Window Controls
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF5F56), CircleShape))
                                Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFBD2E), CircleShape))
                                Box(modifier = Modifier.size(10.dp).background(Color(0xFF27C93F), CircleShape))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Web Address Input Bar
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF131B2E),
                                modifier = Modifier
                                    .widthIn(min = 260.dp, max = 480.dp)
                                    .border(1.dp, Color(0xFF1E2A42), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Secure Web Connection",
                                        tint = RcosNeonGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "https://rcos.platform.ai/app/$currentRoute",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // OS View Mode Selector Pill
                            Surface(
                                onClick = { showModeDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF111827),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen),
                                modifier = Modifier.testTag("os_version_selector_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when(viewMode) {
                                            AppViewMode.WEB_DESKTOP -> Icons.Default.Language
                                            AppViewMode.MOBILE_APP -> Icons.Default.Smartphone
                                            AppViewMode.AUTO_DETECT -> Icons.Default.AutoMode
                                        },
                                        contentDescription = null,
                                        tint = RcosNeonGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when(viewMode) {
                                            AppViewMode.WEB_DESKTOP -> "🌐 Web Version"
                                            AppViewMode.MOBILE_APP -> "📱 Mobile Version"
                                            AppViewMode.AUTO_DETECT -> "⚡ Auto-Detect OS"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = RcosNeonGreen
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = RcosNeonGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Quick Settings, Onboarding & Logout Action Buttons
                            IconButton(
                                onClick = { onNavigate("settings") },
                                modifier = Modifier.size(32.dp).testTag("web_settings_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = RcosNeonGreen, modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = onOpenOnboarding,
                                modifier = Modifier.size(32.dp).testTag("web_onboarding_button")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Setup", tint = RcosNeonGreen, modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier.size(32.dp).testTag("web_logout_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // Web Workspace Main Body
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Web Navigation Sidebar Dock
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(230.dp)
                            .background(Color(0xFF090D16))
                            .border(1.dp, Color(0xFF1E2A42)),
                        containerColor = Color(0xFF0B101D),
                        header = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RcLogoText(fontSize = 26.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "RCOS WEB",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = RcosNeonGreen,
                                                letterSpacing = 1.sp
                                            )
                                        )
                                        Text(
                                            text = "Desktop OS Workstation",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // User Badge Card
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF131B2E),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = RcosNeonGreen,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentUser?.fullName ?: "Enterprise User",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = currentUser?.companyName ?: "Acme Enterprise",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        rcosNavDestinations.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onNavigate(item.route) },
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag(item.tag),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = RcosNeonGreen,
                                    indicatorColor = RcosNeonGreen,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                )
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Onboarding & Logout
                        OutlinedButton(
                            onClick = onOpenOnboarding,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("rail_onboarding_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = RcosNeonGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Onboarding", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }

                        TextButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("rail_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign Out",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Content Workspace
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Scaffold(snackbarHost = snackbarHost) { innerPadding ->
                            content(innerPadding)
                        }
                    }
                }
            }
        } else {
            // MOBILE VERSION LAYOUT
            Scaffold(
                snackbarHost = snackbarHost,
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF070A0F))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            RcLogoText(fontSize = 22.sp, showFullTitle = true)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // OS View Mode Selector Pill
                                Surface(
                                    onClick = { showModeDialog = true },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF111827),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, RcosNeonGreen),
                                    modifier = Modifier.testTag("os_version_selector_pill_mobile")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when(viewMode) {
                                                AppViewMode.WEB_DESKTOP -> Icons.Default.Language
                                                AppViewMode.MOBILE_APP -> Icons.Default.Smartphone
                                                AppViewMode.AUTO_DETECT -> Icons.Default.AutoMode
                                            },
                                            contentDescription = null,
                                            tint = RcosNeonGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = when(viewMode) {
                                                AppViewMode.WEB_DESKTOP -> "Web"
                                                AppViewMode.MOBILE_APP -> "Mobile"
                                                AppViewMode.AUTO_DETECT -> "Auto"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = RcosNeonGreen
                                            )
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = RcosNeonGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onOpenOnboarding,
                                    modifier = Modifier.size(32.dp).testTag("topbar_onboarding_button")
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = "RCOS Onboarding Setup", tint = RcosNeonGreen, modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = onLogout,
                                    modifier = Modifier.size(32.dp).testTag("topbar_logout_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        if (currentRoute == "dashboard") {
                            Text(
                                text = "Mobile Operating System Active",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xFF0B101D),
                        tonalElevation = 8.dp
                    ) {
                        rcosNavDestinations.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onNavigate(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = if (selected) RcosNeonGreen else Color.Gray
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (selected) RcosNeonGreen else Color.Gray,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(item.tag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = RcosNeonGreen,
                                    selectedTextColor = RcosNeonGreen,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                content(innerPadding)
            }
        }

        // View Mode Selector Modal Dialog
        if (showModeDialog) {
            ViewModeSelectorDialog(
                currentMode = viewMode,
                onSelectMode = { mode ->
                    onSetViewMode(mode)
                    showModeDialog = false
                },
                onDismiss = { showModeDialog = false }
            )
        }
    }
}

@Composable
fun ViewModeSelectorDialog(
    currentMode: AppViewMode,
    onSelectMode: (AppViewMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1322),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = RcosNeonGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Select OS Version / Mode",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(
                    text = "Switch between Web/Desktop operating system layout or Mobile phone app layout:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                // Option 1: Auto Detect
                ViewModeOptionCard(
                    title = "⚡ Auto-Detect OS & Device",
                    subtitle = "Automatically adapts between Web and Mobile based on screen size & platform",
                    isSelected = currentMode == AppViewMode.AUTO_DETECT,
                    onClick = { onSelectMode(AppViewMode.AUTO_DETECT) }
                )

                // Option 2: Web Desktop Version
                ViewModeOptionCard(
                    title = "🌐 Web / Desktop OS Version",
                    subtitle = "Full Web Workstation layout with top browser URL bar, multi-column workspace grid & sidebar dock",
                    isSelected = currentMode == AppViewMode.WEB_DESKTOP,
                    onClick = { onSelectMode(AppViewMode.WEB_DESKTOP) }
                )

                // Option 3: Mobile App Version
                ViewModeOptionCard(
                    title = "📱 Mobile Phone OS Version",
                    subtitle = "Optimized single-column mobile app with top header & bottom touch navigation bar",
                    isSelected = currentMode == AppViewMode.MOBILE_APP,
                    onClick = { onSelectMode(AppViewMode.MOBILE_APP) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        }
    )
}

@Composable
fun ViewModeOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFF16233B) else Color(0xFF111726),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) RcosNeonGreen else Color(0xFF1E2A42)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) RcosNeonGreen else Color.White
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                )
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = RcosNeonGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


