package com.example.asciiwall

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AsciiWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = AsciiEngine()

    inner class AsciiEngine : Engine(), Choreographer.FrameCallback {
        private val prefs = Cfg.p(this@AsciiWallpaperService)
        private val choreographer = Choreographer.getInstance()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE }
        private val glyphs = ".,:;+*#%@".map { it.toString() }

        private var visible = false
        private var last = 0L
        private var angle = 0f
        private var appliedFps = 0
        private var depth = FloatArray(0)
        private var chr = ByteArray(0)
        private var curPal = 0
        private var colors = Palettes.colors(0)

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            if (v) {
                last = 0L
                choreographer.postFrameCallback(this)
            } else {
                choreographer.removeFrameCallback(this)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            choreographer.removeFrameCallback(this)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            choreographer.removeFrameCallback(this)
            super.onDestroy()
        }

        private fun applyFps(fps: Int) {
            if (fps == appliedFps || Build.VERSION.SDK_INT < 30) return
            try {
                surfaceHolder.surface.setFrameRate(fps.toFloat(), Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
                appliedFps = fps
            } catch (_: Exception) {}
        }

        override fun doFrame(t: Long) {
            if (!visible) return
            val fps = prefs.getInt("fps", 120)
            applyFps(fps)
            val first = last == 0L
            val dt = if (first) 0f else (t - last) / 1e9f
            if (first || dt * 1000f >= 1000f / fps - 3f) {
                angle += prefs.getInt("speed", 10) / 10f * dt
                last = t
                draw()
            }
            choreographer.postFrameCallback(this)
        }

        private fun draw() {
            val c: Canvas = try { surfaceHolder.lockCanvas() } catch (e: Exception) { null } ?: return
            try { render(c) } finally { surfaceHolder.unlockCanvasAndPost(c) }
        }

        private fun render(c: Canvas) {
            val w = c.width.toFloat()
            val h = c.height.toFloat()
            val cols = prefs.getInt("cols", 96).coerceIn(40, 200)
            val cw = w / cols
            val fs = cw / .6f
            val rows = (h / fs).toInt()
            val n = cols * rows
            if (depth.size != n) { depth = FloatArray(n); chr = ByteArray(n) }
            java.util.Arrays.fill(depth, -9f)
            java.util.Arrays.fill(chr, -1)

            val palIdx = prefs.getInt("pal", 0)
            if (palIdx != curPal) { curPal = palIdx; colors = Palettes.colors(palIdx) }

            val k = min(w, h * .62f) * .3f
            for (g in Shapes.get(prefs.getInt("shape", 0).coerceIn(0, 4))) {
                val ay = if (g.fixed) 0f else angle * g.sy
                val ax = (if (g.fixed) 0f else angle * g.sx) + g.tx
                val cy = cos(ay); val sy = sin(ay)
                val cx = cos(ax); val sx = sin(ax)
                val p = g.pts
                var i = 0
                while (i < p.size) {
                    val qx = p[i]; val qy = p[i + 1]; val qz = p[i + 2]
                    i += 3
                    var x = qx * cy + qz * sy
                    var z = -qx * sy + qz * cy
                    var y = qy
                    val y2 = y * cx - z * sx
                    val z2 = y * sx + z * cx
                    y = y2; z = z2
                    x += g.ox
                    val s = 4f / (4f - z * .6f)
                    val px = w / 2 + x * k * s
                    val py = h / 2 + y * k * s
                    if (px < 0f || py < 0f) continue
                    val col = (px / cw).toInt()
                    val row = (py / fs).toInt()
                    if (col >= cols || row >= rows) continue
                    val idx = row * cols + col
                    if (z > depth[idx]) {
                        depth[idx] = z
                        val b = (.5f + z * .32f).coerceIn(0f, 1f)
                        chr[idx] = min(8, (b * 9).toInt()).toByte()
                    }
                }
            }

            c.drawColor(Color.BLACK)
            paint.textSize = fs
            val base = -paint.ascent()
            for (r in 0 until rows) for (col in 0 until cols) {
                val v = chr[r * cols + col].toInt()
                if (v < 0) continue
                paint.color = colors[v]
                c.drawText(glyphs[v], col * cw, r * fs + base, paint)
            }
        }
    }
}
