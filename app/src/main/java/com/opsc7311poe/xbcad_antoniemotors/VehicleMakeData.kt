package com.opsc7311poe.xbcad_antoniemotors

data class VehicleMakeData(
    val make: String = "", // e.g., "BMW"
    val models: Map<String, String> = emptyMap() // key: "1", value: "320i", etc.
)