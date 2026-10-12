package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadTask
import com.example.data.model.TaskStatus
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberDark
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RoseContainer
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
 * Filter categories for WorkManager Progress Dashboard
 */
enum class QueueDashboardFilter(val label: String) {
    ALL("All Items"),
    ACTIVE("Active"),
    SUCCESS("Success"),
    PENDING("Pending"),
    FAILED("Failed")
}

/**
 * Dedicated Material 3 Progress Dashboard Screen:
 * 1. Real-time status of the WorkManager queue items
 * 2. High-precision progress bars and telemetry
 * 3. Prominent success, failure, active, and pending icons
 * 4. Interactive filter tabs and retry/pause controls
 */
@Composable
fun QueueStatusScreen(
    viewModel: LRDownloaderViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept hardware/system back gesture to return to Downloader
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

    var selectedFilter by remember { mutableStateOf(QueueDashboardFilter.ALL) }

    val filteredTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            QueueDashboardFilter.ALL -> tasks
            QueueDashboardFilter.ACTIVE -> activeTasks
            QueueDashboardFilter.SUCCESS -> completedTasks
            QueueDashboardFilter.PENDING -> pendingTasks
            QueueDashboardFilter.FAILED -> failedTasks
        }
    }

    val totalTasksCount = tasks.size
    val overallProgress = if (totalTasksCount == 0) 0 else {
        tasks.sumOf { it.progress } / totalTasksCount
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .testTag("queue_status_screen"),
        topBar = {
            QueueStatusTopBar(
                isPaused = uiState.isPaused,
                activeCount = activeTasks.size,
                pendingCount = pendingTasks.size,
                failedCount = failedTasks.size,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                QueueStickyBottomBar(
                    isPaused = uiState.isPaused,
                    totalTasks = totalTasksCount,
                    pendingTasks = pendingTasks.size,
                    onTogglePause = { viewModel.togglePauseQueue() },
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
                        onClick = { /* already on queue screen */ },
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
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Overall Real-Time WorkManager Dashboard Hero
            item {
                QueueStatusHeroCard(
                    isPaused = uiState.isPaused,
                    totalTasks = totalTasksCount,
                    completedCount = completedTasks.size,
                    activeCount = activeTasks.size,
                    pendingCount = pendingTasks.size,
                    failedCount = failedTasks.size,
                    overallProgress = overallProgress,
                    currentBatch = uiState.currentBatchNumber,
                    totalBatches = viewModel.totalBatches,
                    targetTotalMouzas = uiState.targetTotalMouzas,
                    isWorkManagerActive = uiState.isWorkManagerActive,
                    useWorkManagerService = uiState.useWorkManagerBackgroundService,
                    antiBlockingDescription = uiState.antiBlockingDelayDescription,
                    onToggleWorkManager = { viewModel.setUseWorkManagerBackgroundService(it) },
                    onRetryAllFailed = { viewModel.retryAllFailed() }
                )
            }

            // 2. Interactive Material 3 Filter Chips Row
            item {
                DashboardFilterRow(
                    selectedFilter = selectedFilter,
                    totalCount = totalTasksCount,
                    activeCount = activeTasks.size,
                    successCount = completedTasks.size,
                    pendingCount = pendingTasks.size,
                    failedCount = failedTasks.size,
                    onSelectFilter = { selectedFilter = it }
                )
            }

            // 3. Queue Section Title with Active Counts
            item {
                SectionHeaderRow(
                    title = when (selectedFilter) {
                        QueueDashboardFilter.ALL -> "Queue Items (কিউ তালিকা)"
                        QueueDashboardFilter.ACTIVE -> "Active Downloads (চলমান ডাউনলোড)"
                        QueueDashboardFilter.SUCCESS -> "Verified & Completed (সম্পন্ন)"
                        QueueDashboardFilter.PENDING -> "Pending in Queue (অপেক্ষমান)"
                        QueueDashboardFilter.FAILED -> "Failed / Retry Queue (ব্যর্থ রেকর্ড)"
                    },
                    badgeCount = filteredTasks.size,
                    badgeColor = when (selectedFilter) {
                        QueueDashboardFilter.ALL -> Slate800
                        QueueDashboardFilter.ACTIVE -> BlueAccent
                        QueueDashboardFilter.SUCCESS -> EmeraldDark
                        QueueDashboardFilter.PENDING -> AmberDark
                        QueueDashboardFilter.FAILED -> RoseError
                    },
                    badgeBg = when (selectedFilter) {
                        QueueDashboardFilter.ALL -> Slate200
                        QueueDashboardFilter.ACTIVE -> BlueContainer
                        QueueDashboardFilter.SUCCESS -> EmeraldContainer
                        QueueDashboardFilter.PENDING -> AmberContainer
                        QueueDashboardFilter.FAILED -> RoseContainer
                    },
                    testTag = "dashboard_section_header"
                )
            }

            // 4. Render Queue Item Dashboard Cards with Progress Bars & Success/Failure Icons
            if (filteredTasks.isEmpty()) {
                item {
                    EmptyFilterStateCard(
                        filter = selectedFilter,
                        isPaused = uiState.isPaused,
                        onResetFilter = { selectedFilter = QueueDashboardFilter.ALL },
                        onNavigateBack = onNavigateBack,
                        onResume = { viewModel.togglePauseQueue() }
                    )
                }
            } else {
                itemsIndexed(filteredTasks, key = { _, task -> task.id }) { index, task ->
                    QueueItemDashboardCard(
                        task = task,
                        queueIndex = index + 1,
                        onRetry = { viewModel.retryTask(task.id) },
                        onRemove = { viewModel.removeTaskFromQueue(task.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Top App Bar with back button, live telemetry badges, and quick controls
 */
@Composable
private fun QueueStatusTopBar(
    isPaused: Boolean,
    activeCount: Int,
    pendingCount: Int,
    failedCount: Int,
    onNavigateBack: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("queue_screen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Downloader",
                            tint = Slate800
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Queue Dashboard",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            failedCount > 0 -> RoseContainer
                                            isPaused -> AmberContainer
                                            activeCount > 0 -> BlueContainer
                                            else -> EmeraldContainer
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when {
                                        failedCount > 0 -> "ATTENTION"
                                        isPaused -> "PAUSED"
                                        activeCount > 0 -> "ACTIVE"
                                        else -> "STANDBY"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            failedCount > 0 -> RoseError
                                            isPaused -> AmberDark
                                            activeCount > 0 -> BlueAccent
                                            else -> EmeraldDark
                                        }
                                    )
                                )
                            }
                        }
                        Text(
                            text = "WorkManager রিয়েল-টাইম প্রোগ্রেস মনিটর",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate500,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filter Chips row for interactive dashboard filtering
 */
@Composable
private fun DashboardFilterRow(
    selectedFilter: QueueDashboardFilter,
    totalCount: Int,
    activeCount: Int,
    successCount: Int,
    pendingCount: Int,
    failedCount: Int,
    onSelectFilter: (QueueDashboardFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DashboardChip(
            label = "All ($totalCount)",
            selected = selectedFilter == QueueDashboardFilter.ALL,
            icon = Icons.Default.Layers,
            selectedColor = Slate900,
            onClick = { onSelectFilter(QueueDashboardFilter.ALL) },
            testTag = "filter_chip_all"
        )

        DashboardChip(
            label = "Active ($activeCount)",
            selected = selectedFilter == QueueDashboardFilter.ACTIVE,
            icon = Icons.Default.Sync,
            selectedColor = BlueAccent,
            onClick = { onSelectFilter(QueueDashboardFilter.ACTIVE) },
            testTag = "filter_chip_active"
        )

        DashboardChip(
            label = "Success ($successCount)",
            selected = selectedFilter == QueueDashboardFilter.SUCCESS,
            icon = Icons.Default.CheckCircle,
            selectedColor = EmeraldPrimary,
            onClick = { onSelectFilter(QueueDashboardFilter.SUCCESS) },
            testTag = "filter_chip_success"
        )

        DashboardChip(
            label = "Pending ($pendingCount)",
            selected = selectedFilter == QueueDashboardFilter.PENDING,
            icon = Icons.Default.HourglassEmpty,
            selectedColor = AmberDark,
            onClick = { onSelectFilter(QueueDashboardFilter.PENDING) },
            testTag = "filter_chip_pending"
        )

        if (failedCount > 0) {
            DashboardChip(
                label = "Failed ($failedCount)",
                selected = selectedFilter == QueueDashboardFilter.FAILED,
                icon = Icons.Default.Error,
                selectedColor = RoseError,
                onClick = { onSelectFilter(QueueDashboardFilter.FAILED) },
                testTag = "filter_chip_failed"
            )
        }
    }
}

@Composable
private fun DashboardChip(
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
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
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
            selectedContainerColor = selectedColor.copy(alpha = 0.12f),
            selectedLabelColor = selectedColor,
            containerColor = Color.White,
            labelColor = Slate700
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag(testTag)
    )
}

/**
 * Hero Card summarizing the WorkManager metrics, progress bars, and background engine
 */
@Composable
private fun QueueStatusHeroCard(
    isPaused: Boolean,
    totalTasks: Int,
    completedCount: Int,
    activeCount: Int,
    pendingCount: Int,
    failedCount: Int,
    overallProgress: Int,
    currentBatch: Int,
    totalBatches: Int,
    targetTotalMouzas: Int,
    isWorkManagerActive: Boolean,
    useWorkManagerService: Boolean,
    antiBlockingDescription: String,
    onToggleWorkManager: (Boolean) -> Unit,
    onRetryAllFailed: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("queue_hero_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Status + Batch Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    failedCount > 0 -> RoseError
                                    isPaused -> AmberAccent
                                    activeCount > 0 -> EmeraldLight
                                    else -> Slate400
                                }
                            )
                    )
                    Text(
                        text = when {
                            failedCount > 0 -> "Attention Needed ($failedCount Failed)"
                            isPaused -> "Queue Paused (স্থগিত)"
                            activeCount > 0 -> "Processing Batch #$currentBatch (সক্রিয়)"
                            totalTasks == 0 -> "Queue Ready (প্রস্তুত)"
                            else -> "Batch Pipeline Idle"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate900
                        )
                    )
                }

                if (totalBatches > 0) {
                    Text(
                        text = "Batch $currentBatch of $totalBatches",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Overall Progress Bar Section with Live Telemetry
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Overall Queue Progress",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate700
                        )
                        Text(
                            text = "($completedCount/$totalTasks Completed)",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                        )
                    }
                    Text(
                        text = "$overallProgress%",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (overallProgress == 100) EmeraldDark else EmeraldPrimary
                        )
                    )
                }

                LinearProgressIndicator(
                    progress = { overallProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .testTag("overall_queue_progress_bar"),
                    color = when {
                        failedCount > 0 -> AmberAccent
                        overallProgress == 100 -> EmeraldDark
                        else -> EmeraldPrimary
                    },
                    trackColor = Slate200
                )
            }

            // Metric Summary Pills with Icons (Total, Success, Active, Queued, Failed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricSummaryCard(
                    title = "Total",
                    count = totalTasks,
                    icon = Icons.Default.Layers,
                    bgColor = Slate100,
                    textColor = Slate800,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Success",
                    count = completedCount,
                    icon = Icons.Default.CheckCircle,
                    bgColor = EmeraldContainer,
                    textColor = EmeraldDark,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Active",
                    count = activeCount,
                    icon = Icons.Default.Sync,
                    bgColor = BlueContainer,
                    textColor = BlueAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Queued",
                    count = pendingCount,
                    icon = Icons.Default.HourglassEmpty,
                    bgColor = AmberContainer,
                    textColor = AmberDark,
                    modifier = Modifier.weight(1f)
                )
                if (failedCount > 0) {
                    MetricSummaryCard(
                        title = "Failed",
                        count = failedCount,
                        icon = Icons.Default.Error,
                        bgColor = RoseContainer,
                        textColor = RoseError,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Retry All Failed Alert (visible only when tasks failed)
            AnimatedVisibility(visible = failedCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoseContainer.copy(alpha = 0.5f))
                        .border(1.dp, RoseError.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
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
                        Text(
                            text = "$failedCount টি রেকর্ড সাময়িক ত্রুটিতে পড়েছে।",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                    }

                    Button(
                        onClick = onRetryAllFailed,
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry All", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            HorizontalDivider(color = Slate200, thickness = 1.dp)

            // WorkManager Background Service & Anti-Blocking Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isWorkManagerActive) EmeraldContainer.copy(alpha = 0.4f) else Slate100)
                    .border(1.dp, if (isWorkManagerActive) EmeraldPrimary.copy(alpha = 0.4f) else Slate200, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isWorkManagerActive) EmeraldPrimary else Slate500,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "WorkManager Background Engine",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isWorkManagerActive) EmeraldDark else Slate800
                        )
                        Text(
                            text = if (isWorkManagerActive) "Active (dataSync Foreground) • $antiBlockingDescription" else "Standby (Anti-Blocking Ready)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = if (isWorkManagerActive) EmeraldPrimary else Slate500,
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
 * Metric card pill with icon and counter
 */
@Composable
private fun MetricSummaryCard(
    title: String,
    count: Int,
    icon: ImageVector,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            ),
            color = textColor.copy(alpha = 0.8f)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = textColor
        )
    }
}

/**
 * Primary Real-Time WorkManager Queue Item Dashboard Card
 * Features progress bars, live telemetry, and prominent success/failure/running icons.
 */
@Composable
private fun QueueItemDashboardCard(
    task: DownloadTask,
    queueIndex: Int,
    onRetry: () -> Unit,
    onRemove: () -> Unit
) {
    val isSuccess = task.status == TaskStatus.COMPLETED
    val isRunning = task.status == TaskStatus.DOWNLOADING
    val isFailed = task.status == TaskStatus.FAILED
    val isPending = task.status == TaskStatus.PENDING

    val cardBorderColor = when {
        isSuccess -> EmeraldPrimary.copy(alpha = 0.4f)
        isRunning -> BlueAccent.copy(alpha = 0.6f)
        isFailed -> RoseError.copy(alpha = 0.6f)
        else -> Slate200
    }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                isSuccess -> Color.White
                isRunning -> BlueContainer.copy(alpha = 0.15f)
                isFailed -> RoseContainer.copy(alpha = 0.15f)
                else -> Color.White
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
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
            // Header Row: Status Icon, Mouza Titles, and Actions
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
                    // Prominent Status Icon Badge (Success / Failure / Running / Pending)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSuccess -> EmeraldContainer
                                    isRunning -> BlueContainer
                                    isFailed -> RoseContainer
                                    else -> AmberContainer.copy(alpha = 0.7f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isSuccess -> Icons.Default.CheckCircle
                                isRunning -> Icons.Default.Sync
                                isFailed -> Icons.Default.Error
                                else -> Icons.Default.HourglassTop
                            },
                            contentDescription = task.status.name,
                            tint = when {
                                isSuccess -> EmeraldPrimary
                                isRunning -> BlueAccent
                                isFailed -> RoseError
                                else -> AmberDark
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Mouza & Survey Information
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = task.mouzaNameBn,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Slate900
                            )
                            if (task.referenceId != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BlueContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Ref #${task.referenceId}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            color = BlueAccent
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "(${task.mouzaNameEn})",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Slate500
                            )
                        }

                        Text(
                            text = "${task.districtNameBn} ➔ ${task.upazilaNameBn} • J.L. No. ${task.jlNo}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        )
                    }
                }

                // Status Pill / Quick Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when {
                        isSuccess -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "100% DONE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldDark,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                        isRunning -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BlueContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "${task.progress}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BlueAccent,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                        isFailed -> {
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Retry", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            Text(
                                text = "Queue #$queueIndex",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AmberDark,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberContainer.copy(alpha = 0.8f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (!isRunning) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove from Queue",
                                tint = Slate400,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Real-Time LinearProgressIndicator with Custom State Coloring
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
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

                // Stats under Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isSuccess -> "✅ ${task.totalKhatians} / ${task.totalKhatians} Khatians Verified"
                            isRunning -> "📥 ${task.downloadedKhatians} / ${task.totalKhatians} Khatians"
                            isFailed -> "❌ Download Interrupted"
                            else -> "⏳ ${task.totalKhatians} Khatians Queued"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSuccess -> EmeraldDark
                                isRunning -> Slate700
                                isFailed -> RoseError
                                else -> Slate500
                            }
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isRunning) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${task.speedKbps} KB/s",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        } else {
                            Text(
                                text = String.format("%.2f MB", task.fileSizeBytes / (1024.0 * 1024.0)),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Slate500,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            // Bottom Badges: Document types + Batch / Storage Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Types:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Slate500
                    )
                    Text(
                        text = task.recordTypeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = EmeraldDark
                        ),
                        modifier = Modifier
                            .background(EmeraldContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.FolderSpecial else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isSuccess) AmberDark else EmeraldPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = if (isSuccess) "Saved to Vault" else "Batch #${task.batchNumber} • Anti-blocking",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = if (isSuccess) AmberDark else EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

/**
 * Empty state card customized for each filter tab
 */
@Composable
private fun EmptyFilterStateCard(
    filter: QueueDashboardFilter,
    isPaused: Boolean,
    onResetFilter: () -> Unit,
    onNavigateBack: () -> Unit,
    onResume: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate100),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when (filter) {
                    QueueDashboardFilter.ALL -> Icons.Default.CloudDownload
                    QueueDashboardFilter.ACTIVE -> if (isPaused) Icons.Default.Pause else Icons.Default.Sync
                    QueueDashboardFilter.SUCCESS -> Icons.Default.CheckCircle
                    QueueDashboardFilter.PENDING -> Icons.Default.HourglassEmpty
                    QueueDashboardFilter.FAILED -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = when (filter) {
                    QueueDashboardFilter.FAILED -> EmeraldPrimary
                    QueueDashboardFilter.SUCCESS -> EmeraldPrimary
                    else -> Slate400
                },
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (filter) {
                    QueueDashboardFilter.ALL -> "Queue is Empty"
                    QueueDashboardFilter.ACTIVE -> if (isPaused) "Queue is currently Paused (স্থগিত)" else "No Downloads Currently Active"
                    QueueDashboardFilter.SUCCESS -> "No Completed Tasks Yet"
                    QueueDashboardFilter.PENDING -> "No Pending Tasks Waiting"
                    QueueDashboardFilter.FAILED -> "Zero Failures! All Downloads Healthy 🎉"
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Slate700
            )
            Text(
                text = when (filter) {
                    QueueDashboardFilter.ALL -> "Select Mouzas from the Downloader to start queuing"
                    QueueDashboardFilter.ACTIVE -> if (isPaused) "Tap Resume to continue processing tasks" else "Pending tasks automatically begin according to concurrency limits"
                    QueueDashboardFilter.SUCCESS -> "Items will appear here with success badges once verified"
                    QueueDashboardFilter.PENDING -> "All current tasks are either active or finished"
                    QueueDashboardFilter.FAILED -> "WorkManager anti-blocking jitter is keeping connection stable"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Slate500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (filter != QueueDashboardFilter.ALL) {
                    OutlinedButton(
                        onClick = onResetFilter,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Show All Items", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (isPaused && filter == QueueDashboardFilter.ACTIVE) {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume Queue", style = MaterialTheme.typography.labelSmall)
                    }
                } else if (filter == QueueDashboardFilter.ALL || filter == QueueDashboardFilter.PENDING) {
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Select Mouzas", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

/**
 * Section header row with count badge
 */
@Composable
private fun SectionHeaderRow(
    title: String,
    badgeCount: Int,
    badgeColor: Color,
    badgeBg: Color,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Slate800
            )
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(badgeBg)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$badgeCount",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor
                )
            )
        }
    }
}

/**
 * Persistent bottom control bar with actions
 */
@Composable
private fun QueueStickyBottomBar(
    isPaused: Boolean,
    totalTasks: Int,
    pendingTasks: Int,
    onTogglePause: () -> Unit,
    onClearQueue: () -> Unit,
    onReturnToSelection: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Return to Downloader Button
            OutlinedButton(
                onClick = onReturnToSelection,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                modifier = Modifier
                    .weight(0.9f)
                    .testTag("queue_screen_bottom_back_btn"),
                contentPadding = PaddingValues(vertical = 12.dp)
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

            // Pause / Resume Button
            Button(
                onClick = onTogglePause,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) EmeraldPrimary else AmberDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("queue_screen_bottom_pause_resume_btn"),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPaused) "Resume" else "Pause",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Clear Queue Button
            OutlinedButton(
                onClick = onClearQueue,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                modifier = Modifier
                    .weight(0.9f)
                    .testTag("queue_screen_bottom_clear_btn"),
                contentPadding = PaddingValues(vertical = 12.dp)
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
