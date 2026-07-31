package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.rememberLazyListState
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Manga
import com.example.data.MangaChapter
import com.example.data.MangaReadingProgress
import com.example.ui.OfflineTubeViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MangaScreen(
    viewModel: OfflineTubeViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val mangas by viewModel.allManga.collectAsStateWithLifecycle()
    var showAddMangaDialog by remember { mutableStateOf(false) }

    if (showAddMangaDialog) {
        AddMangaDialog(
            onDismiss = { showAddMangaDialog = false },
            onConfirm = { title, author, description, coverUri, genre, rating, isCompleted ->
                viewModel.addManga(title, author, description, coverUri, genre, rating, isCompleted)
                showAddMangaDialog = false
            }
        )
    }

    // Navigation and detail state
    var selectedManga by remember { mutableStateOf<Manga?>(null) }
    var activeChapter by remember { mutableStateOf<MangaChapter?>(null) }
    var activeMangaForReader by remember { mutableStateOf<Manga?>(null) }

    // If a chapter is open, display the reader
    if (activeChapter != null && activeMangaForReader != null) {
        MangaReaderScreen(
            manga = activeMangaForReader!!,
            chapter = activeChapter!!,
            viewModel = viewModel,
            onBack = {
                activeChapter = null
                activeMangaForReader = null
            }
        )
    } else if (selectedManga != null) {
        // Show Manga details
        MangaDetailScreen(
            manga = selectedManga!!,
            viewModel = viewModel,
            onBack = { selectedManga = null },
            onReadChapter = { chapter ->
                activeMangaForReader = selectedManga
                activeChapter = chapter
            }
        )
    } else {
        // Show Manga catalog (list of books)
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Bar
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Коллекция Манги 18+ 📖",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddMangaDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить мангу")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            if (mangas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Загрузка библиотеки или список пуст...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        OutlinedButton(
                            onClick = { showAddMangaDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Добавить свою мангу")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Cozy hero banner
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Премиум Читалка ✨",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Уютные романтические истории, детализированные офлайн-иллюстрации и удобные закладки.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        maxLines = 3
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Все доступные произведения:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(mangas) { manga ->
                        MangaCatalogItem(
                            manga = manga,
                            viewModel = viewModel,
                            onClick = { selectedManga = manga },
                            onContinueReading = {
                                coroutineScope.launch {
                                    val chapters = viewModel.getChaptersForManga(manga.id).first()
                                    val progress = viewModel.getProgressForManga(manga.id).first()
                                    if (chapters.isNotEmpty()) {
                                        val targetChapter = chapters.find { it.id == progress?.lastReadChapterId } ?: chapters.first()
                                        activeMangaForReader = manga
                                        activeChapter = targetChapter
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MangaCatalogItem(
    manga: Manga,
    viewModel: OfflineTubeViewModel,
    onClick: () -> Unit,
    onContinueReading: () -> Unit
) {
    val progressFlow = remember(manga.id) { viewModel.getProgressForManga(manga.id) }
    val progress by progressFlow.collectAsStateWithLifecycle(initialValue = null)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Book cover
            Box(
                modifier = Modifier
                    .size(width = 85.dp, height = 120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (manga.coverUri.isNotEmpty()) {
                    AsyncImage(
                        model = manga.coverUri,
                        contentDescription = manga.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Rating label
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = manga.rating.toString(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Info and Actions
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Автор: ${manga.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = manga.genre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = manga.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Continue reading row
                if (progress != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Прочитано до стр. ${progress!!.lastReadPage + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = onContinueReading,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Продолжить", fontSize = 11.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Читать", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(
    manga: Manga,
    viewModel: OfflineTubeViewModel,
    onBack: () -> Unit,
    onReadChapter: (MangaChapter) -> Unit
) {
    val chapters by viewModel.getChaptersForManga(manga.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val progress by viewModel.getProgressForManga(manga.id).collectAsStateWithLifecycle(initialValue = null)
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showAddChapterDialog) {
        AddChapterDialog(
            nextChapterNumber = chapters.size + 1,
            onDismiss = { showAddChapterDialog = false },
            onConfirm = { title, chapterNumber, pageUris ->
                viewModel.addMangaChapter(manga.id, title, chapterNumber, pageUris)
                showAddChapterDialog = false
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Удалить мангу?", fontWeight = FontWeight.Bold) },
            text = { Text("Вы действительно хотите полностью удалить эту мангу, все сохраненные главы и прогресс чтения?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteManga(manga.id)
                        showDeleteConfirmation = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
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
        // Top Bar
        TopAppBar(
            title = { Text(manga.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                IconButton(onClick = { showAddChapterDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить главу")
                }
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить мангу", tint = MaterialTheme.colorScheme.error)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Book Info Header Card
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 110.dp, height = 160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (manga.coverUri.isNotEmpty()) {
                            AsyncImage(
                                model = manga.coverUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = manga.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Автор: ${manga.author}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = manga.genre,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            Text(text = "${manga.rating} / 5.0", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            SuggestionChip(
                                onClick = {},
                                label = { Text(if (manga.isCompleted) "Завершено" else "В процессе") }
                            )
                        }
                    }
                }
            }

            // Description Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Описание", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = manga.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Bookmark Quick Start Row
            if (progress != null && chapters.isNotEmpty()) {
                item {
                    val lastChapter = chapters.find { it.id == progress!!.lastReadChapterId }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        onClick = {
                            if (lastChapter != null) onReadChapter(lastChapter)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Продолжить чтение",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${lastChapter?.title ?: "Глава"} • Страница ${progress!!.lastReadPage + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Chapter List Heading
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Список глав (${chapters.size}):",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (chapters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Нет доступных глав.", color = MaterialTheme.colorScheme.secondary)
                            Button(
                                onClick = { showAddChapterDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить первую главу")
                            }
                        }
                    }
                }
            } else {
                items(chapters) { chapter ->
                    val isRead = progress != null && (chapter.chapterNumber < progress!!.lastReadChapterId || chapter.id == progress!!.lastReadChapterId)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (chapter.id == progress?.lastReadChapterId) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        onClick = { onReadChapter(chapter) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isRead) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Размер: ${chapter.pageCount} страниц",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Читать", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Append chapter option at the end
                item {
                    OutlinedButton(
                        onClick = { showAddChapterDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить еще главу")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MangaReaderScreen(
    manga: Manga,
    chapter: MangaChapter,
    viewModel: OfflineTubeViewModel,
    onBack: () -> Unit
) {
    val pages = remember(chapter.pagesCsv) {
        chapter.pagesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    var showUIControls by remember { mutableStateOf(true) }

    val lazyListState = rememberLazyListState()

    // Restore page progress
    val progressFlow = remember(manga.id) { viewModel.getProgressForManga(manga.id) }
    val progress by progressFlow.collectAsStateWithLifecycle(initialValue = null)

    var isProgressRestored by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        if (!isProgressRestored && progress != null && progress!!.lastReadChapterId == chapter.id) {
            val savedPage = progress!!.lastReadPage
            if (savedPage in pages.indices) {
                lazyListState.scrollToItem(savedPage)
            }
            isProgressRestored = true
        }
    }

    // Save progress as they scroll
    LaunchedEffect(lazyListState.firstVisibleItemIndex) {
        val activePage = lazyListState.firstVisibleItemIndex
        if (activePage in pages.indices) {
            viewModel.saveMangaProgress(manga.id, chapter.id, activePage)
        }
    }

    val context = LocalContext.current
    val activity = context as? Activity

    // Toggle immersive system bars in sync with UI Controls
    LaunchedEffect(showUIControls) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (!showUIControls) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Restore system bars on exit
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Box containing the entire reader with dimming overlay support
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main Reader Content Container (strictly vertical continuous scroll list)
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = { showUIControls = !showUIControls },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentPadding = PaddingValues(
                top = if (showUIControls) 72.dp else 16.dp,
                bottom = if (showUIControls) 110.dp else 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(pages) { pageUri ->
                MangaPageRenderer(
                    pageUri = pageUri,
                    onTap = { showUIControls = !showUIControls },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }

        // Overlay toolbar at the top
        AnimatedVisibility(
            visible = showUIControls,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 220.dp)
                        )
                        Text(
                            text = manga.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 220.dp)
                        )
                    }
                }

                Text(
                    text = "Лента 📖",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }

        // Overlay control bar at the bottom
        AnimatedVisibility(
            visible = showUIControls,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Страница ${lazyListState.firstVisibleItemIndex + 1} из ${pages.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Скролл ↕️ • Зум 🔍",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Robust zoomable/pannable Manga Page Renderer.
 * Supports single-tap to toggle controls, pinch-to-zoom (up to 4x), multi-touch panning,
 * and double-tap to toggle zoom levels quickly.
 */
@Composable
fun MangaPageRenderer(
    pageUri: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1.0f, 4.0f)
        scale = nextScale
        if (nextScale > 1.0f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f) // traditional manga portrait layout
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.5.dp, Color.Black, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.0f) {
                            scale = 1.0f
                            offset = Offset.Zero
                        } else {
                            scale = 2.2f
                        }
                    }
                )
            }
            .transformable(state = state)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        contentAlignment = Alignment.Center
    ) {
        if (pageUri.startsWith("panel://")) {
            MangaCanvasPanel(panelId = pageUri)
        } else {
            AsyncImage(
                model = pageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * Renders beautiful fully offline drawn manga panels using Jetpack Compose Canvas,
 * including dialog speech bubbles and speed action lines.
 */
@Composable
fun MangaCanvasPanel(panelId: String) {
    // Parse mangaId, chapter and page from "panel://m1_1_2" style
    val details = remember(panelId) {
        val clean = panelId.removePrefix("panel://")
        val parts = clean.split("_")
        val manga = parts.getOrNull(0) ?: "m1"
        val chapter = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val page = parts.getOrNull(2)?.toIntOrNull() ?: 1
        Triple(manga, chapter, page)
    }

    val (mCode, chapterNum, pageNum) = details

    // Match dialog text, emotions and background composition style
    val (dialogText, soundEffect, mood) = remember(mCode, chapterNum, pageNum) {
        when (mCode) {
            "m1" -> { // Тайная любовь горничной
                when (pageNum) {
                    1 -> Triple(
                        "Какая огромная комната... Я должна быть очень осторожна здесь.",
                        "ШУРХ...",
                        "shy"
                    )
                    2 -> Triple(
                        "О нет! Фарфоровая чашка господина Рю...! Она выскальзывает!",
                        "ДЗЫНЬ!",
                        "panic"
                    )
                    3 -> Triple(
                        "Не шевелись. Осколки острые, ты можешь порезаться...",
                        "ХВАТЬ!",
                        "touch"
                    )
                    4 -> Triple(
                        "Г-господин Рю?.. Почему ваше лицо так близко? Моё сердце...",
                        "ТУК-ТУК!",
                        "love"
                    )
                    else -> Triple(
                        "Твои секреты в безопасности со мной. Если ты пообещаешь...",
                        "ШЁПОТ",
                        "teasing"
                    )
                }
            }
            "m2" -> { // Сладкое Искушение
                when (pageNum) {
                    1 -> Triple(
                        "Этот крем... Просто божественен! Но я могу сделать лучше!",
                        "М-М-М...",
                        "confident"
                    )
                    2 -> Triple(
                        "Лучше моего авторского крема? Какая дерзость для стажёра!",
                        "ТОП...",
                        "angry"
                    )
                    3 -> Triple(
                        "Ш-шеф Сато?! Я не... я просто проводила анализ конкурентов!",
                        "ОЙ!",
                        "panic"
                    )
                    4 -> Triple(
                        "Анализ, говоришь? Тогда позволь мне завершить этот эксперимент...",
                        "КРАСКА...",
                        "touch"
                    )
                    else -> Triple(
                        "Что это только что было?! Он... лизнул шоколад с моего лица?!",
                        "БУМ!",
                        "blush"
                    )
                }
            }
            else -> { // Полуночный Шепот
                when (pageNum) {
                    1 -> Triple(
                        "Дождь смывает следы этого города... Я совсем одна.",
                        "КАП-КАП",
                        "sad"
                    )
                    2 -> Triple(
                        "Мокнуть под дождём вредно для здоровья. Держи зонт.",
                        "ШУРХ...",
                        "touch"
                    )
                    3 -> Triple(
                        "Вы... тот самый пианист из полуночного джаз-бара?",
                        "ВЗГЛЯД",
                        "surprise"
                    )
                    4 -> Triple(
                        "А ты - та самая слушательница, которая никогда не улыбается.",
                        "УЛЫБКА",
                        "smile"
                    )
                    else -> Triple(
                        "Две одинокие тени наконец слились в одну под неоном города.",
                        "ШАГ...",
                        "love"
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Drawing standard Manga Art & Speed Lines on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Background Fill (Warm manga gray / screen tones)
            drawRect(color = Color(0xFFF9F9F9))

            // Draw panel split border lines
            drawRect(
                color = Color.Black,
                style = Stroke(width = 4f)
            )

            // Draw horizontal panel divider
            drawLine(
                color = Color.Black,
                start = Offset(0f, h * 0.55f),
                end = Offset(w, h * 0.55f),
                strokeWidth = 3f
            )

            // Draw dramatic background speed lines or half-tones depending on mood
            if (mood == "panic" || mood == "blush" || mood == "confident") {
                // Radial dramatic speedlines
                val center = Offset(w * 0.5f, h * 0.3f)
                for (angle in 0..360 step 8) {
                    val rad = Math.toRadians(angle.toDouble())
                    val endX = center.x + Math.cos(rad).toFloat() * w
                    val endY = center.y + Math.sin(rad).toFloat() * h
                    drawLine(
                        color = Color.Black.copy(alpha = 0.12f),
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 2f
                    )
                }
            } else if (mood == "love" || mood == "teasing" || mood == "touch") {
                // Love hearts or soft diagonal screentones
                for (i in -10..20) {
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(0f, i * 40f),
                        end = Offset(w, i * 40f + w),
                        strokeWidth = 10f
                    )
                }
            } else if (mood == "sad") {
                // Rain lines vertical
                for (x in 20..(w.toInt() - 20) step 40) {
                    val rY = (x % 3) * 30f
                    drawLine(
                        color = Color.DarkGray.copy(alpha = 0.25f),
                        start = Offset(x.toFloat(), rY),
                        end = Offset(x.toFloat() - 10f, rY + 120f),
                        strokeWidth = 1.5f
                    )
                }
            }

            // Draw artistic character silhouettes on Canvas
            when (mood) {
                "love", "touch", "teasing" -> {
                    // Two romantic silhouettes close together
                    drawCircle(
                        color = Color(0xFFE8E8E8),
                        radius = w * 0.25f,
                        center = Offset(w * 0.5f, h * 0.35f)
                    )
                    // Hearts
                    drawPath(
                        path = Path().apply {
                            moveTo(w * 0.45f, h * 0.15f)
                            cubicTo(w * 0.45f, h * 0.12f, w * 0.4f, h * 0.12f, w * 0.4f, h * 0.15f)
                            cubicTo(w * 0.4f, h * 0.2f, w * 0.45f, h * 0.23f, w * 0.45f, h * 0.25f)
                            cubicTo(w * 0.45f, h * 0.23f, w * 0.5f, h * 0.2f, w * 0.5f, h * 0.15f)
                            cubicTo(w * 0.5f, h * 0.12f, w * 0.45f, h * 0.12f, w * 0.45f, h * 0.15f)
                        },
                        color = Color(0xFFFF5252).copy(alpha = 0.6f)
                    )
                }
                "panic" -> {
                    // Spiky splash star background
                    val path = Path()
                    val center = Offset(w * 0.5f, h * 0.3f)
                    val spikes = 10
                    val outerRad = w * 0.35f
                    val innerRad = w * 0.18f
                    for (i in 0 until spikes * 2) {
                        val angle = i * Math.PI / spikes
                        val r = if (i % 2 == 0) outerRad else innerRad
                        val x = center.x + Math.cos(angle).toFloat() * r
                        val y = center.y + Math.sin(angle).toFloat() * r
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(path, color = Color(0xFFD6D6D6))
                }
                else -> {
                    // Soft gradient background for scenery
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        radius = w * 0.3f,
                        center = Offset(w * 0.3f, h * 0.28f)
                    )
                }
            }
        }

        // 2. Comic dialog speech bubble
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.9f)
                .padding(bottom = 16.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = dialogText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start
                )
            }
        }

        // 3. Manga Sound Effect sticker overlay! (Very stylish, e.g. "ТУК-ТУК")
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 24.dp, top = 28.dp)
                .background(Color.Black, RoundedCornerShape(6.dp))
                .border(1.5.dp, Color.White, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = soundEffect,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = if (soundEffect == "ДЗЫНЬ!" || soundEffect == "БУМ!") Color(0xFFFF5252) else Color.White,
                fontSize = 15.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMangaDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, author: String, description: String, coverUri: String, genre: String, rating: Float, isCompleted: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf("") }
    var rating by remember { mutableFloatStateOf(4.8f) }
    var isCompleted by remember { mutableStateOf(false) }

    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            coverUri = it.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить новую мангу 📖", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    placeholder = { Text("Например: Моя сладкая любовь") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Автор") },
                    placeholder = { Text("Например: Аои Миямото") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Жанр (через запятую)") },
                    placeholder = { Text("Например: Романтика, Драма, Комедия") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание сюжета") },
                    placeholder = { Text("Краткое описание произведения...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                // Cover Picker Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            singlePhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Выбрать обложку", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp)
                        }
                    }

                    if (coverUri.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            AsyncImage(
                                model = coverUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Rating
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Рейтинг: ${String.format("%.1f", rating)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Slider(
                        value = rating,
                        onValueChange = { rating = it },
                        valueRange = 1.0f..5.0f,
                        steps = 40
                    )
                }

                // Completed Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Произведение завершено?", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isCompleted, onCheckedChange = { isCompleted = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, author, description, coverUri, genre, rating, isCompleted)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChapterDialog(
    nextChapterNumber: Int,
    onDismiss: () -> Unit,
    onConfirm: (title: String, chapterNumber: Int, pageUris: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("Глава $nextChapterNumber: ") }
    var chapterNumber by remember { mutableIntStateOf(nextChapterNumber) }
    var selectedPages by remember { mutableStateOf<List<String>>(emptyList()) }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedPages = uris.map { it.toString() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить главу 📑", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название главы") },
                    placeholder = { Text("Например: Глава $nextChapterNumber: Случайная встреча") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = chapterNumber.toString(),
                    onValueChange = { chapterNumber = it.toIntOrNull() ?: nextChapterNumber },
                    label = { Text("Номер главы") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Page selection button
                Button(
                    onClick = {
                        multiplePhotoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            text = if (selectedPages.isEmpty()) "Выбрать страницы из галереи" else "Выбрано страниц: ${selectedPages.size}",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (selectedPages.isNotEmpty()) {
                    Text(
                        text = "Страницы будут скопированы во внутреннюю память приложения, чтобы быть всегда доступными без интернета.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // Small row of thumbnails of first few pages
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        selectedPages.take(5).forEach { uri ->
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        if (selectedPages.size > 5) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+${selectedPages.size - 5}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedPages.isNotEmpty()) {
                        onConfirm(title, chapterNumber, selectedPages)
                    }
                },
                enabled = title.isNotBlank() && selectedPages.isNotEmpty()
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

