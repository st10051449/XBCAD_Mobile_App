package com.opsc7311poe.xbcad_antoniemotors

data class EmpTask(
    var taskID: String?,
    var taskName: String?,
    var taskDescription: String?,
    var employeeID: String?,
    var serviceID: String?,
    var adminID: String?,
    var vehicleID: String?,
    var creationDate: Long?,      // New property for task creation date
    var completedDate: Long?,      // New property for task completion date
    var dueDate: Long?,      // New property for task completion date
    var taskApprovalRequired: Boolean?,
    var status: String?
){
    // No-argument constructor (required by Firebase)
    constructor() : this(null, null, null, null , null, null, null, null, null, null, null, null)
}

