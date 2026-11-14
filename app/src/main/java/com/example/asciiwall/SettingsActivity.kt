package com.example.asciiwall

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

class SettingsActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private val acc = 0xFF39FF88.toInt()
    private val fg = 0xFFCFE8D6.toInt()
    private val line = 0xFF1F3326.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Cfg.p(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(44), dp(16), dp(28))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFF070B08.toInt())
            addView(root)
        }
        setContentView(scroll)

        text(root, "ASCII // LIVE WALLPAPER", 18f, acc, bold = true)

        header(root, "01 / ФОРМА")
        choice(root, Shapes.names, "shape")

        header(root, "02 / ЦВЕТ")
        choice(root, Palettes.names, "pal")

        header(root, "03 / РАЗРЕШЕНИЕ")
        slider(root, "Плотность символов", "cols", 40, 200, 96) { "$it" }

        header(root, "04 / ДВИЖЕНИЕ")
        slider(root, "Скорость вращения", "speed", 0, 30, 10) { "%.1f×".format(it / 10f) }
        text(root, "Частота кадров", 14f, fg)
        choice(root, listOf("30", "45", "60", "90", "120"), "fps", listOf(30, 45, 60, 90, 120), 120)

        val set = Button(this).apply {
            text = "Установить как обои"
            isAllCaps = false
            style(this, true)
            setOnClickListener {
                val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@SettingsActivity, AsciiWallpaperService::class.java)
                )
                try { startActivity(i) } catch (e: Exception) {
                    startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                }
            }
        }
        root.addView(set, lp().apply { topMargin = dp(24) })
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun lp() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun text(root: LinearLayout, s: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
            typeface = if (bold) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) else Typeface.MONOSPACE
        }
        root.addView(t, lp())
        return t
    }

    private fun header(root: LinearLayout, s: String) {
        text(root, s, 12f, acc, bold = true).setPadding(0, dp(20), 0, dp(6))
    }

    private fun style(b: Button, on: Boolean) {
        b.background = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setStroke(dp(1), if (on) acc else line)
            setColor(Color.TRANSPARENT)
        }
        b.setTextColor(if (on) acc else fg)
    }

    private fun choice(
        root: LinearLayout, labels: List<String>, key: String,
        values: List<Int> = labels.indices.toList(), def: Int = 0
    ) {
        val buttons = ArrayList<Pair<Button, Int>>()
        fun refresh() {
            val cur = prefs.getInt(key, def)
            buttons.forEach { (b, v) -> style(b, v == cur) }
        }
        labels.chunked(3).forEachIndexed { rowIdx, chunk ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            chunk.forEachIndexed { j, label ->
                val v = values[rowIdx * 3 + j]
                val b = Button(this).apply {
                    text = label; isAllCaps = false; gravity = Gravity.CENTER
                    setOnClickListener { prefs.edit().putInt(key, v).apply(); refresh() }
                }
                buttons.add(b to v)
                row.addView(b, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
            }
            root.addView(row, lp())
        }
        refresh()
    }

    private fun slider(
        root: LinearLayout, label: String, key: String, min: Int, max: Int, def: Int,
        fmt: (Int) -> String
    ) {
        val value = prefs.getInt(key, def)
        val t = text(root, "$label: ${fmt(value)}", 14f, fg)
        val sb = SeekBar(this).apply {
            this.min = min; this.max = max; progress = value
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                    t.text = "$label: ${fmt(p)}"
                    if (fromUser) prefs.edit().putInt(key, p).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            })
        }
        root.addView(sb, lp())
    }
}
