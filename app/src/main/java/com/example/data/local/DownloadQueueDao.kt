package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DownloadTask
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadQueueDao {
    @Query("SELECT * FROM download_queue ORDER BY batchNumber ASC, sortOrder ASC, timestamp ASC")
    fun getQueue(): Flow<List<DownloadTask>>

    @Query("SELECT * FROM download_queue WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): DownloadTask?

    @Query("SELECT * FROM download_queue WHERE mouzaId = :mouzaId LIMIT 1")
    suspend fun getTaskByMouzaId(mouzaId: String): DownloadTask?

    @Query("SELECT * FROM download_queue WHERE status = 'PENDING' ORDER BY batchNumber ASC, sortOrder ASC, timestamp ASC")
    suspend fun getPendingTasks(): List<DownloadTask>

    @Query("SELECT * FROM download_queue WHERE status = 'PENDING' ORDER BY batchNumber ASC, sortOrder ASC, timestamp ASC LIMIT 1")
    suspend fun getNextPendingTask(): DownloadTask?

    @Query("SELECT * FROM download_queue ORDER BY batchNumber ASC, sortOrder ASC, timestamp ASC")
    suspend fun getAllTasksDirect(): List<DownloadTask>

    @Query("SELECT COUNT(*) FROM download_queue WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM download_queue WHERE status = 'COMPLETED'")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM download_queue")
    suspend fun getTotalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<DownloadTask>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTask)

    @Update
    suspend fun updateTask(task: DownloadTask)

    @Query("DELETE FROM download_queue WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM download_queue WHERE batchNumber = :batchNumber")
    suspend fun deleteBatch(batchNumber: Int)

    @Query("DELETE FROM download_queue")
    suspend fun clearQueue()

    @Query("UPDATE download_queue SET status = 'PENDING' WHERE status IN ('DOWNLOADING', 'AUTHENTICATING', 'VERIFYING')")
    suspend fun resetInterruptedTasks()

    @Query("UPDATE download_queue SET status = 'CANCELLED' WHERE status IN ('PENDING', 'PAUSED', 'WAITING_FOR_NETWORK', 'WAITING_FOR_USER')")
    suspend fun cancelAllPendingTasks()

    @Query("UPDATE download_queue SET status = 'UNAVAILABLE' WHERE id = :taskId")
    suspend fun markTaskUnavailable(taskId: String)
}
