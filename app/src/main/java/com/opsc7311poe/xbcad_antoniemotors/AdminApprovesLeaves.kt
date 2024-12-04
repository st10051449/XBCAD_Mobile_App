package com.opsc7311poe.xbcad_antoniemotors

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
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale

class AdminApprovesLeaves : Fragment() {
    private lateinit var leaveRequestContainer: LinearLayout
    private lateinit var txtNoLeaveRequests: TextView
    private lateinit var businessID: String
    private lateinit var btnBack: ImageView
    private val database = Firebase.database
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val view = inflater.inflate(R.layout.fragment_admin_approves_leaves, container, false)
        leaveRequestContainer = view.findViewById(R.id.leaveRequestContainer)

        txtNoLeaveRequests = view.findViewById(R.id.txtNoLeaveRequests)

        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener {
            replaceFragment(AdminLeaveMenuFragment()) // Going back to menu
        }


        fetchAdminBusinessID()

        return view
    }

    // Fetch the current logged-in admin's BusinessID
    private fun fetchAdminBusinessID() {
        val currentAdminId = auth.currentUser?.uid ?: return
        val employeeRef = database.reference.child("Users")

        employeeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (businessSnapshot in snapshot.children) {
                    val employees = businessSnapshot.child("Employees")
                    if (employees.hasChild(currentAdminId)) {
                        // The admin is associated with this business
                        businessID = businessSnapshot.key.toString()
                        // Now fetch leave requests for this business
                        fetchLeaveRequestsForBusiness(businessID, currentAdminId)
                        break
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to retrieve business ID", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.frame_container, fragment)?.commit()
    }


    // Fetch leave requests for the business where managerID matches the logged-in admin
    private fun fetchLeaveRequestsForBusiness(businessId: String, managerId: String) {
        val employeesRef = database.reference.child("Users").child(businessId).child("Employees")
        var hasRequests = false
        var pendingCallbacks = 0 // Track the number of callbacks still running

        employeesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                leaveRequestContainer.removeAllViews() // Clear previous leave requests
                txtNoLeaveRequests.visibility = View.GONE // Hide message initially
                val employees = snapshot.children.toList()

                if (employees.isEmpty()) {
                    // No employees found
                    txtNoLeaveRequests.visibility = View.VISIBLE
                    return
                }

                for (employeeSnapshot in employees) {
                    val employeeId = employeeSnapshot.key
                    val managerID = employeeSnapshot.child("managerID").getValue(String::class.java)

                    if (managerID == managerId) {
                        pendingCallbacks++ // Increment pending callbacks
                        fetchPendingLeaveRequests(businessId, employeeId) { hasEmployeeRequests ->
                            if (hasEmployeeRequests) {
                                hasRequests = true
                            }
                            pendingCallbacks-- // Decrement when callback finishes

                            // Check if all callbacks are done
                            if (pendingCallbacks == 0) {
                                // Update visibility based on whether requests were found
                                if (!hasRequests) {
                                    txtNoLeaveRequests.visibility = View.VISIBLE
                                }
                            }
                        }
                    }
                }

                // If no employees matched, show the "No Requests" message
                if (pendingCallbacks == 0 && !hasRequests) {
                    txtNoLeaveRequests.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to fetch employees", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Fetch pending leave requests for the employee
    private fun fetchPendingLeaveRequests(
        businessId: String,
        employeeId: String?,
        callback: (Boolean) -> Unit
    ) {
        if (employeeId == null) {
            callback(false)
            return
        }

        val leaveRef = database.reference.child("Users").child(businessId).child("Employees")
            .child(employeeId).child("PendingLeave")

        leaveRef.orderByChild("status").equalTo("pending").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (leaveSnapshot in snapshot.children) {
                        val leaveType = leaveSnapshot.child("leaveType").getValue(String::class.java) ?: "N/A"
                        val startDate = leaveSnapshot.child("startDate").getValue(String::class.java) ?: "N/A"
                        val endDate = leaveSnapshot.child("endDate").getValue(String::class.java) ?: "N/A"

                        fetchEmployeeName(employeeId, leaveType, startDate, endDate, businessId)
                    }
                    callback(true) // Found pending requests
                } else {
                    callback(false) // No pending requests
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to fetch leave requests", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }
    // Fetch the full name of the employee
    private fun fetchEmployeeName(employeeId: String, leaveType: String, startDate: String, endDate: String, businessId: String) {
        val employeeRef = database.reference.child("Users").child(businessId).child("Employees").child(employeeId)

        employeeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Retrieve first and last name from the database
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Unknown"
                val lastName = snapshot.child("lastName").getValue(String::class.java) ?: "Unknown"

                // Concatenate first and last name to get full name
                val fullName = "$firstName $lastName"

                // Now that we have the employee's full name, add the leave request to the layout
                addLeaveRequestToLayout(employeeId, fullName, leaveType, startDate, endDate, businessId)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to fetch employee name", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Add leave request to the UI for admin to approve
    private fun addLeaveRequestToLayout(
        employeeId: String,
        employeeName: String,
        leaveType: String,
        startDate: String,
        endDate: String,
        businessId: String
    ) {
        val cardView = layoutInflater.inflate(R.layout.card_leave_approval, leaveRequestContainer, false) as CardView

        val txtEmployeeName = cardView.findViewById<TextView>(R.id.txtEmployeeName)
        val txtLeaveType = cardView.findViewById<TextView>(R.id.txtLeaveType)
        val txtStartDate = cardView.findViewById<TextView>(R.id.txtStartDate)
        val txtEndDate = cardView.findViewById<TextView>(R.id.txtEndDate)
        val txtTotalDuration = cardView.findViewById<TextView>(R.id.txtTotalDuration)
        val btnViewMore = cardView.findViewById<Button>(R.id.btnRestoreTask)

        txtEmployeeName.text = employeeName
        txtLeaveType.text = leaveType
        txtStartDate.text = startDate
        txtEndDate.text = endDate

        val totalDuration = calculateLeaveDuration(startDate, endDate)
        txtTotalDuration.text = "$totalDuration days"

        btnViewMore.setOnClickListener {
            val bundle = Bundle().apply {
                putString("userId", employeeId)  // Pass the employee ID
                putString("businessId", businessId)  // Pass the business ID

            }
            val leaveApprovalFragment = LeaveApproval().apply {
                arguments = bundle
            }
            requireFragmentManager().beginTransaction()
                .replace(R.id.frame_container, leaveApprovalFragment)
                .addToBackStack(null)
                .commit()
        }

        leaveRequestContainer.addView(cardView)
    }

    // Helper function to calculate the total duration of leave
    private fun calculateLeaveDuration(startDate: String, endDate: String): Int {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            val start = format.parse(startDate)
            val end = format.parse(endDate)

            // Check if dates were parsed successfully
            if (start == null || end == null) {

                return 0
            }

            // Calculate the difference in milliseconds and convert to days
            val difference = end.time - start.time
            val daysDifference = (difference / (1000 * 60 * 60 * 24)).toInt()



            daysDifference
        } catch (e: Exception) {

            0
        }
    }
}
