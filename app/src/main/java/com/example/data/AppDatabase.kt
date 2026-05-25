package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "break_history")
data class BreakHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val durationSeconds: Int,
    val wasSkipped: Boolean = false
)

@Dao
interface BreakHistoryDao {
    @Query("SELECT * FROM break_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<BreakHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: BreakHistoryEntity)
}

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val colorHex: String = "#FFB74D", // Accent hex color
    val iconName: String = "bed", // e.g. bed, drink, fitness, book, star
    val category: String = "Health", // e.g. Health, Mind, Productivity, Custom
    val frequencyType: String = "Everyday", // Everyday, Specific Days of Week, etc.
    val isChecklist: Boolean = false,
    val hasReminder: Boolean = false,
    val showStreaks: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habit_logs")
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val dateString: String, // format: "yyyy-MM-dd"
    val isCompleted: Boolean = true,
    val loggedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val priority: String = "Medium", // High, Medium, Low
    val category: String = "Work", // Work, Study, Health, Personal, Shopping, etc.
    val dueDate: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val subtasksText: String = "", // formatted: "subtask1|false\nsubtask2|true"
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("SELECT * FROM habit_logs")
    fun getAllLogs(): Flow<List<HabitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun deleteLog(habitId: Int, dateString: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, dueDate ASC, id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}

@Database(entities = [BreakHistoryEntity::class, HabitEntity::class, HabitLogEntity::class, TaskEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun breakHistoryDao(): BreakHistoryDao
    abstract fun habitDao(): HabitDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focus_lock_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
