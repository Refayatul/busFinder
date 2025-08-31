package com.rex.busfinder.data.model

data class BusRoute(
    val id: String,
    val name_en: String,
    val name_bn: String,
    val routes: Routes,
    val service_type: String
)

data class Routes(
    val forward: List<String>,
    val backward: List<String>? = null
)

fun Routes.getBackwardRoute(): List<String> {
    return backward ?: forward.reversed()
}