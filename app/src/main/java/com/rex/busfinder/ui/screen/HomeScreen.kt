package com.rex.busfinder.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.rex.busfinder.R
import com.rex.busfinder.data.model.SearchHistoryItem
import com.rex.busfinder.ui.component.SearchBar
import com.rex.busfinder.viewmodel.BusViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: BusViewModel = viewModel()
) {
    val recentSearches by viewModel.recentSearches.observeAsState(emptyList())
    val fromSearchQuery by viewModel.fromSearchQuery.observeAsState("")
    val toSearchQuery by viewModel.toSearchQuery.observeAsState("")
    val fromSuggestions by viewModel.fromSuggestions.observeAsState(emptyList())
    val toSuggestions by viewModel.toSuggestions.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    onNavigate = { destination ->
                        scope.launch {
                            drawerState.close()
                            // Handle navigation
                            when(destination) {
                                "map" -> { /* Navigate to map */ }
                                "favorites" -> { /* Navigate to favorites */ }
                                "settings" -> { /* Navigate to settings */ }
                                "about" -> { /* Navigate to about */ }
                                "feedback" -> { /* Navigate to feedback */ }
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Find your bus route easily",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = fromSearchQuery.isNotBlank() && toSearchQuery.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            viewModel.searchBuses(fromSearchQuery, toSearchQuery)
                            navController.navigate("search")
                        },
                        icon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search Buses",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        },
                        text = {
                            Text(
                                "Search Buses",
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Section with Search
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            // IMPROVEMENT: Changed background to surfaceContainerHighest
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Where would you like to go?",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                // IMPROVEMENT: Changed text color to onSurface for contrast with surfaceContainerHighest
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // From Search Bar (assuming SearchBar takes its own colors from theme)
                            SearchBar(
                                value = fromSearchQuery,
                                onValueChange = { newValue ->
                                    viewModel.updateFromQuery(newValue)
                                },
                                label = stringResource(id = R.string.search_from_hint),
                                suggestions = fromSuggestions,
                                onSuggestionClick = { suggestion ->
                                    viewModel.updateFromQuery(suggestion)
                                },
                                onClear = {
                                    viewModel.updateFromQuery("")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Swap Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        val temp = fromSearchQuery
                                        viewModel.updateFromQuery(toSearchQuery)
                                        viewModel.updateToQuery(temp)
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = "Swap locations",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // To Search Bar
                            SearchBar(
                                value = toSearchQuery,
                                onValueChange = { newValue ->
                                    viewModel.updateToQuery(newValue)
                                },
                                label = stringResource(id = R.string.search_to_hint),
                                suggestions = toSuggestions,
                                onSuggestionClick = { suggestion ->
                                    viewModel.updateToQuery(suggestion)
                                },
                                onClear = {
                                    viewModel.updateToQuery("")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Quick Actions Section
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionCard(
                                icon = Icons.Default.Map,
                                title = "Route Map",
                                onClick = { /* Navigate to map */ },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                icon = Icons.Default.Favorite,
                                title = "Favorites",
                                onClick = { /* Navigate to favorites */ },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Popular Routes Section
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Popular Routes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { /* Show all popular routes */ }) {
                                Text("See All")
                            }
                        }

                        // Sample popular routes
                        PopularRouteCard(
                            from = "Mirpur 10",
                            to = "Farmgate",
                            buses = "5 buses available",
                            onClick = {
                                viewModel.updateFromQuery("Mirpur 10")
                                viewModel.updateToQuery("Farmgate")
                                viewModel.searchBuses("Mirpur 10", "Farmgate")
                                navController.navigate("search")
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        PopularRouteCard(
                            from = "Uttara",
                            to = "Motijheel",
                            buses = "8 buses available",
                            onClick = {
                                viewModel.updateFromQuery("Uttara")
                                viewModel.updateToQuery("Motijheel")
                                viewModel.searchBuses("Uttara", "Motijheel")
                                navController.navigate("search")
                            }
                        )
                    }
                }

                // Recent Searches Section
                if (recentSearches.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.recent_searches),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = { viewModel.clearSearchHistory() }
                                ) {
                                    Text(
                                        text = "Clear All",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    items(recentSearches.take(5)) { item ->
                        RecentSearchCard(
                            item = item,
                            onClick = {
                                viewModel.updateFromQuery(item.fromLocation)
                                viewModel.updateToQuery(item.toLocation)
                                viewModel.searchBuses(item.fromLocation, item.toLocation)
                                navController.navigate("search")
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Loading Indicator
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PopularRouteCard(
    from: String,
    to: String,
    buses: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = "Popular",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = from,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "to",
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = to,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = buses,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentSearchCard(
    item: SearchHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Recent search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.fromLocation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "to",
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.toLocation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DrawerContent(
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = "App Logo",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BUS Finder",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Menu Items
        DrawerMenuItem(
            icon = Icons.Default.Home,
            title = "Home",
            onClick = { /* Already on home */ }
        )
        DrawerMenuItem(
            icon = Icons.Default.Map,
            title = "Route Map",
            onClick = { onNavigate("map") }
        )
        DrawerMenuItem(
            icon = Icons.Default.Favorite,
            title = "Favorite Routes",
            onClick = { onNavigate("favorites") }
        )
        // REMOVED: DrawerMenuItem for "Bus Schedule"

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        DrawerMenuItem(
            icon = Icons.Default.Settings,
            title = "Settings",
            onClick = { onNavigate("settings") }
        )
        DrawerMenuItem(
            icon = Icons.Default.Info,
            title = "About",
            onClick = { onNavigate("about") }
        )
        DrawerMenuItem(
            icon = Icons.Default.Feedback,
            title = "Feedback",
            onClick = { onNavigate("feedback") }
        )
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}