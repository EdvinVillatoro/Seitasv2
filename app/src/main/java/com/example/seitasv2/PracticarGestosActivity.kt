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
import java.util.concurrent.Executors
import kotlin.math.sqrt

// Modelo de datos renombrado
data class PracticaGesto(val id: Int, val nombre: String, val datos: List<Float>)

// Actividad renombrada
class PracticarGestosActivity : ComponentActivity() {

    private var landmarker: HandLandmarker? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Variables de estado renombradas
    private var gestosPracticaDB by mutableStateOf<List<PracticaGesto>>(emptyList())
    private var gestoEnPantalla by mutableStateOf("Esperando gesto...")

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
        // Cargar gestos para la práctica
        coroutineScope.launch { cargarGestosParaPractica() }

        setContent {
            Seitasv2Theme {
                Surface {
                    // Composable principal renombrado
                    PracticaGestosScreen(
                        landmarkerProvider = ::buildLandmarker,
                        onDisposeLandmarker = ::disposeLandmarker,
                        gestoActual = { gestoEnPantalla }
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
            Log.e("PracticarGestosActivity", "Init error: ${e.message}", e)
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
    // Función renombrada
    private suspend fun cargarGestosParaPractica() {
        try {
            val lista = obtenerGestosPractica(this@PracticarGestosActivity)
            gestosPracticaDB = lista
            Log.d("PracticarGestosActivity", "Gestos cargados: ${gestosPracticaDB.size}")
        } catch (e: Exception) {
            Log.e("PracticarGestosActivity", "Error cargando gestos", e)
        }
    }

    // Esqueleto para la función de backend (SIN 'suspend' por ahora para evitar advertencia si no se usa)
    private fun obtenerGestosPractica(context: Context): List<PracticaGesto> {
        Log.w("PracticarGestosActivity", "ADVERTENCIA: Usando base de datos de gestos vacía. Implementa 'obtenerGestosPractica'.")
        // **REEMPLAZA ESTO con la llamada real a tu API/backend**
        return emptyList()
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

    // Función renombrada
    private fun encontrarMejorCoincidencia(current: List<Float>): String? {
        var best: String? = null
        var minDist = Float.MAX_VALUE
        for (g in gestosPracticaDB) {
            val dist = euclideanDistance(current, g.datos)
            Log.d("PracticarGestosActivity", "Comparando con ${g.nombre}, distancia=$dist")
            if (dist < minDist) {
                minDist = dist
                best = g.nombre
            }
        }
        // Usamos un umbral para la detección
        return if (minDist < 10f) best else null
    }


    /** ==== Procesar landmarks con normalización ==== */
    // Función renombrada
    fun procesarLandmarksDeMano(result: HandLandmarkerResult) {
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

            Log.d("PracticarGestosActivity", "Vector normalizado=$vector")

            // 4. Comparar con base
            val match = encontrarMejorCoincidencia(vector)
            // Actualización de la variable de estado
            gestoEnPantalla = match ?: "Sin coincidencia"
        }
    }
}

@OptIn(ExperimentalGetImage::class)
// Composable principal renombrado
@Composable
private fun PracticaGestosScreen(
    landmarkerProvider: ((HandLandmarkerResult) -> Unit) -> HandLandmarker?,
    onDisposeLandmarker: () -> Unit,
    gestoActual: () -> String
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
        // Llamada a la función de procesamiento renombrada
        (ctx.findActivity() as? PracticarGestosActivity)?.procesarLandmarksDeMano(newResult)
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
                        // Utiliza la función de extensión 'toBitmap' existente en el paquete
                        val bitmap = imageProxy.toBitmap()
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        val opts = ImageProcessingOptions.builder()
                            .setRotationDegrees(lastRotation)
                            .build()
                        val ts = SystemClock.elapsedRealtime()
                        detector.detectAsync(mpImage, opts, ts)
                    } catch (t: Throwable) {
                        Log.e("PracticarGestosActivity", "Analyzer error", t)
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
            Log.e("PracticarGestosActivity", "Bind camera error", e)
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
            // Overlay renombrado
            PracticaHandLandmarkOverlay(
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
            Text(text = gestoActual(), color = Color.Yellow)
        }

        Button(
            onClick = { ctx.findActivity()?.finish() },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) { Text("Volver") }
    }
}

/* ---------- Overlay para dibujar landmarks (REUTILIZA toViewCoords) ---------- */
// Composable renombrado
@Composable
fun PracticaHandLandmarkOverlay(
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
            // toViewCoords es una función de extensión o global en HandsActivity.kt
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

// **AQUÍ TERMINA EL ARCHIVO PracticarGestosActivity.kt**
// Las funciones toViewCoords, findActivity y toBitmap NO están incluidas aquí
// porque causaban conflictos y ya existen en HandsActivity.kt