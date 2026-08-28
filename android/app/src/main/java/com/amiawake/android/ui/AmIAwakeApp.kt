package com.amiawake.android.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amiawake.android.AmIAwakeApplication
import com.amiawake.android.ui.components.OfflineNotice
import com.amiawake.android.ui.screens.AuthScreen
import com.amiawake.android.ui.screens.FriendDetailScreen
import com.amiawake.android.ui.screens.FriendsScreen
import com.amiawake.android.ui.screens.HomeScreen
import com.amiawake.android.ui.screens.ProfileScreen
import com.amiawake.android.ui.screens.SleepScheduleScreen
import com.amiawake.android.ui.screens.StatusScreen
import com.amiawake.android.ui.theme.AmIAwakeTheme

@Composable
fun AmIAwakeApp() {
    val application = LocalContext.current.applicationContext as AmIAwakeApplication
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(application.container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    AmIAwakeTheme {
        when {
            state.checkingSession -> SplashScreen()
            !state.authenticated -> AuthScreen(state.isRunning(MainViewModel.AUTH_ACTION), state.authError, viewModel::authenticate)
            else -> AppShell(state, viewModel)
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Bedtime, "Am I Awake?", tint = MaterialTheme.colorScheme.primary)
        CircularProgressIndicator(Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppShell(state: MainUiState, viewModel: MainViewModel) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: AppDestination.Home.route
    val rootDestination = AppDestination.entries.firstOrNull { it.route == route }
    val nestedTitle = when (route) {
        Routes.FriendDetail -> "Профиль друга"
        Routes.SleepSchedule -> "Расписание сна"
        else -> null
    }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (nestedTitle != null) {
                TopAppBar(
                    title = { Text(nestedTitle) },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                )
            }
        },
        bottomBar = {
            if (rootDestination != null) {
                NavigationBar {
                    AppDestination.entries.forEach { destination ->
                        val selected = destination == rootDestination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigate(destination.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(if (selected) destination.selectedIcon else destination.icon, destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            NavHost(navController, startDestination = AppDestination.Home.route) {
                composable(AppDestination.Home.route) {
                    HomeScreen(state, padding, viewModel::refreshAll, { navController.navigateRoot(AppDestination.Friends) }, { navController.navigate("friend/${Uri.encode(it)}") })
                }
                composable(AppDestination.Friends.route) {
                    FriendsScreen(state, padding, viewModel::refreshAll, viewModel::searchUsers, viewModel::sendFriendRequest, viewModel::acceptFriend, viewModel::declineRequest, viewModel::cancelRequest) {
                        navController.navigate("friend/${Uri.encode(it)}")
                    }
                }
                composable(AppDestination.Status.route) { StatusScreen(state, padding, viewModel::setStatus) }
                composable(AppDestination.Profile.route) {
                    ProfileScreen(
                        state = state,
                        padding = padding,
                        onSchedule = { navController.navigate(Routes.SleepSchedule) },
                        onDisplayName = viewModel::updateDisplayName,
                        onTimeZone = viewModel::updateTimeZone,
                        onLogout = viewModel::logout,
                    )
                }
                composable(Routes.FriendDetail) { entry ->
                    val username = Uri.decode(entry.arguments?.getString("username").orEmpty())
                    val friend = state.friends.friends.firstOrNull { it.username == username }
                    FriendDetailScreen(friend, padding) { viewModel.removeFriend(it); navController.popBackStack() }
                }
                composable(Routes.SleepSchedule) {
                    SleepScheduleScreen(state, padding, viewModel::saveSchedule, viewModel::setScheduleEnabled, viewModel::deleteSchedule)
                }
            }
            OfflineNotice((state.dashboard?.pendingEventCount ?: 0) > 0 && state.loadError != null)
        }
    }
}

private fun NavHostController.navigateRoot(destination: AppDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private enum class AppDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector) {
    Home("home", "Главная", Icons.Outlined.Home, Icons.Default.Home),
    Friends("friends", "Друзья", Icons.Outlined.Group, Icons.Default.Group),
    Status("status", "Статус", Icons.Outlined.Tune, Icons.Default.Tune),
    Profile("profile", "Профиль", Icons.Outlined.Person, Icons.Default.Person),
}

private object Routes {
    const val FriendDetail = "friend/{username}"
    const val SleepSchedule = "sleep-schedule"
}
