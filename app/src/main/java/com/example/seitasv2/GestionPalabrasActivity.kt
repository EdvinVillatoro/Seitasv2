package com.example.seitasv2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.seitasv2.models.Palabra
import com.example.seitasv2.ui.theme.Seitasv2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GestionPalabrasActivity : ComponentActivity() {

    private val isAdmin: Boolean
        get() = getSharedPreferences("session", MODE_PRIVATE)
            .getString("tipo", "") == "admin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isAdmin) {
            Toast.makeText(this, "Acceso solo para administradores", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            Seitasv2Theme {
                GestionPalabrasScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionPalabrasScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var palabras by remember { mutableStateOf(emptyList<Palabra>()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var isActionLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<Palabra?>(null) }
    var wordToDelete by remember { mutableStateOf<Palabra?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        isLoading = true
        runCatching {
            withContext(Dispatchers.IO) { getPalabras(context) }
        }.onSuccess { palabras = it }
            .onFailure { e -> error = e.message ?: "Error al cargar palabras" }
        isLoading = false
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Palabras") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingWord = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar palabra")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
                palabras.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay palabras registradas")
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = palabras, key = { it.id }) { palabra ->
                        WordItem(palabra = palabra,
                            onEdit = { editingWord = it; showDialog = true },
                            onDelete = { wordToDelete = it })
                    }
                }
            }

            if (isActionLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Diálogo crear/editar
    if (showDialog) {
        WordFormDialog(
            palabra = editingWord,
            onDismiss = { showDialog = false },
            onSave = { draft ->
                if (isActionLoading) return@WordFormDialog
                scope.launch {
                    isActionLoading = true
                    val res = runCatching {
                        withContext(Dispatchers.IO) {
                            if (draft.id == 0) {
                                // CREATE
                                addPalabra(context, draft.palabra, draft.pista)
                            } else {
                                // UPDATE
                                updatePalabra(context, draft.id, draft.palabra, draft.pista)
                                draft
                            }
                        }
                    }
                    isActionLoading = false
                    res.onSuccess {
                        reload()
                        showDialog = false
                        snackbarHostState.showSnackbar("Palabra guardada")
                    }.onFailure { e ->
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            }
        )
    }

    // Diálogo de confirmación de borrado
    if (wordToDelete != null) {
        AlertDialog(
            onDismissRequest = { wordToDelete = null },
            title = { Text("Eliminar palabra") },
            text = { Text("¿Seguro que deseas eliminar '${wordToDelete!!.palabra}'?") },
            confirmButton = {
                TextButton(onClick = {
                    val target = wordToDelete!!
                    wordToDelete = null
                    scope.launch {
                        isActionLoading = true
                        val result = runCatching {
                            withContext(Dispatchers.IO) { deletePalabra(context, target.id) }
                        }
                        isActionLoading = false
                        result.onSuccess {
                            palabras = palabras.filter { it.id != target.id }
                            snackbarHostState.showSnackbar("Palabra eliminada")
                        }.onFailure { e ->
                            snackbarHostState.showSnackbar("No se pudo eliminar: ${e.message}")
                        }
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { wordToDelete = null }) { Text("Cancelar") } }
        )
    }
}

/* ==== UI items ==== */

@Composable
private fun WordItem(
    palabra: Palabra,
    onEdit: (Palabra) -> Unit,
    onDelete: (Palabra) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("ID: ${palabra.id}", style = MaterialTheme.typography.bodySmall)
                    Text("Palabra: ${palabra.palabra}", style = MaterialTheme.typography.bodyMedium)
                    Text("Pista: ${palabra.pista}", style = MaterialTheme.typography.bodySmall)
                    Text("Minijuego: ${palabra.minijuego}", style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    IconButton(onClick = { onEdit(palabra) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { onDelete(palabra) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }
    }
}

/* ---------- Diálogo con inputs ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordFormDialog(
    palabra: Palabra?,
    onDismiss: () -> Unit,
    onSave: (Palabra) -> Unit
) {
    var palabraTxt by remember { mutableStateOf(palabra?.palabra ?: "") }
    var pistaTxt by remember { mutableStateOf(palabra?.pista ?: "") }

    val isNewWord = palabra == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNewWord) "Nueva Palabra" else "Editar Palabra") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = palabraTxt,
                    onValueChange = { palabraTxt = it },
                    label = { Text("Palabra") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pistaTxt,
                    onValueChange = { pistaTxt = it },
                    label = { Text("Pista") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Palabra(
                            id = palabra?.id ?: 0,
                            palabra = palabraTxt,
                            pista = pistaTxt,
                            minijuego = "ahorcado"
                        )
                    )
                },
                enabled = palabraTxt.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
