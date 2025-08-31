package com.rex.busfinder.data.repository

import android.content.Context
import com.rex.busfinder.data.local.BusDatabase
import com.rex.busfinder.data.model.BusRoute
import com.rex.busfinder.data.model.SearchHistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BusRepository(private val context: Context) {
    private val database = BusDatabase.getDatabase(context)
    private val searchHistoryDao = database.searchHistoryDao()

    suspend fun getAllBusRoutes(): List<BusRoute> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("bus_routes.json").bufferedReader().use { it.readText() }
                val listType = object : TypeToken<List<BusRoute>>() {}.type
                Gson().fromJson(jsonString, listType)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun searchBuses(from: String, to: String): List<BusRoute> {
        return withContext(Dispatchers.IO) {
            val allBuses = getAllBusRoutes()
            allBuses.filter { bus ->
                val forwardRoute = bus.routes.forward
                val backwardRoute = bus.routes.getBackwardRoute()

                (forwardRoute.contains(from) && forwardRoute.contains(to) &&
                        forwardRoute.indexOf(from) < forwardRoute.indexOf(to)) ||
                        (backwardRoute.contains(from) && backwardRoute.contains(to) &&
                                backwardRoute.indexOf(from) < backwardRoute.indexOf(to))
            }
        }
    }

    suspend fun getRecentSearches(): List<SearchHistoryItem> {
        return withContext(Dispatchers.IO) {
            searchHistoryDao.getRecentSearches()
        }
    }

    suspend fun saveSearch(from: String, to: String) {
        withContext(Dispatchers.IO) {
            val search = SearchHistoryItem(
                fromLocation = from,
                toLocation = to
            )
            searchHistoryDao.insertSearch(search)
        }
    }

    suspend fun clearSearchHistory() {
        withContext(Dispatchers.IO) {
            searchHistoryDao.clearHistory()
        }
    }
}