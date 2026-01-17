package com.example.fitnesstracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.ui.theme.FitnessTrackerTheme
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest

class SettingsActivity : ComponentActivity(), EasyPermissions.PermissionCallbacks {
    private val viewModel: FitnessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            FitnessTrackerTheme(darkTheme = isDarkTheme) {
                SettingsScreen(
                    viewModel = viewModel,
                    onRequestNotificationPermission = { requestNotificationPermission() },
                    onRequestStepPermission = { requestStepPermission() }
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.POST_NOTIFICATIONS)) {
            viewModel.setStepGoalReminderEnabled(true)
        } else {
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, 200, Manifest.permission.POST_NOTIFICATIONS)
                    .setRationale("We need this permission to send you notifications")
                    .setPositiveButtonText("Allow")
                    .setNegativeButtonText("Cancel")
                    .build()
            )
        }
    }

    private fun requestStepPermission() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACTIVITY_RECOGNITION)) {
            viewModel.setStepTrackingEnabled(true)
        } else {
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(this, 100, Manifest.permission.ACTIVITY_RECOGNITION)
                    .setRationale("We need this permission to track your steps")
                    .setPositiveButtonText("Allow")
                    .setNegativeButtonText("Cancel")
                    .build()
            )
        }
    }

    override fun onPermissionsGranted(p0: Int, p1: List<String?>) {
        if (p1.contains(Manifest.permission.POST_NOTIFICATIONS)) viewModel.setStepGoalReminderEnabled(true)
        if (p1.contains(Manifest.permission.ACTIVITY_RECOGNITION)) viewModel.setStepTrackingEnabled(true)
    }

    override fun onPermissionsDenied(p0: Int, p1: List<String?>) {
        if (p1.contains(Manifest.permission.POST_NOTIFICATIONS)) viewModel.setStepGoalReminderEnabled(false)
        if (p1.contains(Manifest.permission.ACTIVITY_RECOGNITION)) viewModel.setStepTrackingEnabled(false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}

@Composable
fun SettingsScreen(
    viewModel: FitnessViewModel,
    onRequestNotificationPermission: () -> Unit,
    onRequestStepPermission: () -> Unit
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val stepTrackingEnabled by viewModel.stepTrackingEnabled.collectAsState()
    val autoResetEnabled by viewModel.autoResetEnabled.collectAsState()
    val stepGoalReminderEnabled by viewModel.stepGoalReminderEnabled.collectAsState()

    Scaffold(
        bottomBar = { BottomNavBarSettings() }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            TopHeader("Settings")

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Section
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Hello, Adam", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("adam.mail@example.com", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard {
                SettingsItem(icon = Icons.Default.Description, title = "My Profile")
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard {
                SettingsItem(icon = Icons.Default.Logout, title = "Log Out", textColor = Color(0xFFE74C3C), arrowColor = Color(0xFFE74C3C))
            }

            SectionHeader("Account")
            SettingsCard {
                Column {
                    SettingsItem(icon = Icons.Default.PersonOutline, title = "My Profile")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.background)

                    SettingsSwitchItem(
                        icon = Icons.Default.DirectionsRun,
                        title = "Step Tracking",
                        checked = stepTrackingEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) onRequestStepPermission() else viewModel.setStepTrackingEnabled(false)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.background)

                    SettingsSwitchItem(
                        icon = Icons.Default.History,
                        title = "Auto-Reset Daily Steps",
                        checked = autoResetEnabled,
                        onCheckedChange = { viewModel.setAutoResetEnabled(it) }
                    )
                }
            }

            SectionHeader("Reminders")
            SettingsCard {
                SettingsSwitchItem(
                    icon = Icons.Default.NotificationsNone,
                    title = "Step Goal Reminder",
                    checked = stepGoalReminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) onRequestNotificationPermission() else viewModel.setStepGoalReminderEnabled(false)
                    }
                )
            }

            SectionHeader("General")
            SettingsCard {
                Column {
                    SettingsItem(
                        icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.WbSunny,
                        title = "App Theme",
                        trailingText = if (isDarkTheme) "Dark" else "Light",
                        onClick = { viewModel.toggleTheme() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.background)
                    SettingsItem(icon = Icons.Default.Send, title = "Send Feedback")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        content = { content() }
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    showArrow: Boolean = true,
    arrowColor: Color = Color.Gray,
    trailingText: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, modifier = Modifier.weight(1f), color = textColor, fontSize = 16.sp)
        if (trailingText != null) Text(text = trailingText, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))
        if (showArrow) Icon(Icons.Default.ChevronRight, null, tint = arrowColor)
    }
}

@Composable
fun SettingsSwitchItem(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, modifier = Modifier.weight(1f), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF7ED957))
        )
    }
}

@Composable
fun BottomNavBarSettings() {
    val context = LocalContext.current
    NavigationBar {
        NavigationBarItem(selected = false, onClick = { context.startActivity(Intent(context, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }); (context as? Activity)?.finish() }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
        NavigationBarItem(selected = false, onClick = { context.startActivity(Intent(context, State::class.java)); (context as? Activity)?.finish() }, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Stats") })
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
    }
}