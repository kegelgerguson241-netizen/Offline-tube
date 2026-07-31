package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Channel
import com.example.data.Video
import com.example.data.Playlist
import com.example.ui.OfflineTubeViewModel
import com.example.ui.FileUtils
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.VideoCard
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    viewModel: OfflineTubeViewModel,
    onVideoClick: (Int) -> Unit,
    onNavigateToShorts: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val videos by viewModel.allVideos.collectAsStateWithLifecycle()

    val activeChannelId by viewModel.activeChannelDetailId.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    // LIFTED CHANNEL CREATION STATE FOR PICKERS
    var createName by remember { mutableStateOf("") }
    var createDesc by remember { mutableStateOf("") }
    var createAvatarUrl by remember { mutableStateOf("") }
    var createBannerUrl by remember { mutableStateOf("") }
    var createIsAgeRestricted by remember { mutableStateOf(false) }

    val createAvatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copied = FileUtils.copyUriToInternalStorage(context, it, "avatars")
            if (copied != null) {
                createAvatarUrl = copied
            } else {
                createAvatarUrl = it.toString()
            }
        }
    }

    val createBannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copied = FileUtils.copyUriToInternalStorage(context, it, "banners")
            if (copied != null) {
                createBannerUrl = copied
            } else {
                createBannerUrl = it.toString()
            }
        }
    }

    val activeChannel = channels.find { it.id == activeChannelId }

    if (activeChannel != null) {
        BackHandler {
            viewModel.activeChannelDetailId.value = null
        }
        // Channel Detail View
        ChannelDetailScreen(
            channel = activeChannel,
            videos = videos.filter { it.channelId == activeChannel.id },
            viewModel = viewModel,
            onBack = { viewModel.activeChannelDetailId.value = null },
            onNavigateToShorts = onNavigateToShorts,
            onVideoClick = onVideoClick
        )
    } else {
        // Channels List View
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Каналы", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Назад"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        createName = ""
                        createDesc = ""
                        createAvatarUrl = ""
                        createBannerUrl = ""
                        createIsAgeRestricted = false
                        showCreateDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_cabinet_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Создать")
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                if (true) {
                    // TAB 0: CHANNELS
                    if (channels.isEmpty()) {
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
                                Text(
                                    text = "Каналов нет",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Нажмите '+' внизу чтобы создать свой первый уникальный канал и кастомизировать его аватарку, баннер и описание.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
                        ) {
                            items(channels) { channel ->
                                val channelVideosCount = videos.count { it.channelId == channel.id }
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clickable { viewModel.activeChannelDetailId.value = channel.id }
                                        .testTag("channel_item_${channel.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        ChannelAvatar(
                                            avatarUri = channel.avatarUri,
                                            name = channel.name,
                                            size = 56
                                        )
                                        
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = channel.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "$channelVideosCount видео",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = channel.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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

    // Channel Creation Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Создать Канал") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it },
                        label = { Text("Название канала") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_channel_name_input")
                    )
                    OutlinedTextField(
                        value = createDesc,
                        onValueChange = { createDesc = it },
                        label = { Text("Описание канала") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Avatar picking Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Аватар канала",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (createAvatarUrl.isNotEmpty()) "Выбран" else "Не выбран (стандартный)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (createAvatarUrl.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { createAvatarPickerLauncher.launch("image/*") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = "Выбрать аватар",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Banner picking Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Баннер канала",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (createBannerUrl.isNotEmpty()) "Выбран" else "Не выбран (стандартный)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (createBannerUrl.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { createBannerPickerLauncher.launch("image/*") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = "Выбрать баннер",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Age restricted toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked = createIsAgeRestricted,
                            onCheckedChange = { createIsAgeRestricted = it }
                        )
                        Text("Возрастное ограничение 18+", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createChannel(createName, createDesc, createAvatarUrl, createBannerUrl, 0, createIsAgeRestricted)
                        showCreateDialog = false
                    },
                    modifier = Modifier.testTag("submit_channel_button")
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun ChannelDetailScreen(
    channel: Channel,
    videos: List<Video>,
    viewModel: OfflineTubeViewModel,
    onBack: () -> Unit,
    onNavigateToShorts: () -> Unit,
    onVideoClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val videoProgressMap by viewModel.videoProgressMap.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var editAvatar by remember { mutableStateOf("") }
    var editBanner by remember { mutableStateOf("") }
    var editIsAgeRestricted by remember { mutableStateOf(false) }
    var editIsLectorMode by remember { mutableStateOf(false) }

    val editAvatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copied = FileUtils.copyUriToInternalStorage(context, it, "avatars")
            if (copied != null) {
                editAvatar = copied
            } else {
                editAvatar = it.toString()
            }
        }
    }

    val editBannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copied = FileUtils.copyUriToInternalStorage(context, it, "banners")
            if (copied != null) {
                editBanner = copied
            } else {
                editBanner = it.toString()
            }
        }
    }

    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val channelPlaylists = remember(playlists, channel) {
        playlists.filter { it.channelId == channel.id }
    }

    LaunchedEffect(showEditDialog, channel) {
        if (showEditDialog) {
            editName = channel.name
            editDesc = channel.description
            editAvatar = channel.avatarUri
            editBanner = channel.bannerUri
            editIsAgeRestricted = channel.isAgeRestricted
            editIsLectorMode = channel.isLectorMode
        }
    }

    var showAddToPlaylistDialogForVideo by remember { mutableStateOf<Int?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Видео, 1: Плейлисты
    var selectedPlaylistIdInsideChannel by remember { mutableStateOf<Int?>(null) }
    var showCreatePlaylistDialogInsideChannel by remember { mutableStateOf(false) }

    val activePlaylistInsideChannel = remember(playlists, selectedPlaylistIdInsideChannel) {
        playlists.find { it.id == selectedPlaylistIdInsideChannel }
    }

    val shortsVideos = remember(videos) {
        videos.filter { it.isShorts }.sortedByDescending { it.uploadDate }
    }
    val regularVideos = remember(videos) {
        videos.filter { !it.isShorts }.sortedByDescending { it.uploadDate }
    }

    if (activePlaylistInsideChannel != null) {
        PlaylistDetailView(
            playlist = activePlaylistInsideChannel,
            channels = channels,
            viewModel = viewModel,
            onBack = { selectedPlaylistIdInsideChannel = null },
            onNavigateToShorts = onNavigateToShorts,
            onVideoClick = onVideoClick
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Channel Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val bannerGradient = when (channel.bannerUri) {
                        "space_banner" -> Brush.horizontalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
                        "nature_banner" -> Brush.horizontalGradient(listOf(Color(0xFF134E5E), Color(0xFF71B280)))
                        "dzen_banner" -> Brush.horizontalGradient(listOf(Color(0xFF43C6AC), Color(0xFF191654)))
                        "ocean_banner" -> Brush.horizontalGradient(listOf(Color(0xFF028090), Color(0xFF00A896), Color(0xFF02C39A)))
                        "sunset_banner" -> Brush.horizontalGradient(listOf(Color(0xFFe65c00), Color(0xFFF9D423)))
                        "neon_banner" -> Brush.horizontalGradient(listOf(Color(0xFFf72585), Color(0xFF7209b7), Color(0xFF3f37c9)))
                        else -> null
                    }

                        if (bannerGradient != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(bannerGradient)
                            )
                        } else if (channel.bannerUri.isNotEmpty() && channel.bannerUri != "default_banner") {
                            coil.compose.AsyncImage(
                                model = channel.bannerUri,
                                contentDescription = "Баннер канала",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                            )
                                        )
                                    )
                            )
                        }

                        // Back button
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.TopStart)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White
                            )
                        }

                        // Edit & Delete actions row
                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.TopEnd),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Редактировать канал",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить канал",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }

                // 2. Identity Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ChannelAvatar(
                            avatarUri = channel.avatarUri,
                            name = channel.name,
                            size = 72
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (channel.isLectorMode) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = "Режим Лектора активен",
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${videos.size} видео",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // 3. Description
                if (channel.description.isNotEmpty()) {
                    item {
                        Text(
                            text = channel.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                }



                // 5. VIDEOS & PLAYLISTS TAB ROW
                @OptIn(ExperimentalFoundationApi::class)
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background,
                        tonalElevation = 1.dp
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Видео", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Плейлисты", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }

                // 6. TAB CONTENT
                if (selectedTab == 0) {
                    if (shortsVideos.isEmpty() && regularVideos.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "На этом канале еще нет видео.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    } else {
                        // Section: Horizontal Scrolling Shorts Row (if present)
                        if (shortsVideos.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "Shorts 🎬",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(shortsVideos) { video ->
                                            ChannelShortsCard(
                                                video = video,
                                                onClick = { onVideoClick(video.id) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), thickness = 1.dp)
                                }
                            }
                        }

                        // Section: Regular Long-Form Videos (if present)
                        if (regularVideos.isNotEmpty()) {
                            items(regularVideos) { video ->
                                val progress = videoProgressMap[video.id]
                                VideoCard(
                                    video = video,
                                    channel = channel,
                                    onClick = { viewModel.playVideo(video, regularVideos) },
                                    onDelete = { viewModel.deleteVideo(video.id) },
                                    progress = progress,
                                    onClearProgress = { viewModel.clearVideoProgress(video.id) },
                                    onAddToPlaylist = { showAddToPlaylistDialogForVideo = video.id }
                                )
                                Divider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), thickness = 4.dp)
                            }
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Плейлисты канала",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Button(
                                onClick = { showCreatePlaylistDialogInsideChannel = true },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Создать", fontSize = 12.sp)
                            }
                        }
                    }

                    if (channelPlaylists.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "На этом канале еще нет плейлистов.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    } else {
                        items(channelPlaylists) { playlist ->
                            PlaylistRowCard(
                                playlist = playlist,
                                viewModel = viewModel,
                                onClick = { selectedPlaylistIdInsideChannel = playlist.id },
                                onDelete = { viewModel.deletePlaylist(playlist.id) }
                            )
                            Divider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), thickness = 1.dp)
                        }
                    }
                }
            }

            // Dialogs
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
                        viewModel.createPlaylist(name, channelId = channel.id)
                    }
                )
            }

            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text("Удалить канал?", fontWeight = FontWeight.Bold) },
                    text = { Text("Вы действительно хотите удалить канал '${channel.name}'? Все его видео и история просмотров будут безвозвратно удалены.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteChannel(channel.id)
                                showDeleteConfirmation = false
                                onBack() // Go back to cabinet list
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Удалить", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }

            if (showCreatePlaylistDialogInsideChannel) {
                var playlistName by remember { mutableStateOf("") }
                var playlistDesc by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreatePlaylistDialogInsideChannel = false },
                    title = { Text("Создать плейлист канала", fontWeight = FontWeight.Bold) },
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
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (playlistName.isNotBlank()) {
                                    viewModel.createPlaylist(playlistName, playlistDesc, channelId = channel.id)
                                    showCreatePlaylistDialogInsideChannel = false
                                }
                            },
                            enabled = playlistName.isNotBlank()
                        ) {
                            Text("Создать")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreatePlaylistDialogInsideChannel = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Редактировать канал") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Название канала") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Описание канала") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Avatar picking Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Аватар канала",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (editAvatar.isNotEmpty()) "Выбран" else "Не выбран (стандартный)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (editAvatar.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { editAvatarPickerLauncher.launch("image/*") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = "Выбрать аватар",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Banner picking Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Баннер канала",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (editBanner.isNotEmpty()) "Выбран" else "Не выбран (стандартный)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (editBanner.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { editBannerPickerLauncher.launch("image/*") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = "Выбрать баннер",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Age restricted toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked = editIsAgeRestricted,
                            onCheckedChange = { editIsAgeRestricted = it }
                        )
                        Text("Возрастное ограничение 18+", style = MaterialTheme.typography.bodyMedium)
                    }

                    // Lector Mode toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked = editIsLectorMode,
                            onCheckedChange = { editIsLectorMode = it }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = if (editIsLectorMode) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text("Режим Лектора", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Сохраняет прогресс просмотра видео", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateChannel(
                            channel.copy(
                                name = editName,
                                description = editDesc,
                                avatarUri = editAvatar,
                                bannerUri = editBanner,
                                isAgeRestricted = editIsAgeRestricted,
                                isLectorMode = editIsLectorMode
                            )
                        )
                        showEditDialog = false
                    }
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
}

@Composable
fun PlaylistExpandableCard(
    playlist: Playlist,
    viewModel: OfflineTubeViewModel,
    onVideoClick: (Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val playlistVideos by viewModel.getVideosForPlaylist(playlist.id).collectAsStateWithLifecycle(initialValue = emptyList())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("playlist_card_${playlist.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${playlistVideos.size} видео",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.deletePlaylist(playlist.id) },
                        modifier = Modifier.testTag("delete_playlist_${playlist.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить плейлист",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (isExpanded) {
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                if (playlistVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "В этом плейлисте пока нет видео. Добавьте их во время просмотра видео!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                playlistVideos.firstOrNull()?.let { firstVideo ->
                                    viewModel.playVideo(firstVideo, playlistVideos)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("play_playlist_button")
                        ) {
                            Text("Воспроизвести все", color = Color.White)
                        }
                    }

                    playlistVideos.forEach { video ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playVideo(video, playlistVideos) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = video.duration,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.removeVideoFromPlaylist(playlist.id, video.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить видео из плейлиста",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelShortsCard(
    video: Video,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        // Thumbnail card in 9:16 aspect ratio
        Card(
            modifier = Modifier
                .width(130.dp)
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(12.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                com.example.ui.components.VideoFrameThumbnail(
                    videoUri = video.videoUri,
                    thumbnailUri = video.thumbnailUri,
                    modifier = Modifier.fillMaxSize()
                )
                // Play overlay icon
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${video.views} просмотров",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


