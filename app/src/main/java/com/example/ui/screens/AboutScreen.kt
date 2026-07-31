package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.OfflineTubeViewModel
import com.example.data.Channel
import com.example.data.Video
import com.example.data.WatchHistory
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Brush
import com.example.ui.components.ChannelAvatar

data class ChannelStatData(
    val channel: Channel,
    val viewsOrCount: Int,
    val hours: Double,
    val videos: List<Video>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: OfflineTubeViewModel,
    onNavigateToChannels: () -> Unit,
    onNavigateToPlaylists: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val isAdultMode by viewModel.isAdultMode.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val videos by viewModel.allVideos.collectAsStateWithLifecycle()
    val history by viewModel.watchHistory.collectAsStateWithLifecycle()

    val topChannelsByHours = remember(channels, videos) {
        channels.map { channel ->
            val channelVideos = videos.filter { it.channelId == channel.id }
            val totalHours = channelVideos.sumOf { parseDurationToHours(it.duration) }
            channel to totalHours
        }
        .filter { it.second > 0.0 }
        .sortedByDescending { it.second }
        .take(5)
    }

    val historyVideos = remember(history, videos, channels) {
        history.mapNotNull { hist ->
            val video = videos.find { it.id == hist.videoId }
            val channel = channels.find { it.id == video?.channelId }
            if (video != null) Triple(hist, video, channel) else null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(if (isAdultMode) "Панель 18+" else "Личный кабинет", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Channel Management Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToChannels() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = if (isAdultMode) "Управление 18+ каналами" else "Управление каналами",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isAdultMode) "Создание, удаление и настройка 18+ каналов" else "Создание, удаление и настройка каналов",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Перейти",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Storage Settings & Backup Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Настройки памяти и бэкап",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Резервное копирование и очистка неиспользуемых файлов, отсутствующих в базе.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exportBackup() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text("Создать бэкап", fontSize = 11.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { viewModel.importBackup() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text("Восстановить", fontSize = 11.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { viewModel.cleanupOrphanFiles() },
                            modifier = Modifier.weight(1.1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text("Очистить мусор", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }

            // Watch History Card Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "История просмотров",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (historyVideos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "История просмотров пуста. Ваши просмотренные видео будут появляться здесь!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 4.dp)
                        ) {
                            items(historyVideos) { (hist, video, channel) ->
                                Box(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .clickable { viewModel.playVideo(video) }
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            val presetGradient = when (video.thumbnailUri) {
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
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            } else if (video.thumbnailUri.isNotEmpty() && video.thumbnailUri != "default_video") {
                                                AsyncImage(
                                                    model = video.thumbnailUri,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
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
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }

                                            // Duration Badge
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = video.duration,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Close button to remove from history
                                            IconButton(
                                                onClick = { viewModel.clearVideoProgress(video.id) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(2.dp)
                                                    .size(24.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Удалить из истории",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        // Video Title
                                        Text(
                                            text = video.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        // Channel Name
                                        Text(
                                            text = channel?.name ?: "Канал",
                                            style = MaterialTheme.typography.labelSmall,
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

            // Stats Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isAdultMode) "Офлайн Статистика 18+" else "Офлайн Статистика",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = channels.size.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Каналов",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = videos.size.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Видео",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = history.size.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Просмотров",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Interactive Channel Activity Statistics Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isAdultMode) "Активность по 18+ каналам" else "Активность по каналам",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Interactive State Managers
                    var statsSource by remember { mutableStateOf("history") } // "history" or "total"
                    var metricType by remember { mutableStateOf("hours") } // "hours" or "count"
                    var selectedChannelIdForDetail by remember { mutableStateOf<Int?>(null) }

                    // Tab Selector (FilterChip style)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = statsSource == "history",
                            onClick = { 
                                statsSource = "history"
                                selectedChannelIdForDetail = null
                            },
                            label = { Text("Мои просмотры", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = statsSource == "total",
                            onClick = { 
                                statsSource = "total"
                                selectedChannelIdForDetail = null
                            },
                            label = { Text("Объем медиатеки", fontSize = 12.sp) }
                        )
                    }

                    // Context description
                    Text(
                        text = if (statsSource == "history") {
                            "Анализ времени просмотра и количества воспроизведений контента по каждому каналу."
                        } else {
                            "Распределение общего хронометража и количества загруженных видео по каналам."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 16.sp
                    )

                    // Compute statistics safely
                    val computedStats: List<ChannelStatData> = remember(channels, videos, history, statsSource) {
                        val videoMap = videos.associateBy { it.id }
                        channels.map { channel ->
                            if (statsSource == "history") {
                                val channelHistory = history.filter { hist ->
                                    val video = videoMap[hist.videoId]
                                    video?.channelId == channel.id
                                }
                                val views = channelHistory.size
                                val hours = channelHistory.sumOf { hist ->
                                    val video = videoMap[hist.videoId]
                                    if (video != null) parseDurationToHours(video.duration) else 0.0
                                }
                                val watchedVideos = channelHistory.mapNotNull { videoMap[it.videoId] }.distinctBy { it.id }
                                ChannelStatData(channel, views, hours, watchedVideos)
                            } else {
                                val channelVideos = videos.filter { it.channelId == channel.id }
                                val count = channelVideos.size
                                val hours = channelVideos.sumOf { parseDurationToHours(it.duration) }
                                ChannelStatData(channel, count, hours, channelVideos)
                            }
                        }
                    }

                    // Sort statistics
                    val sortedStats: List<ChannelStatData> = remember(computedStats, metricType) {
                        if (metricType == "hours") {
                            computedStats.sortedByDescending { it.hours }
                        } else {
                            computedStats.sortedByDescending { it.viewsOrCount }
                        }
                    }

                    // Find Maximum for scaling the progress bar
                    val maxVal: Double = remember(sortedStats, metricType) {
                        if (metricType == "hours") {
                            sortedStats.maxOfOrNull { it.hours }?.coerceAtLeast(0.01) ?: 1.0
                        } else {
                            sortedStats.maxOfOrNull { it.viewsOrCount.toDouble() }?.coerceAtLeast(1.0) ?: 1.0
                        }
                    }

                    // Metric toggle bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val activeColor = MaterialTheme.colorScheme.primary
                        val inactiveColor = Color.Transparent
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (metricType == "hours") activeColor else inactiveColor)
                                .clickable { metricType = "hours" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Часы просмотра",
                                color = if (metricType == "hours") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (metricType == "count") activeColor else inactiveColor)
                                .clickable { metricType = "count" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (statsSource == "history") "Просмотры" else "Количество видео",
                                color = if (metricType == "count") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Render chart or placeholder
                    if (sortedStats.isEmpty() || (statsSource == "history" && history.isEmpty())) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Нет данных для анализа",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                "Смотрите видео в приложении, чтобы накопить статистику активности!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            sortedStats.take(5).forEach { stat ->
                                val progress = if (metricType == "hours") {
                                    (stat.hours / maxVal).toFloat()
                                } else {
                                    (stat.viewsOrCount.toFloat() / maxVal.toFloat())
                                }
                                val isSelected = selectedChannelIdForDetail == stat.channel.id

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                        .clickable {
                                            selectedChannelIdForDetail = if (isSelected) null else stat.channel.id
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (stat.channel.avatarUri.startsWith("http") || stat.channel.avatarUri.startsWith("content")) {
                                            AsyncImage(
                                                model = stat.channel.avatarUri,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = stat.channel.name.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Content & Progress
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stat.channel.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Custom rounded visual bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }

                                    // Metric Label
                                    Text(
                                        text = if (metricType == "hours") {
                                            formatHours(stat.hours)
                                        } else {
                                            if (statsSource == "history") {
                                                "${stat.viewsOrCount} просм."
                                            } else {
                                                "${stat.viewsOrCount} видео"
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Detailed sub-card
                        AnimatedVisibility(
                            visible = selectedChannelIdForDetail != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            val selectedStat = sortedStats.find { it.channel.id == selectedChannelIdForDetail }
                            if (selectedStat != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Канал: ${selectedStat.channel.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            IconButton(
                                                onClick = { selectedChannelIdForDetail = null },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Закрыть",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = selectedStat.channel.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                        Text(
                                            text = if (statsSource == "history") "Просмотренные видео канала:" else "Загруженные видео канала:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )

                                        if (selectedStat.videos.isEmpty()) {
                                            Text(
                                                "Нет доступных видео для этого режима.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                selectedStat.videos.take(3).forEach { video ->
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = video.title,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(
                                                            text = video.duration,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.padding(start = 8.dp)
                                                        )
                                                    }
                                                }
                                                if (selectedStat.videos.size > 3) {
                                                    Text(
                                                        text = "и еще ${selectedStat.videos.size - 3} видео...",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                        var showDeleteDialog by remember { mutableStateOf(false) }
                                        if (showDeleteDialog) {
                                            AlertDialog(
                                                onDismissRequest = { showDeleteDialog = false },
                                                title = { Text("Удалить канал?") },
                                                text = { Text("Вы действительно хотите удалить канал \"${selectedStat.channel.name}\"? Это также безвозвратно удалит все его видео, историю просмотров и локальные файлы.") },
                                                confirmButton = {
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.deleteChannel(selectedStat.channel.id)
                                                            showDeleteDialog = false
                                                            selectedChannelIdForDetail = null
                                                        }
                                                    ) {
                                                        Text("Удалить", color = MaterialTheme.colorScheme.error)
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showDeleteDialog = false }) {
                                                        Text("Отмена")
                                                    }
                                                }
                                            )
                                        }

                                        Button(
                                            onClick = { showDeleteDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Удалить канал",
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Удалить канал и все данные", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // "Top by hours" Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Топ каналов по часам видео 🏆",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Список каналов, отсортированный по суммарной длительности видео на канале.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    if (topChannelsByHours.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Добавьте каналы и видео, чтобы наполнить топ!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            topChannelsByHours.forEachIndexed { index, (channel, hours) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Rank index badge
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                color = when (index) {
                                                    0 -> Color(0xFFFFD700) // Gold
                                                    1 -> Color(0xFFC0C0C0) // Silver
                                                    2 -> Color(0xFFCD7F32) // Bronze
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (index < 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Channel Avatar
                                    ChannelAvatar(
                                        avatarUri = channel.avatarUri,
                                        name = channel.name,
                                        size = 36
                                    )

                                    // Channel Name
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = channel.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${channel.subscribers} подписчиков",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    // Time spent
                                    Text(
                                        text = formatHours(hours),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (index < topChannelsByHours.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                }
            }

            // App purpose & Donation card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "О приложении 📱",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Это приложение создано для людей, которым бы всегда хотелось иметь собственную медиатеку вне зависимости от того, что происходит во внешнем мире (в политическом плане).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri("https://pay.cloudtips.ru/p/4f7db931")
                            } catch (e: Exception) {
                                // Ignore if URI cannot be handled
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Поддержать проект",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoFeatureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun parseDurationToHours(durationStr: String): Double {
    try {
        val parts = durationStr.split(":").map { it.trim().toIntOrNull() ?: 0 }
        val seconds = when (parts.size) {
            1 -> parts[0]
            2 -> parts[0] * 60 + parts[1] // MM:SS
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2] // HH:MM:SS
            else -> 0
        }
        return seconds.toDouble() / 3600.0
    } catch (e: Exception) {
        return 0.0
    }
}

private fun formatHours(hours: Double): String {
    if (hours == 0.0) return "0 мин"
    val totalMins = (hours * 60).toInt()
    if (totalMins < 60) {
        return "$totalMins мин"
    }
    val hrs = totalMins / 60
    val mins = totalMins % 60
    return if (mins == 0) "$hrs ч" else "$hrs ч $mins мин"
}
