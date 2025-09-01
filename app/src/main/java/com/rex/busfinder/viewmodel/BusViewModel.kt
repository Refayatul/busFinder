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
import kotlinx.coroutines.launch

class BusViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BusRepository(application, BusRepository.getSearchHistoryDao(application))

    // Existing LiveData
    private val _busRoutes = MutableLiveData<List<BusRoute>>()
    val busRoutes: LiveData<List<BusRoute>> = _busRoutes

    private val _searchResults = MutableLiveData<List<BusRoute>>()
    val searchResults: LiveData<List<BusRoute>> = _searchResults

    private val _recentSearches = MutableLiveData<List<SearchHistoryItem>>()
    val recentSearches: LiveData<List<SearchHistoryItem>> = _recentSearches

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // --- LiveData for Search Suggestions ---

    // Holds the master list of all stop names
    private val _allStopNames = MutableLiveData<List<String>>(emptyList())

    // Holds the user's current input for the 'from' and 'to' fields
    val fromSearchQuery = MutableLiveData<String>("")
    val toSearchQuery = MutableLiveData<String>("")

    // Holds the filtered list of suggestions for the UI
    private val _fromSuggestions = MediatorLiveData<List<String>>()
    val fromSuggestions: LiveData<List<String>> = _fromSuggestions

    private val _toSuggestions = MediatorLiveData<List<String>>()
    val toSuggestions: LiveData<List<String>> = _toSuggestions

    // Debounce jobs for search
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

    // Improved fuzzy search algorithm
    private fun fuzzySearch(query: String, items: List<String>, limit: Int = 10): List<String> {
        if (query.isBlank() || items.isEmpty()) return emptyList()

        val normalizedQuery = query.trim().lowercase()

        // Calculate scores for each item
        val scoredItems = items.mapNotNull { item ->
            val normalizedItem = item.lowercase()
            val score = calculateFuzzyScore(normalizedQuery, normalizedItem, item)
            if (score > 0) Pair(item, score) else null
        }

        // Sort by score and return top results
        return scoredItems
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun calculateFuzzyScore(query: String, normalizedItem: String, originalItem: String): Int {
        var score = 0

        // Exact match
        if (normalizedItem == query) return 1000

        // Starts with query (highest priority)
        if (normalizedItem.startsWith(query)) {
            score += 500
        }

        // Contains query as a substring
        if (normalizedItem.contains(query)) {
            score += 300
        }

        // Word boundary match (each word that starts with query)
        val words = normalizedItem.split(" ", "-", ",", ".", "/", "(", ")")
        for (word in words) {
            if (word.startsWith(query)) {
                score += 200
            } else if (word.contains(query)) {
                score += 50
            }
        }

        // Character sequence match (for typos)
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

        // All query characters found in sequence
        if (queryIndex == query.length) {
            score += 100
        }

        // Penalty for length difference (prefer shorter, more relevant matches)
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
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                _searchResults.value = repository.searchBuses(from, to)
                repository.saveSearch(from, to)
                loadRecentSearches()
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
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

    // Get a bus route by ID - FIXED: Changed parameter type from Int to String
    fun getBusRoute(routeId: String): LiveData<BusRoute?> {
        val result = MutableLiveData<BusRoute?>()
        viewModelScope.launch {
            try {
                val route = repository.getBusRoute(routeId)
                result.value = route
            } catch (e: Exception) {
                e.printStackTrace()
                result.value = null
            }
        }
        return result
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