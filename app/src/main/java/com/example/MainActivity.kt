package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AiAssistantService
import com.example.data.BabelRepository
import com.example.firebase.configureAppCheck
import com.example.ui.assistant.AssistantScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.finance.FinanceScreen
import com.example.ui.goals.GoalsScreen
import com.example.ui.home.HomeScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.planner.PlannerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.study.StudyScreen
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val repository = BabelRepository(this)
        val aiService = AiAssistantService()

        setContent {
            BabelOSTheme {
                BabelAppRoot(repository = repository, aiService = aiService)
            }
        }
    }
}

@Composable
fun BabelAppRoot(
    repository: BabelRepository,
    aiService: AiAssistantService
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        // Unauthenticated gate: Google Sign-In with Credential Manager
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        // Authenticated Session: Check Onboarding status
        val profile by repository.observeUserProfile().collectAsState(initial = null)

        if (profile != null && !profile!!.onboardingCompleted) {
            OnboardingScreen(
                repository = repository,
                onCompleted = {
                    // Triggers navigation into main shell
                }
            )
        } else {
            BabelMainShell(
                repository = repository,
                aiService = aiService
            )
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Planner : Screen("planner", "Planner")
    object Study : Screen("study", "Study")
    object Finance : Screen("finance", "Finance")
    object Goals : Screen("goals", "Goals")
    object Assistant : Screen("assistant", "AI")
    object Settings : Screen("settings", "Settings")
}

@Composable
fun BabelMainShell(
    repository: BabelRepository,
    aiService: AiAssistantService
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // Android Hardware Back button handling
    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    Scaffold(
        containerColor = BabelDarkBackground,
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            NavigationBar(
                containerColor = BabelDarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                val isMoreSelected = currentScreen is Screen.Goals || currentScreen is Screen.Assistant || currentScreen is Screen.Settings

                NavigationBarItem(
                    selected = currentScreen is Screen.Home,
                    onClick = { currentScreen = Screen.Home },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001F29),
                        indicatorColor = BabelCyan,
                        selectedTextColor = BabelCyan,
                        unselectedIconColor = BabelTextMuted,
                        unselectedTextColor = BabelTextMuted
                    )
                )

                NavigationBarItem(
                    selected = currentScreen is Screen.Planner,
                    onClick = { currentScreen = Screen.Planner },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Planner") },
                    label = { Text("Planner", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001F29),
                        indicatorColor = BabelCyan,
                        selectedTextColor = BabelCyan,
                        unselectedIconColor = BabelTextMuted,
                        unselectedTextColor = BabelTextMuted
                    )
                )

                NavigationBarItem(
                    selected = currentScreen is Screen.Study,
                    onClick = { currentScreen = Screen.Study },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Study") },
                    label = { Text("Study", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001F29),
                        indicatorColor = BabelCyan,
                        selectedTextColor = BabelCyan,
                        unselectedIconColor = BabelTextMuted,
                        unselectedTextColor = BabelTextMuted
                    )
                )

                NavigationBarItem(
                    selected = currentScreen is Screen.Finance,
                    onClick = { currentScreen = Screen.Finance },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Finance") },
                    label = { Text("Finance", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001F29),
                        indicatorColor = BabelCyan,
                        selectedTextColor = BabelCyan,
                        unselectedIconColor = BabelTextMuted,
                        unselectedTextColor = BabelTextMuted
                    )
                )

                NavigationBarItem(
                    selected = isMoreSelected,
                    onClick = {
                        // Cycles or defaults to Goals
                        if (!isMoreSelected) currentScreen = Screen.Goals
                    },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                    label = { Text("More", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001F29),
                        indicatorColor = BabelCyan,
                        selectedTextColor = BabelCyan,
                        unselectedIconColor = BabelTextMuted,
                        unselectedTextColor = BabelTextMuted
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Secondary Sub-Header for More section destinations (Goals, Assistant, Settings)
            if (currentScreen is Screen.Goals || currentScreen is Screen.Assistant || currentScreen is Screen.Settings) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BabelDarkSurface)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val subScreens = listOf(
                        Screen.Goals to "Goals",
                        Screen.Assistant to "AI Assistant",
                        Screen.Settings to "Settings"
                    )
                    subScreens.forEach { (scr, title) ->
                        val isSel = currentScreen == scr
                        TextButton(
                            onClick = { currentScreen = scr },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isSel) BabelCyan else BabelTextSecondary
                            )
                        ) {
                            Text(
                                text = title,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Screen Content Transitions
            Crossfade(targetState = currentScreen, label = "screen_fade") { target ->
                when (target) {
                    is Screen.Home -> HomeScreen(
                        repository = repository,
                        aiService = aiService,
                        onNavigateToPlanner = { currentScreen = Screen.Planner },
                        onNavigateToStudy = { currentScreen = Screen.Study },
                        onNavigateToFinance = { currentScreen = Screen.Finance },
                        onNavigateToGoals = { currentScreen = Screen.Goals },
                        onNavigateToAssistant = { currentScreen = Screen.Assistant }
                    )
                    is Screen.Planner -> PlannerScreen(repository = repository)
                    is Screen.Study -> StudyScreen(repository = repository)
                    is Screen.Finance -> FinanceScreen(repository = repository)
                    is Screen.Goals -> GoalsScreen(repository = repository)
                    is Screen.Assistant -> AssistantScreen(repository = repository, aiService = aiService)
                    is Screen.Settings -> SettingsScreen(
                        repository = repository,
                        onSignOut = {
                            // Handled by authStateListener in BabelAppRoot
                        }
                    )
                }
            }
        }
    }
}
