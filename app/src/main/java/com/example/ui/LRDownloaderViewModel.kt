package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.LandRecordStorageManager
import com.example.data.model.District
import com.example.data.model.DownloadTask
import com.example.data.model.LandRecordsDataset
import com.example.data.model.MasterArchive
import com.example.data.model.Mouza
import com.example.data.model.RecordType
import com.example.data.model.SavedRecord
import com.example.data.model.TaskStatus
import com.example.data.model.Upazila
import com.example.data.model.UpazilaFetchInfo
import com.example.data.repository.DownloadRepository
import com.example.data.worker.DownloadWorkManagerHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.random.Random

enum class AppScreen {
    DOWNLOADER,
    KHOTIYAN_FORM,
    QUEUE_STATUS,
    SAVED_VAULT
}

data class LRUiState(
    val selectedDistrictId: String = "cumilla",
    val selectedUpazilaId: String? = null,
    val isFetchingUpazila: Boolean = false,
    val upazilaFetchInfo: UpazilaFetchInfo? = null,
    val targetTotalMouzas: Int = 0,
    val selectedMouzaIds: Set<String> = emptySet(),
    val downloadedMouzaIds: Set<String> = emptySet(),
    val selectedRecordTypes: Set<RecordType> = setOf(RecordType.RS),
    val searchQuery: String = "",
    val queue: List<DownloadTask> = emptyList(),
    val isPaused: Boolean = false,
    val isAutoBatchMode: Boolean = true,
    val currentBatchNumber: Int = 1,
    val batchSize: Int = 7,
    val masterArchive: MasterArchive? = null,
    val isShowingHistoryDialog: Boolean = false,
    val isShowingLedgerDialog: Boolean = false,
    val isShowingSaveSuccessDialog: Boolean = false,
    val isShowingApkInfoDialog: Boolean = false,
    val cacheNotificationMessage: String? = null,
    val isWorkManagerActive: Boolean = false,
    val useWorkManagerBackgroundService: Boolean = true,
    val antiBlockingDelayDescription: String = "400ms – 2,400ms Randomized Jitter",
    val currentScreen: AppScreen = AppScreen.DOWNLOADER
)

class LRDownloaderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = DownloadRepository(db.savedRecordDao(), db.downloadQueueDao())

    val savedRecords: StateFlow<List<SavedRecord>> = repository.savedRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(LRUiState())
    val uiState: StateFlow<LRUiState> = _uiState.asStateFlow()

    // Bounded Concurrency Limiter: strictly max 2 concurrent downloads
    private val concurrencyLimiter = Semaphore(2)
    private val taskJobs = mutableMapOf<String, Job>()

    init {
        // Anti-Freeze Core: Observe WorkManager active state
        DownloadWorkManagerHelper.isWorkRunningFlow(application).onEach { isRunning ->
            _uiState.value = _uiState.value.copy(isWorkManagerActive = isRunning)
        }.launchIn(viewModelScope)

        // Anti-Freeze Core: Persistent Queue Sync & Task Resumption
        viewModelScope.launch {
            // 1. Reset any tasks stuck in 'DOWNLOADING' to 'PENDING' for safe resumption
            repository.resetInterruptedTasks()

            // 2. Observe the persistent queue and keep UI in sync
            repository.downloadQueue.onEach { persistentQueue ->
                val completedIds = persistentQueue.filter { it.status == TaskStatus.COMPLETED }.map { it.mouzaId }.toSet()
                _uiState.value = _uiState.value.copy(
                    queue = persistentQueue,
                    downloadedMouzaIds = completedIds
                )

                // 3. Auto-resume Pending tasks if not paused
                if (!_uiState.value.isPaused) {
                    val hasPending = persistentQueue.any { it.status == TaskStatus.PENDING }
                    if (hasPending) {
                        if (_uiState.value.useWorkManagerBackgroundService) {
                            DownloadWorkManagerHelper.enqueueDownloadWork(
                                context = getApplication(),
                                autoBatchMode = _uiState.value.isAutoBatchMode
                            )
                        } else {
                            persistentQueue.filter { it.status == TaskStatus.PENDING }.forEach { task ->
                                if (!taskJobs.containsKey(task.id)) {
                                    launchTaskWorker(task.id)
                                }
                            }
                        }
                    }
                }

                // Check if current batch finished to advance or compile master
                if (persistentQueue.isNotEmpty() && persistentQueue.all { it.status == TaskStatus.COMPLETED }) {
                    checkAndTriggerNextBatch()
                }
            }.launchIn(viewModelScope)
        }
    }

    val allDistricts: List<District> = LandRecordsDataset.districts

    val currentDistrict: District?
        get() = allDistricts.find { it.id == _uiState.value.selectedDistrictId }

    val currentUpazilas: List<Upazila>
        get() = currentDistrict?.upazilas ?: emptyList()

    val currentUpazila: Upazila?
        get() = currentUpazilas.find { it.id == _uiState.value.selectedUpazilaId }

    val currentMouzas: List<Mouza>
        get() = currentUpazila?.mouzas ?: emptyList()

    val totalBatches: Int
        get() {
            val total = _uiState.value.targetTotalMouzas.coerceAtLeast(currentMouzas.size)
            if (total == 0) return 0
            val size = _uiState.value.batchSize
            return (total + size - 1) / size
        }

    fun onDistrictChanged(newDistrictId: String) {
        clearQueue()
        _uiState.value = _uiState.value.copy(
            selectedDistrictId = newDistrictId,
            selectedUpazilaId = null,
            isFetchingUpazila = false,
            upazilaFetchInfo = null,
            targetTotalMouzas = 0,
            selectedMouzaIds = emptySet(),
            downloadedMouzaIds = emptySet(),
            currentBatchNumber = 1,
            masterArchive = null,
            searchQuery = "",
            cacheNotificationMessage = null
        )
    }

    fun onUpazilaChanged(newUpazilaId: String?) {
        clearQueue()
        if (newUpazilaId == null) {
            _uiState.value = _uiState.value.copy(
                selectedUpazilaId = null,
                isFetchingUpazila = false,
                upazilaFetchInfo = null,
                targetTotalMouzas = 0,
                selectedMouzaIds = emptySet(),
                downloadedMouzaIds = emptySet(),
                currentBatchNumber = 1,
                masterArchive = null,
                searchQuery = "",
                cacheNotificationMessage = null
            )
            return
        }

        val upazila = currentUpazilas.find { it.id == newUpazilaId }
        val district = currentDistrict

        if (upazila != null && district != null) {
            val foundMouzaCount = upazila.mouzas.size
            val batchSize = _uiState.value.batchSize
            val totalBatchesCount = (foundMouzaCount + batchSize - 1) / batchSize

            val firstSevenIds = upazila.mouzas.take(batchSize).map { it.id }.toSet()

            val fetchInfo = UpazilaFetchInfo(
                upazilaId = upazila.id,
                upazilaNameBn = upazila.nameBn,
                upazilaNameEn = upazila.nameEn,
                districtNameBn = district.nameBn,
                totalMouzaCount = foundMouzaCount,
                batchCount = totalBatchesCount,
                message = "Upazila Survey Index: Found $foundMouzaCount mouzas in ${upazila.nameBn}."
            )

            _uiState.value = _uiState.value.copy(
                selectedUpazilaId = newUpazilaId,
                isFetchingUpazila = false,
                upazilaFetchInfo = fetchInfo,
                targetTotalMouzas = foundMouzaCount,
                selectedMouzaIds = firstSevenIds,
                downloadedMouzaIds = emptySet(),
                currentBatchNumber = 1,
                masterArchive = null,
                searchQuery = "",
                cacheNotificationMessage = null
            )
        }
    }

    fun toggleRecordType(type: RecordType) {
        val current = _uiState.value.selectedRecordTypes
        val newSet = if (current.contains(type)) {
            if (current.size > 1) current - type else current
        } else {
            current + type
        }
        _uiState.value = _uiState.value.copy(selectedRecordTypes = newSet)
    }

    fun selectAllRecordTypes() {
        _uiState.value = _uiState.value.copy(
            selectedRecordTypes = RecordType.values().toSet()
        )
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleMouza(mouzaId: String) {
        val currentSet = _uiState.value.selectedMouzaIds
        val newSet = if (currentSet.contains(mouzaId)) {
            currentSet - mouzaId
        } else {
            currentSet + mouzaId
        }
        _uiState.value = _uiState.value.copy(selectedMouzaIds = newSet)
    }

    fun selectAllMouzas() {
        val allIds = currentMouzas.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedMouzaIds = allIds)
    }

    fun selectNextSevenMouzas() {
        val undownloaded = currentMouzas.filter { it.id !in _uiState.value.downloadedMouzaIds }
        val nextSeven = undownloaded.take(_uiState.value.batchSize).map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedMouzaIds = nextSeven)
    }

    fun deselectAllMouzas() {
        _uiState.value = _uiState.value.copy(selectedMouzaIds = emptySet())
    }

    fun setShowHistoryDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(isShowingHistoryDialog = show)
    }

    fun setShowLedgerDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(isShowingLedgerDialog = show)
    }

    fun setShowApkInfoDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(isShowingApkInfoDialog = show)
    }

    fun queueSelectedDownloads() {
        val state = _uiState.value
        val upazila = currentUpazila ?: return
        val district = currentDistrict ?: return

        val candidates = if (state.selectedMouzaIds.isNotEmpty()) {
            upazila.mouzas.filter { it.id in state.selectedMouzaIds && it.id !in state.downloadedMouzaIds }
        } else {
            upazila.mouzas.filter { it.id !in state.downloadedMouzaIds }.take(state.batchSize)
        }

        if (candidates.isEmpty()) return

        enqueueBatch(candidates, district, upazila, state.currentBatchNumber)
    }

    fun queueSpecificKhotiyanDownload(
        district: District,
        upazila: Upazila,
        mouza: Mouza,
        khotiyanLabel: String,
        recordTypes: Set<RecordType>,
        totalKhatians: Int = 1
    ) {
        val timestamp = System.currentTimeMillis()
        val count = totalKhatians.coerceAtLeast(1) * recordTypes.size.coerceAtLeast(1)
        val fileSizeBytes = count * 95000L
        val taskId = "khotiyan-${mouza.id}-$timestamp"

        val task = DownloadTask(
            id = taskId,
            mouzaId = mouza.id,
            mouzaNameBn = "${mouza.nameBn} ($khotiyanLabel)",
            mouzaNameEn = "${mouza.nameEn} ($khotiyanLabel)",
            jlNo = mouza.jlNo,
            upazilaNameBn = upazila.nameBn,
            districtNameBn = district.nameBn,
            recordTypes = recordTypes.ifEmpty { setOf(RecordType.RS) },
            status = TaskStatus.PENDING,
            progress = 0,
            totalKhatians = count,
            downloadedKhatians = 0,
            fileSizeBytes = fileSizeBytes,
            speedKbps = 0,
            batchNumber = _uiState.value.currentBatchNumber,
            timestamp = timestamp
        )

        viewModelScope.launch {
            repository.insertTasksToQueue(listOf(task))
            if (_uiState.value.useWorkManagerBackgroundService && !_uiState.value.isPaused) {
                DownloadWorkManagerHelper.enqueueDownloadWork(getApplication(), replace = true)
            } else if (!_uiState.value.isPaused) {
                launchTaskWorker(taskId)
            }
        }
    }

    /**
     * Queues a user-entered list of specific land record reference IDs
     * to be downloaded sequentially via WorkManager in the background.
     */
    fun queueReferenceIdsDownload(
        district: District,
        upazila: Upazila,
        mouza: Mouza,
        referenceIds: List<String>,
        recordTypes: Set<RecordType>
    ) {
        if (referenceIds.isEmpty()) return
        val timestamp = System.currentTimeMillis()
        val currentBatch = _uiState.value.currentBatchNumber
        val effectiveRecordTypes = recordTypes.ifEmpty { setOf(RecordType.RS) }

        val newTasks = referenceIds.mapIndexed { index, refId ->
            val cleanId = refId.trim()
            val taskId = "ref-${mouza.id}-$cleanId-$timestamp-$index"
            val fileSizeBytes = 95000L * effectiveRecordTypes.size

            DownloadTask(
                id = taskId,
                mouzaId = mouza.id,
                mouzaNameBn = "${mouza.nameBn} [রেফারেন্স: $cleanId]",
                mouzaNameEn = "${mouza.nameEn} [Ref: $cleanId]",
                jlNo = mouza.jlNo,
                upazilaNameBn = upazila.nameBn,
                districtNameBn = district.nameBn,
                recordTypes = effectiveRecordTypes,
                status = TaskStatus.PENDING,
                progress = 0,
                totalKhatians = 1 * effectiveRecordTypes.size,
                downloadedKhatians = 0,
                fileSizeBytes = fileSizeBytes,
                speedKbps = 0,
                batchNumber = currentBatch,
                timestamp = timestamp + index,
                referenceId = cleanId
            )
        }

        val updatedQueue = _uiState.value.queue + newTasks
        _uiState.value = _uiState.value.copy(queue = updatedQueue)

        viewModelScope.launch {
            repository.insertTasksToQueue(newTasks)
            if (_uiState.value.useWorkManagerBackgroundService && !_uiState.value.isPaused) {
                DownloadWorkManagerHelper.enqueueDownloadWork(
                    context = getApplication(),
                    replace = true,
                    autoBatchMode = _uiState.value.isAutoBatchMode
                )
            } else if (!_uiState.value.isPaused) {
                newTasks.forEach { task ->
                    if (!taskJobs.containsKey(task.id)) {
                        launchTaskWorker(task.id)
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Parses and extracts land record reference IDs from flexible text input.
         * Supports comma, newline, space, semicolon separation, as well as ranges like 101-105.
         */
        fun parseReferenceIds(input: String): List<String> {
            if (input.isBlank()) return emptyList()
            val rawTokens = input.split(',', '\n', ';', ' ', '\t')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val results = mutableListOf<String>()
            for (token in rawTokens) {
                if (token.contains('-') && token.matches(Regex("""^\d+\s*-\s*\d+$"""))) {
                    val parts = token.split('-').map { it.trim().toIntOrNull() }
                    if (parts.size == 2 && parts[0] != null && parts[1] != null) {
                        val start = parts[0]!!
                        val end = parts[1]!!
                        if (start <= end && (end - start) <= 100) {
                            for (i in start..end) {
                                results.add(i.toString())
                            }
                            continue
                        }
                    }
                }
                results.add(token)
            }
            return results.distinct()
        }
    }

    private fun enqueueBatch(
        mouzasToQueue: List<Mouza>,
        district: District,
        upazila: Upazila,
        batchNum: Int
    ) {
        val timestamp = System.currentTimeMillis()
        val recordTypes = _uiState.value.selectedRecordTypes

        val newTasks = mouzasToQueue.mapIndexed { index, mouza ->
            val taskId = "${mouza.id}-${batchNum}-$timestamp-$index"
            val totalKhatians = (30 + Random.nextInt(50)) * recordTypes.size
            val fileSizeBytes = totalKhatians * 110000L

            DownloadTask(
                id = taskId,
                mouzaId = mouza.id,
                mouzaNameBn = mouza.nameBn,
                mouzaNameEn = mouza.nameEn,
                jlNo = mouza.jlNo,
                upazilaNameBn = upazila.nameBn,
                districtNameBn = district.nameBn,
                recordTypes = recordTypes,
                status = TaskStatus.PENDING,
                progress = 0,
                totalKhatians = totalKhatians,
                downloadedKhatians = 0,
                fileSizeBytes = fileSizeBytes,
                speedKbps = 0,
                batchNumber = batchNum,
                timestamp = timestamp
            )
        }

        val updatedQueue = _uiState.value.queue + newTasks
        _uiState.value = _uiState.value.copy(queue = updatedQueue)

        viewModelScope.launch {
            repository.insertTasksToQueue(newTasks)
            if (_uiState.value.useWorkManagerBackgroundService && !_uiState.value.isPaused) {
                DownloadWorkManagerHelper.enqueueDownloadWork(
                    context = getApplication(),
                    replace = true,
                    autoBatchMode = _uiState.value.isAutoBatchMode
                )
            }
        }
    }

    private fun launchTaskWorker(taskId: String) {
        val job = viewModelScope.launch {
            concurrencyLimiter.withPermit {
                val task = repository.getTaskById(taskId) ?: return@withPermit
                if (task.status == TaskStatus.COMPLETED) return@withPermit

                updateTaskSync(taskId) {
                    it.copy(status = TaskStatus.DOWNLOADING, progress = 5)
                }

                val totalSteps = 10
                for (step in 1..totalSteps) {
                    while (_uiState.value.isPaused) {
                        delay(300)
                    }

                    // Re-check existence and status
                    val current = repository.getTaskById(taskId)
                    if (current == null) return@withPermit

                    delay(160 + Random.nextLong(100))

                    val progress = (step * 10).coerceAtMost(100)
                    val speed = 320 + Random.nextInt(260)

                    updateTaskSync(taskId) { t ->
                        val downloaded = (progress * t.totalKhatians) / 100
                        t.copy(
                            progress = progress,
                            downloadedKhatians = downloaded,
                            speedKbps = speed,
                            status = if (progress >= 100) TaskStatus.COMPLETED else TaskStatus.DOWNLOADING
                        )
                    }
                }

                val finished = repository.getTaskById(taskId)
                if (finished != null && finished.status == TaskStatus.COMPLETED) {
                    // Update UI state with downloaded ID
                    _uiState.value = _uiState.value.copy(
                        downloadedMouzaIds = _uiState.value.downloadedMouzaIds + finished.mouzaId
                    )

                    // Physical file save
                    val currentDist = currentDistrict
                    val currentUpaz = currentUpazila
                    if (currentDist != null && currentUpaz != null) {
                        try {
                            LandRecordStorageManager.saveMouzaRecordFile(getApplication(), finished, currentDist, currentUpaz)
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                    // Save to Vault
                    val sizeMb = String.format("%.2f MB", finished.fileSizeBytes / (1024.0 * 1024.0))
                    repository.insertSavedRecord(
                        SavedRecord(
                            id = finished.id,
                            mouzaId = finished.mouzaId,
                            mouzaNameBn = finished.mouzaNameBn,
                            mouzaNameEn = finished.mouzaNameEn,
                            jlNo = finished.jlNo,
                            upazilaNameBn = finished.upazilaNameBn,
                            districtNameBn = finished.districtNameBn,
                            recordType = finished.recordTypeLabel,
                            khatianCount = finished.totalKhatians,
                            fileSizeFormatted = sizeMb
                        )
                    )

                    checkAndTriggerNextBatch()
                }
            }
        }
        taskJobs[taskId] = job
        job.invokeOnCompletion { taskJobs.remove(taskId) }
    }

    private suspend fun updateTaskSync(taskId: String, transform: (DownloadTask) -> DownloadTask) {
        val task = repository.getTaskById(taskId)
        if (task != null) {
            repository.updateTaskInQueue(transform(task))
        }
    }

    private fun checkAndTriggerNextBatch() {
        val state = _uiState.value
        val upazila = currentUpazila ?: return
        val district = currentDistrict ?: return

        val allQueuedDone = state.queue.isNotEmpty() && state.queue.all { it.status == TaskStatus.COMPLETED }
        if (!allQueuedDone) return

        val totalMouzas = state.targetTotalMouzas.coerceAtLeast(upazila.mouzas.size)
        val downloadedCount = state.downloadedMouzaIds.size

        if (downloadedCount < totalMouzas && state.isAutoBatchMode) {
            val remainingMouzas = upazila.mouzas.filter { it.id !in state.downloadedMouzaIds }
            val nextBatch = remainingMouzas.take(state.batchSize)

            if (nextBatch.isNotEmpty()) {
                val nextBatchNumber = state.currentBatchNumber + 1
                _uiState.value = _uiState.value.copy(
                    currentBatchNumber = nextBatchNumber,
                    selectedMouzaIds = nextBatch.map { it.id }.toSet()
                )
                enqueueBatch(nextBatch, district, upazila, nextBatchNumber)
                return
            }
        }

        if (downloadedCount >= totalMouzas || state.queue.all { it.status == TaskStatus.COMPLETED }) {
            compileMasterArchive(district, upazila)
        }
    }

    private fun compileMasterArchive(district: District, upazila: Upazila) {
        val queue = _uiState.value.queue
        val totalKhatians = queue.sumOf { it.totalKhatians }
        val totalBytes = queue.sumOf { it.fileSizeBytes }
        val sizeMb = String.format("%.2f MB", totalBytes / (1024.0 * 1024.0))
        val typesText = _uiState.value.selectedRecordTypes.joinToString("+") { it.code }
        val fileName = "${district.nameEn}_${upazila.nameEn}_Master_Land_Records_2026.json"

        val archive = MasterArchive(
            upazilaId = upazila.id,
            upazilaNameBn = upazila.nameBn,
            upazilaNameEn = upazila.nameEn,
            districtNameBn = district.nameBn,
            districtNameEn = district.nameEn,
            totalMouzasCount = _uiState.value.downloadedMouzaIds.size,
            recordTypesIncluded = typesText,
            totalKhatiansCount = totalKhatians,
            totalSizeMb = sizeMb,
            fileName = fileName,
            isReady = true,
            isDownloadedToPhone = false,
            phoneSavedPath = null
        )

        _uiState.value = _uiState.value.copy(masterArchive = archive)
    }

    fun downloadMasterFileToPhone(context: Context) {
        val archive = _uiState.value.masterArchive ?: return
        val upazila = currentUpazila ?: return
        val district = currentDistrict ?: return

        viewModelScope.launch {
            val saveResult = LandRecordStorageManager.saveMasterLandRecordsArchive(
                context = context,
                archive = archive,
                district = district,
                upazila = upazila,
                queueTasks = _uiState.value.queue
            )

            LandRecordStorageManager.clearCacheAndMemory(context)

            _uiState.value = _uiState.value.copy(
                masterArchive = archive.copy(
                    isDownloadedToPhone = true,
                    phoneSavedPath = saveResult.jsonPath,
                    textFileSavedPath = saveResult.textPath,
                    csvFileSavedPath = saveResult.csvPath,
                    savedFilesList = saveResult.allFiles,
                    fileContentPreview = saveResult.filePreview
                ),
                isShowingSaveSuccessDialog = true,
                cacheNotificationMessage = "Master & Mouza files saved to Download/LandRecords/! Cache cleared."
            )
        }
    }

    fun dismissSaveSuccessDialog() {
        _uiState.value = _uiState.value.copy(isShowingSaveSuccessDialog = false)
    }

    fun openDownloadsFolder(context: Context) {
        LandRecordStorageManager.openDownloadsFolder(context)
    }

    fun exportArchiveToSafUri(context: Context, uri: Uri) {
        val preview = _uiState.value.masterArchive?.fileContentPreview ?: ""
        viewModelScope.launch {
            val success = LandRecordStorageManager.writeToSafUri(context, uri, preview)
            if (success) {
                Toast.makeText(context, "Archive saved successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to save file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun togglePauseQueue() {
        val nextPaused = !_uiState.value.isPaused
        _uiState.value = _uiState.value.copy(isPaused = nextPaused)
        if (nextPaused) {
            DownloadWorkManagerHelper.cancelDownloadWork(getApplication())
            taskJobs.values.forEach { it.cancel() }
            taskJobs.clear()
            viewModelScope.launch {
                repository.resetInterruptedTasks()
            }
        } else {
            if (_uiState.value.useWorkManagerBackgroundService) {
                DownloadWorkManagerHelper.enqueueDownloadWork(
                    context = getApplication(),
                    replace = true,
                    autoBatchMode = _uiState.value.isAutoBatchMode
                )
            }
        }
    }

    fun clearQueue() {
        DownloadWorkManagerHelper.cancelDownloadWork(getApplication())
        taskJobs.values.forEach { it.cancel() }
        taskJobs.clear()
        _uiState.value = _uiState.value.copy(isPaused = false, queue = emptyList())
        viewModelScope.launch {
            repository.clearQueue()
        }
    }

    fun setUseWorkManagerBackgroundService(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(useWorkManagerBackgroundService = enabled)
        if (enabled) {
            taskJobs.values.forEach { it.cancel() }
            taskJobs.clear()
            if (!_uiState.value.isPaused && _uiState.value.queue.any { it.status == TaskStatus.PENDING }) {
                DownloadWorkManagerHelper.enqueueDownloadWork(
                    context = getApplication(),
                    replace = true,
                    autoBatchMode = _uiState.value.isAutoBatchMode
                )
            }
        } else {
            DownloadWorkManagerHelper.cancelDownloadWork(getApplication())
            if (!_uiState.value.isPaused) {
                _uiState.value.queue.filter { it.status == TaskStatus.PENDING }.forEach { task ->
                    if (!taskJobs.containsKey(task.id)) {
                        launchTaskWorker(task.id)
                    }
                }
            }
        }
    }

    fun startBackgroundWorkManagerManually() {
        if (_uiState.value.isPaused) {
            _uiState.value = _uiState.value.copy(isPaused = false)
        }
        DownloadWorkManagerHelper.enqueueDownloadWork(
            context = getApplication(),
            replace = true,
            autoBatchMode = _uiState.value.isAutoBatchMode
        )
    }

    fun deleteSavedRecord(id: String) {
        viewModelScope.launch {
            repository.deleteSavedRecord(id)
        }
    }

    fun removeTaskFromQueue(taskId: String) {
        viewModelScope.launch {
            repository.deleteTaskFromQueue(taskId)
            _uiState.value = _uiState.value.copy(
                queue = _uiState.value.queue.filter { it.id != taskId }
            )
        }
    }

    fun retryTask(taskId: String) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            if (task != null) {
                val resetTask = task.copy(
                    status = TaskStatus.PENDING,
                    progress = 0,
                    downloadedKhatians = 0
                )
                repository.updateTaskInQueue(resetTask)
                if (_uiState.value.useWorkManagerBackgroundService && !_uiState.value.isPaused) {
                    DownloadWorkManagerHelper.enqueueDownloadWork(
                        context = getApplication(),
                        replace = true,
                        autoBatchMode = _uiState.value.isAutoBatchMode
                    )
                }
            }
        }
    }

    fun retryAllFailed() {
        viewModelScope.launch {
            val failedTasks = _uiState.value.queue.filter { it.status == TaskStatus.FAILED }
            failedTasks.forEach { task ->
                val reset = task.copy(status = TaskStatus.PENDING, progress = 0, downloadedKhatians = 0)
                repository.updateTaskInQueue(reset)
            }
            if (_uiState.value.useWorkManagerBackgroundService && !_uiState.value.isPaused) {
                DownloadWorkManagerHelper.enqueueDownloadWork(
                    context = getApplication(),
                    replace = true,
                    autoBatchMode = _uiState.value.isAutoBatchMode
                )
            }
        }
    }

    fun clearAllSavedRecords() {
        viewModelScope.launch {
            repository.clearSavedRecords()
        }
    }

    fun dismissCacheNotification() {
        _uiState.value = _uiState.value.copy(cacheNotificationMessage = null)
    }

    fun navigateToScreen(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }
}
