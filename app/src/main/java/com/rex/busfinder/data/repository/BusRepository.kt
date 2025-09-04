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

    // Add this new function for multi-hop journey planning
    suspend fun findMultiHopJourney(from: String, to: String): List<BusRoute> {
        return withContext(Dispatchers.IO) {
            try {
                val allBuses = getAllBusRoutes()
                val normalizedFrom = normalizeStopName(from)
                val normalizedTo = normalizeStopName(to)

                println("Repository: Finding multi-hop journey from '$from' to '$to'")

                // First check for direct buses
                val directResults = findDirectBuses(allBuses, normalizedFrom, normalizedTo)
                if (directResults.isNotEmpty()) {
                    println("Repository: Found ${directResults.size} direct buses")
                    return@withContext directResults
                }

                // If no direct buses, find multi-hop journey (2 or 3 hops)
                val multiHopResults = findMultiHopRoutes(allBuses, normalizedFrom, normalizedTo)

                if (multiHopResults.isNotEmpty()) {
                    println("Repository: Found ${multiHopResults.size} buses for multi-hop journey")
                    return@withContext multiHopResults
                }

                // No routes found
                println("Repository: No routes found for journey from '$from' to '$to'")
                emptyList()
            } catch (e: Exception) {
                println("Repository: Error finding multi-hop journey: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // Enhanced multi-hop route finder
    private fun findMultiHopRoutes(allBuses: List<BusRoute>, from: String, to: String): List<BusRoute> {
        val results = mutableSetOf<BusRoute>()

        println("Repository: Finding multi-hop routes from '$from' to '$to'")

        // First hop: Find buses from starting point
        val firstHopBuses = allBuses.filter { bus ->
            busServesStop(bus, from)
        }

        println("Repository: Found ${firstHopBuses.size} buses from starting point '$from'")

        // Second hop: For each first hop bus, find connecting buses
        for (firstBus in firstHopBuses) {
            // Get all stops this bus serves
            val firstBusStops = getBusStops(firstBus)
            val startIndex = firstBusStops.indexOfFirst { normalizeStopName(it) == from }

            if (startIndex != -1) {
                // Check each stop after the starting point as a potential transfer point
                for (i in startIndex + 1 until firstBusStops.size) {
                    val intermediateStop = firstBusStops[i]

                    // Find buses that serve this intermediate stop and can reach destination
                    val connectingBuses = allBuses.filter { bus ->
                        bus.id != firstBus.id &&
                                busServesStop(bus, intermediateStop) &&
                                canReachAnyDirection(bus, intermediateStop, to)
                    }

                    if (connectingBuses.isNotEmpty()) {
                        results.add(firstBus)
                        results.addAll(connectingBuses.take(3)) // Limit connecting buses
                        println("Repository: Found 2-hop connection: ${firstBus.name_en} -> ${connectingBuses.take(3).joinToString(", ") { it.name_en ?: it.name }} via $intermediateStop")
                    }

                    // Third hop: Try one more connection if needed
                    if (results.isEmpty()) {
                        for (j in i + 1 until firstBusStops.size) {
                            val secondIntermediateStop = firstBusStops[j]

                            // Find buses that serve second intermediate stop
                            val secondBuses = allBuses.filter { bus ->
                                bus.id != firstBus.id &&
                                        busServesStop(bus, secondIntermediateStop)
                            }

                            for (secondBus in secondBuses) {
                                if (canReachAnyDirection(secondBus, secondIntermediateStop, to)) {
                                    results.add(firstBus)
                                    results.add(secondBus)
                                    println("Repository: Found 3-hop connection: ${firstBus.name_en} -> ${secondBus.name_en} via $secondIntermediateStop")
                                }
                            }
                        }
                    }
                }
            }
        }

        return results.toList().take(10) // Limit to prevent too many results
    }

    // Helper function to check if a bus serves a particular stop
    private fun busServesStop(bus: BusRoute, stop: String): Boolean {
        val normalizedStop = normalizeStopName(stop)
        val allStops = getBusStops(bus)

        return allStops.any { busStop ->
            val normalizedBusStop = normalizeStopName(busStop)
            normalizedBusStop == normalizedStop ||
                    normalizedBusStop.contains(normalizedStop) ||
                    normalizedStop.contains(normalizedBusStop)
        }
    }

    // Helper function to get all stops for a bus
    private fun getBusStops(bus: BusRoute): List<String> {
        return bus.routes.forward + (bus.routes.backward ?: emptyList())
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
            canReachAnyDirection(bus, from, to)
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
            busServesStop(bus, from)
        }

        // Step 2: Find buses that serve the destination
        val busesToEnd = allBuses.filter { bus ->
            busServesStop(bus, to)
        }

        println("Repository: Found ${busesFromStart.size} buses from start, ${busesToEnd.size} buses to end")

        // Step 3: Look for buses that share common intermediate stops
        // This creates the "connecting" part of the journey
        for (startBus in busesFromStart) {
            for (endBus in busesToEnd) {
                // Skip if it's the same bus (we already checked for direct routes)
                if (startBus.id == endBus.id) continue

                // Find stops that both buses serve (potential transfer points)
                val startStops = getBusStops(startBus).map { normalizeStopName(it) }.toSet()
                val endStops = getBusStops(endBus).map { normalizeStopName(it) }.toSet()

                // Find intersection of stops (excluding start and end)
                val commonStops = startStops.intersect(endStops)
                    .filter { it != from && it != to }

                println("Repository: Bus ${startBus.name_en} and ${endBus.name_en} share ${commonStops.size} common stops")

                if (commonStops.isNotEmpty()) {
                    // Validate that the connection is possible
                    val commonStop = commonStops.first()
                    val startBusCanReachCommon = canReachAnyDirection(startBus, from, commonStop)
                    val endBusCanReachFromCommon = canReachAnyDirection(endBus, commonStop, to)

                    if (startBusCanReachCommon && endBusCanReachFromCommon) {
                        // Add both buses to results (user will transfer at common stop)
                        if (!results.contains(startBus)) {
                            results.add(startBus)
                            println("Repository: Added connecting bus ${startBus.name_en} (from $from to $commonStop)")
                        }
                        if (!results.contains(endBus)) {
                            results.add(endBus)
                            println("Repository: Added connecting bus ${endBus.name_en} (from $commonStop to $to)")
                        }
                    }
                }
            }
        }

        // Step 4: Fallback strategy - use major interchange points
        if (results.isEmpty()) {
            // Major bus terminals and interchange points in Dhaka
            val majorStops = listOf("gabtoli", "technical", "shyamoli", "farmgate", "motijheel", "uttara", "bashundhara")

            for (majorStop in majorStops) {
                // Find buses that pass through this major stop
                val busesThroughMajorStop = allBuses.filter { bus ->
                    busServesStop(bus, majorStop)
                }

                // Check if any of these buses can connect our start and end points
                for (bus in busesThroughMajorStop) {
                    val canReachFrom = canReachAnyDirection(bus, from, majorStop)
                    val canReachTo = canReachAnyDirection(bus, majorStop, to)

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

    // Helper function to check if a bus can reach from one stop to another (any direction)
    private fun canReachAnyDirection(bus: BusRoute, fromStop: String, toStop: String): Boolean {
        val forwardRoute = bus.routes.forward
        val backwardRoute = bus.routes.backward ?: emptyList()

        val normalizedFrom = normalizeStopName(fromStop)
        val normalizedTo = normalizeStopName(toStop)

        // Check forward direction
        val fromIndex = forwardRoute.indexOfFirst { normalizeStopName(it) == normalizedFrom }
        val toIndex = forwardRoute.indexOfFirst { normalizeStopName(it) == normalizedTo }
        if (fromIndex != -1 && toIndex != -1 && fromIndex < toIndex) {
            return true
        }

        // Check backward direction
        if (backwardRoute.isNotEmpty()) {
            val backFromIndex = backwardRoute.indexOfFirst { normalizeStopName(it) == normalizedFrom }
            val backToIndex = backwardRoute.indexOfFirst { normalizeStopName(it) == normalizedTo }
            if (backFromIndex != -1 && backToIndex != -1 && backFromIndex < backToIndex) {
                return true
            }
        }

        // Check reverse direction (bus can go in reverse even if not explicitly defined)
        val reversedForward = forwardRoute.reversed()
        val revFromIndex = reversedForward.indexOfFirst { normalizeStopName(it) == normalizedFrom }
        val revToIndex = reversedForward.indexOfFirst { normalizeStopName(it) == normalizedTo }
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
                    busServesStop(bus, stopName)
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
        val backwardRoute = bus.routes.backward ?: emptyList()

        // Check forward route
        val fromIndex = forwardRoute.indexOfFirst { normalizeStopName(it) == from }
        val toIndex = forwardRoute.indexOfFirst { normalizeStopName(it) == to }

        if (fromIndex != -1 && toIndex != -1 && fromIndex < toIndex) {
            // Return the stop just before the destination
            return forwardRoute.getOrNull(toIndex - 1)
        }

        // Check backward route
        if (backwardRoute.isNotEmpty()) {
            val backFromIndex = backwardRoute.indexOfFirst { normalizeStopName(it) == from }
            val backToIndex = backwardRoute.indexOfFirst { normalizeStopName(it) == to }

            if (backFromIndex != -1 && backToIndex != -1 && backFromIndex < backToIndex) {
                // Return the stop just before the destination
                return backwardRoute.getOrNull(backToIndex - 1)
            }
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
            bus.id != excludeBusId && canReachAnyDirection(bus, fromStop, toStop)
        }
    }
}