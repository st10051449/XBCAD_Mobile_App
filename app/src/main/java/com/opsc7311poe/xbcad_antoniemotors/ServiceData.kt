package com.opsc7311poe.xbcad_antoniemotors

import java.util.Date

data class ServiceData(
    var serviceID: String?,
    var name: String?,
    var custID: String?,
    var vehicleID: String?,
    var status: String?,
    var dateReceived: Date?,
    var dateReturned: Date?,
    var parts: List<Part>? ,
    var labourCost: Double?,
    var totalCost: Double?,
    var paid: Boolean?

){
    // No-argument constructor (required by Firebase)
    constructor() : this(null, null, null , null, null, null, null, null, null, null, null)
}

data class Part(
    var name: String?,
    var cost: Double?
){
    // No-argument constructor (required by Firebase)
    constructor() : this(null, null)
}

//service type class
data class ServiceTypeData(
    var name: String?,
    var parts: List<Part>? ,
    var labourCost: Double?,

){
    constructor() : this(null, null , null)
}

