package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Channel
import com.example.data.Playlist
import com.example.data.Video
import com.example.ui.OfflineTubeViewModel
import com.example.ui.components.VideoThumbnail

private data class PlaylistSection(
    val channel: Channel?,
    val playlists: List<Playlist>
)

private fun getPlaylistCountText(count: Int): String {
    val lastTwo = count % 100
    val lastOne = count % 10
    val word = when {
        lastTwo in 11..19 -> "плейлистов"
        lastOne == 1 -> "плейлист"
        lastOne in 2..4 -> "плейлиста"
        else -> "плейлистов"
    }
    return "$count $word"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    viewModel: OfflineTubeViewModel,
    onNavigateToShorts: () -> Unit,
    onVideoClick: (Int) -> Unit
) {
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    
    var selectedPlaylistId by remember { mutableStateOf<Int?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("Все") }
    var sortOption by remember { mutableStateOf("newest") }
    var collapsedGroupKeys by remember { mutableStateOf(setOf<String>()) }

    val filteredPlaylists = remember(playlists, channels, searchQuery, selectedFilterCategory, sortOption) {
        var list = playlists
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
        if (selectedFilterCategory == "Shorts") {
            list = list.filter { it.isShorts }
        } else if (selectedFilterCategory == "По каналам") {
            list = list.filter { it.channelId != 0 && channels.any { ch -> ch.id == it.channelId } }
        } else if (selectedFilterCategory == "Пользовательские") {
            list = list.filter { it.channelId == 0 || channels.none { ch -> ch.id == it.channelId } }
        }

        when (sortOption) {
            "newest" -> list.sortedByDescending { it.createdAt }
            "oldest" -> list.sortedBy { it.createdAt }
            "name" -> list.sortedBy { it.name.lowercase() }
            else -> list
        }
    }

    val sections = remember(filteredPlaylists, channels) {
        val result = mutableListOf<PlaylistSection>()
        
        // Group channel playlists
        val channelGroupMap = filteredPlaylists
            .filter { it.channelId != 0 }
            .groupBy { pl -> channels.find { ch -> ch.id == pl.channelId } }
            
        channelGroupMap.forEach { (channel, plList) ->
            if (channel != null && plList.isNotEmpty()) {
                result.add(PlaylistSection(channel = channel, playlists = plList))
            }
        }
        
        // Custom / user playlists group (channelId == 0 or channel not found)
        val customList = filteredPlaylists.filter { it.channelId == 0 || channels.none { ch -> ch.id == it.channelId } }
        if (customList.isNotEmpty()) {
            result.add(PlaylistSection(channel = null, playlists = customList))
        }
        
        result
    }

    AnimatedContent(
        targetState = selectedPlaylistId,
        transitionSpec = {
            if (targetState != null) {
                // Slide in from right, fade out to left
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                // Slide in from left, fade out to right
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "PlaylistScreenTransition"
    ) { playlistId ->
        if (playlistId == null) {
            // MAIN PLAYLIST LIST SCREEN
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Плейлисты", fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showCreatePlaylistDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("create_playlist_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Создать плейлист"
                        )
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
                    if (playlists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "Нет плейлистов",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Создайте свой первый плейлист и добавляйте видео для офлайн просмотра!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Search Bar
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Поиск плейлистов...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 2.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                )
                            }

                            // Interactive Filter Chips Row
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val categories = listOf("Все", "По каналам", "Пользовательские", "Shorts")
                                    items(categories) { cat ->
                                        FilterChip(
                                            selected = selectedFilterCategory == cat,
                                            onClick = { selectedFilterCategory = cat },
                                            label = { Text(cat, fontSize = 13.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            // Sort Options Bar
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sort,
                                            contentDescription = "Сортировка",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Сортировка:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val sortOptions = listOf(
                                            "newest" to "Новые",
                                            "oldest" to "Старые",
                                            "name" to "А-Я"
                                        )
                                        sortOptions.forEach { (key, label) ->
                                            val isSelected = sortOption == key
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.clickable { sortOption = key }
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (filteredPlaylists.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Ничего не найдено",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            TextButton(onClick = {
                                                searchQuery = ""
                                                selectedFilterCategory = "Все"
                                            }) {
                                                Text("Сбросить фильтры")
                                            }
                                        }
                                    }
                                }
                            } else {
                                sections.forEach { section ->
                                    val groupKey = section.channel?.id?.toString() ?: "custom"
                                    val isCollapsed = collapsedGroupKeys.contains(groupKey)

                                    item(key = "header_$groupKey") {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, bottom = 2.dp)
                                                .clickable {
                                                    collapsedGroupKeys = if (isCollapsed) {
                                                        collapsedGroupKeys - groupKey
                                                    } else {
                                                        collapsedGroupKeys + groupKey
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    if (section.channel != null) {
                                                        if (section.channel.avatarUri.isNotBlank()) {
                                                            AsyncImage(
                                                                model = section.channel.avatarUri,
                                                                contentDescription = section.channel.name,
                                                                modifier = Modifier
                                                                    .size(44.dp)
                                                                    .clip(CircleShape),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(44.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = section.channel.name.take(1).uppercase(),
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                                )
                                                            }
                                                        }

                                                        Column {
                                                            Text(
                                                                text = section.channel.name,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = getPlaylistCountText(section.playlists.size),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.secondary
                                                            )
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(44.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Folder,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }

                                                        Column {
                                                            Text(
                                                                text = "Пользовательские плейлисты",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = getPlaylistCountText(section.playlists.size),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.secondary
                                                            )
                                                        }
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        collapsedGroupKeys = if (isCollapsed) {
                                                            collapsedGroupKeys - groupKey
                                                        } else {
                                                            collapsedGroupKeys + groupKey
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                                        contentDescription = if (isCollapsed) "Развернуть" else "Свернуть",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (!isCollapsed) {
                                        items(section.playlists, key = { "pl_${it.id}" }) { playlist ->
                                            PlaylistRowCard(
                                                playlist = playlist,
                                                viewModel = viewModel,
                                                onClick = { selectedPlaylistId = playlist.id },
                                                onDelete = { viewModel.deletePlaylist(playlist.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // DETAILED PLAYLIST VIEW
            val activePlaylist = playlists.find { it.id == playlistId }
            if (activePlaylist != null) {
                PlaylistDetailView(
                    playlist = activePlaylist,
                    channels = channels,
                    viewModel = viewModel,
                    onBack = { selectedPlaylistId = null },
                    onNavigateToShorts = onNavigateToShorts,
                    onVideoClick = onVideoClick
                )
            } else {
                selectedPlaylistId = null
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        var playlistDesc by remember { mutableStateOf("") }
        var coverUri by remember { mutableStateOf("") }
        var isShortsPlaylist by remember { mutableStateOf(false) }
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                coverUri = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Новый плейлист", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = playlistDesc,
                        onValueChange = { playlistDesc = it },
                        label = { Text("Описание (необязательно)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Плейлист Shorts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Проигрывать видео в формате Shorts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Switch(
                            checked = isShortsPlaylist,
                            onCheckedChange = { isShortsPlaylist = it }
                        )
                    }

                    Text("Обложка плейлиста", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 100.dp, height = 56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverUri.isNotEmpty()) {
                                AsyncImage(
                                    model = coverUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "Добавить фото", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (coverUri.isNotEmpty()) {
                            TextButton(onClick = { coverUri = "" }) {
                                Text("Сбросить", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Text("По умолчанию (первое видео)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName, playlistDesc, coverUri, isShorts = isShortsPlaylist)
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
}

@Composable
fun PlaylistRowCard(
    playlist: Playlist,
    viewModel: OfflineTubeViewModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val playlistVideos by viewModel.getVideosForPlaylist(playlist.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val firstVideo = playlistVideos.firstOrNull()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val channels by viewModel.allChannels.collectAsStateWithLifecycle(initialValue = emptyList())
    val playlistChannel = remember(channels, playlist.channelId) {
        channels.find { it.id == playlist.channelId }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить плейлист?", fontWeight = FontWeight.Bold) },
            text = { Text("Вы действительно хотите удалить плейлист '${playlist.name}'? Сами видео на устройстве не будут удалены.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail section styled like native YouTube
        Box(
            modifier = Modifier
                .size(width = 140.dp, height = 78.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            if (playlist.coverUri.isNotBlank()) {
                AsyncImage(
                    model = playlist.coverUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (firstVideo != null) {
                VideoThumbnail(
                    thumbnailUri = firstVideo.thumbnailUri,
                    duration = firstVideo.duration,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Right overlay with playlist symbol and video count
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(44.dp)
                    .align(Alignment.CenterEnd)
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${playlistVideos.size}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Info Section
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (playlistChannel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = playlistChannel.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Text(
                text = if (playlist.description.isNotBlank()) playlist.description else "Нет описания",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${playlistVideos.size} видео",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Deletion option
        IconButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Удалить плейлист",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    channels: List<Channel>,
    viewModel: OfflineTubeViewModel,
    onBack: () -> Unit,
    onNavigateToShorts: () -> Unit,
    onVideoClick: (Int) -> Unit
) {
    val playlistVideos by viewModel.getVideosForPlaylist(playlist.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить плейлист?", fontWeight = FontWeight.Bold) },
            text = { Text("Вы действительно хотите полностью удалить плейлист '${playlist.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showEditDialog) {
        var editName by remember { mutableStateOf(playlist.name) }
        var editDesc by remember { mutableStateOf(playlist.description) }
        var editCoverUri by remember { mutableStateOf<String?>(playlist.coverUri) }
        val editLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                editCoverUri = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Редактировать плейлист", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Обложка плейлиста", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 100.dp, height = 56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { editLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!editCoverUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = editCoverUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "Добавить фото", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (!editCoverUri.isNullOrBlank()) {
                            TextButton(onClick = { editCoverUri = "" }) {
                                Text("Сбросить", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Text("По умолчанию (первое видео)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updatePlaylistWithCover(
                                playlist.copy(name = editName, description = editDesc),
                                editCoverUri
                            )
                            showEditDialog = false
                        }
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Cover Banner Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 230.dp)
        ) {
            val firstVideo = playlistVideos.firstOrNull()
            // Full background image or fallback gradient
            if (playlist.coverUri.isNotBlank()) {
                AsyncImage(
                    model = playlist.coverUri,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (firstVideo != null) {
                AsyncImage(
                    model = firstVideo.thumbnailUri,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            // Dark gradient scrim overlay to make top buttons & bottom text super readable
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            // Content layered over the full banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Редактировать",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить плейлист",
                                tint = Color.Red.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Playlist Title, Stats & Description
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${playlistVideos.size} видео",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        if (playlist.isShorts) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Shorts",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (playlist.description.isNotBlank()) {
                        Text(
                            text = playlist.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons Row (compact icon buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = {
                            if (playlist.isShorts) {
                                viewModel.playlistShortsToPlay.value = playlistVideos
                                onNavigateToShorts()
                            } else {
                                playlistVideos.firstOrNull()?.let { firstVideo ->
                                    viewModel.playVideo(firstVideo, playlistVideos)
                                }
                            }
                        },
                        enabled = playlistVideos.isNotEmpty(),
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Смотреть всё",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            if (playlistVideos.isNotEmpty()) {
                                if (playlist.isShorts) {
                                    viewModel.playlistShortsToPlay.value = playlistVideos.shuffled()
                                    onNavigateToShorts()
                                } else {
                                    val shuffledList = playlistVideos.shuffled()
                                    val randomVideo = shuffledList.first()
                                    viewModel.playVideo(randomVideo, shuffledList)
                                }
                            }
                        },
                        enabled = playlistVideos.isNotEmpty(),
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.25f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Перемешать",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Scrollable list of video items inside playlist
        Box(modifier = Modifier.weight(1f)) {
            if (playlistVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Плейлист пуст",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Вы можете добавить любые сохраненные видео в этот плейлист со страницы проигрывателя.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(playlistVideos) { index, video ->
                        val videoChannel = channels.find { it.id == video.channelId }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (playlist.isShorts) {
                                        viewModel.playlistShortsToPlay.value = playlistVideos
                                        viewModel.selectedShortsId.value = video.id
                                        onNavigateToShorts()
                                    } else {
                                        viewModel.playVideo(video, playlistVideos)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Video Thumbnail (left)
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 62.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                VideoThumbnail(
                                    thumbnailUri = video.thumbnailUri,
                                    duration = video.duration,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Details text (middle)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${videoChannel?.name ?: "Канал"} • ${video.views} просмотров",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Move Up / Move Down buttons
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val newList = playlistVideos.map { it.id }.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index - 1]
                                            newList[index - 1] = temp
                                            viewModel.reorderPlaylist(playlist.id, newList)
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Переместить вверх",
                                        tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (index < playlistVideos.size - 1) {
                                            val newList = playlistVideos.map { it.id }.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index + 1]
                                            newList[index + 1] = temp
                                            viewModel.reorderPlaylist(playlist.id, newList)
                                        }
                                    },
                                    enabled = index < playlistVideos.size - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Переместить вниз",
                                        tint = if (index < playlistVideos.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Remove action (right)
                            IconButton(
                                onClick = { viewModel.removeVideoFromPlaylist(playlist.id, video.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить из плейлиста",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
