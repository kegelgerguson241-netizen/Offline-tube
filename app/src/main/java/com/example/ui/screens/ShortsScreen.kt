package com.example.ui.screens

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.Channel
import com.example.data.Video
import com.example.ui.OfflineTubeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    viewModel: OfflineTubeViewModel,
    onBack: () -> Unit,
    onChannelClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val allVideos by viewModel.allVideos.collectAsStateWithLifecycle()
    val allChannels by viewModel.allChannels.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle(initialValue = emptyList())
    val playlistShorts by viewModel.playlistShortsToPlay.collectAsStateWithLifecycle()

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val activeChannelId by viewModel.activeChannelDetailId.collectAsStateWithLifecycle()

    // Filter to obtain all Shorts, or active channel Shorts, or active playlist Shorts
    val shortsList: List<Video> = remember(allVideos, playlistShorts, activeChannelId) {
        val currentPlaylistShorts = playlistShorts
        if (currentPlaylistShorts != null) {
            currentPlaylistShorts
        } else if (activeChannelId != null) {
            allVideos.filter { it.isShorts && it.channelId == activeChannelId }
        } else {
            allVideos.filter { it.isShorts }
        }
    }

    // Clear playlist shorts and active channel detail on screen exit
    DisposableEffect(Unit) {
        onDispose {
            viewModel.playlistShortsToPlay.value = null
            viewModel.activeChannelDetailId.value = null
        }
    }

    // Shut down the main floating video player when entering the Shorts screen
    LaunchedEffect(Unit) {
        viewModel.closePlayer()
    }

    if (shortsList.isEmpty()) {
        // EMPTY STATE: Display a visually polished placeholder with advice on how to upload
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Shorts", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Text(
                        text = "Раздел Shorts пуст",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "В приложении пока нет вертикальных Shorts видео (9:16). Перейдите в раздел «Добавить» (+) и включите переключатель «Шортс», чтобы загрузить своё первое Shorts видео!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    } else {
        // Capture the initial selectedShortsId once when entering the screen
        val initialSelectedId = remember { viewModel.selectedShortsId.value }

        // Secure state to remember the shuffled order of video IDs and avoid re-shuffling on database updates
        var shuffledIds by remember { mutableStateOf<List<Int>>(emptyList()) }

        LaunchedEffect(shortsList) {
            if (shuffledIds.isEmpty() && shortsList.isNotEmpty()) {
                val targeted = shortsList.find { it.id == initialSelectedId }
                val remainingIds = shortsList.filter { it.id != initialSelectedId }.map { it.id }.shuffled()
                shuffledIds = if (targeted != null) {
                    listOf(targeted.id) + remainingIds
                } else {
                    shortsList.map { it.id }.shuffled()
                }
            } else if (shortsList.isNotEmpty()) {
                val currentIds = shortsList.map { it.id }.toSet()
                val updatedShuffled = shuffledIds.filter { it in currentIds }.toMutableList()
                val newIds = currentIds - shuffledIds.toSet()
                if (newIds.isNotEmpty()) {
                    updatedShuffled.addAll(newIds.shuffled())
                }
                shuffledIds = updatedShuffled
            }
        }

        val shuffledShorts = remember(shuffledIds, shortsList) {
            shuffledIds.mapNotNull { id -> shortsList.find { it.id == id } }
        }

        // Reset selectedShortsId once consumed
        LaunchedEffect(Unit) {
            viewModel.selectedShortsId.value = null
        }

        val virtualPageCount = 100000
        val initialPage = virtualPageCount / 2
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { virtualPageCount }
        )

        // Single high-performance player for the page switcher
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
        }

        DisposableEffect(exoPlayer) {
            onDispose {
                exoPlayer.release()
            }
        }

        // Active video state
        val activePageIndex = pagerState.currentPage
        val activeVideo = remember(activePageIndex, shuffledShorts) {
            if (shuffledShorts.isNotEmpty()) {
                shuffledShorts[activePageIndex % shuffledShorts.size]
            } else null
        }

        // Keep track of and auto-increment views of the active short - keyed by video ID to prevent reload loops
        LaunchedEffect(activeVideo?.id) {
            activeVideo?.let { video ->
                viewModel.incrementVideoViews(video.id)
            }
        }

        // Manage player preparation and playback on page changes - keyed by video ID to prevent reload loops
        LaunchedEffect(activeVideo?.id) {
            if (activeVideo != null) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                val mediaItem = MediaItem.fromUri(activeVideo.videoUri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }

        // Main layout of the full-screen immersive video player
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (shuffledShorts.isNotEmpty()) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val video = shuffledShorts[page % shuffledShorts.size]
                    val channel = allChannels.find { it.id == video.channelId }

                    ShortsPlayerItem(
                        video = video,
                        channel = channel,
                        exoPlayer = exoPlayer,
                        isActive = (page == activePageIndex),
                        onChannelClick = onChannelClick,
                        viewModel = viewModel
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            // Top Header Overlay: Back button & Title & Playlist Add button (optimized for unobstructed video view)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 0.dp), // Positioned as high as possible
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }

                if (activeVideo != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showPlaylistDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Добавить в плейлист",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить Shorts",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Shorts Dialog
    if (showDeleteDialog) {
        val currentVideoToDelete = remember(shortsList, viewModel) {
            val initialSelectedId = viewModel.selectedShortsId.value
            if (initialSelectedId != null) {
                shortsList.find { it.id == initialSelectedId }
            } else shortsList.firstOrNull()
        } ?: shortsList.firstOrNull()

        if (currentVideoToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удалить Shorts?", fontWeight = FontWeight.Bold) },
                text = { Text("Вы уверены, что хотите удалить «${currentVideoToDelete.title}»? Это видео будет удалено из системы.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteVideo(currentVideoToDelete.id)
                            showDeleteDialog = false
                            Toast.makeText(context, "Shorts удален", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Удалить", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }

    // Add to Playlist Dialog
    if (showPlaylistDialog) {
        val currentVideoToSave = remember(shortsList, viewModel) {
            if (shortsList.isNotEmpty()) {
                val activeIndex = if (shortsList.size == 1) 0 else {
                    // Try to resolve the currently showing video
                    val initialSelectedId = viewModel.selectedShortsId.value
                    if (initialSelectedId != null) {
                        shortsList.indexOfFirst { it.id == initialSelectedId }.coerceAtLeast(0)
                    } else 0
                }
                shortsList.getOrNull(activeIndex)
            } else null
        }

        // Fallback: If currentVideoToSave is null, try to use whatever is the active short
        val videoToSave = currentVideoToSave ?: shortsList.firstOrNull()

        if (videoToSave != null) {
            AlertDialog(
                onDismissRequest = {
                    showPlaylistDialog = false
                    isCreatingNew = false
                },
                title = { Text("Сохранить Shorts", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (playlists.isEmpty() && !isCreatingNew) {
                            Text(
                                text = "У вас нет плейлистов. Создайте новый!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else if (!isCreatingNew) {
                            Text(
                                text = "Выберите плейлист для добавления:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(playlists) { playlist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.addVideoToPlaylist(playlist.id, videoToSave.id)
                                                Toast.makeText(context, "Добавлено в '${playlist.name}'", Toast.LENGTH_SHORT).show()
                                                showPlaylistDialog = false
                                            }
                                            .padding(vertical = 10.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(
                                                text = playlist.name + if (playlist.isShorts) " (Shorts)" else "",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (playlist.description.isNotBlank()) {
                                                Text(
                                                    text = playlist.description,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                }
                            }
                        }

                        if (isCreatingNew) {
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                label = { Text("Название нового плейлиста") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            Button(
                                onClick = { isCreatingNew = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Создать новый плейлист")
                            }
                        }
                    }
                },
                confirmButton = {
                    if (isCreatingNew) {
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    viewModel.createPlaylist(newPlaylistName, isShorts = true)
                                    Toast.makeText(context, "Плейлист создан", Toast.LENGTH_SHORT).show()
                                    newPlaylistName = ""
                                    isCreatingNew = false
                                }
                            }
                        ) {
                            Text("Создать")
                        }
                    } else {
                        TextButton(onClick = { showPlaylistDialog = false }) {
                            Text("Готово")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            if (isCreatingNew) {
                                isCreatingNew = false
                            } else {
                                showPlaylistDialog = false
                            }
                        }
                    ) {
                        Text(if (isCreatingNew) "Отмена" else "Закрыть")
                    }
                }
            )
        }
    }
}

@Composable
fun ShortsPlayerItem(
    video: Video,
    channel: Channel?,
    exoPlayer: ExoPlayer,
    isActive: Boolean,
    onChannelClick: (Int) -> Unit,
    viewModel: OfflineTubeViewModel
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var showPauseOverlay by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Sync play state if active page
    LaunchedEffect(isActive) {
        if (isActive) {
            isPlaying = exoPlayer.isPlaying
        }
    }

    // Listen to changes in exoPlayer play state
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                if (isActive) {
                    isPlaying = playing
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        } else {
                            exoPlayer.play()
                            isPlaying = true
                        }
                        // Animate a brief play/pause icon overlay
                        coroutineScope.launch {
                            showPauseOverlay = true
                            delay(600)
                            showPauseOverlay = false
                        }
                    }
                )
            }
    ) {
        // Immersive full screen 9:16 video view with background frame-based thumbnail as placeholder
        Box(modifier = Modifier.fillMaxSize()) {
            com.example.ui.components.VideoFrameThumbnail(
                videoUri = video.videoUri,
                thumbnailUri = video.thumbnailUri,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            if (isActive) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Ambient Bottom Shadow Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )

        // Center Pause/Play Visual Indicator Overlay
        AnimatedVisibility(
            visible = showPauseOverlay,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 1.3f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Overlay Content (Title, Channel details, and Action buttons) - aligned lower for better video view
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 0.dp, start = 16.dp, end = 16.dp) // Aligned lower
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: Channel Avatar, Name, Description & Title
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp) // Tighter vertical spacing
            ) {
                // Channel Info Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable {
                        channel?.let { onChannelClick(it.id) }
                    }
                ) {
                    // Avatar Image with clean white border
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        if (channel?.avatarUri != null && channel.avatarUri.isNotBlank()) {
                            AsyncImage(
                                model = channel.avatarUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Channel Name
                    Text(
                        text = channel?.name ?: "Канал",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Video Title
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
