package com.offlineplaya.shared.data.scheduling

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.offlineplaya.shared.domain.scheduling.BackgroundTaskKind
import com.offlineplaya.shared.domain.scheduling.BackgroundTaskRunner
import com.offlineplaya.shared.domain.scheduling.BackgroundTaskStatus
import com.offlineplaya.shared.presentation.artwork.EmbedArtCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/**
 * Android implementation of [BackgroundTaskRunner] backed by WorkManager.
 *
 * Each [BackgroundTaskKind] maps to a unique work name so concurrent
 * `enqueue` calls collapse onto the same in-flight job ([ExistingWorkPolicy.KEEP]) —
 * starting an embed pass while one is already running is a no-op that hands
 * back the existing task id.
 *
 * The task id we return is the WorkRequest's UUID string. Callers pass it
 * back to [observe] and [cancel]; the UUID is what WorkManager indexes by
 * even across process death.
 */
class WorkManagerTaskRunner(
    context: Context,
) : BackgroundTaskRunner {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    override suspend fun enqueue(kind: BackgroundTaskKind): String {
        val request = OneTimeWorkRequestBuilder<BackgroundTaskWorker>()
            .setInputData(workDataOf(BackgroundTaskWorker.KEY_KIND_ID to kind.id))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(
                Constraints.Builder()
                    // The embed pass needs network for the MusicBrainz lookup.
                    // Other kinds can override once they get added.
                    .setRequiredNetworkType(
                        if (kind is BackgroundTaskKind.EmbedMissingArt) NetworkType.CONNECTED
                        else NetworkType.NOT_REQUIRED
                    )
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork(
            uniqueWorkName(kind),
            ExistingWorkPolicy.KEEP,
            request,
        ).result.get()
        return request.id.toString()
    }

    override fun observe(taskId: String): Flow<BackgroundTaskStatus> {
        val uuid = java.util.UUID.fromString(taskId)
        return workManager.getWorkInfoByIdFlow(uuid)
            .filterNotNull()
            .map { it.toBackgroundTaskStatus() }
            .distinctUntilChanged()
    }

    override suspend fun cancel(taskId: String) {
        workManager.cancelWorkById(java.util.UUID.fromString(taskId)).result.get()
    }

    private fun uniqueWorkName(kind: BackgroundTaskKind): String = "background_task_${kind.id}"

    private fun WorkInfo.toBackgroundTaskStatus(): BackgroundTaskStatus = when (state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> BackgroundTaskStatus.Pending
        WorkInfo.State.RUNNING -> BackgroundTaskStatus.Running(
            current = progress.getInt(BackgroundTaskWorker.KEY_CURRENT, 0),
            total = progress.getInt(BackgroundTaskWorker.KEY_TOTAL, 0),
        )
        WorkInfo.State.SUCCEEDED -> BackgroundTaskStatus.Succeeded(outputData.toLongMap())
        WorkInfo.State.FAILED -> BackgroundTaskStatus.Failed(
            outputData.getString(BackgroundTaskWorker.KEY_ERROR)
                ?: "Background task failed"
        )
        WorkInfo.State.CANCELLED -> BackgroundTaskStatus.Cancelled
    }

    private fun Data.toLongMap(): Map<String, Long> {
        // We only pass through the well-known numeric payload keys the
        // EmbedMissingArt task uses; any other key would be a wire-format
        // bug rather than something we silently forward.
        val keys = listOf(
            EmbedArtCoordinator.KEY_PROCESSED,
            EmbedArtCoordinator.KEY_EMBEDDED,
            EmbedArtCoordinator.KEY_FAILED,
        )
        val map = mutableMapOf<String, Long>()
        for (k in keys) {
            if (keyValueMap.containsKey(k)) {
                // setProgress / output can be Int OR Long depending on caller.
                map[k] = getLong(k, getInt(k, 0).toLong())
            }
        }
        return map
    }

    companion object {
        /** Notification channel id for the foreground worker notification. The
         *  channel itself is created by the host app at startup. */
        const val CHANNEL_ID = "background_processing"
    }
}
