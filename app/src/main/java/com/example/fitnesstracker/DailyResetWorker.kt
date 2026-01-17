package com.example.fitnesstracker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.*
import java.util.concurrent.TimeUnit

class DailyResetWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("fitness_data", Context.MODE_PRIVATE)
        val lastDate = prefs.getString("last_date", "")
        val today = getTodayDate()

        if (lastDate != today) {
            val todaySteps = prefs.getInt("today_steps", 0)

            val last7 = (0..5).map { i -> prefs.getInt("steps_day_${i+1}", 0) }.toMutableList()
            last7.add(todaySteps)

            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            if (dayOfWeek == Calendar.SUNDAY) {
                // Sunday → reset all last7Days
                (0..6).forEach { i ->
                    prefs.edit().putInt("steps_day_$i", 0).apply()
                }
            } else {
                // Save last 7 days normally
                last7.forEachIndexed { i, value ->
                    prefs.edit().putInt("steps_day_$i", value).apply()
                }
            }

            // Reset today_steps and last_date
            prefs.edit().putInt("today_steps", 0).putString("last_date", today).apply()
        }

        scheduleNextReset()
        return Result.success()
    }

    private fun scheduleNextReset() {
        val now = Calendar.getInstance()
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val delay = nextMidnight.timeInMillis - now.timeInMillis

        val work = OneTimeWorkRequestBuilder<DailyResetWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "DAILY_RESET_WORK",
            ExistingWorkPolicy.REPLACE,
            work
        )
    }

    private fun getTodayDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}