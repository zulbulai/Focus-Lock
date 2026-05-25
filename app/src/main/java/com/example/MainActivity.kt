package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.SettingsRepository
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

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
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to start ScreenTimeService safely", e)
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
            LargeTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Focus Lock", 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick = onNavigateSettings,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings, 
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Circular timer progress card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val remaining = (workDuration - currentSessionSeconds).coerceAtLeast(0)
                    val minutes = remaining / 60
                    val seconds = remaining % 60
                    val progress = if (workDuration > 0) {
                        currentSessionSeconds.toFloat() / workDuration.toFloat()
                    } else 0f

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(220.dp)
                    ) {
                        // Background circle
                        CircularProgressIndicator(
                            progress = 1f,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 14.dp,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                        // Foreground dynamic progress
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 14.dp,
                            color = MaterialTheme.colorScheme.primary,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        // Centered text
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "REMAINING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Focus Timer Session Progress",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            "Status Bar Control", 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "You can also pause / start directly in your notification panel.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            QuickSettingsTileGuideCard()

            Spacer(modifier = Modifier.height(8.dp))

            // Main Action Buttons
            Button(
                onClick = onStartService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Tracking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onStopService,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop Tracking / Reset", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            DeveloperFooter()

            Spacer(modifier = Modifier.height(24.dp))
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
    val floatingTimerEnabled by settingsRepository.floatingTimerEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val warningSoundEnabled by settingsRepository.warningSoundEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val strictModeEnabled by settingsRepository.strictModeEnabledFlow.collectAsStateWithLifecycle(initialValue = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings (सेटिंग्स)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // Work Duration Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Work Duration (काम का समय)", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var sliderWorkValue by remember(workDuration) { mutableStateOf((workDuration / 60).toFloat()) }
                    Slider(
                        value = sliderWorkValue,
                        onValueChange = { sliderWorkValue = it },
                        onValueChangeFinished = {
                            scope.launch { settingsRepository.setWorkDuration(sliderWorkValue.toInt() * 60) }
                        },
                        valueRange = 1f..120f,
                        steps = 119
                    )
                    
                    Text(
                        "${sliderWorkValue.toInt()} minutes", 
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Preset Shortcuts (त्वरित विकल्प):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Prest shortcuts for focus session
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 20, 30, 45, 60).forEach { mins ->
                            val active = (workDuration / 60) == mins
                            SuggestionChip(
                                onClick = {
                                    scope.launch { settingsRepository.setWorkDuration(mins * 60) }
                                },
                                label = { Text("${mins}m") },
                                border = if (active) androidx.compose.foundation.BorderStroke(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                ) else androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = if (active) SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) else SuggestionChipDefaults.suggestionChipColors()
                            )
                        }
                    }
                }
            }

            // Break Duration Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Break Duration (ब्रेक का समय)", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var sliderBreakValue by remember(breakDuration) { mutableStateOf((breakDuration / 60).toFloat()) }
                    Slider(
                        value = sliderBreakValue,
                        onValueChange = { sliderBreakValue = it },
                        onValueChangeFinished = {
                            scope.launch { settingsRepository.setBreakDuration(sliderBreakValue.toInt() * 60) }
                        },
                        valueRange = 1f..30f,
                        steps = 29
                    )
                    
                    Text(
                        "${sliderBreakValue.toInt()} minutes", 
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Preset Shortcuts (त्वरित विकल्प):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Preset shortcuts for focus break
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 5, 10).forEach { mins ->
                            val active = (breakDuration / 60) == mins
                            SuggestionChip(
                                onClick = {
                                    scope.launch { settingsRepository.setBreakDuration(mins * 60) }
                                },
                                label = { Text("${mins}m") },
                                border = if (active) androidx.compose.foundation.BorderStroke(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                ) else androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = if (active) SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) else SuggestionChipDefaults.suggestionChipColors()
                            )
                        }
                    }
                }
            }

            // General Settings List Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "Advanced Options (अतिरिक्त सेटिंग्स)", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    // 1. Floating Timer Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Floating Timer Overlay", fontWeight = FontWeight.Bold)
                            Text(
                                "Show draggable minutes timer badge over other applications.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = floatingTimerEnabled,
                            onCheckedChange = { isChecked ->
                                scope.launch { settingsRepository.setFloatingTimerEnabled(isChecked) }
                            }
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 2. 3-Second Warning Buzzer Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("3-Sec Warning Beep (चेतावनी टोन)", fontWeight = FontWeight.Bold)
                            Text(
                                "Play an alert sound 3 seconds before focus session finishes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = warningSoundEnabled,
                            onCheckedChange = { isChecked ->
                                scope.launch { settingsRepository.setWarningSoundEnabled(isChecked) }
                            }
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 3. Strict focus mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Strict Lock Mode (कठोर अनुशासन)", fontWeight = FontWeight.Bold)
                            Text(
                                "Force absolute screen locks over apps. Restrict quick bypasses during break.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = strictModeEnabled,
                            onCheckedChange = { isChecked ->
                                scope.launch { settingsRepository.setStrictModeEnabled(isChecked) }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DeveloperFooter()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DeveloperFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Button(
        onClick = {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/jitendrauno"))
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                context.startActivity(intent)
            } catch (e: Exception) {
                // fallback
            }
        },
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Follow on Instagram @jitendrauno",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun QuickSettingsTileGuideCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isNativelySupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    Button(
        onClick = {
            if (isNativelySupported) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val statusBarManager = context.getSystemService(android.app.StatusBarManager::class.java)
                        val componentName = ComponentName(context, FocusLockTileService::class.java)
                        statusBarManager?.requestAddTileService(
                            componentName,
                            "Focus Lock",
                            android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_lock_lock),
                            context.mainExecutor
                        ) { _ -> }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error requesting tile service", e)
                }
            } else {
                android.widget.Toast.makeText(
                    context, 
                    "Status Bar को नीचे स्वाइप कर 'Edit' बटन से 'Focus Lock' जोड़ें!", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        },
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Status Bar में शर्टकट जोड़ें (Add Quick Status Tile)",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
