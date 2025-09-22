package com.example.seitasv2

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.sqrt

// ---------------- Utils ----------------
private fun distance(v1: List<Float>, v2: List<Float>): Float {
    val n = minOf(v1.size, v2.size)
    var sum = 0f
    for (i in 0 until n) {
        val d = v1[i] - v2[i]
        sum += d * d
    }
    return sqrt(sum)
}

fun normalize(points: List<Float>): List<Float> {
    if (points.size < 63) return points
    val baseX = points[0]
    val baseY = points[1]
    val baseZ = points[2]

    val rel = points.chunked(3).map {
        listOf(it[0] - baseX, it[1] - baseY, it[2] - baseZ)
    }.flatten()

    // distancia muñeca–dedo medio (landmark 9)
    val dx = rel[27]; val dy = rel[28]; val dz = rel[29]
    val scale = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-6f)

    return rel.map { it / scale }
}

// ---------------- Dataset ----------------
data class StoredGesture(
    val nombre: String,
    val vector: List<Float> // vector representativo
)

object GestureRepository {
    private val gestures = mutableListOf<StoredGesture>()

    fun getGestures(): List<StoredGesture> = gestures

    suspend fun loadGestures(context: Context) {
        val url = "$BASE_URL/gestos"
        try {
            val res = httpGet(context, url)
            val arr = JSONArray(res)
            gestures.clear()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val nombre = obj.getString("nombre")
                val datos = obj.get("datos")

                val vector = mutableListOf<Float>()

                when (datos) {
                    is JSONArray -> {
                        if (datos.length() > 0 && datos[0] is JSONArray) {
                            // Caso: lista de listas [[...], [...]]
                            val frames = mutableListOf<List<Float>>()
                            for (j in 0 until datos.length()) {
                                val inner = datos.getJSONArray(j)
                                val frame = mutableListOf<Float>()
                                for (k in 0 until inner.length()) {
                                    frame.add(inner.getDouble(k).toFloat())
                                }
                                frames.add(frame)
                            }
                            // Hacemos un promedio para quedarnos con 1 solo vector
                            val size = frames[0].size
                            for (k in 0 until size) {
                                var sum = 0f
                                for (f in frames) sum += f[k]
                                vector.add(sum / frames.size)
                            }
                        } else {
                            // Caso: vector plano [0.35, 0.28, ...]
                            for (j in 0 until datos.length()) {
                                vector.add(datos.getDouble(j).toFloat())
                            }
                        }
                    }
                }

                gestures.add(StoredGesture(nombre, vector))
            }

            Log.d("GestureRepository", "✅ Gestos cargados: ${gestures.size}")
        } catch (e: Exception) {
            Log.e("GestureRepository", "❌ Error cargando gestos", e)
        }
    }
}

// ---------------- Clasificador ----------------
class GestureClassifier(
    private val windowSize: Int = 5,
    private val threshold: Float = 0.5f
) {
    private val history: ArrayDeque<String> = ArrayDeque()
    private var lastStable = ""

    fun classify(hand: List<NormalizedLandmark>): String {
        if (hand.size != 21) return ""
        val pts = hand.flatMap { listOf(it.x(), it.y(), it.z()) }
        val normPts = normalize(pts)

        val candidates = GestureRepository.getGestures()
        if (candidates.isEmpty()) return ""

        var bestLabel = ""
        var bestDist = Float.MAX_VALUE

        for (g in candidates) {
            val d = distance(normPts, normalize(g.vector))
            if (d < bestDist) {
                bestDist = d
                bestLabel = g.nombre
            }
        }

        if (bestDist > threshold) return ""

        history.addLast(bestLabel)
        if (history.size > windowSize) history.removeFirst()

        val most = history.filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: ""
        val freq = if (history.isNotEmpty()) history.count { it == most }.toFloat() / history.size else 0f
        if (most.isNotEmpty() && freq >= 0.6f) lastStable = most

        return lastStable
    }
}
