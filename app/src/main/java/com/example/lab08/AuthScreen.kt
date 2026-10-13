package com.example.lab08

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

// Pantalla de inicio de sesión / registro con correo y contraseña
@Composable
fun AuthScreen() {
    val auth = remember { FirebaseAuth.getInstance() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (email.isBlank() || password.length < 6) {
            error = "Ingresa un correo y una contraseña de al menos 6 caracteres"
            return
        }
        loading = true
        error = null
        val task = if (isRegister) {
            auth.createUserWithEmailAndPassword(email.trim(), password)
        } else {
            auth.signInWithEmailAndPassword(email.trim(), password)
        }
        // Si sale bien, el AuthStateListener de MainActivity cambia a la pantalla de tareas
        task.addOnCompleteListener { result ->
            loading = false
            if (!result.isSuccessful) {
                error = friendlyError(result.exception?.message)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mis tareas",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isRegister) "Crea tu cuenta" else "Inicia sesión para sincronizar tus tareas",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { submit() },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isRegister) "Crear cuenta" else "Iniciar sesión")
            }
        }
        TextButton(onClick = { isRegister = !isRegister; error = null }) {
            Text(if (isRegister) "Ya tengo cuenta" else "No tengo cuenta, crear una")
        }
    }
}

private fun friendlyError(message: String?): String = when {
    message == null -> "Ocurrió un error. Inténtalo de nuevo"
    "badly formatted" in message -> "El correo no tiene un formato válido"
    "already in use" in message -> "Ese correo ya está registrado"
    "password" in message && "invalid" in message.lowercase() -> "Correo o contraseña incorrectos"
    "credential" in message.lowercase() -> "Correo o contraseña incorrectos"
    "network" in message.lowercase() -> "Sin conexión a internet"
    else -> message
}
