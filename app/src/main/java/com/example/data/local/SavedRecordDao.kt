package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SavedRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRecordDao {
    @Query("SELECT * FROM saved_land_records ORDER BY downloadedAt DESC")
    fun getAllSavedRecords(): Flow<List<SavedRecord>>

    @Query("SELECT * FROM saved_land_records ORDER BY downloadedAt DESC")
    suspend fun getAllSavedRecordsDirect(): List<SavedRecord>

    @Query("SELECT * FROM saved_land_records WHERE sha256Checksum = :sha256 AND sha256Checksum != '' LIMIT 1")
    suspend fun findBySha256(sha256: String): SavedRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecord(record: SavedRecord): Long

    @Query("DELETE FROM saved_land_records WHERE id = :id")
    suspend fun deleteRecordById(id: String)

    @Query("DELETE FROM saved_land_records")
    suspend fun clearAllRecords()
}
