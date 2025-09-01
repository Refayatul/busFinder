package com.rex.busfinder.data.local

import androidx.room.*
import com.rex.busfinder.data.model.SearchHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getRecentSearches(): Flow<List<SearchHistoryItem>>

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    suspend fun getRecentSearchesList(): List<SearchHistoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryItem)

    @Update
    suspend fun updateSearch(search: SearchHistoryItem)

    @Delete
    suspend fun deleteSearch(search: SearchHistoryItem)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}