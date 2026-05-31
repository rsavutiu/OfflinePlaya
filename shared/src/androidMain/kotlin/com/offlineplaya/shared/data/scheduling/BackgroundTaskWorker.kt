package com.offlineplaya.shared.data.scheduling

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.offlineplaya.shared.domain.scheduling.BackgroundTaskKind
import com.offlineplaya.shared.domain.usecase.BurnMetadataUseCase
import com.offlineplaya.shared.domain.usecase.EmbedReport
import com.offlineplaya.shared.presentation.metadata.BurnMetadataCoordinator
import org.koin.core.context.GlobalContext

/**
 * The single Worker class behind [WorkManagerTaskRunner]. Reads the
 * [BackgroundTaskKind] out of [WorkerParameters.inputData] and dispatches
 * to the matching use case.
 */
class BackgroundTaskWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val kind = BackgroundTaskKind.fromId(inputData.getString(KEY_KIND_ID))
            ?: return Result.failure(
                workDataOf(KEY_ERROR to "Unknown background task kind: ${inputData.getString(KEY_KIND_ID)}")
            )

        setForeground(buildForegroundInfo(kind, current = 0, total = 0))

        return when (kind) {
            is BackgroundTaskKind.BurnMetadata -> runBurnMetadata(kind)
        }
    }

    private suspend fun runBurnMetadata(kind: BackgroundTaskKind): Result {
        val useCase: BurnMetadataUseCase = GlobalContext.get().get()
        val report = useCase(
            onProgress = { running ->
                setProgressAsync(
                    workDataOf(
                        KEY_CURRENT to running.processed,
                        KEY_TOTAL to running.total,
                    )
                )
                setForegroundAsync(buildForegroundInfo(kind, running.processed, running.total))
            },
            shouldCancel = { isStopped },
        )
        return when (report) {
            is EmbedReport.Completed -> Result.success(
                workDataOf(
                    BurnMetadataCoordinator.KEY_PROCESSED to report.processed.toLong(),
                    BurnMetadataCoordinator.KEY_EMBEDDED to report.embedded.toLong(),
                    BurnMetadataCoordinator.KEY_FAILED to report.failed.toLong(),
                )
            )
            is EmbedReport.Failed -> Result.failure(workDataOf(KEY_ERROR to report.message))
            EmbedReport.Idle -> Result.failure(workDataOf(KEY_ERROR to "Burn pass produced no result"))
            is EmbedReport.Running -> Result.failure(workDataOf(KEY_ERROR to "Burn pass ended in Running state"))
        }
    }

    private fun buildForegroundInfo(
        kind: BackgroundTaskKind,
        current: Int,
        total: Int,
    ): ForegroundInfo {
        val notification = buildNotification(kind, current, total)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(
        kind: BackgroundTaskKind,
        current: Int,
        total: Int,
    ): Notification {
        val (title, text) = titleAndText(kind, current, total)
        val builder = NotificationCompat.Builder(applicationContext, WorkManagerTaskRunner.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (total > 0) {
            builder.setProgress(total, current, /* indeterminate = */ false)
        } else {
            builder.setProgress(0, 0, /* indeterminate = */ true)
        }
        return builder.build()
    }

    private fun titleAndText(
        kind: BackgroundTaskKind,
        current: Int,
        total: Int,
    ): Pair<String, String> = when (kind) {
        is BackgroundTaskKind.BurnMetadata -> {
            val title = "Burning metadata"
            val text = if (total > 0) "$current / $total tracks" else "Preparing…"
            title to text
        }
    }

    companion object {
        const val KEY_KIND_ID = "kind_id"
        const val KEY_CURRENT = "current"
        const val KEY_TOTAL = "total"
        const val KEY_ERROR = "error"
        private const val NOTIFICATION_ID = 4711
    }
}
