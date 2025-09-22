package com.example.seitasv2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.seitasv2.ui.common.PeachButton
import com.example.seitasv2.ui.common.PeachScreen
import com.example.seitasv2.ui.theme.Seitasv2Theme

class AjustesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Seitasv2Theme {
                AjustesScreen(
                    onLogout = {
                        SessionManager(this).clear()
                        startActivity(Intent(this, LoginActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        })
                    },
                    onStats = { startActivity(Intent(this, EstadisticasActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
fun AjustesScreen(onLogout: () -> Unit, onStats: () -> Unit) {
    PeachScreen {
        Text("⚙️ Ajustes")
        Spacer(Modifier.height(16.dp))
        PeachButton(text = "Estadísticas", onClick = onStats)
        Spacer(Modifier.height(12.dp))
        PeachButton(text = "Cerrar Sesión", onClick = onLogout)
    }
}
