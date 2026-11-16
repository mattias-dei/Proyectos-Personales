package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.audio.BuzzerAudioEngine
import com.example.data.bluetooth.Esp32BluetoothManager
import com.example.data.preferences.ThemePreferenceStore
import com.example.data.repository.ChristmasCloudRepository
import com.example.notifications.FestiveNotificationManager
import com.example.ui.components.FestiveStringLights
import com.example.ui.components.FestiveTutorialDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ControlStudioScreen
import com.example.ui.screens.PlaylistsAndWidgetScreen
import com.example.ui.screens.SongLibraryScreen
import com.example.ui.screens.SettingsAndSyncScreen
import com.example.ui.theme.NavidadBuzzerTheme
import com.example.ui.viewmodel.ChristmasBuzzerViewModel
import com.example.widget.ChristmasCountdownWidgetProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object NavRoutes {
    const val CONTROL = "control"
    const val LIBRARY = "library"
    const val STREAMING = "streaming"
    const val SETTINGS = "settings"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_TABS = listOf(
    BottomNavTab(
        route = NavRoutes.CONTROL,
        label = "Control",
        selectedIcon = Icons.Rounded.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_tab_control"
    ),
    BottomNavTab(
        route = NavRoutes.LIBRARY,
        label = "Canciones",
        selectedIcon = Icons.Rounded.MusicNote,
        unselectedIcon = Icons.Outlined.MusicNote,
        testTag = "nav_tab_library"
    ),
    BottomNavTab(
        route = NavRoutes.STREAMING,
        label = "Listas",
        selectedIcon = Icons.Rounded.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic,
        testTag = "nav_tab_streaming"
    ),
    BottomNavTab(
        route = NavRoutes.SETTINGS,
        label = "Ajustes",
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_tab_settings"
    )
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FestiveNotificationManager.ensureChannel(this)
        ChristmasCountdownWidgetProvider.refreshAllWidgets(this)

        setContent {
            val context = LocalContext.current
            val themeStore = remember(context) { ThemePreferenceStore(context.applicationContext) }
            val darkWinterMode by themeStore.darkWinterModeFlow.collectAsStateWithLifecycle(initialValue = true)
            val scope = rememberCoroutineScope()

            NavidadBuzzerTheme(darkWinterMode = darkWinterMode) {
                AppRootNavigation(
                    themeStore = themeStore,
                    darkWinterMode = darkWinterMode,
                    onToggleLocalTheme = {
                        scope.launch { themeStore.setDarkWinterMode(!darkWinterMode) }
                    }
                )
            }
        }
    }
}

@Composable
fun AppRootNavigation(
    themeStore: ThemePreferenceStore,
    darkWinterMode: Boolean,
    onToggleLocalTheme: () -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    val showTutorialOnStart by themeStore.showTutorialOnStartFlow.collectAsStateWithLifecycle(initialValue = null)
    var startupTutorialDismissed by remember { mutableStateOf(false) }
    var manualTutorialOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (user == null) {
        AuthScreen(
            darkWinterMode = darkWinterMode,
            onToggleDarkMode = onToggleLocalTheme,
            onAuthSuccess = {}
        )
    } else {
        androidx.compose.runtime.key(user.uid) {
            AuthenticatedChristmasStudio(
                currentUserId = user.uid,
                userEmail = user.email,
                themeStore = themeStore,
                onOpenTutorial = { manualTutorialOpen = true }
            )
        }
    }

    val shouldShowTutorial = manualTutorialOpen || (showTutorialOnStart == true && !startupTutorialDismissed)
    if (shouldShowTutorial) {
        FestiveTutorialDialog(
            initialShowOnStart = showTutorialOnStart ?: true,
            onDismiss = { keepShowingOnStart ->
                startupTutorialDismissed = true
                manualTutorialOpen = false
                scope.launch {
                    themeStore.setShowTutorialOnStart(keepShowingOnStart)
                }
            }
        )
    }
}

@Composable
fun AuthenticatedChristmasStudio(
    currentUserId: String,
    userEmail: String?,
    themeStore: ThemePreferenceStore,
    onOpenTutorial: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appScope = rememberCoroutineScope()

    val bluetoothManager = remember(context) {
        Esp32BluetoothManager(context.applicationContext, appScope)
    }
    val audioEngine = remember { BuzzerAudioEngine(appScope) }

    val viewModel: ChristmasBuzzerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repo = ChristmasCloudRepository(db, Firebase.auth)
                ChristmasBuzzerViewModel(
                    repository = repo,
                    bluetoothManager = bluetoothManager,
                    audioEngine = audioEngine,
                    themeStore = themeStore,
                    appContext = app.applicationContext,
                    currentUserId = currentUserId
                )
            }
        }
    )

    val settings by viewModel.localSettings.collectAsStateWithLifecycle()
    val selectedSong by viewModel.selectedSong.collectAsStateWithLifecycle()
    val loopMode by viewModel.loopMode.collectAsStateWithLifecycle()
    val telemetry by viewModel.playbackTelemetry.collectAsStateWithLifecycle()
    val btState by viewModel.bluetoothState.collectAsStateWithLifecycle()
    val remainingAutoOff by viewModel.remainingAutoOffSeconds.collectAsStateWithLifecycle()
    val customMelodiesState by viewModel.customMelodiesState.collectAsStateWithLifecycle()
    val playlistsState by viewModel.playlistsState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoutes.CONTROL

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                FestiveStringLights(
                    enabled = settings.lightsEnabled,
                    pattern = settings.lightsPattern,
                    isPlaying = telemetry.isPlaying
                )
            }
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                NAV_TABS.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.CONTROL,
                modifier = Modifier.weight(1f)
            ) {
                composable(NavRoutes.CONTROL) {
                    ControlStudioScreen(
                        settings = settings,
                        selectedSong = selectedSong,
                        loopMode = loopMode,
                        telemetry = telemetry,
                        btState = btState,
                        remainingAutoOffSeconds = remainingAutoOff,
                        requiredBtPermissions = bluetoothManager.requiredPermissions(),
                        onRefreshBt = { viewModel.refreshBluetooth() },
                        onStartScan = { viewModel.startBluetoothScan() },
                        onStopScan = { viewModel.stopBluetoothScan() },
                        onConnectDevice = { viewModel.connectEsp32Device(it) },
                        onDisconnectDevice = { viewModel.disconnectEsp32() },
                        onPlay = { viewModel.playSong() },
                        onStop = { viewModel.stopPlayback() },
                        onPreviousSong = { viewModel.previousSong() },
                        onNextSong = { viewModel.nextSong() },
                        onSelectLoopMode = { viewModel.setLoopMode(it) },
                        onTempoChange = { viewModel.updateTempo(it) },
                        onVolumeChange = { viewModel.updateVolume(it) },
                        onToggleLights = { viewModel.toggleLights() },
                        onSelectLightsPattern = { viewModel.setLightsPattern(it) },
                        onStartAutoOff = { viewModel.startAutoOffTimer(it) },
                        onCancelAutoOff = { viewModel.cancelAutoOffTimer() },
                        onOpenTutorial = onOpenTutorial,
                        onNavigateToLibrary = {
                            navController.navigate(NavRoutes.LIBRARY) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(NavRoutes.LIBRARY) {
                    SongLibraryScreen(
                        selectedSong = selectedSong,
                        isPlaying = telemetry.isPlaying,
                        customMelodiesState = customMelodiesState,
                        onPlaySong = { song ->
                            viewModel.playSong(song)
                        },
                        onStop = { viewModel.stopPlayback() },
                        onImportFileUri = { uri -> viewModel.importMelodyFromUri(uri) },
                        onDeleteCustomMelody = { id -> viewModel.deleteCustomMelody(id) }
                    )
                }
                composable(NavRoutes.STREAMING) {
                    PlaylistsAndWidgetScreen(
                        playlistsState = playlistsState,
                        onAddPlaylist = { title, platform, url, desc ->
                            viewModel.addPlaylist(title, platform, url, desc)
                        },
                        onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                        onShowMessage = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }
                composable(NavRoutes.SETTINGS) {
                    SettingsAndSyncScreen(
                        userEmail = userEmail,
                        settings = settings,
                        onToggleDarkWinterMode = { viewModel.toggleDarkWinterMode() },
                        onToggleFestiveSounds = { viewModel.toggleFestiveSounds() },
                        onSaveNotifications = { enabled, title, msg, h, m ->
                            viewModel.saveNotificationSettings(enabled, title, msg, h, m)
                        },
                        onSendTestNotification = { title, msg ->
                            viewModel.sendTestNotification(title, msg)
                        },
                        onSignOut = {
                            viewModel.stopPlayback()
                            signOutUser(
                                credentialManager = CredentialManager.create(context),
                                scope = scope
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
            }
        }
    }
}
