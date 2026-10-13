package com.example.lab08

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lab08.ui.theme.Lab08Theme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    // Una única instancia de la base de datos
    private val db by lazy { TaskDatabase.getInstance(applicationContext) }

    // El ViewModel sobrevive a rotaciones; se crea con una factory porque recibe dependencias
    private val viewModel: TaskViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TaskViewModel(
                    db.taskDao(),
                    ReminderScheduler(applicationContext),
                    SyncRepository(db.taskDao())
                ) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannel(this)
        setContent {
            Lab08Theme {
                val auth = remember { FirebaseAuth.getInstance() }
                var user by remember { mutableStateOf(auth.currentUser) }

                // Escucha el inicio/cierre de sesión
                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                // Con sesión iniciada, empieza la sincronización
                LaunchedEffect(user?.uid) {
                    user?.uid?.let { viewModel.startSync(it) }
                }

                if (user == null) {
                    AuthScreen()
                } else {
                    TaskScreen(
                        viewModel = viewModel,
                        userEmail = user?.email,
                        onSignOut = { viewModel.signOut() }
                    )
                }
            }
        }
    }
}
