package com.example.seitasv2

import android.annotation.SuppressLint
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.seitasv2.ui.theme.Seitasv2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GestionLeccionesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Seitasv2Theme {
                GestionLeccionesScreen(onBackClick = { finish() })
            }
        }
    }
}

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionLeccionesScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current as ComponentActivity
    var lecciones by remember { mutableStateOf(emptyList<Leccion>()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Leccion?>(null) }
    var toDelete by remember { mutableStateOf<Leccion?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        isLoading = true
        runCatching {
            withContext(Dispatchers.IO) {
                val body = httpGet(context, "$BASE_URL/lecciones")
                val arr = JSONArray(body)
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    Leccion(
                        id = o.getInt("id"),
                        descripcion = o.getString("descripcion"),
                        videoUrl = o.getString("video_url"),
                        tips = o.optString("tips", ""),
                        idCategoria = if (o.isNull("id_categoria")) null else o.getInt("id_categoria"),
                        estado = o.getString("estado"),
                        fechaGeneracion = o.getString("fecha_generacion"),
                        completada = o.optBoolean("completada", false)
                    )
                }
            }
        }.onSuccess { lecciones = it }
            .onFailure { e -> error = e.message ?: "Error al cargar lecciones" }
        isLoading = false
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Lecciones") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar lección")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(text = error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
                lecciones.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No hay lecciones registradas")
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = lecciones,
                        key = { lec -> lec.id }
                    ) { lec ->
                        LeccionAdminItem(
                            leccion = lec,
                            onEdit = { editing = it; showDialog = true },
                            onDelete = { toDelete = it }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        LeccionFormDialog(
            leccion = editing,
            onDismiss = { showDialog = false },
            onSave = { draft ->
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val payload = JSONObject().apply {
                                put("descripcion", draft.descripcion)
                                put("video_url", draft.videoUrl)
                                put("tips", draft.tips)
                                put("estado", draft.estado)
                            }.toString()
                            if (draft.id == 0) {
                                val res = httpPost(context, "$BASE_URL/lecciones", payload)
                                val obj = JSONObject(res)
                                Leccion(
                                    id = obj.getInt("id"),
                                    descripcion = obj.getString("descripcion"),
                                    videoUrl = obj.getString("video_url"),
                                    tips = obj.optString("tips", ""),
                                    idCategoria = if (obj.isNull("id_categoria")) null else obj.getInt("id_categoria"),
                                    estado = obj.getString("estado"),
                                    fechaGeneracion = obj.getString("fecha_generacion"),
                                    completada = obj.optBoolean("completada", false)
                                )
                            } else {
                                httpPut(context, "$BASE_URL/lecciones/${draft.id}", payload)
                                draft
                            }
                        }
                    }.onSuccess {
                        showDialog = false
                        scope.launch { reload() }
                        scope.launch { snackbarHostState.showSnackbar("Lección guardada") }
                    }.onFailure { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Eliminar lección") },
            text = { Text("¿Seguro que deseas eliminar esta lección?") },
            confirmButton = {
                TextButton(onClick = {
                    val target = toDelete!!
                    toDelete = null
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                httpDelete(context, "$BASE_URL/lecciones/${target.id}")
                            }
                        }.onSuccess {
                            scope.launch { reload() }
                            scope.launch { snackbarHostState.showSnackbar("Lección eliminada") }
                        }.onFailure { e ->
                            Toast.makeText(context, "No se pudo eliminar: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } }
        )
    }
}

/* -------- Items de UI ---------- */

@Composable
private fun LeccionAdminItem(
    leccion: Leccion,
    onEdit: (Leccion) -> Unit,
    onDelete: (Leccion) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("ID: ${leccion.id}", style = MaterialTheme.typography.bodySmall)
            Text("Descripción: ${leccion.descripcion}", style = MaterialTheme.typography.bodyMedium)
            Text("Estado: ${leccion.estado}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { onEdit(leccion) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = { onDelete(leccion) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeccionFormDialog(
    leccion: Leccion?,
    onDismiss: () -> Unit,
    onSave: (Leccion) -> Unit
) {
    var descripcion by remember { mutableStateOf(leccion?.descripcion ?: "") }
    var videoUrl by remember { mutableStateOf(leccion?.videoUrl ?: "") }
    var tips by remember { mutableStateOf(leccion?.tips ?: "") }
    var estado by remember { mutableStateOf(leccion?.estado ?: "pendiente") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (leccion == null) "Nueva Lección" else "Editar Lección") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("URL del Video") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tips,
                    onValueChange = { tips = it },
                    label = { Text("Tips") },
                    modifier = Modifier.fillMaxWidth()
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = estado,
                        onValueChange = {},
                        label = { Text("Estado") },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        readOnly = true
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("pendiente", "validada").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { estado = option; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Leccion(
                            id = leccion?.id ?: 0,
                            descripcion = descripcion,
                            videoUrl = videoUrl,
                            tips = tips,
                            idCategoria = leccion?.idCategoria,
                            estado = estado,
                            fechaGeneracion = leccion?.fechaGeneracion ?: "",
                            completada = leccion?.completada ?: false
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
