package com.rex.busfinder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fromLocation: String,
    val toLocation: String,
    val timestamp: Long = System.currentTimeMillis()
)