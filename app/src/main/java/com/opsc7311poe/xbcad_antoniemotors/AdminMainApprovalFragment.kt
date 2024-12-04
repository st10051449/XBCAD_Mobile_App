package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


 class AdminMainApprovalFragment : Fragment() {

    private lateinit var ivSwitchRole: ImageView
    private lateinit var ivDeny: ImageView
    private lateinit var ivIgnore: ImageView
    private lateinit var ivApprove: ImageView
    private lateinit var btnBack: ImageView

    private lateinit var txtName: TextView
    private lateinit var txtSurname: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtPhone: TextView
    private lateinit var txtAddress: TextView
    private lateinit var txtRole: TextView

    private var userId: String? = null
    private var businessId: String? = null
    private var currentRole: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_main_approval, container, false)

        // Get userId and businessId from bundle
        userId = arguments?.getString("userId")
        businessId = arguments?.getString("businessId")

        if (userId == null || businessId == null) {
            Toast.makeText(context, "User or Business ID is missing.", Toast.LENGTH_SHORT).show()
            return view
        }

        // Initialize views
        txtName = view.findViewById(R.id.txtAppName)
        txtSurname = view.findViewById(R.id.txtAppSurname)
        txtEmail = view.findViewById(R.id.txtAppEmail)
        txtPhone = view.findViewById(R.id.txtAppPhone)
        txtAddress = view.findViewById(R.id.txtAppAddress)
        txtRole = view.findViewById(R.id.txtAppRole)
        ivSwitchRole = view.findViewById(R.id.ivSwitchRole)
        ivDeny = view.findViewById(R.id.ivDenyCross)
        ivIgnore = view.findViewById(R.id.ivIgnoreClock)
        ivApprove = view.findViewById(R.id.ivApproveTick)

        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener(){
            replaceFragment(AdminApproveRegistrationFragment())
        }

        fetchUserData()

        ivSwitchRole.setOnClickListener { toggleRole() }
        ivApprove.setOnClickListener { approveUser() }
        ivDeny.setOnClickListener { denyUser() }
        ivIgnore.setOnClickListener { replaceFragment(AdminApproveRegistrationFragment()) }

        return view
    }

    private fun fetchUserData() {
        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(businessId!!).child("Pending").child(userId!!)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                    val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                    val address = snapshot.child("address").getValue(String::class.java) ?: ""
                    val role = snapshot.child("role").getValue(String::class.java) ?: ""

                    txtName.text = firstName
                    txtSurname.text = lastName
                    txtEmail.text = email
                    txtPhone.text = phone
                    txtAddress.text = address
                    txtRole.text = role

                    currentRole = role
                } else {
                    Toast.makeText(context, "User data not found in Pending node.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load user data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun toggleRole() {
        currentRole = if (currentRole == "admin") "employee" else "admin"
        txtRole.text = currentRole

        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(businessId!!).child("Pending").child(userId!!)
        dbRef.child("role").setValue(currentRole).addOnSuccessListener {
            Toast.makeText(context, "Role updated to $currentRole.", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to update role.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun approveUser() {
        if (businessId == null) {
            Toast.makeText(context, "Business ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        val pendingRef = FirebaseDatabase.getInstance().getReference("Users").child(businessId!!).child("Pending").child(userId!!)
        val employeeRef = FirebaseDatabase.getInstance().getReference("Users").child(businessId!!).child("Employees").child(userId!!)

        pendingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val employeeData = snapshot.value
                if (employeeData != null) {
                    employeeRef.setValue(employeeData).addOnSuccessListener {
                        employeeRef.child("approval").setValue("approved")
                        pendingRef.removeValue().addOnSuccessListener {
                            Toast.makeText(context, "User approved and moved successfully.", Toast.LENGTH_SHORT).show()
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    }
                } else {
                    Toast.makeText(context, "No data to move.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to move user.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun denyUser() {
        if (businessId == null) {
            Toast.makeText(context, "Business ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(businessId!!).child("Pending").child(userId!!)
        dbRef.child("approval").setValue("denied").addOnSuccessListener {
            Toast.makeText(context, "User denied.", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to deny user.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
