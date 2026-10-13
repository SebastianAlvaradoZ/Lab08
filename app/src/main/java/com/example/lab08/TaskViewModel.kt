package com.example.lab08

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val dao: TaskDao,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = dao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addTask(description: String, reminderAt: Long? = null) {
        viewModelScope.launch {
            val id = dao.insertTask(
                Task(description = description.trim(), reminderAt = reminderAt)
            ).toInt()
            if (reminderAt != null) scheduler.schedule(id, reminderAt)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            dao.updateTask(updated)
            if (updated.isCompleted) {
                scheduler.cancel(task.id)
            } else if (updated.reminderAt != null && updated.reminderAt > System.currentTimeMillis()) {
                scheduler.schedule(task.id, updated.reminderAt)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            scheduler.cancel(task.id)
            dao.deleteTask(task)
        }
    }

    fun deleteCompleted() {
        viewModelScope.launch { dao.deleteCompleted() }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            scheduler.cancelAll()
            dao.deleteAllTasks()
        }
    }
}