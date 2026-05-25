package com.example

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class FocusLockTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val active = ScreenTimeService.isRunning
        val tile = qsTile ?: return
        if (active) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Focus Lock (On)"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Tracking active"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Focus Lock"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Tap to setup/start"
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        val active = ScreenTimeService.isRunning
        if (active) {
            val intent = Intent(context, ScreenTimeService::class.java)
            stopService(intent)
        } else {
            val intent = Intent(context, ScreenTimeService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("FocusLockTileService", "Failed to start service safely", e)
            }
        }
        
        // Small delay to let service state change, then update tile state
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            updateTileState()
        }, 300)
    }
}
