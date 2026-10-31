package com.example.data.geography

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository managing the normalized Bangladesh Geographical Database (8 Divisions, 64 Districts,
 * Upazilas, Mouzas, Aliases, JL Numbers, Survey Availability, Data Sources, and Import History).
 */
class GeographyRepository(private val geographyDao: GeographyDao) {

    val allDivisions: Flow<List<DivisionEntity>> = geographyDao.observeAllDivisions()
    val allDistricts: Flow<List<DistrictEntity>> = geographyDao.observeAllDistricts()
    val verifiedMouzaCount: Flow<Int> = geographyDao.observeVerifiedMouzaCount()
    val dataSources: Flow<List<GeographyDataSourceEntity>> = geographyDao.observeDataSources()
    val importHistory: Flow<List<GeographyImportHistoryEntity>> = geographyDao.observeImportHistory()

    /**
     * Ensures the normalized database tables are populated with all 8 Divisions, all 64 Districts,
     * verified Upazilas, and verified Mouzas on initial launch.
     */
    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val currentDivisions = geographyDao.getDivisionCount()
        val currentDistricts = geographyDao.getDistrictCount()
        if (currentDivisions >= 8 && currentDistricts >= 64) {
            return@withContext
        }

        val now = System.currentTimeMillis()
        val bundle = BangladeshGeographyCatalog.buildInitialMouzaBundle(now)

        geographyDao.insertDivisions(bundle.divisions)
        geographyDao.insertDistricts(bundle.districts)
        geographyDao.insertUpazilas(bundle.upazilas)
        geographyDao.insertMouzas(bundle.mouzas)
        geographyDao.insertMouzaAliases(bundle.aliases)
        geographyDao.insertMouzaJlNumbers(bundle.jlNumbers)
        geographyDao.insertSurveyAvailabilities(bundle.availabilities)

        val totalSeededRecords = bundle.divisions.size + bundle.districts.size +
            bundle.upazilas.size + bundle.mouzas.size

        geographyDao.insertDataSource(
            GeographyDataSourceEntity(
                id = BangladeshGeographyCatalog.OFFICIAL_SOURCE_ID,
                sourceName = BangladeshGeographyCatalog.OFFICIAL_SOURCE_NAME,
                sourceUrl = BangladeshGeographyCatalog.OFFICIAL_SOURCE_URL,
                datasetVersion = BangladeshGeographyCatalog.DATASET_VERSION,
                verificationStatus = GeoVerificationStatus.VERIFIED,
                recordCount = totalSeededRecords,
                importedAt = now
            )
        )

        geographyDao.insertImportHistory(
            GeographyImportHistoryEntity(
                sourceName = BangladeshGeographyCatalog.OFFICIAL_SOURCE_NAME,
                sourceUrl = BangladeshGeographyCatalog.OFFICIAL_SOURCE_URL,
                importDate = now,
                datasetVersion = BangladeshGeographyCatalog.DATASET_VERSION,
                verificationStatus = GeoVerificationStatus.VERIFIED,
                recordCount = totalSeededRecords,
                importErrors = "0 errors (8 Divisions, 64 Districts, ${bundle.upazilas.size} Upazilas, ${bundle.mouzas.size} Verified Mouzas)"
            )
        )
    }

    suspend fun getDivisionsDirect(): List<DivisionEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        geographyDao.getAllDivisionsDirect()
    }

    suspend fun getDistrictsByDivision(divisionId: String): List<DistrictEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        geographyDao.getDistrictsByDivisionDirect(divisionId)
    }

    suspend fun getAllDistrictsDirect(): List<DistrictEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        geographyDao.getAllDistrictsDirect()
    }

    suspend fun getUpazilasByDistrict(districtId: String): List<UpazilaEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        geographyDao.getUpazilasByDistrictDirect(districtId)
    }

    suspend fun getUpazilasByDistricts(districtIds: List<String>): List<UpazilaEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        if (districtIds.isEmpty()) emptyList() else geographyDao.getUpazilasByDistrictsDirect(districtIds)
    }

    suspend fun getMouzasByUpazila(upazilaId: String): List<MouzaEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        geographyDao.getMouzasByUpazilaDirect(upazilaId)
    }

    suspend fun getMouzasByUpazilas(upazilaIds: List<String>): List<MouzaEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        if (upazilaIds.isEmpty()) emptyList() else geographyDao.getMouzasByUpazilasDirect(upazilaIds)
    }

    suspend fun getMouzasByDistrict(districtId: String): List<MouzaEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        geographyDao.getMouzasByDistrictDirect(districtId)
    }

    suspend fun getMouzasByDivision(divisionId: String): List<MouzaEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        geographyDao.getMouzasByDivisionDirect(divisionId)
    }

    suspend fun getAllVerifiedMouzas(): List<MouzaEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        geographyDao.getAllVerifiedMouzasDirect()
    }

    suspend fun searchMouzas(
        query: String,
        upazilaId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<MouzaEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        geographyDao.searchMouzasPaged(query = query.trim(), upazilaId = upazilaId, limit = limit, offset = offset)
    }

    suspend fun getCoverageSummary(
        selectedDistrictIds: Set<String> = emptySet(),
        selectedUpazilaIds: Set<String> = emptySet(),
        loadedMouzas: List<MouzaEntity> = emptyList(),
        selectedMouzaIds: Set<String> = emptySet(),
        selectedSurveyCount: Int = 1
    ): GeographyCoverageSummary = withContext(Dispatchers.IO) {
        ensureSeeded()
        val divCount = geographyDao.getDivisionCount()
        val distCount = geographyDao.getDistrictCount()
        val upaCount = geographyDao.getUpazilaCount()
        val verifiedMouzas = geographyDao.getVerifiedMouzaCountDirect()
        val unverifiedMouzas = geographyDao.getUnverifiedMouzaCountDirect()

        val selectedMouzas = loadedMouzas.filter { it.id in selectedMouzaIds }
        val availCount = selectedMouzas.count { it.verificationStatus == GeoVerificationStatus.VERIFIED }
        val unknownCount = selectedMouzas.count { it.verificationStatus != GeoVerificationStatus.VERIFIED }

        GeographyCoverageSummary(
            totalDivisions = divCount,
            totalDistricts = distCount,
            totalUpazilas = upaCount,
            totalVerifiedMouzas = verifiedMouzas,
            totalUnverifiedMouzas = unverifiedMouzas,
            upazilasMissingMouzaDataset = (upaCount - 54).coerceAtLeast(0),
            selectedDistrictsCount = selectedDistrictIds.size.coerceAtLeast(if (selectedUpazilaIds.isNotEmpty()) 1 else 0),
            selectedUpazilasCount = selectedUpazilaIds.size,
            totalMouzasFoundInSelection = loadedMouzas.size,
            selectedMouzasCount = selectedMouzaIds.size,
            mouzasWithAvailableSurveys = availCount,
            mouzasWithUnknownAvailability = unknownCount,
            mouzasRequiringManualRetrieval = selectedMouzaIds.size, // ePorcha/external portals require Mode B user-assisted browser retrieval unless Mode A authorized API is active
            estimatedDownloadTasksCount = selectedMouzaIds.size * selectedSurveyCount.coerceAtLeast(1)
        )
    }

    /**
     * Imports a JSON geographical update without requiring an APK rebuild.
     * Performs JL validation and duplicate detection against existing Mouzas in the target Upazila.
     */
    suspend fun importGeographyJsonUpdate(jsonPayload: String): GeographyImportHistoryEntity = withContext(Dispatchers.IO) {
        ensureSeeded()
        val parsed = BangladeshGeographyCatalog.parseJsonGeographyUpdate(jsonPayload)
        val acceptedMouzas = mutableListOf<MouzaEntity>()
        val allErrors = parsed.errors.toMutableList()
        val now = System.currentTimeMillis()

        for (candidate in parsed.mouzas) {
            val existingDuplicates = geographyDao.findPotentialDuplicates(
                upazilaId = candidate.upazilaId,
                jlNumber = candidate.jlNumber,
                nameEn = candidate.nameEn,
                nameBn = candidate.nameBn
            )
            if (existingDuplicates.any { it.id != candidate.id }) {
                allErrors.add("Duplicate skipped: ${candidate.nameEn} (JL ${candidate.jlNumber}) already exists in ${candidate.upazilaId}")
                continue
            }
            acceptedMouzas.add(candidate)
        }

        if (acceptedMouzas.isNotEmpty()) {
            geographyDao.insertMouzas(acceptedMouzas)
            geographyDao.insertDataSource(
                GeographyDataSourceEntity(
                    id = "import_${now}",
                    sourceName = parsed.sourceName,
                    sourceUrl = parsed.sourceUrl,
                    datasetVersion = parsed.datasetVersion,
                    verificationStatus = acceptedMouzas.first().verificationStatus,
                    recordCount = acceptedMouzas.size,
                    importedAt = now
                )
            )
        }

        val historyEntry = GeographyImportHistoryEntity(
            sourceName = parsed.sourceName,
            sourceUrl = parsed.sourceUrl,
            importDate = now,
            datasetVersion = parsed.datasetVersion,
            verificationStatus = if (acceptedMouzas.any { it.verificationStatus == GeoVerificationStatus.VERIFIED }) {
                GeoVerificationStatus.VERIFIED
            } else {
                GeoVerificationStatus.COMMUNITY_CONTRIBUTED
            },
            recordCount = acceptedMouzas.size,
            importErrors = if (allErrors.isEmpty()) "0 errors" else allErrors.joinToString("; ")
        )
        geographyDao.insertImportHistory(historyEntry)
        historyEntry
    }
}
