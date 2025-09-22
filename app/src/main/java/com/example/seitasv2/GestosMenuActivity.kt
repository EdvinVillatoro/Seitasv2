package com.example.seitasv2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.seitasv2.ui.common.PeachButton
import com.example.seitasv2.ui.common.PeachScreen
import com.example.seitasv2.ui.theme.Seitasv2Theme

class GestosMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Seitasv2Theme {
                Surface {
                    GestosMenuScreen(
                        onOpenDataset = { startActivity(Intent(this, DatasetActivity::class.java)) },
                        onOpenHands = { startActivity(Intent(this, HandsActivity::class.java)) }
                    )
                }
            }
        }
    }
}

@Composable
fun GestosMenuScreen(onOpenDataset: () -> Unit, onOpenHands: () -> Unit) {
    PeachScreen {
        PeachButton(text = "Guardar Gestos", onClick = onOpenDataset)
        Spacer(Modifier.height(12.dp))
        PeachButton(text = "Practicar Gestos Agregados", onClick = onOpenHands)
    }
}
