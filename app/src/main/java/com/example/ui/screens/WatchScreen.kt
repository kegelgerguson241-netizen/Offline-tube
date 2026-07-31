package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.data.Channel
import com.example.data.Video
import com.example.service.VideoPlayerManager
import com.example.ui.OfflineTubeViewModel
import com.example.ui.FileUtils
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.VideoThumbnail
import com.example.ui.components.formatViews
import com.example.ui.components.rememberRelativeTime
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit

@OptIn(UnstableApi::class)
@Composable
fun WatchScreen(
    videoId: Int,
    viewModel: OfflineTubeViewModel,
    onBack: () -> Unit,
    onChannelClick: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = context as? Activity

    // Intercept back button in landscape mode
    BackHandler(enabled = isLandscape) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
    }

    // Toggle immersive system bars in landscape mode
    LaunchedEffect(isLandscape) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isLandscape) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val videos by viewModel.allVideos.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val recommendedVideos by viewModel.recommendedVideos.collectAsStateWithLifecycle()
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()

    val currentVideo by viewModel.currentVideo.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isBackgroundEnabled by viewModel.isBackgroundPlaybackEnabled.collectAsStateWithLifecycle()
    val isRepeatEnabled by viewModel.isRepeatEnabled.collectAsStateWithLifecycle()
    val isAutoplayEnabled by viewModel.isAutoplayEnabled.collectAsStateWithLifecycle()

    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showEditVideoDialog by remember { mutableStateOf(false) }

    // Lifted Video Editing States
    var editTitle by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editDuration by remember { mutableStateOf("") }
    var editThumbnail by remember { mutableStateOf("") }
    var editAgeRestricted by remember { mutableStateOf(false) }
    var editUploadDate by remember { mutableStateOf(0L) }

    val editThumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copied = FileUtils.copyUriToInternalStorage(context, it, "thumbnails")
            if (copied != null) {
                editThumbnail = copied
            } else {
                editThumbnail = it.toString()
            }
        }
    }

    val video = currentVideo ?: videos.find { it.id == videoId }

    LaunchedEffect(showEditVideoDialog, video) {
        if (showEditVideoDialog && video != null) {
            editTitle = video.title
            editDescription = video.description
            editDuration = video.duration
            editThumbnail = video.thumbnailUri
            editAgeRestricted = video.isAgeRestricted
            editUploadDate = video.uploadDate
        }
    }

    // On enter, trigger playing if not already playing or if different video is selected
    LaunchedEffect(videoId) {
        val targetVideo = videos.find { it.id == videoId }
        if (targetVideo != null && currentVideo?.id != videoId) {
            viewModel.playVideo(targetVideo)
        }
    }

    if (video == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val channel = channels.find { it.id == video.channelId }
    val recommendationsList = recommendedVideos.filter { it.id != video.id && !it.isShorts }

    if (isLandscape) {
        YouTubeVideoPlayer(
            video = video,
            viewModel = viewModel,
            onBack = {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Android Player View Container (YouTube Custom Player)
            YouTubeVideoPlayer(
                video = video,
                viewModel = viewModel,
                onBack = onBack
            )

            // Rest of details
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
            item {
                // Video Title
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    val uploadDateText = rememberRelativeTime(video.uploadDate)
                    Text(
                        text = "${formatViews(video.views)} • $uploadDateText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            item {
                // Channel Info Bar (YouTube-style, with custom subscription state)
                var isSubscribed by remember { mutableStateOf(true) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = channel != null && onChannelClick != null) {
                            if (channel != null && onChannelClick != null) {
                                onChannelClick(channel.id)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ChannelAvatar(
                        avatarUri = channel?.avatarUri ?: "",
                        name = channel?.name ?: "Канал"
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = channel?.name ?: "Канал",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Interactive Subscribe Button
                    Button(
                        onClick = { isSubscribed = !isSubscribed },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubscribed) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                Color.Red
                            }
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSubscribed) "Вы подписаны" else "Подписаться",
                            color = if (isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                // Horizontally Scrollable Action Chips Row (Just like native YouTube)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like / Dislike joined Chip
                    Surface(
                        color = if (video.isLiked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("like_video_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.clickable { viewModel.toggleLike(video) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (video.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                    contentDescription = "Like",
                                    tint = if (video.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Нравится",
                                    color = if (video.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Divider line inside Chip
                            Box(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            )
                            Icon(
                                imageVector = Icons.Outlined.ThumbDown,
                                contentDescription = "Dislike",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp).clickable { }
                            )
                        }
                    }

                    // Playlist+ Chip
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.clickable { showPlaylistDialog = true }.testTag("add_to_playlist_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Добавить в плейлист",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Плейлист+",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Repeat Chip
                    Surface(
                        color = if (isRepeatEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.clickable { viewModel.toggleRepeat() }.testTag("repeat_toggle_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Повтор видео",
                                tint = if (isRepeatEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Повтор",
                                color = if (isRepeatEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Autoplay Chip
                    Surface(
                        color = if (isAutoplayEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.clickable { viewModel.toggleAutoplay() }.testTag("autoplay_toggle_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Автопроигрывание",
                                tint = if (isAutoplayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Авто",
                                color = if (isAutoplayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Edit/Change Chip
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.clickable { showEditVideoDialog = true }.testTag("edit_video_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Редактировать видео",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Изменить",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Expandable Description Box
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { isDescriptionExpanded = !isDescriptionExpanded },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Описание видео",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = if (isDescriptionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = if (video.description.isBlank()) "Описание отсутствует" else video.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Collapsible Offline Comments Section
            item {
                var isCommentsExpanded by remember { mutableStateOf(true) }
                val comments by viewModel.getCommentsForVideo(video.id).collectAsStateWithLifecycle(initialValue = emptyList())
                var authorNameText by remember { mutableStateOf("") }
                var commentContentText by remember { mutableStateOf("") }
                var commenterAvatarUri by remember { mutableStateOf("") }

                val commenterAvatarLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        val copied = FileUtils.copyUriToInternalStorage(context, it, "commenters")
                        if (copied != null) {
                            commenterAvatarUri = copied
                        } else {
                            commenterAvatarUri = it.toString()
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCommentsExpanded = !isCommentsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Комментарии (${comments.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "Оффлайн",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (isCommentsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (isCommentsExpanded) {
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Comment Entry Box
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Commenter Avatar Picker
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .clickable { commenterAvatarLauncher.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (commenterAvatarUri.isNotEmpty()) {
                                            AsyncImage(
                                                model = commenterAvatarUri,
                                                contentDescription = "Аватар комментатора",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Photo,
                                                contentDescription = "Выбрать аватар",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = authorNameText,
                                        onValueChange = { authorNameText = it },
                                        label = { Text("Имя", fontSize = 11.sp) },
                                        placeholder = { Text("Аноним") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.35f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )

                                    OutlinedTextField(
                                        value = commentContentText,
                                        onValueChange = { commentContentText = it },
                                        label = { Text("Комментарий", fontSize = 11.sp) },
                                        placeholder = { Text("Напишите отзыв...") },
                                        modifier = Modifier.weight(0.65f),
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    if (commentContentText.isNotBlank()) {
                                                        viewModel.addComment(
                                                            videoId = video.id,
                                                            authorName = authorNameText,
                                                            content = commentContentText,
                                                            authorAvatarUri = commenterAvatarUri
                                                        )
                                                        commentContentText = ""
                                                        commenterAvatarUri = ""
                                                    }
                                                },
                                                modifier = Modifier.testTag("submit_comment_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = "Отправить",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // List of comments
                            if (comments.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Комментариев пока нет. Напишите первый!",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    comments.forEach { comment ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            // Avatar display
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (comment.authorAvatarUri.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = comment.authorAvatarUri,
                                                        contentDescription = "Аватар автора",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    val firstLetter = comment.authorName.firstOrNull()?.toString() ?: "?"
                                                    Text(
                                                        text = firstLetter.uppercase(Locale.getDefault()),
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = comment.authorName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "• оффлайн",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                                Text(
                                                    text = comment.content,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteComment(comment.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Удалить комментарий",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(14.dp)
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
            }

            item {
                Divider(
                    color = MaterialTheme.colorScheme.surface,
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Следующее видео (Рекомендации)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Recommended Videos Underneath
            if (recommendationsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Другие видео в рекомендациях отсутствуют 🏜️",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                items(recommendationsList) { recVideo ->
                    val recChannel = channels.find { it.id == recVideo.channelId }
                    
                    // Mini Video item card for compact recommendations
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.playVideo(recVideo)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("rec_video_item_${recVideo.id}"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VideoThumbnail(
                            thumbnailUri = recVideo.thumbnailUri,
                            duration = recVideo.duration,
                            modifier = Modifier
                                .width(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = recVideo.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = recChannel?.name ?: "Канал",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatViews(recVideo.views)} • ${rememberRelativeTime(recVideo.uploadDate)}",
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

    // Playlist Selector & Creator Dialog
    if (showPlaylistDialog) {
        var newPlaylistName by remember { mutableStateOf("") }
        var isCreatingNew by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Добавить в плейлист") },
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
                                            viewModel.addVideoToPlaylist(playlist.id, video.id)
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
                                            text = playlist.name,
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
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
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
                                viewModel.createPlaylist(newPlaylistName)
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

    if (showEditVideoDialog) {
        val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showEditVideoDialog = false },
            title = { Text("Редактировать видео") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Название видео") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Thumbnail picking row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editThumbnail,
                            onValueChange = { editThumbnail = it },
                            label = { Text("Ссылка на обложку") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Выберите из галереи") }
                        )
                        IconButton(
                            onClick = { editThumbnailPickerLauncher.launch("image/*") },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = "Выбрать обложку",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Upload Date custom selector row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Дата публикации 📅",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dateFormatter.format(Date(editUploadDate)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Button(
                                onClick = {
                                    val currentCal = Calendar.getInstance().apply { timeInMillis = editUploadDate }
                                    val datePickerDialog = android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            currentCal.set(Calendar.YEAR, year)
                                            currentCal.set(Calendar.MONTH, month)
                                            currentCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                            // Show TimePickerDialog right after Date is chosen
                                            android.app.TimePickerDialog(
                                                context,
                                                { _, hourOfDay, minute ->
                                                    currentCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                    currentCal.set(Calendar.MINUTE, minute)
                                                    editUploadDate = currentCal.timeInMillis
                                                },
                                                currentCal.get(Calendar.HOUR_OF_DAY),
                                                currentCal.get(Calendar.MINUTE),
                                                true
                                            ).show()
                                        },
                                        currentCal.get(Calendar.YEAR),
                                        currentCal.get(Calendar.MONTH),
                                        currentCal.get(Calendar.DAY_OF_MONTH)
                                    )
                                    datePickerDialog.show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Изменить",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ограничение 18+ (скрыть из рекомендаций)")
                        Switch(
                            checked = editAgeRestricted,
                            onCheckedChange = { editAgeRestricted = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateVideo(
                            video.copy(
                                title = editTitle,
                                description = editDescription,
                                duration = editDuration,
                                thumbnailUri = editThumbnail,
                                isAgeRestricted = editAgeRestricted,
                                uploadDate = editUploadDate
                            )
                        )
                        showEditVideoDialog = false
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditVideoDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
}

@OptIn(UnstableApi::class)
@Composable
fun YouTubeVideoPlayer(
    video: Video,
    viewModel: OfflineTubeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = context as? Activity
    var resizeModeState by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    val exoPlayer = remember { VideoPlayerManager.getPlayer(context) }
    
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.playbackPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    
    var showControls by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var selectedQuality by remember { mutableStateOf("Авто") }
    
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    
    val isAudioOnly by viewModel.isAudioOnly.collectAsStateWithLifecycle()
    
    // Double tap ripple animation states
    var doubleTapLeftTrigger by remember { mutableStateOf(0) }
    var doubleTapRightTrigger by remember { mutableStateOf(0) }
    
    // Auto-hide controls timer
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }
    
    // Double tap animation resets
    var showLeftDoubleTapIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(doubleTapLeftTrigger) {
        if (doubleTapLeftTrigger > 0) {
            showLeftDoubleTapIndicator = true
            kotlinx.coroutines.delay(650)
            showLeftDoubleTapIndicator = false
        }
    }
    
    var showRightDoubleTapIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(doubleTapRightTrigger) {
        if (doubleTapRightTrigger > 0) {
            showRightDoubleTapIndicator = true
            kotlinx.coroutines.delay(650)
            showRightDoubleTapIndicator = false
        }
    }

    val playerModifier = if (isLandscape) {
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    } else {
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag > 120f) {
                            onBack()
                        }
                    },
                    onDragCancel = { totalDrag = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    }
                )
            }
    }

    Box(
        modifier = playerModifier
    ) {
        if (isAudioOnly) {
            // Audio mode cover
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = video.thumbnailUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.35f
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = video.thumbnailUri,
                            contentDescription = "Превью",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = "Только аудио (Экономия трафика) 🎧",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Underlaying Video Player
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // Custom Controls are built in Compose below!
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        resizeMode = resizeModeState
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.resizeMode = resizeModeState
                }
            )
        }

        // GestureDetector Overlays
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Half
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onDoubleTap = {
                                val newPos = (currentPosition - 10000L).coerceAtLeast(0L)
                                VideoPlayerManager.seekTo(newPos)
                                doubleTapLeftTrigger++
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (showLeftDoubleTapIndicator) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "-10 сек",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Right Half
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onDoubleTap = {
                                val newPos = (currentPosition + 10000L).coerceAtMost(duration)
                                VideoPlayerManager.seekTo(newPos)
                                doubleTapRightTrigger++
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (showRightDoubleTapIndicator) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "+10 сек",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Custom Controller HUD Overlay
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Row: Minimize, Video Title, Audio Toggle, and Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Свернуть",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = video.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(3f)
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Audio Mode Toggle
                    IconButton(
                        onClick = { viewModel.isAudioOnly.value = !isAudioOnly },
                        modifier = Modifier.background(
                            if (isAudioOnly) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.3f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = if (isAudioOnly) Icons.Default.MusicNote else Icons.Default.Movie,
                            contentDescription = "Только аудио",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Fullscreen Toggle Button
                    IconButton(
                        onClick = {
                            if (isLandscape) {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                            } else {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "Полноэкранный режим",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSettingsMenu = true },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Center Row: Previous Video, Play/Pause, Next Video
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.playPreviousVideo() },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Предыдущее видео",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Large circular play/pause button
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.playNextVideo() },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Следующее видео",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Bottom Row: Current Time, Custom Seekbar, Total Duration, Speed
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = if (playbackSpeed != 1.0f) "${playbackSpeed}x" else "Обычная",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Slider(
                        value = currentPosition.toFloat(),
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                        onValueChange = { VideoPlayerManager.seekTo(it.toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Red,
                            activeTrackColor = Color.Red,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(24.dp)
                    )
                }
            }
        }

        // Settings Dropdown Menu
        if (showSettingsMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                DropdownMenu(
                    expanded = showSettingsMenu,
                    onDismissRequest = { showSettingsMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = { Text("Скорость воспроизведения: ${playbackSpeed}x") },
                        onClick = {
                            showSettingsMenu = false
                            showSpeedDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Качество: $selectedQuality") },
                        onClick = {
                            showSettingsMenu = false
                            showQualityDialog = true
                        }
                    )
                    val resizeModeLabel = when (resizeModeState) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Вписать (Оригинал) 📺"
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Заполнить (Обрезать) 🔍"
                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Растянуть 📐"
                        else -> "Вписать 📺"
                    }
                    DropdownMenuItem(
                        text = { Text("Масштаб: $resizeModeLabel") },
                        onClick = {
                            resizeModeState = when (resizeModeState) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                            showSettingsMenu = false
                        }
                    )
                }
            }
        }

        // Playback Speed Selection Dialog
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = { Text("Скорость воспроизведения") },
                text = {
                    Column {
                        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                        speeds.forEach { speed ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playbackSpeed = speed
                                        exoPlayer.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = if (speed == 1.0f) "Обычная" else "${speed}x")
                                if (playbackSpeed == speed) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Выбрано",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }

        // Video Quality Selection Dialog (Mock)
        if (showQualityDialog) {
            AlertDialog(
                onDismissRequest = { showQualityDialog = false },
                title = { Text("Качество видео") },
                text = {
                    Column {
                        val qualities = listOf("1080p", "720p", "480p", "360p", "Авто")
                        qualities.forEach { quality ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedQuality = quality
                                        showQualityDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = quality)
                                if (selectedQuality == quality) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Выбрано",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQualityDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
