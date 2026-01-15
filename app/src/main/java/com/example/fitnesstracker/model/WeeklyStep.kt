package com.example.fitnesstracker.model

data class WeeklyStep(
    val day: String,
    val steps: Int,
    val isHighlight: Boolean = false
)