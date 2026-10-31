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
    suspend fun getDivisionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDivisions(divisions: List<DivisionEntity>)

    // --- Districts ---
    @Query("SELECT * FROM districts WHERE division_id = :divisionId ORDER BY name_en ASC")
    fun observeDistrictsByDivision(divisionId: String): Flow<List<DistrictEntity>>

    @Query("SELECT * FROM districts WHERE division_id = :divisionId ORDER BY name_en ASC")
    suspend fun getDistrictsByDivisionDirect(divisionId: String): List<DistrictEntity>

    @Query("SELECT * FROM districts ORDER BY division_id ASC, name_en ASC")
    fun observeAllDistricts(): Flow<List<DistrictEntity>>

    @Query("SELECT * FROM districts ORDER BY division_id ASC, name_en ASC")
    suspend fun getAllDistrictsDirect(): List<DistrictEntity>

    @Query("SELECT * FROM districts WHERE id = :districtId LIMIT 1")
    suspend fun getDistrictById(districtId: String): DistrictEntity?

    @Query("SELECT COUNT(*) FROM districts")
    suspend fun getDistrictCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistricts(districts: List<DistrictEntity>)

    // --- Upazilas ---
    @Query("SELECT * FROM upazilas WHERE district_id = :districtId ORDER BY name_en ASC")
    fun observeUpazilasByDistrict(districtId: String): Flow<List<UpazilaEntity>>

    @Query("SELECT * FROM upazilas WHERE district_id = :districtId ORDER BY name_en ASC")
    suspend fun getUpazilasByDistrictDirect(districtId: String): List<UpazilaEntity>

    @Query("SELECT * FROM upazilas WHERE district_id IN (:districtIds) ORDER BY district_id ASC, name_en ASC")
    suspend fun getUpazilasByDistrictsDirect(districtIds: List<String>): List<UpazilaEntity>

    @Query("SELECT * FROM upazilas ORDER BY district_id ASC, name_en ASC")
    fun observeAllUpazilas(): Flow<List<UpazilaEntity>>

    @Query("SELECT * FROM upazilas ORDER BY district_id ASC, name_en ASC")
    suspend fun getAllUpazilasDirect(): List<UpazilaEntity>

    @Query("SELECT COUNT(*) FROM upazilas")
    suspend fun getUpazilaCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpazilas(upazilas: List<UpazilaEntity>)

    // --- Mouzas ---
    @Query("SELECT * FROM mouzas WHERE upazila_id = :upazilaId ORDER BY CAST(jl_number AS INTEGER) ASC, jl_number ASC, name_en ASC")
    fun observeMouzasByUpazila(upazilaId: String): Flow<List<MouzaEntity>>

    @Query("SELECT * FROM mouzas WHERE upazila_id = :upazilaId ORDER BY CAST(jl_number AS INTEGER) ASC, jl_number ASC, name_en ASC")
    suspend fun getMouzasByUpazilaDirect(upazilaId: String): List<MouzaEntity>

    @Query("SELECT * FROM mouzas WHERE upazila_id IN (:upazilaIds) ORDER BY upazila_id ASC, CAST(jl_number AS INTEGER) ASC, jl_number ASC, name_en ASC")
    fun observeMouzasByUpazilas(upazilaIds: List<String>): Flow<List<MouzaEntity>>

    @Query("SELECT * FROM mouzas WHERE upazila_id IN (:upazilaIds) ORDER BY upazila_id ASC, CAST(jl_number AS INTEGER) ASC, jl_number ASC, name_en ASC")
    suspend fun getMouzasByUpazilasDirect(upazilaIds: List<String>): List<MouzaEntity>

    @Query("""
        SELECT m.* FROM mouzas m
        INNER JOIN upazilas u ON m.upazila_id = u.id
        WHERE u.district_id = :districtId
        ORDER BY u.name_en ASC, CAST(m.jl_number AS INTEGER) ASC, m.name_en ASC
    """)
    suspend fun getMouzasByDistrictDirect(districtId: String): List<MouzaEntity>

    @Query("""
        SELECT m.* FROM mouzas m
        INNER JOIN upazilas u ON m.upazila_id = u.id
        INNER JOIN districts d ON u.district_id = d.id
        WHERE d.division_id = :divisionId
        ORDER BY d.name_en ASC, u.name_en ASC, CAST(m.jl_number AS INTEGER) ASC, m.name_en ASC
    """)
    suspend fun getMouzasByDivisionDirect(divisionId: String): List<MouzaEntity>

    @Query("SELECT * FROM mouzas WHERE verification_status = 'VERIFIED' ORDER BY upazila_id ASC, jl_number ASC")
    suspend fun getAllVerifiedMouzasDirect(): List<MouzaEntity>

    @Query("SELECT * FROM mouzas ORDER BY upazila_id ASC, jl_number ASC")
    suspend fun getAllMouzasDirect(): List<MouzaEntity>

    @Query("SELECT COUNT(*) FROM mouzas WHERE verification_status = 'VERIFIED'")
    fun observeVerifiedMouzaCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM mouzas WHERE verification_status = 'VERIFIED'")
    suspend fun getVerifiedMouzaCountDirect(): Int

    @Query("SELECT COUNT(*) FROM mouzas WHERE verification_status != 'VERIFIED'")
    suspend fun getUnverifiedMouzaCountDirect(): Int

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
        ORDER BY m.verification_status ASC, m.name_en ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchMouzasPaged(
        query: String,
        upazilaId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<MouzaEntity>

    @Query("""
        SELECT * FROM mouzas
        WHERE upazila_id = :upazilaId
          AND (jl_number = :jlNumber OR LOWER(name_en) = LOWER(:nameEn) OR name_bn = :nameBn)
    """)
    suspend fun findPotentialDuplicates(
        upazilaId: String,
        jlNumber: String,
        nameEn: String,
        nameBn: String
    ): List<MouzaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMouzas(mouzas: List<MouzaEntity>)

    // --- Aliases & JL Numbers ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMouzaAliases(aliases: List<MouzaAliasEntity>)

    @Query("SELECT * FROM mouza_aliases WHERE mouza_id = :mouzaId")
    suspend fun getAliasesForMouza(mouzaId: String): List<MouzaAliasEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMouzaJlNumbers(jlNumbers: List<MouzaJlNumberEntity>)

    @Query("SELECT * FROM mouza_jl_numbers WHERE mouza_id = :mouzaId")
    suspend fun getJlNumbersForMouza(mouzaId: String): List<MouzaJlNumberEntity>

    // --- Survey Availability ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurveyAvailabilities(items: List<MouzaSurveyAvailabilityEntity>)

    @Query("SELECT * FROM mouza_survey_availability WHERE mouza_id IN (:mouzaIds)")
    suspend fun getSurveyAvailabilitiesForMouzas(mouzaIds: List<String>): List<MouzaSurveyAvailabilityEntity>

    @Query("SELECT * FROM mouza_survey_availability WHERE mouza_id = :mouzaId")
    suspend fun getSurveyAvailabilityForMouza(mouzaId: String): List<MouzaSurveyAvailabilityEntity>

    // --- Data Sources & Import History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataSource(source: GeographyDataSourceEntity)

    @Query("SELECT * FROM geography_data_sources ORDER BY imported_at DESC")
    fun observeDataSources(): Flow<List<GeographyDataSourceEntity>>

    @Query("SELECT * FROM geography_data_sources ORDER BY imported_at DESC")
    suspend fun getDataSourcesDirect(): List<GeographyDataSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportHistory(history: GeographyImportHistoryEntity)

    @Query("SELECT * FROM geography_import_history ORDER BY import_date DESC")
    fun observeImportHistory(): Flow<List<GeographyImportHistoryEntity>>

    @Query("SELECT * FROM geography_import_history ORDER BY import_date DESC")
    suspend fun getImportHistoryDirect(): List<GeographyImportHistoryEntity>
}
