package com.example.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()
    val allLogs: Flow<List<HabitLogEntity>> = habitDao.getAllLogs()

    suspend fun insertHabit(habit: HabitEntity): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: HabitEntity) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        habitDao.deleteHabit(habit)
    }

    suspend fun completeHabit(habitId: Int, dateString: String) {
        val log = HabitLogEntity(habitId = habitId, dateString = dateString, isCompleted = true)
        habitDao.insertLog(log)
    }

    suspend fun uncompleteHabit(habitId: Int, dateString: String) {
        habitDao.deleteLog(habitId, dateString)
    }
}
