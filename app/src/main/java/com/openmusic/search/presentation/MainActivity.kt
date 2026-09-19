package com.openmusic.search.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.openmusic.search.domain.model.SearchResult
import com.openmusic.search.presentation.downloads.DownloadsScreen
import com.openmusic.search.presentation.home.HomeScreen
import com.openmusic.search.presentation.player.MiniPlayer
import com.openmusic.search.presentation.playlist.PlaylistScreen
import com.openmusic.search.presentation.search.SearchViewModel
import com.openmusic.search.presentation.settings.SettingsScreen
import com.openmusic.search.presentation.theme.OpenMusicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenMusicTheme {
                AppRoot()
            }
        }
    }
}

private sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object Playlist : Screen("playlist", "收藏", Icons.Default.Favorite)
    data object Downloads : Screen("downloads", "下载", Icons.Default.Download)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

private val items = listOf(Screen.Home, Screen.Playlist, Screen.Downloads, Screen.Settings)

@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val playerManager = searchViewModel.playerManager
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(
                    playerManager = playerManager,
                    onExpand = {}
                )
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            NavHost(navController = navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onOpenOriginal = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        onDownload = { result ->
                            com.openmusic.search.download.DownloadManager.enqueue(context, result)
                        }
                    )
                }
                composable(Screen.Playlist.route) { PlaylistScreen() }
                composable(Screen.Downloads.route) { DownloadsScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
            }
        }
    }
}
