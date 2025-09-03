package com.rex.busfinder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rex.busfinder.data.model.BusRoute
import com.rex.busfinder.data.model.SearchHistoryItem
import com.rex.busfinder.data.repository.BusRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * BusViewModel - Central state management for the BUSFinder app
 *
 * Responsibilities:
 * - Manages UI state using LiveData for reactive updates
 * - Coordinates data operations between UI and Repository
 * - Implements debounced search suggestions for smooth UX
 * - Handles search history and user preferences
 * - Manages loading states and error handling
 *
 * Architecture: MVVM - ViewModel acts as bridge between UI (Views) and Data (Repository)
 * State Management: LiveData for observable data that survives configuration changes
 * Threading: viewModelScope for coroutine management with automatic cancellation
 */
class BusViewModel(application: Application) : AndroidViewModel(application) {
    // Repository handles all data operations (database, API, file/I/O)
    private val repository = BusRepository(application, BusRepository.getSearchHistoryDao(application))

    // === CORE DATA STATE ===
    // All bus routes loaded from JSON - used for search and display
    private val _busRoutes = MutableLiveData<List<BusRoute>>()
    val busRoutes: LiveData<List<BusRoute>> = _busRoutes

    // Current search results - updated when user searches for buses
    private val _searchResults = MutableLiveData<List<BusRoute>>()
    val searchResults: LiveData<List<BusRoute>> = _searchResults

    // Recent search history - persists user searches for quick access
    private val _recentSearches = MutableLiveData<List<SearchHistoryItem>>()
    val recentSearches: LiveData<List<SearchHistoryItem>> = _recentSearches

    // Loading state - shows progress indicators during async operations
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Search type indicator - shows what kind of search was performed
    private val _searchType = MutableLiveData<String>() // "direct", "connecting", "multi-hop"
    val searchType: LiveData<String> = _searchType

    // === SEARCH SUGGESTIONS SYSTEM ===
    // Master list of all stop names for fuzzy search suggestions
    private val _allStopNames = MutableLiveData<List<String>>(emptyList())

    // User's current input in search fields - triggers suggestion updates
    val fromSearchQuery = MutableLiveData<String>("")
    val toSearchQuery = MutableLiveData<String>("")

    // Filtered suggestions shown in dropdown - updated with 300ms debounce
    private val _fromSuggestions = MediatorLiveData<List<String>>()
    val fromSuggestions: LiveData<List<String>> = _fromSuggestions

    private val _toSuggestions = MediatorLiveData<List<String>>()
    val toSuggestions: LiveData<List<String>> = _toSuggestions

    // Coroutine jobs for debounced search - prevents excessive API calls
    private var fromSearchJob: Job? = null
    private var toSearchJob: Job? = null

    init {
        loadBusRoutes()
        loadRecentSearches()
        loadAllStopNames()

        // Setup observers with debounce for 'from' field
        _fromSuggestions.addSource(fromSearchQuery) { query ->
            fromSearchJob?.cancel()
            fromSearchJob = viewModelScope.launch {
                delay(300) // 300ms debounce
                updateFromSuggestions(query ?: "")
            }
        }

        // Setup observers with debounce for 'to' field
        _toSuggestions.addSource(toSearchQuery) { query ->
            toSearchJob?.cancel()
            toSearchJob = viewModelScope.launch {
                delay(300) // 300ms debounce
                updateToSuggestions(query ?: "")
            }
        }
    }

    private fun loadBusRoutes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _busRoutes.value = repository.getAllBusRoutes()
            } catch (e: Exception) {
                e.printStackTrace()
                _busRoutes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadAllStopNames() {
        viewModelScope.launch {
            try {
                val stopNames = repository.getAllStopNames()
                _allStopNames.value = stopNames

                // Debug: Print the number of stops loaded
                println("BusViewModel: Loaded ${stopNames.size} stop names")
                if (stopNames.isNotEmpty()) {
                    println("BusViewModel: Sample stops: ${stopNames.take(5)}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _allStopNames.value = emptyList()
            }
        }
    }

    /**
     * Advanced fuzzy search algorithm for stop name suggestions
     * Provides intelligent autocomplete as user types in search fields
     *
     * Features:
     * - Handles typos and partial matches
     * - Prioritizes exact matches, then prefix matches, then contains matches
     * - Word boundary matching for compound stop names
     * - Character sequence matching for misspelled words
     * - Length-based scoring to prefer more relevant results
     *
     * @param query User's search input (can be partial or misspelled)
     * @param items List of all available stop names to search through
     * @param limit Maximum number of suggestions to return
     * @return List of best matching stop names, sorted by relevance
     */
    private fun fuzzySearch(query: String, items: List<String>, limit: Int = 10): List<String> {
        if (query.isBlank() || items.isEmpty()) return emptyList()

        val normalizedQuery = query.trim().lowercase()

        // Calculate relevance scores for each stop name
        val scoredItems = items.mapNotNull { item ->
            val normalizedItem = item.lowercase()
            val score = calculateFuzzyScore(normalizedQuery, normalizedItem, item)
            if (score > 0) Pair(item, score) else null
        }

        // Return top matches sorted by score (highest first)
        return scoredItems
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Calculates relevance score for fuzzy search matching
     * Higher scores indicate better matches for user suggestions
     *
     * Scoring System (higher = better match):
     * - Exact match: 1000 points (perfect match)
     * - Starts with query: +500 points (e.g., "Gab" matches "Gabtoli")
     * - Contains query: +300 points (e.g., "toli" matches "Gabtoli")
     * - Word starts with query: +200 points per word (e.g., "Gab" matches "Gabtoli Bus Stand")
     * - Word contains query: +50 points per word
     * - Character sequence match: +10 points per consecutive character
     * - All characters found: +100 points bonus
     * - Length penalty: -2 points per extra character (prefers concise matches)
     *
     * Examples:
     * - "gab" vs "Gabtoli": 500 (starts with) + 100 (all chars) = 600
     * - "gab" vs "Gabtoli Bus Stand": 200 (word match) + 100 (all chars) = 300
     * - "xyz" vs "Mohakhali": 0 (no match)
     */
    private fun calculateFuzzyScore(query: String, normalizedItem: String, originalItem: String): Int {
        var score = 0

        // Perfect match gets highest score
        if (normalizedItem == query) return 1000

        // Prefix match (query at start of item)
        if (normalizedItem.startsWith(query)) {
            score += 500
        }

        // Substring match (query anywhere in item)
        if (normalizedItem.contains(query)) {
            score += 300
        }

        // Word-level matching for compound names
        val words = normalizedItem.split(" ", "-", ",", ".", "/", "(", ")")
        for (word in words) {
            if (word.startsWith(query)) {
                score += 200  // Word starts with query
            } else if (word.contains(query)) {
                score += 50   // Word contains query
            }
        }

        // Character-by-character sequence matching (handles typos)
        var queryIndex = 0
        var itemIndex = 0
        var consecutiveMatches = 0

        while (queryIndex < query.length && itemIndex < normalizedItem.length) {
            if (query[queryIndex] == normalizedItem[itemIndex]) {
                consecutiveMatches++
                score += consecutiveMatches * 10 // Bonus for consecutive matches
                queryIndex++
            } else {
                consecutiveMatches = 0
            }
            itemIndex++
        }

        // Bonus if all query characters were found in sequence
        if (queryIndex == query.length) {
            score += 100
        }

        // Length penalty - prefer shorter, more relevant matches
        val lengthDiff = normalizedItem.length - query.length
        if (lengthDiff > 0) {
            score -= lengthDiff * 2
        }

        return score.coerceAtLeast(0) // Ensure score is never negative
    }

    private fun updateFromSuggestions(query: String) {
        viewModelScope.launch {
            val allStops = _allStopNames.value ?: emptyList()

            if (query.isBlank() || allStops.isEmpty()) {
                _fromSuggestions.value = emptyList()
                return@launch
            }

            val suggestions = fuzzySearch(query, allStops, limit = 5)
            _fromSuggestions.value = suggestions

            // Debug
            println("BusViewModel: From Query: '$query' -> Found ${suggestions.size} suggestions")
            if (suggestions.isNotEmpty()) {
                println("BusViewModel: Top suggestions: $suggestions")
            }
        }
    }

    private fun updateToSuggestions(query: String) {
        viewModelScope.launch {
            val allStops = _allStopNames.value ?: emptyList()

            if (query.isBlank() || allStops.isEmpty()) {
                _toSuggestions.value = emptyList()
                return@launch
            }

            val suggestions = fuzzySearch(query, allStops, limit = 5)
            _toSuggestions.value = suggestions

            // Debug
            println("BusViewModel: To Query: '$query' -> Found ${suggestions.size} suggestions")
            if (suggestions.isNotEmpty()) {
                println("BusViewModel: Top suggestions: $suggestions")
            }
        }
    }

    fun updateFromQuery(query: String) {
        fromSearchQuery.value = query

        // Clear 'to' suggestions when 'from' is being typed
        if (query.isNotEmpty()) {
            _toSuggestions.value = emptyList()
        }
    }

    fun updateToQuery(query: String) {
        toSearchQuery.value = query

        // Clear 'from' suggestions when 'to' is being typed
        if (query.isNotEmpty()) {
            _fromSuggestions.value = emptyList()
        }
    }

    fun searchBuses(from: String, to: String) {
        if (from.isBlank() || to.isBlank()) {
            _searchResults.value = emptyList()
            _searchType.value = ""
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _searchType.value = "Searching..."
            try {
                _searchResults.value = repository.searchBuses(from, to)
                _searchType.value = "Direct routes"
                repository.saveSearch(from, to)
                loadRecentSearches()
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
                _searchType.value = "Error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Enhanced search function with multi-hop journey planning
    fun searchBusesWithConnections(from: String, to: String) {
        if (from.isBlank() || to.isBlank()) {
            _searchResults.value = emptyList()
            _searchType.value = ""
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _searchType.value = "Searching..."
            try {
                // First try direct search
                val directResults = repository.searchBuses(from, to)

                if (directResults.isNotEmpty()) {
                    _searchResults.value = directResults
                    _searchType.value = "Direct routes"
                    repository.saveSearch(from, to)
                    loadRecentSearches()
                } else {
                    // If no direct results, try multi-hop journey
                    println("ViewModel: No direct buses found, searching for multi-hop journey")
                    val multiHopResults = repository.findMultiHopJourney(from, to)
                    _searchResults.value = multiHopResults
                    if (multiHopResults.isNotEmpty()) {
                        _searchType.value = "Connecting routes (multi-hop)"
                        repository.saveSearch(from, to)
                        loadRecentSearches()
                    } else {
                        _searchType.value = "No routes found"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
                _searchType.value = "Error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            try {
                repository.getRecentSearches().collect { searches ->
                    _recentSearches.value = searches
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _recentSearches.value = emptyList()
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            try {
                repository.clearSearchHistory()
                loadRecentSearches()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearSearches() {
        fromSearchQuery.value = ""
        toSearchQuery.value = ""
        _fromSuggestions.value = emptyList()
        _toSuggestions.value = emptyList()
    }

    // Get a bus route by ID - Enhanced debugging version
    fun getBusRoute(routeId: String): Flow<BusRoute?> {
        return flow {
            println("=== DEBUG: BusViewModel.getBusRoute ===")
            println("Requested routeId: '$routeId'")
            println("RouteId length: ${routeId.length}")
            println("RouteId is blank: ${routeId.isBlank()}")

            try {
                // Load all routes first to ensure we have data
                val allRoutes = repository.getAllBusRoutes()
                println("Total routes loaded from repository: ${allRoutes.size}")

                // Log first few routes for debugging
                allRoutes.take(3).forEach { route ->
                    println("Available route - ID: '${route.id}', Name: '${route.name_en}'")
                }

                // Try to find the route
                val route = allRoutes.find { busRoute ->
                    val matches = busRoute.id == routeId
                    if (matches) {
                        println("MATCH FOUND! Route ID: '${busRoute.id}' matches requested: '$routeId'")
                    }
                    matches
                }

                if (route != null) {
                    println("SUCCESS: Found route: ${route.name_en ?: route.name} (ID: ${route.id})")
                    println("Route forward stops count: ${route.routes.forward.size}")
                    if (route.routes.backward != null) {
                        println("Route backward stops count: ${route.routes.backward?.size}")
                    }
                } else {
                    println("ERROR: No route found for routeId: '$routeId'")
                    println("Available route IDs:")
                    allRoutes.forEach { r ->
                        println("  - '${r.id}'")
                    }
                }

                emit(route)
            } catch (e: Exception) {
                println("EXCEPTION in getBusRoute: ${e.message}")
                e.printStackTrace()
                emit(null)
            }
        }
    }

    // Get favorite routes
    fun getFavoriteRoutes(): LiveData<List<BusRoute>> {
        val result = MutableLiveData<List<BusRoute>>()
        viewModelScope.launch {
            try {
                // This would need to be implemented in the repository
                // For now, return empty list
                result.value = emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                result.value = emptyList()
            }
        }
        return result
    }

    override fun onCleared() {
        super.onCleared()
        fromSearchJob?.cancel()
        toSearchJob?.cancel()
    }
}