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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class AdminLeavehistory : Fragment() {

    private lateinit var leaveHistoryContainer: LinearLayout
    private lateinit var btnApproved: Button
    private lateinit var btnDenied: Button
    private lateinit var btnback : ImageView
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var businessID: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements
        leaveHistoryContainer = view.findViewById(R.id.leaveHistoryContainer)
        btnApproved = view.findViewById(R.id.btnApproved)
        btnDenied = view.findViewById(R.id.btnDenied)
        btnback =  view.findViewById(R.id.ivBackButton)


        // Retrieve business ID from shared preferences
        businessID = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("business_id", null) ?: return

        // Set button listeners to toggle between approved and denied leave requests
        btnApproved.setOnClickListener { fetchLeaveHistory("ApprovedLeave") }
        btnDenied.setOnClickListener { fetchLeaveHistory("DeniedLeave") }


        btnback.setOnClickListener {
            parentFragmentManager.popBackStack() // Navigate back to the previous fragment
        }

        // Load approved leave by default
        fetchLeaveHistory("ApprovedLeave")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin_leavehistory, container, false)
    }

    // Function to fetch leave history based on leave type (approved or denied)
    private fun fetchLeaveHistory(leaveType: String) {
        leaveHistoryContainer.removeAllViews() // Clear previous entries

        val employeesRef = database.child("Users").child(businessID).child("Employees")

        employeesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var leaveFound = false // Track if any leaves are found

                for (employeeSnapshot in snapshot.children) {
                    val employeeId = employeeSnapshot.key ?: continue
                    val employeeName = "${employeeSnapshot.child("firstName").getValue(String::class.java)} ${employeeSnapshot.child("lastName").getValue(String::class.java)}"
                    val leaves = employeeSnapshot.child(leaveType)

                    for (leaveSnapshot in leaves.children) {
                        val leaveStart = leaveSnapshot.child("startDate").getValue(String::class.java) ?: continue
                        val leaveEnd = leaveSnapshot.child("endDate").getValue(String::class.java) ?: continue
                        val specificLeaveType = leaveSnapshot.child("leaveType").getValue(String::class.java) ?: "N/A"

                        // Set the status correctly based on the leave type
                        val leaveStatus = if (leaveType == "ApprovedLeave") "Approved" else "Denied"

                        // Add leave information to the UI
                        addLeaveCard(employeeName, specificLeaveType, leaveStart, leaveEnd, leaveStatus)
                        leaveFound = true
                    }
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
    private fun addLeaveCard(employeeName: String, leaveType: String, startDate: String, endDate: String, status: String) {
        // Inflate the appropriate card layout based on the leave status
        val layoutRes = if (status == "Approved") R.layout.card_approved_leave else R.layout.card_dennied_leave
        val leaveCard = layoutInflater.inflate(layoutRes, leaveHistoryContainer, false) as CardView

        // Find and set values for each TextView in the card
        val txtEmployeeName = leaveCard.findViewById<TextView>(R.id.txtEmployeeName)
        val txtLeaveType = leaveCard.findViewById<TextView>(R.id.txtLeaveType)
        val txtStartDate = leaveCard.findViewById<TextView>(R.id.txtStartDate)
        val txtEndDate = leaveCard.findViewById<TextView>(R.id.txtEndDate)
        val txtStatus = leaveCard.findViewById<TextView>(R.id.txtstatus)

        // Set the data in each TextView
        txtEmployeeName.text = employeeName
        txtLeaveType.text = leaveType
        txtStartDate.text = startDate
        txtEndDate.text = endDate
        txtStatus.text = status

        // Add the populated card to the container
        leaveHistoryContainer.addView(leaveCard)
    }
}