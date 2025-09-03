package com.rex.busfinder.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.rex.busfinder.R
import com.rex.busfinder.data.model.BusRoute
import com.rex.busfinder.viewmodel.BusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailsScreen(
    navController: NavController,
    routeId: String,
    viewModel: BusViewModel = viewModel()
) {
    val route by viewModel.getBusRoute(routeId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.route_details)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            route == null -> {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            route != null -> {
                RouteDetailsContent(
                    modifier = Modifier.padding(padding),
                    route = route!!
                )
            }
        }
    }
}

@Composable
fun RouteDetailsContent(
    modifier: Modifier = Modifier,
    route: BusRoute
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Route Header
        item {
            RouteHeaderCard(route = route)
        }

        // Service Type
        if (!route.service_type.isNullOrBlank()) {
            item {
                ServiceTypeCard(serviceType = route.service_type!!)
            }
        }

        // Forward Route
        item {
            Text(
                text = stringResource(R.string.forward_route),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        itemsIndexed(route.routes.forward) { index, stop ->
            StopItem(
                stopName = stop,
                stopNumber = index + 1,
                isFirst = index == 0,
                isLast = index == route.routes.forward.size - 1
            )
        }

        // Backward Route (if available)
        if (!route.routes.backward.isNullOrEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.backward_route),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            itemsIndexed(route.routes.backward!!) { index, stop ->
                StopItem(
                    stopName = stop,
                    stopNumber = index + 1,
                    isFirst = index == 0,
                    isLast = index == route.routes.backward!!.size - 1,
                    isBackward = true
                )
            }
        }
    }
}

@Composable
fun RouteHeaderCard(route: BusRoute) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Route number/name
            Text(
                text = route.name_en ?: route.name.ifEmpty { "Unknown Route" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Bengali name
            if (!route.name_bn.isNullOrBlank()) {
                Text(
                    text = route.name_bn!!,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RouteStat(
                    icon = Icons.Default.Place,
                    label = "Stops",
                    value = route.routes.forward.size.toString()
                )

                if (!route.routes.backward.isNullOrEmpty()) {
                    RouteStat(
                        icon = Icons.Default.CompareArrows,
                        label = "Directions",
                        value = "2"
                    )
                }

                RouteStat(
                    icon = Icons.Default.DirectionsBus,
                    label = "Total Stops",
                    value = (route.routes.forward.size + (route.routes.backward?.size ?: 0)).toString()
                )
            }
        }
    }
}

@Composable
fun ServiceTypeCard(serviceType: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Service Type: $serviceType",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun RouteStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StopItem(
    stopName: String,
    stopNumber: Int,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    isBackward: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timeline indicator
        Box(
            modifier = Modifier.width(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Line above (if not first)
                if (!isFirst) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(
                                color = if (isBackward)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                    )
                }

                // Stop circle
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (isBackward)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.primary
                        )
                )

                // Line below (if not last)
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(
                                color = if (isBackward)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
        }

        // Stop number and name
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = "$stopNumber. $stopName",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            if (isFirst) {
                Text(
                    text = if (isBackward) "Starting Point (Return)" else "Starting Point",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (isLast) {
                Text(
                    text = if (isBackward) "Final Stop (Return)" else "Final Stop",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Direction indicator for backward routes
        if (isBackward) {
            Icon(
                imageVector = Icons.Default.CompareArrows,
                contentDescription = "Return route",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}