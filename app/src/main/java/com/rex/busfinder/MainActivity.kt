package com.rex.busfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rex.busfinder.ui.screen.HomeScreen
import com.rex.busfinder.ui.screen.SearchScreen
import com.rex.busfinder.ui.theme.BUSFinderTheme
import com.rex.busfinder.ui.theme.LocalTheme
import com.rex.busfinder.ui.theme.Theme
import com.rex.busfinder.viewmodel.BusViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DhakaBusFinderApp()
        }
    }
}

@Composable
fun DhakaBusFinderApp() {
    val theme = remember { mutableStateOf(Theme.LIGHT) }

    CompositionLocalProvider(LocalTheme provides theme) {
        BUSFinderTheme(theme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val navController = rememberNavController()
                val viewModel: BusViewModel = viewModel()

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            navController = navController,
                            onSearch = { from, to ->
                                viewModel.searchBuses(from, to)
                            }
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                    composable("favorites") {
                        // Favorites screen implementation
                    }
                    composable("settings") {
                        // Settings screen implementation
                    }
                }
            }
        }
    }
}