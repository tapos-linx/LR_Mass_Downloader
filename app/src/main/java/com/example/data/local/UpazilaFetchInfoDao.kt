package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UpazilaFetchInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface UpazilaFetchInfoDao {
    @Query("SELECT * FROM upazila_fetch_info LIMIT 1")
    fun getLatestFetchInfo(): Flow<UpazilaFetchInfo?>

    @Query("SELECT * FROM upazila_fetch_info LIMIT 1")
    suspend fun getLatestFetchInfoDirect(): UpazilaFetchInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFetchInfo(info: UpazilaFetchInfo)

    @Query("DELETE FROM upazila_fetch_info")
    suspend fun clearFetchInfo()
}
