package com.example.data.repository

import com.example.data.local.DownloadQueueDao
import com.example.data.local.SavedRecordDao
import com.example.data.model.DownloadTask
import com.example.data.model.SavedRecord
import kotlinx.coroutines.flow.Flow

class DownloadRepository(
    private val savedRecordDao: SavedRecordDao,
    private val downloadQueueDao: DownloadQueueDao
) {
    val savedRecords: Flow<List<SavedRecord>> = savedRecordDao.getAllSavedRecords()
    val downloadQueue: Flow<List<DownloadTask>> = downloadQueueDao.getQueue()

    suspend fun insertSavedRecord(record: SavedRecord) = savedRecordDao.insertRecord(record)
    suspend fun deleteSavedRecord(id: String) = savedRecordDao.deleteRecordById(id)
    suspend fun clearSavedRecords() = savedRecordDao.clearAllRecords()

    suspend fun getTaskById(taskId: String) = downloadQueueDao.getTaskById(taskId)
    suspend fun getPendingTasks() = downloadQueueDao.getPendingTasks()
    suspend fun getNextPendingTask() = downloadQueueDao.getNextPendingTask()
    suspend fun getAllTasksDirect() = downloadQueueDao.getAllTasksDirect()
    suspend fun getPendingCount() = downloadQueueDao.getPendingCount()
    suspend fun getCompletedCount() = downloadQueueDao.getCompletedCount()
    suspend fun getTotalCount() = downloadQueueDao.getTotalCount()
    suspend fun insertTasksToQueue(tasks: List<DownloadTask>) = downloadQueueDao.insertTasks(tasks)
    suspend fun insertTaskToQueue(task: DownloadTask) = downloadQueueDao.insertTask(task)
    suspend fun updateTaskInQueue(task: DownloadTask) = downloadQueueDao.updateTask(task)
    suspend fun deleteTaskFromQueue(taskId: String) = downloadQueueDao.deleteTaskById(taskId)
    suspend fun clearQueue() = downloadQueueDao.clearQueue()
    suspend fun resetInterruptedTasks() = downloadQueueDao.resetInterruptedTasks()
}
