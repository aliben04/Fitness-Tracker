package com.example.fitnesstracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityReceiver : BroadcastReceiver() {

    companion object {
        const val STEP_UPDATE_ACTION = "com.example.fitnesstracker.STEP_UPDATE"
        const val EXTRA_STEPS = "extra_steps"
    }

    override fun onReceive(context: Context, intent: Intent) {

        // Check if this intent has activity recognition result
        if (ActivityRecognitionResult.hasResult(intent)) {
            // Handle activity recognition
            handleActivityRecognition(context, intent)
            return
        }

        // Handle custom actions
        when (intent.action) {
            "com.example.fitnesstracker.STEP_UPDATE" -> {
                val steps = intent.getIntExtra("extra_steps", 0)
                handleStepUpdate(context, steps)
            }
            // Add more actions as needed
        }
    }

    private fun handleStepUpdate(context: Context, steps: Int) {
        // Store steps in SharedPreferences or update database
        val prefs = context.getSharedPreferences("fitness_data", Context.MODE_PRIVATE)
        prefs.edit().putInt("step_count", steps).apply()
        // Update ViewModel
        FitnessViewModel.instance?.onSensorStepChanged(steps)
        // Send broadcast to update UI
        val updateIntent = Intent("STEP_COUNT_UPDATED")
        updateIntent.putExtra("steps", steps)
        context.sendBroadcast(updateIntent)

        Toast.makeText(context, "Steps updated: $steps", Toast.LENGTH_SHORT).show()
    }

    private fun handleActivityRecognition(context: Context, intent: Intent) {
        val result = ActivityRecognitionResult.extractResult(intent)
        val mostProbableActivity = result?.mostProbableActivity

        mostProbableActivity?.let {
            val activityType = getActivityString(it.type)
            val confidence = it.confidence

            // Log or process the activity
            when (it.type) {
                DetectedActivity.WALKING, DetectedActivity.RUNNING -> {
                    // You could increment steps here
                    incrementSteps(context)
                }
            }

            Toast.makeText(
                context,
                "Activity: $activityType ($confidence%)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun incrementSteps(context: Context) {
        val prefs = context.getSharedPreferences("fitness_prefs", Context.MODE_PRIVATE)
        val currentSteps = prefs.getInt("step_count", 0)
        val newSteps = currentSteps + 1  // Or use a more sophisticated algorithm

        prefs.edit().putInt("step_count", newSteps).apply()
        FitnessViewModel.instance?.onSensorStepChanged(newSteps)
        // Notify UI
        val updateIntent = Intent("STEP_COUNT_UPDATED")
        updateIntent.putExtra("steps", newSteps)
        context.sendBroadcast(updateIntent)
    }

    private fun getActivityString(detectedActivityType: Int): String {
        return when (detectedActivityType) {
            DetectedActivity.IN_VEHICLE -> "In Vehicle"
            DetectedActivity.ON_BICYCLE -> "On Bicycle"
            DetectedActivity.ON_FOOT -> "On Foot"
            DetectedActivity.RUNNING -> "Running"
            DetectedActivity.STILL -> "Still"
            DetectedActivity.TILTING -> "Tilting"
            DetectedActivity.WALKING -> "Walking"
            DetectedActivity.UNKNOWN -> "Unknown"
            else -> "Unknown"
        }
    }
}