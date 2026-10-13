package com.example.lab08

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lab08.ui.theme.Lab08Theme

class MainActivity : ComponentActivity() {

    // Una única instancia de la base de datos
    private val db by lazy { TaskDatabase.getInstance(applicationContext) }

    // El ViewModel sobrevive a rotaciones; se crea con una factory porque recibe el DAO
    private val viewModel: TaskViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TaskViewModel(db.taskDao(), ReminderScheduler(applicationContext)) as T        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannel(this)
        setContent {
            Lab08Theme {
                TaskScreen(viewModel)
            }
        }
    }
}