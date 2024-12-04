package com.opsc7311poe.xbcad_antoniemotors

import android.os.Parcel
import android.os.Parcelable

data class Tasks(
    var taskID: String? = null,
    val taskName: String? = null,
    val taskDescription: String? = null,
    val vehicleNumberPlate: String? = null,
    val creationDate: Long? = null,      // Property for task creation date
    val completedDate: Long? = null      // Property for task completion date
) : Parcelable {
    constructor() : this(null, null, null, null, null, null)  // No-argument constructor for Firebase

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readLong().takeIf { it != -1L },
        parcel.readLong().takeIf { it != -1L }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(taskID)
        parcel.writeString(taskName)
        parcel.writeString(taskDescription)
        parcel.writeString(vehicleNumberPlate)
        parcel.writeLong(creationDate ?: -1)  // Write creation date to parcel
        parcel.writeLong(completedDate ?: -1) // Write completed date to parcel
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Tasks> {
        override fun createFromParcel(parcel: Parcel): Tasks {
            return Tasks(parcel)
        }

        override fun newArray(size: Int): Array<Tasks?> {
            return arrayOfNulls(size)
        }
    }
}
