package com.example.lab08

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    // Momento del recordatorio en milisegundos (null = sin recordatorio)
    @ColumnInfo(name = "reminder_at") val reminderAt: Long? = null,
    // Identificador igual en todos los dispositivos (documento en Firestore)
    @ColumnInfo(name = "cloud_id") val cloudId: String = UUID.randomUUID().toString(),
    // Última modificación: sirve para resolver conflictos entre dispositivos
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)