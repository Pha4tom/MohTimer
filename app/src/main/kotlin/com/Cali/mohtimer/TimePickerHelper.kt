package com.Cali.mohtimer

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

object TimePickerHelper {

    fun show(
        context: Context,
        title: String,
        unitSeconds: Boolean,
        initialSeconds: Int,
        onSelected: (Int) -> Unit
    ) {
        val presets: List<Int> = if (unitSeconds) {
            // Fine-grained second presets: 0, 5, 10, ... 55
            listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
        } else {
            // Minute presets: every minute up to 10, then bigger steps
            listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30, 40, 45, 60)
        }

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_picker_presets)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.85f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvTitle = dialog.findViewById<TextView>(R.id.tvPickerTitle)
        val llPresets = dialog.findViewById<LinearLayout>(R.id.llPresets)
        val tvSetCustom = dialog.findViewById<TextView>(R.id.tvSetCustom)
        val btnOk = dialog.findViewById<MaterialButton>(R.id.btnPickerOk)

        tvTitle.text = title

        val accent = ContextCompat.getColor(context, R.color.text_primary)
        val dim = ContextCompat.getColor(context, R.color.text_secondary)

        var selectedPresetSeconds = initialSeconds

        fun highlightRows() {
            for (i in 0 until llPresets.childCount) {
                val row = llPresets.getChildAt(i) as TextView
                val value = row.tag as Int
                val isSelected = value == selectedPresetSeconds
                row.setTextColor(if (isSelected) accent else dim)
            }
        }

        for (value in presets) {
            val row = LayoutInflater.from(context)
                .inflate(R.layout.item_picker_row, llPresets, false) as TextView

            val seconds = if (unitSeconds) value else value * 60
            row.tag = seconds
            row.text = formatLabel(seconds, unitSeconds)

            row.setOnClickListener {
                selectedPresetSeconds = seconds
                highlightRows()
                onSelected(seconds)
                dialog.dismiss()
            }

            llPresets.addView(row)
        }

        highlightRows()

        tvSetCustom.setOnClickListener {
            dialog.dismiss()
            showCustom(context, initialSeconds, onSelected)
        }

        btnOk.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showCustom(
        context: Context,
        initialSeconds: Int,
        onSelected: (Int) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom_time)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.85f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etMin = dialog.findViewById<EditText>(R.id.etCustomMin)
        val etSec = dialog.findViewById<EditText>(R.id.etCustomSec)
        val btnOk = dialog.findViewById<MaterialButton>(R.id.btnCustomOk)

        etMin.setText((initialSeconds / 60).toString())
        etSec.setText((initialSeconds % 60).toString())

        btnOk.setOnClickListener {
            val m = etMin.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
            val s = etSec.text.toString().toIntOrNull()?.coerceIn(0, 59) ?: 0
            onSelected(m * 60 + s)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun formatLabel(seconds: Int, unitIsSeconds: Boolean): String {
        return if (unitIsSeconds) {
            if (seconds == 0) "0 seconds"
            else if (seconds == 1) "1 second"
            else "$seconds seconds"
        } else {
            val mins = seconds / 60
            if (mins == 1) "1 minute" else "$mins minutes"
        }
    }
}