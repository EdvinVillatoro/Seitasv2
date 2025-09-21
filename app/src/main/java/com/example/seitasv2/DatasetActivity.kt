package com.example.seitasv2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

// Importa helpers HTTP que ya tienes en network.kt
import com.example.seitasv2.httpPost
import com.example.seitasv2.BASE_URL

class DatasetActivity : ComponentActivity() {

    private var landmarker: HandLandmarker? = null

    private val askCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCompose() else finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startCompose() else askCamera.launch(Manifest.permission.CAMERA)
    }

    private fun startCompose() {
        setContent {
            DatasetScreen(
                landmarkerProvider = ::buildLandmarker,
                onDisposeLandmarker = ::disposeLandmarker
            )
        }
    }

    private fun buildLandmarker(onResults: (HandLandmarkerResult) -> Unit): HandLandmarker? {
        if (landmarker != null) return landmarker
        return try {
            val base = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { r, _ -> r?.let(onResults) }
                .setNumHands(2)
                .build()

            landmarker = HandLandmarker.createFromOptions(this, options)
            landmarker
        } catch (e: Exception) {
            Log.e("DatasetActivity", "Init error: ${e.message}", e)
            Toast.makeText(this, "Error iniciando MediaPipe", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun disposeLandmarker() {
        try { landmarker?.close() } catch (_: Exception) {}
        landmarker = null
    }

    override fun onDestroy() {
        disposeLandmarker()
        super.onDestroy()
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun DatasetScreen(
    landmarkerProvider: ((HandLandmarkerResult) -> Unit) -> HandLandmarker?,
    onDisposeLandmarker: () -> Unit
) {
    val ctx = LocalContext.current
    var result by remember { mutableStateOf<HandLandmarkerResult?>(null) }
    var label by remember { mutableStateOf("A") }
    val previewView = remember {
        PreviewView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // buffer con landmarks de frames
    val buffer = remember { mutableStateListOf<List<Float>>() }

    val onResults: (HandLandmarkerResult) -> Unit = { newResult ->
        result = newResult
        val hand = newResult.landmarks().firstOrNull()
        if (hand != null) {
            val flat = hand.flatMap { listOf(it.x(), it.y(), it.z()) }
            buffer.add(flat)
            if (buffer.size > 30) buffer.removeAt(0)
        }
    }

    DisposableEffect(Unit) {
        val detector = landmarkerProvider(onResults)
        if (detector == null) {
            return@DisposableEffect onDispose { onDisposeLandmarker() }
        }

        val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
        val preview = Preview.Builder()
            .setTargetResolution(Size(640, 480))
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    try {
                        val bmp = imageProxy.toBitmap() // usa el helper de HandsActivity
                        val mpImage = BitmapImageBuilder(bmp).build()
                        val opts = ImageProcessingOptions.builder().build()
                        detector.detectAsync(mpImage, opts, System.currentTimeMillis())
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                ctx.findActivity(),
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                analysis
            )
        } catch (e: Exception) {
            Log.e("DatasetActivity", "Bind camera error", e)
        }

        onDispose {
            cameraProvider.unbindAll()
            onDisposeLandmarker()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Overlay landmarks
        if (result != null) {
            LandmarkOverlay(
                modifier = Modifier.fillMaxSize(),
                result = result,
                usarCamaraFrontal = true,
                srcWidth = 640,
                srcHeight = 480,
                rotationDegrees = 0
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Etiqueta del gesto") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (buffer.isNotEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val payload = JSONObject().apply {
                                    put("nombre", label) // 👈 coincide con backend
                                    put("datos", JSONArray(buffer.map { JSONArray(it) }))
                                }
                                val res = httpPost(ctx, "$BASE_URL/gestos", payload.toString())
                                Log.d("DatasetActivity", "Respuesta servidor: $res")
                            } catch (e: Exception) {
                                Log.e("DatasetActivity", "Error guardando gesto", e)
                            }
                        }
                        buffer.clear()
                        Toast.makeText(ctx, "Gesto enviado a DB", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("Guardar gesto") }

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { ctx.findActivity().finish() }) {
                Text("Volver")
            }
        }
    }
}

/* ---------- Overlay para landmarks ---------- */

@Composable
private fun LandmarkOverlay(
    modifier: Modifier,
    result: HandLandmarkerResult?,
    usarCamaraFrontal: Boolean,
    srcWidth: Int,
    srcHeight: Int,
    rotationDegrees: Int
) {
    if (result == null) return

    val connections = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,          // pulgar
        0 to 5, 5 to 6, 6 to 7, 7 to 8,          // índice
        5 to 9, 9 to 10, 10 to 11, 11 to 12,     // medio
        9 to 13, 13 to 14, 14 to 15, 15 to 16,   // anular
        13 to 17, 17 to 18, 18 to 19, 19 to 20,  // meñique
        0 to 17                                  // palma
    )

    val hands = result.landmarks()

    Canvas(modifier) {
        val viewW = size.width
        val viewH = size.height
        hands.forEach { hand ->
            connections.forEach { (a, b) ->
                val p1 = toViewCoords(hand[a].x(), hand[a].y(), srcWidth, srcHeight, viewW, viewH, usarCamaraFrontal)
                val p2 = toViewCoords(hand[b].x(), hand[b].y(), srcWidth, srcHeight, viewW, viewH, usarCamaraFrontal)
                drawLine(Color.Green, p1, p2, strokeWidth = 4f, cap = StrokeCap.Round)
            }
            hand.forEach { l ->
                val p = toViewCoords(l.x(), l.y(), srcWidth, srcHeight, viewW, viewH, usarCamaraFrontal)
                drawCircle(Color.Yellow, radius = 6f, center = p, style = Stroke(width = 2f))
            }
        }
    }
}
