package com.example.fitnesstracker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.*

class StepResetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)

        // Respect the Auto-Reset toggle
        val autoResetEnabled = prefs.getBoolean("auto_reset_enabled", false)
        if (!autoResetEnabled) return Result.success()

        // 1. Get today's data
        val currentSteps = prefs.getInt("current_daily_steps", 0)

        // 2. Get date key for storage (using yesterday's date since it's now midnight)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        // 3. Save today's steps to history slot
        prefs.edit().putInt("history_$dateKey", currentSteps).apply()

        // 4. Reset daily counters for the new day
        prefs.edit()
            .putInt("current_daily_steps", 0)
            .putLong("active_time_ms", 0L)
            .putInt("current_daily_floors", 0)
            .putString("current_daily_sleep", "0h 0m")
            .putFloat("previous_steps", prefs.getFloat("total_steps", 0f))
            // RESET RANK ACHIEVEMENTS FOR THE NEW DAY
            .putInt("last_notified_rank", 0)
            .apply()

        // Trigger UI update if app is open
        FitnessViewModel.instance?.resetDailyCounters()

        return Result.success()
    }
}