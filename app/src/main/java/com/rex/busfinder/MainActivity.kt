package com.rex.busfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rex.busfinder.ui.screen.HomeScreen
import com.rex.busfinder.ui.screen.SearchScreen
import com.rex.busfinder.ui.screen.RouteDetailsScreen
import com.rex.busfinder.ui.theme.BUSFinderTheme
import com.rex.busfinder.viewmodel.BusViewModel

/**
 * Main Activity - Entry point of the BUSFinder Android application
 *
 * Architecture: MVVM (Model-View-ViewModel) with Jetpack Compose
 * Navigation: Single Activity with multiple Compose screens
 * State Management: ViewModel with LiveData/Flow for reactive UI updates
 *
 * App Flow:
 * 1. Home Screen - Search interface with recent searches
 * 2. Search Screen - Results display with bus route list
 * 3. Route Details Screen - Detailed view of selected bus route
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BUSFinderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Navigation controller manages screen transitions
                    val navController = rememberNavController()

                    // Shared ViewModel for data management across screens
                    val viewModel: BusViewModel = viewModel()

                    // Navigation host defines the app's screen structure
                    NavHost(
                        navController = navController,
                        startDestination = "home" // App starts on home screen
                    ) {
                        // Home screen - main search interface
                        composable("home") {
                            HomeScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }

                        // Search results screen - displays bus routes
                        composable("search") {
                            SearchScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }

                        // Route details screen - shows full route information
                        // Uses routeId parameter to identify which bus to display
                        composable("route_details/{routeId}") { backStackEntry ->
                            val routeId = backStackEntry.arguments?.getString("routeId") ?: ""
                            RouteDetailsScreen(
                                navController = navController,
                                routeId = routeId,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Home Screen Preview")
@Composable
fun HomeScreenPreview() {
    BUSFinderTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val viewModel: BusViewModel = viewModel()

            HomeScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}

@Preview(showBackground = true, name = "Search Screen Preview")
@Composable
fun SearchScreenPreview() {
    BUSFinderTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val viewModel: BusViewModel = viewModel()

            SearchScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}
