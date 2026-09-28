package com.Cali.mohtimer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PresetsActivity : AppCompatActivity() {

    private lateinit var db: PresetDatabaseHelper
    private val presets = mutableListOf<Preset>()
    private lateinit var adapter: PresetAdapter
    private lateinit var tvEmpty: View
    private lateinit var rv: RecyclerView

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presets)

        db = PresetDatabaseHelper(this)
        tvEmpty = findViewById(R.id.tvEmpty)
        rv = findViewById(R.id.rvPresets)

        adapter = PresetAdapter(presets,
            onStart = { preset -> launchPreset(preset) },
            onLongPress = { preset -> confirmDelete(preset) }
        )

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<FloatingActionButton>(R.id.fabAddPreset).setOnClickListener {
            showCreateDialog()
        }

        setupSwipeToDelete()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        presets.clear()
        presets.addAll(db.getAllPresets())
        adapter.notifyDataSetChanged()
        tvEmpty.visibility = if (presets.isEmpty()) View.VISIBLE else View.GONE
        rv.visibility = if (presets.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun launchPreset(p: Preset) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val intent = Intent(this, TimerRunActivity::class.java).apply {
            putExtra("mode", p.mode.name)
            putExtra("interval_ms", p.intervalMs)
            putExtra("rest_ms", p.restMs)
            putExtra("total_ms", p.totalMs)
            putExtra("rounds", p.rounds)
        }
        startActivity(intent)
    }

    private fun confirmDelete(p: Preset) {
        AlertDialog.Builder(this)
            .setTitle("Delete preset?")
            .setMessage("\"${p.name}\" will be removed.")
            .setPositiveButton("Delete") { _, _ ->
                db.deletePreset(p.id)
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupSwipeToDelete() {
        val bgColor = ContextCompat.getColor(this, R.color.error)
        val swipeBg = ColorDrawable(bgColor)

        val callback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                @Suppress("DEPRECATION")
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val preset = presets[position]

                AlertDialog.Builder(this@PresetsActivity)
                    .setTitle("Delete \"${preset.name}\"?")
                    .setMessage("This preset will be permanently removed.")
                    .setCancelable(false)
                    .setPositiveButton("Delete") { _, _ ->
                        db.deletePreset(preset.id)
                        adapter.removeAt(position)
                        tvEmpty.visibility = if (presets.isEmpty()) View.VISIBLE else View.GONE
                        rv.visibility = if (presets.isEmpty()) View.GONE else View.VISIBLE
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (dX > 0) {
                        swipeBg.setBounds(
                            itemView.left, itemView.top,
                            itemView.left + dX.toInt(), itemView.bottom
                        )
                    } else {
                        swipeBg.setBounds(
                            itemView.right + dX.toInt(), itemView.top,
                            itemView.right, itemView.bottom
                        )
                    }
                    swipeBg.draw(c)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(rv)
    }

    // ---------- CREATE DIALOG ----------

    private fun showCreateDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_save_preset, null)
        val etName = view.findViewById<EditText>(R.id.etPresetName)
        val actMode = view.findViewById<AutoCompleteTextView>(R.id.actMode)

        val groupEmom = view.findViewById<View>(R.id.groupEmom)
        val groupTimer = view.findViewById<View>(R.id.groupTimer)
        val groupTabata = view.findViewById<View>(R.id.groupTabata)

        val modes = arrayOf("EMOM", "TIMER", "AMRAP", "FOR TIME", "TABATA")
        actMode.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, modes)
        )
        actMode.setText("EMOM", false)

        fun applyVisibility(mode: String) {
            groupEmom.visibility = if (mode == "EMOM") View.VISIBLE else View.GONE
            groupTimer.visibility =
                if (mode == "TIMER" || mode == "AMRAP" || mode == "FOR TIME") View.VISIBLE else View.GONE
            groupTabata.visibility = if (mode == "TABATA") View.VISIBLE else View.GONE
        }

        applyVisibility("EMOM")
        actMode.setOnItemClickListener { parent, view, position, id ->
            applyVisibility(actMode.text.toString())
        }

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSavePreset)
            .setOnClickListener {
                val name = etName.text.toString().trim().ifEmpty { "Untitled" }
                val modeStr = actMode.text.toString()

                val preset: Preset? = when (modeStr) {
                    "EMOM" -> {
                        val interval = view.findViewById<EditText>(R.id.etEmomInterval)
                            .text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val total = view.findViewById<EditText>(R.id.etEmomTotal)
                            .text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 4
                        val rounds = (total / interval).coerceAtLeast(1)
                        Preset(
                            name = name,
                            mode = TimerMode.EMOM,
                            intervalMs = interval * 60_000L,
                            totalMs = total * 60_000L,
                            rounds = rounds
                        )
                    }
                    "TIMER", "AMRAP", "FOR TIME" -> {
                        val min = view.findViewById<EditText>(R.id.etTimerMin)
                            .text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 4
                        val sec = view.findViewById<EditText>(R.id.etTimerSec)
                            .text.toString().toIntOrNull()?.coerceIn(0, 59) ?: 0
                        val totalMs = (min * 60_000L + sec * 1000L).coerceAtLeast(1000L)
                        val m = when (modeStr) {
                            "TIMER" -> TimerMode.TIMER
                            "AMRAP" -> TimerMode.AMRAP
                            else -> TimerMode.FOR_TIME
                        }
                        Preset(name = name, mode = m, totalMs = totalMs)
                    }
                    "TABATA" -> {
                        val work = view.findViewById<EditText>(R.id.etTabataWork)
                            .text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 20
                        val rest = view.findViewById<EditText>(R.id.etTabataRest)
                            .text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 10
                        val rounds = view.findViewById<EditText>(R.id.etTabataRounds)
                            .text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 8
                        Preset(
                            name = name,
                            mode = TimerMode.TABATA,
                            intervalMs = work * 1000L,
                            restMs = rest * 1000L,
                            rounds = rounds
                        )
                    }
                    else -> null
                }

                if (preset != null) {
                    db.addPreset(preset)
                    refresh()
                    dialog.dismiss()
                    Toast.makeText(this, "Preset saved", Toast.LENGTH_SHORT).show()
                }
            }

        dialog.show()
    }
}