package com.example.lab08

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // Observa todas las tareas: emite una lista nueva cada vez que la tabla cambia
    @Query("SELECT * FROM tasks ORDER BY id ASC")
    fun observeAll(): Flow<List<Task>>

    // Obtener todas las tareas (consulta puntual)
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>

    // Insertar una nueva tarea
    @Insert
    suspend fun insertTask(task: Task): Long
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Int): Task?
    // Marcar una tarea como completada o no completada
    @Update
    suspend fun updateTask(task: Task)

    // Eliminar una tarea
    @Delete
    suspend fun deleteTask(task: Task)

    // Eliminar las tareas completadas
    @Query("DELETE FROM tasks WHERE is_completed = 1")
    suspend fun deleteCompleted()

    // Eliminar todas las tareas
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
    // --- Sincronización con la nube ---

    // Buscar una tarea por su identificador de la nube
    @Query("SELECT * FROM tasks WHERE cloud_id = :cloudId")
    suspend fun getByCloudId(cloudId: String): Task?

    // Eliminar una tarea por su identificador de la nube (cambio hecho en otro dispositivo)
    @Query("DELETE FROM tasks WHERE cloud_id = :cloudId")
    suspend fun deleteByCloudId(cloudId: String)

    // Tareas locales, para subirlas al iniciar sesión
    @Query("SELECT * FROM tasks")
    suspend fun getAllOnce(): List<Task>
}