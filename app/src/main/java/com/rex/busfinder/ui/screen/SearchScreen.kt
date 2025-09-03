package com.rex.busfinder.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.rex.busfinder.R
import com.rex.busfinder.data.model.BusRoute
import com.rex.busfinder.data.model.Routes
import com.rex.busfinder.ui.component.BusItem
import com.rex.busfinder.viewmodel.BusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: BusViewModel
) {
    val searchResults by viewModel.searchResults.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_results)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        SearchScreenContent(
            modifier = Modifier.padding(padding),
            navController = navController,
            isLoading = isLoading,
            searchResults = searchResults
        )
    }
}

@Composable
fun SearchScreenContent(
    modifier: Modifier = Modifier,
    navController: NavController,
    isLoading: Boolean,
    searchResults: List<BusRoute>
) {
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        if (searchResults.isEmpty()) {
            EmptySearchState()
        } else {
            BusList(
                modifier = modifier,
                navController = navController,
                buses = searchResults
            )
        }
    }
}

@Composable
fun EmptySearchState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsBus,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_buses_found),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.try_different_search),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun BusList(
    modifier: Modifier = Modifier,
    navController: NavController,
    buses: List<BusRoute>
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Debug test button - same approach as HomeScreen
        item {
            Button(
                onClick = {
                    Toast.makeText(context, "Test: Navigating to achim_paribahan", Toast.LENGTH_SHORT).show()
                    println("=== SEARCH SCREEN TEST NAVIGATION ===")
                    println("Test navigating to route_details/achim_paribahan")
                    try {
                        navController.navigate("route_details/achim_paribahan")
                    } catch (e: Exception) {
                        val errorMsg = "Test Navigation Error: ${e.message}"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        println(errorMsg)
                        e.printStackTrace()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Test Navigate to Achim Paribahan (Debug)")
            }
        }

        // Show bus count
        item {
            Text(
                text = "Found ${buses.size} bus routes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(buses) { bus ->
            println("=== RENDERING BUS ITEM ===")
            println("Bus name: ${bus.name_en ?: bus.name}")
            println("Bus ID: '${bus.id}'")

            BusItem(
                bus = bus,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    // Show immediate feedback
                    val busName = bus.name_en ?: bus.name
                    val busId = bus.id

                    Toast.makeText(context, "Clicked: $busName", Toast.LENGTH_SHORT).show()
                    println("=== BUS ITEM CLICKED ===")
                    println("Clicked bus: $busName")
                    println("Clicked bus ID: '$busId'")

                    // Validate the ID before navigation
                    if (busId.isBlank()) {
                        val errorMsg = "Error: Invalid bus ID for $busName!"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        println("ERROR: Bus ID is blank!")
                        return@BusItem
                    }

                    // Show the ID we're about to navigate with
                    Toast.makeText(context, "Navigating to ID: $busId", Toast.LENGTH_SHORT).show()
                    println("About to navigate to route_details/$busId")

                    // Navigate with the bus ID
                    val route = "route_details/$busId"
                    println("Attempting to navigate to: $route")

                    try {
                        println("Calling navController.navigate...")
                        navController.navigate(route)
                        println("Navigation command sent successfully")
                        Toast.makeText(context, "Navigation sent for: $busName", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        val errorMsg = "Navigation Error: ${e.message}"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        println("ERROR during navigation: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
fun EmptySearchStatePreview() {
    EmptySearchState()
}

@Preview(showBackground = true, name = "Bus List")
@Composable
fun BusListPreview() {
    val sampleBuses = listOf(
        BusRoute(id = "1", name_en = "7", name_bn = "৭", routes = Routes(forward = listOf("Gulisthan", "Mirpur 12")), service_type = "Local"),
        BusRoute(id = "2", name_en = "12", name_bn = "১২", routes = Routes(forward = listOf("Motijheel", "Uttara")), service_type = "Counter")
    )
    // Note: Preview doesn't work with NavController, so we create a simple version
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sampleBuses) { bus ->
            BusItem(
                bus = bus,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}