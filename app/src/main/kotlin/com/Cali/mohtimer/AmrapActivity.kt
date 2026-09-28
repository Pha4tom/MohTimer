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

class AmrapActivity : AppCompatActivity() {

    private lateinit var etMinutes: EditText
    private lateinit var etSeconds: EditText
    private lateinit var tvTotalPreview: TextView

    private var totalSeconds = 10 * 60

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { launch() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_amrap)

        etMinutes = findViewById(R.id.etMinutes)
        etSeconds = findViewById(R.id.etSeconds)
        tvTotalPreview = findViewById(R.id.tvTotalPreview)

        etMinutes.setText("10")
        etSeconds.setText("0")
        updateTotalFromFields()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnSavePreset).setOnClickListener { saveCurrentAsPreset() }

        etMinutes.setOnClickListener {
            val current = readTotalSeconds()
            TimePickerHelper.show(
                context = this,
                title = "Minutes",
                unitSeconds = false,
                initialSeconds = current
            ) { newTotalSeconds ->
                totalSeconds = newTotalSeconds
                etMinutes.setText((newTotalSeconds / 60).toString())
                etSeconds.setText((newTotalSeconds % 60).toString())
                updateTotalPreview()
            }
        }

        etSeconds.setOnClickListener {
            val current = readTotalSeconds()
            TimePickerHelper.show(
                context = this,
                title = "Seconds",
                unitSeconds = true,
                initialSeconds = current
            ) { newTotalSeconds ->
                totalSeconds = newTotalSeconds
                etMinutes.setText((newTotalSeconds / 60).toString())
                etSeconds.setText((newTotalSeconds % 60).toString())
                updateTotalPreview()
            }
        }

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
        totalSeconds = readTotalSeconds().coerceAtLeast(1)
        val label = formatPresetLabel(totalSeconds)
        val defaultName = "AMRAP $label"

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
                        mode = TimerMode.AMRAP,
                        totalMs = totalSeconds * 1000L
                    )
                )
                Toast.makeText(this, "Saved to presets", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatPresetLabel(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (s == 0) "${m}min" else "${m}min ${s}s"
    }

    private fun readTotalSeconds(): Int {
        val m = etMinutes.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
        val s = etSeconds.text.toString().toIntOrNull()?.coerceIn(0, 59) ?: 0
        return m * 60 + s
    }

    private fun updateTotalFromFields() {
        totalSeconds = readTotalSeconds()
        updateTotalPreview()
    }

    private fun updateTotalPreview() {
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        tvTotalPreview.text = String.format("Total: %02d:%02d", mins, secs)
    }

    private fun launch() {
        totalSeconds = readTotalSeconds().coerceAtLeast(1)
        val intent = Intent(this, TimerRunActivity::class.java).apply {
            putExtra("mode", "AMRAP")
            putExtra("total_ms", totalSeconds * 1000L)
        }
        startActivity(intent)
    }
}