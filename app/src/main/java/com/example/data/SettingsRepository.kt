package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val WORK_DURATION = intPreferencesKey("work_duration")
        val BREAK_DURATION = intPreferencesKey("break_duration")
        val CURRENT_SESSION_SECONDS = intPreferencesKey("current_session_seconds")
        val FLOATING_TIMER_ENABLED = booleanPreferencesKey("floating_timer_enabled")
        val WARNING_SOUND_ENABLED = booleanPreferencesKey("warning_sound_enabled")
        val STRICT_MODE_ENABLED = booleanPreferencesKey("strict_mode_enabled")
        val SELECTED_ALERT_TONE = intPreferencesKey("selected_alert_tone")
        
        // 📈 Screen Time Analytics Keys 📈
        val SCREEN_TIME_GOAL_MINS = intPreferencesKey("screen_time_goal_mins")
        val CHROME_LIMIT_MINS = intPreferencesKey("chrome_limit_mins")
        val SOCIAL_LIMIT_MINS = intPreferencesKey("social_limit_mins")
        val BEDTIME_MODE_ACTIVE = booleanPreferencesKey("bedtime_mode_active")
        val FALLBACK_SCREEN_ON_SECONDS = intPreferencesKey("fallback_screen_on_seconds")
    }

    val workDurationFlow: Flow<Int> = context.dataStore.data.map { it[WORK_DURATION] ?: (30 * 60) }
    val breakDurationFlow: Flow<Int> = context.dataStore.data.map { it[BREAK_DURATION] ?: (5 * 60) }
    val currentSessionSecondsFlow: Flow<Int> = context.dataStore.data.map { it[CURRENT_SESSION_SECONDS] ?: 0 }
    val floatingTimerEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[FLOATING_TIMER_ENABLED] ?: true }
    val warningSoundEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[WARNING_SOUND_ENABLED] ?: true }
    val strictModeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[STRICT_MODE_ENABLED] ?: false }
    val selectedAlertToneFlow: Flow<Int> = context.dataStore.data.map { it[SELECTED_ALERT_TONE] ?: 3 } // Default to CDMA_HIGH_L (3) or similar for loud toon!

    // Screen Time Flows
    val screenTimeGoalMinsFlow: Flow<Int> = context.dataStore.data.map { it[SCREEN_TIME_GOAL_MINS] ?: 300 } // 5 hours default
    val chromeLimitMinsFlow: Flow<Int> = context.dataStore.data.map { it[CHROME_LIMIT_MINS] ?: 83 } // 1h 23m default
    val socialLimitMinsFlow: Flow<Int> = context.dataStore.data.map { it[SOCIAL_LIMIT_MINS] ?: 169 } // 2h 49m default
    val bedtimeModeActiveFlow: Flow<Boolean> = context.dataStore.data.map { it[BEDTIME_MODE_ACTIVE] ?: false }
    val fallbackScreenOnSecondsFlow: Flow<Int> = context.dataStore.data.map { it[FALLBACK_SCREEN_ON_SECONDS] ?: 11245 } // Starts with ~3h-ish of usage, then increases!

    suspend fun setWorkDuration(seconds: Int) {
        context.dataStore.edit { it[WORK_DURATION] = seconds }
    }

    suspend fun setBreakDuration(seconds: Int) {
        context.dataStore.edit { it[BREAK_DURATION] = seconds }
    }

    suspend fun setCurrentSessionSeconds(seconds: Int) {
        context.dataStore.edit { it[CURRENT_SESSION_SECONDS] = seconds }
    }

    suspend fun setFloatingTimerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[FLOATING_TIMER_ENABLED] = enabled }
    }

    suspend fun setWarningSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[WARNING_SOUND_ENABLED] = enabled }
    }

    suspend fun setStrictModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[STRICT_MODE_ENABLED] = enabled }
    }

    suspend fun setSelectedAlertTone(toneId: Int) {
        context.dataStore.edit { it[SELECTED_ALERT_TONE] = toneId }
    }

    suspend fun setScreenTimeGoalMins(mins: Int) {
        context.dataStore.edit { it[SCREEN_TIME_GOAL_MINS] = mins }
    }

    suspend fun setChromeLimitMins(mins: Int) {
        context.dataStore.edit { it[CHROME_LIMIT_MINS] = mins }
    }

    suspend fun setSocialLimitMins(mins: Int) {
        context.dataStore.edit { it[SOCIAL_LIMIT_MINS] = mins }
    }

    suspend fun setBedtimeModeActive(active: Boolean) {
        context.dataStore.edit { it[BEDTIME_MODE_ACTIVE] = active }
    }

    suspend fun setFallbackScreenOnSeconds(seconds: Int) {
        context.dataStore.edit { it[FALLBACK_SCREEN_ON_SECONDS] = seconds }
    }
}
