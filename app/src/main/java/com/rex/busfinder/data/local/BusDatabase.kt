package com.rex.busfinder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rex.busfinder.data.model.SearchHistoryItem

@Database(
    entities = [SearchHistoryItem::class],
    version = 1,
    exportSchema = false
)
abstract class BusDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: BusDatabase? = null

        fun getDatabase(context: Context): BusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BusDatabase::class.java,
                    "bus_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@androidx.room.Dao
interface SearchHistoryDao {
    @androidx.room.Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentSearches(): List<SearchHistoryItem>

    @androidx.room.Insert
    suspend fun insertSearch(search: SearchHistoryItem)

    @androidx.room.Query("DELETE FROM search_history")
    suspend fun clearHistory()
}