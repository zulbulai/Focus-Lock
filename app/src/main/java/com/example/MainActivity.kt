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
import androidx.compose.foundation.border
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
                val database = remember { com.example.data.AppDatabase.getDatabase(applicationContext) }
                com.example.ui.UnifiedWorkspace(
                    database = database,
                    settingsRepository = settingsRepository,
                    onStartService = { checkAndStartService() },
                    onStopService = { stopScreenTimeService() },
                    onNavigateSettings = {}
                )
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
    onNavigateSettings: () -> Unit,
    onNavigateHabits: () -> Unit,
    onNavigateTasks: () -> Unit
) {
    val currentSessionSeconds by settingsRepository.currentSessionSecondsFlow.collectAsStateWithLifecycle(initialValue = 0)
    val workDuration by settingsRepository.workDurationFlow.collectAsStateWithLifecycle(initialValue = 30 * 60)
    val fallbackSecs by settingsRepository.fallbackScreenOnSecondsFlow.collectAsStateWithLifecycle(initialValue = 11245)

    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Real-time task synchronization
    val dashboardDb = remember(context) { com.example.data.AppDatabase.getDatabase(context) }
    val dashboardTasksFlow = remember(dashboardDb) { dashboardDb.taskDao().getAllTasks() }
    val dashboardTasks by dashboardTasksFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val totalTasks = dashboardTasks.size
    val completedTasks = dashboardTasks.count { it.isCompleted }
    val pendingTasks = totalTasks - completedTasks
    val highPriorityPending = dashboardTasks.count { !it.isCompleted && it.priority == "High" }
    val tasksProgress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks.toFloat() else 0f
    val tasksProgressPercentText = "${(tasksProgress * 100).toInt()}%"
    var hasPermission by remember { mutableStateOf(false) }
    var usageStats by remember { mutableStateOf<com.example.data.UsageStatsHelper.DynamicUsageSummary?>(null) }

    LaunchedEffect(context) {
        while (true) {
            hasPermission = com.example.data.UsageStatsHelper.hasUsageStatsPermission(context)
            if (hasPermission) {
                usageStats = com.example.data.UsageStatsHelper.getTodayUsageStats(context)
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    val totalSeconds = if (hasPermission && usageStats != null) usageStats!!.totalSeconds else fallbackSecs.toLong()
    val totalHrs = totalSeconds / 3600
    val totalMins = (totalSeconds % 3600) / 60
    val formattedTotalTime = if (totalHrs > 0) "${totalHrs} hr, ${totalMins} min" else "${totalMins} min"

    val topApps = if (hasPermission && usageStats != null) {
        usageStats!!.topApps
    } else {
        listOf(
            com.example.data.UsageStatsHelper.RealAppUsage("com.android.chrome", "Chrome", (totalSeconds * 0.55).toLong(), "Browsing"),
            com.example.data.UsageStatsHelper.RealAppUsage("com.instagram.android", "Socials", (totalSeconds * 0.11).toLong(), "Social"),
            com.example.data.UsageStatsHelper.RealAppUsage("com.sec.android.gallery3d", "Gallery", (totalSeconds * 0.09).toLong(), "Multimedia")
        )
    }

    val paddedTopApps = remember(topApps, totalSeconds) {
        val list = topApps.toMutableList()
        if (list.size < 1) {
            list.add(com.example.data.UsageStatsHelper.RealAppUsage("com.android.chrome", "Chrome", (totalSeconds * 0.55).toLong(), "Browsing"))
        }
        if (list.size < 2) {
            list.add(com.example.data.UsageStatsHelper.RealAppUsage("com.instagram.android", "Socials", (totalSeconds * 0.11).toLong(), "Social"))
        }
        if (list.size < 3) {
            list.add(com.example.data.UsageStatsHelper.RealAppUsage("com.sec.android.gallery3d", "Gallery", (totalSeconds * 0.09).toLong(), "Multimedia"))
        }
        list.take(3)
    }

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
                        modifier = Modifier.size(200.dp)
                    ) {
                        // Background circle
                        CircularProgressIndicator(
                            progress = 1f,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 12.dp,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                        // Foreground dynamic progress
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 12.dp,
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Focus Timer Session Progress",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 📝 Real-Time Task Organizer Launcher Card 📝
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTasks() },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF141A21) // Premium Dark Slate
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Oval Badge for Task Organizer
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Task Manager",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Status indicator text on right
                        val statusText = if (pendingTasks > 0) "🎯 $pendingTasks Pending" else "✨ Clean list!"
                        Text(
                            text = statusText,
                            color = if (highPriorityPending > 0) Color(0xFFEF5350) else Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bold task headline statistics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = if (totalTasks > 0) "$completedTasks / $totalTasks Done" else "Add Tasks",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "COMPLETION RATE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Task Progress Bar
                    LinearProgressIndicator(
                        progress = tasksProgress,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Hindi slogan / descriptive label matching professional hindi tone
                    val promptSlogan = if (highPriorityPending > 0) {
                        "काम को प्राथमिकता दें: $highPriorityPending आवश्यक कार्य लंबित हैं"
                    } else if (pendingTasks > 0) {
                        "शानदार! अपने दैनिक कार्य व्यवस्थित करें"
                    } else {
                        "उत्कृष्ट! अपने नए कार्य जोड़ने के लिए यहां क्लिक करें"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (pendingTasks > 0) Color(0xFFFFB74D) else MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = promptSlogan,
                            color = Color.LightGray.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 🌟 Habit Tracker Premium Launch Card 🌟
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateHabits() },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF141414)
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.05f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE91E63).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFE91E63).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Habit Tracker (आदत ट्रैकर)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Track daily atomic habits, success streaks, and visual progress indicators in a pro-level dark layout.",
                            fontSize = 12.sp,
                            color = Color.LightGray.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Habits",
                        tint = Color.LightGray
                    )
                }
            }

            // Status bar tile controller trigger button
            QuickSettingsTileGuideCard()

            Spacer(modifier = Modifier.height(4.dp))

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
