package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.data.Playlist
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import coil.compose.AsyncImage
import com.example.data.Channel
import com.example.data.Video
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VideoFrameThumbnail(
    videoUri: String,
    thumbnailUri: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current

    val hasThumbnail = !thumbnailUri.isNullOrBlank() && 
            thumbnailUri != "default_video" && 
            thumbnailUri != "default_thumbnail" && 
            thumbnailUri != "placeholder"

    if (hasThumbnail) {
        val presetGradient = when (thumbnailUri) {
            "jupiter" -> Brush.linearGradient(listOf(Color(0xFFE94E77), Color(0xFFF2D43B)))
            "forest" -> Brush.linearGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d)))
            "lofi" -> Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)))
            else -> null
        }

        if (presetGradient != null) {
            Box(
                modifier = modifier.background(presetGradient)
            )
        } else {
            AsyncImage(
                model = thumbnailUri,
                contentDescription = "Превью видео",
                modifier = modifier,
                contentScale = contentScale,
                error = null
            )
        }
    } else {
        var bitmap by remember(videoUri) { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(videoUri) {
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    if (videoUri.startsWith("content://") || videoUri.startsWith("android.resource://") || videoUri.startsWith("file://")) {
                        retriever.setDataSource(context, Uri.parse(videoUri))
                    } else {
                        retriever.setDataSource(videoUri, HashMap<String, String>())
                    }
                    // Extract frame at 1,000,000 microseconds (1 second) or 0 if too short
                    val rawFrame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                        try {
                            retriever.getScaledFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 640, 360)
                                ?: retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 640, 360)
                        } catch (e: Exception) {
                            retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        }
                    } else {
                        retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }

                    if (rawFrame != null) {
                        if (rawFrame.width > 640 || rawFrame.height > 360) {
                            val targetW = 640
                            val targetH = (640f / rawFrame.width * rawFrame.height).toInt().coerceAtLeast(1)
                            val scaled = Bitmap.createScaledBitmap(rawFrame, targetW, targetH, true)
                            if (scaled != rawFrame) {
                                rawFrame.recycle()
                            }
                            bitmap = scaled
                        } else {
                            bitmap = rawFrame
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Кадр из видео",
                modifier = modifier,
                contentScale = contentScale
            )
        } else {
            // Safe animated gradient fallback backplate
            Box(
                modifier = modifier.background(
                    Brush.linearGradient(listOf(Color(0xFF2C3E50), Color(0xFF3498DB)))
                )
            )
        }
    }
}

@Composable
fun VideoThumbnail(
    thumbnailUri: String,
    duration: String,
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
    ) {
        val presetGradient = when (thumbnailUri) {
            "jupiter" -> Brush.linearGradient(listOf(Color(0xFFE94E77), Color(0xFFF2D43B)))
            "forest" -> Brush.linearGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d)))
            "lofi" -> Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121)))
            else -> null
        }

        if (presetGradient != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(presetGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            AsyncImage(
                model = thumbnailUri,
                contentDescription = "Превью видео",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = null // Handled dynamically or fallback to background gradient
            )
            // Fallback backplate if empty or load fails
            if (thumbnailUri.isBlank() || thumbnailUri == "default_video") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFF2C3E50),
                                    Color(0xFF3498DB),
                                    Color(0xFF2C3E50)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        // Duration Badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = duration,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Optional Progress Bar for Lector Mode
        if (progress != null && progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(Color.Red)
            )
        }
    }
}

@Composable
fun ChannelAvatar(
    avatarUri: String,
    name: String,
    size: Int = 40,
    modifier: Modifier = Modifier
) {
    val initial = name.firstOrNull()?.uppercase() ?: "K"
    val presetGradient = when (avatarUri) {
        "space_avatar" -> Brush.linearGradient(listOf(Color(0xFF3F51B5), Color(0xFF00BCD4)))
        "nature_avatar" -> Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)))
        "dzen_avatar" -> Brush.linearGradient(listOf(Color(0xFFFF9800), Color(0xFFFFC107)))
        else -> null
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        if (presetGradient != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(presetGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = (size * 0.45).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (avatarUri.isNotEmpty() && avatarUri != "default_avatar") {
            AsyncImage(
                model = avatarUri,
                contentDescription = "Аватар канала",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Text fallback avatar
            val hashColor = rememberHashColor(name)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(hashColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = (size * 0.45).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VideoCard(
    video: Video,
    channel: Channel?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    onClearProgress: (() -> Unit)? = null,
    onChannelClick: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить видео?") },
            text = { Text("Вы действительно хотите окончательно удалить видео \"${video.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            VideoThumbnail(
                thumbnailUri = video.thumbnailUri,
                duration = video.duration,
                modifier = Modifier.fillMaxWidth(),
                progress = progress
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier.clickable(enabled = onChannelClick != null) {
                        onChannelClick?.invoke()
                    }
                ) {
                    ChannelAvatar(
                        avatarUri = channel?.avatarUri ?: "",
                        name = channel?.name ?: "Канал"
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    val uploadDateText = rememberRelativeTime(video.uploadDate)
                    Text(
                        text = "${channel?.name ?: "Неизвестный канал"} • ${formatViews(video.views)} • $uploadDateText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(enabled = onChannelClick != null) {
                            onChannelClick?.invoke()
                        }
                    )
                }

                if (onDelete != null || onAddToPlaylist != null || (progress != null && progress > 0f && onClearProgress != null)) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Еще"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (progress != null && progress > 0f && onClearProgress != null) {
                                DropdownMenuItem(
                                    text = { Text("Сбросить прогресс") },
                                    onClick = {
                                        showMenu = false
                                        onClearProgress()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Сбросить прогресс"
                                        )
                                    }
                                )
                            }
                            if (onAddToPlaylist != null) {
                                DropdownMenuItem(
                                    text = { Text("Добавить в плейлист") },
                                    onClick = {
                                        showMenu = false
                                        onAddToPlaylist()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistAdd,
                                            contentDescription = "Добавить в плейлист"
                                        )
                                    }
                                )
                            }
                            if (onDelete != null) {
                                DropdownMenuItem(
                                    text = { Text("Удалить видео", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Удалить",
                                            tint = MaterialTheme.colorScheme.error
                                        )
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

@Composable
fun rememberHashColor(name: String): Color {
    val hash = name.hashCode()
    val r = (hash and 0xFF0000 shr 16) % 180 + 40
    val g = (hash and 0x00FF00 shr 8) % 180 + 40
    val b = (hash and 0x0000FF) % 180 + 40
    return Color(r, g, b)
}

@Composable
fun rememberRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Только что"
        minutes < 60 -> "$minutes мин. назад"
        hours < 24 -> "$hours ч. назад"
        days < 7 -> "$days дн. назад"
        else -> {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

fun formatViews(views: Int): String {
    return when {
        views < 1000 -> "$views просмотров"
        views < 1000000 -> "${views / 1000} тыс. просмотров"
        else -> "${views / 1000000} млн. просмотров"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    videoId: Int,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onAdd: (Int, Int) -> Unit, // (playlistId, videoId)
    onCreatePlaylist: (String) -> Unit // (name)
) {
    var newPlaylistName by remember { mutableStateOf("") }
    var isCreatingNew by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                                        onAdd(playlist.id, videoId)
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
                            onCreatePlaylist(newPlaylistName)
                            newPlaylistName = ""
                            isCreatingNew = false
                        }
                    }
                ) {
                    Text("Создать")
                }
            } else {
                TextButton(onClick = onDismiss) {
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
                        onDismiss()
                    }
                }
            ) {
                Text(if (isCreatingNew) "Назад" else "Отмена")
            }
        }
    )
}
