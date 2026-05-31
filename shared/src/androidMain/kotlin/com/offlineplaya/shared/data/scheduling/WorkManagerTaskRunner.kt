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
import com.offlineplaya.shared.presentation.metadata.BurnMetadataCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/**
 * Android implementation of [BackgroundTaskRunner] backed by WorkManager.
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
                    .setRequiredNetworkType(
                        if (kind is BackgroundTaskKind.BurnMetadata) NetworkType.CONNECTED
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
        val keys = listOf(
            BurnMetadataCoordinator.KEY_PROCESSED,
            BurnMetadataCoordinator.KEY_EMBEDDED,
            BurnMetadataCoordinator.KEY_FAILED,
        )
        val map = mutableMapOf<String, Long>()
        for (k in keys) {
            if (keyValueMap.containsKey(k)) {
                map[k] = getLong(k, getInt(k, 0).toLong())
            }
        }
        return map
    }

    companion object {
        const val CHANNEL_ID = "background_processing"
    }
}
