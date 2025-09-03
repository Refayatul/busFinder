package com.rex.busfinder.data.repository

import android.content.Context
import com.rex.busfinder.data.local.BusDatabase
import com.rex.busfinder.data.local.SearchHistoryDao
import com.rex.busfinder.data.model.BusRoute
import com.rex.busfinder.data.model.SearchHistoryItem
import com.rex.busfinder.data.model.JourneySegment
import com.rex.busfinder.data.model.JourneyPlan
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

    /**
     * Main search function that finds buses from start to end location
     * Implements both direct route finding and multi-route journey planning
     *
     * @param from Starting location name
     * @param to Destination location name
     * @return List of BusRoute objects that can take user from start to end
     */
    suspend fun searchBuses(from: String, to: String): List<BusRoute> {
        return withContext(Dispatchers.IO) {
            try {
                val allBuses = getAllBusRoutes()
                val normalizedFrom = normalizeStopName(from)
                val normalizedTo = normalizeStopName(to)

                println("Repository: Searching from '$from' (normalized: '$normalizedFrom') to '$to' (normalized: '$normalizedTo')")

                // Step 1: Try to find direct buses (A to E in one bus)
                val directResults = findDirectBuses(allBuses, normalizedFrom, normalizedTo)

                if (directResults.isNotEmpty()) {
                    println("Repository: Found ${directResults.size} direct buses")
                    return@withContext directResults
                }

                // Step 2: If no direct buses, find connecting buses (A to B, then B to E)
                println("Repository: No direct buses found, searching for connecting routes...")
                val connectingResults = findConnectingBuses(allBuses, normalizedFrom, normalizedTo)

                if (connectingResults.isNotEmpty()) {
                    println("Repository: Found ${connectingResults.size} connecting buses")
                    return@withContext connectingResults
                }

                // Step 3: No routes found
                println("Repository: No buses found for route from '$from' to '$to'")
                emptyList()
            } catch (e: Exception) {
                println("Repository: Error searching buses: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Finds buses that go directly from start to end location
     * Checks both forward and backward directions of each bus route
     *
     * @param allBuses List of all available bus routes
     * @param from Normalized starting location
     * @param to Normalized destination location
     * @return List of buses that have direct routes from start to end
     */
    private fun findDirectBuses(allBuses: List<BusRoute>, from: String, to: String): List<BusRoute> {
        return allBuses.filter { bus ->
            // Get both forward and backward routes for this bus
            val forwardRoute = bus.routes.forward
            val backwardRoute = bus.routes.backward ?: emptyList()

            // Check if bus goes forward from start to end
            val forwardFromIndex = findStopIndex(forwardRoute, from)
            val forwardToIndex = findStopIndex(forwardRoute, to)
            val isForwardValid = forwardFromIndex != -1 &&
                    forwardToIndex != -1 &&
                    forwardFromIndex < forwardToIndex

            // Check if bus goes backward from start to end (if backward route exists)
            val isBackwardValid = if (backwardRoute.isNotEmpty()) {
                val backwardFromIndex = findStopIndex(backwardRoute, from)
                val backwardToIndex = findStopIndex(backwardRoute, to)
                backwardFromIndex != -1 &&
                        backwardToIndex != -1 &&
                        backwardFromIndex < backwardToIndex
            } else {
                // If no backward route, check if the reverse of forward route works
                // (useful for buses that can run in reverse direction)
                val reversedForward = forwardRoute.reversed()
                val reverseFromIndex = findStopIndex(reversedForward, from)
                val reverseToIndex = findStopIndex(reversedForward, to)
                reverseFromIndex != -1 &&
                        reverseToIndex != -1 &&
                        reverseFromIndex < reverseToIndex
            }

            // Debug logging for each bus
            if (isForwardValid || isBackwardValid) {
                println("Repository: Direct bus ${bus.name_en} matches - Forward: $isForwardValid, Backward: $isBackwardValid")
            }

            isForwardValid || isBackwardValid
        }
    }

    /**
     * Finds buses that can connect the user through intermediate stops
     * This enables multi-route journeys like A→B→C→D→E
     *
     * Algorithm:
     * 1. Find all buses that stop at the starting location
     * 2. Find all buses that stop at the destination
     * 3. Look for buses that share common intermediate stops
     * 4. Validate that connections are possible in the correct direction
     * 5. Fallback to major interchange points if no direct connections found
     *
     * @param allBuses List of all available bus routes
     * @param from Normalized starting location
     * @param to Normalized destination location
     * @return List of buses that can be used for connecting journeys
     */
    private fun findConnectingBuses(allBuses: List<BusRoute>, from: String, to: String): List<BusRoute> {
        val results = mutableListOf<BusRoute>()

        // Step 1: Find buses that serve the starting location
        val busesFromStart = allBuses.filter { bus ->
            val allBusStops = bus.routes.forward + (bus.routes.backward ?: emptyList())
            allBusStops.any { stop -> normalizeStopName(stop) == from }
        }

        // Step 2: Find buses that serve the destination
        val busesToEnd = allBuses.filter { bus ->
            val allBusStops = bus.routes.forward + (bus.routes.backward ?: emptyList())
            allBusStops.any { stop -> normalizeStopName(stop) == to }
        }

        // Step 3: Look for buses that share common intermediate stops
        // This creates the "connecting" part of the journey
        for (startBus in busesFromStart) {
            for (endBus in busesToEnd) {
                // Skip if it's the same bus (we already checked for direct routes)
                if (startBus.id == endBus.id) continue

                // Find stops that both buses serve (potential transfer points)
                val startStops = (startBus.routes.forward + (startBus.routes.backward ?: emptyList()))
                    .map { normalizeStopName(it) }
                    .toSet()

                val endStops = (endBus.routes.forward + (endBus.routes.backward ?: emptyList()))
                    .map { normalizeStopName(it) }
                    .toSet()

                // Find intersection of stops (excluding start and end)
                val commonStops = startStops.intersect(endStops)
                    .filter { it != from && it != to }

                if (commonStops.isNotEmpty()) {
                    // Validate that the connection is possible
                    val startBusCanReachCommon = canReachStop(startBus, from, commonStops.first())
                    val endBusCanReachFromCommon = canReachStop(endBus, commonStops.first(), to)

                    if (startBusCanReachCommon && endBusCanReachFromCommon) {
                        // Add both buses to results (user will transfer at common stop)
                        if (!results.contains(startBus)) {
                            results.add(startBus)
                            println("Repository: Added connecting bus ${startBus.name_en} (from $from to ${commonStops.first()})")
                        }
                        if (!results.contains(endBus)) {
                            results.add(endBus)
                            println("Repository: Added connecting bus ${endBus.name_en} (from ${commonStops.first()} to $to)")
                        }
                    }
                }
            }
        }

        // Step 4: Fallback strategy - use major interchange points
        if (results.isEmpty()) {
            // Major bus terminals and interchange points in Dhaka
            val majorStops = listOf("gabtoli", "technical", "shyamoli", "farmgate", "motijheel", "uttara")

            for (majorStop in majorStops) {
                // Find buses that pass through this major stop
                val busesThroughMajorStop = allBuses.filter { bus ->
                    val allBusStops = bus.routes.forward + (bus.routes.backward ?: emptyList())
                    allBusStops.any { stop -> normalizeStopName(stop).contains(majorStop) }
                }

                // Check if any of these buses can connect our start and end points
                for (bus in busesThroughMajorStop) {
                    val canReachFrom = canReachStop(bus, from, majorStop)
                    val canReachTo = canReachStop(bus, majorStop, to)

                    if (canReachFrom && canReachTo && !results.contains(bus)) {
                        results.add(bus)
                        println("Repository: Added bus ${bus.name_en} via major interchange $majorStop")
                        break // Only add one bus per major stop to avoid overwhelming results
                    }
                }

                if (results.size >= 3) break // Limit to prevent too many results
            }
        }

        return results.take(5) // Return top 5 most relevant connecting buses
    }

    // Helper function to check if a bus can reach from one stop to another
    private fun canReachStop(bus: BusRoute, fromStop: String, toStop: String): Boolean {
        val forwardRoute = bus.routes.forward
        val backwardRoute = bus.routes.backward ?: emptyList()

        // Check forward direction
        val fromIndex = findStopIndex(forwardRoute, fromStop)
        val toIndex = findStopIndex(forwardRoute, toStop)
        if (fromIndex != -1 && toIndex != -1 && fromIndex < toIndex) {
            return true
        }

        // Check backward direction
        if (backwardRoute.isNotEmpty()) {
            val backFromIndex = findStopIndex(backwardRoute, fromStop)
            val backToIndex = findStopIndex(backwardRoute, toStop)
            if (backFromIndex != -1 && backToIndex != -1 && backFromIndex < backToIndex) {
                return true
            }
        }

        // Check if reverse of forward route works
        val reversedForward = forwardRoute.reversed()
        val revFromIndex = findStopIndex(reversedForward, fromStop)
        val revToIndex = findStopIndex(reversedForward, toStop)
        if (revFromIndex != -1 && revToIndex != -1 && revFromIndex < revToIndex) {
            return true
        }

        return false
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

    /**
     * Advanced fuzzy search algorithm to find stop names in bus routes
     * Handles typos, partial matches, and variations in stop naming
     *
     * Matching strategies (in order of priority):
     * 1. Exact match: "Gabtoli" == "Gabtoli"
     * 2. Contains match: "Gab" in "Gabtoli" OR "Gabtoli" in "Gab"
     * 3. Word-by-word match: All words in search must exist in stop name
     *
     * Examples:
     * - "gab" matches "Gabtoli", "Gabtoli Bus Stand"
     * - "bus stand" matches "Gabtoli Bus Stand", "Mohakhali Bus Stand"
     * - "moha" matches "Mohakhali", "Mohammadpur"
     *
     * @param route List of stop names in the bus route
     * @param searchStop The stop name to search for
     * @return Index of the matching stop, or -1 if not found
     */
    private fun findStopIndex(route: List<String>, searchStop: String): Int {
        // Strategy 1: Exact match (highest priority)
        val exactIndex = route.indexOfFirst { stop ->
            normalizeStopName(stop) == searchStop
        }

        if (exactIndex != -1) return exactIndex

        // Strategy 2: Partial/contains matching
        return route.indexOfFirst { stop ->
            val normalizedStop = normalizeStopName(stop)

            // Check various matching strategies - return boolean values
            normalizedStop == searchStop ||
                    normalizedStop.contains(searchStop) ||
                    searchStop.contains(normalizedStop) ||
                    // Word-by-word match (handles compound names)
                    run {
                        val stopWords = normalizedStop.split(" ", "-", "/")
                        val searchWords = searchStop.split(" ", "-", "/")

                        // Check if all search words are present in stop words
                        // Example: "bus stand" should match "gabtoli bus stand"
                        searchWords.all { searchWord ->
                            stopWords.any { stopWord ->
                                stopWord.contains(searchWord) || searchWord.contains(stopWord)
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

    // Create a journey plan for multi-route trips
    suspend fun createJourneyPlan(from: String, to: String): JourneyPlan? {
        return withContext(Dispatchers.IO) {
            try {
                val allBuses = getAllBusRoutes()
                val normalizedFrom = normalizeStopName(from)
                val normalizedTo = normalizeStopName(to)

                println("Repository: Creating journey plan from '$from' to '$to'")

                // First check for direct routes
                val directBuses = findDirectBuses(allBuses, normalizedFrom, normalizedTo)
                if (directBuses.isNotEmpty()) {
                    // Create a single-segment journey plan
                    val segment = JourneySegment(
                        bus = directBuses.first(),
                        fromStop = from,
                        toStop = to,
                        direction = "forward"
                    )
                    return@withContext JourneyPlan(
                        segments = listOf(segment),
                        totalStops = 1,
                        estimatedTime = "Direct route"
                    )
                }

                // If no direct routes, create a multi-segment plan
                val segments = mutableListOf<JourneySegment>()

                // Find connecting buses
                val connectingBuses = findConnectingBuses(allBuses, normalizedFrom, normalizedTo)

                if (connectingBuses.isNotEmpty()) {
                    // For now, create a simple 2-segment plan
                    // In a more advanced implementation, this could handle 3+ segments
                    val firstBus = connectingBuses.first()

                    // Find the best intermediate stop
                    val intermediateStop = findBestIntermediateStop(firstBus, normalizedFrom, normalizedTo)

                    if (intermediateStop != null) {
                        // First segment: from start to intermediate
                        val firstSegment = JourneySegment(
                            bus = firstBus,
                            fromStop = from,
                            toStop = intermediateStop,
                            direction = "forward"
                        )
                        segments.add(firstSegment)

                        // Find a second bus for the remaining journey
                        val secondBus = findConnectingBusForSegment(
                            allBuses,
                            intermediateStop,
                            normalizedTo,
                            firstBus.id
                        )

                        if (secondBus != null) {
                            val secondSegment = JourneySegment(
                                bus = secondBus,
                                fromStop = intermediateStop,
                                toStop = to,
                                direction = "forward"
                            )
                            segments.add(secondSegment)
                        }
                    }
                }

                if (segments.isNotEmpty()) {
                    JourneyPlan(
                        segments = segments,
                        totalStops = segments.size,
                        estimatedTime = "${segments.size} transfers"
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                println("Repository: Error creating journey plan: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    // Find the best intermediate stop for a bus route
    private fun findBestIntermediateStop(bus: BusRoute, from: String, to: String): String? {
        val forwardRoute = bus.routes.forward

        val fromIndex = findStopIndex(forwardRoute, from)
        val toIndex = findStopIndex(forwardRoute, to)

        if (fromIndex != -1 && toIndex != -1 && fromIndex < toIndex) {
            // Return the stop just before the destination
            return forwardRoute.getOrNull(toIndex - 1)
        }

        return null
    }

    // Find a connecting bus for a specific segment
    private fun findConnectingBusForSegment(
        allBuses: List<BusRoute>,
        fromStop: String,
        toStop: String,
        excludeBusId: String
    ): BusRoute? {
        return allBuses.firstOrNull { bus ->
            bus.id != excludeBusId && canReachStop(bus, fromStop, toStop)
        }
    }
}