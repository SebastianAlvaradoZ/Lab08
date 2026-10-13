package com.example.lab08

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val dao: TaskDao,
    private val scheduler: ReminderScheduler,
    private val sync: SyncRepository
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = dao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // --- Sesión y sincronización ---

    fun startSync(uid: String) = sync.start(uid)

    fun signOut() {
        viewModelScope.launch {
            sync.stop()                 // dejar de escuchar la nube
            scheduler.cancelAll()       // cancelar recordatorios de esta cuenta
            dao.deleteAllTasks()        // limpiar solo lo local (la nube no se toca)
            FirebaseAuth.getInstance().signOut()
        }
    }

    // --- Tareas (cada cambio local también se sube a la nube) ---

    fun addTask(description: String, reminderAt: Long? = null) {
        viewModelScope.launch {
            val task = Task(description = description.trim(), reminderAt = reminderAt)
            val id = dao.insertTask(task).toInt()
            sync.push(task)
            if (reminderAt != null) scheduler.schedule(id, reminderAt)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = !task.isCompleted,
                updatedAt = System.currentTimeMillis()
            )
            dao.updateTask(updated)
            sync.push(updated)
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
            sync.remove(task.cloudId)
        }
    }

    fun deleteCompleted() {
        viewModelScope.launch {
            val completed = dao.getAllOnce().filter { it.isCompleted }
            dao.deleteCompleted()
            completed.forEach { sync.remove(it.cloudId) }
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            val all = dao.getAllOnce()
            scheduler.cancelAll()
            dao.deleteAllTasks()
            all.forEach { sync.remove(it.cloudId) }
        }
    }
}
