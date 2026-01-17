package com.example.fitnesstracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class GoalReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val RANKS = listOf(
        Pair("Beginner", 100),
        Pair("Walker", 500),
        Pair("Runner", 1000),
        Pair("Sprinter", 2500),
        Pair("Athlete", 5000),
        Pair("Godlike", 10000)
    )

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)

        // 1. Check if reminder is enabled
        val isEnabled = prefs.getBoolean("step_goal_reminder_enabled", false)
        if (!isEnabled) return Result.success()

        val currentSteps = prefs.getInt("current_daily_steps", 0)

        // 2. Check for Rank Achievements
        checkRankAchievements(currentSteps, prefs)

        // 3. Check for Step Goal Reminder (Existing Logic)
        val goal = 10000
        if (currentSteps < goal) {
            sendStepReminderNotification(currentSteps, goal)
        }

        return Result.success()
    }

    private fun checkRankAchievements(currentSteps: Int, prefs: android.content.SharedPreferences) {
        val lastNotifiedRank = prefs.getInt("last_notified_rank", 0)

        // Find the highest rank achieved that hasn't been notified yet
        val newRank = RANKS.reversed().find { it.second <= currentSteps && it.second > lastNotifiedRank }

        newRank?.let { (rankName, rankSteps) ->
            sendAchievementNotification(rankName, rankSteps)
            prefs.edit().putInt("last_notified_rank", rankSteps).apply()
        }
    }

    private fun sendAchievementNotification(rankName: String, rankSteps: Int) {
        val channelId = "achievement_channel"
        val notificationId = 2001 // Different ID for achievements

        createNotificationChannel(channelId, "Achievements", NotificationManager.IMPORTANCE_HIGH)

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.btn_star_big_on) // Achievement Icon
            .setContentTitle("New Rank Achieved!")
            .setContentText("Congrats! You passed the $rankName rank with $rankSteps steps!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notify(notificationId, builder.build())
    }

    private fun sendStepReminderNotification(current: Int, goal: Int) {
        val channelId = "step_goal_reminder"
        val notificationId = 1001

        createNotificationChannel(channelId, "Step Goal Reminder", NotificationManager.IMPORTANCE_DEFAULT)

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Keep going!")
            .setContentText("You have $current steps. Only ${goal - current} more to reach your goal!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notify(notificationId, builder.build())
    }

    private fun createNotificationChannel(channelId: String, name: String, importance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, name, importance)
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun notify(id: Int, notification: android.app.Notification) {
        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(id, notification)
            }
        }
    }
}