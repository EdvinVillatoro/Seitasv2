package com.example.seitasv2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.seitasv2.ui.common.PeachButton
import com.example.seitasv2.ui.common.PeachScreen
import com.example.seitasv2.ui.theme.Seitasv2Theme

class PracticasMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Seitasv2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PracticasMenuScreen(
                        onOpenAhorcado = { startActivity(Intent(this, PracticasActivity::class.java)) },
                        onOpenDeletreo = { startActivity(Intent(this, DeletreoActivity::class.java)) },
                        onOpenPracticarGestos = { startActivity(Intent(this, HandsActivity::class.java)) }
                    )
                }
            }
        }
    }
}

@Composable
fun PracticasMenuScreen(
    onOpenAhorcado: () -> Unit,
    onOpenDeletreo: () -> Unit,
    onOpenPracticarGestos: () -> Unit
) {
    PeachScreen {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Menú de Prácticas",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(32.dp))

            // 🧩 Botón 1: Ahorcado
            PeachButton(
                text = "Ahorcado con Gestos",
                onClick = onOpenAhorcado
            )
            Spacer(Modifier.height(16.dp))

            // 🖼 Botón 2: Deletreo
            PeachButton(
                text = "Deletreo por Imagen",
                onClick = onOpenDeletreo
            )
            Spacer(Modifier.height(16.dp))

            // ✋ Botón 3: Practicar Gestos Agregados
            PeachButton(
                text = "Practicar Gestos",
                onClick = onOpenPracticarGestos
            )
        }
    }
}
