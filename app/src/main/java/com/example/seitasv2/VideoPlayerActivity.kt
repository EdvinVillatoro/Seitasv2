package com.example.seitasv2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.seitasv2.ui.theme.Seitasv2Theme
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class VideoPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawUrl = intent.getStringExtra("video_url") ?: ""
        val tips = intent.getStringExtra("tips") ?: ""
        val videoId = extractYoutubeId(rawUrl)

        setContent {
            Seitasv2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VideoScreen(videoId, tips)
                }
            }
        }
    }

    /** Extrae el ID de YouTube de forma robusta para múltiples formatos de URL. */
    private fun extractYoutubeId(urlRaw: String): String {
        val url = urlRaw.trim()

        // 1) Full watch URL
        if ("watch?v=" in url) {
            val v = url.substringAfter("watch?v=").substringBefore("&").substringBefore("?")
            return v.trim()
        }

        // 2) Short link youtu.be/VIDEOID[?t=..][&...]
        if ("youtu.be/" in url) {
            val v = url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            return v.trim()
        }

        // 3) /embed/VIDEOID
        if ("/embed/" in url) {
            val v = url.substringAfter("/embed/").substringBefore("?").substringBefore("&")
            return v.trim()
        }

        // 4) /shorts/VIDEOID
        if ("/shorts/" in url) {
            val v = url.substringAfter("/shorts/").substringBefore("?").substringBefore("&")
            return v.trim()
        }

        // 5) /live/VIDEOID
        if ("/live/" in url) {
            val v = url.substringAfter("/live/").substringBefore("?").substringBefore("&")
            return v.trim()
        }

        // 6) Si te mandan directamente el ID
        return url.substringBefore("?").substringBefore("&").trim()
    }
}

@Composable
fun VideoScreen(videoId: String, tips: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "🎬 Video de la Lección",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            if (videoId.isBlank()) {
                // Si la URL venía mala, muéstralo claro al usuario
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se pudo extraer el ID del video.", color = Color.Red)
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        YouTubePlayerView(context).apply {
                            // Muy recomendado: vincular al lifecycle del Activity
                            (context as? ComponentActivity)?.lifecycle?.addObserver(this)

                            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                override fun onReady(player: YouTubePlayer) {
                                    player.loadVideo(videoId, 0f)
                                }
                            })
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "💡 Nota / Tips",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    tips.ifBlank { "Sin nota disponible" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
