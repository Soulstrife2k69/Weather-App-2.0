package com.example.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recent_cities")
data class RecentCity(
    @PrimaryKey val cityName: String,
    val searchTime: Long = System.currentTimeMillis()
)

@Dao
interface RecentCityDao {
    @Query("SELECT * FROM recent_cities ORDER BY searchTime DESC LIMIT 10")
    fun getRecentCities(): Flow<List<RecentCity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(recentCity: RecentCity)

    @Query("DELETE FROM recent_cities WHERE cityName = :cityName")
    suspend fun deleteCity(cityName: String)

    @Query("DELETE FROM recent_cities")
    suspend fun clearAll()
}

@Database(entities = [RecentCity::class], version = 1, exportSchema = false)
abstract class RecentCityDatabase : RoomDatabase() {
    abstract fun recentCityDao(): RecentCityDao

    companion object {
        @Volatile
        private var INSTANCE: RecentCityDatabase? = null

        fun getDatabase(context: Context): RecentCityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RecentCityDatabase::class.java,
                    "recent_cities_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
