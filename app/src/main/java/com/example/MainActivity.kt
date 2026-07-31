package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.OfflineTubeViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn
import com.example.service.VideoPlayerManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: OfflineTubeViewModel = ViewModelProvider(
                this,
                OfflineTubeViewModel.Factory(application)
            )[OfflineTubeViewModel::class.java]
            val isAdultMode by viewModel.isAdultMode.collectAsStateWithLifecycle()

            MyApplicationTheme(isAdultMode = isAdultMode) {
                val context = LocalContext.current

                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                // Observe notifications from the ViewModel
                val uiEvent by viewModel.uiEvent.collectAsStateWithLifecycle()
                LaunchedEffect(uiEvent) {
                    uiEvent?.let { message ->
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                        viewModel.clearUiEvent()
                    }
                }

                // Observe custom floating player states
                val isPlayerActive by viewModel.isPlayerActive.collectAsStateWithLifecycle()
                val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsStateWithLifecycle()
                val currentVideo by viewModel.currentVideo.collectAsStateWithLifecycle()

                // Hide/show system status/navigation bars in expanded player mode (immersive mode)
                val window = this@MainActivity.window
                val windowInsetsController = remember(window) {
                    androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                }
                LaunchedEffect(isPlayerActive, isPlayerExpanded) {
                    if (isPlayerActive && isPlayerExpanded) {
                        windowInsetsController.systemBarsBehavior =
                            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    } else {
                        windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                    }
                }

                // Intercept back button to collapse the player when it's expanded
                BackHandler(enabled = isPlayerActive && isPlayerExpanded) {
                    viewModel.minimizePlayer()
                }

                // Determine whether to display the bottom navigation bar (hide when player is expanded and when on Shorts)
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = !(isPlayerActive && isPlayerExpanded) && currentRoute != "shorts"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showBottomBar) {
                            val ytNavColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (isAdultMode) Color(0xFFFF5722) else Color(0xFFFF0000),
                                unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                selectedTextColor = if (isAdultMode) Color(0xFFFF5722) else Color(0xFFFF0000),
                                unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                indicatorColor = Color.Transparent
                            )
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.background,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == "feed",
                                    onClick = {
                                        viewModel.activeChannelDetailId.value = null
                                        navController.navigate("feed") {
                                            popUpTo("feed") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == "feed") Icons.Filled.Home else Icons.Outlined.Home,
                                            contentDescription = "Главная"
                                        )
                                    },
                                    label = { Text("Главная") },
                                    colors = ytNavColors
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "audio",
                                    onClick = {
                                        viewModel.activeChannelDetailId.value = null
                                        navController.navigate("audio") {
                                            popUpTo("feed") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isAdultMode) {
                                                if (currentRoute == "audio") Icons.Filled.Book else Icons.Outlined.Book
                                            } else {
                                                if (currentRoute == "audio") Icons.Filled.MusicNote else Icons.Outlined.MusicNote
                                            },
                                            contentDescription = if (isAdultMode) "Манга" else "Музыка"
                                        )
                                    },
                                    label = { Text(if (isAdultMode) "Манга" else "Музыка") },
                                    colors = ytNavColors
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "add_content",
                                    onClick = {
                                        viewModel.activeChannelDetailId.value = null
                                        navController.navigate("add_content") {
                                            popUpTo("feed") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Добавить",
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    },
                                    label = null,
                                    colors = ytNavColors
                                )
                                 NavigationBarItem(
                                    selected = currentRoute == "playlists",
                                    onClick = {
                                        viewModel.activeChannelDetailId.value = null
                                        navController.navigate("playlists") {
                                            popUpTo("feed") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == "playlists") Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                                            contentDescription = "Плейлисты"
                                        )
                                    },
                                    label = { Text("Плейлисты") },
                                    colors = ytNavColors
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "about",
                                    onClick = {
                                        viewModel.activeChannelDetailId.value = null
                                        navController.navigate("about") {
                                            popUpTo("feed") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        val iconSelected = Icons.Filled.AccountCircle
                                        val iconUnselected = Icons.Outlined.AccountCircle
                                        Icon(
                                            imageVector = if (currentRoute == "about") iconSelected else iconUnselected,
                                            contentDescription = if (isAdultMode) "Панель 18+" else "Вы"
                                        )
                                    },
                                    label = { Text(if (isAdultMode) "Панель 18+" else "Вы") },
                                    colors = ytNavColors
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "feed",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable("feed") {
                                FeedScreen(
                                    viewModel = viewModel,
                                    onVideoClick = { videoId ->
                                        val video = viewModel.allVideos.value.find { it.id == videoId }
                                        if (video != null) {
                                            if (video.isShorts) {
                                                viewModel.activeChannelDetailId.value = null
                                                viewModel.selectedShortsId.value = videoId
                                                navController.navigate("shorts")
                                            } else {
                                                viewModel.playVideo(video)
                                            }
                                        }
                                    },
                                    onChannelClick = { channelId ->
                                        viewModel.activeChannelDetailId.value = channelId
                                        navController.navigate("channels") {
                                            popUpTo("feed") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }

                            composable("channels") {
                                ChannelsScreen(
                                    viewModel = viewModel,
                                    onVideoClick = { videoId ->
                                        val video = viewModel.allVideos.value.find { it.id == videoId }
                                        if (video != null) {
                                            if (video.isShorts) {
                                                viewModel.selectedShortsId.value = videoId
                                                navController.navigate("shorts")
                                            } else {
                                                viewModel.playVideo(video)
                                            }
                                        }
                                    },
                                    onNavigateToShorts = {
                                        navController.navigate("shorts")
                                    },
                                    onBackClick = {
                                        viewModel.activeChannelDetailId.value = null
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("add_content") {
                                AddContentScreen(
                                    viewModel = viewModel,
                                    onNavigateToChannels = {
                                        navController.navigate("channels") {
                                            popUpTo("feed") { saveState = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable("about") {
                                AboutScreen(
                                    viewModel = viewModel,
                                    onNavigateToChannels = {
                                        navController.navigate("channels")
                                    },
                                    onNavigateToPlaylists = {
                                        navController.navigate("playlists")
                                    }
                                )
                             }

                            composable("shorts") {
                                ShortsScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        navController.popBackStack()
                                    },
                                    onChannelClick = { channelId ->
                                        viewModel.activeChannelDetailId.value = channelId
                                        navController.navigate("channels") {
                                            popUpTo("feed") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }

                            composable("playlists") {
                                PlaylistsScreen(
                                    viewModel = viewModel,
                                    onNavigateToShorts = {
                                        viewModel.activeChannelDetailId.value = null
                                        navController.navigate("shorts")
                                    },
                                    onVideoClick = { videoId ->
                                        val video = viewModel.allVideos.value.find { it.id == videoId }
                                        if (video != null) {
                                            if (video.isShorts) {
                                                viewModel.activeChannelDetailId.value = null
                                                viewModel.selectedShortsId.value = videoId
                                                navController.navigate("shorts")
                                            } else {
                                                viewModel.playVideo(video)
                                            }
                                        }
                                    }
                                )
                            }

                            composable("audio") {
                                if (isAdultMode) {
                                    MangaScreen(viewModel = viewModel)
                                } else {
                                    AudioScreen(viewModel = viewModel)
                                }
                            }
                        }

                        // Floating/overlay YouTube-style player
                        if (isPlayerActive && currentVideo != null) {
                            if (isPlayerExpanded) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
                                    WatchScreen(
                                        videoId = currentVideo!!.id,
                                        viewModel = viewModel,
                                        onBack = { viewModel.minimizePlayer() },
                                        onChannelClick = { channelId ->
                                            viewModel.activeChannelDetailId.value = channelId
                                            viewModel.minimizePlayer()
                                            navController.navigate("channels") {
                                                popUpTo("feed") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            } else {
                                // Minimized Miniplayer Bar
                                val channels by viewModel.allChannels.collectAsStateWithLifecycle()
                                val isAudioOnly by viewModel.isAudioOnly.collectAsStateWithLifecycle()
                                val channel = channels.find { it.id == currentVideo!!.channelId }
                                val channelName = channel?.name ?: "Канал"
                                
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(bottom = innerPadding.calculateBottomPadding()) // Sits exactly on top of bottom navigation!
                                        .height(64.dp)
                                        .clickable { viewModel.expandPlayer() },
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = 4.dp
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(96.dp)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.Black)
                                            ) {
                                                if (isAudioOnly) {
                                                    val presetGradient = when (currentVideo!!.thumbnailUri) {
                                                        "jupiter" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFE94E77), Color(0xFFF2D43B)))
                                                        "forest" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d)))
                                                        "lofi" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)))
                                                        else -> null
                                                    }
                                                    if (presetGradient != null) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(presetGradient)
                                                        )
                                                    } else {
                                                        coil.compose.AsyncImage(
                                                            model = currentVideo!!.thumbnailUri,
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    }
                                                } else {
                                                    AndroidView(
                                                        factory = { ctx ->
                                                            PlayerView(ctx).apply {
                                                                player = VideoPlayerManager.getPlayer(ctx)
                                                                useController = false
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }

                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = currentVideo!!.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Text(
                                                    text = channelName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            val isPlayingState by viewModel.isPlaying.collectAsStateWithLifecycle()
                                            IconButton(
                                                onClick = { viewModel.togglePlayPause() }
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlayingState) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = if (isPlayingState) "Пауза" else "Воспроизвести",
                                                    tint = MaterialTheme.colorScheme.onBackground
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.closePlayer() }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Закрыть",
                                                    tint = MaterialTheme.colorScheme.onBackground
                                                )
                                            }
                                        }

                                        val currentPosition by viewModel.playbackPosition.collectAsStateWithLifecycle()
                                        val duration by viewModel.duration.collectAsStateWithLifecycle()
                                        val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                                        LinearProgressIndicator(
                                            progress = progress,
                                            modifier = Modifier.fillMaxWidth().height(2.dp),
                                            color = Color.Red,
                                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
