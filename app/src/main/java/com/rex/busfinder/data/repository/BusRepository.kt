package com.rex.busfinder.data.repository

import android.content.Context
import com.rex.busfinder.data.local.BusDatabase
import com.rex.busfinder.data.local.SearchHistoryDao
import com.rex.busfinder.data.model.BusRoute
import com.rex.busfinder.data.model.SearchHistoryItem
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Date

class BusRepository(private val context: Context, private val searchHistoryDao: SearchHistoryDao) {

    companion object {
        fun getSearchHistoryDao(context: Context): SearchHistoryDao {
            return BusDatabase.getDatabase(context).searchHistoryDao()
        }
    }

    suspend fun getAllBusRoutes(): List<BusRoute> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("bus_routes.json").bufferedReader().use { it.readText() }

                // Parse the JSON object first
                val gson = Gson()
                val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)

                // Extract the "buses" array
                val busesArray = jsonObject.getAsJsonArray("buses")

                // Convert to list of BusRoute
                val listType = object : TypeToken<List<BusRoute>>() {}.type
                val routes: List<BusRoute> = gson.fromJson(busesArray, listType)

                // Debug log
                println("Repository: Loaded ${routes.size} bus routes from JSON")
                if (routes.isNotEmpty()) {
                    println("Repository: Sample buses: ${routes.take(3).map { it.name_en ?: it.name }.joinToString(", ")}")
                }

                routes
            } catch (e: Exception) {
                println("Repository: Error loading bus routes: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getAllStopNames(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val allBuses = getAllBusRoutes()
                val allStops = mutableSetOf<String>()

                for (bus in allBuses) {
                    // Add all forward stops
                    if (bus.routes.forward.isNotEmpty()) {
                        allStops.addAll(bus.routes.forward)
                    }

                    // Add all backward stops if they exist
                    if (bus.routes.backward?.isNotEmpty() == true) {
                        allStops.addAll(bus.routes.backward)
                    }
                }

                // Convert to list, clean up, and sort alphabetically
                val stopsList = allStops
                    .filter { it.isNotBlank() } // Filter out empty strings
                    .map { stop ->
                        // Clean up stop names
                        stop.trim()
                            .replace("–", "-") // Replace en-dash with regular dash
                            .replace("  ", " ") // Remove double spaces
                    }
                    .distinct() // Remove duplicates after cleaning
                    .sorted()

                // Debug log
                println("Repository: Loaded ${stopsList.size} unique stops")
                if (stopsList.isNotEmpty()) {
                    println("Repository: Sample stops: ${stopsList.take(10).joinToString(", ")}")
                }

                stopsList
            } catch (e: Exception) {
                println("Repository: Error loading stops: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun searchBuses(from: String, to: String): List<BusRoute> {
        return withContext(Dispatchers.IO) {
            try {
                val allBuses = getAllBusRoutes()
                val normalizedFrom = normalizeStopName(from)
                val normalizedTo = normalizeStopName(to)

                println("Repository: Searching from '$from' (normalized: '$normalizedFrom') to '$to' (normalized: '$normalizedTo')")

                val results = allBuses.filter { bus ->
                    // Get routes
                    val forwardRoute = bus.routes.forward
                    val backwardRoute = bus.routes.backward ?: emptyList()

                    // Check forward route
                    val forwardFromIndex = findStopIndex(forwardRoute, normalizedFrom)
                    val forwardToIndex = findStopIndex(forwardRoute, normalizedTo)
                    val isForwardValid = forwardFromIndex != -1 &&
                            forwardToIndex != -1 &&
                            forwardFromIndex < forwardToIndex

                    // Check backward route (if exists)
                    val isBackwardValid = if (backwardRoute.isNotEmpty()) {
                        val backwardFromIndex = findStopIndex(backwardRoute, normalizedFrom)
                        val backwardToIndex = findStopIndex(backwardRoute, normalizedTo)
                        backwardFromIndex != -1 &&
                                backwardToIndex != -1 &&
                                backwardFromIndex < backwardToIndex
                    } else {
                        // If no backward route, check if the reverse of forward route works
                        val reversedForward = forwardRoute.reversed()
                        val reverseFromIndex = findStopIndex(reversedForward, normalizedFrom)
                        val reverseToIndex = findStopIndex(reversedForward, normalizedTo)
                        reverseFromIndex != -1 &&
                                reverseToIndex != -1 &&
                                reverseFromIndex < reverseToIndex
                    }

                    // Debug logging for each bus
                    if (isForwardValid || isBackwardValid) {
                        println("Repository: Bus ${bus.name_en} matches - Forward: $isForwardValid, Backward: $isBackwardValid")
                    }

                    isForwardValid || isBackwardValid
                }

                // Debug log
                println("Repository: Search from '$from' to '$to' found ${results.size} buses")
                if (results.isNotEmpty()) {
                    println("Repository: Found buses: ${results.map { it.name_en ?: it.name }.joinToString(", ")}")
                }

                results
            } catch (e: Exception) {
                println("Repository: Error searching buses: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // Helper function to normalize stop names for comparison
    private fun normalizeStopName(stop: String): String {
        return stop.trim()
            .lowercase()
            .replace("–", "-") // Replace en-dash with regular dash
            .replace("—", "-") // Replace em-dash with regular dash
            .replace("  ", " ") // Remove double spaces
            .replace("(", "")
            .replace(")", "")
            .replace(",", "")
            .replace(".", "")
    }

    // Helper function to find stop index with fuzzy matching
    private fun findStopIndex(route: List<String>, searchStop: String): Int {
        // First try exact match
        val exactIndex = route.indexOfFirst { stop ->
            normalizeStopName(stop) == searchStop
        }

        if (exactIndex != -1) return exactIndex

        // Then try partial matches
        return route.indexOfFirst { stop ->
            val normalizedStop = normalizeStopName(stop)

            // Check various matching strategies
            when {
                // Exact match (already checked above)
                normalizedStop == searchStop -> true

                // Contains match (one contains the other)
                normalizedStop.contains(searchStop) -> true
                searchStop.contains(normalizedStop) -> true

                // Word-by-word match
                else -> {
                    val stopWords = normalizedStop.split(" ", "-", "/")
                    val searchWords = searchStop.split(" ", "-", "/")

                    // Check if all search words are present in stop words
                    searchWords.all { searchWord ->
                        stopWords.any { stopWord ->
                            stopWord.contains(searchWord) || searchWord.contains(stopWord)
                        }
                    }
                }
            }
        }
    }

    suspend fun getBusRoute(routeId: String): BusRoute? {
        return withContext(Dispatchers.IO) {
            try {
                val allRoutes = getAllBusRoutes()
                allRoutes.find { it.id == routeId }
            } catch (e: Exception) {
                println("Repository: Error getting bus route: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    fun getRecentSearches(): Flow<List<SearchHistoryItem>> {
        return searchHistoryDao.getRecentSearches()
    }

    suspend fun saveSearch(from: String, to: String) {
        withContext(Dispatchers.IO) {
            try {
                // Check if the same search already exists
                val existingSearches = searchHistoryDao.getRecentSearchesList()
                val exists = existingSearches.any {
                    it.fromLocation.equals(from, ignoreCase = true) &&
                            it.toLocation.equals(to, ignoreCase = true)
                }

                if (!exists) {
                    val search = SearchHistoryItem(
                        fromLocation = from.trim(),
                        toLocation = to.trim(),
                        timestamp = Date().time
                    )
                    searchHistoryDao.insertSearch(search)

                    // Keep only the last 10 searches
                    val allSearches = searchHistoryDao.getRecentSearchesList()
                    if (allSearches.size > 10) {
                        val toDelete = allSearches.drop(10)
                        toDelete.forEach { searchHistoryDao.deleteSearch(it) }
                    }

                    println("Repository: Saved search from '$from' to '$to'")
                } else {
                    // Update timestamp of existing search
                    val existingSearch = existingSearches.find {
                        it.fromLocation.equals(from, ignoreCase = true) &&
                                it.toLocation.equals(to, ignoreCase = true)
                    }
                    existingSearch?.let {
                        // Create a new instance with updated timestamp
                        val updatedSearch = it.copy(timestamp = Date().time)
                        searchHistoryDao.updateSearch(updatedSearch)
                        println("Repository: Updated timestamp for existing search")
                    }
                }
            } catch (e: Exception) {
                println("Repository: Error saving search: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    suspend fun clearSearchHistory() {
        withContext(Dispatchers.IO) {
            try {
                searchHistoryDao.clearHistory()
                println("Repository: Cleared search history")
            } catch (e: Exception) {
                println("Repository: Error clearing search history: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteSearchHistoryItem(item: SearchHistoryItem) {
        withContext(Dispatchers.IO) {
            try {
                searchHistoryDao.deleteSearch(item)
                println("Repository: Deleted search history item")
            } catch (e: Exception) {
                println("Repository: Error deleting search history item: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Get all unique service types
    suspend fun getServiceTypes(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val allBuses = getAllBusRoutes()
                val types = allBuses
                    .mapNotNull { it.service_type }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                println("Repository: Found service types: ${types.joinToString(", ")}")
                types
            } catch (e: Exception) {
                println("Repository: Error getting service types: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // Get buses passing through a specific stop
    suspend fun getBusesAtStop(stopName: String): List<BusRoute> {
        return withContext(Dispatchers.IO) {
            try {
                val allBuses = getAllBusRoutes()
                val normalized = normalizeStopName(stopName)

                allBuses.filter { bus ->
                    val allStops = bus.routes.forward + (bus.routes.backward ?: emptyList())
                    allStops.any { stop ->
                        val normalizedStop = normalizeStopName(stop)
                        normalizedStop == normalized ||
                                normalizedStop.contains(normalized) ||
                                normalized.contains(normalizedStop)
                    }
                }
            } catch (e: Exception) {
                println("Repository: Error getting buses at stop: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }
}