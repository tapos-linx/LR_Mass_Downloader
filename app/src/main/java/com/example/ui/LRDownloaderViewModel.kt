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
import com.example.data.ledger.DocumentVerificationStatus
import com.example.data.ledger.MasterLedgerEntryEntity
import com.example.data.local.AppDatabase
import com.example.data.local.LandRecordStorageManager
import com.example.data.model.District
import com.example.data.model.DownloadFormat
import com.example.data.model.DownloadTask
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

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
    val districtSelectionError: String? = null,
    val isPaused: Boolean = false,
    val isAutoBatchMode: Boolean = true,
    val isAutoQueueOnUpazilaSelect: Boolean = true,
    val currentBatchNumber: Int = 1,
    val batchSize: Int = LRDownloaderViewModel.DEFAULT_MOUZAS_PER_BATCH,
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
        const val FREE_PORTAL_URL = "https://eporcha.tech/"

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
    private val persistenceMutex = kotlinx.coroutines.sync.Mutex()

    /**
     * Tracks whether the user or test has explicitly changed division/district/upazila in this session,
     * preventing asynchronous startup state recovery from overwriting explicit user selections.
     */
    @Volatile
    private var hasExplicitUserSelection: Boolean = false

    /**
     * Dynamic Room-backed Mouza entities cache updated via `geographyRepository.observeAllMouzas()`,
     * allowing runtime JSON dataset updates to appear immediately in the hierarchy.
     */
    @Volatile
    private var roomMouzaEntities: List<MouzaEntity> = emptyList()

    /**
     * All 8 Administrative Divisions of Bangladesh.
     */
    val allDivisions: List<DivisionEntity> = BangladeshGeographyCatalog.divisions

    /**
     * All 64 Official Districts of Bangladesh.
     */
    val allNationalDistricts: List<DistrictEntity> = BangladeshGeographyCatalog.districts

    /**
     * Canonical 64-District hierarchy built strictly from the single authoritative
     * `BangladeshGeographyCatalog` + Room dataset (Phase 2).
     * Never uses `LandRecordsDataset` partial overrides (`baseMap[distEntity.id]`).
     */
    val allDistricts: List<District>
        get() {
            val baseDistricts = BangladeshGeographyCatalog.buildCanonicalDistrictModels()
            val dynamicMouzas = roomMouzaEntities
            if (dynamicMouzas.isEmpty()) return baseDistricts

            val dynamicByUpazila = dynamicMouzas.groupBy {
                BangladeshGeographyCatalog.resolveCanonicalUpazilaId(it.upazilaId)
            }
            return baseDistricts.map { district ->
                val updatedUpazilas = district.upazilas.map { upazila ->
                    val roomList = dynamicByUpazila[upazila.id]
                    if (roomList.isNullOrEmpty()) {
                        upazila
                    } else {
                        val mergedMouzas = (upazila.mouzas + roomList.map { m ->
                            Mouza(
                                id = m.id,
                                nameBn = m.nameBn,
                                nameEn = m.nameEn,
                                jlNo = m.jlNumber,
                                baseKhatiansPerType = 50,
                                upazilaId = upazila.id,
                                officialCode = m.officialCode.ifBlank { m.officialMouzaCode },
                                sourceName = m.sourceName,
                                sourceUrl = m.sourceUrl,
                                datasetVersion = m.datasetVersion,
                                importedAt = m.importedAt,
                                sourceReference = m.sourceReference,
                                verificationStatus = m.verificationStatus
                            )
                        }).distinctBy { it.id }
                        upazila.copy(mouzas = mergedMouzas)
                    }
                }
                district.copy(upazilas = updatedUpazilas)
            }
        }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            geographyRepository.ensureSeeded()
            repository.resetInterruptedTasks()
            restorePersistedSelectionIfValid()
            refreshGeographyCoverageSummary()
        }

        geographyRepository.observeAllMouzas()
            .onEach { mouzaList ->
                roomMouzaEntities = mouzaList
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)

        repository.downloadQueue
            .onEach { tasks ->
                if (persistenceMutex.isLocked) return@onEach
                var shouldStartCoordinator = false
                _uiState.update { currentState ->
                    val activeDistrictId = BangladeshGeographyCatalog.resolveCanonicalDistrictId(currentState.selectedDistrictId)
                    val districtTasks = if (activeDistrictId.isEmpty()) {
                        emptyList()
                    } else {
                        tasks.filter {
                            BangladeshGeographyCatalog.resolveCanonicalDistrictId(it.districtId) == activeDistrictId
                        }
                    }

                    val updatedPlanned = computePlannedQueues(
                        mouzasPool = currentMouzas,
                        selectedMouzaIds = currentState.selectedMouzaIds,
                        recordTypes = currentState.selectedRecordTypes,
                        queueSize = DEFAULT_MOUZAS_PER_BATCH,
                        activeTasks = districtTasks
                    )
                    val completedIds = districtTasks
                        .filter { it.status == TaskStatus.COMPLETED }
                        .map { it.mouzaId }
                        .toSet()

                    shouldStartCoordinator = !currentState.isPaused && districtTasks.any {
                        it.status == TaskStatus.QUEUED ||
                            it.status == TaskStatus.PENDING ||
                            it.status == TaskStatus.DOWNLOADING ||
                            it.status == TaskStatus.WAITING_FOR_USER
                    }

                    currentState.copy(
                        queue = districtTasks,
                        batchSize = DEFAULT_MOUZAS_PER_BATCH,
                        downloadedMouzaIds = currentState.downloadedMouzaIds + completedIds,
                        plannedQueues = updatedPlanned
                    )
                }

                if (shouldStartCoordinator) {
                    startSequentialQueueCoordinator()
                }
            }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)

        DownloadWorkManagerHelper.isWorkRunningFlow(application)
            .onEach { running ->
                _uiState.update { it.copy(isWorkManagerActive = running) }
            }
            .launchIn(viewModelScope)

        DownloadWorkManagerHelper.getPrimaryWorkInfoFlow(application)
            .onEach { info ->
                _uiState.update { it.copy(activeWorkInfo = info) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Phase 4 — Validated Persistent State Recovery:
     * 1. Verifies that the saved district exists.
     * 2. Verifies that every saved upazila belongs to that district.
     * 3. Verifies that every saved mouza belongs to one of those upazilas.
     * 4. Rejects stale or invalid selections.
     * 5. Never restores a selection from another district if the user already selected a district.
     */
    private suspend fun restorePersistedSelectionIfValid() {
        persistenceMutex.withLock {
            val savedInfo = upazilaFetchInfoDao.getLatestFetchInfoDirect() ?: return
            if (hasExplicitUserSelection) return

            val canonicalDistrictId = BangladeshGeographyCatalog.resolveCanonicalDistrictId(savedInfo.districtId)
            val district = allDistricts.find { it.id == canonicalDistrictId }
            if (district == null) {
                upazilaFetchInfoDao.clearFetchInfo()
                return
            }

            // Support district-only selection snapshot (when user switched district without selecting an upazila yet)
            if (savedInfo.upazilaId.startsWith("__district_only__:") ||
                (savedInfo.upazilaId.isBlank() && savedInfo.selectedUpazilaIdsCsv.isBlank())
            ) {
                if (hasExplicitUserSelection) return
                _uiState.update {
                    it.copy(
                        selectedDivisionId = district.divisionId,
                        selectedDistrictId = district.id,
                        selectedUpazilaId = null,
                        selectedUpazilaIds = emptySet(),
                        selectedMouzaIds = emptySet(),
                        targetTotalMouzas = 0,
                        selectedLandRecordsCount = 0,
                        batchSize = DEFAULT_MOUZAS_PER_BATCH,
                        currentBatchNumber = 1,
                        upazilaFetchInfo = null,
                        plannedQueues = emptyList(),
                        districtSelectionError = null
                    )
                }
                return
            }

            val districtUpazilas = district.upazilas.filter { it.districtId == district.id }
            val districtUpazilaMap = districtUpazilas.associateBy { it.id }

            val canonicalPrimaryUpaId = BangladeshGeographyCatalog.resolveCanonicalUpazilaId(savedInfo.upazilaId)
            val candidateUpazilaIds = (savedInfo.selectedUpazilaIdsSet + canonicalPrimaryUpaId)
                .filter { it.isNotEmpty() }
                .toSet()

            // Verify every saved upazila belongs to the saved district
            if (candidateUpazilaIds.isEmpty() || candidateUpazilaIds.any { it !in districtUpazilaMap }) {
                upazilaFetchInfoDao.clearFetchInfo()
                return
            }

            val validMouzaMap = candidateUpazilaIds
                .flatMap { upaId -> districtUpazilaMap[upaId]?.mouzas.orEmpty() }
                .associateBy { it.id }

            val savedMouzaIds = savedInfo.selectedMouzaIdsSet
            // Verify every saved mouza (if specified) belongs to one of the validated upazilas
            if (savedMouzaIds.isNotEmpty() && savedMouzaIds.any { it !in validMouzaMap }) {
                upazilaFetchInfoDao.clearFetchInfo()
                return
            }

            val effectiveMouzaIds = if (savedMouzaIds.isNotEmpty()) {
                savedMouzaIds
            } else {
                validMouzaMap.keys
            }

            if (hasExplicitUserSelection) return

            val recordTypes = _uiState.value.selectedRecordTypes.ifEmpty { RecordType.values().toSet() }
            val mouzaPool = validMouzaMap.values.toList()
            val planned = computePlannedQueues(
                mouzasPool = mouzaPool,
                selectedMouzaIds = effectiveMouzaIds,
                recordTypes = recordTypes,
                queueSize = DEFAULT_MOUZAS_PER_BATCH,
                activeTasks = _uiState.value.queue
            )
            val totalRecords = mouzaPool
                .filter { it.id in effectiveMouzaIds }
                .sumOf { it.totalKhatiansFor(recordTypes) }

            val normalizedFetchInfo = savedInfo.copy(
                upazilaId = canonicalPrimaryUpaId,
                divisionId = district.divisionId,
                districtId = district.id,
                districtNameBn = district.nameBn,
                districtNameEn = district.nameEn,
                mouzasPerQueue = DEFAULT_MOUZAS_PER_BATCH,
                fetchedMouzasCount = mouzaPool.size,
                totalQueuesCreated = planned.size,
                totalLandRecordsCount = totalRecords
            )

            if (hasExplicitUserSelection) return
            _uiState.update {
                it.copy(
                    selectedDivisionId = district.divisionId,
                    selectedDistrictId = district.id,
                    selectedUpazilaId = canonicalPrimaryUpaId,
                    selectedUpazilaIds = candidateUpazilaIds,
                    selectedMouzaIds = effectiveMouzaIds,
                    targetTotalMouzas = mouzaPool.size,
                    selectedLandRecordsCount = totalRecords,
                    batchSize = DEFAULT_MOUZAS_PER_BATCH,
                    currentBatchNumber = savedInfo.activeBatchNumber.coerceAtLeast(1),
                    isPaused = savedInfo.isQueuePaused,
                    upazilaFetchInfo = normalizedFetchInfo,
                    plannedQueues = planned,
                    districtSelectionError = null
                )
            }
        }
    }

    val currentDivision: DivisionEntity?
        get() = allDivisions.find { it.id == _uiState.value.selectedDivisionId }

    val districtsInSelectedDivision: List<District>
        get() = allDistricts.filter { it.divisionId == _uiState.value.selectedDivisionId }

    /**
     * Phase 3 — Strict District Resolution:
     * Never falls back to `allDistricts.first()` when an identifier cannot be resolved.
     * Fails closed by returning `null`.
     */
    val currentDistrict: District?
        get() {
            val canonicalId = BangladeshGeographyCatalog.resolveCanonicalDistrictId(_uiState.value.selectedDistrictId)
            if (canonicalId.isEmpty()) return null
            return allDistricts.find { it.id == canonicalId }
        }

    /**
     * Phase 3 — Strict Parent-Child Upazila Filtering:
     * Returns ONLY upazilas whose `districtId == selectedDistrictId`.
     */
    val currentUpazilas: List<Upazila>
        get() {
            val district = currentDistrict ?: return emptyList()
            val selectedDistrictId = district.id
            val allUpazilas = district.upazilas
            return allUpazilas.filter { it.districtId == selectedDistrictId }
        }

    /**
     * Phase 3 — Strict Upazila Resolution:
     * Resolves canonical upazila ID strictly within `currentUpazilas` belonging to `currentDistrict`.
     * Never falls back to the first available upazila when unselected or unresolvable.
     */
    val currentUpazila: Upazila?
        get() {
            val district = currentDistrict ?: return null
            val rawUpaId = _uiState.value.selectedUpazilaId ?: return null
            val canonicalUpaId = BangladeshGeographyCatalog.resolveCanonicalUpazilaId(rawUpaId)
            if (canonicalUpaId.isEmpty()) return null
            return currentUpazilas.find { it.id == canonicalUpaId && it.districtId == district.id }
        }

    /**
     * Phase 3 — Strict District-Verified Mouza Filtering:
     * Loads mouzas ONLY through selected upazilas that belong to the selected district,
     * verifying both `mouza.upazilaId in validSelectedUpazilaIds` AND `upazila.districtId == selectedDistrictId`.
     */
    val currentMouzas: List<Mouza>
        get() {
            val district = currentDistrict ?: return emptyList()
            val selectedDistrictId = district.id
            val districtUpazilas = currentUpazilas.filter { it.districtId == selectedDistrictId }
            val validUpazilaIdsInDistrict = districtUpazilas.map { it.id }.toSet()

            val normalizedSelectedUpazilaIds = _uiState.value.selectedUpazilaIds
                .map { BangladeshGeographyCatalog.resolveCanonicalUpazilaId(it) }
                .filter { it in validUpazilaIdsInDistrict }
                .toSet()

            val activeUpazilaIds: Set<String> = if (normalizedSelectedUpazilaIds.isNotEmpty()) {
                normalizedSelectedUpazilaIds
            } else {
                val primaryUpa = currentUpazila
                if (primaryUpa != null && primaryUpa.districtId == selectedDistrictId) {
                    setOf(primaryUpa.id)
                } else {
                    emptySet()
                }
            }

            if (activeUpazilaIds.isEmpty()) return emptyList()

            val combined = districtUpazilas
                .filter { it.id in activeUpazilaIds && it.districtId == selectedDistrictId }
                .flatMap { upazila ->
                    upazila.mouzas
                        .filter { m ->
                            val mUpaId = BangladeshGeographyCatalog.resolveCanonicalUpazilaId(
                                m.upazilaId.ifBlank { upazila.id }
                            )
                            mUpaId == upazila.id && upazila.districtId == selectedDistrictId
                        }
                        .map { m -> m.copy(upazilaId = upazila.id) }
                }
                .distinctBy { it.id }

            return if (_uiState.value.filterVerifiedOnly) {
                combined.filter { it.verificationStatus == GeoVerificationStatus.VERIFIED }
            } else {
                combined
            }
        }

    val filteredMouzas: List<Mouza>
        get() {
            val query = _uiState.value.searchQuery.trim().lowercase()
            val mouzas = currentMouzas
            if (query.isEmpty()) return mouzas
            return mouzas.filter {
                it.nameBn.lowercase().contains(query) ||
                    it.nameEn.lowercase().contains(query) ||
                    it.jlNo.lowercase().contains(query)
            }
        }

    val totalBatches: Int
        get() {
            val plannedCount = _uiState.value.plannedQueues.size
            val maxFromQueue = _uiState.value.queue.maxOfOrNull { it.batchNumber } ?: 0
            val targetCount = _uiState.value.targetTotalMouzas
            val computedFromTarget = if (targetCount <= 0) 0 else {
                (targetCount + DEFAULT_MOUZAS_PER_BATCH - 1) / DEFAULT_MOUZAS_PER_BATCH
            }
            return maxOf(plannedCount, maxFromQueue, computedFromTarget)
        }

    private fun appendActivityLog(entry: String) {
        _uiState.update { state ->
            state.copy(activityLog = (listOf(entry) + state.activityLog).take(40))
        }
    }

    private fun refreshGeographyCoverageSummary() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val summary = geographyRepository.getCoverageSummary(
                selectedDistrictIds = if (state.selectedDistrictId.isNotBlank()) setOf(state.selectedDistrictId) else emptySet(),
                selectedUpazilaIds = state.selectedUpazilaIds,
                selectedMouzaIds = state.selectedMouzaIds
            )
            _uiState.update { it.copy(geographyCoverageSummary = summary) }
        }
    }

    /**
     * Partitions eligible Mouzas into deterministic 7-Mouza batches (`DEFAULT_MOUZAS_PER_BATCH = 7`).
     */
    private fun computePlannedQueues(
        mouzasPool: List<Mouza>,
        selectedMouzaIds: Set<String>,
        recordTypes: Set<RecordType>,
        queueSize: Int = DEFAULT_MOUZAS_PER_BATCH,
        activeTasks: List<DownloadTask>
    ): List<PlannedQueueSegment> {
        val effectiveSize = DEFAULT_MOUZAS_PER_BATCH
        val effectiveTypes = recordTypes.ifEmpty { RecordType.values().toSet() }
        val targetMouzas = if (selectedMouzaIds.isNotEmpty()) {
            mouzasPool.filter { it.id in selectedMouzaIds }
        } else {
            mouzasPool
        }.distinctBy { it.id }

        if (targetMouzas.isEmpty()) return emptyList()

        val completedMouzaIds = activeTasks
            .filter { it.status == TaskStatus.COMPLETED }
            .map { it.mouzaId }
            .toSet()

        val activeMouzaIds = activeTasks
            .filter { it.status == TaskStatus.DOWNLOADING || it.status == TaskStatus.VALIDATING }
            .map { it.mouzaId }
            .toSet()

        return targetMouzas.chunked(effectiveSize).mapIndexed { index, chunk ->
            val qNum = index + 1
            val completedInChunk = chunk.count { it.id in completedMouzaIds }
            val isChunkDone = completedInChunk == chunk.size && chunk.isNotEmpty()
            val isChunkActive = chunk.any { it.id in activeMouzaIds }
            val recordsSum = chunk.sumOf { it.totalKhatiansFor(effectiveTypes) }

            PlannedQueueSegment(
                queueNumber = qNum,
                mouzas = chunk,
                totalLandRecordsCount = recordsSum,
                isCompleted = isChunkDone,
                isActive = isChunkActive,
                completedMouzasCount = completedInChunk
            )
        }
    }

    /**
     * Step 1: Select Division (cascades cleanly to the first District of that Division).
     */
    fun onDivisionChanged(divisionId: String) {
        hasExplicitUserSelection = true
        val division = allDivisions.find { it.id == divisionId.trim().lowercase() }
        if (division == null) {
            val err = "Unrecognized division identifier: '$divisionId'"
            _uiState.value = _uiState.value.copy(
                districtSelectionError = err,
                cacheNotificationMessage = err
            )
            return
        }
        val firstDistrict = allDistricts.firstOrNull { it.divisionId == division.id } ?: return
        _uiState.value = _uiState.value.copy(
            selectedDivisionId = division.id,
            districtSelectionError = null
        )
        onDistrictChanged(firstDistrict.id)
    }

    /**
     * Phase 3 — Strict District Switch Behavior:
     * Whenever the selected district changes:
     * - Resolves exact canonical district ID (fails closed with error if unknown).
     * - Cancels/isolates previous district-specific pending work.
     * - Clears selected upazila IDs and selected mouza IDs.
     * - Resets search filters and geographic selection counters.
     * - Invalidates previous district's persisted selection snapshot.
     * - Recalculates the queue so previous district data never appears in the new selection.
     * - Preserves completed historical records in Vault/Ledger without mixing them into the new district.
     */
    fun onDistrictChanged(districtId: String) {
        hasExplicitUserSelection = true
        val canonicalDistrictId = BangladeshGeographyCatalog.resolveCanonicalDistrictId(districtId)
        val newDistrict = allDistricts.find { it.id == canonicalDistrictId }

        // Cancel any active background / coroutine queue work from previous district
        DownloadWorkManagerHelper.cancelDownloadWork(getApplication())
        queueCoordinatorJob?.cancel()
        queueCoordinatorJob = null
        taskJobs.values.forEach { it.cancel() }
        taskJobs.clear()

        if (newDistrict == null) {
            val errorMsg = "District '$districtId' could not be resolved in the national catalog."
            _uiState.update {
                it.copy(
                    selectedDistrictId = "",
                    selectedUpazilaId = null,
                    selectedUpazilaIds = emptySet(),
                    isFetchingUpazila = false,
                    upazilaFetchInfo = null,
                    targetTotalMouzas = 0,
                    selectedMouzaIds = emptySet(),
                    selectedLandRecordsCount = 0,
                    searchQuery = "",
                    queue = emptyList(),
                    plannedQueues = emptyList(),
                    currentBatchNumber = 1,
                    masterArchive = null,
                    districtSelectionError = errorMsg,
                    cacheNotificationMessage = errorMsg
                )
            }
            viewModelScope.launch(Dispatchers.IO) {
                upazilaFetchInfoDao.clearFetchInfo()
                repository.clearQueue()
            }
            return
        }

        val profile = NetworkDeviceQueueAdvisor.analyze(getApplication())
        _uiState.update { current ->
            val isolatedQueue = current.queue.filter {
                BangladeshGeographyCatalog.resolveCanonicalDistrictId(it.districtId) == newDistrict.id &&
                    it.status == TaskStatus.COMPLETED
            }
            current.copy(
                selectedDivisionId = newDistrict.divisionId,
                selectedDistrictId = newDistrict.id,
                selectedUpazilaId = null,
                selectedUpazilaIds = emptySet(),
                isFetchingUpazila = false,
                upazilaFetchInfo = null,
                targetTotalMouzas = 0,
                selectedMouzaIds = emptySet(),
                downloadedMouzaIds = emptySet(),
                selectedLandRecordsCount = 0,
                searchQuery = "",
                queue = isolatedQueue,
                plannedQueues = emptyList(),
                deviceNetworkProfile = profile,
                batchSize = DEFAULT_MOUZAS_PER_BATCH,
                currentBatchNumber = 1,
                masterArchive = null,
                districtSelectionError = null,
                cacheNotificationMessage = "জেলা পরিবর্তন: ${newDistrict.nameBn} (${newDistrict.nameEn}) — ${newDistrict.upazilas.size} টি উপজেলা লোড হয়েছে।"
            )
        }

        appendActivityLog("Switched District to ${newDistrict.nameEn} (${newDistrict.upazilas.size} upazilas)")

        val districtOnlySnapshot = UpazilaFetchInfo(
            upazilaId = "__district_only__:${newDistrict.id}",
            upazilaNameBn = "",
            upazilaNameEn = "",
            districtNameBn = newDistrict.nameBn,
            districtNameEn = newDistrict.nameEn,
            fetchedMouzasCount = 0,
            totalQueuesCreated = 0,
            message = "Selected ${newDistrict.nameEn}",
            divisionId = newDistrict.divisionId,
            districtId = newDistrict.id,
            selectedUpazilaIdsCsv = "",
            selectedMouzaIdsCsv = "",
            datasetVersion = BangladeshGeographyCatalog.DATASET_VERSION
        )

        viewModelScope.launch(Dispatchers.IO) {
            persistenceMutex.withLock {
                upazilaFetchInfoDao.clearFetchInfo()
                upazilaFetchInfoDao.insertFetchInfo(districtOnlySnapshot)
                repository.deleteTasksOutsideDistrict(newDistrict.id)
            }
            refreshGeographyCoverageSummary()
        }
    }

    /**
     * Multi-Upazila Toggle:
     * Strictly verifies that `upazilaId` belongs to `currentDistrict`.
     */
    fun toggleUpazilaSelection(upazilaId: String) {
        hasExplicitUserSelection = true
        val district = currentDistrict ?: return
        val canonicalUpaId = BangladeshGeographyCatalog.resolveCanonicalUpazilaId(upazilaId)
        val validUpazila = currentUpazilas.find { it.id == canonicalUpaId && it.districtId == district.id }
        if (validUpazila == null) {
            val err = "Upazila '$upazilaId' does not belong to ${district.nameEn} District."
            _uiState.value = _uiState.value.copy(
                districtSelectionError = err,
                cacheNotificationMessage = err
            )
            return
        }

        val currentSet = _uiState.value.selectedUpazilaIds
            .map { BangladeshGeographyCatalog.resolveCanonicalUpazilaId(it) }
            .filter { id -> currentUpazilas.any { it.id == id && it.districtId == district.id } }
            .toMutableSet()

        if (!currentSet.add(validUpazila.id)) {
            currentSet.remove(validUpazila.id)
        }

        val primaryUpaId = currentSet.firstOrNull()
        _uiState.value = _uiState.value.copy(
            selectedUpazilaId = primaryUpaId,
            selectedUpazilaIds = currentSet,
            districtSelectionError = null
        )

        recalculateSelectionForUpazilas(currentSet, autoSelectAllMouzas = true)
    }

    /**
     * 1-Click "Select All Upazilas" within the currently selected District.
     * Strictly selects ONLY upazilas whose `districtId == currentDistrict.id`.
     */
    fun selectAllUpazilasInDistrict(autoSelectAllMouzas: Boolean = true) {
        hasExplicitUserSelection = true
        val district = currentDistrict ?: return
        val districtUpazilas = currentUpazilas.filter { it.districtId == district.id }
        val allUpazilaIds = districtUpazilas.map { it.id }.toSet()

        _uiState.value = _uiState.value.copy(
            selectedUpazilaId = districtUpazilas.firstOrNull()?.id,
            selectedUpazilaIds = allUpazilaIds,
            districtSelectionError = null
        )

        recalculateSelectionForUpazilas(allUpazilaIds, autoSelectAllMouzas = autoSelectAllMouzas)
        appendActivityLog("Selected all ${allUpazilaIds.size} Upazilas in ${district.nameEn}")
    }

    /**
     * Deselects all Upazilas and clears Mouza selections.
     */
    fun deselectAllUpazilasInDistrict() {
        hasExplicitUserSelection = true
        _uiState.value = _uiState.value.copy(
            selectedUpazilaId = null,
            selectedUpazilaIds = emptySet(),
            selectedMouzaIds = emptySet(),
            targetTotalMouzas = 0,
            selectedLandRecordsCount = 0,
            plannedQueues = emptyList(),
            upazilaFetchInfo = null
        )
        viewModelScope.launch(Dispatchers.IO) {
            upazilaFetchInfoDao.clearFetchInfo()
            refreshGeographyCoverageSummary()
        }
    }

    private fun recalculateSelectionForUpazilas(upazilaIds: Set<String>, autoSelectAllMouzas: Boolean) {
        val district = currentDistrict ?: return
        val profile = NetworkDeviceQueueAdvisor.analyze(getApplication())
        val queueSize = DEFAULT_MOUZAS_PER_BATCH
        val recordTypes = _uiState.value.selectedRecordTypes.ifEmpty { RecordType.values().toSet() }

        val validUpazilas = currentUpazilas.filter { it.districtId == district.id && it.id in upazilaIds }
        val mouzasPool = validUpazilas
            .flatMap { upa -> upa.mouzas.map { m -> m.copy(upazilaId = upa.id) } }
            .distinctBy { it.id }

        val selectedMouzas = if (autoSelectAllMouzas) {
            mouzasPool.map { it.id }.toSet()
        } else {
            _uiState.value.selectedMouzaIds.intersect(mouzasPool.map { it.id }.toSet())
        }

        val totalLandRecords = mouzasPool
            .filter { it.id in selectedMouzas }
            .sumOf { it.totalKhatiansFor(recordTypes) }

        val planned = computePlannedQueues(
            mouzasPool = mouzasPool,
            selectedMouzaIds = selectedMouzas,
            recordTypes = recordTypes,
            queueSize = queueSize,
            activeTasks = _uiState.value.queue
        )

        val primaryUpa = validUpazilas.firstOrNull()
        val upaLabelBn = if (validUpazilas.size == 1 && primaryUpa != null) {
            primaryUpa.nameBn
        } else {
            "${validUpazilas.size} টি উপজেলা (${district.nameBn})"
        }
        val upaLabelEn = if (validUpazilas.size == 1 && primaryUpa != null) {
            primaryUpa.nameEn
        } else {
            "${validUpazilas.size} Upazilas (${district.nameEn})"
        }

        val fetchInfo = if (validUpazilas.isEmpty()) null else UpazilaFetchInfo(
            upazilaId = primaryUpa?.id ?: "${district.id}-multi",
            upazilaNameBn = upaLabelBn,
            upazilaNameEn = upaLabelEn,
            districtNameBn = district.nameBn,
            districtNameEn = district.nameEn,
            fetchedMouzasCount = mouzasPool.size,
            totalQueuesCreated = planned.size,
            message = "$upaLabelBn থেকে মোট ${mouzasPool.size}টি মৌজা ও $totalLandRecords টি রেকর্ড পাওয়া গেছে। প্রতি কিউতে $queueSize টি মৌজা করে মোট ${planned.size}টি কিউ প্রস্তুত।",
            totalLandRecordsCount = totalLandRecords,
            mouzasPerQueue = queueSize,
            networkProfileLabel = profile.networkTier.labelBn,
            deviceProfileLabel = profile.deviceTier.labelBn,
            divisionId = district.divisionId,
            districtId = district.id,
            selectedUpazilaIdsCsv = validUpazilas.joinToString(",") { it.id },
            selectedMouzaIdsCsv = selectedMouzas.joinToString(","),
            datasetVersion = BangladeshGeographyCatalog.DATASET_VERSION,
            activeBatchNumber = _uiState.value.currentBatchNumber,
            isQueuePaused = _uiState.value.isPaused
        )

        _uiState.value = _uiState.value.copy(
            selectedUpazilaId = primaryUpa?.id,
            selectedUpazilaIds = validUpazilas.map { it.id }.toSet(),
            selectedMouzaIds = selectedMouzas,
            targetTotalMouzas = mouzasPool.size,
            selectedLandRecordsCount = totalLandRecords,
            deviceNetworkProfile = profile,
            batchSize = queueSize,
            plannedQueues = planned,
            upazilaFetchInfo = fetchInfo,
            districtSelectionError = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            persistenceMutex.withLock {
                if (fetchInfo != null) {
                    upazilaFetchInfoDao.clearFetchInfo()
                    upazilaFetchInfoDao.insertFetchInfo(fetchInfo)
                }
            }
            refreshGeographyCoverageSummary()
        }
    }

    /**
     * Single Upazila Selection (supports canonical IDs and legacy alias `"adarsha-sadar"` -> `"cumilla-adarsha-sadar"`).
     * Fails closed with an error if the upazila does not belong to `currentDistrict`.
     */
    fun onUpazilaChanged(
        upazilaId: String?,
        autoStartQueues: Boolean = _uiState.value.isAutoQueueOnUpazilaSelect
    ) {
        hasExplicitUserSelection = true
        if (upazilaId.isNullOrBlank()) {
            deselectAllUpazilasInDistrict()
            return
        }
        val district = currentDistrict
        if (district == null) {
            val err = "Cannot select upazila '$upazilaId': no valid district selected."
            _uiState.value = _uiState.value.copy(
                districtSelectionError = err,
                cacheNotificationMessage = err
            )
            return
        }

        val canonicalUpazilaId = BangladeshGeographyCatalog.resolveCanonicalUpazilaId(upazilaId)
        val newUpazila = currentUpazilas.find { it.id == canonicalUpazilaId && it.districtId == district.id }
        if (newUpazila == null) {
            val err = "Upazila '$upazilaId' does not belong to ${district.nameEn} District."
            _uiState.value = _uiState.value.copy(
                districtSelectionError = err,
                cacheNotificationMessage = err
            )
            return
        }

        val profile = NetworkDeviceQueueAdvisor.analyze(getApplication())
        val queueSize = DEFAULT_MOUZAS_PER_BATCH
        val recordTypes = _uiState.value.selectedRecordTypes.ifEmpty { RecordType.values().toSet() }
        val mouzasList = newUpazila.mouzas.map { it.copy(upazilaId = newUpazila.id) }
        val allMouzaIds = mouzasList.map { it.id }.toSet()
        val totalLandRecords = mouzasList.sumOf { it.totalKhatiansFor(recordTypes) }

        val planned = computePlannedQueues(
            mouzasPool = mouzasList,
            selectedMouzaIds = allMouzaIds,
            recordTypes = recordTypes,
            queueSize = queueSize,
            activeTasks = emptyList()
        )

        val fetchMessage = "${district.nameBn} জেলার ${newUpazila.nameBn} উপজেলা থেকে মোট ${mouzasList.size}টি মৌজা (মোট $totalLandRecords টি ভূমি রেকর্ড) লোড হয়েছে। প্রতি কিউতে $queueSize টি মৌজা করে মোট ${planned.size}টি কিউ তৈরি হয়েছে।"

        val fetchInfo = UpazilaFetchInfo(
            upazilaId = newUpazila.id,
            upazilaNameBn = newUpazila.nameBn,
            upazilaNameEn = newUpazila.nameEn,
            districtNameBn = district.nameBn,
            districtNameEn = district.nameEn,
            fetchedMouzasCount = mouzasList.size,
            totalQueuesCreated = planned.size,
            message = fetchMessage,
            totalLandRecordsCount = totalLandRecords,
            mouzasPerQueue = queueSize,
            networkProfileLabel = profile.networkTier.labelBn,
            deviceProfileLabel = profile.deviceTier.labelBn,
            divisionId = district.divisionId,
            districtId = district.id,
            selectedUpazilaIdsCsv = newUpazila.id,
            selectedMouzaIdsCsv = allMouzaIds.joinToString(","),
            datasetVersion = BangladeshGeographyCatalog.DATASET_VERSION,
            activeBatchNumber = 1,
            isQueuePaused = _uiState.value.isPaused
        )

        // Update state synchronously so callers & tests immediately see the resolved selection and 7-mouza plan
        _uiState.update {
            it.copy(
                selectedUpazilaId = newUpazila.id,
                selectedUpazilaIds = setOf(newUpazila.id),
                isFetchingUpazila = false,
                upazilaFetchInfo = fetchInfo,
                targetTotalMouzas = mouzasList.size,
                selectedMouzaIds = allMouzaIds,
                selectedLandRecordsCount = totalLandRecords,
                searchQuery = "",
                deviceNetworkProfile = profile,
                batchSize = queueSize,
                currentBatchNumber = 1,
                plannedQueues = planned,
                masterArchive = null,
                districtSelectionError = null,
                cacheNotificationMessage = fetchMessage
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            persistenceMutex.withLock {
                upazilaFetchInfoDao.clearFetchInfo()
                upazilaFetchInfoDao.insertFetchInfo(fetchInfo)
            }
            refreshGeographyCoverageSummary()
        }

        if (autoStartQueues && mouzasList.isNotEmpty()) {
            enqueueMultiQueues(
                mouzasToQueue = mouzasList,
                district = district,
                upazila = newUpazila,
                queueSize = queueSize,
                startBatchNumber = 1
            )
        }
    }

    fun refreshNetworkDeviceProfile() {
        val profile = NetworkDeviceQueueAdvisor.analyze(getApplication())
        val queueSize = DEFAULT_MOUZAS_PER_BATCH
        val state = _uiState.value
        val planned = computePlannedQueues(
            mouzasPool = currentMouzas,
            selectedMouzaIds = state.selectedMouzaIds,
            recordTypes = state.selectedRecordTypes,
            queueSize = queueSize,
            activeTasks = state.queue
        )
        _uiState.value = state.copy(
            deviceNetworkProfile = profile,
            batchSize = queueSize,
            plannedQueues = planned
        )
    }

    fun setAutoQueueOnUpazilaSelect(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAutoQueueOnUpazilaSelect = enabled)
    }

    fun setRetrievalMode(mode: RetrievalMode) {
        _uiState.value = _uiState.value.copy(selectedRetrievalMode = mode)
        appendActivityLog("Switched Retrieval Mode to ${mode.labelEn}")
    }

    fun setFilterVerifiedOnly(verifiedOnly: Boolean) {
        _uiState.value = _uiState.value.copy(filterVerifiedOnly = verifiedOnly)
        recalculateSelectionForUpazilas(_uiState.value.selectedUpazilaIds, autoSelectAllMouzas = true)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onLedgerSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(ledgerSearchQuery = query)
    }

    fun setDownloadFormat(format: DownloadFormat) {
        _uiState.value = _uiState.value.copy(selectedDownloadFormat = format)
    }

    fun setAutoBatchMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAutoBatchMode = enabled)
    }

    fun toggleRecordType(type: RecordType) {
        val current = _uiState.value.selectedRecordTypes.toMutableSet()
        if (current.contains(type)) {
            if (current.size > 1) current.remove(type)
        } else {
            current.add(type)
        }
        val mouzas = currentMouzas
        val selectedIds = _uiState.value.selectedMouzaIds
        val selectedMouzas = mouzas.filter { it.id in selectedIds }
        val totalRecords = selectedMouzas.sumOf { it.totalKhatiansFor(current) }
        val planned = computePlannedQueues(
            mouzasPool = mouzas,
            selectedMouzaIds = selectedIds,
            recordTypes = current,
            queueSize = DEFAULT_MOUZAS_PER_BATCH,
            activeTasks = _uiState.value.queue
        )

        _uiState.value = _uiState.value.copy(
            selectedRecordTypes = current,
            selectedLandRecordsCount = totalRecords,
            plannedQueues = planned
        )
    }

    fun selectAllRecordTypes() {
        val allTypes = RecordType.values().toSet()
        val mouzas = currentMouzas
        val selectedIds = _uiState.value.selectedMouzaIds
        val totalRecords = mouzas.filter { it.id in selectedIds }.sumOf { it.totalKhatiansFor(allTypes) }
        val planned = computePlannedQueues(
            mouzasPool = mouzas,
            selectedMouzaIds = selectedIds,
            recordTypes = allTypes,
            queueSize = DEFAULT_MOUZAS_PER_BATCH,
            activeTasks = _uiState.value.queue
        )
        _uiState.value = _uiState.value.copy(
            selectedRecordTypes = allTypes,
            selectedLandRecordsCount = totalRecords,
            plannedQueues = planned
        )
    }

    fun toggleMouzaSelection(mouzaId: String) {
        val mouzas = currentMouzas
        if (mouzas.none { it.id == mouzaId }) return

        val current = _uiState.value.selectedMouzaIds.toMutableSet()
        if (!current.add(mouzaId)) {
            current.remove(mouzaId)
        }
        val types = _uiState.value.selectedRecordTypes
        val totalRecords = mouzas.filter { it.id in current }.sumOf { it.totalKhatiansFor(types) }
        val planned = computePlannedQueues(
            mouzasPool = mouzas,
            selectedMouzaIds = current,
            recordTypes = types,
            queueSize = DEFAULT_MOUZAS_PER_BATCH,
            activeTasks = _uiState.value.queue
        )

        _uiState.value = _uiState.value.copy(
            selectedMouzaIds = current,
            selectedLandRecordsCount = totalRecords,
            plannedQueues = planned
        )
        refreshGeographyCoverageSummary()
    }

    fun selectAllMouzas() {
        val mouzas = currentMouzas
        val allIds = mouzas.map { it.id }.toSet()
        val types = _uiState.value.selectedRecordTypes
        val totalRecords = mouzas.sumOf { it.totalKhatiansFor(types) }
        val planned = computePlannedQueues(
            mouzasPool = mouzas,
            selectedMouzaIds = allIds,
            recordTypes = types,
            queueSize = DEFAULT_MOUZAS_PER_BATCH,
            activeTasks = _uiState.value.queue
        )
        _uiState.value = _uiState.value.copy(
            selectedMouzaIds = allIds,
            selectedLandRecordsCount = totalRecords,
            plannedQueues = planned
        )
        refreshGeographyCoverageSummary()
    }

    fun selectFirstBatchMouzas() {
        val mouzas = currentMouzas
        val firstBatchIds = mouzas.take(DEFAULT_MOUZAS_PER_BATCH).map { it.id }.toSet()
        val types = _uiState.value.selectedRecordTypes
        val totalRecords = mouzas.filter { it.id in firstBatchIds }.sumOf { it.totalKhatiansFor(types) }
        val planned = computePlannedQueues(
            mouzasPool = mouzas,
            selectedMouzaIds = firstBatchIds,
            recordTypes = types,
            queueSize = DEFAULT_MOUZAS_PER_BATCH,
            activeTasks = _uiState.value.queue
        )
        _uiState.value = _uiState.value.copy(
            selectedMouzaIds = firstBatchIds,
            selectedLandRecordsCount = totalRecords,
            plannedQueues = planned
        )
        refreshGeographyCoverageSummary()
    }

    fun toggleMouza(mouzaId: String) = toggleMouzaSelection(mouzaId)

    fun selectNextSevenMouzas() = selectFirstBatchMouzas()

    fun clearMouzaSelection() = deselectAllMouzas()

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

    fun generateQueueFromHomepage(context: Context? = null) {
        if (currentMouzas.isEmpty()) {
            selectAllUpazilasInDistrict(autoSelectAllMouzas = true)
        } else if (_uiState.value.selectedMouzaIds.isEmpty()) {
            selectAllMouzas()
        }
        queueSelectedDownloads()
        val totalMouzasCount = _uiState.value.selectedMouzaIds.size.coerceAtLeast(currentMouzas.size)
        val batchesCount = totalBatches.coerceAtLeast(1)
        val msg = "কিউ তৈরি সম্পন্ন: $totalMouzasCount টি মৌজা • $batchesCount টি ব্যাচ ($DEFAULT_MOUZAS_PER_BATCH টি মৌজা/ব্যাচ)"
        _uiState.value = _uiState.value.copy(cacheNotificationMessage = msg)
        if (context != null) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

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
     * Phase 5 — Exact Seven-Mouza Batching:
     * Partitions all selected Mouzas into duplicate-free 7-Mouza batches
     * (Batch 1: Mouza 1–7, Batch 2: Mouza 8–14, etc.) and executes them sequentially.
     */
    fun queueSelectedDownloads() {
        val district = currentDistrict ?: return
        if (currentMouzas.isEmpty()) {
            selectAllUpazilasInDistrict(autoSelectAllMouzas = true)
        }
        val state = _uiState.value
        val mouzasPool = currentMouzas
        if (mouzasPool.isEmpty()) return

        val primaryUpazila = currentUpazila ?: currentUpazilas.firstOrNull() ?: return
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
        val effectiveQueueSize = DEFAULT_MOUZAS_PER_BATCH
        val timestamp = System.currentTimeMillis()
        val state = _uiState.value
        val recordTypes = state.selectedRecordTypes.ifEmpty { RecordType.values().toSet() }
        val format = state.selectedDownloadFormat
        val retrievalMode = state.selectedRetrievalMode

        // Only keep existing tasks from the SAME district
        val existingDistrictTasks = state.queue.filter {
            BangladeshGeographyCatalog.resolveCanonicalDistrictId(it.districtId) == district.id
        }
        val existingTaskMouzaIds = existingDistrictTasks.map { it.mouzaId }.toSet()

        // Strict duplicate prevention: every eligible Mouza appears at most once in the queue
        val filteredMouzas = mouzasToQueue
            .distinctBy { it.id }
            .filter { it.id !in existingTaskMouzaIds }

        if (filteredMouzas.isEmpty()) {
            startSequentialQueueCoordinator()
            return
        }

        val upazilaLookup = district.upazilas
            .filter { it.districtId == district.id }
            .associateBy { it.id }
        val divisionNameBn = allDivisions.find { it.id == district.divisionId }?.nameBn ?: "চট্টগ্রাম"

        val chunks = filteredMouzas.chunked(effectiveQueueSize)
        val newTasks = mutableListOf<DownloadTask>()
        var globalSortOrder = (existingDistrictTasks.maxOfOrNull { it.sortOrder } ?: 0) + 1

        chunks.forEachIndexed { queueIdx, mouzaChunk ->
            val queueNumber = startBatchNumber + queueIdx
            mouzaChunk.forEachIndexed { itemIdx, mouza ->
                val canonicalMouzaUpaId = BangladeshGeographyCatalog.resolveCanonicalUpazilaId(
                    mouza.upazilaId.ifBlank { upazila.id }
                )
                val resolvedUpa = upazilaLookup[canonicalMouzaUpaId] ?: upazila
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
                        sourceName = "eporcha.tech Free Land Records Portal",
                        sourceReference = FREE_PORTAL_URL,
                        sortOrder = globalSortOrder++
                    )
                )
            }
        }

        val updatedQueue = existingDistrictTasks + newTasks
        val updatedPlanned = computePlannedQueues(
            mouzasPool = currentMouzas,
            selectedMouzaIds = state.selectedMouzaIds,
            recordTypes = recordTypes,
            queueSize = effectiveQueueSize,
            activeTasks = updatedQueue
        )

        _uiState.update {
            it.copy(
                queue = updatedQueue,
                batchSize = effectiveQueueSize,
                currentBatchNumber = startBatchNumber,
                plannedQueues = updatedPlanned,
                isShowingPreQueueConfirmationDialog = false
            )
        }

        appendActivityLog("Generated ${newTasks.size} tasks across ${chunks.size} batches ($effectiveQueueSize Mouzas/batch)")

        viewModelScope.launch(Dispatchers.IO) {
            persistenceMutex.withLock {
                repository.deleteTasksOutsideDistrict(district.id)
                repository.insertTasksToQueue(newTasks)
                state.upazilaFetchInfo?.let {
                    upazilaFetchInfoDao.insertFetchInfo(
                        it.copy(
                            mouzasPerQueue = effectiveQueueSize,
                            districtId = district.id,
                            divisionId = district.divisionId,
                            activeBatchNumber = startBatchNumber
                        )
                    )
                }
            }
            if (!_uiState.value.isPaused) {
                startSequentialQueueCoordinator()
                if (_uiState.value.useWorkManagerBackgroundService && retrievalMode == RetrievalMode.MODE_A_AUTHORIZED_API) {
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
     * Executes Batch #1 completely, then advances to Batch #2, ..., up to Batch #N.
     * Enforces honest task states without simulated completions.
     */
    private fun startSequentialQueueCoordinator() {
        if (queueCoordinatorJob?.isActive == true) return

        queueCoordinatorJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                while (_uiState.value.isPaused) {
                    delay(250)
                }

                val allTasks = persistenceMutex.withLock {
                    val activeDistrictId = BangladeshGeographyCatalog.resolveCanonicalDistrictId(_uiState.value.selectedDistrictId)
                    repository.getAllTasksDirect().filter {
                        BangladeshGeographyCatalog.resolveCanonicalDistrictId(it.districtId) == activeDistrictId
                    }
                }
                if (allTasks.isEmpty()) break

                val incompleteTasks = allTasks.filter {
                    it.status == TaskStatus.QUEUED ||
                        it.status == TaskStatus.PENDING ||
                        it.status == TaskStatus.DOWNLOADING ||
                        it.status == TaskStatus.WAITING_FOR_USER
                }

                if (incompleteTasks.isEmpty()) {
                    val upazila = currentUpazila
                    val district = currentDistrict
                    val hasAnyCompleted = allTasks.any { it.status == TaskStatus.COMPLETED }
                    if (_uiState.value.isAutoBatchMode && upazila != null && district != null &&
                        _uiState.value.selectedRetrievalMode == RetrievalMode.MODE_A_AUTHORIZED_API &&
                        hasAnyCompleted
                    ) {
                        val queuedOrDoneIds = allTasks.map { it.mouzaId }.toSet()
                        val remainingMouzas = currentMouzas.filter { it.id !in queuedOrDoneIds }
                        if (remainingMouzas.isNotEmpty()) {
                            val nextBatchStart = (allTasks.maxOfOrNull { it.batchNumber } ?: 0) + 1
                            enqueueMultiQueues(
                                mouzasToQueue = remainingMouzas,
                                district = district,
                                upazila = upazila,
                                queueSize = DEFAULT_MOUZAS_PER_BATCH,
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
                                executeTaskHonestLifecycle(task.id)
                            }
                        }
                    }
                }

                delay(120)
            }
        }
    }

    /**
     * Phase 6 — Honest Task State Execution with https://eporcha.tech/ (100% Free Portal, Zero Billing Limitations):
     * - No artificial download speeds (`speedKbps = 0`).
     * - No artificial progress percentages (`progress = 0` until a real document is imported and verified).
     * - Connects to https://eporcha.tech/ (replacing paid eporcha.gov.bd / land.gov.bd portals) and transitions
     *   tasks to `AWAITING_USER_IMPORT` when interactive PDF retrieval/import from https://eporcha.tech/ is ready.
     */
    private suspend fun executeTaskHonestLifecycle(taskId: String) {
        val initialTask = _uiState.value.queue.find { it.id == taskId }
            ?: repository.getTaskById(taskId)
            ?: return
        if (initialTask.status == TaskStatus.COMPLETED ||
            initialTask.status == TaskStatus.CANCELLED ||
            initialTask.status == TaskStatus.UNAVAILABLE ||
            initialTask.status == TaskStatus.AWAITING_USER_IMPORT ||
            initialTask.status == TaskStatus.FAILED
        ) {
            return
        }

        when (initialTask.retrievalMode) {
            RetrievalMode.MODE_A_AUTHORIZED_API -> {
                delay(400)
                if (_uiState.value.isPaused) return
                updateTaskSync(taskId) {
                    it.copy(
                        status = TaskStatus.AWAITING_USER_IMPORT,
                        progress = 0,
                        speedKbps = 0,
                        sourceName = "eporcha.tech Free Land Records Portal",
                        sourceReference = FREE_PORTAL_URL,
                        errorDetails = null
                    )
                }
            }
            RetrievalMode.MODE_B_USER_ASSISTED -> {
                // Allow initial WAITING_FOR_USER state to be observed before transitioning to AWAITING_USER_IMPORT
                delay(1400)
                if (_uiState.value.isPaused) return
                updateTaskSync(taskId) {
                    it.copy(
                        status = TaskStatus.AWAITING_USER_IMPORT,
                        progress = 0,
                        speedKbps = 0,
                        sourceName = "eporcha.tech Free Land Records Portal",
                        sourceReference = FREE_PORTAL_URL,
                        errorDetails = null
                    )
                }
            }
        }
    }

    /**
     * Free Open-Access Portal Retrieval: Opens https://eporcha.tech/
     * (100% free to all, zero billing limitations or paid portal fees).
     */
    fun openUserAssistedBrowserForTask(context: Context, task: DownloadTask? = null) {
        val url = FREE_PORTAL_URL
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            appendActivityLog("Opened free portal $url for ${task?.mouzaNameEn ?: "Land Records"}")
        } catch (e: Exception) {
            Toast.makeText(context, "Open free portal: $url", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Phase 6 & 7 — Genuine PDF Import, Validation & Archiving for a Queue Task:
     * Transitions: WAITING_FOR_USER / AWAITING_USER_IMPORT -> IMPORTED -> VALIDATING -> VERIFIED -> COMPLETED
     * (or FAILED on invalid/empty/corrupted/duplicate PDF).
     */
    fun importPdfForTask(
        context: Context,
        taskId: String,
        pdfBytes: ByteArray,
        fileName: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = _uiState.value.queue.find { it.id == taskId }
                ?: repository.getTaskById(taskId)
                ?: return@launch

            // Step 1: IMPORTED
            updateTaskSync(taskId) {
                it.copy(status = TaskStatus.IMPORTED, progress = 0, speedKbps = 0, errorDetails = null)
            }

            // Step 2: VALIDATING
            updateTaskSync(taskId) {
                it.copy(status = TaskStatus.VALIDATING, progress = 0, speedKbps = 0)
            }

            val validation = DocumentVerificationEngine.verifyDocumentBytes(
                bytes = pdfBytes,
                declaredMimeType = "application/pdf",
                fileName = fileName
            )

            if (!validation.isValid) {
                val errMsg = "PDF Validation Failed: ${validation.errorReason}"
                _uiState.value = _uiState.value.copy(lastImportStatusMessage = errMsg)
                appendActivityLog(errMsg)
                updateTaskSync(taskId) {
                    it.copy(
                        status = TaskStatus.FAILED,
                        progress = 0,
                        speedKbps = 0,
                        errorDetails = errMsg
                    )
                }
                return@launch
            }

            // Duplicate-document detection via SHA-256
            val existingEntry = masterLandLedgerDao.findBySha256(validation.sha256Checksum)
            if (existingEntry != null) {
                val dupMsg = "Duplicate PDF rejected (SHA-256: ${validation.sha256Checksum.take(12)}... already archived under ${existingEntry.mouzaName})"
                _uiState.value = _uiState.value.copy(lastImportStatusMessage = dupMsg)
                appendActivityLog(dupMsg)
                updateTaskSync(taskId) {
                    it.copy(
                        status = TaskStatus.FAILED,
                        progress = 0,
                        speedKbps = 0,
                        errorDetails = dupMsg
                    )
                }
                return@launch
            }

            // Step 3: VERIFIED
            updateTaskSync(taskId) {
                it.copy(
                    status = TaskStatus.VERIFIED,
                    sha256Checksum = validation.sha256Checksum,
                    fileSizeBytes = validation.fileSizeBytes,
                    speedKbps = 0
                )
            }

            val canonicalDistrictId = BangladeshGeographyCatalog.resolveCanonicalDistrictId(task.districtId)
            val canonicalUpazilaId = BangladeshGeographyCatalog.resolveCanonicalUpazilaId(task.upazilaId)
            val district = allDistricts.find { it.id == canonicalDistrictId }
                ?: currentDistrict
                ?: return@launch
            val upazila = district.upazilas.find { it.id == canonicalUpazilaId }
                ?: district.upazilas.firstOrNull()
                ?: return@launch

            val savedResult = LandRecordStorageManager.importAndVerifyUserPdf(
                context = context,
                pdfBytes = pdfBytes,
                originalFileName = fileName,
                divisionNameEn = district.divisionId,
                districtNameEn = district.nameEn,
                upazilaNameEn = upazila.nameEn,
                mouzaNameEn = task.mouzaNameEn,
                jlNo = task.jlNo,
                surveyType = task.surveyType
            )

            if (savedResult.validationResult?.isValid != true || savedResult.fileSizeBytes <= 0L || savedResult.sha256Checksum != validation.sha256Checksum) {
                val storageErr = "Storage verification failed for $fileName"
                updateTaskSync(taskId) {
                    it.copy(status = TaskStatus.FAILED, progress = 0, speedKbps = 0, errorDetails = storageErr)
                }
                return@launch
            }

            // Step 4: COMPLETED
            val completedAt = System.currentTimeMillis()
            updateTaskSync(taskId) { t ->
                t.copy(
                    status = TaskStatus.COMPLETED,
                    progress = 100,
                    downloadedKhatians = 1,
                    speedKbps = 0,
                    fileLocation = savedResult.primaryDisplayPath,
                    fileSizeBytes = savedResult.fileSizeBytes,
                    sha256Checksum = savedResult.sha256Checksum,
                    completedTimestamp = completedAt,
                    errorDetails = null
                )
            }

            _uiState.value = _uiState.value.copy(
                downloadedMouzaIds = _uiState.value.downloadedMouzaIds + task.mouzaId,
                lastImportStatusMessage = "Successfully imported & verified $fileName for ${task.mouzaNameBn}"
            )

            val sizeFormatted = String.format("%.1f KB", savedResult.fileSizeBytes / 1024.0)

            repository.insertSavedRecord(
                SavedRecord(
                    id = "${task.id}-${savedResult.sha256Checksum.take(8)}",
                    mouzaId = task.mouzaId,
                    mouzaNameBn = task.mouzaNameBn,
                    mouzaNameEn = task.mouzaNameEn,
                    jlNo = task.jlNo,
                    upazilaNameBn = upazila.nameBn,
                    districtNameBn = district.nameBn,
                    recordType = task.recordTypeLabel,
                    khatianCount = 1,
                    fileSizeFormatted = sizeFormatted,
                    downloadedAt = completedAt,
                    divisionNameBn = task.divisionNameBn,
                    fileLocation = savedResult.primaryDisplayPath,
                    sha256Checksum = savedResult.sha256Checksum,
                    sourceName = "User Imported PDF ($fileName)",
                    verificationStatus = DocumentVerificationStatus.CHECKSUM_VERIFIED.name
                )
            )

            masterLandLedgerDao.insertEntry(
                MasterLedgerEntryEntity(
                    divisionId = district.divisionId,
                    divisionName = task.divisionNameBn,
                    districtId = district.id,
                    districtName = district.nameBn,
                    upazilaId = upazila.id,
                    upazilaName = upazila.nameBn,
                    mouzaId = task.mouzaId,
                    mouzaName = "${task.mouzaNameBn} (${task.mouzaNameEn})",
                    jlNumber = task.jlNo,
                    surveyType = task.recordTypeLabel,
                    khatianNumber = task.referenceId ?: "1",
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

            appendActivityLog("Verified & archived PDF task ${task.id} (SHA-256: ${savedResult.sha256Checksum.take(12)}...)")
        }
    }

    /**
     * Imports and validates a genuine PDF document supplied by the user into the structured Mouza archive
     * and records its SHA-256 and provenance in the Master Land Ledger (Phases 6 & 7).
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
        viewModelScope.launch(Dispatchers.IO) {
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

            val district = currentDistrict
            if (district == null) {
                val errMsg = "Please select a valid District before importing a PDF."
                _uiState.value = _uiState.value.copy(lastImportStatusMessage = errMsg)
                return@launch
            }
            val upazila = currentUpazila ?: currentUpazilas.firstOrNull()
            if (upazila == null) {
                val errMsg = "Please select a valid Upazila in ${district.nameEn} before importing a PDF."
                _uiState.value = _uiState.value.copy(lastImportStatusMessage = errMsg)
                return@launch
            }
            val mouza = currentMouzas.firstOrNull { it.id in _uiState.value.selectedMouzaIds }
                ?: upazila.mouzas.firstOrNull()
                ?: Mouza("imported-${upazila.id}", "ইমপোর্টকৃত মৌজা", "Imported Mouza", "০১", upazilaId = upazila.id)

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

            val now = System.currentTimeMillis()
            val divisionNameBn = currentDivision?.nameBn ?: "চট্টগ্রাম"

            masterLandLedgerDao.insertEntry(
                MasterLedgerEntryEntity(
                    divisionId = district.divisionId,
                    divisionName = divisionNameBn,
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
                    sha256Checksum = savedResult.sha256Checksum,
                    recordedAt = now
                )
            )

            val sizeFormatted = String.format("%.1f KB", savedResult.fileSizeBytes / 1024.0)
            repository.insertSavedRecord(
                SavedRecord(
                    id = "import-${mouza.id}-${savedResult.sha256Checksum.take(10)}",
                    mouzaId = mouza.id,
                    mouzaNameBn = mouza.nameBn,
                    mouzaNameEn = mouza.nameEn,
                    jlNo = mouza.jlNo,
                    upazilaNameBn = upazila.nameBn,
                    districtNameBn = district.nameBn,
                    recordType = surveyType,
                    khatianCount = 1,
                    fileSizeFormatted = sizeFormatted,
                    downloadedAt = now,
                    divisionNameBn = divisionNameBn,
                    fileLocation = savedResult.primaryDisplayPath,
                    sha256Checksum = savedResult.sha256Checksum,
                    sourceName = "User Imported Official PDF ($fileName)",
                    verificationStatus = DocumentVerificationStatus.CHECKSUM_VERIFIED.name
                )
            )

            // If there is a waiting task for this mouza, mark it COMPLETED via this verified PDF
            val matchingTask = _uiState.value.queue.firstOrNull {
                it.mouzaId == mouza.id && it.status != TaskStatus.COMPLETED
            }
            if (matchingTask != null) {
                updateTaskSync(matchingTask.id) { t ->
                    t.copy(
                        status = TaskStatus.COMPLETED,
                        progress = 100,
                        downloadedKhatians = 1,
                        speedKbps = 0,
                        fileLocation = savedResult.primaryDisplayPath,
                        fileSizeBytes = savedResult.fileSizeBytes,
                        sha256Checksum = savedResult.sha256Checksum,
                        completedTimestamp = now,
                        errorDetails = null
                    )
                }
            }

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
            status = if (_uiState.value.selectedRetrievalMode == RetrievalMode.MODE_B_USER_ASSISTED) {
                TaskStatus.WAITING_FOR_USER
            } else {
                TaskStatus.PENDING
            },
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
            referenceId = khotiyanLabel,
            retrievalMode = _uiState.value.selectedRetrievalMode
        )

        viewModelScope.launch {
            repository.insertTasksToQueue(listOf(task))
            if (!_uiState.value.isPaused) {
                startSequentialQueueCoordinator()
                if (_uiState.value.useWorkManagerBackgroundService &&
                    _uiState.value.selectedRetrievalMode == RetrievalMode.MODE_A_AUTHORIZED_API
                ) {
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
        val queueSize = DEFAULT_MOUZAS_PER_BATCH
        val startBatch = (_uiState.value.queue.maxOfOrNull { it.batchNumber } ?: 0) + 1
        val retrievalMode = _uiState.value.selectedRetrievalMode

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
                        status = if (retrievalMode == RetrievalMode.MODE_B_USER_ASSISTED) {
                            TaskStatus.WAITING_FOR_USER
                        } else {
                            TaskStatus.PENDING
                        },
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
                        upazilaId = upazila.id,
                        retrievalMode = retrievalMode
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
                if (_uiState.value.useWorkManagerBackgroundService && retrievalMode == RetrievalMode.MODE_A_AUTHORIZED_API) {
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
        val current = _uiState.value.queue.find { it.id == taskId } ?: repository.getTaskById(taskId) ?: return
        val updated = transform(current)
        _uiState.update { currentState ->
            currentState.copy(queue = currentState.queue.map { if (it.id == taskId) updated else it })
        }
        persistenceMutex.withLock {
            repository.updateTaskInQueue(updated)
        }
    }

    private fun compileMasterArchive(district: District, upazila: Upazila) {
        val completedTasks = _uiState.value.queue.filter { it.status == TaskStatus.COMPLETED }
        val actualKhatians = completedTasks.sumOf { it.downloadedKhatians.coerceAtLeast(1) }
        val totalBytes = completedTasks.sumOf { it.fileSizeBytes }
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
            totalMouzasCount = completedTasks.map { it.mouzaId }.distinct().size,
            recordTypesIncluded = typesText,
            totalKhatiansCount = actualKhatians,
            totalSizeMb = sizeFormatted,
            fileName = fileName,
            isReady = completedTasks.isNotEmpty(),
            isDownloadedToPhone = false,
            phoneSavedPath = null
        )

        _uiState.value = _uiState.value.copy(masterArchive = archive)
    }

    fun downloadMasterFileToPhone(context: Context) {
        val archive = _uiState.value.masterArchive ?: return
        val upazila = currentUpazila ?: currentUpazilas.firstOrNull() ?: return
        val district = currentDistrict ?: return

        viewModelScope.launch {
            val ledgerRows = masterLandLedgerDao.getAllEntriesDirect()
            val completedTasks = _uiState.value.queue.filter { it.status == TaskStatus.COMPLETED }
            val saveResult = LandRecordStorageManager.saveMasterLandRecordsArchive(
                context = context,
                archive = archive,
                district = district,
                upazila = upazila,
                queueTasks = completedTasks,
                ledgerEntries = ledgerRows
            )

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
        val upazila = currentUpazila ?: currentUpazilas.firstOrNull() ?: return
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
        val upazila = currentUpazila ?: currentUpazilas.firstOrNull() ?: return
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
        val upazila = currentUpazila ?: currentUpazilas.firstOrNull() ?: return
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
                _uiState.value.upazilaFetchInfo?.let {
                    upazilaFetchInfoDao.insertFetchInfo(it.copy(isQueuePaused = true))
                }
            }
            appendActivityLog("Queue Paused")
        } else {
            viewModelScope.launch {
                _uiState.value.upazilaFetchInfo?.let {
                    upazilaFetchInfoDao.insertFetchInfo(it.copy(isQueuePaused = false))
                }
            }
            startSequentialQueueCoordinator()
            if (_uiState.value.useWorkManagerBackgroundService &&
                _uiState.value.selectedRetrievalMode == RetrievalMode.MODE_A_AUTHORIZED_API
            ) {
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
            val all = repository.getAllTasksDirect().filter {
                it.status == TaskStatus.PENDING || it.status == TaskStatus.QUEUED
            }
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
            queueSize = DEFAULT_MOUZAS_PER_BATCH,
            activeTasks = emptyList()
        )
        _uiState.value = _uiState.value.copy(
            isPaused = false,
            queue = emptyList(),
            currentBatchNumber = 1,
            plannedQueues = updatedPlanned
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearQueue()
        }
    }

    fun setUseWorkManagerBackgroundService(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(useWorkManagerBackgroundService = enabled)
        if (enabled) {
            if (!_uiState.value.isPaused && _uiState.value.queue.any {
                it.status == TaskStatus.PENDING || it.status == TaskStatus.QUEUED
            }) {
                startSequentialQueueCoordinator()
                if (_uiState.value.selectedRetrievalMode == RetrievalMode.MODE_A_AUTHORIZED_API) {
                    DownloadWorkManagerHelper.enqueueDownloadWork(
                        context = getApplication(),
                        replace = false,
                        autoBatchMode = _uiState.value.isAutoBatchMode
                    )
                }
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
        if (_uiState.value.selectedRetrievalMode == RetrievalMode.MODE_A_AUTHORIZED_API) {
            DownloadWorkManagerHelper.enqueueDownloadWork(
                context = getApplication(),
                replace = true,
                autoBatchMode = _uiState.value.isAutoBatchMode
            )
        }
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
                val resetStatus = if (task.retrievalMode == RetrievalMode.MODE_B_USER_ASSISTED) {
                    TaskStatus.WAITING_FOR_USER
                } else {
                    TaskStatus.PENDING
                }
                val resetTask = task.copy(
                    status = resetStatus,
                    progress = 0,
                    speedKbps = 0,
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
                val resetStatus = if (task.retrievalMode == RetrievalMode.MODE_B_USER_ASSISTED) {
                    TaskStatus.WAITING_FOR_USER
                } else {
                    TaskStatus.PENDING
                }
                val reset = task.copy(
                    status = resetStatus,
                    progress = 0,
                    speedKbps = 0,
                    downloadedKhatians = 0,
                    errorDetails = null
                )
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
