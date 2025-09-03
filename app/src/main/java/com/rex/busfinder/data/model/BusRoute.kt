package com.rex.busfinder.data.model

data class BusRoute(
    val id: String,
    val name: String = "", // Fallback for compatibility
    val name_en: String? = null,
    val name_bn: String? = null,
    val routes: Routes,
    val service_type: String? = null,
    val type: String? = null // For backward compatibility if needed
)

data class Routes(
    val forward: List<String> = emptyList(),
    val backward: List<String>? = null
)

// Data class for journey segments in multi-route trips
data class JourneySegment(
    val bus: BusRoute,
    val fromStop: String,
    val toStop: String,
    val direction: String = "forward" // "forward" or "backward"
)

// Data class for complete journey with multiple segments
data class JourneyPlan(
    val segments: List<JourneySegment>,
    val totalStops: Int,
    val estimatedTime: String? = null
)
