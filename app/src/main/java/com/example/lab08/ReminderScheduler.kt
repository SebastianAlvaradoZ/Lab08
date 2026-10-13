package com.example.lab08

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class ReminderScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    // Programa (o reemplaza) el recordatorio de una tarea
    fun schedule(taskId: Int, triggerAtMillis: Long) {
        val delay = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_TASK_ID to taskId))
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(workName(taskId), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(taskId: Int) {
        workManager.cancelUniqueWork(workName(taskId))
    }

    fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG)
    }

    private fun workName(taskId: Int) = "reminder_$taskId"

    private companion object {
        const val TAG = "task_reminder"
    }
}