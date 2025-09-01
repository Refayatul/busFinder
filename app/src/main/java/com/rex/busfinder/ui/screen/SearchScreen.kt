package com.rex.busfinder.ui.screen

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
            isLoading = isLoading,
            searchResults = searchResults,
            onBusClick = { bus ->
                // TODO: Navigate to bus details
            }
        )
    }
}

@Composable
fun SearchScreenContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    searchResults: List<BusRoute>,
    onBusClick: (BusRoute) -> Unit
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
                buses = searchResults,
                onBusClick = onBusClick
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
    buses: List<BusRoute>,
    onBusClick: (BusRoute) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(buses) { bus ->
            BusItem(
                bus = bus,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBusClick(bus) }
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
    BusList(buses = sampleBuses, onBusClick = {})
}