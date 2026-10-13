package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.KhatianRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface KhatianRecordDao {
    @Query("SELECT * FROM khatian_records ORDER BY timestamp DESC")
    fun getAllKhatianRecords(): Flow<List<KhatianRecord>>

    @Query("SELECT * FROM khatian_records WHERE district = :district AND khatianNumber = :khatianNumber")
    suspend fun getKhatianRecord(district: String, khatianNumber: String): KhatianRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhatianRecord(record: KhatianRecord)

    @Update
    suspend fun updateKhatianRecord(record: KhatianRecord)

    @Query("DELETE FROM khatian_records WHERE id = :id")
    suspend fun deleteKhatianRecordById(id: Long)

    @Query("DELETE FROM khatian_records")
    suspend fun clearAllKhatianRecords()
}
