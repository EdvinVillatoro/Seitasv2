package com.example.seitasv2

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.SystemClock
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.seitasv2.ui.theme.Seitasv2Theme
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.sqrt

data class GestoDB(val id: Int, val nombre: String, val datos: List<Float>)

class HandsActivity : ComponentActivity() {

    private var landmarker: HandLandmarker? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var gestosDB by mutableStateOf<List<GestoDB>>(emptyList())
    private var gestoDetectado by mutableStateOf("Esperando gesto...")

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
        // cargar gestos del backend
        coroutineScope.launch { cargarGestos() }

        setContent {
            Seitasv2Theme {
                Surface {
                    HandsScreen(
                        landmarkerProvider = ::buildLandmarker,
                        onDisposeLandmarker = ::disposeLandmarker,
                        gestoDetectado = { gestoDetectado }
                    )
                }
            }
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
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            landmarker = HandLandmarker.createFromOptions(this, options)
            landmarker
        } catch (e: Exception) {
            Log.e("HandsActivity", "Init error: ${e.message}", e)
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

    /** ==== Lógica para descargar gestos desde backend ==== */
    private suspend fun cargarGestos() {
        try {
            val lista = getGestos(this@HandsActivity)
            gestosDB = lista
            Log.d("HandsActivity", "Gestos cargados: ${gestosDB.size}")
        } catch (e: Exception) {
            Log.e("HandsActivity", "Error cargando gestos", e)
        }
    }


    /** ==== Comparación de vectores ==== */
    private fun euclideanDistance(v1: List<Float>, v2: List<Float>): Float {
        if (v1.size != v2.size) return Float.MAX_VALUE
        var sum = 0f
        for (i in v1.indices) {
            val diff = v1[i] - v2[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    private fun findBestMatch(current: List<Float>): String? {
        var best: String? = null
        var minDist = Float.MAX_VALUE
        for (g in gestosDB) {
            val dist = euclideanDistance(current, g.datos)
            Log.d("HandsActivity", "Comparando con ${g.nombre}, distancia=$dist") // 👈
            if (dist < minDist) {
                minDist = dist
                best = g.nombre
            }
        }
        // prueba con umbral más grande, por ejemplo 10f
        return if (minDist < 10f) best else null
    }



    /** ==== Procesar landmarks ==== */
    /** ==== Procesar landmarks con normalización ==== */
    fun procesarLandmarks(result: HandLandmarkerResult) {
        result.landmarks().firstOrNull()?.let { hand ->
            // 1. Punto de referencia (muñeca = landmark 0)
            val wrist = hand[0]

            // 2. Calcular distancia de referencia (muñeca -> dedo medio base, landmark 9)
            val middle = hand[9]
            val refDist = sqrt(
                (middle.x() - wrist.x()) * (middle.x() - wrist.x()) +
                        (middle.y() - wrist.y()) * (middle.y() - wrist.y()) +
                        (middle.z() - wrist.z()) * (middle.z() - wrist.z())
            ).coerceAtLeast(1e-6f) // evitar división entre 0

            // 3. Normalizar: trasladar al origen (restar muñeca) y escalar por refDist
            val vector = hand.flatMap { l ->
                listOf(
                    (l.x() - wrist.x()) / refDist,
                    (l.y() - wrist.y()) / refDist,
                    (l.z() - wrist.z()) / refDist
                )
            }

            Log.d("HandsActivity", "Vector normalizado=$vector")

            // 4. Comparar con base
            val match = findBestMatch(vector)
            gestoDetectado = match ?: "Sin coincidencia"
        }
    }



}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun HandsScreen(
    landmarkerProvider: ((HandLandmarkerResult) -> Unit) -> HandLandmarker?,
    onDisposeLandmarker: () -> Unit,
    gestoDetectado: () -> String
) {
    val ctx = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var result by remember { mutableStateOf<HandLandmarkerResult?>(null) }
    var landmarkerInitialized by remember { mutableStateOf(false) }
    var lastFrameWidth by remember { mutableStateOf(0) }
    var lastFrameHeight by remember { mutableStateOf(0) }
    var lastRotation by remember { mutableStateOf(0) }

    val onResults: (HandLandmarkerResult) -> Unit = { newResult ->
        result = newResult
        (ctx.findActivity() as? HandsActivity)?.procesarLandmarks(newResult)
    }

    val previewView = remember {
        PreviewView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(Unit) {
        val detector = landmarkerProvider(onResults)
        if (detector == null) {
            return@DisposableEffect onDispose {
                onDisposeLandmarker(); executor.shutdown()
            }
        }
        landmarkerInitialized = true

        val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
        val preview = Preview.Builder().setTargetResolution(Size(640, 480)).build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(executor) { imageProxy ->
                    try {
                        lastFrameWidth = imageProxy.width
                        lastFrameHeight = imageProxy.height
                        lastRotation = imageProxy.imageInfo.rotationDegrees
                        val bitmap = imageProxy.toBitmap()
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        val opts = ImageProcessingOptions.builder()
                            .setRotationDegrees(lastRotation)
                            .build()
                        val ts = SystemClock.elapsedRealtime()
                        detector.detectAsync(mpImage, opts, ts)
                    } catch (t: Throwable) {
                        Log.e("HandsActivity", "Analyzer error", t)
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider.unbindAll()
            (ctx.findActivity() as? ComponentActivity)?.let { activity ->
                cameraProvider.bindToLifecycle(
                    activity,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis
                )
            }
        } catch (e: Exception) {
            Log.e("HandsActivity", "Bind camera error", e)
        }

        onDispose {
            cameraProvider.unbindAll()
            onDisposeLandmarker()
            executor.shutdown()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        if (landmarkerInitialized && lastFrameWidth > 0 && lastFrameHeight > 0) {
            LandmarkOverlay(
                modifier = Modifier.fillMaxSize(),
                result = result,
                usarCamaraFrontal = true,
                srcWidth = lastFrameWidth,
                srcHeight = lastFrameHeight,
                rotationDegrees = lastRotation
            )
        }

        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = gestoDetectado(), color = Color.Yellow)
        }

        Button(
            onClick = { ctx.findActivity()?.finish() },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) { Text("Volver") }
    }
}

/* ---------- Overlay para dibujar landmarks ---------- */
@Composable
fun LandmarkOverlay(
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

    val baseW = if (rotationDegrees % 180 == 0) srcWidth else srcHeight
    val baseH = if (rotationDegrees % 180 == 0) srcHeight else srcWidth

    Canvas(modifier) {
        val viewW = size.width
        val viewH = size.height
        result.landmarks().forEach { hand ->
            connections.forEach { (a, b) ->
                val p1 = toViewCoords(hand[a].x(), hand[a].y(), baseW, baseH, viewW, viewH, usarCamaraFrontal)
                val p2 = toViewCoords(hand[b].x(), hand[b].y(), baseW, baseH, viewW, viewH, usarCamaraFrontal)
                drawLine(Color.Green, p1, p2, strokeWidth = 4f, cap = StrokeCap.Round)
            }
            hand.forEach { l ->
                val p = toViewCoords(l.x(), l.y(), baseW, baseH, viewW, viewH, usarCamaraFrontal)
                drawCircle(Color.Yellow, radius = 6f, center = p, style = Stroke(width = 2f))
            }
        }
    }
}

/* ---------- Helpers ---------- */
fun toViewCoords(
    xNorm: Float,
    yNorm: Float,
    baseW: Int,
    baseH: Int,
    viewW: Float,
    viewH: Float,
    mirrorFront: Boolean
): Offset {
    var rx = yNorm
    var ry = 1f - xNorm
    var x = rx * baseW
    var y = ry * baseH
    val scale = maxOf(viewW / baseW, viewH / baseH)
    val scaledW = baseW * scale
    val scaledH = baseH * scale
    val dx = (viewW - scaledW) / 2f
    val dy = (viewH - scaledH) / 2f
    x = x * scale + dx
    y = y * scale + dy
    if (mirrorFront) x = viewW - x
    return Offset(x, y)
}

fun Context.findActivity(): ComponentActivity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is ComponentActivity) return c
        c = c.baseContext
    }
    return null
}

/** Convierte ImageProxy a Bitmap ARGB_8888 */
fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    return bmp.copy(Bitmap.Config.ARGB_8888, true)
}
