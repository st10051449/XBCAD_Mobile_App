package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LeaveApproval : Fragment() {
    private val database = Firebase.database

    private lateinit var ivDenyL: ImageView
    private lateinit var ivApproveL: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var txtName: TextView
    private lateinit var txtLeaveType: TextView
    private lateinit var txtStartDate: TextView
    private lateinit var txtEndDate: TextView
    private lateinit var txtTotalDuration: TextView

    private var userId: String? = null
    private var businessId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve arguments in onCreate
        arguments?.let {
            userId = it.getString("userId")
            businessId = it.getString("businessId")
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_leave_approval, container, false)

        if (userId == null || businessId == null) {
            Toast.makeText(context, "User or Business ID is missing.", Toast.LENGTH_SHORT).show()
        } else {
            // Initialize views
            txtName = view.findViewById(R.id.txtEmpFullName)
            txtLeaveType = view.findViewById(R.id.txtEmpLeaveType)
            txtStartDate = view.findViewById(R.id.txtEmpStartDate)
            txtEndDate = view.findViewById(R.id.txtEndDate)
            txtTotalDuration = view.findViewById(R.id.txtEmpDuration)
            ivDenyL = view.findViewById(R.id.ivDenyCross)
            ivApproveL = view.findViewById(R.id.ivApproveTick)
            btnBack = view.findViewById(R.id.ivBackButton)

            // Fetch user data
            fetchUserData(businessId!!, userId!!)
        }
       ivDenyL.setOnClickListener { denyUser() }
        ivApproveL.setOnClickListener { approveUser() }

        btnBack.setOnClickListener {
            replaceFragment(AdminLeaveMenuFragment())
        }

        return view
    }

    private fun fetchUserData(businessId: String, employeeId: String) {
        val leaveRef = database.reference.child("Users").child(businessId).child("Employees")
            .child(employeeId).child("PendingLeave")

        leaveRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (leaveSnapshot in snapshot.children) {
                        val username = leaveSnapshot.child("userName").getValue(String::class.java) ?: ""
                        val leaveType = leaveSnapshot.child("leaveType").getValue(String::class.java) ?: ""
                        val start = leaveSnapshot.child("startDate").getValue(String::class.java) ?: ""
                        val end = leaveSnapshot.child("endDate").getValue(String::class.java) ?: ""
                        val totalDays = leaveSnapshot.child("duration").getValue(Long::class.java)?.toString() ?: "N/A" // Convert to String

                        // Populate the TextViews with data from the database
                        txtName.text = username
                        txtLeaveType.text = leaveType
                        txtStartDate.text = start
                        txtEndDate.text = end
                        txtTotalDuration.text = totalDays
                    }
                } else {
                    Toast.makeText(context, "User request not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load user request.", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun approveUser() {
        if (businessId == null || userId == null) {
            Toast.makeText(context, "Business ID or User ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        val pendingRef = database.getReference("Users/$businessId/Employees/$userId/PendingLeave")
        val approvedRef = database.getReference("Users/$businessId/Employees/$userId/ApprovedLeave")
        val employeeLeaveRef = database.getReference("Users/$businessId/Employees/$userId/Leave")

        pendingRef.orderByChild("status").equalTo("pending").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (leaveSnapshot in snapshot.children) {
                    val leaveData = leaveSnapshot.value
                    val leaveType = leaveSnapshot.child("leaveType").getValue(String::class.java) ?: return
                    val duration = leaveSnapshot.child("duration").getValue(Long::class.java) ?: 0L

                    // Move data to ApprovedLeave node
                    approvedRef.child(leaveSnapshot.key!!).setValue(leaveData).addOnSuccessListener {
                        approvedRef.child(leaveSnapshot.key!!).child("status").setValue("approved")

                        // Retrieve current leaveDays and subtract the duration
                        employeeLeaveRef.child(leaveType).child("leaveDays").addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(daysSnapshot: DataSnapshot) {
                                val currentLeaveDays = daysSnapshot.getValue(Long::class.java) ?: return
                                if (currentLeaveDays >= duration) {
                                    val newLeaveDays = currentLeaveDays - duration

                                    // Update the leaveDays for the leave type
                                    employeeLeaveRef.child(leaveType).child("leaveDays").setValue(newLeaveDays)
                                        .addOnSuccessListener {
                                            // Remove the request from PendingLeave
                                            leaveSnapshot.ref.removeValue().addOnSuccessListener {
                                                requireActivity().supportFragmentManager.popBackStack()
                                            }
                                        }.addOnFailureListener {
                                            Toast.makeText(context, "Failed to update leave days.", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    Toast.makeText(context, "Not enough leave days available.", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(context, "Failed to retrieve leave days.", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to move leave request.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to approve leave request.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun denyUser() {
        if (businessId == null || userId == null) {
            Toast.makeText(context, "Business ID or User ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        val pendingRef = FirebaseDatabase.getInstance().getReference("Users").child(businessId!!).child("Employees").child(userId!!).child("PendingLeave")
        val deniedRef = FirebaseDatabase.getInstance().getReference("Users").child(businessId!!).child("Employees").child(userId!!).child("DeniedLeave")

        pendingRef.orderByChild("status").equalTo("pending").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (leaveSnapshot in snapshot.children) {
                    val leaveData = leaveSnapshot.value

                    // Move data to DeniedLeave node
                    deniedRef.child(leaveSnapshot.key!!).setValue(leaveData).addOnSuccessListener {
                        deniedRef.child(leaveSnapshot.key!!).child("status").setValue("denied")

                        // Remove from PendingLeave node
                        leaveSnapshot.ref.removeValue().addOnSuccessListener {
                            Toast.makeText(context, "Leave denied and moved successfully.", Toast.LENGTH_SHORT).show()
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to deny leave request.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}