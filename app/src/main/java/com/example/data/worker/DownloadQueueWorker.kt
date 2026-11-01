package com.example.data.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.data.geography.BangladeshGeographyCatalog
import com.example.data.geography.GeographyRepository
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadTask
import com.example.data.model.RecordType
import com.example.data.model.RetrievalMode
import com.example.data.model.TaskStatus
import com.example.ui.LRDownloaderViewModel
import com.example.util.NetworkDeviceQueueAdvisor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * WorkManager CoroutineWorker that executes the 7-Mouza batch download queue
 * using Room-backed geographic queries and honest task states.
 *
 * Strictly enforces:
 * - `LRDownloaderViewModel.DEFAULT_MOUZAS_PER_BATCH` (7 Mouzas per batch).
 * - Room-based geographic continuation by canonical `districtId` and `upazilaId` (never name matching).
 * - Zero artificial download speeds, zero fake progress percentages, and zero simulated completions.
 */
class DownloadQueueWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database = AppDatabase.getDatabase(appContext)
    private val queueDao = database.downloadQueueDao()
    private val geographyDao = database.geographyDao()
    private val geographyRepository = GeographyRepository(geographyDao)

    companion object {
        const val CHANNEL_ID = "lr_download_queue_channel"
        const val NOTIFICATION_ID = 7001
        const val DEFAULT_MOUZAS_PER_BATCH = LRDownloaderViewModel.DEFAULT_MOUZAS_PER_BATCH
    }

    override suspend fun doWork(): Result {
        try {
            setForeground(createForegroundInfo("Initializing Land Record Queue...", 0, 0))
        } catch (_: Exception) {
            // Foreground promotion may be restricted in unit test environments
        }

        geographyRepository.ensureSeeded()
        val autoBatchMode = inputData.getBoolean(DownloadWorkManagerHelper.KEY_AUTO_BATCH_MODE, true)

        return try {
            while (!isStopped) {
                val allTasks = queueDao.getAllTasksDirect()
                if (allTasks.isEmpty()) break

                val incompleteTasks = allTasks.filter {
                    it.status == TaskStatus.QUEUED ||
                        it.status == TaskStatus.PENDING ||
                        it.status == TaskStatus.DOWNLOADING ||
                        it.status == TaskStatus.WAITING_FOR_USER
                }

                if (incompleteTasks.isEmpty()) {
                    if (autoBatchMode) {
                        val nextBatchEnqueued = enqueueNextBatchFromRoomIfRemaining(allTasks)
                        if (nextBatchEnqueued) {
                            delay(100)
                            continue
                        }
                    }
                    break
                }

                val activeBatchNumber = incompleteTasks.minOf { it.batchNumber }
                val batchTasks = incompleteTasks.filter { it.batchNumber == activeBatchNumber }

                val profile = NetworkDeviceQueueAdvisor.analyze(applicationContext)
                val concurrency = profile.maxConcurrentDownloads.coerceIn(1, 3)
                val semaphore = Semaphore(concurrency)

                coroutineScope {
                    batchTasks.forEach { task ->
                        if (!isStopped) {
                            semaphore.withPermit {
                                processSingleTask(task)
                            }
                        }
                    }
                }

                // If remaining tasks in this batch are all awaiting user action or failed, stop worker loop cleanly
                val refreshedTasks = queueDao.getAllTasksDirect()
                val stillRunnable = refreshedTasks.any {
                    it.status == TaskStatus.QUEUED ||
                        it.status == TaskStatus.PENDING ||
                        it.status == TaskStatus.DOWNLOADING
                }
                if (!stillRunnable) {
                    break
                }
            }

            val finalTasks = queueDao.getAllTasksDirect()
            val completedCount = finalTasks.count { it.status == TaskStatus.COMPLETED }
            updateWorkerProgress(
                activeTaskName = "Queue Standby",
                itemProgress = if (completedCount == finalTasks.size && finalTasks.isNotEmpty()) 100 else 0,
                completedCount = completedCount,
                totalCount = finalTasks.size,
                speedKbps = 0,
                statusMessage = "Completed $completedCount of ${finalTasks.size} tasks"
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * Enqueues the next 7-Mouza batch using authoritative Room geographic queries by canonical
     * `districtId` and `upazilaId` (never by Bengali/English name matching).
     */
    private suspend fun enqueueNextBatchFromRoomIfRemaining(allTasks: List<DownloadTask>): Boolean {
        val sampleTask = allTasks.lastOrNull() ?: return false
        if (sampleTask.retrievalMode != RetrievalMode.MODE_A_AUTHORIZED_API) return false

        val canonicalDistrictId = BangladeshGeographyCatalog.resolveCanonicalDistrictId(sampleTask.districtId)
        val canonicalUpazilaId = BangladeshGeographyCatalog.resolveCanonicalUpazilaId(sampleTask.upazilaId)
        if (canonicalDistrictId.isEmpty() || canonicalUpazilaId.isEmpty()) return false

        val districtEntity = geographyDao.getDistrictById(canonicalDistrictId) ?: return false
        val upazilaEntity = geographyDao.getUpazilaByIdAndDistrict(canonicalUpazilaId, canonicalDistrictId) ?: return false
        val mouzaEntities = geographyDao.getMouzasForUpazilasInDistrictDirect(
            districtId = canonicalDistrictId,
            upazilaIds = listOf(canonicalUpazilaId)
        )

        val existingMouzaIds = allTasks.map { it.mouzaId }.toSet()
        val remainingMouzas = mouzaEntities
            .filter { it.id !in existingMouzaIds }
            .distinctBy { it.id }

        if (remainingMouzas.isEmpty()) return false

        val nextBatchMouzas = remainingMouzas.take(DEFAULT_MOUZAS_PER_BATCH)
        val nextBatchNumber = (allTasks.maxOfOrNull { it.batchNumber } ?: 0) + 1
        val now = System.currentTimeMillis()
        val recordTypes = sampleTask.recordTypes.ifEmpty { RecordType.values().toSet() }
        var nextSortOrder = (allTasks.maxOfOrNull { it.sortOrder } ?: 0) + 1

        val newTasks = nextBatchMouzas.mapIndexed { idx, mouzaEntity ->
            DownloadTask(
                id = "${mouzaEntity.id}-b$nextBatchNumber-$now-$idx",
                mouzaId = mouzaEntity.id,
                mouzaNameBn = mouzaEntity.nameBn,
                mouzaNameEn = mouzaEntity.nameEn,
                jlNo = mouzaEntity.jlNumber,
                upazilaNameBn = upazilaEntity.nameBn,
                districtNameBn = districtEntity.nameBn,
                recordTypes = recordTypes,
                downloadFormat = sampleTask.downloadFormat,
                status = TaskStatus.PENDING,
                progress = 0,
                totalKhatians = 50 * recordTypes.size,
                downloadedKhatians = 0,
                fileSizeBytes = 0L,
                speedKbps = 0,
                batchNumber = nextBatchNumber,
                timestamp = now + idx,
                divisionId = districtEntity.divisionId,
                divisionNameBn = sampleTask.divisionNameBn,
                districtId = districtEntity.id,
                upazilaId = upazilaEntity.id,
                surveyType = recordTypes.firstOrNull()?.code ?: "RS",
                recordCategory = sampleTask.recordCategory,
                retrievalMode = sampleTask.retrievalMode,
                sourceName = "eporcha.tech Free Land Records Portal",
                sourceReference = "https://eporcha.tech/",
                sortOrder = nextSortOrder++
            )
        }

        queueDao.insertTasks(newTasks)
        return true
    }

    private suspend fun processSingleTask(task: DownloadTask) {
        if (isStopped) return
        val current = queueDao.getTaskById(task.id) ?: return
        if (current.status == TaskStatus.COMPLETED ||
            current.status == TaskStatus.CANCELLED ||
            current.status == TaskStatus.UNAVAILABLE ||
            current.status == TaskStatus.AWAITING_USER_IMPORT ||
            current.status == TaskStatus.FAILED
        ) {
            return
        }

        queueDao.updateTask(
            current.copy(
                status = TaskStatus.AWAITING_USER_IMPORT,
                progress = 0,
                speedKbps = 0,
                sourceName = "eporcha.tech Free Land Records Portal",
                sourceReference = "https://eporcha.tech/",
                errorDetails = null
            )
        )
    }

    private suspend fun updateWorkerProgress(
        activeTaskName: String,
        itemProgress: Int,
        completedCount: Int,
        totalCount: Int,
        speedKbps: Int,
        statusMessage: String
    ) {
        val overallPercent = if (totalCount == 0) 0 else ((completedCount * 100) / totalCount).coerceIn(0, 100)
        val progressData = Data.Builder()
            .putInt(DownloadWorkManagerHelper.KEY_PROGRESS_PERCENT, overallPercent)
            .putInt(DownloadWorkManagerHelper.KEY_ITEM_PROGRESS, itemProgress)
            .putString(DownloadWorkManagerHelper.KEY_ACTIVE_TASK_NAME, activeTaskName)
            .putInt(DownloadWorkManagerHelper.KEY_COMPLETED_TASKS, completedCount)
            .putInt(DownloadWorkManagerHelper.KEY_TOTAL_TASKS, totalCount)
            .putInt(DownloadWorkManagerHelper.KEY_SPEED_KBPS, speedKbps)
            .putString(DownloadWorkManagerHelper.KEY_STATUS_MESSAGE, statusMessage)
            .build()
        try {
            setProgress(progressData)
            setForeground(createForegroundInfo(statusMessage, completedCount, totalCount))
        } catch (_: Exception) {
        }
    }

    private fun createForegroundInfo(statusText: String, completed: Int, total: Int): ForegroundInfo {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Land Record Download Queue",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("LRgetBD Queue Engine")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(total.coerceAtLeast(1), completed.coerceAtLeast(0), total == 0)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
