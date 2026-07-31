package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.AppNotification
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.OfflineTubeViewModel
import com.example.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    viewModel: OfflineTubeViewModel,
    onVideoClick: (Int) -> Unit,
    onChannelClick: (Int) -> Unit
) {
    val videos by viewModel.filteredVideos.collectAsStateWithLifecycle()
    val shortsList = remember(videos) {
        videos.filter { it.isShorts }
    }
    val regularVideos = remember(videos) {
        videos.filter { !it.isShorts }
    }
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedChannelId by viewModel.selectedChannelId.collectAsStateWithLifecycle()
    val isAdultMode by viewModel.isAdultMode.collectAsStateWithLifecycle()
    val videoProgressMap by viewModel.videoProgressMap.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var logoClickCount by remember { mutableStateOf(0) }
    var showAddToPlaylistDialogForVideo by remember { mutableStateOf<Int?>(null) }

    val categories = listOf("Все", "Рекомендованные", "Популярные")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. YouTube-style Header with Toggleable Search
            item {
                if (isSearchActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { 
                            isSearchActive = false
                            viewModel.searchQuery.value = ""
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Назад",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("feed_search_input"),
                            placeholder = { Text("Поиск офлайн видео...", fontSize = 14.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Очистить",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                                viewModel.selectedChannelId.value = null
                                viewModel.selectedCategory.value = "Все"
                                logoClickCount += 1
                                if (logoClickCount >= 3) {
                                    viewModel.toggleAdultMode()
                                    logoClickCount = 0
                                }
                            }
                        ) {
                            val logoColor = if (isAdultMode) Color(0xFFFF5722) else Color.Red
                            Box(
                                modifier = Modifier
                                    .size(width = 30.dp, height = 22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(logoColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Offline",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 20.sp,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = if (isAdultMode) "Tube 18+" else "Tube",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = logoColor,
                                    fontSize = 20.sp,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                        }

                        // Right: Action Icons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { /* Cast placeholder */ }) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = "Трансляция",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            if (!isAdultMode) {
                                val notifications by viewModel.notifications.collectAsStateWithLifecycle()
                                val unreadCount = notifications.count { !it.isRead }
                                var showNotificationsDialog by remember { mutableStateOf(false) }

                                if (showNotificationsDialog) {
                                    NotificationsDialog(
                                        notifications = notifications,
                                        onDismiss = { showNotificationsDialog = false },
                                        onClearAll = { viewModel.clearAllNotifications() },
                                        onNotificationClick = { notif ->
                                            viewModel.markNotificationAsRead(notif.id)
                                            showNotificationsDialog = false
                                            onVideoClick(notif.videoId)
                                        },
                                        onSimulate = { viewModel.simulateNewNotification() }
                                    )
                                }

                                IconButton(onClick = { showNotificationsDialog = true }) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadCount > 0) {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError
                                                ) {
                                                    Text(unreadCount.toString(), fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = "Уведомления",
                                            tint = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Поиск",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Horizontal Carousel of channels (Subscriptions stories style)
            if (channels.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // "Все" item at the beginning
                        item {
                            val isAllSelected = selectedChannelId == null
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.selectedChannelId.value = null }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isAllSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Все",
                                        color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Каналы",
                                    fontSize = 11.sp,
                                    color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        items(channels) { channel ->
                            val isSelected = selectedChannelId == channel.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        if (isSelected) {
                                            viewModel.selectedChannelId.value = null
                                        } else {
                                            viewModel.selectedChannelId.value = channel.id
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    com.example.ui.components.ChannelAvatar(
                                        avatarUri = channel.avatarUri,
                                        name = channel.name,
                                        size = 52,
                                        modifier = Modifier.clickable { onChannelClick(channel.id) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = channel.name,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(64.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.surface, thickness = 1.dp)
                }
            }

            // Shorts Section - Horizontal "шторка" (Curtain / Tray)
            if (shortsList.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Shorts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(shortsList) { video ->
                                val channel = channels.find { it.id == video.channelId }
                                HomeShortsCard(
                                    video = video,
                                    channel = channel,
                                    onClick = { onVideoClick(video.id) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.surface, thickness = 1.dp)
                    }
                }
            }

            // 3. Filter chips (Category chips row) - STICKY!
            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { category ->
                                val isSelected = category == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) Color.White
                                            else Color(0xFF272727)
                                        )
                                        .clickable { viewModel.selectedCategory.value = category }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = category,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.surface, thickness = 1.dp)
                    }
                }
            }

            // 4. Video Feed list or Empty state
            if (regularVideos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 80.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Видео не найдены 🏜️",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) {
                                    "Попробуйте изменить запрос поиска"
                                } else {
                                    "Создайте свой первый канал во вкладке 'Кабинет' и добавьте на него ваши локальные видеофайлы, нажав '+' внизу."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(regularVideos) { video ->
                    val videoChannel = channels.find { it.id == video.channelId }
                    val progress = if (videoChannel?.isLectorMode == true) {
                        videoProgressMap[video.id]
                    } else {
                        null
                    }
                    VideoCard(
                        video = video,
                        channel = videoChannel,
                        onClick = { viewModel.playVideo(video, regularVideos) },
                        onDelete = { viewModel.deleteVideo(video.id) },
                        modifier = Modifier.testTag("video_card_${video.id}"),
                        progress = progress,
                        onClearProgress = { viewModel.clearVideoProgress(video.id) },
                        onChannelClick = { videoChannel?.let { onChannelClick(it.id) } },
                        onAddToPlaylist = { showAddToPlaylistDialogForVideo = video.id }
                    )
                    Divider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), thickness = 4.dp)
                }
            }
        }

        // Add to Playlist Dialog
        if (showAddToPlaylistDialogForVideo != null) {
            com.example.ui.components.AddToPlaylistDialog(
                videoId = showAddToPlaylistDialogForVideo!!,
                playlists = playlists,
                onDismiss = { showAddToPlaylistDialogForVideo = null },
                onAdd = { playlistId, videoId ->
                    viewModel.addVideoToPlaylist(playlistId, videoId)
                    showAddToPlaylistDialogForVideo = null
                },
                onCreatePlaylist = { name ->
                    viewModel.createPlaylist(name)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsDialog(
    notifications: List<AppNotification>,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit,
    onSimulate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Уведомления 🔔",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (notifications.isNotEmpty()) {
                    IconButton(onClick = onClearAll) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Очистить всё",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Button(
                    onClick = onSimulate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("⚡ Симулировать новое уведомление", fontSize = 13.sp)
                }

                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Здесь пусто 📭",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Нажмите кнопку выше, чтобы симулировать новые рекомендации от ваших любимых авторов!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications) { notif ->
                            val timeStr = when (notif.id) {
                                1 -> "1 час назад"
                                2 -> "2 часа назад"
                                3 -> "4 часа назад"
                                else -> "Только что"
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNotificationClick(notif) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notif.isRead) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    }
                                ),
                                border = if (!notif.isRead) {
                                    androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                } else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = notif.channelName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = notif.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!notif.isRead) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = notif.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Рекомендация дня • $timeStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun HomeShortsCard(
    video: com.example.data.Video,
    channel: com.example.data.Channel?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(135.dp)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            com.example.ui.components.VideoFrameThumbnail(
                videoUri = video.videoUri,
                thumbnailUri = video.thumbnailUri,
                modifier = Modifier.fillMaxSize()
            )

            // Dark subtle gradient at the bottom for readability of the text overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )

            // Content overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${video.views} просмотров",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}
