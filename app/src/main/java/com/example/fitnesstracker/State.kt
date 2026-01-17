package com.example.fitnesstracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitnesstracker.ui.theme.FitnessTrackerTheme

class State : ComponentActivity() {
    private val viewModel: FitnessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            FitnessTrackerTheme(darkTheme = isDarkTheme) {
                StateScreen(viewModel)
            }
        }
    }
}

@Composable
fun StateScreen(viewModel: FitnessViewModel) {
    val calories by viewModel.calories.collectAsState()
    val distance by viewModel.distance.collectAsState()
    val floors by viewModel.floors.collectAsState()
    val sleepTime by viewModel.sleepTime.collectAsState()

    val topActiveDay by viewModel.topActiveDay.collectAsState()
    val thirdInactiveDay by viewModel.thirdInactiveDay.collectAsState()

    Scaffold(
        bottomBar = { BottomNavBarstate() }
    ) { padding ->
        Column (
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ){
            TopHeader("Stats")
            Spacer(modifier = Modifier.height(16.dp))

            Last7DaysCard(viewModel)

            Spacer(modifier = Modifier.height(16.dp))

            ActiveInactiveSection(topActiveDay, thirdInactiveDay)

            Spacer(modifier = Modifier.height(16.dp))

            MetricsSummarySection(calories, distance, floors, sleepTime)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun Last7DaysCard(viewModel: FitnessViewModel) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Last 7 Days", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(12.dp))

            WeeklyProgressExact(viewModel)
        }
    }
}

@Composable
fun ActiveInactiveSection(topActive: Pair<String, Int>, thirdInactive: Pair<String, Int>) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DayStatCard(
            title = "#1 Active Day",
            day = topActive.first,
            steps = "${topActive.second} Steps",
            modifier = Modifier.weight(1f)
        )

        DayStatCard(
            title = "#3 Inactive Day",
            day = thirdInactive.first,
            steps = "${thirdInactive.second} Steps",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DayStatCard(title: String, day: String, steps: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text(day, color = Color.Gray)
            Spacer(modifier = Modifier.height(6.dp))
            Text(steps, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun MetricsSummarySection(calories: Int, distance: Float, floors: Int, sleepTime: String) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Metrics Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))

            MetricRow("🔥", "Calories Burned", "$calories kcal")
            MetricRow("👣", "Distance", String.format("%.1f km", distance))
            MetricRow("⬆️", "Floors Climbed", "$floors Floors")
            MetricRow("🌙", "Restful Sleep", sleepTime)
        }
    }
}

@Composable
fun MetricRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$icon  $label", color = MaterialTheme.colorScheme.onSurface)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun BottomNavBarstate() {
    val context = LocalContext.current
    NavigationBar {
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Favorite, null) },
            label = { Text("Stats") }
        )
        NavigationBarItem(
            selected = false,
            onClick = {
                val intent = Intent(context, SettingsActivity::class.java)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            },
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Settings") }
        )
    }
}