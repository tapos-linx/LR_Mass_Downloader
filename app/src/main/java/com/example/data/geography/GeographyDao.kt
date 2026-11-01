package com.example.data.geography

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GeographyDao {

    // --- Divisions ---
    @Query("SELECT * FROM divisions ORDER BY official_code ASC")
    fun observeAllDivisions(): Flow<List<DivisionEntity>>

    @Query("SELECT * FROM divisions ORDER BY official_code ASC")
    suspend fun getAllDivisionsDirect(): List<DivisionEntity>

    @Query("SELECT COUNT(*) FROM divisions")
    suspend fun getDivisionsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDivisions(divisions: List<DivisionEntity>)

    // --- Districts ---
    @Query("SELECT * FROM districts ORDER BY name_en ASC")
    fun observeAllDistricts(): Flow<List<DistrictEntity>>

    @Query("SELECT * FROM districts ORDER BY name_en ASC")
    suspend fun getAllDistrictsDirect(): List<DistrictEntity>

    @Query("SELECT * FROM districts WHERE division_id = :divisionId ORDER BY name_en ASC")
    fun observeDistrictsByDivision(divisionId: String): Flow<List<DistrictEntity>>

    @Query("SELECT * FROM districts WHERE division_id = :divisionId ORDER BY name_en ASC")
    suspend fun getDistrictsByDivisionDirect(divisionId: String): List<DistrictEntity>

    @Query("SELECT * FROM districts WHERE id = :districtId LIMIT 1")
    suspend fun getDistrictById(districtId: String): DistrictEntity?

    @Query("SELECT COUNT(*) FROM districts")
    suspend fun getDistrictsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistricts(districts: List<DistrictEntity>)

    // --- Upazilas ---
    @Query("SELECT * FROM upazilas ORDER BY name_en ASC")
    fun observeAllUpazilas(): Flow<List<UpazilaEntity>>

    @Query("SELECT * FROM upazilas ORDER BY name_en ASC")
    suspend fun getAllUpazilasDirect(): List<UpazilaEntity>

    @Query("SELECT * FROM upazilas WHERE district_id = :districtId ORDER BY name_en ASC")
    fun observeUpazilasByDistrict(districtId: String): Flow<List<UpazilaEntity>>

    @Query("SELECT * FROM upazilas WHERE district_id = :districtId ORDER BY name_en ASC")
    suspend fun getUpazilasByDistrictDirect(districtId: String): List<UpazilaEntity>

    @Query("SELECT * FROM upazilas WHERE id = :upazilaId LIMIT 1")
    suspend fun getUpazilaById(upazilaId: String): UpazilaEntity?

    @Query("SELECT * FROM upazilas WHERE id = :upazilaId AND district_id = :districtId LIMIT 1")
    suspend fun getUpazilaByIdAndDistrict(upazilaId: String, districtId: String): UpazilaEntity?

    @Query("SELECT COUNT(*) FROM upazilas")
    suspend fun getUpazilasCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpazilas(upazilas: List<UpazilaEntity>)

    @Query("DELETE FROM upazilas WHERE id = :upazilaId")
    suspend fun deleteUpazilaById(upazilaId: String)

    // --- Mouzas ---
    @Query("SELECT * FROM mouzas ORDER BY upazila_id ASC, jl_number ASC")
    fun observeAllMouzas(): Flow<List<MouzaEntity>>

    @Query("SELECT * FROM mouzas ORDER BY upazila_id ASC, jl_number ASC")
    suspend fun getAllMouzasDirect(): List<MouzaEntity>

    @Query("SELECT * FROM mouzas WHERE upazila_id = :upazilaId ORDER BY jl_number ASC")
    fun observeMouzasByUpazila(upazilaId: String): Flow<List<MouzaEntity>>

    @Query("SELECT * FROM mouzas WHERE upazila_id = :upazilaId ORDER BY jl_number ASC")
    suspend fun getMouzasByUpazilaDirect(upazilaId: String): List<MouzaEntity>

    @Query("SELECT * FROM mouzas WHERE upazila_id IN (:upazilaIds) ORDER BY upazila_id ASC, jl_number ASC")
    suspend fun getMouzasForUpazilasDirect(upazilaIds: List<String>): List<MouzaEntity>

    /**
     * Strict parent-child district-verified Mouza query:
     * Returns only Mouzas whose parent Upazila is in `upazilaIds` AND belongs to `districtId`.
     */
    @Query("""
        SELECT m.* FROM mouzas m
        INNER JOIN upazilas u ON m.upazila_id = u.id
        WHERE u.district_id = :districtId
          AND m.upazila_id IN (:upazilaIds)
        ORDER BY m.upazila_id ASC, m.jl_number ASC
    """)
    suspend fun getMouzasForUpazilasInDistrictDirect(
        districtId: String,
        upazilaIds: List<String>
    ): List<MouzaEntity>

    @Query("""
        SELECT m.* FROM mouzas m
        INNER JOIN upazilas u ON m.upazila_id = u.id
        WHERE u.district_id = :districtId
        ORDER BY m.upazila_id ASC, m.jl_number ASC
    """)
    suspend fun getMouzasByDistrictDirect(districtId: String): List<MouzaEntity>

    @Query("""
        SELECT DISTINCT m.* FROM mouzas m
        LEFT JOIN mouza_aliases a ON m.id = a.mouza_id
        WHERE (:upazilaId IS NULL OR m.upazila_id = :upazilaId)
          AND (
              m.name_en LIKE '%' || :query || '%'
              OR m.name_bn LIKE '%' || :query || '%'
              OR m.jl_number LIKE '%' || :query || '%'
              OR a.alias_name LIKE '%' || :query || '%'
          )
        ORDER BY m.jl_number ASC
    """)
    suspend fun searchMouzas(query: String, upazilaId: String? = null): List<MouzaEntity>

    @Query("SELECT COUNT(*) FROM mouzas")
    suspend fun getTotalMouzasCount(): Int

    @Query("SELECT COUNT(*) FROM mouzas WHERE verification_status = 'VERIFIED'")
    suspend fun getVerifiedMouzasCount(): Int

    @Query("SELECT COUNT(*) FROM mouzas WHERE verification_status != 'VERIFIED'")
    suspend fun getUnverifiedMouzasCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMouzas(mouzas: List<MouzaEntity>)

    @Query("DELETE FROM mouzas WHERE id LIKE :idPrefix || '%'")
    suspend fun deleteMouzasByPrefix(idPrefix: String)

    @Query("UPDATE mouzas SET upazila_id = :canonicalUpazilaId WHERE upazila_id = :legacyUpazilaId")
    suspend fun migrateMouzaUpazilaId(legacyUpazilaId: String, canonicalUpazilaId: String)

    // --- Aliases, JL Numbers & Survey Availability ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMouzaAliases(aliases: List<MouzaAliasEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMouzaJlNumbers(jlNumbers: List<MouzaJlNumberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurveyAvailabilities(availabilities: List<MouzaSurveyAvailabilityEntity>)

    @Query("SELECT * FROM mouza_survey_availability WHERE mouza_id IN (:mouzaIds)")
    suspend fun getSurveyAvailabilitiesForMouzas(mouzaIds: List<String>): List<MouzaSurveyAvailabilityEntity>

    // --- Provenance & Import History ---
    @Query("SELECT * FROM geography_data_sources ORDER BY retrieval_date DESC")
    fun observeDataSources(): Flow<List<GeographyDataSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataSource(source: GeographyDataSourceEntity)

    @Query("SELECT * FROM geography_import_history ORDER BY imported_at DESC")
    fun observeImportHistory(): Flow<List<GeographyImportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportHistory(history: GeographyImportHistoryEntity)
}
