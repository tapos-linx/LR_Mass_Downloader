package com.example.data.ledger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterLandLedgerDao {

    @Query("SELECT * FROM master_land_ledger ORDER BY recorded_at DESC")
    fun observeAllEntries(): Flow<List<MasterLedgerEntryEntity>>

    @Query("SELECT * FROM master_land_ledger ORDER BY recorded_at DESC")
    suspend fun getAllEntriesDirect(): List<MasterLedgerEntryEntity>

    @Query("SELECT COUNT(*) FROM master_land_ledger")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM master_land_ledger")
    suspend fun getTotalCountDirect(): Int

    @Query("SELECT * FROM master_land_ledger WHERE sha256_checksum = :sha256 AND sha256_checksum != '' LIMIT 1")
    suspend fun findBySha256(sha256: String): MasterLedgerEntryEntity?

    @Query("""
        SELECT * FROM master_land_ledger
        WHERE (:surveyType IS NULL OR survey_type = :surveyType)
          AND (
              mouza_name LIKE '%' || :query || '%'
              OR jl_number LIKE '%' || :query || '%'
              OR khatian_number LIKE '%' || :query || '%'
              OR dag_number LIKE '%' || :query || '%'
              OR owner_name LIKE '%' || :query || '%'
              OR guardian_name LIKE '%' || :query || '%'
              OR upazila_name LIKE '%' || :query || '%'
              OR district_name LIKE '%' || :query || '%'
          )
        ORDER BY recorded_at DESC
    """)
    suspend fun searchLedger(query: String, surveyType: String? = null): List<MasterLedgerEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: MasterLedgerEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<MasterLedgerEntryEntity>)

    @Update
    suspend fun updateEntry(entry: MasterLedgerEntryEntity)

    @Query("DELETE FROM master_land_ledger WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM master_land_ledger")
    suspend fun clearAll()
}
