package com.example

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine

class MyServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        savedStateRegistryController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
}

class ScreenTimeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var settingsRepository: SettingsRepository
    
    private var isScreenOn = true
    private var tickerJob: Job? = null
    
    private var isBreakActive = false
    private var breakJob: Job? = null

    private var windowManager: WindowManager? = null
    private var breakOverlayView: View? = null
    private var serviceLifecycleOwner: MyServiceLifecycleOwner? = null

    private var floatingTimerView: View? = null
    private var floatingTimerLifecycleOwner: MyServiceLifecycleOwner? = null
    private var isFloatingTimerDismissedForSession = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    startTicker()
                    updateFloatingTimerVisibility()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    stopTicker()
                    updateFloatingTimerVisibility()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundService()
        startTicker()

        // Combine flow collector to automatically add / remove the overlay based on preference changes
        serviceScope.launch {
            combine(
                settingsRepository.floatingTimerEnabledFlow,
                settingsRepository.currentSessionSecondsFlow,
                settingsRepository.workDurationFlow
            ) { enabled, current, limit ->
                Triple(enabled, current, limit)
            }.collect { (enabled, current, limit) ->
                withContext(Dispatchers.Main) {
                    val shouldShow = enabled && isScreenOn && !isBreakActive && !isFloatingTimerDismissedForSession
                    if (shouldShow) {
                        showFloatingTimerOverlay()
                    } else {
                        removeFloatingTimerOverlay()
                    }
                }
            }
        }
    }

    private fun updateFloatingTimerVisibility() {
        serviceScope.launch {
            val enabled = settingsRepository.floatingTimerEnabledFlow.first()
            withContext(Dispatchers.Main) {
                val shouldShow = enabled && isScreenOn && !isBreakActive && !isFloatingTimerDismissedForSession
                if (shouldShow) {
                    showFloatingTimerOverlay()
                } else {
                    removeFloatingTimerOverlay()
                }
            }
        }
    }

    private fun startForegroundService() {
        val channelId = "screen_time_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Time Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Focus Lock Active")
            .setContentText("Monitoring screen time to enforce breaks.")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .build()
        startForeground(101, notification)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive && isScreenOn && !isBreakActive) {
                delay(1000)
                val limit = settingsRepository.workDurationFlow.first()
                val current = settingsRepository.currentSessionSecondsFlow.first()
                if (current >= limit) {
                    // Trigger Break
                    triggerBreak()
                    break
                } else {
                    settingsRepository.setCurrentSessionSeconds(current + 1)
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
    }

    private fun triggerBreak() {
        if (isBreakActive) return
        isBreakActive = true
        isFloatingTimerDismissedForSession = false // Reset dismiss state for next session
        stopTicker()
        
        serviceScope.launch(Dispatchers.Main) {
            removeFloatingTimerOverlay() // Remove floating timer during fullscreen break block
            val breakDuration = withContext(Dispatchers.IO) {
                settingsRepository.breakDurationFlow.first()
            }
            showBreakOverlay(breakDuration)
        }

        breakJob = serviceScope.launch {
            val breakDuration = settingsRepository.breakDurationFlow.first()
            var remaining = breakDuration
            while (isActive && remaining > 0) {
                delay(1000)
                remaining--
            }
            // End break
            settingsRepository.setCurrentSessionSeconds(0)
            isBreakActive = false
            serviceScope.launch(Dispatchers.Main) {
                removeBreakOverlay()
                updateFloatingTimerVisibility()
            }
            if (isScreenOn) startTicker()
        }
    }

    private fun showBreakOverlay(breakDuration: Int) {
        if (breakOverlayView != null) return

        val lifecycleOwner = MyServiceLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        serviceLifecycleOwner = lifecycleOwner
        
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                var secondsLeft by remember { mutableStateOf(breakDuration) }
                LaunchedEffect(Unit) {
                    while (secondsLeft > 0) {
                        delay(1000)
                        secondsLeft--
                    }
                }
                MaterialTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F0F12)), // Cozy modern dark slate background
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                "MANDATORY FOCUS BREAK",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Put your phone down and relax your eyes.",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(48.dp))
                            val minutes = secondsLeft / 60
                            val seconds = secondsLeft % 60
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Break time remaining",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        try {
            windowManager?.addView(composeView, params)
            breakOverlayView = composeView
        } catch (e: Exception) {
            Log.e("ScreenTimeService", "Could not add overlay", e)
        }
    }

    private fun removeBreakOverlay() {
        breakOverlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e("ScreenTimeService", "Could not remove overlay", e)
            }
            breakOverlayView = null
        }
        serviceLifecycleOwner?.let {
            it.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            it.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            it.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            serviceLifecycleOwner = null
        }
    }

    private fun showFloatingTimerOverlay() {
        if (floatingTimerView != null) return

        val lifecycleOwner = MyServiceLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        floatingTimerLifecycleOwner = lifecycleOwner

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 150

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                val currentSessionSeconds by settingsRepository.currentSessionSecondsFlow.collectAsState(initial = 0)
                val workDuration by settingsRepository.workDurationFlow.collectAsState(initial = 30 * 60)
                
                val remaining = (workDuration - currentSessionSeconds).coerceAtLeast(0)
                val minutes = remaining / 60
                val seconds = remaining % 60

                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                MaterialTheme {
                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    params.x += dragAmount.x.toInt()
                                    params.y += dragAmount.y.toInt()
                                    try {
                                        windowManager?.updateViewLayout(this@apply, params)
                                    } catch (e: Exception) {
                                        Log.e("ScreenTimeService", "Error dragging floating view", e)
                                    }
                                }
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xE61E1E24))
                            .clickable { /* Prevents click-through while dragging */ }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF4CAF50))
                                .alpha(pulseAlpha)
                        )

                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF33333C))
                                .clickable {
                                    isFloatingTimerDismissedForSession = true
                                    updateFloatingTimerVisibility()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.LightGray,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        try {
            windowManager?.addView(composeView, params)
            floatingTimerView = composeView
        } catch (e: Exception) {
            Log.e("ScreenTimeService", "Could not add floating timer", e)
        }
    }

    private fun removeFloatingTimerOverlay() {
        floatingTimerView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e("ScreenTimeService", "Could not remove floating timer", e)
            }
            floatingTimerView = null
        }
        floatingTimerLifecycleOwner?.let {
            it.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            it.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            it.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            floatingTimerLifecycleOwner = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        serviceScope.cancel()
        removeBreakOverlay()
        removeFloatingTimerOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, ScreenTimeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
