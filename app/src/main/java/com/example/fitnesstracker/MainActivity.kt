package com.example.fitnesstracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import com.example.fitnesstracker.ui.theme.FitnessTrackerTheme
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity(), SensorEventListener, EasyPermissions.PermissionCallbacks {
    companion object {
        const val RC_RECOGNITION = 100
        const val RECOGNITION_PERMISSION = Manifest.permission.ACTIVITY_RECOGNITION
    }

    private val viewModel: FitnessViewModel by viewModels()
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var lastStepTimeNs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { enableEdgeToEdge() } catch (e: Exception) {}

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        scheduleDailyReset()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val stepTrackingEnabled by viewModel.stepTrackingEnabled.collectAsState()
            val reminderEnabled by viewModel.stepGoalReminderEnabled.collectAsState()

            // Re-sync sensors and workers when settings change
            LaunchedEffect(stepTrackingEnabled) {
                updateSensorRegistration()
            }

            LaunchedEffect(reminderEnabled) {
                if (reminderEnabled) {
                    scheduleGoalReminders()
                } else {
                    cancelGoalReminders()
                }
            }

            FitnessTrackerTheme(darkTheme = isDarkTheme) {
                FitnessScreen(
                    onRequestPermission = { requestActivityPermission() },
                    viewModel = viewModel
                )
            }
        }
    }

    private fun scheduleDailyReset() {
        val resetWorkRequest = PeriodicWorkRequestBuilder<StepResetWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyStepReset",
            ExistingPeriodicWorkPolicy.KEEP,
            resetWorkRequest
        )
    }

    private fun scheduleGoalReminders() {
        val reminderRequest = PeriodicWorkRequestBuilder<GoalReminderWorker>(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "HourlyGoalReminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            reminderRequest
        )
    }

    private fun cancelGoalReminders() {
        WorkManager.getInstance(this).cancelUniqueWork("HourlyGoalReminder")
    }

    private fun calculateInitialDelay(): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis - now
    }

    private fun updateSensorRegistration() {
        if (viewModel.stepTrackingEnabled.value && EasyPermissions.hasPermissions(this, RECOGNITION_PERMISSION)) {
            stepSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        } else {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        updateSensorRegistration()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0]
            viewModel.onSensorStepChanged(totalSteps)

            val currentTimeNs = event.timestamp
            if (lastStepTimeNs != 0L) {
                val diffNs = currentTimeNs - lastStepTimeNs
                val diffMs = diffNs / 1_000_000
                if (diffMs < 10000) {
                    viewModel.addActiveTime(diffMs)
                }
            }
            lastStepTimeNs = currentTimeNs
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun requestActivityPermission() {
        if (EasyPermissions.hasPermissions(this, RECOGNITION_PERMISSION)) {
            viewModel.setStepTrackingEnabled(true)
        } else {
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, RC_RECOGNITION, RECOGNITION_PERMISSION)
                    .setRationale("We need this permission to track your steps")
                    .setPositiveButtonText("Allow")
                    .setNegativeButtonText("Cancel")
                    .build()
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(p0: Int, p1: List<String?>) {
        if (p1.contains(RECOGNITION_PERMISSION)) {
            viewModel.setStepTrackingEnabled(true)
            updateSensorRegistration()
        }
    }

    override fun onPermissionsDenied(p0: Int, p1: List<String?>) {
        if (p1.contains(RECOGNITION_PERMISSION)) {
            viewModel.setStepTrackingEnabled(false)
        }
    }
}

@Composable
fun FitnessScreen(onRequestPermission: () -> Unit, viewModel: FitnessViewModel) {
    val stepCount by viewModel.stepCount.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val activeTime by viewModel.activeTime.collectAsState()
    val calories by viewModel.calories.collectAsState()
    val distance by viewModel.distance.collectAsState()
    val floors by viewModel.floors.collectAsState()
    val sleepTime by viewModel.sleepTime.collectAsState()
    val trackingEnabled by viewModel.stepTrackingEnabled.collectAsState()

    Scaffold(
        bottomBar = { BottomNavBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopHeader("Fitness Tracker")

            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { onRequestPermission() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = if (trackingEnabled) "Tracking Active" else "Enable Tracking",
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text("Hello, Adam!", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp))
            Spacer(modifier = Modifier.height(16.dp))

            StepProgress(steps = stepCount, goal = goal)
            Spacer(modifier = Modifier.height(16.dp))

            StatsRow(calories = calories, activeTime = activeTime)
            Spacer(modifier = Modifier.height(16.dp))

            DailySummary(distance, floors, sleepTime)
            Spacer(modifier = Modifier.height(16.dp))

            WeeklyProgressExact(viewModel)
        }
    }
}

@Composable
fun TopHeader(title: String) {
    Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF2E86DE), Color(0xFF54A0FF)))).padding(16.dp), contentAlignment = Alignment.Center) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StepProgress(steps: Int, goal: Int) {
    val progress = if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(progress = { progress }, strokeWidth = 12.dp, modifier = Modifier.size(180.dp), color = Color(0xFF2ECC71))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$steps", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Steps", color = MaterialTheme.colorScheme.onBackground)
            Text("Goal: $goal", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun StatsRow(calories: Int, activeTime: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        StatItem("🔥", "$calories", "kcal")
        StatItem("⏱️", "$activeTime", "min")
    }
}

@Composable
fun StatItem(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 24.sp)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}

@Composable
fun DailySummary(distance: Float, floors: Int, sleepTime: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Daily Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard(Icons.AutoMirrored.Filled.DirectionsRun, "Distance", String.format("%.1f km", distance), Modifier.weight(1f))
            SummaryCard(Icons.AutoMirrored.Filled.TrendingUp, "Floors", floors.toString(), Modifier.weight(1f))
            SummaryCard(Icons.Default.NightsStay, "Sleep", sleepTime, Modifier.weight(1f))
        }
    }
}

@Composable
fun SummaryCard(icon: ImageVector, title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(100.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color(0xFF2E86DE), modifier = Modifier.size(24.dp))
            Text(title, fontSize = 10.sp, color = Color.Gray)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
@Composable
fun WeeklyProgressExact(viewModel: FitnessViewModel) {

    val weeklySteps by viewModel.weeklySteps.collectAsState()

    val baseDays = listOf("M", "T", "W", "T", "F", "S", "S")

    val todayIndex = java.time.LocalDate.now().dayOfWeek.value - 1

    val days = (0 until 7).map {
        baseDays[(todayIndex - (6 - it) + 7) % 7]
    }

    val maxValue = weeklySteps.maxOrNull()
        ?.toFloat()
        ?.coerceAtLeast(1000f) ?: 1000f

    Column(modifier = Modifier.padding(16.dp)) {

        Text(
            "Weekly Progress",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {

            weeklySteps.forEachIndexed { index, steps ->

                val barHeight = (steps / maxValue * 120).dp

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (index == weeklySteps.lastIndex)
                                    Color(0xFF2ECC71) // اليوم
                                else
                                    Color(0xFF2E86DE)
                            )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(days[index], fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}


@Composable
fun BottomNavBar() {
    val context = LocalContext.current
    NavigationBar {
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
        NavigationBarItem(selected = false, onClick = { context.startActivity(Intent(context, State::class.java)) }, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Stats") })
        NavigationBarItem(selected = false, onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
    }
}