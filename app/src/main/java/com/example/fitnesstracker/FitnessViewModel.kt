package com.example.fitnesstracker

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class FitnessViewModel(application: Application) :  AndroidViewModel(application) {
    companion object {
        var instance: FitnessViewModel? = null
    }

    init {
        instance = this
    }
    private val prefs = application.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
    private var previousTotalSteps = prefs.getFloat("previous_steps", 0f)
    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount

    private val _goal = MutableStateFlow(10000)
    val goal: StateFlow<Int> = _goal

    private val _activeTime = MutableStateFlow(0)
    val activeTime: StateFlow<Int> = _activeTime

    private val _calories = MutableStateFlow(0)
    val calories: StateFlow<Int> = _calories

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    fun onSensorStepChanged(totalSteps: Float) {
        val currentSteps = (totalSteps - previousTotalSteps).toInt()
        _stepCount.value = currentSteps
        // Use total steps directly

        // Calculate calories and active time
        _calories.value = (_stepCount.value * 0.04f).toInt()
        _activeTime.value = _stepCount.value / 100

        prefs.edit().putFloat("total_steps", totalSteps).apply()
    }
    fun toggleTheme() {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue
        prefs.edit().putBoolean("is_dark_theme", newValue).apply()
    }

    fun resetData(totalSteps: Float) {
        previousTotalSteps = totalSteps
        prefs.edit().putFloat("previous_steps", previousTotalSteps).apply()
        _stepCount.value = 0
        _calories.value = 0
        _activeTime.value = 0

    }
}
