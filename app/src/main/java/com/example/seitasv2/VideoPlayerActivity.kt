package com.example.seitasv2

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.seitasv2.ui.theme.Seitasv2Theme

class VideoPlayerActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled") // necesario para YouTube
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoUrl = intent.getStringExtra("video_url") ?: ""
        val tips = intent.getStringExtra("tips") ?: ""

        setContent {
            Seitasv2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("🎥 Video de la Lección", style = MaterialTheme.typography.headlineMedium)

                        Spacer(Modifier.height(16.dp))

                        // WebView con Compose
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = true
                                    webChromeClient = WebChromeClient()
                                    webViewClient = WebViewClient()
                                    loadUrl(videoUrl)
                                }
                            }
                        )

                        Spacer(Modifier.height(16.dp))
                        Text("💡 Nota / Tips:", style = MaterialTheme.typography.bodyLarge)
                        Text(tips.ifBlank { "Sin nota" }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
