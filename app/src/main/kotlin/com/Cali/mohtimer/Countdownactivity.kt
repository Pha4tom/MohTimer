package com.Cali.mohtimer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class CountdownActivity : AppCompatActivity() {

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { launch() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countdown)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnSavePreset).setOnClickListener { saveCurrentAsPreset() }

        findViewById<MaterialButton>(R.id.btnStart).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else launch()
        }
    }

    private fun readTotalSeconds(): Int {
        val mins = findViewById<EditText>(R.id.etMinutes).text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 5
        val secs = findViewById<EditText>(R.id.etSeconds).text.toString().toIntOrNull()?.coerceIn(0, 59) ?: 0
        return mins * 60 + secs
    }

    private fun saveCurrentAsPreset() {
        val seconds = readTotalSeconds().coerceAtLeast(1)
        val m = seconds / 60
        val s = seconds % 60
        val label = if (s == 0) "${m}min" else "${m}min ${s}s"
        val defaultName = "Timer $label"

        val input = EditText(this).apply {
            setText(defaultName)
            setHint("Preset name")
            setPadding(40, 40, 40, 40)
        }

        AlertDialog.Builder(this)
            .setTitle("Save as preset")
            .setMessage("Save this configuration to your presets?")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim().ifEmpty { defaultName }
                val db = PresetDatabaseHelper(this)
                db.addPreset(
                    Preset(
                        name = name,
                        mode = TimerMode.TIMER,
                        totalMs = seconds * 1000L
                    )
                )
                Toast.makeText(this, "Saved to presets", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launch() {
        val totalMs = (readTotalSeconds().coerceAtLeast(1)) * 1000L
        startActivity(Intent(this, TimerRunActivity::class.java).apply {
            putExtra("mode", "TIMER")
            putExtra("total_ms", totalMs)
        })
    }
}