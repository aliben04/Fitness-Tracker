package com.example.fitnesstracker


import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FitnessViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        var instance: FitnessViewModel? = null
    }

    private val prefs = application.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)

    // Baselines
    private var previousTotalSteps = prefs.getFloat("previous_steps", 0f)
    private var activeTimeMs = prefs.getLong("active_time_ms", 0L)

    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount

    private val _goal = MutableStateFlow(10000)
    val goal: StateFlow<Int> = _goal

    private val _activeTime = MutableStateFlow((activeTimeMs / 60000).toInt())
    val activeTime: StateFlow<Int> = _activeTime

    private val _calories = MutableStateFlow(0)
    val calories: StateFlow<Int> = _calories

    private val _distance = MutableStateFlow(0.0f) // in km
    val distance: StateFlow<Float> = _distance

    private val _floors = MutableStateFlow(0)
    val floors: StateFlow<Int> = _floors

    private val _sleepTime = MutableStateFlow("0h 0m")
    val sleepTime: StateFlow<String> = _sleepTime

    private val _weeklySteps = MutableStateFlow(listOf(0, 0, 0, 0, 0, 0, 0))
    val weeklySteps: StateFlow<List<Int>> = _weeklySteps

    // Dynamic stats for the Stats page
    private val _topActiveDay = MutableStateFlow(Pair("None", 0))
    val topActiveDay: StateFlow<Pair<String, Int>> = _topActiveDay

    private val _thirdInactiveDay = MutableStateFlow(Pair("None", 0))
    val thirdInactiveDay: StateFlow<Pair<String, Int>> = _thirdInactiveDay

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    // Persistent Settings States
    private val _stepTrackingEnabled = MutableStateFlow(prefs.getBoolean("step_tracking_enabled", false))
    val stepTrackingEnabled: StateFlow<Boolean> = _stepTrackingEnabled

    private val _autoResetEnabled = MutableStateFlow(prefs.getBoolean("auto_reset_enabled", false))
    val autoResetEnabled: StateFlow<Boolean> = _autoResetEnabled

    private val _stepGoalReminderEnabled = MutableStateFlow(prefs.getBoolean("step_goal_reminder_enabled", false))
    val stepGoalReminderEnabled: StateFlow<Boolean> = _stepGoalReminderEnabled

    init {
        instance = this
        loadInitialValues()
        loadWeeklyHistory()

        // Periodic Refresh: refresh weekly history every 5 minutes
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000L) // 5 minutes in ms
                loadWeeklyHistory()
            }
        }
    }

    private fun loadInitialValues() {
        val currentSteps = prefs.getInt("current_daily_steps", 0)
        _stepCount.value = currentSteps
        updateMetrics(currentSteps)

        _activeTime.value = (prefs.getLong("active_time_ms", 0L) / 60000).toInt()
        _floors.value = prefs.getInt("current_daily_floors", 0)
        _sleepTime.value = prefs.getString("current_daily_sleep", "0h 0m") ?: "0h 0m"
    }

    fun onSensorStepChanged(totalSteps: Float) {
        if (!_stepTrackingEnabled.value) return

        if (previousTotalSteps == 0f) {
            previousTotalSteps = totalSteps
            prefs.edit { putFloat("previous_steps", totalSteps) }
        }

        val currentSteps = (totalSteps - previousTotalSteps).toInt().coerceAtLeast(0)
        _stepCount.value = currentSteps

        updateMetrics(currentSteps)
        prefs.edit {
            putInt("current_daily_steps", currentSteps)
            putFloat("total_steps", totalSteps)
        }

        // Immediate chart sync
        loadWeeklyHistory()
    }

    private fun updateMetrics(steps: Int) {
        _calories.value = (steps * 0.04f).toInt()
        _distance.value = (steps * 0.00076f)
        _floors.value = (steps / 500)
        prefs.edit { putInt("current_daily_floors", _floors.value) }
    }

    fun addActiveTime(deltaMs: Long) {
        if (!_stepTrackingEnabled.value) return
        activeTimeMs += deltaMs
        _activeTime.value = (activeTimeMs / 60000).toInt()
        prefs.edit { putLong("active_time_ms", activeTimeMs) }
    }

    fun setStepTrackingEnabled(enabled: Boolean) {
        _stepTrackingEnabled.value = enabled
        prefs.edit { putBoolean("step_tracking_enabled", enabled) }
    }

    fun setAutoResetEnabled(enabled: Boolean) {
        _autoResetEnabled.value = enabled
        prefs.edit { putBoolean("auto_reset_enabled", enabled) }
    }

    fun setStepGoalReminderEnabled(enabled: Boolean) {
        _stepGoalReminderEnabled.value = enabled
        prefs.edit { putBoolean("step_goal_reminder_enabled", enabled) }
    }

    fun resetDailyCounters() {
        previousTotalSteps = prefs.getFloat("previous_steps", 0f)
        activeTimeMs = 0L

        _stepCount.value = 0
        _activeTime.value = 0
        _calories.value = 0
        _distance.value = 0.0f
        _floors.value = 0
        _sleepTime.value = "0h 0m"
        loadWeeklyHistory()
    }

    private fun loadWeeklyHistory() {
        val historyWithDayNames = mutableListOf<Pair<String, Int>>()
        val historyStepsOnly = mutableListOf<Int>()

        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDayName = SimpleDateFormat("EEEE", Locale.getDefault())

        // Live Chart Sync: load previous 6 days from history
        for (i in 6 downTo 1) {
            val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dateKey = "history_${sdfDate.format(calendar.time)}"
            val steps = prefs.getInt(dateKey, 0)
            val dayName = sdfDayName.format(calendar.time)

            historyWithDayNames.add(Pair(dayName, steps))
            historyStepsOnly.add(steps)
        }

        // Always use live _stepCount.value for 'Today'
        val todayName = sdfDayName.format(Date())
        historyWithDayNames.add(Pair(todayName, _stepCount.value))
        historyStepsOnly.add(_stepCount.value)

        _weeklySteps.value = historyStepsOnly

        // Calculate Stats for the chart data
        calculateTopDays(historyWithDayNames)
    }

    private fun calculateTopDays(history: List<Pair<String, Int>>) {
        if (history.isEmpty()) return

        // 1. #1 Active Day (Max steps)
        val mostActive = history.maxByOrNull { it.second }
        if (mostActive != null) {
            _topActiveDay.value = mostActive
        }

        // 2. #3 Inactive Day (3rd lowest steps)
        val sortedBySteps = history.sortedBy { it.second }
        val thirdInactive = if (sortedBySteps.size >= 3) sortedBySteps[2] else sortedBySteps[0]
        _thirdInactiveDay.value = thirdInactive
    }

    fun toggleTheme() {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue
        prefs.edit { putBoolean("is_dark_theme", newValue) }
    }

}
