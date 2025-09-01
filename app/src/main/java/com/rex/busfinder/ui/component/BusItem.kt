package com.rex.busfinder.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rex.busfinder.R
import com.rex.busfinder.data.model.BusRoute

@Composable
fun BusItem(
    bus: BusRoute,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored box indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Bus information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // English name (with fallback)
                Text(
                    text = bus.name_en ?: bus.name.ifEmpty { "Unknown Route" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Bangla name (if available)
                bus.name_bn?.takeIf { it.isNotEmpty() }?.let { nameBn ->
                    Text(
                        text = nameBn,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Service type (if available)
                bus.service_type?.takeIf { it.isNotEmpty() }?.let { serviceType ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = serviceType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Arrow icon
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.view_details)
            )
        }
    }
}