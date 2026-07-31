package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.OfflineTubeViewModel
import com.example.ui.FileUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContentScreen(
    viewModel: OfflineTubeViewModel,
    onNavigateToChannels: () -> Unit
) {
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isAdultMode by viewModel.isAdultMode.collectAsStateWithLifecycle()

    var selectedChannelIndex by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var videoUri by remember { mutableStateOf("") }
    var thumbnailUri by remember { mutableStateOf("") }

    var isChannelDropdownExpanded by remember { mutableStateOf(false) }
    var isAgeRestricted by remember(isAdultMode) { mutableStateOf(isAdultMode) }
    var isShorts by remember { mutableStateOf(false) }

    // Custom Date selection states
    var customDateEnabled by remember { mutableStateOf(false) }
    var selectedTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    // Picker launchers
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copied = FileUtils.copyUriToInternalStorage(context, it, "videos")
            if (copied != null) {
                videoUri = copied
            } else {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // ignore
                }
                videoUri = it.toString()
            }
            val autoTitle = FileUtils.getFileNameWithoutExtension(context, it)
            if (autoTitle.isNotEmpty() && title.isBlank()) {
                title = autoTitle
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copied = FileUtils.copyUriToInternalStorage(context, it, "thumbnails")
            if (copied != null) {
                thumbnailUri = copied
            } else {
                thumbnailUri = it.toString()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Добавить Видео", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Нет активных каналов 📭",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Чтобы загрузить видео, вам необходимо сначала создать свой канал. Вы можете настроить его название, аватарку и описание.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToChannels,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("go_to_channels_button")
                        ) {
                            Text("Создать Канал", color = Color.White)
                        }
                    }
                }
            }
        } else {
            val selectedChannel = channels.getOrNull(selectedChannelIndex)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Channel Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Выберите канал публикации",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { isChannelDropdownExpanded = true }
                            .padding(16.dp)
                            .testTag("channel_dropdown")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedChannel?.name ?: "Выберите канал",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = isChannelDropdownExpanded,
                            onDismissRequest = { isChannelDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            channels.forEachIndexed { index, channel ->
                                DropdownMenuItem(
                                    text = { Text(channel.name, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        selectedChannelIndex = index
                                        isChannelDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("channel_dropdown_item_${channel.id}")
                                )
                            }
                        }
                    }
                }

                // Video Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название видео") },
                    placeholder = { Text("Введите цепляющий заголовок") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_title_input"),
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    placeholder = { Text("Расскажите зрителям о чем ваше видео") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    maxLines = 4
                )

                // Video File Picker section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Видеофайл",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (videoUri.isNotEmpty()) "Выбран" else "Не выбран",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (videoUri.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier.size(48.dp).testTag("pick_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Выбрать видео",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Thumbnail File Picker section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Превью-картинка (опционально)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (thumbnailUri.isNotEmpty()) "Выбрана" else "Не выбрана (автогенерация)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (thumbnailUri.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Выбрать превью",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Shorts toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Switch(
                        checked = isShorts,
                        onCheckedChange = { isShorts = it },
                        modifier = Modifier.testTag("shorts_switch")
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Шортс (9:16) 🎬",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Опубликовать в раздел коротких вертикальных видео Shorts",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Age restriction toggle
                if (isAdultMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5722).copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "🔞", fontSize = 22.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Режим 18+ активен",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFFF5722)
                                )
                                Text(
                                    text = "Все добавляемые здесь видео автоматически получают метку 18+ и видны только в этом режиме",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Switch(
                            checked = isAgeRestricted,
                            onCheckedChange = { isAgeRestricted = it },
                            modifier = Modifier.testTag("age_restricted_switch")
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Возрастное ограничение 18+",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Такие видео не будут рекомендоваться зрителям",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Date Selector Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Switch(
                            checked = customDateEnabled,
                            onCheckedChange = { customDateEnabled = it },
                            modifier = Modifier.testTag("custom_date_switch")
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Кастомная дата публикации 📅",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Позволяет влиять на хронологию видео",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    if (customDateEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Дата: ${dateFormatter.format(Date(selectedTimeMs))}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Button(
                                onClick = {
                                    val currentCal = Calendar.getInstance().apply { timeInMillis = selectedTimeMs }
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
                                                    selectedTimeMs = currentCal.timeInMillis
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
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Изменить",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Submit Button
                Button(
                    onClick = {
                        selectedChannel?.let { channel ->
                            viewModel.addVideo(
                                channelId = channel.id,
                                title = title,
                                description = description,
                                videoUri = videoUri,
                                thumbnailUri = thumbnailUri,
                                isAgeRestricted = isAgeRestricted,
                                isShorts = isShorts,
                                uploadDate = if (customDateEnabled) selectedTimeMs else System.currentTimeMillis()
                            )
                            // Clear fields after adding
                            title = ""
                            description = ""
                            videoUri = ""
                            thumbnailUri = ""
                            isAgeRestricted = isAdultMode
                            isShorts = false
                            customDateEnabled = false
                            selectedTimeMs = System.currentTimeMillis()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_video_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Опубликовать на канале",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
