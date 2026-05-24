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
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.data.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class ScreenTimeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var settingsRepository: SettingsRepository
    
    private var isScreenOn = true
    private var tickerJob: Job? = null
    
    private var isBreakActive = false
    private var breakJob: Job? = null

    private var windowManager: WindowManager? = null
    private var breakOverlayView: View? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    startTicker()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    stopTicker()
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
        stopTicker()
        
        serviceScope.launch(Dispatchers.Main) {
            showBreakOverlay()
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
            }
            if (isScreenOn) startTicker()
        }
    }

    private fun showBreakOverlay() {
        if (breakOverlayView != null) return
        
        val composeView = ComposeView(this).apply {
            setContent {
                MaterialTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Time for a Break", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Put your phone down and relax.", color = Color.White, fontSize = 16.sp)
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
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
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        serviceScope.cancel()
        removeBreakOverlay()
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
