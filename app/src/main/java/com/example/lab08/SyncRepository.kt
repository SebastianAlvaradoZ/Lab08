package com.example.lab08

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sincroniza Room (fuente de verdad local) con Firestore.
 * Ruta en la nube: users/{uid}/tasks/{cloudId}
 * Resolución de conflictos: gana el cambio con mayor updatedAt (last-writer-wins).
 */
class SyncRepository(private val dao: TaskDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex() // procesa un snapshot a la vez
    private var registration: ListenerRegistration? = null
    private var uid: String? = null

    private fun tasksCollection(uid: String) =
        firestore.collection("users").document(uid).collection("tasks")

    /** Empieza a escuchar la nube para este usuario. */
    fun start(uid: String) {
        if (this.uid == uid && registration != null) return
        stop()
        this.uid = uid
        var isFirstSnapshot = true

        registration = tasksCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            val first = isFirstSnapshot
            isFirstSnapshot = false

            // Copiamos los datos aquí (hilo principal) y trabajamos con Room en IO
            val changes = snapshot.documentChanges.map {
                RemoteChange(it.type, it.document.id, it.document.data)
            }
            val remoteStamps = snapshot.documents.associate {
                it.id to (it.getLong("updatedAt") ?: 0L)
            }

            scope.launch {
                mutex.withLock {
                    changes.forEach { applyChange(it) }
                    // Al iniciar sesión, subir lo que solo existe (o es más nuevo) en este dispositivo
                    if (first) uploadMissing(remoteStamps)
                }
            }
        }
    }

    /** Deja de escuchar (al cerrar sesión). */
    fun stop() {
        registration?.remove()
        registration = null
        uid = null
    }

    /** Sube (crea o actualiza) una tarea a la nube. */
    fun push(task: Task) {
        val u = uid ?: return
        tasksCollection(u).document(task.cloudId).set(
            mapOf(
                "description" to task.description,
                "isCompleted" to task.isCompleted,
                "reminderAt" to task.reminderAt,
                "updatedAt" to task.updatedAt
            )
        )
    }

    /** Elimina una tarea de la nube. */
    fun remove(cloudId: String) {
        val u = uid ?: return
        tasksCollection(u).document(cloudId).delete()
    }

    // ---- Nube -> Room ----

    private class RemoteChange(
        val type: DocumentChange.Type,
        val cloudId: String,
        val data: Map<String, Any>?
    )

    private suspend fun applyChange(change: RemoteChange) {
        if (change.type == DocumentChange.Type.REMOVED) {
            dao.deleteByCloudId(change.cloudId)
            return
        }
        val data = change.data ?: return
        val remoteUpdatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
        val description = data["description"] as? String ?: return
        val isCompleted = data["isCompleted"] as? Boolean ?: false
        val reminderAt = (data["reminderAt"] as? Number)?.toLong()

        val local = dao.getByCloudId(change.cloudId)
        if (local == null) {
            dao.insertTask(
                Task(
                    description = description,
                    isCompleted = isCompleted,
                    reminderAt = reminderAt,
                    cloudId = change.cloudId,
                    updatedAt = remoteUpdatedAt
                )
            )
        } else if (remoteUpdatedAt > local.updatedAt) {
            dao.updateTask(
                local.copy(
                    description = description,
                    isCompleted = isCompleted,
                    reminderAt = reminderAt,
                    updatedAt = remoteUpdatedAt
                )
            )
        }
    }

    // ---- Room -> Nube (carga inicial) ----

    private suspend fun uploadMissing(remoteStamps: Map<String, Long>) {
        dao.getAllOnce().forEach { local ->
            val remoteStamp = remoteStamps[local.cloudId]
            if (remoteStamp == null || local.updatedAt > remoteStamp) push(local)
        }
    }
}
