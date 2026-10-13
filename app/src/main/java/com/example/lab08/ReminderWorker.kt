package com.example.lab08

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getInt(KEY_TASK_ID, -1)
        if (taskId == -1) return Result.failure()

        val dao = TaskDatabase.getInstance(applicationContext).taskDao()
        val task = dao.getById(taskId)

        // Solo se notifica si la tarea sigue existiendo y no está completada
        if (task != null && !task.isCompleted) {
            NotificationHelper.showReminder(applicationContext, task)
        }
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
    }
}