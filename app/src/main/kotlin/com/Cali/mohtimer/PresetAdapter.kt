package com.Cali.mohtimer

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class PresetAdapter(
    private val presets: MutableList<Preset>,
    private val onStart: (Preset) -> Unit,
    private val onLongPress: (Preset) -> Unit
) : RecyclerView.Adapter<PresetAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val accent: View = v.findViewById(R.id.vAccent)
        val name: TextView = v.findViewById(R.id.tvPresetName)
        val summary: TextView = v.findViewById(R.id.tvPresetSummary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preset, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = presets[position]
        h.name.text = p.name

        val accentRes = when (p.mode) {
            TimerMode.EMOM -> R.color.emom_purple
            TimerMode.AMRAP -> R.color.amrap_orange
            TimerMode.FOR_TIME -> R.color.fortime_blue
            TimerMode.TABATA -> R.color.tabata_green
            TimerMode.TIMER -> R.color.fortime_blue
        }
        h.accent.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(h.itemView.context, accentRes)
        )

        h.summary.text = when (p.mode) {
            TimerMode.EMOM -> {
                val intervalMin = (p.intervalMs / 60_000L).coerceAtLeast(1)
                val rounds = (p.totalMs / p.intervalMs).coerceAtLeast(1)
                "EMOM • ${intervalMin}m × $rounds rounds"
            }
            TimerMode.TIMER -> "Timer • ${formatShort(p.totalMs)}"
            TimerMode.AMRAP -> "AMRAP • ${formatShort(p.totalMs)}"
            TimerMode.FOR_TIME -> "For Time"
            TimerMode.TABATA -> "Tabata • ${p.rounds} rounds (${p.intervalMs/1000}s/${p.restMs/1000}s)"
        }

        h.itemView.setOnClickListener { onStart(p) }
        h.itemView.setOnLongClickListener {
            onLongPress(p)
            true
        }
    }

    fun removeAt(position: Int): Preset {
        val removed = presets.removeAt(position)
        notifyItemRemoved(position)
        return removed
    }

    fun restoreAt(position: Int, item: Preset) {
        presets.add(position, item)
        notifyItemInserted(position)
    }

    private fun formatShort(ms: Long): String {
        val s = (ms / 1000).toInt()
        val m = s / 60
        val sec = s % 60
        return if (sec == 0) "${m}m" else "${m}m ${sec}s"
    }

    override fun getItemCount() = presets.size
}