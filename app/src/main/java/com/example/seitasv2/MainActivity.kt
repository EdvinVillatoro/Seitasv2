package com.example.seitasv2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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
                    var showUserMenu by remember { mutableStateOf(false) }

                    val ctx = this
                    if (showUserMenu) {
                        UserMenuScreen(
                            onBack = { showUserMenu = false },
                            onStartLessons = { startActivity(Intent(ctx, LeccionesActivity::class.java)) },
                            onAdminLessons = { startActivity(Intent(ctx, GestionLeccionesActivity::class.java)) },
                            onOpenUsers = { startActivity(Intent(ctx, UsuariosActivity::class.java)) },
                            onOpenPracticas = { startActivity(Intent(ctx, `PracticasActivity`::class.java)) }
                        )
                    } else {
                        HomeScreen(
                            onOpenUserMenu = { showUserMenu = true },
                            onStartLessons = { startActivity(Intent(ctx, LeccionesActivity::class.java)) },
                            onOpenPracticas = { startActivity(Intent(ctx, `PracticasActivity`::class.java)) },
                            onOpenGestos = { startActivity(Intent(ctx, GestosMenuActivity::class.java)) },
                            onOpenAjustes = { startActivity(Intent(ctx, AjustesActivity::class.java)) },
                            onOpenPalabras = { startActivity(Intent(ctx, GestionPalabrasActivity::class.java)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onOpenUserMenu: () -> Unit,
    onStartLessons: () -> Unit,
    onOpenPracticas: () -> Unit,
    onOpenGestos: () -> Unit,
    onOpenAjustes: () -> Unit,
    onOpenPalabras: () -> Unit
) {
    val ctx = LocalContext.current
    val tipo = ctx.getSharedPreferences("session", android.content.Context.MODE_PRIVATE)
        .getString("tipo", "")
    val isAdmin = tipo == "admin"

    PeachScreen {
        if (!isAdmin) {
            // ✅ Usuario normal: va directo a sus pantallas
            PeachButton(text = "Lecciones", onClick = onStartLessons)
            Spacer(Modifier.height(12.dp))

            PeachButton(text = "Prácticas", onClick = onOpenPracticas)
            Spacer(Modifier.height(12.dp))

            PeachButton(text = "Ajustes", onClick = onOpenAjustes)
        } else {
            // 👮‍♂️ Admin: menú más completo
            PeachButton(text = "Usuarios", onClick = onOpenUserMenu)
            Spacer(Modifier.height(12.dp))

            PeachButton(text = "Gestos ADMIN", onClick = onOpenGestos)
            Spacer(Modifier.height(12.dp))

            PeachButton(text = "Palabras Ahorcado", onClick = onOpenPalabras)
            Spacer(Modifier.height(12.dp))

            PeachButton(text = "Ajustes", onClick = onOpenAjustes)
        }
    }
}

@Composable
fun UserMenuScreen(
    onBack: () -> Unit,
    onStartLessons: () -> Unit,
    onAdminLessons: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenPracticas: () -> Unit
) {
    PeachScreen {
        PeachButton(text = "⬅ Volver", onClick = onBack)
        Spacer(Modifier.height(12.dp))

        // ✅ Todos dentro del menú "Usuarios"
        PeachButton(text = "Lecciones", onClick = onStartLessons)
        Spacer(Modifier.height(12.dp))

        PeachButton(text = "Prácticas", onClick = onOpenPracticas)
        Spacer(Modifier.height(12.dp))

        // 👮‍♂️ Solo admin dentro del menú "Usuarios"
        PeachButton(text = "Administrar Lecciones", onClick = onAdminLessons)
        Spacer(Modifier.height(12.dp))

        PeachButton(text = "Ver Usuarios", onClick = onOpenUsers)
    }
}
