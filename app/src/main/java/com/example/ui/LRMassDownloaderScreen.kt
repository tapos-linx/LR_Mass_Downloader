package com.example.ui

import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Image
import com.example.data.model.DownloadFormat
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.District
import com.example.data.model.DownloadTask
import com.example.data.model.MasterArchive
import com.example.data.model.Mouza
import com.example.data.model.RecordType
import com.example.data.model.SavedRecord
import com.example.data.model.TaskStatus
import com.example.data.model.Upazila
import com.example.data.model.UpazilaFetchInfo
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberDark
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.io.File

@Composable
fun LRMassDownloaderScreen(
    viewModel: LRDownloaderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val savedRecords by viewModel.savedRecords.collectAsState()

    val availableMouzas = viewModel.currentMouzas
    val filteredMouzas = remember(availableMouzas, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            availableMouzas
        } else {
            val q = uiState.searchQuery.lowercase().trim()
            availableMouzas.filter {
                it.nameBn.lowercase().contains(q) ||
                        it.nameEn.lowercase().contains(q) ||
                        it.jlNo.contains(q)
            }
        }
    }

    val totalTasks = uiState.queue.size
    val completedTasks = uiState.queue.count { it.status == TaskStatus.COMPLETED }
    val downloadingTasks = uiState.queue.count { it.status == TaskStatus.DOWNLOADING }
    val pendingTasks = uiState.queue.count { it.status == TaskStatus.PENDING }
    val overallProgress = if (totalTasks == 0) 0 else {
        uiState.queue.sumOf { it.progress } / totalTasks
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.downloadMasterFileToPhone(context)
        } else {
            Toast.makeText(context, "Storage permission is required to save records to Downloads directory.", Toast.LENGTH_LONG).show()
        }
    }

    // Storage Access Framework (SAF) document creator allowing user to save copy anywhere
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.exportArchiveToSafUri(context, uri)
        }
    }

    val activeAndPendingCount = downloadingTasks + pendingTasks

    when (uiState.currentScreen) {
        AppScreen.QUEUE_STATUS -> {
            QueueStatusScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navigateToScreen(AppScreen.DOWNLOADER) },
                modifier = modifier
            )
        }
        AppScreen.KHOTIYAN_FORM -> {
            KhotiyanFormScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.navigateToScreen(AppScreen.DOWNLOADER) },
                modifier = modifier
            )
        }
        else -> {
            Scaffold(
                modifier = modifier
                    .fillMaxSize()
                    .background(Slate50),
                topBar = {
                    GovTechHeader(
                        onOpenApkInfo = { viewModel.setShowApkInfoDialog(true) }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 4.dp
                    ) {
                        NavigationBarItem(
                            selected = true,
                            onClick = { viewModel.navigateToScreen(AppScreen.DOWNLOADER) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Downloader"
                                )
                            },
                            label = { Text("Downloader", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_item_downloader")
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = { viewModel.navigateToScreen(AppScreen.QUEUE_STATUS) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (activeAndPendingCount > 0) {
                                            Badge(containerColor = if (downloadingTasks > 0) BlueAccent else AmberDark) {
                                                Text("$activeAndPendingCount")
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
                            label = { Text("Queue Status") },
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
            ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Notification Banner for Cache Clearance
            uiState.cacheNotificationMessage?.let { msg ->
                item {
                    CacheNotificationBanner(
                        message = msg,
                        onDismiss = { viewModel.dismissCacheNotification() }
                    )
                }
            }

            // Upazila Fetch Info Banner
            uiState.upazilaFetchInfo?.let { fetchInfo ->
                item {
                    UpazilaFetchCard(
                        fetchInfo = fetchInfo,
                        downloadedCount = uiState.downloadedMouzaIds.size,
                        currentBatch = uiState.currentBatchNumber
                    )
                }
            }

            // Master Archive Download Card
            uiState.masterArchive?.let { archive ->
                item {
                    MasterArchiveCard(
                        archive = archive,
                        onDownloadToPhone = {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else {
                                viewModel.downloadMasterFileToPhone(context)
                            }
                        },
                        onOpenDownloadsFolder = {
                            viewModel.openDownloadsFolder(context)
                        },
                        onSaveCopyAs = {
                            createDocumentLauncher.launch("${archive.districtNameEn}_${archive.upazilaNameEn}_Master_Land_Records.txt")
                        },
                        onShareFile = {
                            shareMasterFile(context, archive)
                        },
                        onOpenLedger = {
                            viewModel.setShowLedgerDialog(true)
                        }
                    )
                }
            }

            // 1. Cascading State Filter Card with "Select All Documents" button
            item {
                CascadingHierarchyCard(
                    allDistricts = viewModel.allDistricts,
                    selectedDistrictId = uiState.selectedDistrictId,
                    onDistrictSelected = { viewModel.onDistrictChanged(it) },
                    upazilas = viewModel.currentUpazilas,
                    selectedUpazilaId = uiState.selectedUpazilaId,
                    onUpazilaSelected = { viewModel.onUpazilaChanged(it) },
                    selectedRecordTypes = uiState.selectedRecordTypes,
                    onToggleRecordType = { viewModel.toggleRecordType(it) },
                    onSelectAllRecordTypes = { viewModel.selectAllRecordTypes() },
                    selectedDownloadFormat = uiState.selectedDownloadFormat,
                    onDownloadFormatSelected = { viewModel.setDownloadFormat(it) }
                )
            }

            // 2. Mouza Registry & 7-Mouza Sequential Batch Selection Card
            item {
                MouzaRegistryCard(
                    currentUpazilaName = viewModel.currentUpazila?.nameBn,
                    selectedCount = uiState.selectedMouzaIds.size,
                    totalCount = uiState.targetTotalMouzas.coerceAtLeast(availableMouzas.size),
                    downloadedCount = uiState.downloadedMouzaIds.size,
                    currentBatchNumber = uiState.currentBatchNumber,
                    totalBatches = viewModel.totalBatches,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onSelectAll = { viewModel.selectAllMouzas() },
                    onSelectNextSeven = { viewModel.selectNextSevenMouzas() },
                    onDeselectAll = { viewModel.deselectAllMouzas() },
                    mouzas = filteredMouzas,
                    selectedMouzaIds = uiState.selectedMouzaIds,
                    downloadedMouzaIds = uiState.downloadedMouzaIds,
                    onToggleMouza = { viewModel.toggleMouza(it) },
                    isUpazilaSelected = uiState.selectedUpazilaId != null,
                    onQueueSelected = { viewModel.queueSelectedDownloads() }
                )
            }

            // 2.5 WorkManager Sequential Reference IDs Input Queue Card
            item {
                BatchReferenceIdsInputCard(
                    currentDistrict = viewModel.currentDistrict,
                    currentUpazila = viewModel.currentUpazila,
                    currentMouza = viewModel.currentMouzas.find { it.id in uiState.selectedMouzaIds } ?: viewModel.currentMouzas.firstOrNull(),
                    availableMouzas = viewModel.currentMouzas,
                    onSelectMouza = { viewModel.toggleMouza(it) },
                    selectedRecordTypes = uiState.selectedRecordTypes,
                    onToggleRecordType = { viewModel.toggleRecordType(it) },
                    onQueueReferenceIds = { dist, upaz, mouza, ids, types ->
                        viewModel.queueReferenceIdsDownload(dist, upaz, mouza, ids, types)
                    },
                    onOpenQueueStatus = { viewModel.navigateToScreen(AppScreen.QUEUE_STATUS) }
                )
            }

            // 3. Concurrency-Limited Queue HUD Card with Anti-Freeze
            item {
                QueueHudCard(
                    totalTasks = totalTasks,
                    completedTasks = completedTasks,
                    downloadingTasks = downloadingTasks,
                    pendingTasks = pendingTasks,
                    overallProgress = overallProgress,
                    currentBatch = uiState.currentBatchNumber,
                    totalBatches = viewModel.totalBatches,
                    targetTotalMouzas = uiState.targetTotalMouzas,
                    downloadedMouzasCount = uiState.downloadedMouzaIds.size,
                    isPaused = uiState.isPaused,
                    isWorkManagerActive = uiState.isWorkManagerActive,
                    useWorkManagerService = uiState.useWorkManagerBackgroundService,
                    onToggleWorkManagerService = { viewModel.setUseWorkManagerBackgroundService(it) },
                    antiBlockingDescription = uiState.antiBlockingDelayDescription,
                    onTogglePause = { viewModel.togglePauseQueue() },
                    onClearQueue = { viewModel.clearQueue() },
                    tasks = uiState.queue,
                    onOpenFullQueueScreen = { viewModel.navigateToScreen(AppScreen.QUEUE_STATUS) }
                )
            }

            // Footer notes
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "গণপ্রজাতন্ত্রী বাংলাদেশ সরকার • ভূমি রেকর্ড ও জরিপ অধিদপ্তর (DLR&S)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Direct Public Storage Downloader • MediaScanner Indexed",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
}

    // Saved Records Vault Dialog
    if (uiState.isShowingHistoryDialog) {
        SavedRecordsDialog(
            records = savedRecords,
            onDismiss = { viewModel.setShowHistoryDialog(false) },
            onDelete = { viewModel.deleteSavedRecord(it) },
            onClearAll = { viewModel.clearAllSavedRecords() }
        )
    }

    // Master Ledger In-App Viewer Dialog
    if (uiState.isShowingLedgerDialog && uiState.masterArchive != null) {
        MasterLedgerDialog(
            archive = uiState.masterArchive!!,
            onDismiss = { viewModel.setShowLedgerDialog(false) },
            onShare = { shareMasterFile(context, uiState.masterArchive!!) }
        )
    }

    // Master Archive Saved Confirmation Dialog
    if (uiState.isShowingSaveSuccessDialog && uiState.masterArchive != null) {
        SaveSuccessDialog(
            archive = uiState.masterArchive!!,
            onDismiss = { viewModel.dismissSaveSuccessDialog() },
            onOpenFolder = { viewModel.openDownloadsFolder(context) },
            onSaveCopyAs = {
                createDocumentLauncher.launch("${uiState.masterArchive!!.districtNameEn}_${uiState.masterArchive!!.upazilaNameEn}_Master_Land_Records.txt")
            },
            onShare = { shareMasterFile(context, uiState.masterArchive!!) },
            onViewLedger = {
                viewModel.dismissSaveSuccessDialog()
                viewModel.setShowLedgerDialog(true)
            }
        )
    }

    // Direct 1-Click APK Download Dialog
    if (uiState.isShowingApkInfoDialog) {
        ApkDownloadDialog(
            onDismiss = { viewModel.setShowApkInfoDialog(false) }
        )
    }
}

fun shareMasterFile(context: Context, archive: MasterArchive) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Master Land Records: ${archive.upazilaNameBn} (${archive.districtNameBn})")
            val textBody = archive.fileContentPreview ?: "Master Land Records for ${archive.upazilaNameBn} (${archive.districtNameBn})"
            putExtra(Intent.EXTRA_TEXT, textBody)

            val shareFile = try {
                val direct = archive.phoneSavedPath?.let { File(it) }
                if (direct != null && direct.exists()) {
                    direct
                } else {
                    val shareDir = File(context.cacheDir, "shared_records").apply { mkdirs() }
                    val temp = File(shareDir, "${archive.districtNameEn}_${archive.upazilaNameEn}_Master_Land_Records.txt")
                    temp.writeText(textBody, Charsets.UTF_8)
                    temp
                }
            } catch (e: Exception) {
                null
            }

            if (shareFile != null && shareFile.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(sendIntent, "Share Master Land Records")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open share sheet: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun UpazilaFetchCard(
    fetchInfo: UpazilaFetchInfo,
    downloadedCount: Int,
    currentBatch: Int
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(EmeraldLight)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(EmeraldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = EmeraldDark,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "উপজেলা জরিপ ইনডেক্স যাচাই",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    )
                    Text(
                        text = "Verified Registry",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        ),
                        modifier = Modifier
                            .background(EmeraldContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = "${fetchInfo.upazilaNameBn} উপজেলায় মোট ${fetchInfo.totalMouzaCount}টি মৌজা শনাক্ত হয়েছে",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Slate900
                )
                Text(
                    text = "টার্গেট: ৭টি করে ক্রমিক মোট ${fetchInfo.batchCount}টি ব্যাচে ${fetchInfo.totalMouzaCount}টি মৌজার খতিয়ান সংগ্রহ হবে (বর্তমানে ব্যাচ #$currentBatch, সম্পন্ন: $downloadedCount/${fetchInfo.totalMouzaCount})",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Slate600
                )
            }
        }
    }
}

@Composable
fun CacheNotificationBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(EmeraldPrimary)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = EmeraldDark,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldDark
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Check, contentDescription = "Dismiss", tint = EmeraldDark)
            }
        }
    }
}

@Composable
fun MasterArchiveCard(
    archive: MasterArchive,
    onDownloadToPhone: () -> Unit,
    onOpenDownloadsFolder: () -> Unit,
    onSaveCopyAs: () -> Unit,
    onShareFile: () -> Unit,
    onOpenLedger: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AmberAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "মাস্টার ফাইল প্রস্তুত (Master Archive Ready)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                        Text(
                            text = "${archive.districtNameBn} • ${archive.upazilaNameBn} সকল মৌজা সম্বলিত",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = EmeraldContainer
                        )
                    }
                }

                Text(
                    text = "100% COMPLETE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = AmberAccent,
                    modifier = Modifier
                        .background(EmeraldDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Metrics summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("মোট মৌজা", fontSize = 10.sp, color = EmeraldContainer)
                    Text("${archive.totalMouzasCount} টি", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("খতিয়ান সংখ্যা", fontSize = 10.sp, color = EmeraldContainer)
                    Text("${archive.totalKhatiansCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("রেকর্ড ধরন", fontSize = 10.sp, color = EmeraldContainer)
                    Text(archive.recordTypesIncluded, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("আকার", fontSize = 10.sp, color = EmeraldContainer)
                    Text(archive.totalSizeMb, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Physical path confirmation box
            if (archive.isDownloadedToPhone) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldContainer)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.DownloadDone, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(20.dp))
                        Text(
                            text = "ফোন মেমোরিতে সংরক্ষিত ফোল্ডার:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = EmeraldDark)
                        )
                    }
                    Text(
                        text = "📁 Internal Storage > Download > LandRecords /",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold),
                        color = Slate900
                    )
                    Text(
                        text = "আপনার ফোনের 'Files' বা 'My Files' অ্যাপ ওপেন করে 'Downloads' ফোল্ডারের ভেতর 'LandRecords' সাব-ফোল্ডারে মাস্টার আর্কাইভ ও সকল মৌজা ফাইল পাবেন।",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = EmeraldDark
                    )

                    // Badges for created file formats
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("JSON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark, modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                        Text("TXT Ledger", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark, modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                        Text("CSV Table", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark, modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                        Text("${archive.totalMouzasCount} Mouzas", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDark, modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // One-Click Master Download Button
                Button(
                    onClick = onDownloadToPhone,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberAccent,
                        contentColor = Slate900
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("one_click_master_download_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GetApp,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (archive.isDownloadedToPhone) "Re-Save to Download/LandRecords (Auto-Clears Cache)" else "One-Click Save to Phone Downloads (Auto-Clears Cache)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }

                // If downloaded, offer direct Open Folder, Save Copy As, Share, and View Ledger
                if (archive.isDownloadedToPhone) {
                    // Open Downloads Folder Primary Action
                    Button(
                        onClick = onOpenDownloadsFolder,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldLight,
                            contentColor = Slate900
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_downloads_folder_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Download/LandRecords Folder", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSaveCopyAs,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.15f)
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(EmeraldLight)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_copy_as_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save As (SAF)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onShareFile,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.15f)
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(EmeraldLight)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("share_master_file_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onOpenLedger,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldContainer,
                            contentColor = EmeraldDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("view_master_ledger_button")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Ledger in App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SaveSuccessDialog(
    archive: MasterArchive,
    onDismiss: () -> Unit,
    onOpenFolder: () -> Unit,
    onSaveCopyAs: () -> Unit,
    onShare: () -> Unit,
    onViewLedger: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "ফাইল সংরক্ষিত হয়েছে!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Slate900
                    )
                    Text(
                        text = "Phone Memory > Download > LandRecords",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = EmeraldDark
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "মাস্টার আর্কাইভ এবং ${archive.totalMouzasCount}টি মৌজার খতিয়ান ফাইল আপনার ফোনের মূল Download ফোল্ডারে সফলভাবে সংরক্ষণ করা হয়েছে।",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Slate800, fontSize = 13.sp)
                )

                // Location Box
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.7f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(EmeraldPrimary)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "📁 ফোল্ডার লোকেশন (Phone Storage):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldDark)
                        )
                        Text(
                            text = "Internal Storage > Download > LandRecords /",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900,
                                fontSize = 12.sp
                            )
                        )
                        Text(
                            text = "আপনার ফোনের 'Files' বা 'My Files' অ্যাপ ওপেন করে 'Downloads' ফোল্ডারের ভেতর 'LandRecords' ফোল্ডারে ফাইলগুলো পাবেন।",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = EmeraldDark)
                        )
                    }
                }

                // File List
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "সংরক্ষিত ফাইলসমূহ (${archive.totalMouzasCount}টি মৌজা):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate700)
                    )
                    Text("• ${archive.fileName} (Master JSON)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Slate800)
                    Text("• ${archive.fileName.replace(".json", ".txt")} (Master TXT Ledger)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Slate800)
                    Text("• ${archive.fileName.replace(".json", ".csv")} (Master CSV Table)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Slate800)
                    Text("• সকল ${archive.totalMouzasCount}টি মৌজার স্বতন্ত্র JSON ও TXT ডাটা", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                }

                Text(
                    text = "নোট: মেমোরি সুরক্ষার জন্য ক্যাশ স্বয়ংক্রিয়ভাবে খালি করা হয়েছে।",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Slate500)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenFolder,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Downloads Folder")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun MasterLedgerDialog(
    archive: MasterArchive,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = EmeraldPrimary)
                    Text("Master Land Records Ledger", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = EmeraldPrimary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Saved Path: ${archive.phoneSavedPath ?: "Downloads"}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EmeraldDark),
                    modifier = Modifier
                        .background(EmeraldContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate100)
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = archive.fileContentPreview ?: "Loading ledger preview...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                ),
                                color = Slate800
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun GovTechHeader(
    savedRecordsCount: Int = 0,
    queueCount: Int = 0,
    onOpenKhotiyanForm: () -> Unit = {},
    onOpenQueueStatus: () -> Unit = {},
    onOpenSavedRecords: () -> Unit = {},
    onOpenApkInfo: () -> Unit
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Surface(
        color = EmeraldPrimary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldDark)
                            .border(1.dp, EmeraldLight.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Land Records Seal",
                            tint = AmberAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "GovTech BD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = EmeraldContainer,
                                modifier = Modifier
                                    .background(EmeraldDark, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "ভূমি রেকর্ড ও জরিপ",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = EmeraldContainer.copy(alpha = 0.9f)
                            )
                        }
                        Text(
                            text = "LR Mass Downloader",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // APK Releases & Direct Installer Button
                    OutlinedButton(
                        onClick = onOpenApkInfo,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            containerColor = EmeraldDark.copy(alpha = 0.7f)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(AmberAccent.copy(alpha = 0.7f))
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .testTag("download_apk_button")
                            .testTag("open_apk_download_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download APK",
                            modifier = Modifier.size(16.dp),
                            tint = AmberAccent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Download APK",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApkDownloadDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var customRepo by remember { mutableStateOf("tapos-linx/LR_Mass_Downloader") }
    var isEditingRepo by remember { mutableStateOf(false) }

    // Official GitHub Releases direct asset download pattern
    val downloadUrl = "https://github.com/$customRepo/releases/latest/download/LR%20Mass%20Downloader.apk"
    val directUrlNoSpaces = "https://github.com/$customRepo/releases/latest/download/LRMassDownloader.apk"
    val actionsUrl = "https://github.com/$customRepo/actions"
    val releasesUrl = "https://github.com/$customRepo/releases"

    val currentVersion = try {
        "${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})"
    } catch (e: Exception) {
        "1.0.3 (Build 103)"
    }

    // Helper: Find or copy current app's APK for direct installation/sharing
    fun getLocalApkFile(): File? {
        return try {
            val sourceDir = context.applicationInfo?.sourceDir
            if (sourceDir != null && File(sourceDir).exists()) {
                val srcFile = File(sourceDir)
                val targetFile = File(context.cacheDir, "LRMassDownloader-v$currentVersion.apk")
                if (!targetFile.exists() || targetFile.length() != srcFile.length()) {
                    srcFile.copyTo(targetFile, overwrite = true)
                }
                targetFile
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Helper: Trigger direct APK install on Android device
    fun triggerInstallLocalApk() {
        try {
            val apkFile = getLocalApkFile()
            if (apkFile != null && apkFile.exists()) {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(installIntent)
            } else {
                Toast.makeText(context, "APK ফাইল পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "ইনস্টলার চালু করতে ব্যর্থ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Helper: 1-Click direct download of APK to device's local storage (Downloads folder) with zero share sheet
    fun downloadApkToLocalStorage(): File? {
        return try {
            val apkFile = getLocalApkFile()
            val fileName = "LR Mass Downloader.apk"
            val altFileName = "LRMassDownloader.apk"
            val mimeType = "application/vnd.android.package-archive"
            var savedPath = ""

            if (apkFile != null && apkFile.exists()) {
                val apkBytes = apkFile.readBytes()

                // 1. Android 10+ (Q+) MediaStore Downloads API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri, "wt")?.use { out ->
                            out.write(apkBytes)
                            out.flush()
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                        savedPath = "/storage/emulated/0/Download/$fileName"
                    }
                }

                // 2. Direct File write to public storage (/sdcard/Download/)
                try {
                    val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!publicDownloads.exists()) publicDownloads.mkdirs()
                    val targetFile = File(publicDownloads, fileName)
                    targetFile.writeBytes(apkBytes)
                    savedPath = targetFile.absolutePath

                    // Also save alias "LRMassDownloader.apk" for convenience
                    val aliasFile = File(publicDownloads, altFileName)
                    aliasFile.writeBytes(apkBytes)

                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath, aliasFile.absolutePath),
                        arrayOf(mimeType, mimeType),
                        null
                    )
                } catch (ignored: Exception) {}

                // Fallback to app external files directory if public access restricted
                if (savedPath.isEmpty()) {
                    val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    if (fallbackDir != null) {
                        val fallbackFile = File(fallbackDir, fileName)
                        fallbackFile.writeBytes(apkBytes)
                        savedPath = fallbackFile.absolutePath
                    }
                }

                val sizeMb = String.format(Locale.US, "%.1f", apkFile.length() / (1024f * 1024f))
                Toast.makeText(
                    context,
                    "📥 APK ডাউনলোড সম্পন্ন! ফাইল: Download/$fileName (${sizeMb} MB)",
                    Toast.LENGTH_LONG
                ).show()

                File(savedPath.ifEmpty { apkFile.absolutePath })
            } else {
                // If local app source APK is not extracted, enqueue direct download via system DownloadManager into Downloads/
                try {
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val request = DownloadManager.Request(Uri.parse(downloadUrl))
                        .setTitle(fileName)
                        .setDescription("Downloading $fileName directly to Downloads...")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                        .setMimeType(mimeType)
                    dm.enqueue(request)
                    Toast.makeText(
                        context,
                        "📥 APK ডাউনলোড শুরু হয়েছে! ফাইল: Download/$fileName",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(browserIntent)
                }
                null
            }
        } catch (e: Exception) {
            Toast.makeText(context, "ডাউনলোড ব্যর্থ: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    // Helper: Open Downloads folder directly on device
    fun openDownloadsFolder() {
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Downloads ফোল্ডার: /sdcard/Download/", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Download & Install APK",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Slate900
                    )
                    Text(
                        text = "Active Build: v$currentVersion",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = EmeraldDark
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Instant Option: Direct Local APK 1-Click Download & Direct Install (Zero Share Sheet)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.5f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(EmeraldLight)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = null,
                                tint = EmeraldDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "📥 সরাসরি ১-ক্লিক লোকাল ডাউনলোড ও ইনস্টল:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldDark
                                )
                            )
                        }
                        Text(
                            text = "কোনো শেয়ার শিট (Gmail/Google Drive) ছাড়াই সরাসরি ডিভাইসের Downloads ফোল্ডারে APK সেভ করুন অথবা ১-ক্লিকে ইনস্টল করুন।",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Slate800)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { downloadApkToLocalStorage() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("download_apk_button")
                                    .testTag("direct_download_apk_btn"),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Downloads এ সেভ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { triggerInstallLocalApk() },
                                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("direct_install_apk_btn"),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("সরাসরি ইনস্টল", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // GitHub Releases Link Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Slate300)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌐 GitHub Releases ডাউনলোড লিংক:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate700)
                            )
                            TextButton(
                                onClick = { isEditingRepo = !isEditingRepo },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(if (isEditingRepo) "Done" else "Change Repo", fontSize = 10.sp, color = EmeraldDark)
                            }
                        }

                        if (isEditingRepo) {
                            OutlinedTextField(
                                value = customRepo,
                                onValueChange = { customRepo = it.trim() },
                                label = { Text("GitHub Repo (owner/repo)", fontSize = 10.sp) },
                                placeholder = { Text("tapos-linx/LR_Mass_Downloader", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                            )
                        } else {
                            Text(
                                text = downloadUrl,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = EmeraldDark,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // 404 Error Fix & Root Cause Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberAccent.copy(alpha = 0.12f)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AmberAccent.copy(alpha = 0.5f))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "💡 কেন 404 Error দেখায় এবং সমাধান:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = Slate900)
                        )
                        Text(
                            text = "১. রিপোজিটরি tapos-linx/LR_Mass_Downloader এ প্রথম পুশের পর GitHub Actions অটোমেটিক APK তৈরি করে (১-২ মিনিট লাগে)।\n২. লাইভ বিল্ড ও রিলিজ দেখতে নিচে 'Live Build Status' অথবা 'All Releases' বাটনে চাপ দিন।\n৩. কোনো ইন্টারনেট বা 404 অপেক্ষা ছাড়া ইনস্টল করতে উপরের 'সরাসরি ইনস্টল' ব্যবহার করুন!",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Slate700)
                        )
                    }
                }

                // Action Buttons inside Dialog: Copy & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("LR Mass Downloader APK Link", downloadUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "ডাউনলোড লিংক কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Copy Link", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = { openDownloadsFolder() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Open Downloads", fontSize = 10.sp)
                    }
                }

                // Check Actions & Releases Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(actionsUrl)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "ব্রাউজার খোলা যায়নি", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("Live Build Status", fontSize = 10.sp, color = EmeraldDark, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releasesUrl)).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "ব্রাউজার খোলা যায়নি", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("All Releases", fontSize = 10.sp, color = EmeraldDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val request = DownloadManager.Request(Uri.parse(downloadUrl))
                            .setTitle("LR Mass Downloader.apk")
                            .setDescription("Downloading LR Mass Downloader APK directly...")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "LR Mass Downloader.apk")
                            .setMimeType("application/vnd.android.package-archive")
                        dm.enqueue(request)
                        Toast.makeText(context, "📥 ডাউনলোড শুরু হয়েছে: Download/LR Mass Downloader.apk", Toast.LENGTH_SHORT).show()
                    } catch (ignored: Exception) {}

                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "ব্রাউজার ওপেন করা যায়নি", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("browser_direct_download_btn")
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("GitHub Releases সরাসরি ডাউনলোড")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CascadingHierarchyCard(
    allDistricts: List<com.example.data.model.District>,
    selectedDistrictId: String,
    onDistrictSelected: (String) -> Unit,
    upazilas: List<com.example.data.model.Upazila>,
    selectedUpazilaId: String?,
    onUpazilaSelected: (String?) -> Unit,
    selectedRecordTypes: Set<RecordType>,
    onToggleRecordType: (RecordType) -> Unit,
    onSelectAllRecordTypes: () -> Unit,
    selectedDownloadFormat: DownloadFormat,
    onDownloadFormatSelected: (DownloadFormat) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ফিল্টার ও স্তর নির্বাচন (Cascading Hierarchy)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate800
                    )
                }

                Text(
                    text = "Pure Cascade",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    ),
                    modifier = Modifier
                        .background(EmeraldContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // District & Upazila Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var districtExpanded by remember { mutableStateOf(false) }
                val currentDistrict = allDistricts.find { it.id == selectedDistrictId }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "১. জেলা (District)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate600,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Slate100)
                                .border(1.dp, Slate200, RoundedCornerShape(14.dp))
                                .clickable { districtExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                                .testTag("district_dropdown_trigger"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentDistrict?.let { "${it.nameBn} (${it.nameEn})" } ?: "জেলা নির্বাচন",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Slate800,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate600)
                        }

                        DropdownMenu(
                            expanded = districtExpanded,
                            onDismissRequest = { districtExpanded = false }
                        ) {
                            allDistricts.forEach { dist ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${dist.nameBn} (${dist.nameEn})",
                                            fontWeight = if (dist.id == selectedDistrictId) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onDistrictSelected(dist.id)
                                        districtExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                var upazilaExpanded by remember { mutableStateOf(false) }
                val currentUpazila = upazilas.find { it.id == selectedUpazilaId }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "২. উপজেলা (Upazila)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate600,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selectedUpazilaId != null) EmeraldContainer.copy(alpha = 0.4f) else Slate100)
                                .border(
                                    1.dp,
                                    if (selectedUpazilaId != null) EmeraldPrimary else Slate200,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { upazilaExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                                .testTag("upazila_dropdown_trigger"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentUpazila?.let { "${it.nameBn} (${it.nameEn})" } ?: "-- উপজেলা নির্বাচন --",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedUpazilaId != null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedUpazilaId != null) Slate900 else Slate400
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate600)
                        }

                        DropdownMenu(
                            expanded = upazilaExpanded,
                            onDismissRequest = { upazilaExpanded = false }
                        ) {
                            upazilas.forEach { upa ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "${upa.nameBn} (${upa.nameEn})",
                                                fontWeight = if (upa.id == selectedUpazilaId) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = "${upa.mouzas.size} টি মৌজা",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Slate400
                                            )
                                        }
                                    },
                                    onClick = {
                                        onUpazilaSelected(upa.id)
                                        upazilaExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Record Type Filter Chips + "Select All Documents" button
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "৩. জরিপ রেকর্ড ধরন (Document Types)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate600
                    )

                    Button(
                        onClick = onSelectAllRecordTypes,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRecordTypes.size == RecordType.values().size) EmeraldPrimary else Slate100,
                            contentColor = if (selectedRecordTypes.size == RecordType.values().size) Color.White else Slate700
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("select_all_document_types_button")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Select All Documents (CS+SA+RS+BRS)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecordType.values().forEach { type ->
                        val isSelected = selectedRecordTypes.contains(type)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) EmeraldPrimary else Slate100)
                                .border(
                                    1.dp,
                                    if (isSelected) EmeraldDark else Slate200,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onToggleRecordType(type) }
                                .padding(vertical = 10.dp, horizontal = 6.dp)
                                .testTag("record_type_${type.code.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        text = type.code,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (isSelected) Color.White else Slate800
                                    )
                                }
                                Text(
                                    text = type.bengaliName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isSelected) EmeraldContainer else Slate500,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 4. Download Format Selection (PDF or Image)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "৪. ডাউনলোড ফরম্যাট নির্বাচন (Download Format)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Slate600
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DownloadFormat.values().forEach { format ->
                        val isSelected = selectedDownloadFormat == format
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) BlueAccent else Slate100)
                                .border(
                                    1.dp,
                                    if (isSelected) BlueAccent.copy(alpha = 0.8f) else Slate200,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onDownloadFormatSelected(format) }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                                .testTag("download_format_${format.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (format == DownloadFormat.PDF) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Slate500,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = format.label,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (isSelected) Color.White else Slate700
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
fun MouzaRegistryCard(
    currentUpazilaName: String?,
    selectedCount: Int,
    totalCount: Int,
    downloadedCount: Int,
    currentBatchNumber: Int,
    totalBatches: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNextSeven: () -> Unit,
    onDeselectAll: () -> Unit,
    mouzas: List<Mouza>,
    selectedMouzaIds: Set<String>,
    downloadedMouzaIds: Set<String>,
    onToggleMouza: (String) -> Unit,
    isUpazilaSelected: Boolean,
    onQueueSelected: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "মৌজা নির্বাচন তালিকা",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Slate800
                        )
                        if (currentUpazilaName != null) {
                            Text(
                                text = "$currentUpazilaName ($totalCount মৌজা শনাক্ত)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                ),
                                modifier = Modifier
                                    .background(EmeraldContainer, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Batch $currentBatchNumber of $totalBatches • ৭টি করে ক্রমিক কিউ (নো রিপিট)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Slate500
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate100)
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (downloadedCount > 0) EmeraldPrimary else Slate400)
                        )
                        Text(
                            text = "Done: $downloadedCount / Target: $totalCount",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        )
                    }
                }
            }

            // Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelectNextSeven,
                    enabled = isUpazilaSelected && totalCount > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EmeraldPrimary,
                        containerColor = EmeraldContainer.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("select_first_7_button")
                ) {
                    Text("Next 7", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                    onClick = onSelectAll,
                    enabled = isUpazilaSelected && totalCount > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Slate700,
                        containerColor = Slate100
                    ),
                    modifier = Modifier.testTag("select_all_button")
                ) {
                    Text("All ($totalCount)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedButton(
                    onClick = onDeselectAll,
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Slate600,
                        containerColor = Slate100
                    ),
                    modifier = Modifier.testTag("deselect_all_button")
                ) {
                    Text("Clear", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("খুঁজুন...", fontSize = 12.sp, color = Slate400) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    enabled = isUpazilaSelected,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate100,
                        unfocusedContainerColor = Slate50,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Slate200
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("search_mouza_input")
                )
            }

            // Mouza list container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Slate50)
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                if (!isUpazilaSelected) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "উপজেলা নির্বাচন করুন",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate600
                        )
                        Text(
                            text = "উপজেলা নির্বাচন করলে প্রথমে মোট মৌজা সংখ্যা ফেচ হবে, তারপর প্রথম ৭টি কিউ হবে",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Slate400,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else if (mouzas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("কোনো মৌজা পাওয়া যায়নি", color = Slate400, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(mouzas, key = { it.id }) { mouza ->
                            val isChecked = selectedMouzaIds.contains(mouza.id)
                            val isDownloaded = downloadedMouzaIds.contains(mouza.id)
                            MouzaItemCard(
                                mouza = mouza,
                                isChecked = isChecked,
                                isDownloaded = isDownloaded,
                                onClick = { onToggleMouza(mouza.id) }
                            )
                        }
                    }
                }
            }

            // Primary Queue CTA
            Button(
                onClick = onQueueSelected,
                enabled = selectedCount > 0,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("queue_selected_downloads_button")
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Queue Batch (Next $selectedCount Mouzas - No Repeats)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun MouzaItemCard(
    mouza: Mouza,
    isChecked: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isDownloaded -> Slate100.copy(alpha = 0.8f)
                    isChecked -> EmeraldContainer.copy(alpha = 0.5f)
                    else -> Color.White
                }
            )
            .border(
                1.dp,
                when {
                    isDownloaded -> Slate300
                    isChecked -> EmeraldPrimary
                    else -> Slate200
                },
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("mouza_card_${mouza.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            isDownloaded -> EmeraldDark
                            isChecked -> EmeraldPrimary
                            else -> Slate100
                        }
                    )
                    .border(
                        1.5.dp,
                        when {
                            isDownloaded -> EmeraldDark
                            isChecked -> EmeraldDark
                            else -> Slate400
                        },
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDownloaded || isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Checked",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = mouza.nameBn,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDownloaded) Slate600 else Slate800
                    )
                    if (isDownloaded) {
                        Text(
                            text = "Downloaded",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark,
                            modifier = Modifier
                                .background(EmeraldContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = mouza.nameEn,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Slate400
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "J.L.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = Slate400
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Slate100)
                    .border(1.dp, Slate200, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = mouza.jlNo,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate800
                    )
                )
            }
        }
    }
}

val Slate300 = Color(0xFFCBD5E1)

/**
 * Dedicated Material 3 Card enabling users to input a list of land record reference IDs
 * and queue them for sequential background downloading via WorkManager.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchReferenceIdsInputCard(
    currentDistrict: District?,
    currentUpazila: Upazila?,
    currentMouza: Mouza?,
    availableMouzas: List<Mouza>,
    onSelectMouza: (String) -> Unit,
    selectedRecordTypes: Set<RecordType>,
    onToggleRecordType: (RecordType) -> Unit,
    onQueueReferenceIds: (District, Upazila, Mouza, List<String>, Set<RecordType>) -> Unit,
    onOpenQueueStatus: () -> Unit
) {
    var rawInputText by remember { mutableStateOf("") }
    var selectedMouzaOverrideId by remember(currentMouza) { mutableStateOf(currentMouza?.id) }
    var isMouzaDropdownOpen by remember { mutableStateOf(false) }

    val activeMouza = remember(selectedMouzaOverrideId, availableMouzas, currentMouza) {
        availableMouzas.find { it.id == selectedMouzaOverrideId } ?: currentMouza ?: availableMouzas.firstOrNull()
    }

    val parsedIds = remember(rawInputText) {
        LRDownloaderViewModel.parseReferenceIds(rawInputText)
    }

    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("batch_reference_ids_input_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Reference IDs Sequential Queue",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Slate900
                        )
                        Text(
                            text = "রেফারেন্স আইডি তালিকা ইনপুট ও WorkManager ব্যাকগ্রাউন্ড ডাউনলোড",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Slate600
                        )
                    }
                }

                if (parsedIds.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BlueContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${parsedIds.size} IDs",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = BlueAccent
                            )
                        )
                    }
                }
            }

            // Target Mouza Selector Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate100,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Target Mouza (টার্গেট মৌজা):",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Slate500)
                        )
                        Text(
                            text = if (activeMouza != null && currentDistrict != null && currentUpazila != null) {
                                "${currentDistrict.nameBn} > ${currentUpazila.nameBn} > ${activeMouza.nameBn} (JL ${activeMouza.jlNo})"
                            } else {
                                "উপজেলা থেকে মৌজা নির্বাচন করুন"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        )
                    }

                    if (availableMouzas.isNotEmpty()) {
                        Box {
                            OutlinedButton(
                                onClick = { isMouzaDropdownOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("মৌজা পরিবর্তন", fontSize = 11.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            DropdownMenu(
                                expanded = isMouzaDropdownOpen,
                                onDismissRequest = { isMouzaDropdownOpen = false }
                            ) {
                                availableMouzas.take(30).forEach { mouza ->
                                    DropdownMenuItem(
                                        text = { Text("${mouza.nameBn} (${mouza.nameEn}) • JL ${mouza.jlNo}", fontSize = 12.sp) },
                                        onClick = {
                                            selectedMouzaOverrideId = mouza.id
                                            onSelectMouza(mouza.id)
                                            isMouzaDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Text Area
            OutlinedTextField(
                value = rawInputText,
                onValueChange = { rawInputText = it },
                label = { Text("List of Reference IDs / খতিয়ান নম্বরসমূহ") },
                placeholder = {
                    Text(
                        "Paste or type IDs e.g. 101, 102, 105, 201-205, LR-401...\nSupports comma, space, newline, or 1-10 ranges",
                        fontSize = 12.sp,
                        color = Slate400
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reference_ids_input_field"),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            )

            // Quick Example Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Presets:",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontWeight = FontWeight.Bold)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate100,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.clickable { rawInputText = "101, 102, 103, 104, 105" }
                ) {
                    Text(
                        text = "101-105",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BlueAccent),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate100,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.clickable { rawInputText = "201, 205, 210, 215, 220" }
                ) {
                    Text(
                        text = "201, 205, 210...",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BlueAccent),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate100,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.clickable { rawInputText = "LR-01, LR-02, LR-03" }
                ) {
                    Text(
                        text = "LR-01..03",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BlueAccent),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (rawInputText.isNotEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall.copy(color = RoseError, fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .clickable { rawInputText = "" }
                            .padding(4.dp)
                    )
                }
            }

            // Detected Reference IDs Preview Chips
            if (parsedIds.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected Reference IDs (${parsedIds.size} Items):",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                        )
                        Text(
                            text = "Sequential FIFO Queue",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = EmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        parsedIds.take(15).forEach { id ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BlueContainer,
                                border = BorderStroke(1.dp, BlueAccent.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "#$id",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BlueAccent
                                        )
                                    )
                                }
                            }
                        }
                        if (parsedIds.size > 15) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate200
                            ) {
                                Text(
                                    text = "+${parsedIds.size - 15} more",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // WorkManager Anti-Freeze Engine Info
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "⚡ WorkManager Sequential Processing: IDs will download one by one in the background with anti-blocking delays. The UI remains 100% fluid and responsive.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Slate800)
                    )
                }
            }

            // Primary Queue Button
            Button(
                onClick = {
                    if (currentDistrict != null && currentUpazila != null && activeMouza != null && parsedIds.isNotEmpty()) {
                        onQueueReferenceIds(
                            currentDistrict,
                            currentUpazila,
                            activeMouza,
                            parsedIds,
                            selectedRecordTypes
                        )
                        rawInputText = ""
                        Toast.makeText(
                            context,
                            "✅ ${parsedIds.size}টি রেফারেন্স আইডি WorkManager কিউ-তে যুক্ত হয়েছে (ক্রমানুসারে ডাউনলোড হচ্ছে)!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                enabled = parsedIds.isNotEmpty() && activeMouza != null && currentDistrict != null && currentUpazila != null,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("queue_reference_ids_button"),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (parsedIds.isEmpty()) {
                        "Enter Reference IDs Above to Queue"
                    } else {
                        "Queue ${parsedIds.size} Reference IDs for Background Download"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun QueueHudCard(
    totalTasks: Int,
    completedTasks: Int,
    downloadingTasks: Int,
    pendingTasks: Int,
    overallProgress: Int,
    currentBatch: Int,
    totalBatches: Int,
    targetTotalMouzas: Int,
    downloadedMouzasCount: Int,
    isPaused: Boolean,
    isWorkManagerActive: Boolean,
    useWorkManagerService: Boolean,
    onToggleWorkManagerService: (Boolean) -> Unit,
    antiBlockingDescription: String,
    onTogglePause: () -> Unit,
    onClearQueue: () -> Unit,
    tasks: List<DownloadTask>,
    onOpenFullQueueScreen: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (downloadingTasks > 0) EmeraldPrimary.copy(alpha = alpha) else Slate400
                            )
                    )

                    Column {
                        Text(
                            text = "Sequential Queue HUD",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Slate800
                        )
                        Text(
                            text = "Batch $currentBatch of $totalBatches • Target: $downloadedMouzasCount/$targetTotalMouzas",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = EmeraldPrimary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onTogglePause,
                        enabled = totalTasks > 0 && completedTasks < totalTasks,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isPaused) AmberDark else Slate700,
                            containerColor = if (isPaused) AmberContainer else Slate100
                        ),
                        modifier = Modifier.testTag("pause_resume_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPaused) "Resume" else "Pause",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onClearQueue,
                        enabled = totalTasks > 0,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RoseError,
                            containerColor = RoseContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("clear_queue_button")
                    ) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Current Batch Progress",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate600
                    )
                    Text(
                        text = "$overallProgress%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                    )
                }

                LinearProgressIndicator(
                    progress = { overallProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = EmeraldPrimary,
                    trackColor = Slate200
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryPill(title = "Total", count = totalTasks, bgColor = Slate100, textColor = Slate800, modifier = Modifier.weight(1f))
                SummaryPill(title = "Done", count = completedTasks, bgColor = EmeraldContainer, textColor = EmeraldDark, modifier = Modifier.weight(1f))
                SummaryPill(title = "Active", count = downloadingTasks, bgColor = BlueContainer, textColor = BlueAccent, modifier = Modifier.weight(1f))
                SummaryPill(title = "Queued", count = pendingTasks, bgColor = AmberContainer, textColor = AmberDark, modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate100)
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                Text(
                    text = "Sequential Auto-Advance: When this batch of 7 completes, the next 7 are automatically queued without repeats until all $targetTotalMouzas mouzas finish.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Slate700
                )
            }

            // WorkManager Background Service & Anti-Blocking Protection Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWorkManagerActive) EmeraldContainer.copy(alpha = 0.45f) else Slate50
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isWorkManagerActive) EmeraldPrimary.copy(alpha = 0.4f) else Slate200
                ),
                modifier = Modifier.fillMaxWidth().testTag("workmanager_status_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isWorkManagerActive) EmeraldPrimary else Slate400)
                            )
                            Column {
                                Text(
                                    text = "WorkManager Background Service",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                    color = if (isWorkManagerActive) EmeraldDark else Slate800
                                )
                                Text(
                                    text = if (isWorkManagerActive) "Processing in Background • UI 100% Fluid & Responsive" else "Anti-Freeze Engine Standby",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isWorkManagerActive) EmeraldPrimary else Slate500
                                )
                            }
                        }

                        Switch(
                            checked = useWorkManagerService,
                            onCheckedChange = onToggleWorkManagerService,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldPrimary,
                                checkedTrackColor = EmeraldContainer
                            ),
                            modifier = Modifier.testTag("workmanager_service_toggle")
                        )
                    }

                    // Anti-Blocking details badge with randomized delay info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.9f))
                            .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Anti-Blocking Protection: $antiBlockingDescription",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                color = Slate800
                            )
                            Text(
                                text = "Randomized request intervals prevent server-side IP blocking and 429 rate limits.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = Slate600
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Slate50)
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                if (tasks.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ডাউনলোড কিউ খালি",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate600
                        )
                        Text(
                            text = "মৌজা সিলেক্ট করে Queue বাটনে ক্লিক করুন",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Slate400
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            DownloadTaskCard(task = task)
                        }
                    }
                }
            }

            // Button to open dedicated full Queue Status & Progress screen
            Button(
                onClick = onOpenFullQueueScreen,
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_queue_screen_button"),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FormatListNumbered,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AmberAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Full Queue Monitor & Progress Bars ➔",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun SummaryPill(
    title: String,
    count: Int,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = textColor.copy(alpha = 0.8f)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = textColor
        )
    }
}

@Composable
fun DownloadTaskCard(task: DownloadTask) {
    val isDone = task.status == TaskStatus.COMPLETED
    val isRunning = task.status == TaskStatus.DOWNLOADING

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isDone -> Slate100.copy(alpha = 0.7f)
                    isRunning -> EmeraldContainer.copy(alpha = 0.4f)
                    else -> Color.White
                }
            )
            .border(
                1.dp,
                when {
                    isDone -> Slate200
                    isRunning -> EmeraldPrimary.copy(alpha = 0.7f)
                    else -> Slate200
                },
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.mouzaNameBn,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Slate800
                )
                Text(
                    text = "(${task.mouzaNameEn})",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Slate500
                )
                Text(
                    text = "J.L. ${task.jlNo}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = Slate700,
                    modifier = Modifier
                        .background(Slate200, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
                Text(
                    text = task.recordTypeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = EmeraldPrimary
                    ),
                    modifier = Modifier
                        .background(EmeraldContainer, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }

            when (task.status) {
                TaskStatus.COMPLETED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .background(EmeraldContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(12.dp))
                        Text("Done", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldDark))
                    }
                }
                TaskStatus.DOWNLOADING -> {
                    Text(
                        text = "${task.progress}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = BlueAccent),
                        modifier = Modifier
                            .background(BlueContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                TaskStatus.PENDING -> {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall.copy(color = Slate500),
                        modifier = Modifier
                            .background(Slate200, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                TaskStatus.PAUSED -> {
                    Text(
                        text = "Paused",
                        style = MaterialTheme.typography.labelSmall.copy(color = AmberDark),
                        modifier = Modifier
                            .background(AmberContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                TaskStatus.FAILED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .background(RoseContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = RoseError, modifier = Modifier.size(12.dp))
                        Text("Failed", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RoseError))
                    }
                }
            }
        }

        LinearProgressIndicator(
            progress = { task.progress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when (task.status) {
                TaskStatus.COMPLETED -> EmeraldPrimary
                TaskStatus.FAILED -> RoseError
                TaskStatus.PAUSED -> AmberAccent
                else -> BlueAccent
            },
            trackColor = Slate200
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${task.upazilaNameBn} (Batch #${task.batchNumber}) • Khatians: ${task.downloadedKhatians}/${task.totalKhatians}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Slate500
            )
            Text(
                text = if (isRunning) "${task.speedKbps} KB/s" else if (isDone) "Archived" else "Queued",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = if (isRunning) BlueAccent else Slate400
            )
        }
    }
}

@Composable
fun SavedRecordsDialog(
    records: List<SavedRecord>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = EmeraldPrimary)
                    Text("Downloaded Records Vault", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                if (records.isNotEmpty()) {
                    IconButton(onClick = onClearAll) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear all", tint = RoseError)
                    }
                }
            }
        },
        text = {
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "এখনো কোনো রেকর্ড সম্পন্ন হয়নি।\nমৌজা ডাউনলোড সম্পন্ন হলে এখানে সংরক্ষিত হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records, key = { it.id }) { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Slate100)
                                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = rec.mouzaNameBn,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Slate900
                                    )
                                    Text(
                                        text = rec.recordType,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary),
                                        modifier = Modifier
                                            .background(EmeraldContainer, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = "${rec.districtNameBn} • ${rec.upazilaNameBn} • J.L. ${rec.jlNo}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate600
                                )
                                Text(
                                    text = "খতিয়ান সংখ্যা: ${rec.khatianCount} • আকার: ${rec.fileSizeFormatted}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Slate400
                                )
                            }

                            IconButton(onClick = { onDelete(rec.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Slate400)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Close")
            }
        }
    )
}
