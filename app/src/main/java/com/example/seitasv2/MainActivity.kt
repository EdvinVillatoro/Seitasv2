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
                    var showMiniJuegosAdmin by remember { mutableStateOf(false) }
                    val ctx = this

                    when {
                        showMiniJuegosAdmin -> {
                            AdminMiniJuegosMenu(
                                onBack = { showMiniJuegosAdmin = false },
                                onOpenPalabras = { startActivity(Intent(ctx, GestionPalabrasActivity::class.java)) },
                                onOpenDeletreo = { startActivity(Intent(ctx, GestionDeletreoActivity::class.java)) }
                            )
                        }

                        showUserMenu -> {
                            UserMenuScreen(
                                onBack = { showUserMenu = false },
                                onStartLessons = { startActivity(Intent(ctx, LeccionesActivity::class.java)) },
                                onAdminLessons = { startActivity(Intent(ctx, GestionLeccionesActivity::class.java)) },
                                onOpenUsers = { startActivity(Intent(ctx, UsuariosActivity::class.java)) },
                                onOpenPracticas = { startActivity(Intent(ctx, PracticasMenuActivity::class.java)) }
                            )
                        }

                        else -> {
                            HomeScreen(
                                onOpenUserMenu = { showUserMenu = true },
                                onStartLessons = { startActivity(Intent(ctx, LeccionesActivity::class.java)) },
                                onOpenPracticas = { startActivity(Intent(ctx, PracticasMenuActivity::class.java)) },
                                onOpenGestos = { startActivity(Intent(ctx, GestosMenuActivity::class.java)) },
                                onOpenAjustes = { startActivity(Intent(ctx, AjustesActivity::class.java)) },
                                onOpenAdminMinijuegos = { showMiniJuegosAdmin = true }
                            )
                        }
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
    onOpenAdminMinijuegos: () -> Unit
) {
    val ctx = LocalContext.current
    val tipo = ctx.getSharedPreferences("session", android.content.Context.MODE_PRIVATE)
        .getString("tipo", "")
    val isAdmin = tipo == "admin"

    PeachScreen {
        if (!isAdmin) {
            // ✅ Usuario normal
            PeachButton(text = "Lecciones", onClick = onStartLessons)
            Spacer(Modifier.height(12.dp))

            PeachButton(text = "Prácticas", onClick = onOpenPracticas)
            Spacer(Modifier.height(12.dp))

            PeachButton(text = "Ajustes", onClick = onOpenAjustes)
        } else {
            // 👮‍♂️ Admin: menú extendido
            PeachButton(text = "Usuarios", onClick = onOpenUserMenu)
            Spacer(Modifier.height(12.dp))

            PeachButton(text = "Gestos ADMIN", onClick = onOpenGestos)
            Spacer(Modifier.height(12.dp))

            PeachButton(text = "Admin Minijuegos", onClick = onOpenAdminMinijuegos)
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

        PeachButton(text = "Lecciones", onClick = onStartLessons)
        Spacer(Modifier.height(12.dp))

        PeachButton(text = "Prácticas", onClick = onOpenPracticas)
        Spacer(Modifier.height(12.dp))

        PeachButton(text = "Administrar Lecciones", onClick = onAdminLessons)
        Spacer(Modifier.height(12.dp))

        PeachButton(text = "Administrar Usuarios", onClick = onOpenUsers)
    }
}

@Composable
fun AdminMiniJuegosMenu(
    onBack: () -> Unit,
    onOpenPalabras: () -> Unit,
    onOpenDeletreo: () -> Unit
) {
    PeachScreen {
        PeachButton(text = "⬅ Volver", onClick = onBack)
        Spacer(Modifier.height(16.dp))

        PeachButton(text = "Palabras Ahorcado", onClick = onOpenPalabras)
        Spacer(Modifier.height(16.dp))

        PeachButton(text = "Imágenes Deletreo", onClick = onOpenDeletreo)
    }
}
