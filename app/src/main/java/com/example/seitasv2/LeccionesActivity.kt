package com.example.seitasv2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.seitasv2.ui.theme.Seitasv2Theme

class LeccionesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Seitasv2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LeccionesScreen()
                }
            }
        }
    }
}

@Composable
fun LeccionesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Aquí irán tus lecciones de lenguaje de señas")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { /* TODO */ }) {
            Text(text = "Cargar lecciones")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LeccionesScreenPreview() {
    Seitasv2Theme { LeccionesScreen() }
}
