package com.example.seitasv2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import com.example.seitasv2.ui.common.PeachButton
import com.example.seitasv2.ui.common.PeachScreen
import com.example.seitasv2.ui.theme.Seitasv2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Seitasv2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        onStartLessons = { startActivity(Intent(this, LeccionesActivity::class.java)) },
                        onAdminLessons = { startActivity(Intent(this, GestionLeccionesActivity::class.java)) },
                        onOpenUsers = { startActivity(Intent(this, UsuariosActivity::class.java)) },
                        onOpenPracticas = { startActivity(Intent(this, PracticasActivity::class.java)) },
                        onOpenGestos = { startActivity(Intent(this, GestosMenuActivity::class.java)) },
                        onOpenAjustes = { startActivity(Intent(this, AjustesActivity::class.java)) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onStartLessons: () -> Unit,
    onAdminLessons: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenPracticas: () -> Unit,
    onOpenGestos: () -> Unit,
    onOpenAjustes: () -> Unit
) {
    val ctx = LocalContext.current
    val tipo = ctx.getSharedPreferences("session", android.content.Context.MODE_PRIVATE)
        .getString("tipo", "")
    val isAdmin = tipo == "admin"

    PeachScreen {
        // ✅ Todos ven la misma lista de lecciones (validadas)
        PeachButton(text = "Lecciones", onClick = onStartLessons)
        Spacer(Modifier.height(12.dp))

        // ✅ Todos ven prácticas
        PeachButton(text = "Prácticas", onClick = onOpenPracticas)
        Spacer(Modifier.height(12.dp))

        // ✅ Ajustes (Estadísticas + Cerrar Sesión)
        PeachButton(text = "Ajustes", onClick = onOpenAjustes)
        Spacer(Modifier.height(12.dp))

        // 👮‍♂️ Opciones adicionales solo para admin
        if (isAdmin) {
            PeachButton(text = "Administrar Lecciones", onClick = onAdminLessons)
            Spacer(Modifier.height(12.dp))

            PeachButton(text = "Gestos ADMIN", onClick = onOpenGestos)
            Spacer(Modifier.height(12.dp))

            PeachButton(text = "Ver Usuarios", onClick = onOpenUsers)
            Spacer(Modifier.height(12.dp))
        }
    }
}
