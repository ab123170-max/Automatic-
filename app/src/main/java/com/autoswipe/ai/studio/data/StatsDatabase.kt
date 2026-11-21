package com.autoswipe.ai.studio.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_stats")
data class AppStatsEntity(
    @PrimaryKey val appId: String,
    val displayName: String,
    val swipeCount: Int = 0,
    val totalVideoIntervalMs: Long = 0L,
    val detectionFailureCount: Int = 0,
    val gestureFailureCount: Int = 0,
    val lastUpdatedEpochMs: Long = 0L
)

@Entity(tableName = "global_stats")
data class GlobalStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalSwipes: Int = 0,
    val totalSessions: Int = 0,
    val totalActiveTimeMs: Long = 0L,
    val totalDetectionFailures: Int = 0,
    val totalGestureFailures: Int = 0,
    val totalVideoIntervalMs: Long = 0L,
    val swipeIntervalsCount: Int = 0
)

@Dao
interface StatsDao {
    @Query("SELECT * FROM global_stats WHERE id = 1")
    fun observeGlobalStats(): Flow<GlobalStatsEntity?>

    @Query("SELECT * FROM global_stats WHERE id = 1")
    suspend fun getGlobalStats(): GlobalStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGlobalStats(entity: GlobalStatsEntity)

    @Query("SELECT * FROM app_stats ORDER BY swipeCount DESC, displayName ASC")
    fun observeAllAppStats(): Flow<List<AppStatsEntity>>

    @Query("SELECT * FROM app_stats WHERE appId = :appId")
    suspend fun getAppStats(appId: String): AppStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppStats(entity: AppStatsEntity)

    @Query("DELETE FROM app_stats")
    suspend fun clearAppStats()

    @Query("DELETE FROM global_stats")
    suspend fun clearGlobalStats()
}

@Database(
    entities = [AppStatsEntity::class, GlobalStatsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile
        private var instance: StatsDatabase? = null

        fun getInstance(context: Context): StatsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StatsDatabase::class.java,
                    "autoswipe_stats.db"
                ).build().also { instance = it }
            }
        }
    }
}
