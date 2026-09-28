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

class TabataActivity : AppCompatActivity() {

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { launch() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabata)

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

    private fun saveCurrentAsPreset() {
        val rounds = findViewById<EditText>(R.id.etRounds).text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 8
        val workSec = findViewById<EditText>(R.id.etWork).text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 20
        val restSec = findViewById<EditText>(R.id.etRest).text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 10
        val defaultName = "Tabata ${rounds}x ${workSec}s/${restSec}s"

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
                        mode = TimerMode.TABATA,
                        intervalMs = workSec * 1000L,
                        restMs = restSec * 1000L,
                        rounds = rounds
                    )
                )
                Toast.makeText(this, "Saved to presets", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launch() {
        val rounds = findViewById<EditText>(R.id.etRounds).text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 8
        val workSec = findViewById<EditText>(R.id.etWork).text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 20
        val restSec = findViewById<EditText>(R.id.etRest).text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 10

        startActivity(Intent(this, TimerRunActivity::class.java).apply {
            putExtra("mode", "TABATA")
            putExtra("interval_ms", workSec * 1000L)
            putExtra("rest_ms", restSec * 1000L)
            putExtra("rounds", rounds)
        })
    }
}