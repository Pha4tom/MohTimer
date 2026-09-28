package com.Cali.mohtimer

data class Preset(
    val id: Long = 0,
    val name: String,
    val mode: TimerMode,
    val intervalMs: Long = 60_000L,
    val restMs: Long = 10_000L,
    val totalMs: Long = 600_000L,
    val rounds: Int = 10
)