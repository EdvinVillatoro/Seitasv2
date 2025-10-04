package com.example.seitasv2

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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.sqrt

data class GestoDBAhorcado(val id: Int, val nombre: String, val datos: List<Float>)

class PracticasActivity : ComponentActivity() {
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
            Seitasv2Theme {
                Surface {
                    PracticasScreen(
                        landmarkerProvider = ::buildLandmarker,
                        onDisposeLandmarker = ::disposeLandmarker
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
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            landmarker = HandLandmarker.createFromOptions(this, options)
            landmarker
        } catch (e: Exception) {
            Log.e("PracticasActivity", "Init error: ${e.message}", e)
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

@Composable
fun PracticasScreen(
    landmarkerProvider: ((HandLandmarkerResult) -> Unit) -> HandLandmarker?,
    onDisposeLandmarker: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estados del juego
    var palabra by remember { mutableStateOf("") }
    var pista by remember { mutableStateOf("") }
    var progreso by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var puntaje by remember { mutableStateOf(0) }
    var intentosRestantes by remember { mutableStateOf(6) }

    // Gestos DB
    var gestosDB by remember { mutableStateOf<List<GestoDBAhorcado>>(emptyList()) }

    // Estados detección y confirmación
    var gestoDetectado by remember { mutableStateOf("") }
    var progresoBarra by remember { mutableStateOf(0f) }
    var letraPendiente by remember { mutableStateOf<String?>(null) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var letraConfirmada by remember { mutableStateOf("") }
    var timerActivo by remember { mutableStateOf(false) }

    // Funciones de juego
    fun cargarNuevaPalabra() {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val body = httpGet(context, "$BASE_URL/practicas/ahorcado")
                    val obj = JSONObject(body)
                    Pair(obj.getString("palabra"), obj.optString("pista", ""))
                }
            }.onSuccess { (palabraReal, pistaAux) ->
                palabra = palabraReal.uppercase()
                pista = pistaAux
                progreso = "_".repeat(palabra.length)
                mensaje = ""
                intentosRestantes = 6
                puntaje = 0
            }.onFailure { e ->
                mensaje = "Error: ${e.message}"
            }
        }
    }

    fun procesarIntento(letra: String) {
        if (palabra.isEmpty()) return
        val upper = letra.uppercase()
        var nuevoProgreso = progreso.toCharArray()
        var acierto = false

        for (i in palabra.indices) {
            if (palabra[i].toString() == upper) {
                nuevoProgreso[i] = upper[0]
                acierto = true
            }
        }
        progreso = String(nuevoProgreso)

        if (!acierto) {
            intentosRestantes--
            if (intentosRestantes <= 0) {
                mensaje = "¡Perdiste! La palabra era $palabra"
                puntaje -= 5
            }
        } else if (!progreso.contains("_")) {
            mensaje = "¡Ganaste! Palabra completada."
            puntaje += 10
        }
    }

    // Cargar palabra y gestos
    LaunchedEffect(Unit) {
        cargarNuevaPalabra()
        scope.launch {
            try {
                val lista = getGestos(context)
                gestosDB = lista.map { GestoDBAhorcado(it.id, it.nombre, it.datos) }
            } catch (e: Exception) {
                Log.e("PracticasActivity", "Error cargando gestos", e)
            }
        }
    }

    // Timer de 3.5 segundos, solo cuando se activa
    LaunchedEffect(timerActivo) {
        if (timerActivo) {
            progresoBarra = 0f
            val start = SystemClock.elapsedRealtime()
            while (SystemClock.elapsedRealtime() - start < 3500 && timerActivo) {
                val elapsed = SystemClock.elapsedRealtime() - start
                progresoBarra = (elapsed / 3500f).coerceIn(0f, 1f)
                delay(100)
            }
            if (timerActivo && gestoDetectado.isNotBlank()) {
                letraPendiente = gestoDetectado
                mostrarConfirmacion = true
                timerActivo = false
                progresoBarra = 0f
            }
        }
    }

    // UI
    Column(Modifier.fillMaxSize()) {
        // Info juego
        Column(
            Modifier.weight(0.35f).fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Pista: $pista")
            Text("Progreso: $progreso", style = MaterialTheme.typography.headlineMedium)
            Text("Intentos restantes: $intentosRestantes")
            Text("Puntaje: $puntaje")
            Spacer(Modifier.height(8.dp))

            Text("Gesto detectado: $gestoDetectado")
            if (timerActivo) {
                LinearProgressIndicator(
                    progress = progresoBarra,
                    modifier = Modifier.fillMaxWidth(0.7f).height(12.dp),
                    color = Color.Red
                )
            }

            if (letraConfirmada.isNotBlank()) {
                Text("Última letra confirmada: $letraConfirmada", color = Color.Green)
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = { timerActivo = true }) {
                Text("Seleccionar letra")
            }

            if (mensaje.isNotBlank()) {
                Text(mensaje, color = Color.Red)
                Button(onClick = { cargarNuevaPalabra() }) { Text("Nueva palabra") }
            }
        }

        // Cámara
        Box(Modifier.weight(0.65f).fillMaxWidth()) {
            CameraWithHandsAhorcado(
                landmarkerProvider = landmarkerProvider,
                onDisposeLandmarker = onDisposeLandmarker,
                onVectorDetectado = { vector ->
                    val match = gestosDB.minByOrNull { euclideanDistanceAhorcado(vector, it.datos) }
                    gestoDetectado =
                        if (match != null && euclideanDistanceAhorcado(vector, match.datos) < 10f) match.nombre else ""
                }
            )
        }
    }

    // Confirmación
    if (mostrarConfirmacion && letraPendiente != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false; letraPendiente = null },
            title = { Text("Confirmar letra") },
            text = { Text("¿Quieres usar la letra '${letraPendiente}'?") },
            confirmButton = {
                TextButton(onClick = {
                    procesarIntento(letraPendiente!!)
                    mostrarConfirmacion = false
                    letraConfirmada = letraPendiente!!
                    letraPendiente = null
                }) { Text("Sí") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                    letraPendiente = null
                }) { Text("No") }
            }
        )
    }
}

/* ================== Cámara ================== */

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraWithHandsAhorcado(
    landmarkerProvider: ((HandLandmarkerResult) -> Unit) -> HandLandmarker?,
    onDisposeLandmarker: () -> Unit,
    onVectorDetectado: (List<Float>) -> Unit
) {
    val ctx = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val onResults: (HandLandmarkerResult) -> Unit = { newResult ->
        newResult.landmarks().firstOrNull()?.let { hand ->
            val wrist = hand[0]
            val middle = hand[9]
            val refDist = sqrt(
                (middle.x() - wrist.x()) * (middle.x() - wrist.x()) +
                        (middle.y() - wrist.y()) * (middle.y() - wrist.y()) +
                        (middle.z() - wrist.z()) * (middle.z() - wrist.z())
            ).coerceAtLeast(1e-6f)
            val vector = hand.flatMap { l ->
                listOf(
                    (l.x() - wrist.x()) / refDist,
                    (l.y() - wrist.y()) / refDist,
                    (l.z() - wrist.z()) / refDist
                )
            }
            onVectorDetectado(vector)
        }
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
                        val bitmap = imageProxy.toBitmapAhorcado()
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        val opts = ImageProcessingOptions.builder()
                            .setRotationDegrees(imageProxy.imageInfo.rotationDegrees)
                            .build()
                        val ts = SystemClock.elapsedRealtime()
                        detector?.detectAsync(mpImage, opts, ts)
                    } catch (t: Throwable) {
                        Log.e("CameraWithHandsAhorcado", "Analyzer error", t)
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider.unbindAll()
            (ctx.findAhorcadoActivity() as? ComponentActivity)?.let { activity ->
                cameraProvider.bindToLifecycle(activity, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
            }
        } catch (e: Exception) {
            Log.e("CameraWithHandsAhorcado", "Bind camera error", e)
        }

        onDispose {
            cameraProvider.unbindAll()
            onDisposeLandmarker()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

/* ================== Helpers ================== */

fun euclideanDistanceAhorcado(v1: List<Float>, v2: List<Float>): Float {
    if (v1.size != v2.size) return Float.MAX_VALUE
    var sum = 0f
    for (i in v1.indices) {
        val diff = v1[i] - v2[i]
        sum += diff * diff
    }
    return sqrt(sum)
}

fun Context.findAhorcadoActivity(): ComponentActivity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is ComponentActivity) return c
        c = c.baseContext
    }
    return null
}

fun ImageProxy.toBitmapAhorcado(): Bitmap {
    val yBuffer: ByteBuffer = planes[0].buffer
    val uBuffer: ByteBuffer = planes[1].buffer
    val vBuffer: ByteBuffer = planes[2].buffer
    val ySize: Int = yBuffer.remaining()
    val uSize: Int = uBuffer.remaining()
    val vSize: Int = vBuffer.remaining()
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
