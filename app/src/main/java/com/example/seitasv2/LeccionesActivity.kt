package com.example.seitasv2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.seitasv2.ui.theme.Seitasv2Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// ----------------------------
// Modelo
// ----------------------------
data class Leccion(
    val id: Int,
    val descripcion: String,
    val videoUrl: String,
    val tips: String,
    val idCategoria: Int,
    val estado: String
)

// ----------------------------
// Activity principal
// ----------------------------
class LeccionesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Seitasv2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LeccionesScreen()
                }
            }
        }
    }
}

// ----------------------------
// Pantalla de lista de lecciones
// ----------------------------
@Composable
fun LeccionesScreen() {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("session", Context.MODE_PRIVATE)
    val tipo = prefs.getString("tipo", "")
    val isAdmin = tipo == "admin"

    var lecciones by remember { mutableStateOf(listOf<Leccion>()) }
    var loading by remember { mutableStateOf(true) }

    // Cargar lista al iniciar
    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = httpGet(ctx, "$BASE_URL/lecciones")
                val arr = JSONArray(body)
                val lista = mutableListOf<Leccion>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    lista.add(
                        Leccion(
                            id = obj.getInt("id"),
                            descripcion = obj.getString("descripcion"),
                            videoUrl = obj.getString("video_url"),
                            tips = obj.getString("tips"),
                            idCategoria = obj.optInt("id_categoria", 0),
                            estado = obj.getString("estado")
                        )
                    )
                }
                lecciones = lista
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Lecciones", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(lecciones) { leccion ->
                    LeccionItem(leccion, isAdmin)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ✅ Solo el admin ve el botón de crear
        if (isAdmin) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val intent = Intent(ctx, LeccionFormActivity::class.java)
                    ctx.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear Nueva Lección")
            }
        }
    }
}

// ----------------------------
// Item de la lista
// ----------------------------
@Composable
fun LeccionItem(leccion: Leccion, isAdmin: Boolean) {
    val ctx = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(leccion.descripcion, style = MaterialTheme.typography.titleMedium)
            Text("Video: ${leccion.videoUrl}", style = MaterialTheme.typography.bodySmall)
            Text("Tips: ${leccion.tips}", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(8.dp))

            Row {
                // Ver video (abre navegador)
                Button(onClick = {
                    val intent = Intent(ctx, VideoPlayerActivity::class.java)
                    intent.putExtra("url", leccion.videoUrl)
                    ctx.startActivity(intent)
                }) { Text("Ver") }

                Spacer(Modifier.width(8.dp))

                // Usuario marca como completada
                if (!isAdmin) {
                    Button(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val body = JSONObject().apply {
                                    put("id_leccion", leccion.id)
                                    put("estado", "completada")
                                }.toString()
                                httpPost(ctx, "$BASE_URL/progreso", body)
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(ctx, "Marcada como completada ✅", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) { Text("Marcar Completada") }
                }

                // Admin puede editar y eliminar
                if (isAdmin) {
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val intent = Intent(ctx, LeccionFormActivity::class.java)
                        intent.putExtra("id", leccion.id)
                        intent.putExtra("descripcion", leccion.descripcion)
                        intent.putExtra("video_url", leccion.videoUrl)
                        intent.putExtra("tips", leccion.tips)
                        ctx.startActivity(intent)
                    }) { Text("Editar") }

                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                httpDelete(ctx, "$BASE_URL/lecciones/${leccion.id}")
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(ctx, "Lección eliminada ❌", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) { Text("Eliminar") }
                }
            }
        }
    }
}

// ----------------------------
// Formulario Crear/Editar
// ----------------------------
class LeccionFormActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Seitasv2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LeccionFormScreen(intent)
                }
            }
        }
    }
}

@Composable
fun LeccionFormScreen(intent: Intent) {
    val ctx = LocalContext.current
    val id = intent.getIntExtra("id", -1)
    var descripcion by remember { mutableStateOf(intent.getStringExtra("descripcion") ?: "") }
    var videoUrl by remember { mutableStateOf(intent.getStringExtra("video_url") ?: "") }
    var tips by remember { mutableStateOf(intent.getStringExtra("tips") ?: "") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(if (id == -1) "Crear Lección" else "Editar Lección", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("URL Video") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = tips, onValueChange = { tips = it }, label = { Text("Tips") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val body = JSONObject().apply {
                        put("descripcion", descripcion)
                        put("video_url", videoUrl)
                        put("tips", tips)
                        put("id_categoria", 1) // ⚠️ por ahora fijo
                    }.toString()

                    if (id == -1) {
                        httpPost(ctx, "$BASE_URL/lecciones", body)
                    } else {
                        httpPut(ctx, "$BASE_URL/lecciones/$id", body)
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(ctx, "Guardado ✅", Toast.LENGTH_SHORT).show()
                        (ctx as? ComponentActivity)?.finish()
                    }
                } catch (e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Guardar")
        }
    }
}
