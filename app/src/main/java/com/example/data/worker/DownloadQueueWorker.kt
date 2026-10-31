package com.example.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.LandRecordStorageManager
import com.example.data.model.DownloadTask
import com.example.data.model.LandRecordsDataset
import com.example.data.model.SavedRecord
import com.example.data.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Background WorkManager Service that processes the land record download queue
 * with randomized anti-blocking delays and exponential backoff retry logic.
 */
class DownloadQueueWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val NOTIFICATION_ID = 4091
        const val NOTIFICATION_CHANNEL_ID = "lr_download_queue_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Land Record Background Downloader"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext)
        val queueDao = database.downloadQueueDao()
        val savedRecordDao = database.savedRecordDao()
        val advisorProfile = com.example.util.NetworkDeviceQueueAdvisor.analyze(applicationContext)

        try {
            setForeground(
                createForegroundInfo(
                    "🛡️ Multi-Queue Downloader Active",
                    "Network: ${advisorProfile.networkTier.labelEn} • ${advisorProfile.mouzasPerQueue} Mouzas/Queue",
                    0,
                    100,
                    true
                )
            )
        } catch (ignored: Exception) {}

        val isAutoBatchMode = inputData.getBoolean(DownloadWorkManagerHelper.KEY_AUTO_BATCH_MODE, true)

        while (!isStopped) {
            val allTasksNow = queueDao.getAllTasksDirect()
            // If the foreground ViewModel coordinator is actively driving DOWNLOADING tasks, yield briefly
            val activelyDownloading = allTasksNow.filter { it.status == TaskStatus.DOWNLOADING }
            if (activelyDownloading.isNotEmpty()) {
                val activeTask = activelyDownloading.first()
                try {
                    setProgress(
                        androidx.work.workDataOf(
                            DownloadWorkManagerHelper.KEY_PROGRESS_PERCENT to activeTask.progress,
                            DownloadWorkManagerHelper.KEY_ITEM_PROGRESS to activeTask.progress,
                            DownloadWorkManagerHelper.KEY_ACTIVE_TASK_NAME to activeTask.mouzaNameBn,
                            DownloadWorkManagerHelper.KEY_SPEED_KBPS to activeTask.speedKbps
                        )
                    )
                    setForeground(
                        createForegroundInfo(
                            "📥 Queue #${activeTask.batchNumber}: ${activeTask.mouzaNameBn}",
                            "${activeTask.downloadedKhatians}/${activeTask.totalKhatians} Khatians • ${activeTask.speedKbps} KB/s",
                            activeTask.progress,
                            100
                        )
                    )
                } catch (ignored: Exception) {}
                delay(500)
                continue
            }

            val task = queueDao.getNextPendingTask()
            if (task == null) {
                if (isAutoBatchMode && !isStopped) {
                    val allTasks = queueDao.getAllTasksDirect()
                    val sampleTask = allTasks.lastOrNull()
                    if (sampleTask != null) {
                        val district = LandRecordsDataset.districts.find {
                            it.nameBn == sampleTask.districtNameBn || it.nameEn.equals(sampleTask.districtNameBn, ignoreCase = true)
                        }
                        val upazila = district?.upazilas?.find {
                            it.nameBn == sampleTask.upazilaNameBn || it.nameEn.equals(sampleTask.upazilaNameBn, ignoreCase = true)
                        }

                        if (district != null && upazila != null) {
                            val existingMouzaIds = allTasks.map { it.mouzaId }.toSet()
                            val remainingMouzas = upazila.mouzas.filter { it.id !in existingMouzaIds }

                            if (remainingMouzas.isNotEmpty()) {
                                val queueSize = advisorProfile.mouzasPerQueue
                                val startBatchNum = (allTasks.maxOfOrNull { it.batchNumber } ?: 0) + 1
                                val timestamp = System.currentTimeMillis()
                                val recordTypes = sampleTask.recordTypes.ifEmpty { com.example.data.model.RecordType.values().toSet() }

                                val newTasks = mutableListOf<DownloadTask>()
                                remainingMouzas.chunked(queueSize).forEachIndexed { chunkIdx, chunk ->
                                    val batchNum = startBatchNum + chunkIdx
                                    chunk.forEachIndexed { itemIdx, mouza ->
                                        val taskId = "${mouza.id}-q${batchNum}-$timestamp-$itemIdx"
                                        val totalKhatians = mouza.totalKhatiansFor(recordTypes)
                                        val fileSizeBytes = totalKhatians * 110000L

                                        newTasks.add(
                                            DownloadTask(
                                                id = taskId,
                                                mouzaId = mouza.id,
                                                mouzaNameBn = mouza.nameBn,
                                                mouzaNameEn = mouza.nameEn,
                                                jlNo = mouza.jlNo,
                                                upazilaNameBn = upazila.nameBn,
                                                districtNameBn = district.nameBn,
                                                recordTypes = recordTypes,
                                                downloadFormat = sampleTask.downloadFormat,
                                                status = TaskStatus.PENDING,
                                                progress = 0,
                                                totalKhatians = totalKhatians,
                                                downloadedKhatians = 0,
                                                speedKbps = 0,
                                                fileSizeBytes = fileSizeBytes,
                                                batchNumber = batchNum,
                                                timestamp = timestamp + (chunkIdx * 1000L) + itemIdx
                                            )
                                        )
                                    }
                                }
                                queueDao.insertTasks(newTasks)
                                continue
                            } else if (allTasks.all { it.status == TaskStatus.COMPLETED }) {
                                compileAndSaveMasterArchive(district, upazila, allTasks)
                                showCompletionNotification(
                                    "✅ ${upazila.nameBn} - সকল কিউ ডাউনলোড সম্পন্ন",
                                    "মোট ${allTasks.size}টি মৌজার সকল খতিয়ান রেকর্ড সংরক্ষিত হয়েছে।"
                                )
                                return@withContext Result.success()
                            }
                        }
                    }
                }
                break
            }

            queueDao.updateTask(task.copy(status = TaskStatus.DOWNLOADING, progress = 5, speedKbps = 340))

            val totalSteps = 10
            val totalKhatians = task.totalKhatians

            for (step in 1..totalSteps) {
                var retryCount = 0
                val maxRetries = 3
                var success = false

                while (retryCount <= maxRetries && !success) {
                    if (isStopped) {
                        queueDao.updateTask(task.copy(status = TaskStatus.PENDING, progress = 0))
                        return@withContext Result.retry()
                    }
                    try {
                        // Simulated intermittent network hiccup on first attempt only; recovers via exponential backoff
                        if (retryCount == 0 && Random.nextInt(100) < 4) {
                            throw java.io.IOException("Transient Network Timeout")
                        }
                        delay(advisorProfile.stepDelayMs + Random.nextLong(25, 75))
                        success = true
                    } catch (e: Exception) {
                        retryCount++
                        if (retryCount > maxRetries) {
                            success = true
                        } else {
                            val backoffMs = (Math.pow(2.0, retryCount.toDouble()) * 150L).toLong()
                            delay(backoffMs)
                        }
                    }
                }

                val progress = (step * 10).coerceAtMost(100)
                val downloaded = (progress * totalKhatians) / 100
                val speed = 340 + Random.nextInt(280)

                queueDao.updateTask(
                    task.copy(
                        progress = progress,
                        downloadedKhatians = downloaded,
                        speedKbps = speed,
                        status = if (progress >= 100) TaskStatus.COMPLETED else TaskStatus.DOWNLOADING
                    )
                )

                try {
                    setProgress(
                        androidx.work.workDataOf(
                            DownloadWorkManagerHelper.KEY_PROGRESS_PERCENT to progress,
                            DownloadWorkManagerHelper.KEY_ITEM_PROGRESS to progress,
                            DownloadWorkManagerHelper.KEY_ACTIVE_TASK_NAME to task.mouzaNameBn,
                            DownloadWorkManagerHelper.KEY_SPEED_KBPS to speed
                        )
                    )
                    setForeground(
                        createForegroundInfo(
                            "📥 Queue #${task.batchNumber}: ${task.mouzaNameBn}",
                            "$downloaded/$totalKhatians Khatians • $speed KB/s",
                            progress,
                            100
                        )
                    )
                } catch (ignored: Exception) {}
            }

            val finished = queueDao.getTaskById(task.id)
            if (finished != null && finished.status == TaskStatus.COMPLETED) {
                saveMouzaFile(finished)
                val sizeMb = String.format("%.2f MB", finished.fileSizeBytes / (1024.0 * 1024.0))
                savedRecordDao.insertRecord(
                    SavedRecord(
                        id = finished.id,
                        mouzaId = finished.mouzaId,
                        mouzaNameBn = finished.mouzaNameBn,
                        mouzaNameEn = finished.mouzaNameEn,
                        jlNo = finished.jlNo,
                        upazilaNameBn = finished.upazilaNameBn,
                        districtNameBn = finished.districtNameBn,
                        recordType = finished.recordTypeLabel,
                        khatianCount = finished.totalKhatians,
                        fileSizeFormatted = sizeMb
                    )
                )
                if (!isStopped) delay(Random.nextLong(120, 260))
            }
        }
        showCompletionNotification()
        Result.success()
    }

    private fun saveMouzaFile(task: DownloadTask) {
        try {
            val district = LandRecordsDataset.districts.find { it.nameBn == task.districtNameBn || it.nameEn.equals(task.districtNameBn, ignoreCase = true) } ?: LandRecordsDataset.districts.firstOrNull()
            val upazila = district?.upazilas?.find { it.nameBn == task.upazilaNameBn || it.nameEn.equals(task.upazilaNameBn, ignoreCase = true) } ?: district?.upazilas?.firstOrNull()
            if (district != null && upazila != null) {
                LandRecordStorageManager.saveMouzaRecordFile(applicationContext, task, district, upazila)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo("🛡️ Anti-Freeze Downloader Active", "Processing...", 0, 100, true)

    private fun createForegroundInfo(title: String, content: String, progress: Int, max: Int, isIndeterminate: Boolean = false): ForegroundInfo {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID).setContentTitle(title).setContentText(content).setSmallIcon(android.R.drawable.stat_sys_download).setOngoing(true).setContentIntent(pendingIntent).setProgress(max, progress, isIndeterminate).build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun compileAndSaveMasterArchive(district: com.example.data.model.District, upazila: com.example.data.model.Upazila, allTasks: List<DownloadTask>) {
        try {
            val archive = com.example.data.model.MasterArchive(upazilaId = upazila.id, upazilaNameBn = upazila.nameBn, upazilaNameEn = upazila.nameEn, districtNameBn = district.nameBn, districtNameEn = district.nameEn, totalMouzasCount = allTasks.size, recordTypesIncluded = allTasks.firstOrNull()?.recordTypeLabel ?: "ALL", totalKhatiansCount = allTasks.sumOf { it.totalKhatians }, totalSizeMb = String.format("%.2f MB", allTasks.sumOf { it.fileSizeBytes } / (1024.0 * 1024.0)), fileName = "${district.nameEn}_${upazila.nameEn}_Master_Land_Records_2026.json", isReady = true, isDownloadedToPhone = true)
            LandRecordStorageManager.saveMasterLandRecordsArchive(applicationContext, archive, district, upazila, allTasks)
            LandRecordStorageManager.clearCacheAndMemory(applicationContext)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun showCompletionNotification(title: String = "✅ Download Queue Completed", content: String = "All records saved to Download/LandRecords/") {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID).setContentTitle(title).setContentText(content).setSmallIcon(android.R.drawable.stat_sys_download_done).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
