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

@Database(entities = [BreakHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun breakHistoryDao(): BreakHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "focus_lock_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
