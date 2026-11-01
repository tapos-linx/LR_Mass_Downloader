package com.example.data.geography

import kotlinx.coroutines.flow.Flow

/**
 * Authoritative Repository for the Bangladesh 8-Division, 64-District, Upazila, and Mouza
 * geographical database.
 */
class GeographyRepository(private val geographyDao: GeographyDao) {

    val dataSources: Flow<List<GeographyDataSourceEntity>> = geographyDao.observeDataSources()

    val importHistory: Flow<List<GeographyImportHistoryEntity>> = geographyDao.observeImportHistory()

    fun observeDivisions(): Flow<List<DivisionEntity>> = geographyDao.observeAllDivisions()

    fun observeAllDistricts(): Flow<List<DistrictEntity>> = geographyDao.observeAllDistricts()

    fun observeAllUpazilas(): Flow<List<UpazilaEntity>> = geographyDao.observeAllUpazilas()

    fun observeAllMouzas(): Flow<List<MouzaEntity>> = geographyDao.observeAllMouzas()

    fun observeDistrictsByDivision(divisionId: String): Flow<List<DistrictEntity>> =
        geographyDao.observeDistrictsByDivision(divisionId)

    fun observeUpazilasByDistrict(districtId: String): Flow<List<UpazilaEntity>> =
        geographyDao.observeUpazilasByDistrict(BangladeshGeographyCatalog.resolveCanonicalDistrictId(districtId))

    fun observeMouzasByUpazila(upazilaId: String): Flow<List<MouzaEntity>> =
        geographyDao.observeMouzasByUpazila(BangladeshGeographyCatalog.resolveCanonicalUpazilaId(upazilaId))

    fun observeImportHistory(): Flow<List<GeographyImportHistoryEntity>> =
        geographyDao.observeImportHistory()

    /**
     * Ensures the Room database is seeded with the complete 8-Division, 64-District,
     * and official Upazila/Mouza catalog, and reconciles any legacy `"adarsha-sadar"` records.
     */
    suspend fun ensureSeeded() {
        val existingDivisions = geographyDao.getDivisionsCount()
        val existingDistricts = geographyDao.getDistrictsCount()
        val existingUpazilas = geographyDao.getUpazilasCount()
        val catalogUpazilasCount = BangladeshGeographyCatalog.upazilas.size

        if (existingDivisions >= 8 && existingDistricts >= 64 && existingUpazilas >= catalogUpazilasCount) {
            geographyDao.migrateMouzaUpazilaId("adarsha-sadar", "cumilla-adarsha-sadar")
            geographyDao.deleteUpazilaById("adarsha-sadar")
            return
        }

        val now = System.currentTimeMillis()
        val bundle = BangladeshGeographyCatalog.buildInitialMouzaBundle(now)

        geographyDao.insertDivisions(bundle.divisions)
        geographyDao.insertDistricts(bundle.districts)
        geographyDao.insertUpazilas(bundle.upazilas)
        geographyDao.migrateMouzaUpazilaId("adarsha-sadar", "cumilla-adarsha-sadar")
        geographyDao.deleteUpazilaById("adarsha-sadar")
        geographyDao.insertMouzas(bundle.mouzas)
        geographyDao.insertMouzaAliases(bundle.aliases)
        geographyDao.insertMouzaJlNumbers(bundle.jlNumbers)
        geographyDao.insertSurveyAvailabilities(bundle.availabilities)

        geographyDao.insertDataSource(
            GeographyDataSourceEntity(
                sourceId = BangladeshGeographyCatalog.OFFICIAL_SOURCE_ID,
                sourceName = BangladeshGeographyCatalog.OFFICIAL_SOURCE_NAME,
                sourceUrl = BangladeshGeographyCatalog.OFFICIAL_SOURCE_URL,
                retrievalDate = now,
                datasetVersion = BangladeshGeographyCatalog.DATASET_VERSION,
                recordCount = bundle.divisions.size + bundle.districts.size + bundle.upazilas.size + bundle.mouzas.size
            )
        )

        geographyDao.insertImportHistory(
            GeographyImportHistoryEntity(
                sourceName = BangladeshGeographyCatalog.OFFICIAL_SOURCE_NAME,
                sourceUrl = BangladeshGeographyCatalog.OFFICIAL_SOURCE_URL,
                importedAt = now,
                datasetVersion = BangladeshGeographyCatalog.DATASET_VERSION,
                recordCount = bundle.mouzas.size,
                importErrors = "0 errors (Initial Verified Seed)"
            )
        )
    }

    suspend fun getUpazilasForDistrict(districtId: String): List<UpazilaEntity> {
        val canonicalDistrictId = BangladeshGeographyCatalog.resolveCanonicalDistrictId(districtId)
        if (canonicalDistrictId.isEmpty()) return emptyList()
        return geographyDao.getUpazilasByDistrictDirect(canonicalDistrictId)
    }

    suspend fun getMouzasForSelectedUpazilas(
        upazilaIds: Set<String>,
        verifiedOnly: Boolean = false,
        districtId: String? = null
    ): List<MouzaEntity> {
        if (upazilaIds.isEmpty()) return emptyList()
        val canonicalUpazilaIds = upazilaIds.map { BangladeshGeographyCatalog.resolveCanonicalUpazilaId(it) }
            .filter { it.isNotEmpty() }
            .distinct()
        if (canonicalUpazilaIds.isEmpty()) return emptyList()

        val raw = if (!districtId.isNullOrBlank()) {
            val canonicalDistrictId = BangladeshGeographyCatalog.resolveCanonicalDistrictId(districtId)
            geographyDao.getMouzasForUpazilasInDistrictDirect(canonicalDistrictId, canonicalUpazilaIds)
        } else {
            geographyDao.getMouzasForUpazilasDirect(canonicalUpazilaIds)
        }
        val filtered = if (verifiedOnly) {
            raw.filter { it.verificationStatus == GeoVerificationStatus.VERIFIED }
        } else {
            raw
        }
        return filtered.distinctBy { "${it.upazilaId}|${it.jlNumber}|${it.nameEn.lowercase()}" }
    }

    suspend fun getCoverageSummary(
        selectedDistrictIds: Set<String>,
        selectedUpazilaIds: Set<String>,
        selectedMouzaIds: Set<String>
    ): GeographyCoverageSummary = computeCoverageSummary(
        selectedDistrictIds = selectedDistrictIds,
        selectedUpazilaIds = selectedUpazilaIds,
        selectedMouzaIds = selectedMouzaIds
    )

    suspend fun computeCoverageSummary(
        selectedDistrictIds: Set<String>,
        selectedUpazilaIds: Set<String>,
        selectedMouzaIds: Set<String>
    ): GeographyCoverageSummary {
        val totalDivisions = geographyDao.getDivisionsCount()
        val totalDistricts = geographyDao.getDistrictsCount()
        val totalUpazilas = geographyDao.getUpazilasCount()
        val totalMouzas = geographyDao.getTotalMouzasCount()
        val verifiedMouzas = geographyDao.getVerifiedMouzasCount()
        val unverifiedMouzas = geographyDao.getUnverifiedMouzasCount()

        val canonicalDistrictId = selectedDistrictIds.firstOrNull()?.let {
            BangladeshGeographyCatalog.resolveCanonicalDistrictId(it)
        }
        val mouzasInUpazilas = if (selectedUpazilaIds.isEmpty()) {
            emptyList()
        } else {
            getMouzasForSelectedUpazilas(
                upazilaIds = selectedUpazilaIds,
                verifiedOnly = false,
                districtId = canonicalDistrictId
            )
        }

        val availabilities = if (selectedMouzaIds.isEmpty()) {
            emptyList()
        } else {
            geographyDao.getSurveyAvailabilitiesForMouzas(selectedMouzaIds.toList())
        }

        val mouzasWithAvailable = availabilities
            .filter { it.availabilityStatus == SurveyAvailabilityStatus.AVAILABLE }
            .map { it.mouzaId }
            .toSet()
            .size

        val mouzasManual = availabilities
            .filter { it.availabilityStatus == SurveyAvailabilityStatus.MANUAL_RETRIEVAL_REQUIRED }
            .map { it.mouzaId }
            .toSet()
            .size

        val unknownCount = (selectedMouzaIds.size - mouzasWithAvailable).coerceAtLeast(0)

        var isDistrictComplete = false
        var warningMessage: String? = null
        if (!canonicalDistrictId.isNullOrBlank()) {
            val districtUpazilas = geographyDao.getUpazilasByDistrictDirect(canonicalDistrictId)
            val districtMouzas = geographyDao.getMouzasByDistrictDirect(canonicalDistrictId)
            val upazilasWithMouzas = districtMouzas.map { it.upazilaId }.toSet()
            val missingUpazilaCount = (districtUpazilas.size - upazilasWithMouzas.size).coerceAtLeast(0)
            isDistrictComplete = districtUpazilas.isNotEmpty() && missingUpazilaCount == 0 && districtMouzas.size >= districtUpazilas.size * 5
            if (!isDistrictComplete) {
                warningMessage = "Incomplete district mouza dataset: ${districtMouzas.size} verified mouzas loaded across ${upazilasWithMouzas.size}/${districtUpazilas.size} upazilas. Import supplemental DLR&S JSON for full coverage."
            }
        }

        return GeographyCoverageSummary(
            totalDivisions = totalDivisions,
            totalDistricts = totalDistricts,
            totalUpazilas = totalUpazilas,
            totalMouzas = totalMouzas,
            totalVerifiedMouzas = verifiedMouzas,
            totalUnverifiedMouzas = unverifiedMouzas,
            selectedDistrictsCount = selectedDistrictIds.size,
            selectedUpazilasCount = selectedUpazilaIds.size,
            totalMouzasFoundInSelection = mouzasInUpazilas.size,
            selectedMouzasCount = selectedMouzaIds.size,
            mouzasWithAvailableSurveys = mouzasWithAvailable.coerceAtLeast(selectedMouzaIds.size),
            mouzasWithUnknownAvailability = unknownCount,
            mouzasRequiringManualRetrieval = mouzasManual,
            isCurrentDistrictComplete = isDistrictComplete,
            incompleteDataWarning = warningMessage.orEmpty()
        )
    }

    suspend fun importGeographyJsonUpdate(jsonPayload: String): GeographyImportHistoryEntity {
        val parsed = BangladeshGeographyCatalog.parseJsonGeographyUpdate(jsonPayload)
        if (parsed.mouzas.isNotEmpty()) {
            geographyDao.insertMouzas(parsed.mouzas)
        }
        val history = GeographyImportHistoryEntity(
            sourceName = parsed.sourceName,
            sourceUrl = parsed.sourceUrl,
            importedAt = System.currentTimeMillis(),
            datasetVersion = parsed.datasetVersion,
            recordCount = parsed.mouzas.size,
            importErrors = if (parsed.errors.isEmpty()) "0 errors" else parsed.errors.joinToString("; ")
        )
        geographyDao.insertImportHistory(history)
        return history
    }
}
