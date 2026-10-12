package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import com.example.data.model.DownloadTask
import com.example.data.model.TaskStatus
import com.example.data.worker.DownloadWorkManagerHelper
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberDark
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.RoseDark
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

/**
 * Parsed snapshot of real-time WorkManager mass download status and progress telemetry.
 */
data class WorkManagerProgressState(
    val state: WorkInfo.State? = null,
    val progressPercent: Int = 0,
    val itemProgress: Int = 0,
    val activeTaskName: String = "",
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val speedKbps: Int = 0,
    val statusMessage: String = "",
    val isRunning: Boolean = false,
    val isEnqueued: Boolean = false,
    val isFinished: Boolean = false,
    val isFailed: Boolean = false
)

/**
 * Helper to extract and normalize progress telemetry from WorkInfo or fallback queue items.
 */
fun parseWorkInfoProgress(
    workInfo: WorkInfo?,
    fallbackQueue: List<DownloadTask> = emptyList()
): WorkManagerProgressState {
    val completedFromQueue = fallbackQueue.count { it.status == TaskStatus.COMPLETED }
    val totalFromQueue = fallbackQueue.size
    val activeFromQueue = fallbackQueue.firstOrNull { it.status == TaskStatus.DOWNLOADING }

    if (workInfo == null) {
        val pct = if (totalFromQueue > 0) (completedFromQueue * 100 / totalFromQueue) else 0
        val isRunning = activeFromQueue != null
        val isFinished = totalFromQueue > 0 && completedFromQueue == totalFromQueue

        return WorkManagerProgressState(
            state = null,
            progressPercent = pct,
            itemProgress = activeFromQueue?.progress ?: 0,
            activeTaskName = activeFromQueue?.let {
                it.referenceId?.let { ref -> "Ref #$ref (${it.mouzaNameBn})" } ?: it.mouzaNameBn
            } ?: "",
            completedTasks = completedFromQueue,
            totalTasks = totalFromQueue,
            speedKbps = activeFromQueue?.speedKbps ?: 0,
            statusMessage = when {
                activeFromQueue != null -> "Downloading ${activeFromQueue.mouzaNameBn}"
                isFinished -> "All downloads finished"
                totalFromQueue > 0 -> "Queue ready ($totalFromQueue items)"
                else -> "Queue idle"
            },
            isRunning = isRunning,
            isFinished = isFinished
        )
    }

    val progressData = workInfo.progress
    val percentFromData = progressData.getInt(DownloadWorkManagerHelper.KEY_PROGRESS_PERCENT, -1)
    val itemProgress = progressData.getInt(DownloadWorkManagerHelper.KEY_ITEM_PROGRESS, 0)
    val activeName = progressData.getString(DownloadWorkManagerHelper.KEY_ACTIVE_TASK_NAME).orEmpty()
    val completedCount = progressData.getInt(DownloadWorkManagerHelper.KEY_COMPLETED_TASKS, completedFromQueue)
    val totalCount = progressData.getInt(DownloadWorkManagerHelper.KEY_TOTAL_TASKS, totalFromQueue)
    val speed = progressData.getInt(DownloadWorkManagerHelper.KEY_SPEED_KBPS, activeFromQueue?.speedKbps ?: 0)
    val message = progressData.getString(DownloadWorkManagerHelper.KEY_STATUS_MESSAGE).orEmpty()

    val calculatedPercent = when {
        percentFromData >= 0 -> percentFromData
        totalCount > 0 -> (completedCount * 100 / totalCount)
        workInfo.state == WorkInfo.State.SUCCEEDED -> 100
        else -> 0
    }

    val activeDisplayName = if (activeName.isNotEmpty()) {
        activeName
    } else {
        activeFromQueue?.let {
            it.referenceId?.let { ref -> "Ref #$ref (${it.mouzaNameBn})" } ?: it.mouzaNameBn
        }.orEmpty()
    }

    return WorkManagerProgressState(
        state = workInfo.state,
        progressPercent = calculatedPercent.coerceIn(0, 100),
        itemProgress = itemProgress,
        activeTaskName = activeDisplayName,
        completedTasks = completedCount,
        totalTasks = totalCount,
        speedKbps = speed,
        statusMessage = when {
            message.isNotEmpty() -> message
            workInfo.state == WorkInfo.State.RUNNING -> "WorkManager service running background downloads..."
            workInfo.state == WorkInfo.State.ENQUEUED -> "WorkManager job enqueued in background scheduler..."
            workInfo.state == WorkInfo.State.SUCCEEDED -> "WorkManager completed all download requests"
            workInfo.state == WorkInfo.State.FAILED -> "WorkManager task encountered an error"
            workInfo.state == WorkInfo.State.CANCELLED -> "WorkManager queue paused or cancelled"
            else -> "WorkManager idle"
        },
        isRunning = workInfo.state == WorkInfo.State.RUNNING,
        isEnqueued = workInfo.state == WorkInfo.State.ENQUEUED,
        isFinished = workInfo.state == WorkInfo.State.SUCCEEDED,
        isFailed = workInfo.state == WorkInfo.State.FAILED
    )
}

/**
 * Clean Material3 LinearProgressIndicator that observes the WorkManager task status
 * with smooth animated progress, adaptive state colors, and accessibility semantics.
 */
@Composable
fun WorkManagerLinearProgressIndicator(
    workInfo: WorkInfo?,
    modifier: Modifier = Modifier,
    fallbackQueue: List<DownloadTask> = emptyList(),
    indicatorHeight: androidx.compose.ui.unit.Dp = 8.dp
) {
    val progressState = parseWorkInfoProgress(workInfo, fallbackQueue)

    val targetFraction = (progressState.progressPercent / 100f).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "wm_linear_progress_animation"
    )

    val indicatorColor by animateColorAsState(
        targetValue = when {
            progressState.isFailed -> RosePrimary
            progressState.isFinished || progressState.progressPercent == 100 -> EmeraldDark
            progressState.isEnqueued -> AmberAccent
            progressState.isRunning -> EmeraldPrimary
            else -> EmeraldPrimary
        },
        animationSpec = tween(300),
        label = "wm_indicator_color"
    )

    val trackColor = when {
        progressState.isFailed -> RoseContainer
        progressState.isEnqueued -> AmberContainer.copy(alpha = 0.5f)
        else -> Slate200
    }

    Box(
        modifier = modifier
            .semantics {
                contentDescription = "WorkManager Download Progress: ${progressState.progressPercent} percent"
            }
            .testTag("workmanager_linear_progress_bar_container")
    ) {
        if (progressState.isEnqueued && progressState.progressPercent == 0) {
            // Indeterminate feedback when enqueued and awaiting worker start
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(indicatorHeight)
                    .clip(RoundedCornerShape(indicatorHeight / 2))
                    .testTag("workmanager_linear_progress_indeterminate"),
                color = indicatorColor,
                trackColor = trackColor
            )
        } else {
            // Determinate smooth animated progress indicator
            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(indicatorHeight)
                    .clip(RoundedCornerShape(indicatorHeight / 2))
                    .testTag("workmanager_linear_progress_determinate"),
                color = indicatorColor,
                trackColor = trackColor
            )
        }
    }
}

/**
 * Comprehensive Material3 Card that observes WorkManager status to provide real-time
 * feedback on mass download progress, active task telemetry, and anti-blocking status.
 */
@Composable
fun WorkManagerProgressFeedbackCard(
    workInfo: WorkInfo?,
    modifier: Modifier = Modifier,
    fallbackQueue: List<DownloadTask> = emptyList(),
    onViewQueueClick: (() -> Unit)? = null
) {
    val progressState = parseWorkInfoProgress(workInfo, fallbackQueue)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("workmanager_progress_feedback_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                progressState.isFailed -> RoseContainer.copy(alpha = 0.35f)
                progressState.isRunning -> EmeraldContainer.copy(alpha = 0.45f)
                progressState.isEnqueued -> AmberContainer.copy(alpha = 0.35f)
                progressState.isFinished -> EmeraldContainer.copy(alpha = 0.3f)
                else -> Slate100
            }
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when {
                    progressState.isFailed -> RosePrimary.copy(alpha = 0.5f)
                    progressState.isRunning -> EmeraldPrimary.copy(alpha = 0.6f)
                    progressState.isEnqueued -> AmberAccent.copy(alpha = 0.6f)
                    progressState.isFinished -> EmeraldDark.copy(alpha = 0.5f)
                    else -> Slate200
                }
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: WorkManager Status Badge + Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        progressState.isFailed -> RosePrimary
                        progressState.isRunning -> EmeraldDark
                        progressState.isEnqueued -> AmberDark
                        progressState.isFinished -> EmeraldDark
                        else -> Slate700
                    },
                    modifier = Modifier.testTag("workmanager_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (progressState.isRunning) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = pulseAlpha))
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    progressState.isFailed -> Icons.Default.Error
                                    progressState.isFinished -> Icons.Default.CheckCircle
                                    progressState.isEnqueued -> Icons.Default.Schedule
                                    else -> Icons.Default.CloudDownload
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Text(
                            text = when {
                                progressState.isFailed -> "FAILED"
                                progressState.isRunning -> "RUNNING • WORKER"
                                progressState.isEnqueued -> "ENQUEUED"
                                progressState.isFinished -> "COMPLETED"
                                else -> "WORKER IDLE"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Progress Percentage Counter
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${progressState.progressPercent}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        ),
                        color = when {
                            progressState.isFailed -> RosePrimary
                            progressState.isFinished -> EmeraldDark
                            else -> Slate900
                        },
                        modifier = Modifier.testTag("workmanager_progress_percent_text")
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = Slate500,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            // The Core Material 3 LinearProgressIndicator
            WorkManagerLinearProgressIndicator(
                workInfo = workInfo,
                fallbackQueue = fallbackQueue,
                indicatorHeight = 10.dp,
                modifier = Modifier.fillMaxWidth()
            )

            // Real-Time Telemetry: Active Task Name & Completed Counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active Task Title
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (progressState.isRunning) Icons.Default.Sync else Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (progressState.isRunning) EmeraldDark else Slate500
                    )
                    Text(
                        text = if (progressState.activeTaskName.isNotEmpty()) {
                            progressState.activeTaskName
                        } else {
                            progressState.statusMessage
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Slate800,
                        maxLines = 1,
                        modifier = Modifier.testTag("workmanager_active_task_name")
                    )
                }

                // Completed Counter (e.g. 5/14)
                if (progressState.totalTasks > 0) {
                    Text(
                        text = "${progressState.completedTasks} / ${progressState.totalTasks} Done",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = if (progressState.completedTasks == progressState.totalTasks) EmeraldDark else Slate600,
                        modifier = Modifier.testTag("workmanager_task_counter")
                    )
                }
            }

            // Real-Time Network Speed & Anti-Blocking Protection Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = EmeraldDark,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Anti-Blocking Jitter Protected",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Slate700
                    )
                }

                if (progressState.isRunning && progressState.speedKbps > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${progressState.speedKbps} KB/s",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = BlueAccent
                        )
                    }
                }
            }
        }
    }
}

/**
 * Self-observing Material3 WorkManager LinearProgressIndicator component.
 * Automatically queries WorkManager for live background status and renders real-time progress.
 */
@Composable
fun WorkManagerLiveProgressIndicator(
    modifier: Modifier = Modifier,
    fallbackQueue: List<DownloadTask> = emptyList(),
    showFullCard: Boolean = true
) {
    val context = LocalContext.current
    val workInfo by DownloadWorkManagerHelper.getPrimaryWorkInfoFlow(context)
        .collectAsState(initial = null)

    if (showFullCard) {
        WorkManagerProgressFeedbackCard(
            workInfo = workInfo,
            fallbackQueue = fallbackQueue,
            modifier = modifier
        )
    } else {
        WorkManagerLinearProgressIndicator(
            workInfo = workInfo,
            fallbackQueue = fallbackQueue,
            modifier = modifier
        )
    }
}
