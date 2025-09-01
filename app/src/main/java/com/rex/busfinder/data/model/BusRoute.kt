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