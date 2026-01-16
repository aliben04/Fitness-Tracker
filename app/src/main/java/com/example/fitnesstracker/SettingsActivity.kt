package com.example.fitnesstracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
    val viewModel: FitnessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            FitnessTrackerTheme(darkTheme = isDarkTheme) {
                SettingsScreen(viewModel,

                    onRequestNotificationPermission = { requestNotificationPermission() })
            }
        }
    }
     private fun requestNotificationPermission() {
        if (EasyPermissions.hasPermissions(this, android.Manifest.permission.POST_NOTIFICATIONS)) {
            Toast.makeText(this, "Notification permission already granted", Toast.LENGTH_SHORT).show()
        } else {
            EasyPermissions.requestPermissions(
                PermissionRequest.Builder(
                    this,
                    200, // request code
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
                    .setRationale("We need this permission to send you notifications")
                    .setPositiveButtonText("Allow")
                    .setNegativeButtonText("Cancel")
                    .build()
            )
        }
    }

    override fun onPermissionsGranted(p0: Int, p1: List<String?>) {
        if (p1.contains(android.Manifest.permission.POST_NOTIFICATIONS)) {
            Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPermissionsDenied(p0: Int, p1: List<String?>) {
        if (p1.contains(android.Manifest.permission.POST_NOTIFICATIONS)) {
            Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

}

@Composable
fun SettingsScreen(viewModel: FitnessViewModel,
                   onRequestNotificationPermission: () -> Unit) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

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
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Hello, Adam",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "adam.mail@example.com",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "My Profile"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = "Log Out",
                    textColor = Color(0xFFE74C3C),
                    arrowColor = Color(0xFFE74C3C)
                )
            }

            SectionHeader("Account")
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsItem(icon = Icons.Default.PersonOutline, title = "My Profile")
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.background)
                    SettingsSwitchItem(icon = Icons.Default.DirectionsRun, title = "Step Tracking", initialValue = false)
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.background)
                    SettingsSwitchItem(icon = Icons.Default.History, title = "Auto-Reset Daily Steps", initialValue = false)
                }
            }

            SectionHeader("Reminders")
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                SettingsSwitchItem(icon = Icons.Default.NotificationsNone, title = "Step Goal Reminder", initialValue = false,
                        onCheckedChange = { enabled ->
                    if (enabled) {
                        onRequestNotificationPermission()
                    }
                })
            }

            SectionHeader("General")
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsItem(
                        icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.WbSunny,
                        title = "App Theme",
                        trailingText = if (isDarkTheme) "Dark" else "Light",
                        onClick = { viewModel.toggleTheme() }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.background)
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, modifier = Modifier.weight(1f), color = textColor, fontSize = 16.sp)

        if (trailingText != null) {
            Text(text = trailingText, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))
        }

        if (showArrow) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = arrowColor)
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    initialValue: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    var checked by remember { mutableStateOf(initialValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, modifier = Modifier.weight(1f), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = checked,
            onCheckedChange = { checked = it
                onCheckedChange(it)},
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF7ED957),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}

@Composable
fun BottomNavBarSettings() {
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
            selected = false,
            onClick = {
                val intent = Intent(context, State::class.java)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            },
            icon = { Icon(Icons.Default.Favorite, null) },
            label = { Text("Stats") }
        )
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Settings") }
        )
    }
}