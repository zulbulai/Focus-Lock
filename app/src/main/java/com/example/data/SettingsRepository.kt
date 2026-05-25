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
    }

    val workDurationFlow: Flow<Int> = context.dataStore.data.map { it[WORK_DURATION] ?: (30 * 60) }
    val breakDurationFlow: Flow<Int> = context.dataStore.data.map { it[BREAK_DURATION] ?: (5 * 60) }
    val currentSessionSecondsFlow: Flow<Int> = context.dataStore.data.map { it[CURRENT_SESSION_SECONDS] ?: 0 }
    val floatingTimerEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[FLOATING_TIMER_ENABLED] ?: true }

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
}
