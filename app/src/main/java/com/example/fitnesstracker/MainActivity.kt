package com.example.fitnesstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.runtime.collectAsState
import com.google.android.gms.location.ActivityRecognition
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest

import androidx.lifecycle.viewmodel.compose.viewModel
import android.os.Build
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity(), EasyPermissions.PermissionCallbacks {
    companion object{
        val rc_recognition=100
        val recognition_permission=Manifest.permission.ACTIVITY_RECOGNITION
    }
    private lateinit var activityReceiver: ActivityReceiver
    val viewModel: FitnessViewModel by viewModels ()

    private val stepUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "STEP_COUNT_UPDATED" -> {
                    val steps = intent.getIntExtra("steps", 0)
                    // In a real app, you would update ViewModel here
                    // For now, we'll just save to SharedPreferences
                    val prefs = getSharedPreferences("fitness_data", Context.MODE_PRIVATE)
                    prefs.edit().putInt("current_steps", steps).apply()

                    // You could also trigger a recomposition here if needed
                    viewModel.onSensorStepChanged(steps)
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val savedSteps = getSharedPreferences("fitness_data", Context.MODE_PRIVATE)
            .getInt("step_count", 100)

        // 2️⃣ Update ViewModel
        viewModel.onSensorStepChanged(savedSteps)
        setContent {

            MaterialTheme{
                FitnessScreen(
                    onRequestPermission = { requestActivityPermission() },
                    viewModel = viewModel
                )
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver
        unregisterReceiver(stepUpdateReceiver)
    }
    private fun requestActivityPermission(){
        if(EasyPermissions.hasPermissions(this, recognition_permission)){
            startActivityRecognition()
        }else{
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(
                    this,
                    rc_recognition,
                    recognition_permission

                )
                    .setRationale("We need this permission to track your daily steps")
                    .setPositiveButtonText("Allow")
                    .setNegativeButtonText("Cancel")
                    .build()
            )
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            this
        )
    }

    override fun onPermissionsGranted(p0: Int, p1: List<String?>) {
        if (p1.contains(recognition_permission)) {
            startActivityRecognition()
        }
    }

    override fun onPermissionsDenied(p0: Int, p1: List<String?>) {
        Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
    }
    private fun startActivityRecognition() {
        // Check if permission is still granted
        if (!EasyPermissions.hasPermissions(this, recognition_permission)) {
            Toast.makeText(this, "Activity recognition permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val client = ActivityRecognition.getClient(this)
            val intent = Intent(this, ActivityReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            client.requestActivityUpdates(3000, pendingIntent)
                .addOnSuccessListener {
                    Toast.makeText(this, "Activity tracking started!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to start tracking: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            // Handle the case where permission was revoked
            Toast.makeText(this, "Permission was revoked. Please grant permission again.", Toast.LENGTH_SHORT).show()

            // Optionally, request permission again
            requestActivityPermission()
        }
    }
}



//@Preview(showBackground = true)
@Composable
fun FitnessScreen(
    onRequestPermission: () -> Unit={},
    viewModel: FitnessViewModel = viewModel()
)  {
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
                .background(Color(0xFFF5F7FA))
        ) {

            TopHeader("Fitness Tracker")

            RequestPermissionButton {
                onRequestPermission()
            }

            Text("Hello, Adam!", color = Color.Black, fontSize = 18.sp , modifier = Modifier.padding(20.dp,5.dp))
            Text("Today's Activity", color = Color.Black.copy(alpha = 0.8f) ,modifier = Modifier.padding(20.dp,5.dp))

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

            WeeklyProgressExact()
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
                fontWeight = FontWeight.Bold
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
        Text(value, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray)
    }
}
@Composable
fun DailySummary() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Daily Summary", fontWeight = FontWeight.Bold ,textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(0.dp,10.dp))

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
            containerColor = Color.White
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
                color = Color.Black
            )
        }
    }
}
@Composable
fun WeeklyProgressExact() {

    val data = listOf(
        Pair("M", 3200),
        Pair("T", 4500),
        Pair("W", 3800),
        Pair("T", 6000),
        Pair("F", 6800),
        Pair("S", 7450), // highlighted
        Pair("S", 7000)
    )

    val highlightIndex = 5
    val maxValue = data.maxOf { it.second }.toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Text(
            text = "Weekly Progress",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
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

                data.forEachIndexed { index, item ->

                    val barHeight = (item.second / maxValue) * 110f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {

                        // Bubble
                        if (index == highlightIndex) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFF7ED957),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = item.second.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
            data.forEach {
                Text(
                    text = it.first,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.width(22.dp),
                    textAlign = TextAlign.Center
                )
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
            onClick = {},
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
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            "Request Permission",
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}