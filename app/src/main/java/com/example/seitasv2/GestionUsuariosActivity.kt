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
import com.example.seitasv2.ui.common.PeachButton
import com.example.seitasv2.ui.common.PeachScreen
import com.example.seitasv2.ui.theme.Seitasv2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GestionUsuariosActivity : ComponentActivity() {

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
                GestionUsuariosScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionUsuariosScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var usuarios by remember { mutableStateOf(emptyList<Usuarios>()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var isActionLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<Usuarios?>(null) }
    var userToDelete by remember { mutableStateOf<Usuarios?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        isLoading = true
        runCatching {
            withContext(Dispatchers.IO) {
                val body = httpGet(context, "$BASE_URL/usuarios")
                val arr = JSONArray(body)
                List(arr.length()) { i ->
                    val obj = arr.getJSONObject(i)
                    Usuarios(
                        id = obj.getInt("id"),
                        email = obj.getString("email"),
                        tipo = obj.getString("tipo"),
                        fecha_creacion = obj.getString("fecha_creacion")
                    )
                }
            }
        }.onSuccess { usuarios = it }
            .onFailure { e -> error = e.message ?: "Error al cargar usuarios" }
        isLoading = false
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Usuarios") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingUser = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar usuario")
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
                usuarios.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay usuarios registrados")
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = usuarios, key = { it.id }) { user ->
                        UserItem(user = user,
                            onEdit = { editingUser = it; showDialog = true },
                            onDelete = { userToDelete = it })
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
        UserFormDialog(
            user = editingUser,
            onDismiss = { showDialog = false },
            onSave = { draft, plainPasswordOrNull ->
                if (isActionLoading) return@UserFormDialog
                scope.launch {
                    isActionLoading = true
                    val res = runCatching {
                        withContext(Dispatchers.IO) {
                            if (draft.id == 0) {
                                // CREATE
                                require(!plainPasswordOrNull.isNullOrBlank()) { "Contraseña requerida" }
                                val payload = JSONObject().apply {
                                    put("email", draft.email)
                                    put("tipo", draft.tipo)
                                    put("password", plainPasswordOrNull)
                                }.toString()
                                val body = httpPost(context, "$BASE_URL/usuarios", payload)
                                JSONObject(body).let { obj ->
                                    Usuarios(
                                        id = obj.getInt("id"),
                                        email = obj.getString("email"),
                                        tipo = obj.getString("tipo"),
                                        fecha_creacion = obj.getString("fecha_creacion")
                                    )
                                }
                            } else {
                                // UPDATE
                                val payload = JSONObject().apply {
                                    put("email", draft.email)
                                    put("tipo", draft.tipo)
                                    if (!plainPasswordOrNull.isNullOrBlank())
                                        put("password", plainPasswordOrNull)
                                }.toString()
                                val body = httpPut(context, "$BASE_URL/usuarios/${draft.id}", payload)
                                JSONObject(body).let { obj ->
                                    Usuarios(
                                        id = obj.getInt("id"),
                                        email = obj.getString("email"),
                                        tipo = obj.getString("tipo"),
                                        fecha_creacion = obj.getString("fecha_creacion")
                                    )
                                }
                            }
                        }
                    }
                    isActionLoading = false
                    res.onSuccess { saved ->
                        usuarios = if (editingUser == null) usuarios + saved
                        else usuarios.map { if (it.id == saved.id) saved else it }
                        showDialog = false
                        snackbarHostState.showSnackbar("Usuario guardado")
                    }.onFailure { e ->
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            }
        )
    }

    // Diálogo de confirmación de borrado
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Eliminar usuario") },
            text = { Text("¿Seguro que deseas eliminar a ${userToDelete!!.email}?") },
            confirmButton = {
                TextButton(onClick = {
                    val target = userToDelete!!
                    userToDelete = null
                    scope.launch {
                        isActionLoading = true
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                httpDelete(context, "$BASE_URL/usuarios/${target.id}")
                            }
                        }
                        isActionLoading = false
                        result.onSuccess {
                            usuarios = usuarios.filter { it.id != target.id }
                            snackbarHostState.showSnackbar("Usuario eliminado")
                        }.onFailure { e ->
                            snackbarHostState.showSnackbar("No se pudo eliminar: ${e.message}")
                        }
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { userToDelete = null }) { Text("Cancelar") } }
        )
    }
}

/* ==== UI items ==== */

@Composable
private fun UserItem(
    user: Usuarios,
    onEdit: (Usuarios) -> Unit,
    onDelete: (Usuarios) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("ID: ${user.id}", style = MaterialTheme.typography.bodySmall)
                    Text("Email: ${user.email}", style = MaterialTheme.typography.bodyMedium)
                    Text("Tipo: ${user.tipo}", style = MaterialTheme.typography.bodySmall)
                    Text("Creado: ${user.fecha_creacion}", style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    IconButton(onClick = { onEdit(user) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { onDelete(user) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }
    }
}

/* ---------- Diálogo con inputs nuevos ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFormDialog(
    user: Usuarios?,
    onDismiss: () -> Unit,
    onSave: (Usuarios, String?) -> Unit
) {
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(user?.tipo ?: "usuario") }
    var passwordError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    val isNewUser = user == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNewUser) "Nuevo Usuario" else "Editar Usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                com.example.seitasv2.ui.common.PeachTextFieldEmail(
                    value = email,
                    onValueChange = { email = it; emailError = false }
                )

                if (isNewUser) {
                    com.example.seitasv2.ui.common.PeachTextFieldPassword(
                        value = password,
                        onValueChange = { password = it; passwordError = false },
                        label = "Contraseña"
                    )
                    com.example.seitasv2.ui.common.PeachTextFieldPassword(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; passwordError = false },
                        label = "Confirmar contraseña"
                    )
                }

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = tipo,
                        onValueChange = {},
                        label = { Text("Tipo") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("usuario", "admin").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { tipo = option; expanded = false }
                            )
                        }
                    }
                }

                if (passwordError) {
                    Text(
                        "Las contraseñas no coinciden o son inválidas",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (emailError) {
                    Text("Email inválido", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    emailError = !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    if (isNewUser) passwordError = password != confirmPassword || password.length < 6
                    if (emailError || (isNewUser && passwordError)) return@Button

                    onSave(
                        Usuarios(
                            id = user?.id ?: 0,
                            email = email,
                            tipo = tipo,
                            fecha_creacion = user?.fecha_creacion ?: ""
                        ),
                        if (isNewUser) password else null
                    )
                },
                enabled = email.isNotBlank() && tipo.isNotBlank() &&
                        (!isNewUser || (password.isNotBlank() && confirmPassword.isNotBlank()))
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
