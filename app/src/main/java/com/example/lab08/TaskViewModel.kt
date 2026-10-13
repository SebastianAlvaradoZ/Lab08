package com.example.lab08

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    // Estado de la UI: se deriva del Flow de Room y se actualiza solo
    val tasks: StateFlow<List<Task>> = dao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addTask(description: String) {
        viewModelScope.launch {
            dao.insertTask(Task(description = description.trim()))
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            dao.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { dao.deleteTask(task) }
    }

    fun deleteCompleted() {
        viewModelScope.launch { dao.deleteCompleted() }
    }

    fun deleteAllTasks() {
        viewModelScope.launch { dao.deleteAllTasks() }
    }
}