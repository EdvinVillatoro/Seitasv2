package com.example.seitasv2

import androidx.compose.ui.graphics.Color
import android.Manifest
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
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

data class GestoDBDeletreo(val id: Int, val nombre: String, val datos: List<Float>)

class DeletreoActivity : ComponentActivity() {
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
                    DeletreoScreen(
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
                .build()
            landmarker = HandLandmarker.createFromOptions(this, options)
            landmarker
        } catch (e: Exception) {
            Log.e("DeletreoActivity", "Init error: ${e.message}", e)
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
fun DeletreoScreen(
    landmarkerProvider: ((HandLandmarkerResult) -> Unit) -> HandLandmarker?,
    onDisposeLandmarker: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var imagenUrl by remember { mutableStateOf("") }
    var palabra by remember { mutableStateOf("") }
    var progreso by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }

    var gestoDetectado by remember { mutableStateOf("") }
    var progresoBarra by remember { mutableStateOf(0f) }
    var letraPendiente by remember { mutableStateOf<String?>(null) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var timerActivo by remember { mutableStateOf(false) }

    var gestosDB by remember { mutableStateOf<List<GestoDBDeletreo>>(emptyList()) }

    // ======== Cargar imagen y palabra desde backend ========
    fun cargarNuevaImagen() {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val body = httpGet(ctx, "$BASE_URL/deletreo")
                    val obj = JSONObject(body) // ✅ backend devuelve un solo objeto
                    Pair(obj.getString("palabra"), obj.getString("imagen_url"))
                }
            }.onSuccess { (pal, url) ->
                palabra = pal.uppercase()
                progreso = "_".repeat(palabra.length)
                imagenUrl = url
                mensaje = ""
            }.onFailure { e ->
                mensaje = "Error: ${e.message}"
            }
        }
    }

    fun procesarIntento(letra: String) {
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
        mensaje = when {
            !acierto -> "Letra incorrecta"
            !progreso.contains("_") -> "¡Correcto!"
            else -> "Letra correcta"
        }
    }

    // ======== Cargar datos iniciales ========
    LaunchedEffect(Unit) {
        cargarNuevaImagen()
        scope.launch {
            try {
                val lista = getGestos(ctx)
                gestosDB = lista.map { GestoDBDeletreo(it.id, it.nombre, it.datos) }
            } catch (e: Exception) {
                Log.e("DeletreoActivity", "Error cargando gestos", e)
            }
        }
    }

    // ======== Timer 3.5 segundos ========
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
            }
            timerActivo = false
            progresoBarra = 0f
        }
    }

    // ======== UI ========
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

        // --- Parte superior (imagen + progreso) ---
        Column(
            Modifier.weight(0.6f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (imagenUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(imagenUrl),
                    contentDescription = "Imagen deletreo",
                    modifier = Modifier.size(200.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Progreso: $progreso", style = MaterialTheme.typography.headlineSmall)
            Text("Gesto detectado: $gestoDetectado")

            if (timerActivo) {
                LinearProgressIndicator(
                    progress = { progresoBarra },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                        .padding(top = 8.dp),
                    color = Color.Red
                )
            }

            Button(
                onClick = { if (!timerActivo) timerActivo = true },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Seleccionar letra")
            }

            Spacer(Modifier.height(12.dp))
            if (mensaje.isNotBlank()) {
                Text(mensaje, color = Color.Blue)
                if (mensaje.contains("Correcto")) {
                    Button(onClick = { cargarNuevaImagen() }) { Text("Siguiente palabra") }
                } else {
                    Button(onClick = { cargarNuevaImagen() }) { Text("Nueva palabra") }
                }
            }
        }

        // --- Parte inferior (cámara) ---
        Box(Modifier.weight(0.4f).fillMaxWidth()) {
            CameraWithHandsDeletreo(
                landmarkerProvider = landmarkerProvider,
                onDisposeLandmarker = onDisposeLandmarker,
                onVectorDetectado = { vector ->
                    val match = gestosDB.minByOrNull { euclideanDistanceDeletreo(vector, it.datos) }
                    gestoDetectado =
                        if (match != null && euclideanDistanceDeletreo(vector, match.datos) < 10f)
                            match.nombre else ""
                }
            )
        }
    }

    // ======== Confirmación de letra ========
    if (mostrarConfirmacion && letraPendiente != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false; letraPendiente = null },
            title = { Text("Confirmar letra") },
            text = { Text("¿Quieres usar la letra '${letraPendiente}'?") },
            confirmButton = {
                TextButton(onClick = {
                    procesarIntento(letraPendiente!!)
                    mostrarConfirmacion = false
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
fun CameraWithHandsDeletreo(
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
                        val bitmap = imageProxy.toBitmapDeletreo()
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        val opts = ImageProcessingOptions.builder()
                            .setRotationDegrees(imageProxy.imageInfo.rotationDegrees)
                            .build()
                        val ts = SystemClock.elapsedRealtime()
                        detector?.detectAsync(mpImage, opts, ts)
                    } catch (t: Throwable) {
                        Log.e("CameraWithHandsDeletreo", "Analyzer error", t)
                    } finally {
                        imageProxy.close()
                    }
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                ctx as ComponentActivity,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                analysis
            )
        } catch (e: Exception) {
            Log.e("CameraWithHandsDeletreo", "Bind camera error", e)
        }

        onDispose {
            cameraProvider.unbindAll()
            onDisposeLandmarker()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

/* ================== Funciones auxiliares ================== */

fun ImageProxy.toBitmapDeletreo(): Bitmap {
    val yBuffer: ByteBuffer = planes[0].buffer
    val uBuffer: ByteBuffer = planes[1].buffer
    val vBuffer: ByteBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
    val bytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

fun euclideanDistanceDeletreo(a: List<Float>, b: List<Float>): Float {
    val n = minOf(a.size, b.size)
    var sum = 0f
    for (i in 0 until n) sum += (a[i] - b[i]) * (a[i] - b[i])
    return sqrt(sum)
}
