package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.data.geography.BangladeshGeographyCatalog
import com.example.data.geography.DistrictEntity
import com.example.data.geography.DivisionEntity
import com.example.data.geography.GeoVerificationStatus
import com.example.data.geography.GeographyCoverageSummary
import com.example.data.geography.GeographyDataSourceEntity
import com.example.data.geography.GeographyImportHistoryEntity
import com.example.data.geography.GeographyRepository
import com.example.data.geography.MouzaEntity
import com.example.data.geography.UpazilaEntity
import com.example.data.ledger.DocumentVerificationStatus
import com.example.data.ledger.MasterLedgerEntryEntity
import com.example.data.local.AppDatabase
import com.example.data.local.LandRecordStorageManager
import com.example.data.model.District
import com.example.data.model.DownloadFormat
import com.example.data.model.DownloadTask
import com.example.data.model.LandRecordsDataset
import com.example.data.model.MasterArchive
import com.example.data.model.Mouza
import com.example.data.model.PlannedQueueSegment
import com.example.data.model.RecordType
import com.example.data.model.RetrievalMode
import com.example.data.model.SavedRecord
import com.example.data.model.TaskStatus
import com.example.data.model.Upazila
import com.example.data.model.UpazilaFetchInfo
import com.example.data.repository.DownloadRepository
import com.example.data.worker.DownloadWorkManagerHelper
import com.example.util.DeviceNetworkDependencyProfile
import com.example.util.DocumentVerificationEngine
import com.example.util.NetworkDeviceQueueAdvisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
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
    SAVED_VAULT,
    GEOGRAPHIC_EXPLORER,
    MASTER_LEDGER,
    MANUAL_ASSISTANT
}

data class LRUiState(
    val selectedDivisionId: String = "chattogram",
    val selectedDistrictId: String = "cumilla",
    val selectedUpazilaId: String? = null,
    val selectedUpazilaIds: Set<String> = emptySet(),
    val isFetchingUpazila: Boolean = false,
    val upazilaFetchInfo: UpazilaFetchInfo? = null,
    val targetTotalMouzas: Int = 0,
    val selectedMouzaIds: Set<String> = emptySet(),
    val downloadedMouzaIds: Set<String> = emptySet(),
    val selectedRecordTypes: Set<RecordType> = RecordType.values().toSet(),
    val selectedLandRecordsCount: Int = 0,
    val selectedDownloadFormat: DownloadFormat = DownloadFormat.PDF,
    val selectedRetrievalMode: RetrievalMode = RetrievalMode.MODE_A_AUTHORIZED_API,
    val filterVerifiedOnly: Boolean = false,
    val searchQuery: String = "",
    val ledgerSearchQuery: String = "",
    val queue: List<DownloadTask> = emptyList(),
    val plannedQueues: List<PlannedQueueSegment> = emptyList(),
    val deviceNetworkProfile: DeviceNetworkDependencyProfile? = null,
    val geographyCoverageSummary: GeographyCoverageSummary = GeographyCoverageSummary(
        totalDivisions = 8,
        totalDistricts = 64,
        totalUpazilas = BangladeshGeographyCatalog.upazilas.size,
        totalVerifiedMouzas = 0
    ),
    val isPaused: Boolean = false,
    val isAutoBatchMode: Boolean = true,
    val isAutoQueueOnUpazilaSelect: Boolean = true,
    val currentBatchNumber: Int = 1,
    val batchSize: Int = 7,
    val masterArchive: MasterArchive? = null,
    val isShowingHistoryDialog: Boolean = false,
    val isShowingLedgerDialog: Boolean = false,
    val isShowingSaveSuccessDialog: Boolean = false,
    val isShowingApkInfoDialog: Boolean = false,
    val isShowingPreQueueConfirmationDialog: Boolean = false,
    val isShowingGeoUpdateDialog: Boolean = false,
    val isShowingPdfImportDialog: Boolean = false,
    val lastImportStatusMessage: String? = null,
    val cacheNotificationMessage: String? = null,
    val isWorkManagerActive: Boolean = false,
    val activeWorkInfo: WorkInfo? = null,
    val useWorkManagerBackgroundService: Boolean = true,
    val antiBlockingDelayDescription: String = "400ms – 2,400ms Randomized Jitter",
    val activityLog: List<String> = emptyList(),
    val currentScreen: AppScreen = AppScreen.DOWNLOADER
)

class LRDownloaderViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val DEFAULT_MOUZAS_PER_BATCH = 7

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

    private val db = AppDatabase.getDatabase(application)
    private val repository = DownloadRepository(db.savedRecordDao(), db.downloadQueueDao())
    val geographyRepository = GeographyRepository(db.geographyDao())
    private val masterLandLedgerDao = db.masterLandLedgerDao()
    private val upazilaFetchInfoDao = db.upazilaFetchInfoDao()

    val savedRecords: StateFlow<List<SavedRecord>> = repository.savedRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val masterLedgerEntries: StateFlow<List<MasterLedgerEntryEntity>> = masterLandLedgerDao.observeAllEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val geographyDataSources: StateFlow<List<GeographyDataSourceEntity>> = geographyRepository.dataSources
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val geographyImportHistory: StateFlow<List<GeographyImportHistoryEntity>> = geographyRepository.importHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val initialProfile = NetworkDeviceQueueAdvisor.analyze(application)

    private val _uiState = MutableStateFlow(
        LRUiState(
            deviceNetworkProfile = initialProfile,
            batchSize = DEFAULT_MOUZAS_PER_BATCH
        )
    )
    val uiState: StateFlow<LRUiState> = _uiState.asStateFlow()

    private var queueCoordinatorJob: Job? = null
    private val taskJobs = mutableMapOf<String, Job>()

    /**
     * All 8 Administrative Divisions of Bangladesh.
     */
    val allDivisions: List<DivisionEntity> = BangladeshGeographyCatalog.divisions

    /**
     * All 64 Official Districts of Bangladesh.
     */
    val allNationalDistricts: List<DistrictEntity> = BangladeshGeographyCatalog.districts

    /**
     * Full in-memory + Room synchronized District models for all 64 Districts of Bangladesh.
     */
    val allDistricts: List<District> by lazy {
        val baseMap = LandRecordsDataset.districts.associateBy { it.id }
        val upazilasByDistrict = BangladeshGeographyCatalog.upazilas.groupBy { it.districtId }
        val seedMouzasByUpazila = BangladeshGeographyCatalog.buildInitialMouzaBundle().mouzas.groupBy { it.upazilaId }

        BangladeshGeographyCatalog.districts.map { distEntity ->
            val existing = baseMap[distEntity.id]
            if (existing != null) {
                existing.copy(
                    divisionId = distEntity.divisionId,
                    officialCode = distEntity.officialCode
                )
            } else {
                val upaEntities = upazilasByDistrict[distEntity.id].orEmpty()
                val upaModels = upaEntities.map { upa ->
                    val mouzaEntities = seedMouzasByUpazila[upa.id].orEmpty()
                    Upazila(
                        id = upa.id,
                        nameBn = upa.nameBn,
                        nameEn = upa.nameEn,
                        districtId = distEntity.id,
                        officialCode = upa.officialCode,
                        mouzas = mouzaEntities.map { m ->
                            Mouza(
                                id = m.id,
                                nameBn = m.nameBn,
                                nameEn = m.nameEn,
                                jlNo = m.jlNumber,
                                upazilaId = upa.id,
                                officialMouzaCode = m.officialMouzaCode,
                                sourceReference = m.sourceReference,
                                verificationStatus = m.verificationStatus
                            )
                        }
                    )
                }
                District(
                    id = distEntity.id,
                    nameBn = distEntity.nameBn,
                    nameEn = distEntity.nameEn,
                    upazilas = upaModels,
                    divisionId = distEntity.divisionId,
                    officialCode = distEntity.officialCode
                )
            }
        }
    }

    init {
        // 1. Seed the normalized 8-Division, 64-District, Upazila, and Mouza Room tables on startup
        viewModelScope.launch {
            geographyRepository.ensureSeeded()
            refreshGeographyCoverageSummary()
        }

        // 2. Observe WorkManager active state
        DownloadWorkManagerHelper.getPrimaryWorkInfoFlow(application).onEach { workInfo ->
            val isRunning = workInfo?.state == WorkInfo.State.RUNNING || workInfo?.state == WorkInfo.State.ENQUEUED
            _uiState.value = _uiState.value.copy(
                activeWorkInfo = workInfo,
                isWorkManagerActive = isRunning
            )
        }.launchIn(viewModelScope)

        // 3. Persistent Queue State Recovery
        upazilaFetchInfoDao.getLatestFetchInfo().onEach { info ->
            if (info != null && _uiState.value.upazilaFetchInfo == null) {
                _uiState.value = _uiState.value.copy(
                    upazilaFetchInfo = info,
                    selectedUpazilaId = info.upazilaId,
                    selectedUpazilaIds = setOf(info.upazilaId),
                    targetTotalMouzas = info.totalMouzaCount,
                    selectedLandRecordsCount = info.totalLandRecordsCount,
                    batchSize = info.mouzasPerQueue
                )
                refreshGeographyCoverageSummary()
            }
        }.launchIn(viewModelScope)

        // 4. Persistent Queue Sync & Sequential Multi-Queue Coordinator
        viewModelScope.launch {
            repository.resetInterruptedTasks()

            repository.downloadQueue.onEach { persistentQueue ->
                val completedIds = persistentQueue
                    .filter { it.status == TaskStatus.COMPLETED }
                    .map { it.mouzaId }
                    .toSet()

                val activeBatchNum = persistentQueue
                    .filter { it.status == TaskStatus.DOWNLOADING || it.status == TaskStatus.PENDING }
                    .minOfOrNull { it.batchNumber }
                    ?: persistentQueue.maxOfOrNull { it.batchNumber }
                    ?: _uiState.value.currentBatchNumber

                val updatedPlannedQueues = computePlannedQueues(
                    mouzasPool = currentMouzas,
                    selectedMouzaIds = _uiState.value.selectedMouzaIds,
                    recordTypes = _uiState.value.selectedRecordTypes,
                    queueSize = _uiState.value.batchSize,
                    activeTasks = persistentQueue
                )

                _uiState.value = _uiState.value.copy(
                    queue = persistentQueue,
                    downloadedMouzaIds = completedIds,
                    currentBatchNumber = activeBatchNum,
                    plannedQueues = updatedPlannedQueues
                )

                if (!_uiState.value.isPaused && persistentQueue.any { it.status == TaskStatus.PENDING }) {
                    startSequentialQueueCoordinator()
                }
            }.launchIn(viewModelScope)
        }
    }

    val currentDivision: DivisionEntity?
        get() = allDivisions.find { it.id == _uiState.value.selectedDivisionId }

    val districtsInSelectedDivision: List<District>
        get() = allDistricts.filter { it.divisionId == _uiState.value.selectedDivisionId }

    val currentDistrict: District?
        get() = allDistricts.find { it.id == _uiState.value.selectedDistrictId }

    val currentUpazilas: List<Upazila>
        get() = currentDistrict?.upazilas ?: emptyList()

    val currentUpazila: Upazila?
        get() = currentUpazilas.find { it.id == _uiState.value.selectedUpazilaId }
            ?: currentUpazilas.firstOrNull { it.id in _uiState.value.selectedUpazilaIds }

    /**
     * Returns all verified Mouzas across all selected Upazilas (supports single Upazila or Select All Upazilas).
     */
    val currentMouzas: List<Mouza>
        get() {
            val state = _uiState.value
            val activeUpazilaIds = when {
                state.selectedUpazilaIds.isNotEmpty() -> state.selectedUpazilaIds
                state.selectedUpazilaId != null -> setOf(state.selectedUpazilaId)
                else -> emptySet()
            }
            if (activeUpazilaIds.isEmpty()) return emptyList()
            val raw = currentUpazilas
                .filter { it.id in activeUpazilaIds }
                .flatMap { upa ->
                    upa.mouzas.map { m ->
                        if (m.upazilaId.isEmpty()) m.copy(upazilaId = upa.id) else m
                    }
                }
                .distinctBy { it.id }

            return if (state.filterVerifiedOnly) {
                raw.filter { it.verificationStatus == GeoVerificationStatus.VERIFIED }
            } else {
                raw
            }
        }

    val totalBatches: Int
        get() {
            val fromPlanned = _uiState.value.plannedQueues.size
            if (fromPlanned > 0) return fromPlanned
            val fromQueue = _uiState.value.queue.maxOfOrNull { it.batchNumber } ?: 0
            if (fromQueue > 0) return fromQueue
            val total = _uiState.value.targetTotalMouzas.coerceAtLeast(currentMouzas.size)
            if (total == 0) return 0
            val size = _uiState.value.batchSize.coerceAtLeast(1)
            return (total + size - 1) / size
        }

    private fun appendActivityLog(event: String) {
        val current = _uiState.value.activityLog
        val updated = (listOf("[${System.currentTimeMillis() % 100000}] $event") + current).take(50)
        _uiState.value = _uiState.value.copy(activityLog = updated)
    }

    fun refreshGeographyCoverageSummary() {
        viewModelScope.launch {
            val state = _uiState.value
            val mouzaEntities = currentMouzas.map { m ->
                MouzaEntity(
                    id = m.id,
                    upazilaId = m.upazilaId.ifEmpty { state.selectedUpazilaId ?: "" },
                    nameEn = m.nameEn,
                    nameBn = m.nameBn,
                    jlNumber = m.jlNo,
                    officialMouzaCode = m.officialMouzaCode,
                    sourceReference = m.sourceReference,
                    verificationStatus = m.verificationStatus,
                    lastVerifiedAt = System.currentTimeMillis()
                )
            }
            val summary = geographyRepository.getCoverageSummary(
                selectedDistrictIds = setOf(state.selectedDistrictId),
                selectedUpazilaIds = state.selectedUpazilaIds.ifEmpty {
                    state.selectedUpazilaId?.let { setOf(it) } ?: emptySet()
                },
                loadedMouzas = mouzaEntities,
                selectedMouzaIds = state.selectedMouzaIds,
                selectedSurveyCount = state.selectedRecordTypes.size
            )
            _uiState.value = _uiState.value.copy(geographyCoverageSummary = summary)
        }
    }

    fun refreshDeviceNetworkProfile() {
        val profile = NetworkDeviceQueueAdvisor.analyze(getApplication())
        val recordTypes = _uiState.value.selectedRecordTypes
        val newBatchSize = DEFAULT_MOUZAS_PER_BATCH
        val planned = computePlannedQueues(
            mouzasPool = currentMouzas,
            selectedMouzaIds = _uiState.value.selectedMouzaIds,
            recordTypes = recordTypes,
            queueSize = newBatchSize,
            activeTasks = _uiState.value.queue
        )
        _uiState.value = _uiState.value.copy(
            deviceNetworkProfile = profile,
            batchSize = newBatchSize,
            plannedQueues = planned
        )
    }

    fun setAutoQueueOnUpazilaSelect(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAutoQueueOnUpazilaSelect = enabled)
    }

    fun setRetrievalMode(mode: RetrievalMode) {
        _uiState.value = _uiState.value.copy(selectedRetrievalMode = mode)
        appendActivityLog("Switched retrieval mode to ${mode.labelEn}")
    }

    fun setFilterVerifiedOnly(verifiedOnly: Boolean) {
        _uiState.value = _uiState.value.copy(filterVerifiedOnly = verifiedOnly)
        refreshGeographyCoverageSummary()
    }

    /**
     * Step 1: Select Division (loads all verified Districts in that Division).
     */
    fun onDivisionChanged(newDivisionId: String) {
        val firstDistrictInDiv = allDistricts.firstOrNull { it.divisionId == newDivisionId }
        _uiState.value = _uiState.value.copy(
            selectedDivisionId = newDivisionId
        )
        if (firstDistrictInDiv != null) {
            onDistrictChanged(firstDistrictInDiv.id)
        }
    }

    /**
     * Step 2: Select District (loads all Upazilas belonging to that District).
     */
    fun onDistrictChanged(newDistrictId: String) {
        val oldDistrictId = _uiState.value.selectedDistrictId
        val shouldClear = oldDistrictId != "cumilla" && oldDistrictId != "brahmanbaria"
        if (shouldClear) {
            clearQueue()
        }
        val targetDistrict = allDistricts.find { it.id == newDistrictId }
        val resolvedDivisionId = targetDistrict?.divisionId ?: _uiState.value.selectedDivisionId
        val profile = NetworkDeviceQueueAdvisor.analyze(getApplication())

        _uiState.value = _uiState.value.copy(
            selectedDivisionId = resolvedDivisionId,
            selectedDistrictId = newDistrictId,
            selectedUpazilaId = null,
            selectedUpazilaIds = emptySet(),
            isFetchingUpazila = false,
            upazilaFetchInfo = null,
            targetTotalMouzas = 0,
            selectedMouzaIds = emptySet(),
            downloadedMouzaIds = emptySet(),
            selectedRecordTypes = RecordType.values().toSet(),
            selectedLandRecordsCount = 0,
            plannedQueues = emptyList(),
            deviceNetworkProfile = profile,
            batchSize = DEFAULT_MOUZAS_PER_BATCH,
            currentBatchNumber = 1,
            masterArchive = null,
            searchQuery = "",
            cacheNotificationMessage = null
        )
        refreshGeographyCoverageSummary()
    }

    /**
     * Step 3 (Multi-Upazila): Select All Upazilas in the selected District and automatically load
     * and select all their verified Mouzas.
     */
    fun selectAllUpazilasInDistrict(autoSelectAllMouzas: Boolean = true) {
        val district = currentDistrict ?: return
        val allUpaIds = district.upazilas.map { it.id }.toSet()
        if (allUpaIds.isEmpty()) return

        val allMouzas = district.upazilas
            .flatMap { upa -> upa.mouzas.map { it.copy(upazilaId = upa.id) } }
            .distinctBy { it.id }

        val selectedIds = if (autoSelectAllMouzas) allMouzas.map { it.id }.toSet() else emptySet()
        val allRecordTypes = RecordType.values().toSet()
        val totalLandRecords = allMouzas.filter { it.id in selectedIds }.sumOf { it.totalKhatiansFor(allRecordTypes) }
        val batchCount = (allMouzas.size + DEFAULT_MOUZAS_PER_BATCH - 1) / DEFAULT_MOUZAS_PER_BATCH
        val profile = NetworkDeviceQueueAdvisor.analyze(getApplication())

        val planned = computePlannedQueues(
            mouzasPool = allMouzas,
            selectedMouzaIds = selectedIds,
            recordTypes = allRecordTypes,
            queueSize = DEFAULT_MOUZAS_PER_BATCH,
            activeTasks = emptyList()
        )

        val fetchInfo = UpazilaFetchInfo(
            upazilaId = district.upazilas.first().id,
            upazilaNameBn = "সকল উপজেলা (${allUpaIds.size}টি)",
            upazilaNameEn = "All ${allUpaIds.size} Upazilas (${district.nameEn})",
            districtNameBn = district.nameBn,
            totalMouzaCount = allMouzas.size,
            batchCount = batchCount,
            message = "Selected All ${allUpaIds.size} Upazilas & ${selectedIds.size} Verified Mouzas across $batchCount Batches (7 Mouzas/Batch).",
            totalLandRecordsCount = totalLandRecords,
            mouzasPerQueue = DEFAULT_MOUZAS_PER_BATCH,
            networkProfileLabel = profile.networkTier.labelEn,
            deviceProfileLabel = "${profile.deviceTier.labelEn} (${profile.cpuCores} Cores, ${profile.totalRamMb}MB RAM)"
        )

        _uiState.value = _uiState.value.copy(
            selectedUpazilaId = district.upazilas.first().id,
            selectedUpazilaIds = allUpaIds,
            upazilaFetchInfo = fetchInfo,
            targetTotalMouzas = allMouzas.size,
            selectedMouzaIds = selectedIds,
            selectedRecordTypes = allRecordTypes,
            selectedLandRecordsCount = totalLandRecords,
            plannedQueues = planned,
            batchSize = DEFAULT_MOUZAS_PER_BATCH,
            currentBatchNumber = 1
        )
        viewModelScope.launch {
            upazilaFetchInfoDao.clearFetchInfo()
            upazilaFetchInfoDao.insertFetchInfo(fetchInfo)
        }
        refreshGeographyCoverageSummary()
    }

    /**
     * Deselects all Upazilas and clears the Mouza selection.
     */
    fun deselectAllUpazilasInDistrict() {
        _uiState.value = _uiState.value.copy(
            selectedUpazilaId = null,
            selectedUpazilaIds = emptySet(),
            upazilaFetchInfo = null,
            targetTotalMouzas = 0,
            selectedMouzaIds = emptySet(),
            selectedLandRecordsCount = 0,
            plannedQueues = emptyList()
        )
        refreshGeographyCoverageSummary()
    }

    /**
     * Toggles an individual Upazila in multi-Upazila selection mode and automatically loads all its Mouzas.
     */
    fun toggleUpazilaSelection(upazilaId: String) {
        val currentIds = _uiState.value.selectedUpazilaIds.ifEmpty {
            _uiState.value.selectedUpazilaId?.let { setOf(it) } ?: emptySet()
        }
        val nextUpazilaIds = if (upazilaId in currentIds) {
            currentIds - upazilaId
        } else {
            currentIds + upazilaId
        }

        if (nextUpazilaIds.isEmpty()) {
            deselectAllUpazilasInDistrict()
            return
        }

        val district = currentDistrict ?: return
        val selectedUpazilas = district.upazilas.filter { it.id in nextUpazilaIds }
        val combinedMouzas = selectedUpazilas
            .flatMap { upa -> upa.mouzas.map { it.copy(upazilaId = upa.id) } }
            .distinctBy { it.id }

        val allMouzaIds = combinedMouzas.map { it.id }.toSet()
        val recordTypes = _uiState.value.selectedRecordTypes.ifEmpty { RecordType.values().toSet() }
        val totalLandRecords = combinedMouzas.sumOf { it.totalKhatiansFor(recordTypes) }
        val batchCount = (combinedMouzas.size + DEFAULT_MOUZAS_PER_BATCH - 1) / DEFAULT_MOUZAS_PER_BATCH
        val profile = NetworkDeviceQueueAdvisor.analyze(getApplication())

        val planned = computePlannedQueues(
            mouzasPool = combinedMouzas,
            selectedMouzaIds = allMouzaIds,
            recordTypes = recordTypes,
            queueSize = DEFAULT_MOUZAS_PER_BATCH,
            activeTasks = _uiState.value.queue
        )

        val primaryUpa = selectedUpazilas.first()
        val labelBn = if (selectedUpazilas.size == 1) primaryUpa.nameBn else "${selectedUpazilas.size}টি নির্বাচিত উপজেলা"
        val labelEn = if (selectedUpazilas.size == 1) primaryUpa.nameEn else "${selectedUpazilas.size} Selected Upazilas"

        val fetchInfo = UpazilaFetchInfo(
            upazilaId = primaryUpa.id,
            upazilaNameBn = labelBn,
            upazilaNameEn = labelEn,
            districtNameBn = district.nameBn,
            totalMouzaCount = combinedMouzas.size,
            batchCount = batchCount,
            message = "Loaded ${combinedMouzas.size} Verified Mouzas from ${selectedUpazilas.size} Upazila(s) in $batchCount Batches of 7.",
            totalLandRecordsCount = totalLandRecords,
            mouzasPerQueue = DEFAULT_MOUZAS_PER_BATCH,
            networkProfileLabel = profile.networkTier.labelEn,
            deviceProfileLabel = "${profile.deviceTier.labelEn} (${profile.cpuCores} Cores, ${profile.totalRamMb}MB RAM)"
        )

        _uiState.value = _uiState.value.copy(
            selectedUpazilaId = primaryUpa.id,
            selectedUpazilaIds = nextUpazilaIds,
            upazilaFetchInfo = fetchInfo,
            targetTotalMouzas = combinedMouzas.size,
            selectedMouzaIds = allMouzaIds,
            selectedLandRecordsCount = totalLandRecords,
            plannedQueues = planned,
            batchSize = DEFAULT_MOUZAS_PER_BATCH
        )
        viewModelScope.launch {
            upazilaFetchInfoDao.clearFetchInfo()
            upazilaFetchInfoDao.insertFetchInfo(fetchInfo)
        }
        refreshGeographyCoverageSummary()
    }

    /**
     * Immediately selects ALL Mouzas of the selected Upazila, ALL Land Record types,
     * and divides them into 7-Mouza batches.
     */
    fun onUpazilaChanged(newUpazilaId: String?, autoStartQueues: Boolean = false) {
        clearQueue()
        val profile = NetworkDeviceQueueAdvisor.analyze(getApplication())
        val queueSize = DEFAULT_MOUZAS_PER_BATCH

        if (newUpazilaId == null) {
            _uiState.value = _uiState.value.copy(
                selectedUpazilaId = null,
                selectedUpazilaIds = emptySet(),
                isFetchingUpazila = false,
                upazilaFetchInfo = null,
                targetTotalMouzas = 0,
                selectedMouzaIds = emptySet(),
                downloadedMouzaIds = emptySet(),
                selectedRecordTypes = RecordType.values().toSet(),
                selectedLandRecordsCount = 0,
                plannedQueues = emptyList(),
                deviceNetworkProfile = profile,
                batchSize = queueSize,
                currentBatchNumber = 1,
                masterArchive = null,
                searchQuery = "",
                cacheNotificationMessage = null
            )
            refreshGeographyCoverageSummary()
            viewModelScope.launch {
                upazilaFetchInfoDao.clearFetchInfo()
            }
            return
        }

        val upazila = currentUpazilas.find { it.id == newUpazilaId }
        val district = currentDistrict

        if (upazila != null && district != null) {
            val foundMouzaCount = upazila.mouzas.size
            val allMouzaIds = upazila.mouzas.map { it.id }.toSet()
            val allRecordTypes = RecordType.values().toSet()
            val totalLandRecords = upazila.totalLandRecordsCount(allRecordTypes)
            val totalQueuesCount = (foundMouzaCount + queueSize - 1) / queueSize

            val planned = computePlannedQueues(
                mouzasPool = upazila.mouzas,
                selectedMouzaIds = allMouzaIds,
                recordTypes = allRecordTypes,
                queueSize = queueSize,
                activeTasks = emptyList()
            )

            val fetchInfo = UpazilaFetchInfo(
                upazilaId = upazila.id,
                upazilaNameBn = upazila.nameBn,
                upazilaNameEn = upazila.nameEn,
                districtNameBn = district.nameBn,
                totalMouzaCount = foundMouzaCount,
                batchCount = totalQueuesCount,
                message = "Auto-Selected All $foundMouzaCount Mouzas & All $totalLandRecords Land Records (${allRecordTypes.size} Survey Types) across $totalQueuesCount Sequential Queues (7 Mouzas/Batch).",
                totalLandRecordsCount = totalLandRecords,
                mouzasPerQueue = queueSize,
                networkProfileLabel = profile.networkTier.labelEn,
                deviceProfileLabel = "${profile.deviceTier.labelEn} (${profile.cpuCores} Cores, ${profile.totalRamMb}MB RAM)"
            )

            _uiState.value = _uiState.value.copy(
                selectedUpazilaId = newUpazilaId,
                selectedUpazilaIds = setOf(newUpazilaId),
                isFetchingUpazila = false,
                upazilaFetchInfo = fetchInfo,
                targetTotalMouzas = foundMouzaCount,
                selectedMouzaIds = allMouzaIds,
                downloadedMouzaIds = emptySet(),
                selectedRecordTypes = allRecordTypes,
                selectedLandRecordsCount = totalLandRecords,
                plannedQueues = planned,
                deviceNetworkProfile = profile,
                batchSize = queueSize,
                currentBatchNumber = 1,
                masterArchive = null,
                searchQuery = "",
                cacheNotificationMessage = null
            )

            viewModelScope.launch {
                upazilaFetchInfoDao.clearFetchInfo()
                upazilaFetchInfoDao.insertFetchInfo(fetchInfo)
            }

            refreshGeographyCoverageSummary()

            if (autoStartQueues) {
                queueSelectedDownloads()
            }
        }
    }

    private fun recalculateSelectedLandRecords(
        selectedMouzaIds: Set<String>,
        selectedRecordTypes: Set<RecordType>
    ): Int {
        return currentMouzas
            .filter { it.id in selectedMouzaIds }
            .sumOf { it.totalKhatiansFor(selectedRecordTypes) }
    }

    private fun computePlannedQueues(
        mouzasPool: List<Mouza>,
        selectedMouzaIds: Set<String>,
        recordTypes: Set<RecordType>,
        queueSize: Int,
        activeTasks: List<DownloadTask>
    ): List<PlannedQueueSegment> {
        // Phase 2: Strictly enforce 7 mouzas per batch
        val effectiveSize = DEFAULT_MOUZAS_PER_BATCH

        if (activeTasks.isNotEmpty()) {
            val grouped = activeTasks.groupBy { it.batchNumber }.toSortedMap()
            val totalQueues = grouped.size
            return grouped.entries.map { (batchNum, tasksInQueue) ->
                val completedCount = tasksInQueue.count { it.status == TaskStatus.COMPLETED }
                val activeCount = tasksInQueue.count { it.status == TaskStatus.DOWNLOADING }
                val failedCount = tasksInQueue.count { it.status == TaskStatus.FAILED }
                val progressAvg = if (tasksInQueue.isEmpty()) 0 else tasksInQueue.sumOf { it.progress } / tasksInQueue.size

                val queueStatus = when {
                    completedCount == tasksInQueue.size -> TaskStatus.COMPLETED
                    activeCount > 0 || (completedCount > 0 && completedCount < tasksInQueue.size) -> TaskStatus.DOWNLOADING
                    failedCount > 0 -> TaskStatus.FAILED
                    else -> TaskStatus.PENDING
                }

                val previewNames = tasksInQueue.take(3).joinToString(", ") { it.mouzaNameBn } +
                    if (tasksInQueue.size > 3) " +${tasksInQueue.size - 3}টি" else ""

                val firstJl = tasksInQueue.firstOrNull()?.jlNo ?: ""
                val lastJl = tasksInQueue.lastOrNull()?.jlNo ?: ""

                PlannedQueueSegment(
                    queueNumber = batchNum,
                    totalQueues = totalQueues,
                    mouzaIds = tasksInQueue.map { it.mouzaId },
                    mouzaNamesPreviewBn = previewNames,
                    mouzaRangeLabel = "JL $firstJl – $lastJl",
                    mouzaCount = tasksInQueue.size,
                    totalKhatiansCount = tasksInQueue.sumOf { it.totalKhatians },
                    completedMouzasCount = completedCount,
                    activeMouzasCount = activeCount,
                    progressPercent = progressAvg,
                    status = queueStatus
                )
            }
        }

        if (mouzasPool.isEmpty()) return emptyList()

        val mouzasToPlan = if (selectedMouzaIds.isNotEmpty()) {
            mouzasPool.filter { it.id in selectedMouzaIds }
        } else {
            mouzasPool
        }.distinctBy { it.id }

        if (mouzasToPlan.isEmpty()) return emptyList()
        val chunks = mouzasToPlan.chunked(effectiveSize)
        val totalQueues = chunks.size

        return chunks.mapIndexed { idx, chunk ->
            val previewNames = chunk.take(3).joinToString(", ") { it.nameBn } +
                if (chunk.size > 3) " +${chunk.size - 3}টি" else ""
            val firstJl = chunk.firstOrNull()?.jlNo ?: ""
            val lastJl = chunk.lastOrNull()?.jlNo ?: ""

            PlannedQueueSegment(
                queueNumber = idx + 1,
                totalQueues = totalQueues,
                mouzaIds = chunk.map { it.id },
                mouzaNamesPreviewBn = previewNames,
                mouzaRangeLabel = "JL $firstJl – $lastJl",
                mouzaCount = chunk.size,
                totalKhatiansCount = chunk.sumOf { it.totalKhatiansFor(recordTypes) },
                completedMouzasCount = 0,
                activeMouzasCount = 0,
                progressPercent = 0,
                status = TaskStatus.PENDING
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
        val newLandRecordsCount = recalculateSelectedLandRecords(_uiState.value.selectedMouzaIds, newSet)
        val updatedPlanned = computePlannedQueues(
            mouzasPool = currentMouzas,
            selectedMouzaIds = _uiState.value.selectedMouzaIds,
            recordTypes = newSet,
            queueSize = _uiState.value.batchSize,
            activeTasks = _uiState.value.queue
        )
        _uiState.value = _uiState.value.copy(
            selectedRecordTypes = newSet,
            selectedLandRecordsCount = newLandRecordsCount,
            plannedQueues = updatedPlanned
        )
        refreshGeographyCoverageSummary()
    }

    fun setDownloadFormat(format: DownloadFormat) {
        _uiState.value = _uiState.value.copy(selectedDownloadFormat = format)
    }

    fun selectAllRecordTypes() {
        val allTypes = RecordType.values().toSet()
        val newLandRecordsCount = recalculateSelectedLandRecords(_uiState.value.selectedMouzaIds, allTypes)
        val updatedPlanned = computePlannedQueues(
            mouzasPool = currentMouzas,
            selectedMouzaIds = _uiState.value.selectedMouzaIds,
            recordTypes = allTypes,
            queueSize = _uiState.value.batchSize,
            activeTasks = _uiState.value.queue
        )
        _uiState.value = _uiState.value.copy(
            selectedRecordTypes = allTypes,
            selectedLandRecordsCount = newLandRecordsCount,
            plannedQueues = updatedPlanned
        )
        refreshGeographyCoverageSummary()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onLedgerSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(ledgerSearchQuery = query)
    }

    fun toggleMouza(mouzaId: String) {
        val currentSet = _uiState.value.selectedMouzaIds
        val newSet = if (currentSet.contains(mouzaId)) {
            currentSet - mouzaId
        } else {
            currentSet + mouzaId
        }
        val newLandRecordsCount = recalculateSelectedLandRecords(newSet, _uiState.value.selectedRecordTypes)
        val updatedPlanned = computePlannedQueues(
            mouzasPool = currentMouzas,
            selectedMouzaIds = newSet,
            recordTypes = _uiState.value.selectedRecordTypes,
            queueSize = _uiState.value.batchSize,
            activeTasks = _uiState.value.queue
        )
        _uiState.value = _uiState.value.copy(
            selectedMouzaIds = newSet,
            selectedLandRecordsCount = newLandRecordsCount,
            plannedQueues = updatedPlanned
        )
        refreshGeographyCoverageSummary()
    }

    fun selectAllMouzas() {
        val allIds = currentMouzas.map { it.id }.toSet()
        val allTypes = RecordType.values().toSet()
        val newLandRecordsCount = recalculateSelectedLandRecords(allIds, allTypes)
        val updatedPlanned = computePlannedQueues(
            mouzasPool = currentMouzas,
            selectedMouzaIds = allIds,
            recordTypes = allTypes,
            queueSize = _uiState.value.batchSize,
            activeTasks = _uiState.value.queue
        )
        _uiState.value = _uiState.value.copy(
            selectedMouzaIds = allIds,
            selectedRecordTypes = allTypes,
            selectedLandRecordsCount = newLandRecordsCount,
            plannedQueues = updatedPlanned
        )
        refreshGeographyCoverageSummary()
    }

    fun selectNextSevenMouzas() {
        val undownloaded = currentMouzas.filter { it.id !in _uiState.value.downloadedMouzaIds }
        val nextBatch = undownloaded.take(DEFAULT_MOUZAS_PER_BATCH).map { it.id }.toSet()
        val newLandRecordsCount = recalculateSelectedLandRecords(nextBatch, _uiState.value.selectedRecordTypes)
        val updatedPlanned = computePlannedQueues(
            mouzasPool = currentMouzas,
            selectedMouzaIds = nextBatch,
            recordTypes = _uiState.value.selectedRecordTypes,
            queueSize = DEFAULT_MOUZAS_PER_BATCH,
            activeTasks = _uiState.value.queue
        )
        _uiState.value = _uiState.value.copy(
            selectedMouzaIds = nextBatch,
            selectedLandRecordsCount = newLandRecordsCount,
            plannedQueues = updatedPlanned
        )
        refreshGeographyCoverageSummary()
    }

    fun deselectAllMouzas() {
        _uiState.value = _uiState.value.copy(
            selectedMouzaIds = emptySet(),
            selectedLandRecordsCount = 0,
            plannedQueues = emptyList()
        )
        refreshGeographyCoverageSummary()
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

    fun setShowPreQueueConfirmationDialog(show: Boolean) {
        if (show) refreshGeographyCoverageSummary()
        _uiState.value = _uiState.value.copy(isShowingPreQueueConfirmationDialog = show)
    }

    fun setShowGeoUpdateDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(isShowingGeoUpdateDialog = show)
    }

    fun setShowPdfImportDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(isShowingPdfImportDialog = show)
    }

    /**
     * Homepage 1-Click Queue Generator:
     * Automatically selects all Mouzas in the active Upazila(s) (or entire District if no Upazila is selected yet),
     * generates the 7-Mouza batch plan, and enqueues all batches into the active download queue.
     */
    fun generateQueueFromHomepage(context: Context? = null) {
        if (currentMouzas.isEmpty()) {
            selectAllUpazilasInDistrict(autoSelectAllMouzas = true)
        } else if (_uiState.value.selectedMouzaIds.isEmpty()) {
            selectAllMouzas()
        }
        queueSelectedDownloads()
        val totalMouzasCount = _uiState.value.selectedMouzaIds.size.coerceAtLeast(currentMouzas.size)
        val batchesCount = totalBatches.coerceAtLeast(1)
        val msg = "কিউ তৈরি সম্পন্ন: $totalMouzasCount টি মৌজা • $batchesCount টি ব্যাচ (৭টি মৌজা/ব্যাচ)"
        _uiState.value = _uiState.value.copy(cacheNotificationMessage = msg)
        if (context != null) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Homepage 1-Click Download All:
     * Selects all Mouzas for the active Upazila/District, generates all 7-Mouza queues,
     * unpauses/resumes all active & pending downloads, and exports completed records to Phone Memory.
     */
    fun downloadAllFromHomepage(context: Context? = null) {
        if (currentMouzas.isEmpty()) {
            selectAllUpazilasInDistrict(autoSelectAllMouzas = true)
        } else {
            selectAllMouzas()
        }
        if (_uiState.value.isPaused) {
            togglePauseQueue()
        }
        queueSelectedDownloads()
        startSequentialQueueCoordinator()
        if (context != null && (savedRecords.value.isNotEmpty() || _uiState.value.masterArchive != null)) {
            downloadAllSessionRecordsToPhone(context)
        }
        val totalMouzasCount = _uiState.value.selectedMouzaIds.size.coerceAtLeast(currentMouzas.size)
        val batchesCount = totalBatches.coerceAtLeast(1)
        val msg = "সব ডাউনলোড শুরু হয়েছে: $totalMouzasCount টি মৌজা ($batchesCount টি ব্যাচ) ➔ Phone Memory/Download/LandRecords"
        _uiState.value = _uiState.value.copy(cacheNotificationMessage = msg)
        if (context != null) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Partitions all selected Mouzas into duplicate-free 7-Mouza batches
     * (Batch 1: Mouza 1–7, Batch 2: Mouza 8–14, etc.) and executes them sequentially.
     */
    fun queueSelectedDownloads() {
        if (currentMouzas.isEmpty()) {
            selectAllUpazilasInDistrict(autoSelectAllMouzas = true)
        }
        val state = _uiState.value
        val district = currentDistrict ?: return
        val mouzasPool = currentMouzas
        if (mouzasPool.isEmpty()) return

        val primaryUpazila = currentUpazila ?: district.upazilas.firstOrNull() ?: return
        val queueSize = DEFAULT_MOUZAS_PER_BATCH

        val candidates = if (state.selectedMouzaIds.isNotEmpty()) {
            mouzasPool.filter { it.id in state.selectedMouzaIds && it.id !in state.downloadedMouzaIds }
        } else {
            mouzasPool.filter { it.id !in state.downloadedMouzaIds }
        }.distinctBy { it.id }

        if (candidates.isEmpty()) return

        enqueueMultiQueues(
            mouzasToQueue = candidates,
            district = district,
            upazila = primaryUpazila,
            queueSize = queueSize,
            startBatchNumber = 1
        )
    }

    private fun enqueueMultiQueues(
        mouzasToQueue: List<Mouza>,
        district: District,
        upazila: Upazila,
        queueSize: Int,
        startBatchNumber: Int = 1
    ) {
        // Phase 2: Strictly enforce 7 mouzas per batch as per requirement
        val effectiveQueueSize = DEFAULT_MOUZAS_PER_BATCH 
        val timestamp = System.currentTimeMillis()
        val state = _uiState.value
        val recordTypes = state.selectedRecordTypes.ifEmpty { RecordType.values().toSet() }
        val format = state.selectedDownloadFormat
        val retrievalMode = state.selectedRetrievalMode
        val existingTaskMouzaIds = state.queue.map { it.mouzaId }.toSet()

        // Strict duplicate prevention: every eligible Mouza appears at most once in the queue
        val filteredMouzas = mouzasToQueue
            .distinctBy { it.id }
            .filter { it.id !in existingTaskMouzaIds }

        if (filteredMouzas.isEmpty()) {
            startSequentialQueueCoordinator()
            return
        }

        val upazilaLookup = district.upazilas.associateBy { it.id }
        val divisionNameBn = currentDivision?.nameBn ?: "চট্টগ্রাম"

        val chunks = filteredMouzas.chunked(effectiveQueueSize)
        val newTasks = mutableListOf<DownloadTask>()
        var globalSortOrder = (state.queue.maxOfOrNull { it.sortOrder } ?: 0) + 1

        chunks.forEachIndexed { queueIdx, mouzaChunk ->
            val queueNumber = startBatchNumber + queueIdx
            mouzaChunk.forEachIndexed { itemIdx, mouza ->
                val resolvedUpa = upazilaLookup[mouza.upazilaId] ?: upazila
                val taskId = "${mouza.id}-q${queueNumber}-$timestamp-$itemIdx"
                val totalKhatians = mouza.totalKhatiansFor(recordTypes)
                val initialStatus = if (retrievalMode == RetrievalMode.MODE_B_USER_ASSISTED) {
                    TaskStatus.WAITING_FOR_USER
                } else {
                    TaskStatus.PENDING
                }

                newTasks.add(
                    DownloadTask(
                        id = taskId,
                        mouzaId = mouza.id,
                        mouzaNameBn = mouza.nameBn,
                        mouzaNameEn = mouza.nameEn,
                        jlNo = mouza.jlNo,
                        upazilaNameBn = resolvedUpa.nameBn,
                        districtNameBn = district.nameBn,
                        recordTypes = recordTypes,
                        downloadFormat = format,
                        status = initialStatus,
                        progress = 0,
                        totalKhatians = totalKhatians,
                        downloadedKhatians = 0,
                        fileSizeBytes = 0L,
                        speedKbps = 0,
                        batchNumber = queueNumber,
                        timestamp = timestamp + (queueIdx * 1000L) + itemIdx,
                        divisionId = district.divisionId,
                        divisionNameBn = divisionNameBn,
                        districtId = district.id,
                        upazilaId = resolvedUpa.id,
                        surveyType = recordTypes.firstOrNull()?.code ?: "RS",
                        recordCategory = "KHATIAN",
                        retrievalMode = retrievalMode,
                        sourceName = if (retrievalMode == RetrievalMode.MODE_A_AUTHORIZED_API) {
                            "Authorized Local / DLR&S Registry"
                        } else {
                            "User-Assisted Browser / Official Portal"
                        },
                        sourceReference = mouza.sourceReference,
                        sortOrder = globalSortOrder++
                    )
                )
            }
        }

        val updatedQueue = state.queue + newTasks
        val updatedPlanned = computePlannedQueues(
            mouzasPool = currentMouzas,
            selectedMouzaIds = state.selectedMouzaIds,
            recordTypes = recordTypes,
            queueSize = effectiveQueueSize,
            activeTasks = updatedQueue
        )

        _uiState.value = state.copy(
            queue = updatedQueue,
            batchSize = effectiveQueueSize,
            currentBatchNumber = startBatchNumber,
            plannedQueues = updatedPlanned,
            isShowingPreQueueConfirmationDialog = false
        )

        appendActivityLog("Generated ${newTasks.size} tasks across ${chunks.size} batches (${effectiveQueueSize} Mouzas/batch)")

        viewModelScope.launch {
            repository.insertTasksToQueue(newTasks)
            state.upazilaFetchInfo?.let { 
                upazilaFetchInfoDao.insertFetchInfo(it.copy(mouzasPerQueue = effectiveQueueSize)) 
            }
            if (!_uiState.value.isPaused && retrievalMode == RetrievalMode.MODE_A_AUTHORIZED_API) {
                startSequentialQueueCoordinator()
                if (_uiState.value.useWorkManagerBackgroundService) {
                    DownloadWorkManagerHelper.enqueueDownloadWork(
                        context = getApplication(),
                        replace = false,
                        autoBatchMode = _uiState.value.isAutoBatchMode
                    )
                }
            }
        }
    }

    /**
     * Sequential Multi-Queue Coordinator:
     * Executes Batch #1 completely, then automatically advances to Batch #2, ..., up to Batch #N.
     */
    private fun startSequentialQueueCoordinator() {
        if (queueCoordinatorJob?.isActive == true) return

        queueCoordinatorJob = viewModelScope.launch {
            while (true) {
                while (_uiState.value.isPaused) {
                    delay(250)
                }

                val allTasks = repository.getAllTasksDirect()
                if (allTasks.isEmpty()) break

                val incompleteTasks = allTasks.filter {
                    it.status == TaskStatus.PENDING || it.status == TaskStatus.DOWNLOADING
                }

                if (incompleteTasks.isEmpty()) {
                    val upazila = currentUpazila
                    val district = currentDistrict
                    if (_uiState.value.isAutoBatchMode && upazila != null && district != null &&
                        _uiState.value.selectedRetrievalMode == RetrievalMode.MODE_A_AUTHORIZED_API
                    ) {
                        val queuedOrDoneIds = allTasks.map { it.mouzaId }.toSet()
                        val remainingMouzas = currentMouzas.filter { it.id !in queuedOrDoneIds }
                        if (remainingMouzas.isNotEmpty()) {
                            val nextBatchStart = (allTasks.maxOfOrNull { it.batchNumber } ?: 0) + 1
                            val queueSize = _uiState.value.batchSize.coerceAtLeast(1)
                            enqueueMultiQueues(
                                mouzasToQueue = remainingMouzas,
                                district = district,
                                upazila = upazila,
                                queueSize = queueSize,
                                startBatchNumber = nextBatchStart
                            )
                            delay(100)
                            continue
                        }
                    }

                    if (allTasks.isNotEmpty() && allTasks.all { it.status == TaskStatus.COMPLETED }) {
                        val district = currentDistrict
                        val upazila = currentUpazila
                        if (district != null && upazila != null) {
                            compileMasterArchive(district, upazila)
                        }
                    }
                    break
                }

                val activeQueueNumber = incompleteTasks.minOf { it.batchNumber }
                if (_uiState.value.currentBatchNumber != activeQueueNumber) {
                    _uiState.value = _uiState.value.copy(currentBatchNumber = activeQueueNumber)
                }

                val tasksInActiveQueue = incompleteTasks.filter { it.batchNumber == activeQueueNumber }
                val profile = _uiState.value.deviceNetworkProfile ?: NetworkDeviceQueueAdvisor.analyze(getApplication())
                val concurrency = profile.maxConcurrentDownloads.coerceIn(1, 3)
                val queueSemaphore = Semaphore(concurrency)

                coroutineScope {
                    tasksInActiveQueue.forEach { task ->
                        launch {
                            queueSemaphore.withPermit {
                                executeTaskWithExponentialBackoff(task.id, profile.stepDelayMs)
                            }
                        }
                    }
                }

                delay(120)
            }
        }
    }

    private suspend fun executeTaskWithExponentialBackoff(taskId: String, baseStepDelayMs: Long) {
        val initialTask = repository.getTaskById(taskId) ?: return
        if (initialTask.status == TaskStatus.COMPLETED || initialTask.status == TaskStatus.CANCELLED ||
            initialTask.status == TaskStatus.UNAVAILABLE || initialTask.status == TaskStatus.WAITING_FOR_USER
        ) return

        updateTaskSync(taskId) {
            it.copy(status = TaskStatus.DOWNLOADING, progress = 10, speedKbps = 380)
        }

        // Section 1: Eliminate simulated downloads. No artificial delays or fake progress bars.
        // A task can only become COMPLETED if an actual document is received and validated.

        when (initialTask.retrievalMode) {
            RetrievalMode.MODE_A_AUTHORIZED_API -> {
                // Mode A requires an official, permitted access method (Adapter).
                // Since no authorized adapter is configured in this Phase, we fail safely.
                updateTaskSync(taskId) {
                    it.copy(
                        status = TaskStatus.FAILED,
                        progress = 0,
                        errorDetails = "Authorized Automated Retrieval (Mode A) is currently unavailable. No official API adapter configured."
                    )
                }
                return
            }
            RetrievalMode.MODE_B_USER_ASSISTED -> {
                // Mode B: User-assisted workflow.
                // Transition task to AWAITING_USER_IMPORT. The UI will prompt the user to import the PDF.
                updateTaskSync(taskId) {
                    it.copy(
                        status = TaskStatus.AWAITING_USER_IMPORT,
                        progress = 10,
                        speedKbps = 0
                    )
                }
                return
            }
        }

    }

    /**
     * Mode B User-Assisted Retrieval: Opens the official/reference browser search page
     * without scraping or bypassing website terms.
     */
    fun openUserAssistedBrowserForTask(context: Context, task: DownloadTask? = null) {
        val url = "https://land.gov.bd"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            appendActivityLog("Opened user-assisted browser for ${task?.mouzaNameEn ?: "Land Portal"}")
        } catch (e: Exception) {
            Toast.makeText(context, "Open browser: $url", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Imports and validates a genuine PDF document specifically for an active DownloadTask.
     * Marks the task as COMPLETED upon successful validation and persistence.
     */
    fun importPdfForTask(
        context: Context,
        taskId: String,
        pdfBytes: ByteArray,
        fileName: String
    ) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val validation = DocumentVerificationEngine.verifyDocumentBytes(
                bytes = pdfBytes,
                declaredMimeType = "application/pdf",
                fileName = fileName
            )

            if (!validation.isValid) {
                val errMsg = "PDF Validation Failed: ${validation.errorReason}"
                _uiState.value = _uiState.value.copy(lastImportStatusMessage = errMsg)
                appendActivityLog(errMsg)
                updateTaskSync(taskId) { it.copy(errorDetails = errMsg) }
                return@launch
            }

            val district = allDistricts.find { it.nameBn == task.districtNameBn || it.nameEn == task.districtNameBn }
                ?: allDistricts.first { it.id == task.districtId }
            val upazila = district.upazilas.find { it.nameBn == task.upazilaNameBn || it.nameEn == task.upazilaNameBn }
                ?: district.upazilas.first { it.id == task.upazilaId }

            val savedResult = LandRecordStorageManager.importAndVerifyUserPdf(
                context = context,
                pdfBytes = pdfBytes,
                originalFileName = fileName,
                divisionNameEn = task.divisionId,
                districtNameEn = district.nameEn,
                upazilaNameEn = upazila.nameEn,
                mouzaNameEn = task.mouzaNameEn,
                jlNo = task.jlNo,
                surveyType = task.surveyType
            )

            val completedAt = System.currentTimeMillis()
            updateTaskSync(taskId) { t ->
                t.copy(
                    status = TaskStatus.COMPLETED,
                    progress = 100,
                    fileLocation = savedResult.primaryDisplayPath,
                    fileSizeBytes = savedResult.fileSizeBytes,
                    sha256Checksum = savedResult.sha256Checksum,
                    completedTimestamp = completedAt
                )
            }

            _uiState.value = _uiState.value.copy(
                downloadedMouzaIds = _uiState.value.downloadedMouzaIds + task.mouzaId,
                lastImportStatusMessage = "Successfully imported $fileName for ${task.mouzaNameBn}"
            )

            val sizeFormatted = String.format("%.1f KB", savedResult.fileSizeBytes / 1024.0)

            repository.insertSavedRecord(
                SavedRecord(
                    id = task.id,
                    mouzaId = task.mouzaId,
                    mouzaNameBn = task.mouzaNameBn,
                    mouzaNameEn = task.mouzaNameEn,
                    jlNo = task.jlNo,
                    upazilaNameBn = task.upazilaNameBn,
                    districtNameBn = task.districtNameBn,
                    recordType = task.recordTypeLabel,
                    khatianCount = task.totalKhatians,
                    fileSizeFormatted = sizeFormatted,
                    divisionNameBn = task.divisionNameBn,
                    fileLocation = savedResult.primaryDisplayPath,
                    sha256Checksum = savedResult.sha256Checksum,
                    sourceName = "User Imported PDF ($fileName)",
                    verificationStatus = DocumentVerificationStatus.CHECKSUM_VERIFIED.name
                )
            )

            masterLandLedgerDao.insertEntry(
                MasterLedgerEntryEntity(
                    divisionId = task.divisionId,
                    divisionName = task.divisionNameBn,
                    districtId = task.districtId,
                    districtName = task.districtNameBn,
                    upazilaId = task.upazilaId,
                    upazilaName = task.upazilaNameBn,
                    mouzaId = task.mouzaId,
                    mouzaName = "${task.mouzaNameBn} (${task.mouzaNameEn})",
                    jlNumber = task.jlNo,
                    surveyType = task.recordTypeLabel,
                    khatianNumber = task.referenceId ?: "User-Imported",
                    dagNumber = "",
                    ownerName = "",
                    guardianName = "",
                    documentType = "USER_IMPORTED_PDF",
                    sourceName = "User Imported PDF ($fileName)",
                    retrievalMethod = RetrievalMode.MODE_B_USER_ASSISTED.name,
                    verificationStatus = DocumentVerificationStatus.USER_IMPORTED_PDF,
                    fileReference = savedResult.primaryDisplayPath,
                    fileSizeBytes = savedResult.fileSizeBytes,
                    sha256Checksum = savedResult.sha256Checksum,
                    recordedAt = completedAt
                )
            )

            appendActivityLog("Manually fulfilled task ${task.id} via PDF import")
        }
    }

    /**
     * Imports and validates a genuine PDF document supplied by the user into the structured Mouza archive
     * and records its SHA-256 and provenance in the Master Land Ledger (Sections 8 & 9).
     */
    fun importUserPdfDocument(
        context: Context,
        pdfBytes: ByteArray,
        fileName: String,
        khatianNumber: String = "",
        dagNumber: String = "",
        ownerName: String = "",
        guardianName: String = "",
        surveyType: String = "RS"
    ) {
        viewModelScope.launch {
            val validation = DocumentVerificationEngine.verifyDocumentBytes(
                bytes = pdfBytes,
                declaredMimeType = "application/pdf",
                fileName = fileName
            )

            if (!validation.isValid) {
                val errMsg = "PDF Validation Failed: ${validation.errorReason}"
                _uiState.value = _uiState.value.copy(lastImportStatusMessage = errMsg)
                appendActivityLog(errMsg)
                return@launch
            }

            // Duplicate file detection by cryptographic SHA-256
            val existingEntry = masterLandLedgerDao.findBySha256(validation.sha256Checksum)
            if (existingEntry != null) {
                val dupMsg = "Duplicate PDF detected (SHA-256: ${validation.sha256Checksum.take(12)}... already archived under ${existingEntry.mouzaName})"
                _uiState.value = _uiState.value.copy(lastImportStatusMessage = dupMsg)
                appendActivityLog(dupMsg)
                return@launch
            }

            val district = currentDistrict ?: allDistricts.first()
            val upazila = currentUpazila ?: district.upazilas.first()
            val mouza = currentMouzas.firstOrNull { it.id in _uiState.value.selectedMouzaIds }
                ?: upazila.mouzas.firstOrNull()
                ?: Mouza("imported-mouza", "ইমপোর্টকৃত মৌজা", "Imported Mouza", "০১")

            val savedResult = LandRecordStorageManager.importAndVerifyUserPdf(
                context = context,
                pdfBytes = pdfBytes,
                originalFileName = fileName,
                divisionNameEn = district.divisionId,
                districtNameEn = district.nameEn,
                upazilaNameEn = upazila.nameEn,
                mouzaNameEn = mouza.nameEn,
                jlNo = mouza.jlNo,
                surveyType = surveyType
            )

            masterLandLedgerDao.insertEntry(
                MasterLedgerEntryEntity(
                    divisionId = district.divisionId,
                    divisionName = currentDivision?.nameBn ?: "চট্টগ্রাম",
                    districtId = district.id,
                    districtName = district.nameBn,
                    upazilaId = upazila.id,
                    upazilaName = upazila.nameBn,
                    mouzaId = mouza.id,
                    mouzaName = "${mouza.nameBn} (${mouza.nameEn})",
                    jlNumber = mouza.jlNo,
                    surveyType = surveyType,
                    khatianNumber = khatianNumber.trim(),
                    dagNumber = dagNumber.trim(),
                    ownerName = ownerName.trim(),
                    guardianName = guardianName.trim(),
                    documentType = "USER_IMPORTED_PDF",
                    sourceName = "User Imported Official PDF ($fileName)",
                    retrievalMethod = RetrievalMode.MODE_B_USER_ASSISTED.name,
                    verificationStatus = DocumentVerificationStatus.USER_IMPORTED_PDF,
                    fileReference = savedResult.primaryDisplayPath,
                    fileSizeBytes = savedResult.fileSizeBytes,
                    sha256Checksum = savedResult.sha256Checksum
                )
            )

            val okMsg = "Verified & Imported PDF (${savedResult.fileSizeBytes} bytes, SHA-256: ${savedResult.sha256Checksum.take(12)}...)"
            _uiState.value = _uiState.value.copy(lastImportStatusMessage = okMsg)
            appendActivityLog(okMsg)
        }
    }

    /**
     * Allows users to correct OCR or metadata fields in the Master Land Ledger while preserving original OCR text.
     */
    fun updateMasterLedgerEntryMetadata(
        entry: MasterLedgerEntryEntity,
        correctedOwnerName: String,
        correctedGuardianName: String,
        correctedKhatianNumber: String,
        correctedDagNumber: String,
        userNotes: String
    ) {
        viewModelScope.launch {
            val updated = entry.copy(
                ownerName = correctedOwnerName.trim(),
                guardianName = correctedGuardianName.trim(),
                khatianNumber = correctedKhatianNumber.trim(),
                dagNumber = correctedDagNumber.trim(),
                userCorrectedNotes = userNotes.trim(),
                verificationStatus = DocumentVerificationStatus.USER_CORRECTED_OCR
            )
            masterLandLedgerDao.updateEntry(updated)
            appendActivityLog("Updated Master Ledger record #${entry.id} (preserved original OCR reference)")
        }
    }

    /**
     * Imports a JSON geographical database update at runtime without requiring an APK rebuild.
     */
    fun importGeographyDatasetJson(jsonPayload: String) {
        viewModelScope.launch {
            val history = geographyRepository.importGeographyJsonUpdate(jsonPayload)
            refreshGeographyCoverageSummary()
            val msg = "Geo Update (${history.datasetVersion}): Imported ${history.recordCount} Mouzas. ${history.importErrors}"
            _uiState.value = _uiState.value.copy(lastImportStatusMessage = msg)
            appendActivityLog(msg)
        }
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
        val effectiveTypes = recordTypes.ifEmpty { RecordType.values().toSet() }
        val count = totalKhatians.coerceAtLeast(1) * effectiveTypes.size.coerceAtLeast(1)
        val taskId = "khotiyan-${mouza.id}-$timestamp"

        val task = DownloadTask(
            id = taskId,
            mouzaId = mouza.id,
            mouzaNameBn = "${mouza.nameBn} ($khotiyanLabel)",
            mouzaNameEn = "${mouza.nameEn} ($khotiyanLabel)",
            jlNo = mouza.jlNo,
            upazilaNameBn = upazila.nameBn,
            districtNameBn = district.nameBn,
            recordTypes = effectiveTypes,
            downloadFormat = _uiState.value.selectedDownloadFormat,
            status = TaskStatus.PENDING,
            progress = 0,
            totalKhatians = count,
            downloadedKhatians = 0,
            fileSizeBytes = 0L,
            speedKbps = 0,
            batchNumber = _uiState.value.currentBatchNumber,
            timestamp = timestamp,
            divisionId = district.divisionId,
            districtId = district.id,
            upazilaId = upazila.id,
            referenceId = khotiyanLabel
        )

        viewModelScope.launch {
            repository.insertTasksToQueue(listOf(task))
            if (!_uiState.value.isPaused) {
                startSequentialQueueCoordinator()
                if (_uiState.value.useWorkManagerBackgroundService) {
                    DownloadWorkManagerHelper.enqueueDownloadWork(getApplication(), replace = false)
                }
            }
        }
    }

    fun queueReferenceIdsDownload(
        district: District,
        upazila: Upazila,
        mouza: Mouza,
        referenceIds: List<String>,
        recordTypes: Set<RecordType>
    ) {
        if (referenceIds.isEmpty()) return
        val timestamp = System.currentTimeMillis()
        val effectiveRecordTypes = recordTypes.ifEmpty { RecordType.values().toSet() }
        val queueSize = _uiState.value.batchSize.coerceAtLeast(1)
        val startBatch = (_uiState.value.queue.maxOfOrNull { it.batchNumber } ?: 0) + 1

        val newTasks = mutableListOf<DownloadTask>()
        referenceIds.chunked(queueSize).forEachIndexed { chunkIdx, refChunk ->
            val batchNum = startBatch + chunkIdx
            refChunk.forEachIndexed { itemIdx, refId ->
                val cleanId = refId.trim()
                val taskId = "ref-${mouza.id}-$cleanId-$timestamp-$chunkIdx-$itemIdx"

                newTasks.add(
                    DownloadTask(
                        id = taskId,
                        mouzaId = mouza.id,
                        mouzaNameBn = "${mouza.nameBn} [রেফারেন্স: $cleanId]",
                        mouzaNameEn = "${mouza.nameEn} [Ref: $cleanId]",
                        jlNo = mouza.jlNo,
                        upazilaNameBn = upazila.nameBn,
                        districtNameBn = district.nameBn,
                        recordTypes = effectiveRecordTypes,
                        downloadFormat = _uiState.value.selectedDownloadFormat,
                        status = TaskStatus.PENDING,
                        progress = 0,
                        totalKhatians = 1 * effectiveRecordTypes.size,
                        downloadedKhatians = 0,
                        fileSizeBytes = 0L,
                        speedKbps = 0,
                        batchNumber = batchNum,
                        timestamp = timestamp + (chunkIdx * 1000L) + itemIdx,
                        referenceId = cleanId,
                        divisionId = district.divisionId,
                        districtId = district.id,
                        upazilaId = upazila.id
                    )
                )
            }
        }

        val updatedQueue = _uiState.value.queue + newTasks
        _uiState.value = _uiState.value.copy(queue = updatedQueue)

        viewModelScope.launch {
            repository.insertTasksToQueue(newTasks)
            if (!_uiState.value.isPaused) {
                startSequentialQueueCoordinator()
                if (_uiState.value.useWorkManagerBackgroundService) {
                    DownloadWorkManagerHelper.enqueueDownloadWork(
                        context = getApplication(),
                        replace = false,
                        autoBatchMode = _uiState.value.isAutoBatchMode
                    )
                }
            }
        }
    }

    private suspend fun updateTaskSync(taskId: String, transform: (DownloadTask) -> DownloadTask) {
        val task = repository.getTaskById(taskId)
        if (task != null) {
            repository.updateTaskInQueue(transform(task))
        }
    }

    private fun compileMasterArchive(district: District, upazila: Upazila) {
        val queue = _uiState.value.queue
        val totalKhatians = queue.sumOf { it.totalKhatians }
        val totalBytes = queue.sumOf { it.fileSizeBytes }
        val sizeFormatted = if (totalBytes >= 1024 * 1024) {
            String.format("%.2f MB", totalBytes / (1024.0 * 1024.0))
        } else {
            String.format("%.1f KB", totalBytes / 1024.0)
        }
        val typesText = _uiState.value.selectedRecordTypes.joinToString("+") { it.code }
        val fileName = "${district.nameEn}_${upazila.nameEn}_Master_Land_Ledger.json"

        val archive = MasterArchive(
            upazilaId = upazila.id,
            upazilaNameBn = upazila.nameBn,
            upazilaNameEn = upazila.nameEn,
            districtNameBn = district.nameBn,
            districtNameEn = district.nameEn,
            totalMouzasCount = _uiState.value.downloadedMouzaIds.size.coerceAtLeast(queue.size),
            recordTypesIncluded = typesText,
            totalKhatiansCount = totalKhatians,
            totalSizeMb = sizeFormatted,
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
            val ledgerRows = masterLandLedgerDao.getAllEntriesDirect()
            val saveResult = LandRecordStorageManager.saveMasterLandRecordsArchive(
                context = context,
                archive = archive,
                district = district,
                upazila = upazila,
                queueTasks = _uiState.value.queue,
                ledgerEntries = ledgerRows
            )

            val shouldClearCache = district.id != "cumilla" && district.id != "brahmanbaria"
            if (shouldClearCache) {
                LandRecordStorageManager.clearCacheAndMemory(context)
            }

            val saveMessage = if (saveResult.isPublicDownloads) {
                "Master Land Ledger (JSON, CSV, TXT, XLSX, PDF) saved to Download/LandRecords/LRgetBD/!"
            } else {
                "Saved Master Land Ledger (JSON, CSV, TXT, XLSX, PDF) to app storage."
            }

            _uiState.value = _uiState.value.copy(
                masterArchive = archive.copy(
                    isDownloadedToPhone = true,
                    phoneSavedPath = saveResult.primaryDisplayPath,
                    textFileSavedPath = saveResult.textPath,
                    csvFileSavedPath = saveResult.csvPath,
                    xlsxFileSavedPath = saveResult.xlsxPath,
                    pdfFileSavedPath = saveResult.pdfPath,
                    sha256Checksum = saveResult.sha256Checksum,
                    savedFilesList = saveResult.allFiles,
                    fileContentPreview = saveResult.filePreview
                ),
                isShowingSaveSuccessDialog = true,
                cacheNotificationMessage = saveMessage
            )
        }
    }

    fun downloadAllSessionRecordsToPhone(context: Context) {
        val upazila = currentUpazila ?: return
        val district = currentDistrict ?: return
        val completedTasks = _uiState.value.queue.filter { it.status == TaskStatus.COMPLETED }

        if (completedTasks.isEmpty()) {
            Toast.makeText(context, "No completed records in current session to download.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            completedTasks.forEach { task ->
                try {
                    LandRecordStorageManager.saveMouzaRecordFile(context, task, district, upazila)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            downloadMasterFileToPhone(context)

            Toast.makeText(context, "All ${completedTasks.size} records exported to Downloads!", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Dedicated trigger for Section 9 CSV Metadata Export.
     */
    fun exportMasterLedgerCsvOnly(context: Context) {
        val upazila = currentUpazila ?: return
        val district = currentDistrict ?: return

        viewModelScope.launch {
            val ledgerRows = masterLandLedgerDao.getAllEntriesDirect()
            if (ledgerRows.isEmpty() && _uiState.value.queue.none { it.status == TaskStatus.COMPLETED }) {
                Toast.makeText(context, "No metadata available to export yet.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val result = LandRecordStorageManager.exportMetadataCsv(
                context = context,
                district = district,
                upazila = upazila,
                ledgerEntries = ledgerRows
            )

            val msg = "Metadata CSV exported to: ${result.primaryDisplayPath}"
            _uiState.value = _uiState.value.copy(cacheNotificationMessage = msg)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Dedicated trigger for JSON Metadata Export.
     */
    fun exportMasterLedgerJsonOnly(context: Context) {
        val upazila = currentUpazila ?: return
        val district = currentDistrict ?: return

        viewModelScope.launch {
            val ledgerRows = masterLandLedgerDao.getAllEntriesDirect()
            if (ledgerRows.isEmpty() && _uiState.value.queue.none { it.status == TaskStatus.COMPLETED }) {
                Toast.makeText(context, "No metadata available to export yet.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val result = LandRecordStorageManager.exportMetadataJson(
                context = context,
                district = district,
                upazila = upazila,
                ledgerEntries = ledgerRows
            )

            val msg = "Metadata JSON exported to: ${result.primaryDisplayPath}"
            _uiState.value = _uiState.value.copy(cacheNotificationMessage = msg)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
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
            queueCoordinatorJob?.cancel()
            queueCoordinatorJob = null
            taskJobs.values.forEach { it.cancel() }
            taskJobs.clear()
            viewModelScope.launch {
                repository.resetInterruptedTasks()
            }
            appendActivityLog("Queue Paused")
        } else {
            startSequentialQueueCoordinator()
            if (_uiState.value.useWorkManagerBackgroundService) {
                DownloadWorkManagerHelper.enqueueDownloadWork(
                    context = getApplication(),
                    replace = true,
                    autoBatchMode = _uiState.value.isAutoBatchMode
                )
            }
            appendActivityLog("Queue Resumed")
        }
    }

    fun cancelAllQueueTasks() {
        DownloadWorkManagerHelper.cancelDownloadWork(getApplication())
        queueCoordinatorJob?.cancel()
        queueCoordinatorJob = null
        viewModelScope.launch {
            repository.cancelAllPendingTasks()
            appendActivityLog("Cancelled all pending tasks")
        }
    }

    fun skipUnavailableTask(taskId: String) {
        viewModelScope.launch {
            repository.markTaskUnavailable(taskId)
            appendActivityLog("Marked task $taskId as UNAVAILABLE")
        }
    }

    /**
     * Reorders a pending task up or down within its batch.
     */
    fun reorderPendingTask(taskId: String, moveUp: Boolean) {
        viewModelScope.launch {
            val all = repository.getAllTasksDirect().filter { it.status == TaskStatus.PENDING }
            val index = all.indexOfFirst { it.id == taskId }
            if (index == -1) return@launch
            val swapIndex = if (moveUp) index - 1 else index + 1
            if (swapIndex !in all.indices) return@launch

            val currentTask = all[index]
            val neighborTask = all[swapIndex]
            repository.updateTaskInQueue(currentTask.copy(sortOrder = neighborTask.sortOrder, timestamp = neighborTask.timestamp))
            repository.updateTaskInQueue(neighborTask.copy(sortOrder = currentTask.sortOrder, timestamp = currentTask.timestamp))
            appendActivityLog("Reordered task ${currentTask.mouzaNameEn}")
        }
    }

    fun removeBatchFromQueue(batchNumber: Int) {
        viewModelScope.launch {
            repository.deleteBatchFromQueue(batchNumber)
            appendActivityLog("Removed Batch #$batchNumber from queue")
        }
    }

    fun clearQueue() {
        DownloadWorkManagerHelper.cancelDownloadWork(getApplication())
        queueCoordinatorJob?.cancel()
        queueCoordinatorJob = null
        taskJobs.values.forEach { it.cancel() }
        taskJobs.clear()
        val updatedPlanned = computePlannedQueues(
            mouzasPool = currentMouzas,
            selectedMouzaIds = _uiState.value.selectedMouzaIds,
            recordTypes = _uiState.value.selectedRecordTypes,
            queueSize = _uiState.value.batchSize,
            activeTasks = emptyList()
        )
        _uiState.value = _uiState.value.copy(
            isPaused = false,
            queue = emptyList(),
            currentBatchNumber = 1,
            plannedQueues = updatedPlanned
        )
        viewModelScope.launch {
            repository.clearQueue()
        }
    }

    fun setUseWorkManagerBackgroundService(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(useWorkManagerBackgroundService = enabled)
        if (enabled) {
            if (!_uiState.value.isPaused && _uiState.value.queue.any { it.status == TaskStatus.PENDING }) {
                startSequentialQueueCoordinator()
                DownloadWorkManagerHelper.enqueueDownloadWork(
                    context = getApplication(),
                    replace = false,
                    autoBatchMode = _uiState.value.isAutoBatchMode
                )
            }
        } else {
            DownloadWorkManagerHelper.cancelDownloadWork(getApplication())
            if (!_uiState.value.isPaused) {
                startSequentialQueueCoordinator()
            }
        }
    }

    fun startBackgroundWorkManagerManually() {
        if (_uiState.value.isPaused) {
            _uiState.value = _uiState.value.copy(isPaused = false)
        }
        startSequentialQueueCoordinator()
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
                    downloadedKhatians = 0,
                    errorDetails = null
                )
                repository.updateTaskInQueue(resetTask)
                if (!_uiState.value.isPaused) {
                    startSequentialQueueCoordinator()
                }
            }
        }
    }

    fun retryAllFailed() {
        viewModelScope.launch {
            val failedTasks = _uiState.value.queue.filter {
                it.status == TaskStatus.FAILED || it.status == TaskStatus.CANCELLED
            }
            failedTasks.forEach { task ->
                val reset = task.copy(status = TaskStatus.PENDING, progress = 0, downloadedKhatians = 0, errorDetails = null)
                repository.updateTaskInQueue(reset)
            }
            if (!_uiState.value.isPaused) {
                startSequentialQueueCoordinator()
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
