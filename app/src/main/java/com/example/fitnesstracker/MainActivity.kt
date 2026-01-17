package com.example.fitnesstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.runtime.collectAsState
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.fitnesstracker.ui.theme.FitnessTrackerTheme
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity(), SensorEventListener, EasyPermissions.PermissionCallbacks {

    companion object {
        const val RC_RECOGNITION = 100
        const val RECOGNITION_PERMISSION = Manifest.permission.ACTIVITY_RECOGNITION
    }

    private var sensorManager: SensorManager? = null
    private var totalSteps = 0f
    private var previousTotalSteps = 0f

    val viewModel: FitnessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        loadData() // load previous steps if any
        requestActivityPermission()
        scheduleDailyReset()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            FitnessTrackerTheme(darkTheme = isDarkTheme) {
                FitnessScreen(
                    viewModel = viewModel,
                    onRequestPermission = { requestActivityPermission() }
                )
            }
        }
    }
    private fun scheduleDailyReset() {
        val now = Calendar.getInstance()

        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        val delay = nextMidnight.timeInMillis - now.timeInMillis

        val dailyWork = OneTimeWorkRequestBuilder<DailyResetWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("DAILY_RESET_WORK")
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "DAILY_RESET_WORK",
            ExistingWorkPolicy.REPLACE,
            dailyWork
        )

        Log.d("MainActivity", "Daily reset scheduled in ${delay / 1000 / 60} minutes")
    }

    override fun onResume() {
        super.onResume()
        // register sensor
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) {
            Toast.makeText(this, "No step counter sensor detected", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            totalSteps = event.values[0]


            if (previousTotalSteps == 0f) {
                previousTotalSteps = totalSteps
                saveData()
            }

            val currentSteps = (totalSteps - previousTotalSteps).toInt()
            viewModel.onSensorStepChanged(totalSteps, this)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}



    private fun saveData() {
        getSharedPreferences("fitness_data", Context.MODE_PRIVATE)
            .edit()
            .putFloat("previous_steps", previousTotalSteps)
            .apply()
    }

    private fun loadData() {
        previousTotalSteps = getSharedPreferences("fitness_data", Context.MODE_PRIVATE)
            .getFloat("previous_steps", 0f)
    }

    // ----- EasyPermissions -----
    private fun requestActivityPermission() {
        if (EasyPermissions.hasPermissions(this, RECOGNITION_PERMISSION)) {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show()
        } else {
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, RC_RECOGNITION, RECOGNITION_PERMISSION)
                    .setRationale("We need this permission to track your daily steps")
                    .setPositiveButtonText("Allow")
                    .setNegativeButtonText("Cancel")
                    .build()
            )
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String?>) {
        if (perms.contains(RECOGNITION_PERMISSION)) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String?>) {
        Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}



//@Preview(showBackground = true)
@Composable
fun FitnessScreen(
    onRequestPermission: () -> Unit={},
    viewModel: FitnessViewModel = viewModel()
)  {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadTodaySteps(context)
    }
    val stepCount by viewModel.stepCount.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val activeTime by viewModel.activeTime.collectAsState()
    val calories by viewModel.calories.collectAsState()
    Scaffold(
        bottomBar = { BottomNavBar() }
    ) { padding ->
        Column (
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {

            TopHeader("Fitness Tracker")

            RequestPermissionButton {
                onRequestPermission()
            }

            Text("Hello, Adam!", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp , modifier = Modifier.padding(20.dp,5.dp))
            Text("Today's Activity", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),modifier = Modifier.padding(20.dp,5.dp))

            Spacer(modifier = Modifier.height(16.dp))

            StepProgress(
                steps = stepCount,  // Use ViewModel state
                goal = goal
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatsRow(calories = calories,
                activeTime = activeTime)

            Spacer(modifier = Modifier.height(16.dp))

            DailySummary()

            Spacer(modifier = Modifier.height(16.dp))

            WeeklyProgressExact(viewModel)
        }
    }
}
@Composable
fun TopHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF2E86DE), Color(0xFF54A0FF))
                )
            )
            .padding(16.dp)
    ) {
        Text("$title", color = Color.White,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
@Composable
fun StepProgress(steps: Int, goal: Int) {
    val progress = (steps.toFloat() / goal)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            progress = progress,
            strokeWidth = 12.dp,
            modifier = Modifier.size(180.dp),
            color = Color(0xFF2ECC71)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$steps",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text("Steps")
            Text("Goal: $goal Steps", color = Color.Gray)
        }
    }
}
@Composable
fun StatsRow(calories: Int, activeTime: Int) {
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("🔥", "$calories", "Calories Burned")
        StatItem("⏱️", "$activeTime min", "Active Time")
    }
}

@Composable
fun StatItem(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 24.sp)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(label, color = Color.Gray)
    }
}
@Composable
fun DailySummary() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Daily Summary", fontWeight = FontWeight.Bold ,textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(0.dp,10.dp), color = MaterialTheme.colorScheme.onBackground)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            SummaryCard( icon = Icons.Default.DirectionsRun,"Distance", "5.6 km")
            SummaryCard(icon = Icons.Default.TrendingUp, "Floors Climoed", "8 Floors")
            SummaryCard( icon = Icons.Default.NightsStay,"Resful Sleep", "7h 20m")
        }
    }
}
@Composable
fun SummaryCard(
    icon: ImageVector,
    title: String,
    value: String
) {
    Card(
        modifier = Modifier
            .width(105.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor =  MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF2C3E50),
                modifier = Modifier.size(28.dp)
            )

            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
fun WeeklyProgressExact(viewModel: FitnessViewModel) {

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadLast7Days(context) // هنا نحمّل آخر 7 أيام
    }
    val last7Days by viewModel.last7Days.collectAsState()
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
            else -> 6
        }
    val last7DaysByChart = last7Days.toList().reversed()
    val maxValue = (last7DaysByChart.maxOrNull() ?: 1).toFloat()

    val dayLabels = listOf("M","T","W","T","F","S","S")



    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Text(
            text = "Weekly Progress",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color =  MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ===== CHART AREA =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {

            // Grid lines
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                }
            }

            // Bars
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {

                last7DaysByChart.forEachIndexed { index, steps ->
                    val barHeight = (steps / maxValue) * 110f
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {

                        if (index == highlightIndex) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF7ED957), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = steps.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        // Bar
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (index == highlightIndex)
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF7ED957), Color(0xFF4CAF50))
                                        )
                                    else
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF6FB1FC), Color(0xFF4A90E2))
                                        )
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ===== DAYS LABELS (برا Box) =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLabels.forEach { day ->
                Text(day, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
            }
        }
    }
}


@Composable
fun BottomNavBar() {
    val context = LocalContext.current
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent= Intent(context, State::class.java)
                context.startActivity(intent)
            },
            icon = { Icon(Icons.Default.Favorite, null) },
            label = { Text("Stats") }
        )
        NavigationBarItem(
            selected = false,
            onClick = {val intent= Intent(context, SettingsActivity::class.java)
                context.startActivity(intent)},
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Settings") }
        )
    }
}
@Composable
fun RequestPermissionButton(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onRequestPermission() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            "Request Permission",
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}