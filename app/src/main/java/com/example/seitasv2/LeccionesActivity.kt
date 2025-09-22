package com.example.seitasv2

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

class LeccionesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Seitasv2Theme {
                LeccionesScreen()
            }
        }
    }
}

/* ------------ API ------------- */
suspend fun fetchLeccionesUser(context: ComponentActivity): List<Leccion> =
    withContext(Dispatchers.IO) {
        val body = httpGet(context, "$BASE_URL/lecciones")
        val arr = JSONArray(body)
        val list = mutableListOf<Leccion>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
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
            )
        }
        list
    }

private suspend fun marcarProgresoUser(
    context: ComponentActivity,
    idLeccion: Int,
    checked: Boolean
): Boolean = withContext(Dispatchers.IO) {
    val estado = if (checked) "completada" else "pendiente"
    val payload = JSONObject().apply {
        put("id_leccion", idLeccion)
        put("estado", estado)
    }.toString()
    httpPost(context, "$BASE_URL/progreso", payload)
    true
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "ContextCastToActivity")
@Composable
fun LeccionesScreen() {
    val ctx = LocalContext.current as ComponentActivity
    val scope = rememberCoroutineScope()
    var lecciones by remember { mutableStateOf(listOf<Leccion>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            lecciones = fetchLeccionesUser(ctx)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
        loading = false
    }

    PeachScreen {
        Text("📚 Lecciones", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        if (loading) {
            Text("Cargando…")
        } else if (lecciones.isEmpty()) {
            Text("No hay lecciones disponibles")
        } else {
            LazyColumn {
                items(lecciones, key = { it.id }) { lec ->
                    LeccionItemUsuario(
                        leccion = lec,
                        onVerVideo = {
                            val intent = Intent(ctx, VideoPlayerActivity::class.java).apply {
                                putExtra("video_url", lec.videoUrl)
                                putExtra("tips", lec.tips)
                            }
                            ctx.startActivity(intent)
                        },
                        onToggleVista = { checked ->
                            scope.launch {
                                if (marcarProgresoUser(ctx, lec.id, checked)) {
                                    lecciones = lecciones.map {
                                        if (it.id == lec.id) it.copy(completada = checked) else it
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LeccionItemUsuario(
    leccion: Leccion,
    onVerVideo: () -> Unit,
    onToggleVista: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(text = leccion.descripcion, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        PeachButton(text = "Ver Video", onClick = onVerVideo)
        Spacer(Modifier.height(8.dp))

        PeachButton(
            text = if (leccion.completada) "Vista" else "Marcar como vista",
            onClick = { onToggleVista(!leccion.completada) }
        )
    }
}
