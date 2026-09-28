package com.Cali.mohtimer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class EmomActivity : AppCompatActivity() {

    private lateinit var etInterval: EditText
    private lateinit var etTotal: EditText
    private lateinit var tvRoundPreview: TextView

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { launch() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emom)

        etInterval = findViewById(R.id.etInterval)
        etTotal = findViewById(R.id.etTotal)
        tvRoundPreview = findViewById(R.id.tvRoundPreview)

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

        etInterval.setOnFocusChangeListener { _, _ -> updatePreview() }
        etTotal.setOnFocusChangeListener { _, _ -> updatePreview() }
        updatePreview()
    }

    private fun saveCurrentAsPreset() {
        val interval = etInterval.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
        val total = etTotal.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 10
        val rounds = (total / interval).coerceAtLeast(1)
        val defaultName = "EMOM ${interval}m x ${rounds}"

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
                        mode = TimerMode.EMOM,
                        intervalMs = interval * 60_000L,
                        totalMs = total * 60_000L,
                        rounds = rounds
                    )
                )
                Toast.makeText(this, "Saved to presets", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePreview() {
        val i = etInterval.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
        val t = etTotal.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 10
        val rounds = (t / i).coerceAtLeast(1)
        tvRoundPreview.text = "$rounds round${if (rounds == 1) "" else "s"} total"
    }

    private fun launch() {
        val i = etInterval.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
        val t = etTotal.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 10
        val rounds = (t / i).coerceAtLeast(1)
        val intent = Intent(this, TimerRunActivity::class.java).apply {
            putExtra("mode", "EMOM")
            putExtra("interval_ms", i * 60_000L)
            putExtra("total_ms", t * 60_000L)
            putExtra("rounds", rounds)
        }
        startActivity(intent)
    }
}