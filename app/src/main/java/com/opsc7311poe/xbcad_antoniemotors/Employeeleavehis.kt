package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class Employeeleavehis : Fragment() {

    private lateinit var leaveHistoryContainer: LinearLayout
    private lateinit var btnApproved: Button
    private lateinit var btnDenied: Button
    private lateinit var btnPend: Button
    private lateinit var btnBack: ImageView
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var employeeId: String
    private lateinit var businessId: String
    private var employeeName: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements
        leaveHistoryContainer = view.findViewById(R.id.leaveHistoryContainer)
        btnApproved = view.findViewById(R.id.btnApproved)
        btnDenied = view.findViewById(R.id.btnDenied)
        btnPend = view.findViewById(R.id.btnPending) // Initialize the new Pending button
        btnBack  = view.findViewById(R.id.ivBackButton)
        // Retrieve the business ID from shared preferences
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        businessId = sharedPref.getString("business_id", null) ?: return
        employeeId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Fetch and store the employee's name once
        fetchEmployeeName()

        // Set button listeners to toggle between approved, denied, and pending leave requests
        btnApproved.setOnClickListener { fetchEmployeeLeaveHistory("ApprovedLeave") }
        btnDenied.setOnClickListener { fetchEmployeeLeaveHistory("DeniedLeave") }
        btnPend.setOnClickListener { fetchEmployeeLeaveHistory("PendingLeave") } // Set up the Pending button

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


        // Load approved leave by default
        fetchEmployeeLeaveHistory("ApprovedLeave")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_employeeleavehis, container, false)
    }

    // Fetch the employee's name from Firebase
    private fun fetchEmployeeName() {
        val employeeRef = database.child("Users").child(businessId).child("Employees").child(employeeId)

        employeeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                employeeName = "$firstName $lastName"
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    // Fetch leave history specific to the logged-in employee based on leave type
    private fun fetchEmployeeLeaveHistory(leaveType: String) {
        leaveHistoryContainer.removeAllViews() // Clear previous entries

        val leaveRef = database.child("Users").child(businessId)
            .child("Employees").child(employeeId).child(leaveType)

        leaveRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var leaveFound = false // Track if any leaves are found

                for (leaveSnapshot in snapshot.children) {
                    val leaveStart = leaveSnapshot.child("startDate").getValue(String::class.java) ?: continue
                    val leaveEnd = leaveSnapshot.child("endDate").getValue(String::class.java) ?: continue
                    val specificLeaveType = leaveSnapshot.child("leaveType").getValue(String::class.java) ?: "N/A"
                    val leaveStatus = when (leaveType) {
                        "ApprovedLeave" -> "Approved"
                        "DeniedLeave" -> "Denied"
                        else -> "Pending" // Set status as Pending for pending requests
                    }

                    // Add leave information to the UI
                    addLeaveCard(specificLeaveType, leaveStart, leaveEnd, leaveStatus)
                    leaveFound = true
                }

                if (!leaveFound) {
                    Toast.makeText(requireContext(), "No ${leaveType.replace("Leave", "").toLowerCase()} leave history found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load leave history.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Add a card for each leave entry to the layout
    private fun addLeaveCard(leaveType: String, startDate: String, endDate: String, status: String) {
        // Use different card layouts based on the leave status
        val layoutRes = when (status) {
            "Approved" -> R.layout.card_approved_leave
            "Denied" -> R.layout.card_dennied_leave
            "Pending" -> R.layout.card_pending_leave
            else -> R.layout.card_pending_leave // Default to pending if not specified
        }

        val leaveCard = layoutInflater.inflate(layoutRes, leaveHistoryContainer, false) as CardView

        // Find and set values for each TextView in the card
        val txtEmployeeName = leaveCard.findViewById<TextView>(R.id.txtEmployeeName)
        val txtLeaveType = leaveCard.findViewById<TextView>(R.id.txtLeaveType)
        val txtStartDate = leaveCard.findViewById<TextView>(R.id.txtStartDate)
        val txtEndDate = leaveCard.findViewById<TextView>(R.id.txtEndDate)
        val txtStatus = leaveCard.findViewById<TextView>(R.id.txtstatus)

        // Set the data in each TextView
        txtEmployeeName.text = employeeName ?: "Employee Name"
        txtLeaveType.text = leaveType
        txtStartDate.text = startDate
        txtEndDate.text = endDate
        txtStatus.text = status

        // Add the populated card to the container
        leaveHistoryContainer.addView(leaveCard)
    }
}