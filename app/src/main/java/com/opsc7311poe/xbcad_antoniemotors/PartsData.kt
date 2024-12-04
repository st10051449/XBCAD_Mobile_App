package com.opsc7311poe.xbcad_antoniemotors

data class PartsData(
    var id: String? = null,
    val partName: String? = null,
    val partDescription: String? = null,
    val stockCount: Int? = null,
    val minStock: Int? = null,
    val costPrice: Double? = null
)

{
    constructor() : this(null,null, null, 0, 0, null)
}
