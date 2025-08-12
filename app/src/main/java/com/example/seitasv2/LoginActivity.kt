package com.example.seitasv2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.seitasv2.ui.common.PeachButton
import com.example.seitasv2.ui.common.PeachScreen
import com.example.seitasv2.ui.common.PeachTextFieldEmail
import com.example.seitasv2.ui.common.PeachTextFieldPassword
import com.example.seitasv2.ui.theme.Seitasv2Theme
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ya logueado → Home
        if (SessionManager(this).isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            Seitasv2Theme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LoginScreen(
                        onSuccess = {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onSuccess: () -> Unit) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showRegister by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun saveAndGo(token: String, user: JSONObject) {
        SessionManager(ctx).saveSession(
            token = token,
            id = user.getInt("id"),
            email = user.getString("email"),
            tipo = user.getString("tipo")
        )
        onSuccess()
    }

    PeachScreen {
        PeachTextFieldEmail(
            value = email,
            onValueChange = { email = it; error = null }
        )
        Spacer(Modifier.height(12.dp))
        PeachTextFieldPassword(
            value = password,
            onValueChange = { password = it; error = null }
        )
        Spacer(Modifier.height(24.dp))

        PeachButton(
            text = "Ingresar",
            onClick = {
                scope.launch {
                    loading = true; error = null
                    runCatching {
                        val (token, user) = loginRequest(ctx, email.trim(), password)
                        saveAndGo(token, user)
                    }.onFailure { e -> error = e.message ?: "Error de autenticación" }
                    loading = false
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        PeachButton(
            text = "Crear una cuenta",
            onClick = { showRegister = true }
        )

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        if (loading) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
    }

    if (showRegister) {
        RegisterDialog(
            onDismiss = { showRegister = false },
            onRegistered = { token, user ->
                showRegister = false
                saveAndGo(token, user)
            }
        )
    }
}

/* ---------- Diálogo de registro ---------- */

@Composable
private fun RegisterDialog(
    onDismiss: () -> Unit,
    onRegistered: (String, JSONObject) -> Unit
) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text("Crear cuenta") },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                PeachTextFieldEmail(
                    value = email,
                    onValueChange = { email = it; error = null }
                )
                PeachTextFieldPassword(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = "Contraseña"
                )
                PeachTextFieldPassword(
                    value = confirm,
                    onValueChange = { confirm = it; error = null },
                    label = "Confirmar contraseña"
                )
                if (error != null)
                    androidx.compose.material3.Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error
                    )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        error = "Email inválido"; return@Button
                    }
                    if (password.length < 6) {
                        error = "Contraseña mínimo 6 caracteres"; return@Button
                    }
                    if (password != confirm) {
                        error = "Las contraseñas no coinciden"; return@Button
                    }
                    scope.launch {
                        loading = true; error = null
                        runCatching {
                            val (token, user) = registerRequest(ctx, email.trim(), password)
                            onRegistered(token, user)
                        }.onFailure { e ->
                            error = e.message ?: "No se pudo registrar"
                        }
                        loading = false
                    }
                },
                enabled = !loading
            ) { androidx.compose.material3.Text("Crear") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("Cancelar")
            }
        }
    )
}
