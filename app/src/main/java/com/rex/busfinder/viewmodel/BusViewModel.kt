package com.rex.busfinder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rex.busfinder.data.model.BusRoute
import com.rex.busfinder.data.model.SearchHistoryItem
import com.rex.busfinder.data.repository.BusRepository
import kotlinx.coroutines.launch

class BusViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BusRepository(application)

    private val _busRoutes = MutableLiveData<List<BusRoute>>()
    val busRoutes: LiveData<List<BusRoute>> = _busRoutes

    private val _searchResults = MutableLiveData<List<BusRoute>>()
    val searchResults: LiveData<List<BusRoute>> = _searchResults

    private val _recentSearches = MutableLiveData<List<SearchHistoryItem>>()
    val recentSearches: LiveData<List<SearchHistoryItem>> = _recentSearches

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadBusRoutes()
        loadRecentSearches()
    }

    private fun loadBusRoutes() {
        viewModelScope.launch {
            _isLoading.value = true
            _busRoutes.value = repository.getAllBusRoutes()
            _isLoading.value = false
        }
    }

    fun searchBuses(from: String, to: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _searchResults.value = repository.searchBuses(from, to)
            repository.saveSearch(from, to)
            loadRecentSearches()
            _isLoading.value = false
        }
    }

    private fun loadRecentSearches() {
        viewModelScope.launch {
            _recentSearches.value = repository.getRecentSearches()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
            loadRecentSearches()
        }
    }
}