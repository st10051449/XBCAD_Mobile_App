package com.opsc7311poe.xbcad_antoniemotors

data class RegisterEmployee(
var name: String?,
var surname: String?,
var salary: String?,
var totalLeave: String?,
var leaveLeft: String?,
var number: String?,
var email: String?,
var address: String?,
var role: String?,
var businessName: String?,
var registeredBy: String?,
var profileImage: String? = null
) {
    constructor() : this(null, null, null, null, null, null, null, null, null, null, null)
}

