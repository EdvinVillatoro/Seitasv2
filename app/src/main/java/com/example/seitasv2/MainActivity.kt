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
    onOpenUsers: () -> Unit
) {
    val ctx = LocalContext.current
    val isAdmin = ctx
        .getSharedPreferences("session", android.content.Context.MODE_PRIVATE)
        .getString("tipo", "") == "admin"

    PeachScreen {
        PeachButton("Lecciones", onClick = onStartLessons)
        Spacer(Modifier.height(12.dp))

        if (isAdmin) {
            PeachButton("Ver Usuarios", onClick = onOpenUsers)
            Spacer(Modifier.height(12.dp))
        }

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
