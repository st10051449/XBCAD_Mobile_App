
package com.opsc7311poe.xbcad_antoniemotors

import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

data class CustomerData(
    var CustomerID: String = "",
    var BusinessID: String ="",
    var CustomerName: String = "",
    var CustomerSurname: String = "",
    var CustomerMobileNum: String = "",
    var CustomerEmail: String = "",
    var CustomerAddress: String = "",
    var CustomerType: String = "",
    var CustomerPassword: String="",
    var CustomerAddedDate: String = ""
)
