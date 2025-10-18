package com.example.seitasv2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.seitasv2.ui.theme.Seitasv2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class GestionDeletreoActivity : ComponentActivity() {

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
                GestionDeletreoScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionDeletreoScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var imagenes by remember { mutableStateOf(emptyList<ImagenDeletreo>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var editingImagen by remember { mutableStateOf<ImagenDeletreo?>(null) }
    var isActionLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    /** 🔧 Carga imágenes del backend */
    fun reload() {
        scope.launch {
            isLoading = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val body = httpGet(context, "$BASE_URL/deletreo/todas")
                    try {
                        val arr = JSONArray(body)
                        List(arr.length()) { i ->
                            val obj = arr.getJSONObject(i)
                            ImagenDeletreo(
                                id = obj.getInt("id"),
                                palabra = obj.getString("palabra"),
                                imagen_url = obj.getString("imagen_url").trim()
                            )
                        }
                    } catch (_: Exception) {
                        val obj = JSONObject(body)
                        listOf(
                            ImagenDeletreo(
                                id = obj.getInt("id"),
                                palabra = obj.getString("palabra"),
                                imagen_url = obj.getString("imagen_url").trim()
                            )
                        )
                    }
                }
            }.onSuccess { imagenes = it }
                .onFailure { e ->
                    snackbarHostState.showSnackbar("Error: ${e.message}")
                }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Imágenes Deletreo") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { editingImagen = null; showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar imagen")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                imagenes.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay imágenes registradas")
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = imagenes, key = { it.id }) { img ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Palabra: ${img.palabra}")
                                Spacer(Modifier.height(8.dp))

                                val ctx = LocalContext.current
                                AsyncImage(
                                    model = ImageRequest.Builder(ctx)
                                        .data(img.imagen_url.trim())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentScale = ContentScale.Crop,
                                    onSuccess = {
                                        android.util.Log.d("DeletreoTodas", "✅ Cargó: ${img.imagen_url}")
                                    },
                                    onError = {
                                        android.util.Log.e(
                                            "DeletreoTodas",
                                            "❌ Error: ${img.imagen_url} - ${it.result.throwable}"
                                        )
                                    }
                                )

                                Spacer(Modifier.height(8.dp))
                                Row {
                                    IconButton(onClick = { editingImagen = img; showDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            isActionLoading = true
                                            runCatching {
                                                withContext(Dispatchers.IO) {
                                                    httpDelete(context, "$BASE_URL/deletreo/${img.id}")
                                                }
                                            }.onSuccess {
                                                imagenes = imagenes.filter { it.id != img.id }
                                                snackbarHostState.showSnackbar("Eliminada correctamente")
                                            }.onFailure {
                                                snackbarHostState.showSnackbar("Error: ${it.message}")
                                            }
                                            isActionLoading = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isActionLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showDialog) {
        ImagenFormDialog(
            imagen = editingImagen,
            onDismiss = { showDialog = false },
            onSaved = {
                showDialog = false
                reload()
            }
        )
    }
}

/* -------------------------- FORMULARIO -------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagenFormDialog(
    imagen: ImagenDeletreo?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    var palabra by remember { mutableStateOf(imagen?.palabra ?: "") }
    var imagenUri by remember { mutableStateOf<Uri?>(null) }

    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imagenUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (imagen == null) "Nueva Imagen" else "Editar Imagen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = palabra,
                    onValueChange = { palabra = it },
                    label = { Text("Palabra asociada") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { launcher.launch("image/*") }) {
                    Text(if (imagenUri == null) "Seleccionar imagen" else "Imagen seleccionada ✅")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (palabra.isBlank() || imagenUri == null) {
                    Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch {
                        uploadImagenDeletreo(context, palabra, imagenUri!!)
                        onSaved()
                    }
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/* -------------------------- SUBIDA MULTIPART -------------------------- */

fun uploadImagenDeletreo(context: Context, palabra: String, uri: Uri) {
    val contentResolver = context.contentResolver
    val inputStream: InputStream? = contentResolver.openInputStream(uri)
    if (inputStream == null) {
        Toast.makeText(context, "No se pudo abrir la imagen", Toast.LENGTH_SHORT).show()
        return
    }

    val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
    val outputStream = FileOutputStream(file)
    inputStream.copyTo(outputStream)
    outputStream.close()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("palabra", palabra)
        .addFormDataPart(
            "file",
            file.name,
            RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
        )
        .build()

    val request = Request.Builder()
        .url("$BASE_URL/deletreo")
        .addHeader("Authorization", "Bearer ${SessionManager(context).getToken()}")
        .post(requestBody)
        .build()

    Thread {
        try {
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            val resBody = response.body?.string()
            if (response.isSuccessful) {
                (context as ComponentActivity).runOnUiThread {
                    Toast.makeText(context, "Imagen subida correctamente", Toast.LENGTH_SHORT).show()
                }
            } else {
                (context as ComponentActivity).runOnUiThread {
                    Toast.makeText(context, "Error: $resBody", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as ComponentActivity).runOnUiThread {
                Toast.makeText(context, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }.start()
}
