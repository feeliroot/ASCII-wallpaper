package com.example.asciiwall

import android.content.Context
import android.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

object Cfg {
    fun p(c: Context) = c.getSharedPreferences("ascii", Context.MODE_PRIVATE)!!
}

class Group(
    val pts: FloatArray,
    val sy: Float,      // множитель вращения вокруг Y
    val sx: Float,      // множитель вращения вокруг X
    val tx: Float,      // постоянный наклон по X
    val ox: Float = 0f, // смещение по X после вращения
    val fixed: Boolean = false
)

private class P {
    val l = ArrayList<Float>(4096)
    fun add(x: Float, y: Float, z: Float) { l.add(x); l.add(y); l.add(z) }
    fun arr() = l.toFloatArray()
}

object Palettes {
    val names = listOf("Зелёный", "Циан", "Янтарь", "Розовый", "Белый", "Радуга")
    private val hs = listOf(140f to .8f, 185f to .85f, 38f to .95f, 320f to .85f, 0f to 0f)

    fun colors(i: Int): IntArray = IntArray(9) { k ->
        if (i < hs.size)
            Color.HSVToColor(floatArrayOf(hs[i].first, hs[i].second, (.25f + k * .09f).coerceAtMost(1f)))
        else
            Color.HSVToColor(floatArrayOf(k * 42f, .9f, (.55f + k * .05f).coerceAtMost(1f)))
    }
}

object Shapes {
    val names = listOf("Макароны", "Кубы", "Большой куб", "ДНК", "Чёрная дыра")
    private val TAU = (2 * PI).toFloat()
    private val cache = arrayOfNulls<List<Group>>(5)

    fun get(i: Int): List<Group> = cache[i] ?: build(i).also { cache[i] = it }

    private fun cube(s: Float, n: Int): FloatArray {
        val p = P()
        for (a in intArrayOf(-1, 1)) for (b in intArrayOf(-1, 1)) for (i in 0..n) {
            val t = (i.toFloat() / n * 2 - 1) * s
            p.add(t, a * s, b * s); p.add(a * s, t, b * s); p.add(a * s, b * s, t)
        }
        return p.arr()
    }

    private fun ribbon(r0: Float, r1: Float, h: Float, turns: Float, n: Int, w: Int): FloatArray {
        val p = P()
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val a = t * turns * TAU
            for (j in 0..w) {
                val r = r0 + (r1 - r0) * j / w
                p.add(r * cos(a), (t - .5f) * h, r * sin(a))
            }
        }
        return p.arr()
    }

    private fun dna(): FloatArray {
        val p = P(); val n = 520; val hh = 2.6f; val rr = .6f
        val pi = PI.toFloat()
        for (i in 0 until n) {
            val t = i.toFloat() / n; val a = t * 3 * TAU; val y = (t - .5f) * hh
            p.add(rr * cos(a), y, rr * sin(a))
            p.add(rr * cos(a + pi), y, rr * sin(a + pi))
        }
        for (k in 0 until 26) {
            val t = k / 26f; val a = t * 3 * TAU; val y = (t - .5f) * hh
            for (j in 0..10) {
                val u = j / 10f * 2 - 1
                p.add(rr * u * cos(a), y, rr * u * sin(a))
            }
        }
        return p.arr()
    }

    private fun disk(): FloatArray {
        val p = P(); val rnd = Random(7)
        repeat(2600) {
            val r = .85f + rnd.nextFloat().pow(1.5f) * 1.25f
            val a = rnd.nextFloat() * TAU
            p.add(r * cos(a), (rnd.nextFloat() - .5f) * .04f, r * sin(a))
        }
        return p.arr()
    }

    private fun ring(r: Float, n: Int): FloatArray {
        val p = P()
        for (i in 0 until n) { val a = i.toFloat() / n * TAU; p.add(r * cos(a), r * sin(a), 0f) }
        return p.arr()
    }

    private fun build(i: Int): List<Group> = when (i) {
        0 -> {
            val a = ribbon(.08f, .38f, 1.9f, 4f, 420, 3)
            listOf(Group(a, 1f, 0f, .3f, -.62f), Group(a, -1f, 0f, -.3f, .62f))
        }
        1 -> listOf(Group(cube(.95f, 50), 1f, .6f, .2f), Group(cube(.45f, 30), -1.7f, -1f, .4f))
        2 -> listOf(Group(cube(1.25f, 70), 1f, .55f, .25f))
        3 -> listOf(Group(dna(), 1f, 0f, .25f))
        else -> listOf(
            Group(disk(), .8f, 0f, 1.2f),
            Group(ring(.8f, 260), 0f, 0f, 0f, 0f, true),
            Group(ring(1f, 200), 0f, 0f, 0f, 0f, true)
        )
    }
}
