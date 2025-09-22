package com.example.seitasv2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.seitasv2.ui.theme.Seitasv2Theme

class PracticasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Seitasv2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {

                }
            }
        }
    }
}


