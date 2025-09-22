package com.example.seitasv2

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.seitasv2.ui.common.PeachScreen
import com.example.seitasv2.ui.theme.Seitasv2Theme
import kotlinx.coroutines.launch
import org.json.JSONObject

class EstadisticasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Seitasv2Theme {
                EstadisticasScreen()
            }
        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun EstadisticasScreen() {
    val ctx = LocalContext.current as ComponentActivity
    val scope = rememberCoroutineScope()

    var total by remember { mutableStateOf(0) }
    var completadas by remember { mutableStateOf(0) }
    var porcentaje by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val body = httpGet(ctx, "$BASE_URL/estadisticas")
                val obj = JSONObject(body)
                total = obj.getInt("total")
                completadas = obj.getInt("completadas")
                porcentaje = obj.getInt("porcentaje")
            } catch (_: Exception) {}
        }
    }

    PeachScreen {
        Text("📊 Estadísticas", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text("Total de lecciones: $total", style = MaterialTheme.typography.bodyLarge)
        Text("Completadas: $completadas", style = MaterialTheme.typography.bodyLarge)
        Text("Avance: $porcentaje%", style = MaterialTheme.typography.bodyLarge)
    }
}
