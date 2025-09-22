package com.example.seitasv2

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.seitasv2.ui.theme.Seitasv2Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.ui.platform.LocalContext


class EstadisticasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Seitasv2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EstadisticasScreen()
                }
            }
        }
    }
}

@Composable
fun EstadisticasScreen() {
    val ctx = LocalContext.current
    var completadas by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var porcentaje by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = httpGet(ctx, "$BASE_URL/estadisticas")
                val obj = JSONObject(body)
                completadas = obj.getInt("completadas")
                total = obj.getInt("total")
                porcentaje = obj.getInt("porcentaje")
            } catch (e: Exception) {
                Log.e("Estadisticas", "Error cargando", e)
            } finally {
                loading = false
            }
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(Modifier.padding(16.dp)) {
            Text("Lecciones completadas: $completadas de $total")
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = if (total > 0) completadas.toFloat() / total else 0f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("Progreso: $porcentaje%")
        }
    }
}
