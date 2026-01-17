package com.example.fitnesstracker

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.fitnesstracker.model.WeeklyStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class FitnessViewModel(application: Application) : AndroidViewModel(application) {

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

    private val _last7Days = MutableStateFlow(listOf(0,0,0,0,0,0,0))
    val last7Days: StateFlow<List<Int>> = _last7Days

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    fun onSensorStepChanged(totalSteps: Float, context: Context) {
        val currentSteps = (totalSteps - previousTotalSteps).toInt()
        _stepCount.value = currentSteps
        _calories.value = (currentSteps * 0.04f).toInt()
        _activeTime.value = currentSteps / 100
        prefs.edit().putFloat("total_steps", totalSteps).apply()
        saveTodaySteps(context, currentSteps)
    }

    fun toggleTheme() {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue
        prefs.edit().putBoolean("is_dark_theme", newValue).apply()
    }

    fun loadTodaySteps(context: Context) {
        val steps = context.getSharedPreferences("fitness_data", Context.MODE_PRIVATE)
            .getInt("today_steps", 0)
        _stepCount.value = steps
    }

    fun saveTodaySteps(context: Context, steps: Int) {
        context.getSharedPreferences("fitness_data", Context.MODE_PRIVATE)
            .edit().putInt("today_steps", steps).apply()
    }

    fun loadLast7Days(context: Context) {
        val last7 = context.getSharedPreferences("fitness_data", Context.MODE_PRIVATE)
            .let { prefs -> (0..6).map { i -> prefs.getInt("steps_day_$i", 0) } }
        _last7Days.value = last7
    }

    fun saveLast7Days(context: Context, stepsList: List<Int>) {
        val prefs = context.getSharedPreferences("fitness_data", Context.MODE_PRIVATE)
        stepsList.forEachIndexed { i, value ->
            prefs.edit().putInt("steps_day_$i", value).apply()
        }
    }
    private val _weeklySteps = MutableStateFlow<List<WeeklyStep>>(emptyList())
    val weeklySteps: StateFlow<List<WeeklyStep>> = _weeklySteps

    fun addTodaySteps(context: Context, todaySteps: Int) {
        val prefs = context.getSharedPreferences("fitness_data", Context.MODE_PRIVATE)

        // Load current last7Days
        val last7Days = (0..6).map { i -> prefs.getInt("steps_day_$i", 0) }.toMutableList()

        // Shift older days
        last7Days.removeAt(0) // remove oldest
        last7Days.add(todaySteps) // add today at the end

        // Save back
        last7Days.forEachIndexed { i, steps ->
            prefs.edit().putInt("steps_day_$i", steps).apply()
        }

        // Highlight today
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val highlightIndex = when(dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val dayLabels = listOf("M","T","W","T","F","S","S")

        val weeklyStepsList = last7Days.mapIndexed { index, steps ->
            WeeklyStep(
                day = dayLabels[index],
                steps = steps,
                isHighlight = index == highlightIndex
            )
        }

        _weeklySteps.value = weeklyStepsList
    }
}
