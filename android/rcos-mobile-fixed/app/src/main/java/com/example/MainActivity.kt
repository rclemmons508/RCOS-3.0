package com.example

// Force rebuild for APK export
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.NovaViewModel
import com.example.ui.components.AdaptiveNavigationLayout
import com.example.ui.components.AgentDashboard
import com.example.ui.components.OnboardingDialog
import com.example.ui.screens.*
import com.example.ui.theme.NovaDashboardTheme

class MainActivity : FragmentActivity() {

    private val viewModel: NovaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            NovaDashboardTheme(themeMode = themeMode) {
                NovaAppMainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NovaAppMainScreen(viewModel: NovaViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by viewModel.currentUser.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()
    val statusNotice by viewModel.statusNotice.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    var showRegisterScreen by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "dashboard"

    // Show status snackbar whenever statusNotice updates
    LaunchedEffect(statusNotice) {
        statusNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.clearStatusNotice()
        }
    }
    
    // Auto-launch onboarding for first-time or unconfigured enterprise users
    val isNewEnterpriseUser = currentUser != null && (companyProfile == null || companyProfile?.isConfigured == false)
    LaunchedEffect(currentUser, companyProfile) {
        if (isNewEnterpriseUser) {
            showOnboardingDialog = true
        }
    }

    if (currentUser == null) {
        // Authentication Flow
        if (showRegisterScreen) {
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { showRegisterScreen = false }
            )
        } else {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { showRegisterScreen = true }
            )
        }
    } else {
        // Authenticated Dashboard with Responsive Adaptive Navigation
        AdaptiveNavigationLayout(
            currentRoute = currentRoute,
            currentUser = currentUser,
            viewMode = viewMode,
            onSetViewMode = { mode -> viewModel.setViewMode(mode) },
            onNavigate = { targetRoute ->
                if (currentRoute != targetRoute) {
                    navController.navigate(targetRoute) {
                        popUpTo("dashboard") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            onLogout = { viewModel.logout() },
            onOpenOnboarding = { showOnboardingDialog = true },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("dashboard") {
                    DashboardOverviewScreen(
                        viewModel = viewModel,
                        onNavigateTab = { route -> navController.navigate(route) },
                        onOpenOnboarding = { showOnboardingDialog = true }
                    )
                }
                composable("phone") {
                    PhoneSystemScreen(viewModel = viewModel)
                }
                composable("jobs") {
                    JobsScreen(viewModel = viewModel)
                }
                composable("clients") {
                    ClientsScreen(viewModel = viewModel)
                }
                composable("calendar") {
                    CalendarScreen(viewModel = viewModel)
                }
                composable("more") {
                    MoreSystemScreen(
                        viewModel = viewModel,
                        onNavigate = { route -> navController.navigate(route) },
                        onOpenOnboarding = { showOnboardingDialog = true },
                        onLogout = { viewModel.logout() }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToIntegrations = { navController.navigate("integrations") },
                        onOpenOnboarding = { showOnboardingDialog = true }
                    )
                }
                composable("integrations") {
                    IntegrationsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("chat") {
                    GeminiChatScreen(viewModel = viewModel)
                }
                composable("transcribe") {
                    VoiceTranscriberScreen(viewModel = viewModel)
                }
                composable("reasoning") {
                    DeepThinkingScreen(viewModel = viewModel)
                }
                composable("saved") {
                    SavedIntelligenceScreen(viewModel = viewModel)
                }
                composable("workflow") {
                    WorkflowEngineScreen(viewModel = viewModel)
                }
                composable("active_agents") {
                    ActiveAgentsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("agents") {
                    AgentRegistryView(viewModel = viewModel)
                }
                composable("agent_dashboard") {
                    AgentDashboard(
                        viewModel = viewModel,
                        onNavigateToWorkflows = { navController.navigate("workflow") },
                        onNavigateToRegistry = { navController.navigate("agents") }
                    )
                }
            }
        }

        // Multi-Step Enterprise Onboarding Wizard Dialog
        if (showOnboardingDialog || isNewEnterpriseUser) {
            OnboardingDialog(
                currentProfile = companyProfile,
                viewModel = viewModel,
                canDismiss = !isNewEnterpriseUser,
                onDismiss = { showOnboardingDialog = false },
                onSaveProfile = { company, ind, bottleneck, targetPct, agents, customRules ->
                    showOnboardingDialog = false
                    viewModel.saveOnboardingProfile(
                        companyName = company,
                        industry = ind,
                        bottleneck = bottleneck,
                        reductionPercent = targetPct,
                        agents = agents,
                        customInstructions = customRules
                    )
                }
            )
        }
    }
}
