package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.AudioPlaylist
import com.example.data.AudioTrack
import com.example.ui.OfflineTubeViewModel
import com.example.ui.FileUtils
import com.example.service.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScreen(viewModel: OfflineTubeViewModel) {
    val context = LocalContext.current
    val audioTracks by viewModel.allAudioTracks.collectAsStateWithLifecycle()
    val audioPlaylists by viewModel.allAudioPlaylists.collectAsStateWithLifecycle()

    val groupedByDecade = remember(audioTracks) {
        audioTracks.groupBy { (it.year / 10) * 10 }
    }

    val isPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentAudioTrack.collectAsStateWithLifecycle()
    val playbackPosition by viewModel.audioPlaybackPosition.collectAsStateWithLifecycle()
    val duration by viewModel.audioDuration.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Songs, 1: Playlists
    var showAddTrackDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    
    // Add track to playlist dialog states
    var trackToAddToPlaylist by remember { mutableStateOf<AudioTrack?>(null) }
    var trackToDelete by remember { mutableStateOf<AudioTrack?>(null) }
    var playlistToDelete by remember { mutableStateOf<AudioPlaylist?>(null) }
    var trackToRemoveFromPlaylist by remember { mutableStateOf<Pair<AudioPlaylist, AudioTrack>?>(null) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        BackHandler(enabled = isPlayerExpanded) {
            isPlayerExpanded = false
        }

        Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Offline Music", fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        if (activeTab == 0) {
                            TextButton(
                                onClick = { showAddTrackDialog = true },
                                modifier = Modifier.testTag("add_song_fab")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Добавить песню",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить", color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            TextButton(
                                onClick = { showCreatePlaylistDialog = true },
                                modifier = Modifier.testTag("add_playlist_fab")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = "Создать плейлист",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Создать", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Песни", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Плейлисты", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (activeTab == 0) {
                    // SONGS LIST
                    if (audioTracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Text(
                                    text = "Музыкальная коллекция пуста",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Добавьте ваши любимые оффлайн аудиофайлы с устройства, нажав на иконку внизу.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            groupedByDecade.forEach { (decade, tracksInDecade) ->
                                item(key = "decade_$decade") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            thickness = 1.dp
                                        )
                                        Text(
                                            text = "${decade}-е гг.",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            thickness = 1.dp
                                        )
                                    }
                                }

                                items(tracksInDecade, key = { it.id }) { track ->
                                    val isCurrent = currentTrack?.id == track.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.playAudioTrack(track, audioTracks)
                                                isPlayerExpanded = true
                                            }
                                            .testTag("audio_track_${track.id}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Track Cover/Art preview with overlay
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (track.coverUri.isNotBlank()) {
                                                    AsyncImage(
                                                        model = track.coverUri,
                                                        contentDescription = "Обложка трека",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    if (isCurrent) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color.Black.copy(alpha = 0.35f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(
                                                                Brush.linearGradient(
                                                                    colors = if (isCurrent) listOf(
                                                                        MaterialTheme.colorScheme.primary,
                                                                        MaterialTheme.colorScheme.secondary
                                                                    ) else listOf(
                                                                        Color(0xFF3E3E3E),
                                                                        Color(0xFF242424)
                                                                    )
                                                                )
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.MusicNote,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = track.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${if (track.artist.isBlank()) "Неизвестный исполнитель" else track.artist} • ${track.year}",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = track.duration,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                                
                                                var showTrackOptions by remember { mutableStateOf(false) }
                                                Box {
                                                    IconButton(
                                                        onClick = { showTrackOptions = true },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.MoreVert,
                                                            contentDescription = "Опции трека",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    DropdownMenu(
                                                        expanded = showTrackOptions,
                                                        onDismissRequest = { showTrackOptions = false }
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("Добавить в плейлист") },
                                                            leadingIcon = {
                                                                Icon(
                                                                    imageVector = Icons.Default.PlaylistAdd,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.primary
                                                                )
                                                            },
                                                            onClick = {
                                                                showTrackOptions = false
                                                                trackToAddToPlaylist = track
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Удалить из медиатеки", color = MaterialTheme.colorScheme.error) },
                                                            leadingIcon = {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.error
                                                                )
                                                            },
                                                            onClick = {
                                                                showTrackOptions = false
                                                                trackToDelete = track
                                                            }
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
                } else {
                    // PLAYLISTS LIST
                    if (audioPlaylists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Text(
                                    text = "Нет аудио плейлистов",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Создайте ваш первый музыкальный плейлист и добавляйте туда песни из медиатеки.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(audioPlaylists) { playlist ->
                                var isExpanded by remember { mutableStateOf(false) }
                                val playlistTracksFlow = remember(playlist.id) { viewModel.getTracksForAudioPlaylist(playlist.id) }
                                val playlistTracks by playlistTracksFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isExpanded = !isExpanded },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.QueueMusic,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = playlist.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${playlistTracks.size} аудиозаписей",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { playlistToDelete = playlist }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Удалить плейлист",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                    )
                                                }
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                            Spacer(modifier = Modifier.height(4.dp))

                                            if (playlistTracks.isEmpty()) {
                                                Text(
                                                    text = "Плейлист пуст. Добавьте песни во вкладке 'Песни'!",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 12.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            } else {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    FilledIconButton(
                                                        onClick = {
                                                            playlistTracks.firstOrNull()?.let { firstTrack ->
                                                                viewModel.playAudioTrack(firstTrack, playlistTracks)
                                                                isPlayerExpanded = true
                                                            }
                                                        },
                                                        modifier = Modifier.size(34.dp),
                                                        colors = IconButtonDefaults.filledIconButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.primary,
                                                            contentColor = Color.White
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = "Слушать все",
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    FilledTonalIconButton(
                                                        onClick = {
                                                            val shuffledTracks = playlistTracks.shuffled()
                                                            shuffledTracks.firstOrNull()?.let { firstTrack ->
                                                                viewModel.playAudioTrack(firstTrack, shuffledTracks)
                                                                isPlayerExpanded = true
                                                            }
                                                        },
                                                        modifier = Modifier.size(34.dp),
                                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Shuffle,
                                                            contentDescription = "Перемешать",
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                playlistTracks.forEach { track ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                viewModel.playAudioTrack(track, playlistTracks)
                                                                isPlayerExpanded = true
                                                            }
                                                            .padding(vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            // Small Cover
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(40.dp)
                                                                    .clip(RoundedCornerShape(6.dp)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (track.coverUri.isNotBlank()) {
                                                                    AsyncImage(
                                                                        model = track.coverUri,
                                                                        contentDescription = null,
                                                                        modifier = Modifier.fillMaxSize(),
                                                                        contentScale = ContentScale.Crop
                                                                    )
                                                                } else {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .fillMaxSize()
                                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Default.MusicNote,
                                                                            contentDescription = null,
                                                                            tint = MaterialTheme.colorScheme.primary,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            Column {
                                                                Text(
                                                                    text = track.title,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = "${if (track.artist.isBlank()) "Неизвестный исполнитель" else track.artist} • ${track.year}",
                                                                    fontSize = 12.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }

                                                        IconButton(
                                                            onClick = { trackToRemoveFromPlaylist = Pair(playlist, track) },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Удалить из плейлиста",
                                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
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
        }
    }

    // FLOATING MUSIC PLAYER OVERLAYS (Outside Scaffold, inside root Box)
    if (currentTrack != null) {
        // Expanded fullscreen player console
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E1E1E),
                                Color(0xFF121212),
                                Color(0xFF070707)
                            )
                        )
                    )
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                            // Header collapse row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { isPlayerExpanded = false }) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Свернуть",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = "ИГРАЕТ СЕЙЧАС",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 2.sp
                                )
                                Box(modifier = Modifier.size(32.dp))
                            }

                            // Premium Responsive Cover Image Box
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .sizeIn(minWidth = 120.dp, minHeight = 120.dp, maxWidth = 180.dp, maxHeight = 180.dp)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF1F1F1F)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentTrack!!.coverUri.isNotBlank()) {
                                    AsyncImage(
                                        model = currentTrack!!.coverUri,
                                        contentDescription = "Обложка альбома",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.secondary
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }
                            }

                            // Title & artist details
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = currentTrack!!.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (currentTrack!!.artist.isBlank()) "Неизвестный исполнитель" else currentTrack!!.artist,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Progress Slider Bar
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Slider(
                                    value = playbackPosition.toFloat(),
                                    onValueChange = { viewModel.seekAudioTo(it.toLong()) },
                                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val positionSec = playbackPosition / 1000
                                    val durationSec = duration / 1000
                                    Text(
                                        text = String.format("%d:%02d", positionSec / 60, positionSec % 60),
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = String.format("%d:%02d", durationSec / 60, durationSec % 60),
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            // Controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val repeatTrackEnabled by viewModel.audioRepeatTrackEnabled.collectAsStateWithLifecycle()
                                IconButton(
                                    onClick = { viewModel.toggleAudioRepeatTrack() },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = "Автоповтор трека",
                                            tint = if (repeatTrackEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        if (repeatTrackEnabled) {
                                            Text(
                                                text = "1",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .background(Color.Black, shape = CircleShape)
                                                    .padding(horizontal = 2.dp)
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.playPreviousAudio() },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Назад",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                FloatingActionButton(
                                    onClick = { viewModel.toggleAudioPlayPause() },
                                    containerColor = Color.White,
                                    contentColor = Color.Black,
                                    shape = CircleShape,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Играть/Пауза",
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.playNextAudio() },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Вперед",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                val shuffleEnabled by viewModel.audioShuffleEnabled.collectAsStateWithLifecycle()
                                IconButton(
                                    onClick = { viewModel.toggleAudioShuffle() },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = "Случайный порядок",
                                        tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Close/Stop Button
                            TextButton(
                                onClick = {
                                    viewModel.stopAudioPlayback()
                                    isPlayerExpanded = false
                                },
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ОСТАНОВИТЬ ПЛЕЕР",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                // Small floating player bar at bottom when player is collapsed
                if (!isPlayerExpanded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { isPlayerExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("collapsed_music_player")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Mini Track Cover
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentTrack!!.coverUri.isNotBlank()) {
                                        AsyncImage(
                                            model = currentTrack!!.coverUri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.secondary
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack!!.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (currentTrack!!.artist.isBlank()) "Неизвестный исполнитель" else currentTrack!!.artist,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { viewModel.playPreviousAudio() }) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Назад",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleAudioPlayPause() }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Играть/Пауза",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                IconButton(onClick = { viewModel.playNextAudio() }) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Вперед",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(onClick = { viewModel.stopAudioPlayback() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Закрыть",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

    // DIALOGS

    if (showAddTrackDialog) {
        var trackTitle by remember { mutableStateOf("") }
        var trackArtist by remember { mutableStateOf("") }
        var trackUri by remember { mutableStateOf("") }
        var trackCoverUri by remember { mutableStateOf("") }
        var trackYear by remember { mutableStateOf("2026") }

        val pickAudioLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri: Uri? ->
                if (uri != null) {
                    val copied = FileUtils.copyUriToInternalStorage(context, uri, "audio")
                    trackUri = copied ?: uri.toString()
                    val autoTitle = FileUtils.getFileNameWithoutExtension(context, uri)
                    if (autoTitle.isNotEmpty() && trackTitle.isBlank()) {
                        trackTitle = autoTitle
                    }
                }
            }
        )

        val pickCoverLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri: Uri? ->
                if (uri != null) {
                    trackCoverUri = uri.toString()
                }
            }
        )

        AlertDialog(
            onDismissRequest = { showAddTrackDialog = false },
            title = { Text("Добавить аудиозапись", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Audio File Selection Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { pickAudioLauncher.launch(arrayOf("audio/*")) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Аудиофайл",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (trackUri.isNotBlank()) "Файл выбран" else "Нажмите для выбора файла",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (trackUri.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (trackUri.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = if (trackUri.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = trackTitle,
                        onValueChange = { trackTitle = it },
                        label = { Text("Название песни") },
                        modifier = Modifier.fillMaxWidth().testTag("add_audio_title_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = trackArtist,
                        onValueChange = { trackArtist = it },
                        label = { Text("Исполнитель / Группа") },
                        modifier = Modifier.fillMaxWidth().testTag("add_audio_artist_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = trackYear,
                        onValueChange = { trackYear = it.filter { c -> c.isDigit() } },
                        label = { Text("Год выпуска") },
                        modifier = Modifier.fillMaxWidth().testTag("add_audio_year_input"),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )

                    // Cover Image Selection Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { pickCoverLauncher.launch(arrayOf("image/*")) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Обложка песни (превью)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (trackCoverUri.isNotBlank()) "Обложка выбрана" else "Необязательно",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (trackCoverUri.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (trackCoverUri.isNotBlank()) {
                            AsyncImage(
                                model = trackCoverUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (trackTitle.isNotBlank() && trackUri.isNotBlank()) {
                            val yearVal = trackYear.toIntOrNull() ?: 2026
                            viewModel.addAudioTrack(trackTitle, trackArtist, trackUri, trackCoverUri, yearVal)
                            showAddTrackDialog = false
                        }
                    },
                    enabled = trackTitle.isNotBlank() && trackUri.isNotBlank(),
                    modifier = Modifier.testTag("add_audio_confirm")
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTrackDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        var playlistDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Новый аудио плейлист", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Название плейлиста") },
                        modifier = Modifier.fillMaxWidth().testTag("playlist_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = playlistDesc,
                        onValueChange = { playlistDesc = it },
                        label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createAudioPlaylist(playlistName, playlistDesc)
                            showCreatePlaylistDialog = false
                        }
                    },
                    enabled = playlistName.isNotBlank()
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Add track to playlist picker dialog
    if (trackToAddToPlaylist != null) {
        AlertDialog(
            onDismissRequest = { trackToAddToPlaylist = null },
            title = { Text("Добавить в плейлист", fontWeight = FontWeight.Bold) },
            text = {
                if (audioPlaylists.isEmpty()) {
                    Text(
                        text = "У вас еще нет аудио плейлистов. Создайте их во вкладке 'Плейлисты'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(audioPlaylists) { playlist ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addTrackToAudioPlaylist(playlist.id, trackToAddToPlaylist!!.id)
                                        trackToAddToPlaylist = null
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = playlist.name,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { trackToAddToPlaylist = null }) {
                    Text("Закрыть")
                }
            }
        )
    }

    // Confirmation dialog for deleting a track from library
    if (trackToDelete != null) {
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Удалить песню?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Вы действительно хотите удалить песню «${trackToDelete?.title}» из медиатеки? Это действие нельзя будет отменить.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        trackToDelete?.let { viewModel.deleteAudioTrack(it.id) }
                        trackToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { trackToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Confirmation dialog for deleting a playlist
    if (playlistToDelete != null) {
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Удалить плейлист?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Вы действительно хотите удалить плейлист «${playlistToDelete?.name}»?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        playlistToDelete?.let { viewModel.deleteAudioPlaylist(it.id) }
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Confirmation dialog for removing a track from playlist
    if (trackToRemoveFromPlaylist != null) {
        val (playlist, track) = trackToRemoveFromPlaylist!!
        AlertDialog(
            onDismissRequest = { trackToRemoveFromPlaylist = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Удалить из плейлиста?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Удалить песню «${track.title}» из плейлиста «${playlist.name}»?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeTrackFromAudioPlaylist(playlist.id, track.id)
                        trackToRemoveFromPlaylist = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { trackToRemoveFromPlaylist = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}
}
