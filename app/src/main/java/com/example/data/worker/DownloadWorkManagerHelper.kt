package com.example.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Controller and bridge for scheduling, observing, and controlling
 * the background WorkManager queue download worker.
 */
object DownloadWorkManagerHelper {

    const val UNIQUE_WORK_NAME = "lr_download_queue_work"
    const val TAG_ANTI_FREEZE = "anti_freeze_downloader"
    const val KEY_AUTO_BATCH_MODE = "key_auto_batch_mode"

    // Progress Data Keys for WorkInfo.progress observation
    const val KEY_PROGRESS_PERCENT = "key_progress_percent"
    const val KEY_ITEM_PROGRESS = "key_item_progress"
    const val KEY_ACTIVE_TASK_NAME = "key_active_task_name"
    const val KEY_COMPLETED_TASKS = "key_completed_tasks"
    const val KEY_TOTAL_TASKS = "key_total_tasks"
    const val KEY_SPEED_KBPS = "key_speed_kbps"
    const val KEY_STATUS_MESSAGE = "key_status_message"

    /**
     * Enqueues the WorkManager download worker with foreground execution support.
     * Uses ExistingWorkPolicy.KEEP by default to avoid duplicate jobs,
     * or ExistingWorkPolicy.REPLACE when user explicitly resumes/re-triggers.
     */
    fun enqueueDownloadWork(
        context: Context,
        replace: Boolean = false,
        autoBatchMode: Boolean = true
    ) {
        try {
            val inputData = androidx.work.Data.Builder()
                .putBoolean(KEY_AUTO_BATCH_MODE, autoBatchMode)
                .build()

            val constraints = Constraints.Builder()
                .build()

            val workRequest = OneTimeWorkRequestBuilder<DownloadQueueWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(TAG_ANTI_FREEZE)
                .build()

            val policy = if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                policy,
                workRequest
            )
        } catch (e: Exception) {
            // Safe fallback when WorkManager is unavailable in local unit test environment
        }
    }

    /**
     * Cancels the active WorkManager job.
     */
    fun cancelDownloadWork(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * Observes whether the WorkManager job is actively ENQUEUED or RUNNING.
     */
    fun isWorkRunningFlow(context: Context): Flow<Boolean> {
        return try {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME)
                .map { workInfoList ->
                    workInfoList.any { info ->
                        info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED
                    }
                }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(false)
        }
    }

    /**
     * Observes the active WorkInfo list for detailed diagnostics.
     */
    fun getWorkInfosFlow(context: Context): Flow<List<WorkInfo>> {
        return try {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME)
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Observes the primary WorkInfo instance for real-time progress feedback.
     */
    fun getPrimaryWorkInfoFlow(context: Context): Flow<WorkInfo?> {
        return try {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME)
                .map { list ->
                    list.firstOrNull { it.tags.contains(TAG_ANTI_FREEZE) } ?: list.firstOrNull()
                }
        } catch (e: Exception) {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }
}
