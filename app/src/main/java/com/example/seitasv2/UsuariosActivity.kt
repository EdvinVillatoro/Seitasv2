package com.example.seitasv2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.seitasv2.ui.theme.Seitasv2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class UsuariosActivity : ComponentActivity() {

    private val isAdmin: Boolean
        get() = getSharedPreferences("session", MODE_PRIVATE)
            .getString("tipo", "") == "admin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Seitasv2Theme {
                Surface(Modifier.fillMaxSize()) {
                    UsuarioScreen(
                        isAdmin = isAdmin,
                        onBackClick = { finish() },
                        onNavigateToGestion = {
                            startActivity(Intent(this, GestionUsuariosActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioScreen(
    isAdmin: Boolean,
    onBackClick: () -> Unit,
    onNavigateToGestion: () -> Unit
) {
    val usuarios = remember { mutableStateListOf<Usuarios>() }
    var cargando by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val response = withContext(Dispatchers.IO) { httpGet(context, "$BASE_URL/usuarios") }
            val jsonArray = JSONArray(response)
            val result = List(jsonArray.length()) { i ->
                val obj = jsonArray.getJSONObject(i)
                Usuarios(
                    id = obj.getInt("id"),
                    email = obj.getString("email"),
                    tipo = obj.getString("tipo"),
                    fecha_creacion = obj.getString("fecha_creacion")
                )
            }
            usuarios.clear(); usuarios.addAll(result)
        } catch (e: Exception) {
            errorMessage = e.message ?: "Error desconocido"
        } finally {
            cargando = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Usuarios") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = onNavigateToGestion) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Gestión de Usuarios")

                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                cargando -> CircularProgressIndicator(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )
                errorMessage != null -> Text(
                    text = errorMessage ?: "Error desconocido",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                usuarios.isEmpty() -> Text(
                    "No hay usuarios registrados",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(usuarios, key = { it.id }) { usuario ->
                        UsuarioItem(usuario = usuario)
                    }
                }
            }
        }
    }
}

@Composable
private fun UsuarioItem(usuario: Usuarios) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ID: ${usuario.id}", style = MaterialTheme.typography.bodySmall)
            Text("Email: ${usuario.email}", style = MaterialTheme.typography.bodyMedium)
            Text("Tipo: ${usuario.tipo}", style = MaterialTheme.typography.bodySmall)
            Text("Creado: ${usuario.fecha_creacion}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
