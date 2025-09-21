package com.example.seitasv2

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.*

// ---------------- Utils ----------------
data class Point2(val x: Float, val y: Float)

private fun dist(a: Point2, b: Point2): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun angle(a: Point2, b: Point2, c: Point2): Float {
    val v1x = a.x - b.x; val v1y = a.y - b.y
    val v2x = c.x - b.x; val v2y = c.y - b.y
    val dot = v1x * v2x + v1y * v2y
    val m1 = sqrt(v1x * v1x + v1y * v1y)
    val m2 = sqrt(v2x * v2x + v2y * v2y)
    if (m1 == 0f || m2 == 0f) return 180f
    val cosang = (dot / (m1 * m2)).coerceIn(-1f, 1f)
    return (acos(cosang) * 180f / Math.PI).toFloat()
}

fun toPoints2(hand: List<NormalizedLandmark>): List<Point2> =
    hand.map { Point2(it.x(), it.y()) }

// --------------- Finger state ---------------
data class FingerState(
    val thumb: Boolean,
    val index: Boolean,
    val middle: Boolean,
    val ring: Boolean,
    val pinky: Boolean
)

class FingerAnalyzer {
    fun analyze(hand: List<Point2>): FingerState {
        val wrist = hand[0]
        fun isStraight(mcp: Int, tip: Int): Boolean =
            dist(hand[tip], wrist) > dist(hand[mcp], wrist) * 0.9f
        val thumbStraight = dist(hand[4], hand[0]) > dist(hand[3], hand[0]) * 0.9f
        return FingerState(
            thumb = thumbStraight,
            index = isStraight(5, 8),
            middle = isStraight(9, 12),
            ring = isStraight(13, 16),
            pinky = isStraight(17, 20)
        )
    }
}

// --------------- Rules ---------------
data class GestureRule(
    val name: String,
    val match: (FingerState, List<Point2>) -> Boolean
)

class GestureClassifier(
    private val analyzer: FingerAnalyzer = FingerAnalyzer(),
    private val rules: List<GestureRule> = defaultRules()
) {
    private val windowSize = 5
    private val history1: ArrayDeque<String> = ArrayDeque()
    private val history2: ArrayDeque<String> = ArrayDeque()
    private var lastStable1 = ""
    private var lastStable2 = ""

    // Overload: una mano
    fun classify(hand: List<NormalizedLandmark>): String {
        if (hand.size != 21) return ""
        val pts = toPoints2(hand)
        val fs = analyzer.analyze(pts)
        val label = rules.firstOrNull { it.match(fs, pts) }?.name ?: ""
        history1.addLast(label); if (history1.size > windowSize) history1.removeFirst()
        val most = history1.filter { it.isNotEmpty() }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: ""
        val freq = if (history1.isNotEmpty()) history1.count { it == most }.toFloat() / history1.size else 0f
        if (most.isNotEmpty() && freq >= 0.6f) lastStable1 = most
        return lastStable1
    }

    // Overload: dos manos
    fun classify(hands: List<List<NormalizedLandmark>>): List<String> {
        val h1 = hands.getOrNull(0)
        val h2 = hands.getOrNull(1)
        val l1 = if (h1 != null) classify(h1) else ""
        val l2 = if (h2 != null) {
            // usar history2 para la segunda mano
            if (h2.size != 21) "" else run {
                val pts = toPoints2(h2)
                val fs = analyzer.analyze(pts)
                val label = rules.firstOrNull { it.match(fs, pts) }?.name ?: ""
                history2.addLast(label); if (history2.size > windowSize) history2.removeFirst()
                val most = history2.filter { it.isNotEmpty() }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: ""
                val freq = if (history2.isNotEmpty()) history2.count { it == most }.toFloat() / history2.size else 0f
                if (most.isNotEmpty() && freq >= 0.6f) lastStable2 = most
                lastStable2
            }
        } else ""
        return listOf(l1, l2).filter { it.isNotEmpty() }
    }

    companion object {
        fun defaultRules(): List<GestureRule> = listOf(
            // A
            GestureRule("A") { f, pts ->
                !f.index && !f.middle && !f.ring && !f.pinky && !f.thumb &&
                        dist(pts[4], pts[5]) < 0.1f
            },
            // B
            GestureRule("B") { f, _ ->
                !f.thumb && f.index && f.middle && f.ring && f.pinky
            },
            // C
            GestureRule("C") { _, pts ->
                dist(pts[4], pts[8]) > 0.1f && dist(pts[4], pts[12]) > 0.1f && dist(pts[4], pts[16]) > 0.1f
            },
            // D
            GestureRule("D") { f, _ ->
                !f.thumb && f.index && !f.middle && !f.ring && !f.pinky
            },
            // E
            GestureRule("E") { f, pts ->
                !f.index && !f.middle && !f.ring && !f.pinky && !f.thumb &&
                        pts[4].y < pts[8].y
            },
            // F
            GestureRule("F") { f, pts ->
                f.middle && f.ring && f.pinky && !f.thumb && !f.index &&
                        dist(pts[4], pts[8]) < 0.1f
            },
            // G
            GestureRule("G") { f, pts ->
                f.thumb && f.index && !f.middle && !f.ring && !f.pinky &&
                        abs(pts[4].y - pts[8].y) < 0.1f
            },
            // H
            GestureRule("H") { f, pts ->
                !f.thumb && f.index && f.middle && !f.ring && !f.pinky &&
                        dist(pts[8], pts[12]) < 0.05f
            },
            // I
            GestureRule("I") { f, _ ->
                !f.thumb && !f.index && !f.middle && !f.ring && f.pinky
            },
            // J (estático = I)
            GestureRule("J") { f, _ ->
                !f.thumb && !f.index && !f.middle && !f.ring && f.pinky
            },
            // L
            GestureRule("L") { f, _ ->
                f.thumb && f.index && !f.middle && !f.ring && !f.pinky
            },
            // LL (estático = L)
            GestureRule("LL") { f, _ ->
                f.thumb && f.index && !f.middle && !f.ring && !f.pinky
            },
            // M
            GestureRule("M") { f, pts ->
                !f.pinky && !f.thumb && !f.index && !f.middle && !f.ring &&
                        (pts[4].x > pts[8].x && pts[4].x > pts[12].x && pts[4].x > pts[16].x)
            },
            // N
            GestureRule("N") { f, pts ->
                !f.ring && !f.pinky && !f.thumb && !f.index && !f.middle &&
                        (pts[4].x > pts[8].x && pts[4].x > pts[12].x)
            },
            // Ñ (estático = N)
            GestureRule("Ñ") { f, pts ->
                !f.ring && !f.pinky && !f.thumb && !f.index && !f.middle &&
                        (pts[4].x > pts[8].x && pts[4].x > pts[12].x)
            },
            // O
            GestureRule("O") { f, pts ->
                !f.thumb && !f.index && !f.middle && !f.ring && !f.pinky &&
                        dist(pts[4], pts[8]) < 0.1f
            },
            // P
            GestureRule("P") { f, pts ->
                f.index && f.middle && !f.ring && !f.pinky && f.thumb &&
                        dist(pts[4], pts[12]) < 0.1f && pts[8].y > pts[5].y && pts[12].y > pts[9].y
            },
            // Q
            GestureRule("Q") { f, pts ->
                !f.middle && !f.ring && !f.pinky && !f.thumb && !f.index &&
                        pts[4].y > pts[0].y && pts[8].y > pts[0].y &&
                        dist(pts[4], pts[8]) < 0.1f
            },
            // R
            GestureRule("R") { f, pts ->
                !f.thumb && f.index && f.middle && !f.ring && !f.pinky &&
                        (pts[8].x > pts[12].x)
            },
            // RR (estático = R)
            GestureRule("RR") { f, pts ->
                !f.thumb && f.index && f.middle && !f.ring && !f.pinky &&
                        (pts[8].x > pts[12].x)
            },
            // S
            GestureRule("S") { f, pts ->
                !f.index && !f.middle && !f.ring && !f.pinky && !f.thumb &&
                        (pts[4].x > pts[5].x)
            },
            // T
            GestureRule("T") { f, pts ->
                !f.index && !f.middle && !f.ring && !f.pinky && !f.thumb &&
                        (pts[4].x > pts[5].x && pts[4].x < pts[9].x)
            },
            // U
            GestureRule("U") { f, pts ->
                !f.thumb && f.index && f.middle && !f.ring && !f.pinky &&
                        dist(pts[8], pts[12]) < 0.05f
            },
            // V
            GestureRule("V") { f, pts ->
                !f.thumb && f.index && f.middle && !f.ring && !f.pinky &&
                        dist(pts[8], pts[12]) > 0.1f
            },
            // W
            GestureRule("W") { f, pts ->
                !f.thumb && f.index && f.middle && f.ring && !f.pinky &&
                        dist(pts[8], pts[12]) > 0.1f && dist(pts[12], pts[16]) > 0.1f
            },
            // X (índice en gancho)
            GestureRule("X") { f, pts ->
                !f.thumb && !f.index && !f.middle && !f.ring && !f.pinky &&
                        angle(pts[5], pts[6], pts[7]) < 90f
            },
            // Y
            GestureRule("Y") { f, _ ->
                f.thumb && !f.index && !f.middle && !f.ring && f.pinky
            },
            // Z (estático = D)
            GestureRule("Z") { f, _ ->
                !f.thumb && f.index && !f.middle && !f.ring && !f.pinky
            }
        )
    }
}
