package com.example.fitnesstracker

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
class FitnessViewModel : ViewModel() {
    companion object {
        var instance: FitnessViewModel? = null
    }

    init {
        instance = this
    }

    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount

    private val _goal = MutableStateFlow(10000)
    val goal: StateFlow<Int> = _goal

    private val _activeTime = MutableStateFlow(0)
    val activeTime: StateFlow<Int> = _activeTime

    private val _calories = MutableStateFlow(0)
    val calories: StateFlow<Int> = _calories

    fun onSensorStepChanged(totalSteps: Int) {
        // Use total steps directly
        _stepCount.value = totalSteps

        // Calculate calories and active time
        _calories.value = (_stepCount.value * 0.04f).toInt()
        _activeTime.value = _stepCount.value / 100
    }

    fun resetData() {
        _stepCount.value = 0
        _calories.value = 0
        _activeTime.value = 0
    }
}
