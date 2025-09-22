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
                        onStartLessons = {
                            startActivity(Intent(this, LeccionesActivity::class.java))
                        },
                        onOpenUsers = {
                            startActivity(Intent(this, UsuariosActivity::class.java))
                        },
                        onOpenPracticas = {
                            startActivity(Intent(this, PracticasActivity::class.java))
                        },
                        onOpenDataset = {
                            startActivity(Intent(this, DatasetActivity::class.java))
                        },
                        onOpenHands = {
                            startActivity(Intent(this, HandsActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onStartLessons: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenPracticas: () -> Unit,
    onOpenDataset: () -> Unit,
    onOpenHands: () -> Unit
) {
    val ctx = LocalContext.current
    val tipo = ctx
        .getSharedPreferences("session", android.content.Context.MODE_PRIVATE)
        .getString("tipo", "")

    val isAdmin = tipo == "admin"

    PeachScreen {
        // ✅ Todos ven Lecciones
        PeachButton("Lecciones", onClick = onStartLessons)
        Spacer(Modifier.height(12.dp))

        // ✅ Todos ven Prácticas
        PeachButton("Prácticas", onClick = onOpenPracticas)
        Spacer(Modifier.height(12.dp))

        // 👮‍♂️ Solo Admin
        if (isAdmin) {
            PeachButton("Modo Dataset (Guardar Gestos)", onClick = onOpenDataset)
            Spacer(Modifier.height(12.dp))

            PeachButton("Practicar Gestos Agregados", onClick = onOpenHands)
            Spacer(Modifier.height(12.dp))

            PeachButton("Ver Usuarios", onClick = onOpenUsers)
            Spacer(Modifier.height(12.dp))
        }

        // ✅ Todos ven cerrar sesión
        PeachButton(
            text = "Cerrar sesión",
            onClick = {
                SessionManager(ctx).clear()
                ctx.startActivity(
                    Intent(ctx, LoginActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                )
            }
        )
    }
}
