package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminApproveRegistrationFragment : Fragment() {

    private lateinit var linlayEmployees: LinearLayout
    private var adminId: String? = null // ID of the current logged-in admin
    private lateinit var btnBack: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_approve_registration, container, false)
        linlayEmployees = view.findViewById(R.id.linlayEmployees)

        // Get the current admin's ID from Firebase Auth
        adminId = FirebaseAuth.getInstance().currentUser?.uid

        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener {
            replaceFragment(AdminEmpFragment()) // Going back to menu
        }

        // Fetch pending users from Firebase if adminId is available
        if (adminId != null) {
            fetchPendingUsers()
        } else {
            Toast.makeText(context, "User not authenticated.", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun fetchPendingUsers() {
        val dbRef = FirebaseDatabase.getInstance().getReference("Users")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                linlayEmployees.removeAllViews() // Clear layout

                var hasPendingUsers = false // Flag to check if there are pending users

                // Loop through each business node
                for (businessSnapshot in snapshot.children) {
                    val businessId = businessSnapshot.key ?: continue
                    val pendingNode = businessSnapshot.child("Pending")

                    // Fetch each pending user under the business
                    if (pendingNode.exists()) {
                        for (pendingSnapshot in pendingNode.children) {
                            val firstName = pendingSnapshot.child("firstName").getValue(String::class.java)
                            val lastName = pendingSnapshot.child("lastName").getValue(String::class.java)
                            val role = pendingSnapshot.child("role").getValue(String::class.java)
                            val userId = pendingSnapshot.key
                            val managerId = pendingSnapshot.child("managerID").getValue(String::class.java)

                            // Display only employees with matching managerID
                            if (firstName != null && lastName != null && role != null && userId != null && managerId == adminId) {
                                addEmployeeToLayout("$firstName $lastName", role, userId, businessId)
                                hasPendingUsers = true
                            }
                        }
                    }
                }

                // Dynamically add the "No Pending Requests" message if no pending users were found
                if (!hasPendingUsers) {
                    val noPendingTextView = TextView(context)
                    noPendingTextView.text = "No Pending Requests"
                    noPendingTextView.textSize = 18f
                    noPendingTextView.setPadding(16, 16, 16, 16)
                    noPendingTextView.setTextColor(resources.getColor(android.R.color.black))
                    linlayEmployees.addView(noPendingTextView)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load pending employees.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.frame_container, fragment)?.commit()
    }

    private fun addEmployeeToLayout(
        name: String,
        role: String,
        userId: String,
        businessId: String
    ) {
        val employeeLayout = LinearLayout(context)
        employeeLayout.orientation = LinearLayout.VERTICAL
        employeeLayout.setPadding(0, 16, 0, 16)

        val nameTextView = TextView(context)
        nameTextView.text = name
        nameTextView.textSize = 20f
        nameTextView.setPadding(16, 0, 16, 8)

        val roleTextView = TextView(context)
        roleTextView.text = role
        roleTextView.textSize = 14f
        roleTextView.setPadding(16, 0, 16, 8)

        val dividerView = View(context)
        dividerView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 2
        )
        dividerView.setBackgroundColor(resources.getColor(android.R.color.black))

        employeeLayout.addView(nameTextView)
        employeeLayout.addView(roleTextView)
        employeeLayout.addView(dividerView)

        employeeLayout.setOnClickListener {
            val bundle = Bundle().apply {
                putString("userId", userId)
                putString("businessId", businessId)
            }
            val adminMainApprovalFragment = AdminMainApprovalFragment().apply {
                arguments = bundle
            }
            requireFragmentManager().beginTransaction()
                .replace(R.id.frame_container, adminMainApprovalFragment)
                .addToBackStack(null)
                .commit()
        }

        linlayEmployees.addView(employeeLayout)
    }
}
