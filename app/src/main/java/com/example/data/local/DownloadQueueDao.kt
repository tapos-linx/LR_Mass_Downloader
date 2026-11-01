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

    @Query("SELECT * FROM download_queue ORDER BY batchNumber ASC, sortOrder ASC, timestamp ASC")
    fun getAllQueueTasks(): Flow<List<DownloadTask>>

    @Query("SELECT * FROM download_queue ORDER BY batchNumber ASC, sortOrder ASC, timestamp ASC")
    suspend fun getAllTasksDirect(): List<DownloadTask>

    @Query("SELECT * FROM download_queue ORDER BY batchNumber ASC, sortOrder ASC, timestamp ASC")
    suspend fun getAllQueueTasksDirect(): List<DownloadTask>

    @Query("SELECT * FROM download_queue WHERE districtId = :districtId ORDER BY batchNumber ASC, sortOrder ASC, timestamp ASC")
    suspend fun getQueueTasksByDistrictDirect(districtId: String): List<DownloadTask>

    @Query("SELECT * FROM download_queue WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): DownloadTask?

    @Query("SELECT * FROM download_queue WHERE mouzaId = :mouzaId LIMIT 1")
    suspend fun getTaskByMouzaId(mouzaId: String): DownloadTask?

    @Query("SELECT * FROM download_queue WHERE status = 'PENDING' OR status = 'QUEUED' ORDER BY batchNumber ASC, sortOrder ASC, timestamp ASC")
    suspend fun getPendingTasks(): List<DownloadTask>

    @Query("SELECT * FROM download_queue WHERE status = 'PENDING' OR status = 'QUEUED' ORDER BY batchNumber ASC, sortOrder ASC, timestamp ASC LIMIT 1")
    suspend fun getNextPendingTask(): DownloadTask?

    @Query("SELECT COUNT(*) FROM download_queue WHERE status = 'PENDING' OR status = 'QUEUED'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM download_queue WHERE status = 'COMPLETED'")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM download_queue")
    suspend fun getTotalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<DownloadTask>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueTasks(tasks: List<DownloadTask>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTask)

    @Update
    suspend fun updateTask(task: DownloadTask)

    @Update
    suspend fun updateQueueTask(task: DownloadTask)

    @Query("DELETE FROM download_queue WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM download_queue WHERE id = :taskId")
    suspend fun deleteQueueTaskById(taskId: String)

    @Query("DELETE FROM download_queue WHERE batchNumber = :batchNumber AND status != 'COMPLETED'")
    suspend fun deleteBatch(batchNumber: Int)

    @Query("DELETE FROM download_queue WHERE batchNumber = :batchNumber AND status != 'COMPLETED'")
    suspend fun deleteBatchTasks(batchNumber: Int)

    @Query("DELETE FROM download_queue")
    suspend fun clearQueue()

    /**
     * Isolates/removes non-completed tasks from other districts when switching districts,
     * while preserving completed tasks and historical archives.
     */
    @Query("DELETE FROM download_queue WHERE districtId != :activeDistrictId AND status != 'COMPLETED'")
    suspend fun deleteNonCompletedTasksOutsideDistrict(activeDistrictId: String)

    @Query("DELETE FROM download_queue WHERE districtId != :activeDistrictId")
    suspend fun deleteTasksOutsideDistrict(activeDistrictId: String)

    @Query("UPDATE download_queue SET status = 'PENDING', speedKbps = 0 WHERE status = 'DOWNLOADING' OR status = 'VALIDATING'")
    suspend fun resetInterruptedTasks()

    @Query("UPDATE download_queue SET status = 'PENDING', speedKbps = 0 WHERE status = 'DOWNLOADING' OR status = 'VALIDATING'")
    suspend fun resetInterruptedDownloads()

    @Query("UPDATE download_queue SET status = 'CANCELLED', speedKbps = 0 WHERE status = 'PENDING' OR status = 'QUEUED' OR status = 'DOWNLOADING' OR status = 'WAITING_FOR_NETWORK' OR status = 'WAITING_FOR_USER' OR status = 'AWAITING_USER_IMPORT'")
    suspend fun cancelAllPendingTasks()

    @Query("UPDATE download_queue SET status = 'CANCELLED', speedKbps = 0 WHERE status = 'PENDING' OR status = 'QUEUED' OR status = 'DOWNLOADING' OR status = 'WAITING_FOR_NETWORK' OR status = 'WAITING_FOR_USER' OR status = 'AWAITING_USER_IMPORT'")
    suspend fun cancelAllActiveAndPendingTasks()

    @Query("UPDATE download_queue SET status = 'UNAVAILABLE', speedKbps = 0, errorDetails = 'Marked unavailable / skipped' WHERE id = :taskId")
    suspend fun markTaskUnavailable(taskId: String)
}
