package com.example.ui

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadFormat
import com.example.data.model.DownloadTask
import com.example.data.model.PlannedQueueSegment
import com.example.data.model.TaskStatus
import com.example.util.DeviceNetworkDependencyProfile
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberDark
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LogoMagenta
import com.example.ui.theme.LogoNavy
import com.example.ui.theme.LogoTeal
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseDark
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

/**
 * Filter modes for the MD3 GovTech DownloadQueueScreen.
 */
enum class DownloadQueueFilter(val labelEn: String, val labelBn: String) {
    ALL("All Tasks", "সকল টাস্ক"),
    ACTIVE_AND_PENDING("Active & Pending", "চলমান ও অপেক্ষমান"),
    ACTIVE("Active", "চলমান"),
    PENDING("Pending", "অপেক্ষমান"),
    COMPLETED("Completed", "সম্পন্ন"),
    FAILED("Failed", "ব্যর্থ")
}

/**
 * Dedicated Material Design 3 (MD3) GovTech DownloadQueueScreen.
 * Visualizes pending, active, completed, and failed download tasks with:
 * - GovTech Navy, Teal, and Magenta tonal branding header & 4-column real-time telemetry strip
 * - Animated global & per-task progress bars with live throughput (KB/s) and Khatian counters
 * - Interactive search & status filter chips focusing on active and pending tasks
 * - WorkManager background engine controls, batch sequencing status, and retry/pause actions
 */
@Composable
fun DownloadQueueScreen(
    viewModel: LRDownloaderViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var activeTaskIdForImport by remember { mutableStateOf<String?>(null) }

    val pdfPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val taskId = activeTaskIdForImport ?: return@let
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                val fileName = "imported_${System.currentTimeMillis()}.pdf"
                viewModel.importPdfForTask(context, taskId, bytes, fileName)
            }
            activeTaskIdForImport = null
        }
    }

    BackHandler {
        onNavigateBack()
    }

    val uiState by viewModel.uiState.collectAsState()
    val savedRecords by viewModel.savedRecords.collectAsState()
    val tasks = uiState.queue

    val activeTasks = remember(tasks) { tasks.filter { it.status == TaskStatus.DOWNLOADING } }
    val pendingTasks = remember(tasks) { tasks.filter { it.status == TaskStatus.PENDING } }
    val completedTasks = remember(tasks) { tasks.filter { it.status == TaskStatus.COMPLETED } }
    val failedTasks = remember(tasks) { tasks.filter { it.status == TaskStatus.FAILED } }

    var selectedFilter by rememberSaveable { mutableStateOf(DownloadQueueFilter.ALL) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredTasks = remember(tasks, selectedFilter, searchQuery) {
        val statusFiltered = when (selectedFilter) {
            DownloadQueueFilter.ALL -> tasks
            DownloadQueueFilter.ACTIVE_AND_PENDING -> tasks.filter {
                it.status == TaskStatus.DOWNLOADING || it.status == TaskStatus.PENDING
            }
            DownloadQueueFilter.ACTIVE -> activeTasks
            DownloadQueueFilter.PENDING -> pendingTasks
            DownloadQueueFilter.COMPLETED -> completedTasks
            DownloadQueueFilter.FAILED -> failedTasks
        }
        if (searchQuery.isBlank()) {
            statusFiltered
        } else {
            val q = searchQuery.trim().lowercase()
            statusFiltered.filter { task ->
                task.mouzaNameBn.lowercase().contains(q) ||
                    task.mouzaNameEn.lowercase().contains(q) ||
                    task.upazilaNameBn.lowercase().contains(q) ||
                    task.districtNameBn.lowercase().contains(q) ||
                    task.jlNo.lowercase().contains(q) ||
                    (task.referenceId?.lowercase()?.contains(q) == true)
            }
        }
    }

    val totalTasksCount = tasks.size
    val overallProgress = if (totalTasksCount == 0) 0 else {
        tasks.sumOf { it.progress } / totalTasksCount
    }
    val totalSpeedKbps = remember(activeTasks) {
        activeTasks.sumOf { it.speedKbps }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .testTag("download_queue_screen"),
        topBar = {
            GovTechDownloadQueueTopBar(
                isPaused = uiState.isPaused,
                totalCount = totalTasksCount,
                activeCount = activeTasks.size,
                pendingCount = pendingTasks.size,
                completedCount = completedTasks.size,
                failedCount = failedTasks.size,
                totalSpeedKbps = totalSpeedKbps,
                onTogglePauseAll = { viewModel.togglePauseQueue() },
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
            ) {
                DownloadQueueStickyActionToolbar(
                    isPaused = uiState.isPaused,
                    totalTasks = totalTasksCount,
                    pendingTasks = pendingTasks.size,
                    failedTasks = failedTasks.size,
                    onTogglePause = { viewModel.togglePauseQueue() },
                    onRetryFailed = { viewModel.retryAllFailed() },
                    onClearQueue = { viewModel.clearQueue() },
                    onReturnToSelection = onNavigateBack
                )
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        selected = false,
                        onClick = onNavigateBack,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Downloader"
                            )
                        },
                        label = { Text("Downloader") },
                        modifier = Modifier.testTag("nav_item_downloader")
                    )
                    NavigationBarItem(
                        selected = true,
                        onClick = { /* Already on DownloadQueueScreen */ },
                        icon = {
                            BadgedBox(
                                badge = {
                                    val activeAndPending = activeTasks.size + pendingTasks.size
                                    if (activeAndPending > 0) {
                                        Badge(containerColor = if (activeTasks.isNotEmpty()) BlueAccent else AmberDark) {
                                            Text("$activeAndPending")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListNumbered,
                                    contentDescription = "Queue Status"
                                )
                            }
                        },
                        label = { Text("Queue Status", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_item_queue_status")
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { viewModel.setShowHistoryDialog(true) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (savedRecords.isNotEmpty()) {
                                        Badge(containerColor = EmeraldPrimary) {
                                            Text("${savedRecords.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Vault"
                                )
                            }
                        },
                        label = { Text("Vault") },
                        modifier = Modifier.testTag("nav_item_vault")
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("queue_status_screen"),
            contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Real-Time Pipeline Progress & WorkManager Telemetry Hero Card
            item {
                GovTechPipelineHeroCard(
                    isPaused = uiState.isPaused,
                    totalTasks = totalTasksCount,
                    completedCount = completedTasks.size,
                    activeCount = activeTasks.size,
                    pendingCount = pendingTasks.size,
                    failedCount = failedTasks.size,
                    overallProgress = overallProgress,
                    totalSpeedKbps = totalSpeedKbps,
                    currentBatch = uiState.currentBatchNumber,
                    totalBatches = viewModel.totalBatches,
                    plannedQueues = uiState.plannedQueues,
                    deviceNetworkProfile = uiState.deviceNetworkProfile,
                    isWorkManagerActive = uiState.isWorkManagerActive,
                    useWorkManagerService = uiState.useWorkManagerBackgroundService,
                    antiBlockingDescription = uiState.antiBlockingDelayDescription,
                    onTogglePauseAll = { viewModel.togglePauseQueue() },
                    onToggleWorkManager = { viewModel.setUseWorkManagerBackgroundService(it) },
                    onRetryAllFailed = { viewModel.retryAllFailed() }
                )
            }

            // 2. Search & Filter Control Card (MD3 Tonal Surface)
            item {
                QueueSearchAndFilterBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedFilter = selectedFilter,
                    totalCount = totalTasksCount,
                    activeAndPendingCount = activeTasks.size + pendingTasks.size,
                    activeCount = activeTasks.size,
                    pendingCount = pendingTasks.size,
                    completedCount = completedTasks.size,
                    failedCount = failedTasks.size,
                    onSelectFilter = { selectedFilter = it }
                )
            }

            // 3. Dedicated Active Downloads Spotlight (when in ALL or ACTIVE_AND_PENDING view and active tasks exist)
            if ((selectedFilter == DownloadQueueFilter.ALL || selectedFilter == DownloadQueueFilter.ACTIVE_AND_PENDING) &&
                activeTasks.isNotEmpty() && searchQuery.isBlank()
            ) {
                item {
                    QueueSectionHeader(
                        titleEn = "ACTIVE DOWNLOADS",
                        titleBn = "চলমান ডাউনলোড (${activeTasks.size})",
                        badgeCount = activeTasks.size,
                        accentColor = BlueAccent,
                        containerColor = BlueContainer,
                        testTag = "active_downloads_section_header"
                    )
                }

                itemsIndexed(
                    items = activeTasks,
                    key = { _, task -> "spotlight_active_${task.id}" }
                ) { index, task ->
                    GovTechQueueTaskCard(
                        task = task,
                        queuePosition = index + 1,
                        onRetry = { viewModel.retryTask(task.id) },
                        onRemove = { viewModel.removeTaskFromQueue(task.id) },
                        onImportPdf = {
                            activeTaskIdForImport = task.id
                            pdfPickerLauncher.launch("application/pdf")
                        },
                        onOpenBrowser = {
                            viewModel.openUserAssistedBrowserForTask(context, task)
                        }
                    )
                }
            }

            // 4. Main Queue Tasks Section Header
            item {
                QueueSectionHeader(
                    titleEn = when (selectedFilter) {
                        DownloadQueueFilter.ALL -> "QUEUE REGISTRY"
                        DownloadQueueFilter.ACTIVE_AND_PENDING -> "PENDING & ACTIVE PIPELINE"
                        DownloadQueueFilter.ACTIVE -> "ACTIVE STREAM"
                        DownloadQueueFilter.PENDING -> "PENDING QUEUE"
                        DownloadQueueFilter.COMPLETED -> "VERIFIED COMPLETED"
                        DownloadQueueFilter.FAILED -> "FAILED & RETRY QUEUE"
                    },
                    titleBn = when (selectedFilter) {
                        DownloadQueueFilter.ALL -> "সকল কিউ তালিকা"
                        DownloadQueueFilter.ACTIVE_AND_PENDING -> "অপেক্ষমান ও চলমান টাস্ক"
                        DownloadQueueFilter.ACTIVE -> "চলমান ডাউনলোড"
                        DownloadQueueFilter.PENDING -> "অপেক্ষমান মৌজা ও খতিয়ান"
                        DownloadQueueFilter.COMPLETED -> "যাচাইকৃত সম্পন্ন রেকর্ড"
                        DownloadQueueFilter.FAILED -> "ত্রুটিপূর্ণ রেকর্ড"
                    },
                    badgeCount = filteredTasks.size,
                    accentColor = when (selectedFilter) {
                        DownloadQueueFilter.ALL -> Slate800
                        DownloadQueueFilter.ACTIVE_AND_PENDING -> LogoTeal
                        DownloadQueueFilter.ACTIVE -> BlueAccent
                        DownloadQueueFilter.PENDING -> AmberDark
                        DownloadQueueFilter.COMPLETED -> EmeraldDark
                        DownloadQueueFilter.FAILED -> RoseError
                    },
                    containerColor = when (selectedFilter) {
                        DownloadQueueFilter.ALL -> Slate200
                        DownloadQueueFilter.ACTIVE_AND_PENDING -> EmeraldContainer
                        DownloadQueueFilter.ACTIVE -> BlueContainer
                        DownloadQueueFilter.PENDING -> AmberContainer
                        DownloadQueueFilter.COMPLETED -> EmeraldContainer
                        DownloadQueueFilter.FAILED -> RoseContainer
                    },
                    testTag = "dashboard_section_header"
                )
            }

            // 5. Task Cards List or Empty State
            if (filteredTasks.isEmpty()) {
                item {
                    GovTechEmptyQueueCard(
                        filter = selectedFilter,
                        hasSearchQuery = searchQuery.isNotBlank(),
                        isPaused = uiState.isPaused,
                        onResetFilter = {
                            selectedFilter = DownloadQueueFilter.ALL
                            searchQuery = ""
                        },
                        onNavigateBack = onNavigateBack,
                        onResume = { viewModel.togglePauseQueue() }
                    )
                }
            } else {
                itemsIndexed(
                    items = filteredTasks,
                    key = { _, task -> task.id }
                ) { index, task ->
                    GovTechQueueTaskCard(
                        task = task,
                        queuePosition = index + 1,
                        onRetry = { viewModel.retryTask(task.id) },
                        onRemove = { viewModel.removeTaskFromQueue(task.id) },
                        onImportPdf = {
                            activeTaskIdForImport = task.id
                            pdfPickerLauncher.launch("application/pdf")
                        },
                        onOpenBrowser = {
                            viewModel.openUserAssistedBrowserForTask(context, task)
                        }
                    )
                }
            }
        }
    }
}

/**
 * GovTech Navy/Teal/Magenta Top Header with 4-column real-time stats strip.
 */
@Composable
private fun GovTechDownloadQueueTopBar(
    isPaused: Boolean,
    totalCount: Int,
    activeCount: Int,
    pendingCount: Int,
    completedCount: Int,
    failedCount: Int,
    totalSpeedKbps: Int,
    onTogglePauseAll: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Surface(
        color = LogoNavy,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Back Button + Brand Badge + Title + Live Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .testTag("queue_screen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Downloader",
                            tint = Color.White
                        )
                    }

                    // Mini 3D-style GovTech Brand Emblem
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate800)
                            .border(1.dp, LogoTeal.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LR",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color.White
                                ),
                                modifier = Modifier.offset(y = 2.dp)
                            )
                            Text(
                                text = "QUEUE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 7.sp,
                                    color = LogoTeal
                                )
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "GOVTECH BD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.8.sp
                                ),
                                color = EmeraldContainer,
                                modifier = Modifier
                                    .background(EmeraldDark, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "ডাউনলোড কিউ মনিটর",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    color = EmeraldLight
                                )
                            )
                        }
                        Text(
                            text = "Download Queue",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Top Right: Pause All / Resume All Button + Live State Badge
                val canPauseOrResume = totalCount > 0 && (activeCount > 0 || pendingCount > 0 || isPaused)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTogglePauseAll,
                        enabled = canPauseOrResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaused) EmeraldPrimary else AmberDark,
                            contentColor = Color.White,
                            disabledContainerColor = Color.White.copy(alpha = 0.12f),
                            disabledContentColor = Color.White.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("top_pause_resume_all_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume All Downloads" else "Pause All Downloads",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = if (isPaused) "Resume All" else "Pause All",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (isPaused) "চালু করুন" else "ব্যান্ডউইথ সেভার",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                }
            }

            // Zone 1: 4-Column Real-Time Stats Overview Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GovTechHeaderStatPill(
                    labelBn = "মোট টাস্ক",
                    labelEn = "TOTAL",
                    count = totalCount,
                    accentColor = Color.White,
                    containerColor = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1f)
                )
                GovTechHeaderStatPill(
                    labelBn = "চলমান",
                    labelEn = "ACTIVE",
                    count = activeCount,
                    accentColor = LogoTeal,
                    containerColor = BlueAccent.copy(alpha = 0.28f),
                    modifier = Modifier.weight(1f)
                )
                GovTechHeaderStatPill(
                    labelBn = "অপেক্ষমান",
                    labelEn = "PENDING",
                    count = pendingCount,
                    accentColor = AmberAccent,
                    containerColor = AmberDark.copy(alpha = 0.28f),
                    modifier = Modifier.weight(1f)
                )
                GovTechHeaderStatPill(
                    labelBn = if (failedCount > 0) "ব্যর্থ ($failedCount)" else "সম্পন্ন",
                    labelEn = if (failedCount > 0) "FAILED" else "DONE",
                    count = if (failedCount > 0) failedCount else completedCount,
                    accentColor = if (failedCount > 0) RoseContainer else EmeraldLight,
                    containerColor = if (failedCount > 0) RoseDark.copy(alpha = 0.45f) else EmeraldDark.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GovTechHeaderStatPill(
    labelBn: String,
    labelEn: String,
    count: Int,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = labelEn,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp
            ),
            color = accentColor.copy(alpha = 0.85f)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 17.sp
            ),
            color = Color.White
        )
        Text(
            text = labelBn,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Hero Card displaying overall batch progress, active throughput, and WorkManager engine status.
 */
@Composable
private fun GovTechPipelineHeroCard(
    isPaused: Boolean,
    totalTasks: Int,
    completedCount: Int,
    activeCount: Int,
    pendingCount: Int,
    failedCount: Int,
    overallProgress: Int,
    totalSpeedKbps: Int,
    currentBatch: Int,
    totalBatches: Int,
    plannedQueues: List<PlannedQueueSegment> = emptyList(),
    deviceNetworkProfile: DeviceNetworkDependencyProfile? = null,
    isWorkManagerActive: Boolean,
    useWorkManagerService: Boolean,
    antiBlockingDescription: String,
    onTogglePauseAll: () -> Unit,
    onToggleWorkManager: (Boolean) -> Unit,
    onRetryAllFailed: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (overallProgress / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "overall_queue_progress_anim"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "hero_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_dot"
    )

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(24.dp))
            .testTag("queue_hero_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    failedCount > 0 -> RoseError
                                    isPaused -> AmberAccent
                                    activeCount > 0 -> EmeraldPrimary.copy(alpha = pulseAlpha)
                                    pendingCount > 0 -> BlueAccent
                                    else -> Slate400
                                }
                            )
                    )
                    Column {
                        Text(
                            text = when {
                                failedCount > 0 -> "Attention Needed ($failedCount Failed)"
                                isPaused -> "Queue Paused (ডাউনলোড স্থগিত)"
                                activeCount > 0 -> "Downloading Active Batch #$currentBatch"
                                pendingCount > 0 -> "$pendingCount Tasks Pending in Queue"
                                totalTasks == 0 -> "Queue Ready (প্রস্তুত)"
                                else -> "All Queued Downloads Completed"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                        )
                        Text(
                            text = "Active: $activeCount • Pending: $pendingCount • Done: $completedCount/$totalTasks",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate500,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (totalBatches > 0) {
                        Surface(
                            color = EmeraldContainer,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = "Queue $currentBatch/$totalBatches",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldDark
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    val canPauseOrResume = totalTasks > 0 && (activeCount > 0 || pendingCount > 0 || isPaused)
                    FilledTonalButton(
                        onClick = onTogglePauseAll,
                        enabled = canPauseOrResume,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isPaused) EmeraldContainer else AmberContainer,
                            contentColor = if (isPaused) EmeraldDark else AmberDark
                        ),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("hero_pause_resume_all_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume All" else "Pause All",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isPaused) "Resume All" else "Pause All",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }

            // Global Progress Bar Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Slate50)
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (activeCount > 0) BlueAccent else EmeraldDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Global Queue Completion",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeCount > 0 && totalSpeedKbps > 0) {
                            Text(
                                text = "$totalSpeedKbps KB/s",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueAccent
                                ),
                                modifier = Modifier
                                    .background(BlueContainer, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "$overallProgress%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (overallProgress == 100) EmeraldDark else BlueAccent
                            )
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .testTag("overall_queue_progress_bar"),
                    color = when {
                        failedCount > 0 -> AmberAccent
                        overallProgress == 100 -> EmeraldDark
                        activeCount > 0 -> BlueAccent
                        else -> EmeraldPrimary
                    },
                    trackColor = Slate200
                )

                deviceNetworkProfile?.let { profile ->
                    Text(
                        text = "${profile.profileTitleBn} • প্রতি কিউতে ${profile.mouzasPerQueue}টি মৌজা • একে একে শেষ মৌজা পর্যন্ত ডাউনলোড",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = Slate600,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            if (plannedQueues.isNotEmpty()) {
                SequentialMultiQueuePipelineStrip(
                    plannedQueues = plannedQueues,
                    activeBatchNumber = currentBatch
                )
            }

            // Failed Tasks Banner with 1-Click Retry All
            AnimatedVisibility(visible = failedCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RoseContainer.copy(alpha = 0.6f))
                        .border(1.dp, RoseError.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = RoseError,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "$failedCount টি রেকর্ড ডাউনলোডে ত্রুটি হয়েছে",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            )
                            Text(
                                text = "Exponential Backoff সহ পুনরায় চেষ্টা করতে Retry চাপুন",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = Slate700
                                )
                            )
                        }
                    }

                    Button(
                        onClick = onRetryAllFailed,
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("retry_all_failed_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry All", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            HorizontalDivider(color = Slate200, thickness = 1.dp)

            // WorkManager Anti-Blocking Engine Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isWorkManagerActive) EmeraldContainer.copy(alpha = 0.4f) else Slate100)
                    .border(
                        1.dp,
                        if (isWorkManagerActive) EmeraldPrimary.copy(alpha = 0.45f) else Slate200,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isWorkManagerActive) EmeraldContainer else Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isWorkManagerActive) EmeraldDark else Slate600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "WorkManager Background Queue Engine",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isWorkManagerActive) EmeraldDark else Slate800
                        )
                        Text(
                            text = if (isWorkManagerActive) {
                                "Active Foreground Sync • $antiBlockingDescription"
                            } else {
                                "Bounded Concurrency (Max 2) • $antiBlockingDescription"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = if (isWorkManagerActive) EmeraldDark else Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Switch(
                    checked = useWorkManagerService,
                    onCheckedChange = onToggleWorkManager,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldPrimary,
                        checkedTrackColor = EmeraldContainer
                    ),
                    modifier = Modifier.testTag("queue_screen_workmanager_switch")
                )
            }
        }
    }
}

/**
 * Search bar and horizontal filter chips for slicing pending, active, completed, and failed tasks.
 */
@Composable
private fun QueueSearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: DownloadQueueFilter,
    totalCount: Int,
    activeAndPendingCount: Int,
    activeCount: Int,
    pendingCount: Int,
    completedCount: Int,
    failedCount: Int,
    onSelectFilter: (DownloadQueueFilter) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search Mouza, J.L. No, Upazila or Ref ID...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Queue",
                    tint = Slate500,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("clear_queue_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(999.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = BlueAccent,
                unfocusedBorderColor = Slate200
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("download_queue_search_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GovTechFilterChip(
                label = "All ($totalCount)",
                selected = selectedFilter == DownloadQueueFilter.ALL,
                icon = Icons.Default.Layers,
                selectedColor = LogoNavy,
                onClick = { onSelectFilter(DownloadQueueFilter.ALL) },
                testTag = "filter_chip_all"
            )
            GovTechFilterChip(
                label = "Active & Pending ($activeAndPendingCount)",
                selected = selectedFilter == DownloadQueueFilter.ACTIVE_AND_PENDING,
                icon = Icons.Default.Schedule,
                selectedColor = EmeraldDark,
                onClick = { onSelectFilter(DownloadQueueFilter.ACTIVE_AND_PENDING) },
                testTag = "filter_chip_active_pending"
            )
            GovTechFilterChip(
                label = "Active ($activeCount)",
                selected = selectedFilter == DownloadQueueFilter.ACTIVE,
                icon = Icons.Default.Sync,
                selectedColor = BlueAccent,
                onClick = { onSelectFilter(DownloadQueueFilter.ACTIVE) },
                testTag = "filter_chip_active"
            )
            GovTechFilterChip(
                label = "Pending ($pendingCount)",
                selected = selectedFilter == DownloadQueueFilter.PENDING,
                icon = Icons.Default.HourglassEmpty,
                selectedColor = AmberDark,
                onClick = { onSelectFilter(DownloadQueueFilter.PENDING) },
                testTag = "filter_chip_pending"
            )
            GovTechFilterChip(
                label = "Success ($completedCount)",
                selected = selectedFilter == DownloadQueueFilter.COMPLETED,
                icon = Icons.Default.CheckCircle,
                selectedColor = EmeraldPrimary,
                onClick = { onSelectFilter(DownloadQueueFilter.COMPLETED) },
                testTag = "filter_chip_success"
            )
            if (failedCount > 0) {
                GovTechFilterChip(
                    label = "Failed ($failedCount)",
                    selected = selectedFilter == DownloadQueueFilter.FAILED,
                    icon = Icons.Default.Error,
                    selectedColor = RoseError,
                    onClick = { onSelectFilter(DownloadQueueFilter.FAILED) },
                    testTag = "filter_chip_failed"
                )
            }
        }
    }
}

@Composable
private fun GovTechFilterChip(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    selectedColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                )
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) selectedColor else Slate500
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedColor.copy(alpha = 0.14f),
            selectedLabelColor = selectedColor,
            containerColor = Color.White,
            labelColor = Slate700
        ),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.testTag(testTag)
    )
}

/**
 * Individual Download Task Card designed according to the MD3 GovTech specification.
 * Clearly distinguishes Active (Downloading), Pending (Queued), Completed, and Failed states.
 */
@Composable
private fun GovTechQueueTaskCard(
    task: DownloadTask,
    queuePosition: Int,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onImportPdf: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    val isSuccess = task.status == TaskStatus.COMPLETED
    val isRunning = task.status == TaskStatus.DOWNLOADING || task.status == TaskStatus.RUNNING
    val isFailed = task.status == TaskStatus.FAILED || task.status == TaskStatus.UNAVAILABLE
    val isPending = task.status == TaskStatus.PENDING || task.status == TaskStatus.WAITING_FOR_USER
    val isAwaitingImport = task.status == TaskStatus.AWAITING_USER_IMPORT || task.status == TaskStatus.WAITING_FOR_USER

    val animatedTaskProgress by animateFloatAsState(
        targetValue = (task.progress / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "task_progress_${task.id}"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isRunning -> BlueAccent.copy(alpha = 0.65f)
            isPending -> AmberAccent.copy(alpha = 0.45f)
            isAwaitingImport -> LogoMagenta.copy(alpha = 0.65f)
            isSuccess -> EmeraldPrimary.copy(alpha = 0.45f)
            isFailed -> RoseError.copy(alpha = 0.65f)
            else -> Slate200
        },
        label = "card_border_color"
    )

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                isRunning -> Color.White
                isFailed -> RoseContainer.copy(alpha = 0.15f)
                else -> Color.White
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isRunning) 4.dp else 2.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isRunning) 2.dp else 1.2.dp, cardBorderColor, RoundedCornerShape(18.dp))
            .animateContentSize()
            .testTag(
                when {
                    isRunning -> "active_task_${task.id}"
                    isSuccess -> "completed_task_${task.id}"
                    else -> "pending_task_${task.id}"
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Status Emblem + Mouza & Location Metadata + Status Pill & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Status Icon Container
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSuccess -> EmeraldContainer
                                    isRunning -> BlueContainer
                                    isAwaitingImport -> LogoMagenta.copy(alpha = 0.12f)
                                    isFailed -> RoseContainer
                                    else -> AmberContainer.copy(alpha = 0.75f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isSuccess -> Icons.Default.CheckCircle
                                isRunning -> Icons.Default.Sync
                                isAwaitingImport -> Icons.Default.Description
                                isFailed -> Icons.Default.Error
                                else -> Icons.Default.HourglassTop
                            },
                            contentDescription = task.status.name,
                            tint = when {
                                isSuccess -> EmeraldDark
                                isRunning -> BlueAccent
                                isAwaitingImport -> LogoMagenta
                                isFailed -> RoseError
                                else -> AmberDark
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = task.mouzaNameBn,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Slate900
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (task.referenceId != null) {
                                Surface(
                                    color = LogoMagenta.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Ref #${task.referenceId}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            color = LogoMagenta
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${task.districtNameBn} ➔ ${task.upazilaNameBn} • J.L. No. ${task.jlNo} (${task.mouzaNameEn})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = Slate600
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right-side Status Pill & Action Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when {
                        isSuccess -> {
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldDark,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "COMPLETED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp,
                                            color = EmeraldDark
                                        )
                                    )
                                }
                            }
                        }
                        isRunning -> {
                            Surface(
                                color = BlueContainer,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = BlueAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${task.progress}% ACTIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            color = BlueAccent
                                        )
                                    )
                                }
                            }
                        }
                        isFailed -> {
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("retry_task_button_${task.id}")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        isAwaitingImport -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = onOpenBrowser,
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Portal", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = onImportPdf,
                                    colors = ButtonDefaults.buttonColors(containerColor = LogoMagenta),
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("import_pdf_button_${task.id}")
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        isPending -> {
                            Surface(
                                color = AmberContainer.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "PENDING #$queuePosition",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AmberDark,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (!isRunning) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("remove_task_button_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove task from queue",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Progress Bar & Live Telemetry Row
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedTaskProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("task_progress_bar_${task.id}"),
                    color = when {
                        isSuccess -> EmeraldPrimary
                        isRunning -> BlueAccent
                        isFailed -> RoseError
                        else -> AmberAccent
                    },
                    trackColor = Slate200
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isSuccess -> "Verified ${task.totalKhatians}/${task.totalKhatians} Khatians"
                            isRunning -> "Downloading ${task.downloadedKhatians}/${task.totalKhatians} Khatians"
                            isAwaitingImport -> "Awaiting Manual PDF Import • Tap to Open Portal"
                            isFailed -> task.errorDetails ?: "Connection interrupted • Tap Retry"
                            else -> "Waiting in Queue • ${task.totalKhatians} Khatians"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSuccess -> EmeraldDark
                                isRunning -> BlueAccent
                                isFailed -> RoseError
                                else -> Slate600
                            }
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isRunning && task.speedKbps > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = EmeraldDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${task.speedKbps} KB/s",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }

                        Text(
                            text = String.format("%.2f MB", task.fileSizeBytes / (1024.0 * 1024.0)),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate500,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Metadata Chips Footer: Survey Types, Format, and Vault/Batch Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Survey Record Type Badge
                    Surface(
                        color = EmeraldContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.recordTypeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = EmeraldDark
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    // Output Format Badge (PDF / IMAGE)
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (task.downloadFormat == DownloadFormat.PDF) {
                                    Icons.Default.Description
                                } else {
                                    Icons.Default.Image
                                },
                                contentDescription = null,
                                tint = Slate600,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = task.downloadFormat.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = Slate700
                                )
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.FolderSpecial else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isSuccess) EmeraldDark else Slate500,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isSuccess) {
                            "LandRecords/${task.upazilaNameBn}"
                        } else {
                            "Batch #${task.batchNumber} • Jitter Safe"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = if (isSuccess) EmeraldDark else Slate500,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Section Header with uppercase micro-label, Bengali subtitle, and pill count badge.
 */
@Composable
private fun QueueSectionHeader(
    titleEn: String,
    titleBn: String,
    badgeCount: Int,
    accentColor: Color,
    containerColor: Color,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = titleEn,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.9.sp,
                    color = Slate500
                )
            )
            Text(
                text = titleBn,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900
                )
            )
        }

        Surface(
            color = containerColor,
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                text = "$badgeCount Items",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Empty state card for the DownloadQueueScreen when no tasks match the current filter.
 */
@Composable
private fun GovTechEmptyQueueCard(
    filter: DownloadQueueFilter,
    hasSearchQuery: Boolean,
    isPaused: Boolean,
    onResetFilter: () -> Unit,
    onNavigateBack: () -> Unit,
    onResume: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_queue_state_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Slate100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        hasSearchQuery -> Icons.Default.Search
                        filter == DownloadQueueFilter.FAILED -> Icons.Default.CheckCircle
                        filter == DownloadQueueFilter.ACTIVE && isPaused -> Icons.Default.Pause
                        else -> Icons.Default.CloudDownload
                    },
                    contentDescription = null,
                    tint = if (filter == DownloadQueueFilter.FAILED && !hasSearchQuery) EmeraldPrimary else Slate500,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when {
                    hasSearchQuery -> "No Matching Queue Tasks Found"
                    filter == DownloadQueueFilter.ALL -> "Download Queue is Empty (ডাউনলোড কিউ খালি)"
                    filter == DownloadQueueFilter.ACTIVE_AND_PENDING -> "No Active or Pending Tasks"
                    filter == DownloadQueueFilter.ACTIVE -> if (isPaused) "Queue is Currently Paused" else "No Active Downloads Right Now"
                    filter == DownloadQueueFilter.PENDING -> "No Pending Tasks Waiting in Queue"
                    filter == DownloadQueueFilter.COMPLETED -> "No Completed Tasks Yet"
                    filter == DownloadQueueFilter.FAILED -> "Zero Failed Tasks! Pipeline Healthy"
                    else -> "No Tasks Available"
                },
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate800
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when {
                    hasSearchQuery -> "Try clearing your search query or switching filter tabs."
                    filter == DownloadQueueFilter.ALL -> "Select an Upazila & Mouzas from the main Downloader screen to enqueue batch land record downloads."
                    filter == DownloadQueueFilter.ACTIVE && isPaused -> "Tap Resume below to continue processing pending Mouza records."
                    else -> "Switch filter tabs or return to the Downloader to add new Mouza batches."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate500,
                    fontSize = 12.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (filter != DownloadQueueFilter.ALL || hasSearchQuery) {
                    OutlinedButton(
                        onClick = onResetFilter,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("reset_queue_filter_button")
                    ) {
                        Text("Show All Tasks", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                if (isPaused && (filter == DownloadQueueFilter.ACTIVE || filter == DownloadQueueFilter.ACTIVE_AND_PENDING)) {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume Queue", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                } else {
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = LogoNavy),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("empty_state_select_mouzas_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Mouzas to Queue", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

/**
 * Sticky bottom control bar with Back, Pause/Resume, Retry Failed, and Clear actions.
 */
@Composable
private fun DownloadQueueStickyActionToolbar(
    isPaused: Boolean,
    totalTasks: Int,
    pendingTasks: Int,
    failedTasks: Int,
    onTogglePause: () -> Unit,
    onRetryFailed: () -> Unit,
    onClearQueue: () -> Unit,
    onReturnToSelection: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReturnToSelection,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                modifier = Modifier
                    .weight(0.9f)
                    .height(48.dp)
                    .testTag("queue_screen_bottom_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Button(
                onClick = if (failedTasks > 0 && pendingTasks == 0) onRetryFailed else onTogglePause,
                enabled = totalTasks > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        failedTasks > 0 && pendingTasks == 0 -> RoseError
                        isPaused -> EmeraldDark
                        else -> LogoNavy
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("queue_screen_bottom_pause_resume_btn")
            ) {
                Icon(
                    imageVector = when {
                        failedTasks > 0 && pendingTasks == 0 -> Icons.Default.Refresh
                        isPaused -> Icons.Default.PlayArrow
                        else -> Icons.Default.Pause
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        failedTasks > 0 && pendingTasks == 0 -> "Retry ($failedTasks)"
                        isPaused -> "Resume"
                        else -> "Pause"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            OutlinedButton(
                onClick = onClearQueue,
                enabled = totalTasks > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                modifier = Modifier
                    .weight(0.9f)
                    .height(48.dp)
                    .testTag("queue_screen_bottom_clear_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
