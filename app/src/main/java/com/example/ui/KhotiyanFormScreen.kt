package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.District
import com.example.data.model.DownloadTask
import com.example.data.model.Mouza
import com.example.data.model.RecordType
import com.example.data.model.TaskStatus
import com.example.data.model.Upazila
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

enum class KhotiyanInputMode {
    SINGLE,
    RANGE,
    CUSTOM_LIST,
    ALL_KHOTIYANS
}

/**
 * Multi-Step Input Form Screen for selecting Region (Cumilla/Brahmanbaria),
 * Mouza, and Khotiyan number with real-time live progress indicators
 * for the active download queue.
 */
@Composable
fun KhotiyanFormScreen(
    viewModel: LRDownloaderViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsState()
    val savedRecords by viewModel.savedRecords.collectAsState()

    // Stepper State: Step 1 = Region & Upazila, Step 2 = Mouza, Step 3 = Khotiyan & Survey, Step 4 = Summary & Queue
    var currentStep by remember { mutableIntStateOf(1) }

    // Region & Upazila state
    var selectedDistrictId by remember { mutableStateOf(uiState.selectedDistrictId.ifEmpty { "cumilla" }) }
    val allDistricts = viewModel.allDistricts
    val currentDistrict = remember(selectedDistrictId, allDistricts) {
        allDistricts.find { it.id == selectedDistrictId } ?: allDistricts.firstOrNull()
    }
    val currentUpazilas = currentDistrict?.upazilas ?: emptyList()
    var selectedUpazilaId by remember(selectedDistrictId) {
        mutableStateOf(currentUpazilas.firstOrNull()?.id)
    }
    val currentUpazila = remember(selectedUpazilaId, currentUpazilas) {
        currentUpazilas.find { it.id == selectedUpazilaId } ?: currentUpazilas.firstOrNull()
    }

    // Mouza state
    val availableMouzas = currentUpazila?.mouzas ?: emptyList()
    var selectedMouzaId by remember(selectedUpazilaId) {
        mutableStateOf(availableMouzas.firstOrNull()?.id)
    }
    val currentMouza = remember(selectedMouzaId, availableMouzas) {
        availableMouzas.find { it.id == selectedMouzaId } ?: availableMouzas.firstOrNull()
    }
    var mouzaSearchQuery by remember { mutableStateOf("") }
    val filteredMouzas = remember(availableMouzas, mouzaSearchQuery) {
        if (mouzaSearchQuery.isBlank()) {
            availableMouzas
        } else {
            val q = mouzaSearchQuery.lowercase().trim()
            availableMouzas.filter {
                it.nameBn.lowercase().contains(q) ||
                        it.nameEn.lowercase().contains(q) ||
                        it.jlNo.contains(q)
            }
        }
    }

    // Khotiyan selection state
    var inputMode by remember { mutableStateOf(KhotiyanInputMode.RANGE) }
    var singleKhotiyanText by remember { mutableStateOf("১০১") }
    var rangeStartText by remember { mutableStateOf("১") }
    var rangeEndText by remember { mutableStateOf("৫০") }
    var customListText by remember { mutableStateOf("১, ৫, ১২, ১৮, ২৫") }
    var selectedRecordTypes by remember {
        mutableStateOf(setOf(RecordType.RS))
    }

    // Queue telemetry
    val tasks = uiState.queue
    val activeTasks = remember(tasks) { tasks.filter { it.status == TaskStatus.DOWNLOADING } }
    val pendingTasks = remember(tasks) { tasks.filter { it.status == TaskStatus.PENDING } }
    val completedTasks = remember(tasks) { tasks.filter { it.status == TaskStatus.COMPLETED } }
    val totalTasksCount = tasks.size
    val overallProgress = if (totalTasksCount == 0) 0 else {
        tasks.sumOf { it.progress } / totalTasksCount
    }
    val activeAndPendingCount = activeTasks.size + pendingTasks.size

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .testTag("khotiyan_form_screen"),
        topBar = {
            KhotiyanFormTopBar(
                currentStep = currentStep,
                activeQueueCount = activeAndPendingCount,
                onNavigateBack = onNavigateBack,
                onOpenQueueStatus = { viewModel.navigateToScreen(AppScreen.QUEUE_STATUS) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* already on form */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = "Multi-Step Form"
                        )
                    },
                    label = { Text("Khotiyan Form", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_item_khotiyan_form")
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateToScreen(AppScreen.DOWNLOADER) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Mass Downloader"
                        )
                    },
                    label = { Text("Downloader") },
                    modifier = Modifier.testTag("nav_item_downloader")
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateToScreen(AppScreen.QUEUE_STATUS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeAndPendingCount > 0) {
                                    Badge(containerColor = if (activeTasks.isNotEmpty()) BlueAccent else AmberDark) {
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
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. STEPPER WIZARD TRACK
            item {
                StepIndicatorCard(
                    currentStep = currentStep,
                    onStepClick = { targetStep ->
                        if (targetStep < currentStep) {
                            currentStep = targetStep
                        } else if (targetStep == 2 && currentDistrict != null && currentUpazila != null) {
                            currentStep = 2
                        } else if (targetStep == 3 && currentMouza != null) {
                            currentStep = 3
                        }
                    }
                )
            }

            // 2. LIVE QUEUE PROGRESS INDICATOR (Always visible at top of form)
            item {
                ActiveQueueProgressIndicatorsSection(
                    activeTasks = activeTasks,
                    pendingTasks = pendingTasks,
                    completedCount = completedTasks.size,
                    totalTasks = totalTasksCount,
                    overallProgress = overallProgress,
                    isPaused = uiState.isPaused,
                    onTogglePause = { viewModel.togglePauseQueue() },
                    onClearQueue = { viewModel.clearQueue() },
                    onOpenQueueMonitor = { viewModel.navigateToScreen(AppScreen.QUEUE_STATUS) }
                )
            }

            // 3. STEP CONTENT SWITCHER
            when (currentStep) {
                1 -> {
                    // STEP 1: REGION (CUMILLA / BRAHMANBARIA) & UPAZILA SELECTION
                    item {
                        Step1RegionSelectionCard(
                            districts = allDistricts,
                            selectedDistrictId = selectedDistrictId,
                            onDistrictSelect = { distId ->
                                selectedDistrictId = distId
                                viewModel.onDistrictChanged(distId)
                                val newDist = allDistricts.find { it.id == distId }
                                selectedUpazilaId = newDist?.upazilas?.firstOrNull()?.id
                                selectedMouzaId = newDist?.upazilas?.firstOrNull()?.mouzas?.firstOrNull()?.id
                            },
                            upazilas = currentUpazilas,
                            selectedUpazilaId = selectedUpazilaId,
                            onUpazilaSelect = { upazId ->
                                selectedUpazilaId = upazId
                                viewModel.onUpazilaChanged(upazId)
                                val newUpaz = currentUpazilas.find { it.id == upazId }
                                selectedMouzaId = newUpaz?.mouzas?.firstOrNull()?.id
                            },
                            onNext = {
                                if (currentUpazila != null) {
                                    currentStep = 2
                                } else {
                                    Toast.makeText(context, "অনুগ্রহ করে একটি উপজেলা নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
                2 -> {
                    // STEP 2: MOUZA & JL NUMBER SELECTION
                    item {
                        Step2MouzaSelectionCard(
                            districtName = currentDistrict?.nameBn ?: "",
                            upazilaName = currentUpazila?.nameBn ?: "",
                            mouzas = filteredMouzas,
                            selectedMouzaId = selectedMouzaId,
                            searchQuery = mouzaSearchQuery,
                            onSearchQueryChange = { mouzaSearchQuery = it },
                            onMouzaSelect = { mouzaId ->
                                selectedMouzaId = mouzaId
                            },
                            onPrevious = { currentStep = 1 },
                            onNext = {
                                if (currentMouza != null) {
                                    currentStep = 3
                                } else {
                                    Toast.makeText(context, "অনুগ্রহ করে একটি মৌজা নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
                3 -> {
                    // STEP 3: KHOTIYAN NUMBER & SURVEY TYPES INPUT
                    item {
                        Step3KhotiyanInputCard(
                            selectedDistrict = currentDistrict,
                            selectedUpazila = currentUpazila,
                            selectedMouza = currentMouza,
                            inputMode = inputMode,
                            onInputModeChange = { inputMode = it },
                            singleKhotiyanText = singleKhotiyanText,
                            onSingleKhotiyanChange = { singleKhotiyanText = it },
                            rangeStartText = rangeStartText,
                            onRangeStartChange = { rangeStartText = it },
                            rangeEndText = rangeEndText,
                            onRangeEndChange = { rangeEndText = it },
                            customListText = customListText,
                            onCustomListChange = { customListText = it },
                            selectedRecordTypes = selectedRecordTypes,
                            onToggleRecordType = { type ->
                                selectedRecordTypes = if (selectedRecordTypes.contains(type)) {
                                    if (selectedRecordTypes.size > 1) selectedRecordTypes - type else selectedRecordTypes
                                } else {
                                    selectedRecordTypes + type
                                }
                            },
                            onSelectAllRecordTypes = {
                                selectedRecordTypes = RecordType.values().toSet()
                            },
                            onPrevious = { currentStep = 2 },
                            onNext = {
                                val validation = validateKhotiyanInput(inputMode, singleKhotiyanText, rangeStartText, rangeEndText, customListText)
                                if (validation.isValid) {
                                    focusManager.clearFocus()
                                    currentStep = 4
                                } else {
                                    Toast.makeText(context, validation.errorMessage, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
                4 -> {
                    // STEP 4: REVIEW SUMMARY & ENQUEUE DOWNLOAD ACTION
                    item {
                        Step4SummaryAndQueueCard(
                            district = currentDistrict,
                            upazila = currentUpazila,
                            mouza = currentMouza,
                            inputMode = inputMode,
                            singleKhotiyanText = singleKhotiyanText,
                            rangeStartText = rangeStartText,
                            rangeEndText = rangeEndText,
                            customListText = customListText,
                            recordTypes = selectedRecordTypes,
                            onPrevious = { currentStep = 3 },
                            onQueueDownload = {
                                if (currentDistrict != null && currentUpazila != null && currentMouza != null) {
                                    val (label, count) = getKhotiyanLabelAndCount(
                                        inputMode, singleKhotiyanText, rangeStartText, rangeEndText, customListText
                                    )
                                    viewModel.queueSpecificKhotiyanDownload(
                                        district = currentDistrict,
                                        upazila = currentUpazila,
                                        mouza = currentMouza,
                                        khotiyanLabel = label,
                                        recordTypes = selectedRecordTypes,
                                        totalKhatians = count
                                    )
                                    Toast.makeText(
                                        context,
                                        "✅ ${currentMouza.nameBn} ($label) ডাউনলোড কিউ-তে যুক্ত হয়েছে!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    // Reset to step 1 or stay on review with fresh queue indicators
                                    currentStep = 1
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top App Bar for Khotiyan Form Screen
 */
@Composable
private fun KhotiyanFormTopBar(
    currentStep: Int,
    activeQueueCount: Int,
    onNavigateBack: () -> Unit,
    onOpenQueueStatus: () -> Unit
) {
    Surface(
        color = EmeraldDark,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.5f))
                            .testTag("btn_khotiyan_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Downloader",
                            tint = Color.White
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "DLR&S Form Wizard",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = EmeraldContainer,
                                modifier = Modifier
                                    .background(EmeraldPrimary, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                            Text(
                                text = "ধাপ $currentStep / ৪",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = AmberAccent
                            )
                        }
                        Text(
                            text = "খতিয়ান ডাউনলোড অনুসন্ধান",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4-Step Interactive Visual Stepper Indicator
 */
@Composable
private fun StepIndicatorCard(
    currentStep: Int,
    onStepClick: (Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_step_indicator"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            val steps = listOf(
                Pair(1, "১. অঞ্চল ও উপজেলা"),
                Pair(2, "২. মৌজা ও জেএল"),
                Pair(3, "৩. খতিয়ান নম্বর"),
                Pair(4, "৪. যাচাই ও কিউ")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                steps.forEachIndexed { index, (stepNum, stepTitle) ->
                    val isDone = currentStep > stepNum
                    val isActive = currentStep == stepNum

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onStepClick(stepNum) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> EmeraldPrimary
                                        isActive -> BlueAccent
                                        else -> Slate200
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "$stepNum",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = if (isActive) Color.White else Slate600
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stepTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal
                            ),
                            color = when {
                                isActive -> BlueAccent
                                isDone -> EmeraldPrimary
                                else -> Slate500
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(2.dp)
                                .background(if (currentStep > index + 1) EmeraldPrimary else Slate200)
                        )
                    }
                }
            }
        }
    }
}

/**
 * LIVE PROGRESS INDICATORS FOR THE ACTIVE DOWNLOAD QUEUE
 * Prominently embedded right above the form to meet user specification.
 */
@Composable
private fun ActiveQueueProgressIndicatorsSection(
    activeTasks: List<DownloadTask>,
    pendingTasks: List<DownloadTask>,
    completedCount: Int,
    totalTasks: Int,
    overallProgress: Int,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onClearQueue: () -> Unit,
    onOpenQueueMonitor: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("section_active_queue_indicators"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (activeTasks.isNotEmpty()) Color.White else Slate100
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row with Controls
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTasks.isNotEmpty()) BlueContainer else Slate200),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeTasks.isNotEmpty()) Icons.Default.CloudDownload else Icons.Default.HourglassTop,
                            contentDescription = "Active Queue",
                            tint = if (activeTasks.isNotEmpty()) BlueAccent else Slate600,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "সক্রিয় ডাউনলোড কিউ (Active Queue)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Slate900
                            )
                            if (activeTasks.isNotEmpty()) {
                                Text(
                                    text = "চলমান (${activeTasks.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = BlueAccent,
                                    modifier = Modifier
                                        .background(BlueContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (totalTasks == 0) "কিউ ফাঁকা • ফর্ম পূরণ করে ডাউনলোড শুরু করুন"
                            else "$completedCount/$totalTasks সম্পন্ন • $overallProgress% প্রগ্রেস",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Slate500
                        )
                    }
                }

                // Quick Action Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (totalTasks > 0) {
                        IconButton(
                            onClick = onTogglePause,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isPaused) AmberContainer else Slate100)
                                .testTag("btn_form_toggle_pause")
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume" else "Pause",
                                tint = if (isPaused) AmberDark else Slate700,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onClearQueue,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RoseContainer)
                                .testTag("btn_form_clear_queue")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Queue",
                                tint = RoseError,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Overall Progress Bar (if tasks exist)
            if (totalTasks > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPaused) "⏸️ কিউ স্থগিত (Paused)" else "সার্বিক অগ্রগতি (Overall Progress)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = if (isPaused) AmberDark else Slate700
                        )
                        Text(
                            text = "$overallProgress%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (overallProgress == 100) EmeraldPrimary else BlueAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { overallProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("progress_overall_queue"),
                        color = if (overallProgress == 100) EmeraldPrimary else BlueAccent,
                        trackColor = Slate200,
                    )
                }
            }

            // Active Downloads Progress Bars (Individual tasks)
            if (activeTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Slate200)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "সরাসরি ডাউনলোড হচ্ছে (${activeTasks.size} টি টাস্ক):",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeTasks.forEach { task ->
                        ActiveTaskMiniProgressRow(task = task)
                    }
                }
            }

            // Pending Queue Indicator Banner
            if (pendingTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = AmberDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${pendingTasks.size} টি টাস্ক কিউ-তে অপেক্ষমান",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = AmberDark
                        )
                    }
                    Text(
                        text = "পরবর্তী টাস্ক: ${pendingTasks.firstOrNull()?.mouzaNameBn ?: ""}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Slate600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Real-time Active Task Progress Row with live percentage, speed, and counter
 */
@Composable
private fun ActiveTaskMiniProgressRow(task: DownloadTask) {
    Surface(
        color = Slate50,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BlueAccent.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_active_${task.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
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
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Syncing",
                        tint = BlueAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = task.mouzaNameBn,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (task.speedKbps > 0) {
                        Text(
                            text = "${task.speedKbps} KB/s",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = EmeraldDark
                        )
                    }
                    Text(
                        text = "${task.progress}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = BlueAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { task.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BlueAccent,
                trackColor = BlueContainer
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${task.downloadedKhatians} / ${task.totalKhatians} খতিয়ান সম্পন্ন",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Slate600
                )
                Text(
                    text = "${task.upazilaNameBn} • ${task.recordTypeLabel}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Slate500
                )
            }
        }
    }
}

/**
 * STEP 1: REGION SELECTION (CUMILLA / BRAHMANBARIA) & UPAZILA
 */
@Composable
private fun Step1RegionSelectionCard(
    districts: List<District>,
    selectedDistrictId: String,
    onDistrictSelect: (String) -> Unit,
    upazilas: List<Upazila>,
    selectedUpazilaId: String?,
    onUpazilaSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_step_1_region"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "ধাপ ১: অঞ্চল ও উপজেলা নির্বাচন",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Slate900
                    )
                    Text(
                        text = "কুমিল্লা অথবা ব্রাহ্মণবাড়িয়া জেলা বাছাই করুন",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Slate500
                    )
                }
            }

            HorizontalDivider(color = Slate200)

            // REGION CARDS: CUMILLA & BRAHMANBARIA
            Text(
                text = "১.১ অঞ্চল / জেলা নির্বাচন করুন (Select Region):",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Slate700
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                districts.forEach { dist ->
                    val isSelected = dist.id == selectedDistrictId
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) EmeraldPrimary else Slate200,
                        label = "border_color"
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDistrictSelect(dist.id) }
                            .testTag("region_chip_${dist.id}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) EmeraldContainer.copy(alpha = 0.4f) else Slate50,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) EmeraldPrimary else Slate200),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${dist.upazilas.size} উপজেলা",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isSelected) EmeraldDark else Slate500
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = dist.nameBn,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                ),
                                color = if (isSelected) EmeraldDark else Slate800
                            )

                            Text(
                                text = dist.nameEn,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Slate500
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 1.2 UPAZILA SELECTION
            Text(
                text = "১.২ সংশ্লিষ্ট উপজেলা নির্বাচন করুন (${upazilas.size} টি উপলব্ধ):",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Slate700
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(upazilas) { upazila ->
                    val isSelected = upazila.id == selectedUpazilaId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onUpazilaSelect(upazila.id) },
                        label = {
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = upazila.nameBn,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = "${upazila.mouzas.size} মৌজা",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (isSelected) Color.White else Slate500
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Slate100,
                            labelColor = Slate800
                        ),
                        modifier = Modifier.testTag("chip_upazila_${upazila.id}")
                    )
                }
            }

            // Next Step Button
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_step1_next"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "পরবর্তী ধাপ: মৌজা নির্বাচন (Next)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * STEP 2: MOUZA SELECTION WITH SEARCH & JL NUMBERS
 */
@Composable
private fun Step2MouzaSelectionCard(
    districtName: String,
    upazilaName: String,
    mouzas: List<Mouza>,
    selectedMouzaId: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMouzaSelect: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_step_2_mouza"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with breadcrumb
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
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ধাপ ২: মৌজা ও জেএল নির্বাচন",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        Text(
                            text = "$districtName > $upazilaName",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = BlueAccent
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate100
                ) {
                    Text(
                        text = "${mouzas.size} মৌজা",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate700
                    )
                }
            }

            HorizontalDivider(color = Slate200)

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("মৌজার নাম বা জেএল নং লিখুন (যেমন: মেদ্দা, ৪২)...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Slate500
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Slate500
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_mouza_search"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BlueAccent,
                    unfocusedBorderColor = Slate200
                )
            )

            // Mouza Grid / List
            Text(
                text = "মৌজা তালিকা থেকে নির্বাচন করুন:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Slate700
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(mouzas, key = { it.id }) { mouza ->
                        val isSelected = mouza.id == selectedMouzaId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMouzaSelect(mouza.id) }
                                .testTag("mouza_row_${mouza.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BlueContainer.copy(alpha = 0.4f) else Slate50,
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) BlueAccent else Slate200)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) BlueAccent else Slate200),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = mouza.nameBn,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Slate900
                                        )
                                        Text(
                                            text = mouza.nameEn,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = Slate500
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) BlueAccent else Slate200
                                ) {
                                    Text(
                                        text = "জেএল নং: ${mouza.jlNo}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (isSelected) Color.White else Slate700
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Navigation Buttons (Back & Next)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_step2_prev"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("পূর্ববর্তী ধাপ", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("btn_step2_next"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("পরবর্তী ধাপ: খতিয়ান (Next)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * STEP 3: KHOTIYAN NUMBER & SURVEY TYPES INPUT
 */
@Composable
private fun Step3KhotiyanInputCard(
    selectedDistrict: District?,
    selectedUpazila: Upazila?,
    selectedMouza: Mouza?,
    inputMode: KhotiyanInputMode,
    onInputModeChange: (KhotiyanInputMode) -> Unit,
    singleKhotiyanText: String,
    onSingleKhotiyanChange: (String) -> Unit,
    rangeStartText: String,
    onRangeStartChange: (String) -> Unit,
    rangeEndText: String,
    onRangeEndChange: (String) -> Unit,
    customListText: String,
    onCustomListChange: (String) -> Unit,
    selectedRecordTypes: Set<RecordType>,
    onToggleRecordType: (RecordType) -> Unit,
    onSelectAllRecordTypes: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_step_3_khotiyan"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title & Selected Mouza Pill
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
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = AmberDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ধাপ ৩: খতিয়ান নম্বর ও জরিপ ধরন",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate900
                        )
                        Text(
                            text = "মৌজা: ${selectedMouza?.nameBn ?: ""} (জেএল: ${selectedMouza?.jlNo ?: ""})",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AmberDark
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate200)

            // 3.1 SURVEY RECORD TYPES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "৩.১ জরিপ ধরন (Survey Record Type):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Slate700
                )
                Text(
                    text = "সব ধরন",
                    modifier = Modifier
                        .clickable { onSelectAllRecordTypes() }
                        .padding(4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecordType.values().forEach { type ->
                    val isChecked = selectedRecordTypes.contains(type)
                    FilterChip(
                        selected = isChecked,
                        onClick = { onToggleRecordType(type) },
                        label = {
                            Text(
                                text = "${type.code} (${type.bengaliName})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldDark,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("chip_record_type_${type.code}")
                    )
                }
            }

            // 3.2 KHOTIYAN INPUT MODES
            Text(
                text = "৩.২ খতিয়ান অনুসন্ধানের পদ্ধতি:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Slate700
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val modes = listOf(
                    Pair(KhotiyanInputMode.RANGE, "খতিয়ান রেঞ্জ (১-৫০)"),
                    Pair(KhotiyanInputMode.SINGLE, "একক খতিয়ান"),
                    Pair(KhotiyanInputMode.CUSTOM_LIST, "তালিকা"),
                    Pair(KhotiyanInputMode.ALL_KHOTIYANS, "সকল")
                )

                modes.forEach { (mode, label) ->
                    val isSelected = inputMode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onInputModeChange(mode) }
                            .testTag("mode_${mode.name}"),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) EmeraldPrimary else Slate100
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp
                            ),
                            color = if (isSelected) Color.White else Slate700,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // INPUT FIELDS ACCORDING TO MODE
            when (inputMode) {
                KhotiyanInputMode.SINGLE -> {
                    OutlinedTextField(
                        value = singleKhotiyanText,
                        onValueChange = onSingleKhotiyanChange,
                        label = { Text("খতিয়ান নম্বর (Khotiyan Number)") },
                        placeholder = { Text("যেমন: ১০১ বা 101") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_single_khotiyan"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                KhotiyanInputMode.RANGE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = rangeStartText,
                            onValueChange = onRangeStartChange,
                            label = { Text("শুরুর খতিয়ান") },
                            placeholder = { Text("১") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_range_start"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = rangeEndText,
                            onValueChange = onRangeEndChange,
                            label = { Text("শেষের খতিয়ান") },
                            placeholder = { Text("৫০") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_range_end"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Quick range chips
                    Text(
                        text = "দ্রুত রেঞ্জ নির্বাচন (Quick Presets):",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Slate500
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("১ - ২৫", Pair("১", "২৫")),
                            Pair("১ - ৫০", Pair("১", "৫০")),
                            Pair("১ - ১০০", Pair("১", "১০০")),
                            Pair("১০১ - ১৫০", Pair("১০১", "১৫০"))
                        ).forEach { (chipText, values) ->
                            Surface(
                                modifier = Modifier
                                    .clickable {
                                        onRangeStartChange(values.first)
                                        onRangeEndChange(values.second)
                                    },
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100,
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Text(
                                    text = chipText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Slate700
                                )
                            }
                        }
                    }
                }
                KhotiyanInputMode.CUSTOM_LIST -> {
                    OutlinedTextField(
                        value = customListText,
                        onValueChange = onCustomListChange,
                        label = { Text("কমা দিয়ে একাধিক খতিয়ান নম্বর লিখুন") },
                        placeholder = { Text("যেমন: ১, ৫, ১২, ২৫, ৪৪, ১০১") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_list"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                KhotiyanInputMode.ALL_KHOTIYANS -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, EmeraldLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = EmeraldDark
                            )
                            Text(
                                text = "এই মৌজার সকল খতিয়ান (১ থেকে শেষ পর্যন্ত) সিকোয়েনশিয়াল ব্যাচে ডাউনলোড হবে।",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = EmeraldDark
                            )
                        }
                    }
                }
            }

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_step3_prev"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("পূর্ববর্তী ধাপ", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("btn_step3_next"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("সারসংক্ষেপ ও কিউ (Review)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * STEP 4: CONFIRMATION SUMMARY & ONE-TAP QUEUE ACTION
 */
@Composable
private fun Step4SummaryAndQueueCard(
    district: District?,
    upazila: Upazila?,
    mouza: Mouza?,
    inputMode: KhotiyanInputMode,
    singleKhotiyanText: String,
    rangeStartText: String,
    rangeEndText: String,
    customListText: String,
    recordTypes: Set<RecordType>,
    onPrevious: () -> Unit,
    onQueueDownload: () -> Unit
) {
    val (khotiyanLabel, totalKhotiyansCount) = remember(
        inputMode, singleKhotiyanText, rangeStartText, rangeEndText, customListText
    ) {
        getKhotiyanLabelAndCount(inputMode, singleKhotiyanText, rangeStartText, rangeEndText, customListText)
    }

    val totalRecordsToDownload = totalKhotiyansCount * recordTypes.size
    val estimatedSizeMb = String.format("%.2f MB", totalRecordsToDownload * 0.095)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_step_4_summary"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "ধাপ ৪: ডাউনলোড সারসংক্ষেপ ও নিশ্চিতকরণ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Slate900
                    )
                    Text(
                        text = "তথ্য যাচাই করে কিউ-তে যুক্ত করতে বাটন চাপুন",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Slate500
                    )
                }
            }

            HorizontalDivider(color = Slate200)

            // Official GovTech Verified Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Slate50,
                border = BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryRow(label = "অঞ্চল / জেলা (Region)", value = "${district?.nameBn ?: ""} (${district?.nameEn ?: ""})")
                    SummaryRow(label = "উপজেলা (Upazila)", value = "${upazila?.nameBn ?: ""} (${upazila?.nameEn ?: ""})")
                    SummaryRow(label = "মৌজা ও জেএল (Mouza & JL)", value = "${mouza?.nameBn ?: ""} • জেএল নং: ${mouza?.jlNo ?: ""}")
                    SummaryRow(label = "জরিপ ধরন (Record Types)", value = recordTypes.joinToString(" + ") { it.code })
                    SummaryRow(label = "খতিয়ান বিবরণ (Khotiyans)", value = khotiyanLabel)
                    SummaryRow(label = "মোট খতিয়ান সংখ্যা", value = "$totalKhotiyansCount টি (মোট $totalRecordsToDownload রেকর্ড)")
                    SummaryRow(label = "আনুমানিক আকার (Est. Size)", value = estimatedSizeMb)
                    SummaryRow(label = "এপিআই প্রোটোকল", value = "DLR&S TLS 1.3 Anti-Freeze Indexed")
                }
            }

            // PRIMARY ONE-TAP ACTION BUTTON
            Button(
                onClick = onQueueDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_submit_queue_khotiyan"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "ডাউনলোড কিউ-তে যুক্ত করুন (Queue Download)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }

            // Return to edit button
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("btn_step4_edit_again"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("তথ্য পরিবর্তন করুন (Edit Inputs)", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = Slate600
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = Slate900,
            textAlign = TextAlign.End
        )
    }
}

private data class ValidationResult(val isValid: Boolean, val errorMessage: String = "")

private fun validateKhotiyanInput(
    mode: KhotiyanInputMode,
    single: String,
    start: String,
    end: String,
    custom: String
): ValidationResult {
    return when (mode) {
        KhotiyanInputMode.SINGLE -> {
            if (single.trim().isEmpty()) ValidationResult(false, "খতিয়ান নম্বর লিখুন")
            else ValidationResult(true)
        }
        KhotiyanInputMode.RANGE -> {
            val startNum = parseBanglaOrEnglishNumber(start)
            val endNum = parseBanglaOrEnglishNumber(end)
            if (startNum == null || endNum == null) {
                ValidationResult(false, "শুরু ও শেষের খতিয়ান সঠিক সংখ্যায় লিখুন")
            } else if (startNum > endNum) {
                ValidationResult(false, "শুরুর নম্বর শেষের নম্বরের চেয়ে ছোট হতে হবে")
            } else if (endNum - startNum > 200) {
                ValidationResult(false, "একবারে সর্বোচ্চ ২০০ খতিয়ান রেঞ্জ সমর্থনযোগ্য")
            } else {
                ValidationResult(true)
            }
        }
        KhotiyanInputMode.CUSTOM_LIST -> {
            if (custom.trim().isEmpty()) ValidationResult(false, "কমপক্ষে একটি খতিয়ান নম্বর লিখুন")
            else ValidationResult(true)
        }
        KhotiyanInputMode.ALL_KHOTIYANS -> ValidationResult(true)
    }
}

private fun parseBanglaOrEnglishNumber(str: String): Int? {
    val englishStr = str.trim().map { ch ->
        when (ch) {
            '০' -> '0'
            '১' -> '1'
            '২' -> '2'
            '৩' -> '3'
            '৪' -> '4'
            '৫' -> '5'
            '৬' -> '6'
            '৭' -> '7'
            '৮' -> '8'
            '৯' -> '9'
            else -> ch
        }
    }.joinToString("")
    return englishStr.toIntOrNull()
}

private fun getKhotiyanLabelAndCount(
    mode: KhotiyanInputMode,
    single: String,
    start: String,
    end: String,
    custom: String
): Pair<String, Int> {
    return when (mode) {
        KhotiyanInputMode.SINGLE -> {
            val clean = single.trim().ifEmpty { "১" }
            Pair("খতিয়ান $clean", 1)
        }
        KhotiyanInputMode.RANGE -> {
            val s = parseBanglaOrEnglishNumber(start) ?: 1
            val e = parseBanglaOrEnglishNumber(end) ?: 50
            val count = (e - s + 1).coerceAtLeast(1)
            Pair("খতিয়ান $start হতে $end ($count টি)", count)
        }
        KhotiyanInputMode.CUSTOM_LIST -> {
            val items = custom.split(',', ' ', ';').filter { it.isNotBlank() }
            val count = items.size.coerceAtLeast(1)
            Pair("নির্বাচিত খতিয়ান ($count টি)", count)
        }
        KhotiyanInputMode.ALL_KHOTIYANS -> {
            Pair("সকল খতিয়ান (Full Mouza)", 60)
        }
    }
}
