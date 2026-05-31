package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnifiedWorkspace(
    database: AppDatabase,
    settingsRepository: SettingsRepository,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 🌟 ACTIVE TAB STATE - 0: Clock, 1: Tasks, 2: Habits, 3: Alerts/Sounds 🌟
    var activeTab by remember { mutableStateOf(0) }

    // State bindings
    val currentSessionSeconds by settingsRepository.currentSessionSecondsFlow.collectAsStateWithLifecycle(initialValue = 0)
    val workDuration by settingsRepository.workDurationFlow.collectAsStateWithLifecycle(initialValue = 30 * 60)
    val breakDuration by settingsRepository.breakDurationFlow.collectAsStateWithLifecycle(initialValue = 5 * 60)
    val floatingTimerEnabled by settingsRepository.floatingTimerEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val warningSoundEnabled by settingsRepository.warningSoundEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val strictModeEnabled by settingsRepository.strictModeEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val selectedAlertTone by settingsRepository.selectedAlertToneFlow.collectAsStateWithLifecycle(initialValue = 3)

    // Room tasks binding
    val tasksFlow = remember { database.taskDao().getAllTasks() }
    val tasks by tasksFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Room habits binding
    val habitsFlow = remember { database.habitDao().getAllHabits() }
    val habits by habitsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val logsFlow = remember { database.habitDao().getAllLogs() }
    val logs by logsFlow.collectAsStateWithLifecycle(initialValue = emptyList())



    Scaffold(
        topBar = {
            // App Name strictly positioned at TOP LEFT with clean Pro elements
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Focus Lock",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Premium dynamic badge
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFFFB300), Color(0xFFFF9100))
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                }
                            }
                            Text(
                                text = "Ultimate Self-Discipline Suite",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    // Instagram profile redirect button
                    IconButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://instagram.com/jitendrauno")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Follow Developer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F141C) // Premium dark background
                )
            )
        },
        bottomBar = {
            // Unified workspace custom segmented M3 Bottom navigation view linking ALL tools
            NavigationBar(
                containerColor = Color(0xFF0D1117),
                tonalElevation = 8.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = "Clock") },
                    label = { Text("फोकस घड़ी", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFB300),
                        selectedTextColor = Color(0xFFFFB300),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray,
                        indicatorColor = Color(0xFFFFB300).copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { 
                        BadgedBox(badge = {
                            val activePendingCount = tasks.count { !it.isCompleted }
                            if (activePendingCount > 0) {
                                Badge(containerColor = Color(0xFFEF5350)) {
                                    Text(activePendingCount.toString(), color = Color.White)
                                }
                            }
                        }) {
                            Icon(Icons.Default.Checklist, contentDescription = "Tasks")
                        }
                    },
                    label = { Text("कार्य लिस्ट", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4CAF50),
                        selectedTextColor = Color(0xFF4CAF50),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray,
                        indicatorColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = "Habits") },
                    label = { Text("आदतें", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE91E63),
                        selectedTextColor = Color(0xFFE91E63),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray,
                        indicatorColor = Color(0xFFE91E63).copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = "Alert Tones") },
                    label = { Text("ट्यून्स सेटिंग्स", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00B0FF),
                        selectedTextColor = Color(0xFF00B0FF),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray,
                        indicatorColor = Color(0xFF00B0FF).copy(alpha = 0.15f)
                    )
                )
            }
        },
        containerColor = Color(0xFF0A0F16) // Premium slate-coal background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                },
                label = "WorkspaceTabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> FocusTimerWorkspace(
                        settingsRepository = settingsRepository,
                        workDuration = workDuration,
                        currentSessionSeconds = currentSessionSeconds,
                        breakDuration = breakDuration,
                        floatingTimerEnabled = floatingTimerEnabled,
                        strictModeEnabled = strictModeEnabled,
                        warningSoundEnabled = warningSoundEnabled,
                        onStartService = onStartService,
                        onStopService = onStopService,
                        scope = scope
                    )
                    1 -> TasksWorkspace(
                        database = database,
                        tasks = tasks,
                        scope = scope
                    )
                    2 -> HabitsWorkspace(
                        database = database,
                        habits = habits,
                        logs = logs,
                        scope = scope
                    )
                    3 -> AlertSoundsWorkspace(
                        settingsRepository = settingsRepository,
                        selectedAlertTone = selectedAlertTone,
                        warningSoundEnabled = warningSoundEnabled,
                        scope = scope
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. FOCUS TIMER WORKSPACE (TAB 0)
// ==========================================
@Composable
fun FocusTimerWorkspace(
    settingsRepository: SettingsRepository,
    workDuration: Int,
    currentSessionSeconds: Int,
    breakDuration: Int,
    floatingTimerEnabled: Boolean,
    strictModeEnabled: Boolean,
    warningSoundEnabled: Boolean,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var hasPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            hasPermission = com.example.data.UsageStatsHelper.hasUsageStatsPermission(context)
            kotlinx.coroutines.delay(2000)
        }
    }

    val remaining = (workDuration - currentSessionSeconds).coerceAtLeast(0)
    val minutes = remaining / 60
    val seconds = remaining % 60
    val progress = if (workDuration > 0) {
        currentSessionSeconds.toFloat() / workDuration.toFloat()
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Circular Visual Timer Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A24)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                    // Premium background ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color(0xFFFFB300).copy(alpha = 0.08f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    // Animated gradient foreground ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFF8F00), Color(0xFFFFD54F))
                            ),
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    // Clock labels
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "शेष समय (REMAINING)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300),
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Control Action Indicators
                val isActiveMode = com.example.ScreenTimeService.isRunning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${workDuration / 60} मिनट",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("कुल लक्ष्य", fontSize = 11.sp, color = Color.Gray)
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isActiveMode) "सक्रिय (ON)" else "रूका (OFF)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActiveMode) Color(0xFF4CAF50) else Color(0xFFEF5350)
                        )
                        Text("ताला स्थिति", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Duration Adjustment Options Inline
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A24)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "काम की समय-अवधि (Focus Limit)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${workDuration / 60} मिनट",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFB300)
                    )
                }

                var sliderVal by remember(workDuration) { mutableStateOf((workDuration / 60).toFloat()) }
                Slider(
                    value = sliderVal,
                    onValueChange = { sliderVal = it },
                    onValueChangeFinished = {
                        scope.launch { settingsRepository.setWorkDuration(sliderVal.toInt() * 60) }
                    },
                    valueRange = 1f..120f,
                    steps = 119,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color(0xFFFFB300),
                        inactiveTrackColor = Color.White.copy(alpha = 0.08f),
                        thumbColor = Color(0xFFFFB300)
                    )
                )

                // Prest pills durations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 25, 45, 60).forEach { mins ->
                        val active = (workDuration / 60) == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Color(0xFFFFB300).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                .border(
                                    width = 1.dp,
                                    color = if (active) Color(0xFFFFB300) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    scope.launch { settingsRepository.setWorkDuration(mins * 60) }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}म",
                                color = if (active) Color(0xFFFFB300) else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Custom Quick Start & Stop Dashboard Control Triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onStartService,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("स्टार्ट (Start)", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onStopService,
                modifier = Modifier
                    .weight(1.5f)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("बंद / रीसेट करें", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Quick Advanced Configuration Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A24)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "अतिरिक्त नियंत्रण (Additional Tuning)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.LightGray
                )

                // Draggable floating timer overlay view
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("फ्लोटिंग टाइमर (Floating Badge)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("अन्य ऐप्स के ऊपर लाइव टाइमर दिखाता है", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = floatingTimerEnabled,
                        onCheckedChange = { isChecked ->
                            scope.launch { settingsRepository.setFloatingTimerEnabled(isChecked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFFB300),
                            checkedTrackColor = Color(0xFFFFB300).copy(alpha = 0.3f)
                        )
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Strict Discipline Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("कठोर ताला (Strict Bypass Lock)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("ब्रेक ख़त्म होने से पहले बाईपास रोकता है", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = strictModeEnabled,
                        onCheckedChange = { isChecked ->
                            scope.launch { settingsRepository.setStrictModeEnabled(isChecked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFFB300),
                            checkedTrackColor = Color(0xFFFFB300).copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

// ==========================================
// 2. ADVANCED TASK BOARD WORKSPACE (TAB 1)
// ==========================================
@Composable
fun TasksWorkspace(
    database: AppDatabase,
    tasks: List<TaskEntity>,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") } // All, High, Pending, Completed

    val filteredTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            "High" -> tasks.filter { it.priority == "High" && !it.isCompleted }
            "Pending" -> tasks.filter { !it.isCompleted }
            "Completed" -> tasks.filter { it.isCompleted }
            else -> tasks
        }
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Segmented task filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All" to "सभी", "High" to "🔴 उच्च priority", "Pending" to "⏳ जारी", "Completed" to "✅ पूर्ण").forEach { (key, label) ->
                val active = selectedFilter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFF131A24))
                        .border(
                            width = 1.dp,
                            color = if (active) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedFilter = key }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = label,
                        color = if (active) Color(0xFF4CAF50) else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic empty state indicator
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "कोई कार्य नहीं मिला (No tasks here)",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "नए कार्य जोड़ने के लिए '+' बटन दबाएँ",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onCheckedChange = {
                            scope.launch {
                                val updated = task.copy(isCompleted = !task.isCompleted)
                                database.taskDao().updateTask(updated)
                            }
                        },
                        onDelete = {
                            scope.launch {
                                database.taskDao().deleteTask(task)
                                Toast.makeText(context, "कार्य हटाया गया", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Premium Add Task Trigger Fab Button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(6.dp))
            Text("नया कार्य जोड़े (Add Task)", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, prio, cat ->
                scope.launch {
                    val task = TaskEntity(
                        title = title,
                        description = desc,
                        priority = prio,
                        category = cat
                    )
                    database.taskDao().insertTask(task)
                    showAddDialog = false
                    Toast.makeText(context, "नया कार्य जोड़ा गया!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun TaskCard(
    task: TaskEntity,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A24)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF4CAF50),
                    checkmarkColor = Color.Black
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) Color.Gray else Color.White,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Priority Pill Tag Indicator
                    val (tagCol, textP) = when (task.priority) {
                        "High" -> Color(0xFFEF5350).copy(alpha = 0.15f) to "High"
                        "Low" -> Color(0xFF00B0FF).copy(alpha = 0.15f) to "Low"
                        else -> Color(0xFFFFB300).copy(alpha = 0.15f) to "Medium"
                    }
                    val textCol = when (task.priority) {
                        "High" -> Color(0xFFEF5350)
                        "Low" -> Color(0xFF00B0FF)
                        else -> Color(0xFFFFB300)
                    }
                    Box(
                        modifier = Modifier
                            .background(tagCol, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = textP,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = textCol
                        )
                    }
                }
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        fontSize = 11.sp,
                        color = Color.LightGray.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") } // High, Medium, Low
    var category by remember { mutableStateOf("Work") } // Work, Study, Health

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A24)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "नया कार्य जोड़ें (Add Task)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("शीर्षक (Title)*") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("विवरण (Description)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority Selection
                Text("प्राथमिकता (Priority)", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Low" to "Low", "Medium" to "Med", "High" to "High").forEach { (key, name) ->
                        val active = priority == key
                        val activeCol = when (key) {
                            "High" -> Color(0xFFEF5350)
                            "Low" -> Color(0xFF00B0FF)
                            else -> Color(0xFFFFB300)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) activeCol.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                .border(
                                    width = 1.dp,
                                    color = if (active) activeCol else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { priority = key }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) activeCol else Color.LightGray
                            )
                        }
                    }
                }

                // Category selection Row
                Text("श्रेणी (Category)", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Work", "Study", "Personal").forEach { cat ->
                        val active = category == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                .border(
                                    width = 1.dp,
                                    color = if (active) Color(0xFF4CAF50) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { category = cat }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color(0xFF4CAF50) else Color.LightGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("रद्द करें (Cancel)", color = Color.LightGray)
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(title, desc, priority, category)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("सहेजें (Save)", color = Color.Black)
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. PREMIUM DISCIPLINE HABITS WORKSPACE (TAB 2)
// ==========================================
@Composable
fun HabitsWorkspace(
    database: AppDatabase,
    habits: List<HabitEntity>,
    logs: List<HabitLogEntity>,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var showHabitDialog by remember { mutableStateOf(false) }
    var selectedDateStr by remember { mutableStateOf(getUnifiedWorkspaceTodayDate()) }

    val logsForDate = remember(logs, selectedDateStr) {
        logs.filter { it.dateString == selectedDateStr }
    }

    val context = LocalContext.current
    val daysOfWeek = remember { getUnifiedWorkspaceWeekDays() }

    val habitsCount = habits.size
    val completedCount = habits.filter { h -> logsForDate.any { it.habitId == h.id } }.size
    val progress = if (habitsCount > 0) completedCount.toFloat() / habitsCount.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Horizontal elegant Calendar Strip for selecting days of week
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach { day ->
                val isSelected = selectedDateStr == day.dateStr
                val isToday = getUnifiedWorkspaceTodayDate() == day.dateStr
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) Color(0xFFE91E63)
                            else if (isToday) Color(0xFFE91E63).copy(alpha = 0.1f)
                            else Color(0xFF131A24)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFFE91E63) else if (isToday) Color(0xFFE91E63).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedDateStr = day.dateStr }
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                        .weight(1f)
                        .padding(horizontal = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day.nameBrief,
                            color = if (isSelected) Color.Black else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = day.dayOfMonth,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Summary Tile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A24)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // visual circular statistics gauge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(54.dp)
                ) {
                    CircularProgressIndicator(
                        progress = 1f,
                        color = Color.White.copy(alpha = 0.05f),
                        strokeWidth = 6.dp
                    )
                    CircularProgressIndicator(
                        progress = progress,
                        color = Color(0xFFE91E63),
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "दैनिक अनुशासन (Atomic Discipline)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = "$completedCount / $habitsCount आदतें आज पूर्ण (Completed)",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active Habits Checklist listing
        if (habits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "कोई आदत नहीं मिली (No habits here)",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "नई आदत जोड़ने के लिए 'नई आदत जोड़े' बटन दबाएँ",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(habits, key = { it.id }) { habit ->
                    val isDone = logsForDate.any { it.habitId == habit.id }
                    HabitCard(
                        habit = habit,
                        isDone = isDone,
                        onToggle = {
                            scope.launch {
                                if (isDone) {
                                    database.habitDao().deleteLog(habit.id, selectedDateStr)
                                    vibratePhone(context, 40)
                                } else {
                                    database.habitDao().insertLog(
                                        HabitLogEntity(
                                            habitId = habit.id,
                                            dateString = selectedDateStr,
                                            isCompleted = true
                                        )
                                    )
                                    vibratePhone(context, 120)
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                database.habitDao().deleteHabit(habit)
                                Toast.makeText(context, "आदत हटाई गई", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fab button to add new high-productivity streak building habits
        Button(
            onClick = { showHabitDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(6.dp))
            Text("नई आदत जोड़े (Add Habit)", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }

    if (showHabitDialog) {
        AddHabitDialog(
            onDismiss = { showHabitDialog = false },
            onSave = { title, desc, colHex, cat ->
                scope.launch {
                    val habit = HabitEntity(
                        title = title,
                        description = desc,
                        colorHex = colHex,
                        category = cat
                    )
                    database.habitDao().insertHabit(habit)
                    showHabitDialog = false
                    Toast.makeText(context, "आदत सहेजी गई!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun HabitCard(
    habit: HabitEntity,
    isDone: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val hColor = remember(habit.colorHex) {
        try { Color(android.graphics.Color.parseColor(habit.colorHex)) } catch (e: Exception) { Color(0xFFE91E63) }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A24)),
        border = BorderStroke(1.dp, if (isDone) hColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Checklist Complete Circle checkmark trigger bubble
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDone) hColor else Color.White.copy(alpha = 0.04f))
                    .border(1.5.dp, if (isDone) hColor else Color.Gray.copy(alpha = 0.4f), CircleShape)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed habit",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) Color.Gray else Color.White,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (habit.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = habit.description,
                        fontSize = 11.sp,
                        color = Color.LightGray.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Simple delete trigger
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete habit",
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var colorSelected by remember { mutableStateOf("#E91E63") }
    var category by remember { mutableStateOf("Health") }

    val colorsGroup = listOf("#E91E63", "#FF9100", "#FFB300", "#4CAF50", "#00B0FF", "#9C27B0")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A24)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "नई आदत बनाएँ (Create Habit)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("आदत का नाम (Habit Title)*") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE91E63),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = Color(0xFFE91E63)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("विवरण (Objective Description)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE91E63),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = Color(0xFFE91E63)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category select
                Text("आदत श्रेणी (Category)", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Health", "Mind", "Work").forEach { cat ->
                        val active = category == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Color(0xFFE91E63).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                .border(
                                    width = 1.dp,
                                    color = if (active) Color(0xFFE91E63) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { category = cat }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color(0xFFE91E63) else Color.LightGray
                            )
                        }
                    }
                }

                // Color selects list
                Text("रंग थीम (Accent Color Tag)", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorsGroup.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = colorSelected == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorSelected = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("रद्द करें", color = Color.LightGray)
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(title, desc, colorSelected, category)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("सहेजें (Save)", color = Color.Black)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. PHONE SOUNDS, TOONS & ALERTS CONFIGURATION (TAB 3)
// ==========================================
@Composable
fun AlertSoundsWorkspace(
    settingsRepository: SettingsRepository,
    selectedAlertTone: Int,
    warningSoundEnabled: Boolean,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val context = LocalContext.current

    val tonesList = listOf(
        1 to "Zen Bell Pip (चिड़िया की चहचहाहट)",
        2 to "Double Soft Pulse (धीमी सुरीली आवाज़)",
        3 to "High Siren Alarm (तेज़ बज़र अलार्म)",
        4 to "Digital Soft Buzz (डिजिटल बीप)",
        5 to "Echo Wave Alert (इको साउंड वेव)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Notification & Tone Header Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A24)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF00B0FF),
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "अलार्म और टोन सेटिंग्स (Phones Tunes & Toons)",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            "Set physical tone alarms for focus boundaries",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Tones Checklist Selector panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A24)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "फ़ोन की चेतावनी टोन चुनें (Select Tone Alert)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                tonesList.forEach { (id, label) ->
                    val isChosen = selectedAlertTone == id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isChosen) Color(0xFF00B0FF).copy(alpha = 0.08f) else Color.Transparent)
                            .clickable {
                                scope.launch {
                                    settingsRepository.setSelectedAlertTone(id)
                                    playAlertTonePreview(context, id)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            RadioButton(
                                selected = isChosen,
                                onClick = {
                                    scope.launch {
                                        settingsRepository.setSelectedAlertTone(id)
                                        playAlertTonePreview(context, id)
                                    }
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF00B0FF),
                                    unselectedColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isChosen) FontWeight.Black else FontWeight.Normal,
                                color = if (isChosen) Color.White else Color.LightGray
                            )
                        }

                        // Play/Test sounding icon button
                        IconButton(
                            onClick = { playAlertTonePreview(context, id) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White.copy(alpha = 0.04f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Listen tune",
                                tint = if (isChosen) Color(0xFF00B0FF) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Divider(color = Color.White.copy(alpha = 0.02f))
                }
            }
        }

        // Additional Premium Notification tuning controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131A24)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "अधिसूचना नियंत्रण (Notification Control)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.LightGray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("चेतावनी अलार्म बजाएँ (Play Beep)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("3 सेकंड बचे होने पर ऑटो अलार्म टोन बजाएँ", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = warningSoundEnabled,
                        onCheckedChange = { isChecked ->
                            scope.launch { settingsRepository.setWarningSoundEnabled(isChecked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00B0FF),
                            checkedTrackColor = Color(0xFF00B0FF).copy(alpha = 0.3f)
                        )
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Vibration Test Control trigger
                Button(
                    onClick = { vibratePhone(context, 400) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2936))
                ) {
                    Icon(Icons.Default.Vibration, contentDescription = null, tint = Color.LightGray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("वाइब्रेशन टेस्ट करें (Test Phone Vibration)", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// Supporting helper functions for alert tone audio simulation
fun playAlertTonePreview(context: Context, toneId: Int) {
    try {
        val toneType = when (toneId) {
            1 -> android.media.ToneGenerator.TONE_CDMA_PIP
            2 -> android.media.ToneGenerator.TONE_PROP_PROMPT
            3 -> android.media.ToneGenerator.TONE_CDMA_HIGH_L
            4 -> android.media.ToneGenerator.TONE_PROP_BEEP
            5 -> android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT
            else -> android.media.ToneGenerator.TONE_CDMA_PIP
        }
        val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 90)
        toneGen.startTone(toneType, 400)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try { toneGen.release() } catch (ex: Exception) {}
        }, 500)
    } catch (e: Exception) {
        Toast.makeText(context, "मैसेज ट्यून सिमुलेशन में त्रुटि", Toast.LENGTH_SHORT).show()
    }
}

fun vibratePhone(context: Context, durationMs: Long) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    } catch (ex: Exception) {}
}

// Supporting datetime format helpers
fun getUnifiedWorkspaceTodayDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

data class DayOfWeekInfo(
    val dateStr: String,
    val dayOfMonth: String,
    val nameBrief: String
)

fun getUnifiedWorkspaceWeekDays(): List<DayOfWeekInfo> {
    val list = mutableListOf<DayOfWeekInfo>()
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDay = SimpleDateFormat("d", Locale.getDefault())
    val sdfName = SimpleDateFormat("E", Locale.getDefault())
    for (i in 0..6) {
        list.add(
            DayOfWeekInfo(
                dateStr = sdf.format(cal.time),
                dayOfMonth = sdfDay.format(cal.time),
                nameBrief = sdfName.format(cal.time).uppercase()
            )
        )
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return list
}


