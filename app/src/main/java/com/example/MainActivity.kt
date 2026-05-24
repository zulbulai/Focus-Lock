package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.SettingsRepository
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkAndStartService()
    }

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsRepository = SettingsRepository(applicationContext)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            settingsRepository = settingsRepository,
                            onStartService = { checkAndStartService() },
                            onStopService = { stopScreenTimeService() },
                            onNavigateSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            settingsRepository = settingsRepository,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndStartService() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            val serviceIntent = Intent(this, ScreenTimeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun stopScreenTimeService() {
        val serviceIntent = Intent(this, ScreenTimeService::class.java)
        stopService(serviceIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    settingsRepository: SettingsRepository,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val currentSessionSeconds by settingsRepository.currentSessionSecondsFlow.collectAsStateWithLifecycle(initialValue = 0)
    val workDuration by settingsRepository.workDurationFlow.collectAsStateWithLifecycle(initialValue = 30 * 60)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Lock") },
                actions = {
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Current Session", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            
            val remaining = (workDuration - currentSessionSeconds).coerceAtLeast(0)
            val minutes = remaining / 60
            val seconds = remaining % 60
            
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("until next mandatory break", color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onStartService,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Start Tracking")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onStopService,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Stop Tracking")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val workDuration by settingsRepository.workDurationFlow.collectAsStateWithLifecycle(initialValue = 30 * 60)
    val breakDuration by settingsRepository.breakDurationFlow.collectAsStateWithLifecycle(initialValue = 5 * 60)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Work Duration (minutes)", fontWeight = FontWeight.Bold)
            Slider(
                value = (workDuration / 60).toFloat(),
                onValueChange = { newValue ->
                    scope.launch { settingsRepository.setWorkDuration(newValue.toInt() * 60) }
                },
                valueRange = 1f..120f,
                steps = 119
            )
            Text("${workDuration / 60} mins", modifier = Modifier.align(Alignment.End))

            Spacer(modifier = Modifier.height(32.dp))

            Text("Break Duration (minutes)", fontWeight = FontWeight.Bold)
            Slider(
                value = (breakDuration / 60).toFloat(),
                onValueChange = { newValue ->
                    scope.launch { settingsRepository.setBreakDuration(newValue.toInt() * 60) }
                },
                valueRange = 1f..30f,
                steps = 29
            )
            Text("${breakDuration / 60} mins", modifier = Modifier.align(Alignment.End))
        }
    }
}

