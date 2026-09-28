package com.Cali.mohtimer

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PresetDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "SmartTimerPresets.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            "CREATE TABLE presets (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT, " +
                    "mode TEXT, " +
                    "interval_ms INTEGER, " +
                    "rest_ms INTEGER, " +
                    "total_ms INTEGER, " +
                    "rounds INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS presets")
        onCreate(db)
    }

    fun addPreset(p: Preset): Long {
        val db = writableDatabase
        val v = ContentValues().apply {
            put("name", p.name)
            put("mode", p.mode.name)
            put("interval_ms", p.intervalMs)
            put("rest_ms", p.restMs)
            put("total_ms", p.totalMs)
            put("rounds", p.rounds)
        }
        val id = db.insert("presets", null, v)
        db.close()
        return id
    }

    fun getAllPresets(): MutableList<Preset> {
        val list = mutableListOf<Preset>()
        val db = readableDatabase
        val c = db.rawQuery("SELECT * FROM presets ORDER BY id DESC", null)
        if (c.moveToFirst()) {
            do {
                list.add(
                    Preset(
                        id = c.getLong(0),
                        name = c.getString(1),
                        mode = try {
                            TimerMode.valueOf(c.getString(2))
                        } catch (_: Exception) { TimerMode.EMOM },
                        intervalMs = c.getLong(3),
                        restMs = c.getLong(4),
                        totalMs = c.getLong(5),
                        rounds = c.getInt(6)
                    )
                )
            } while (c.moveToNext())
        }
        c.close()
        db.close()
        return list
    }

    fun deletePreset(id: Long) {
        val db = writableDatabase
        db.delete("presets", "id=?", arrayOf(id.toString()))
        db.close()
    }
}