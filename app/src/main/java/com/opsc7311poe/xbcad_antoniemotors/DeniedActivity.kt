package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class DeniedActivity : AppCompatActivity() {

    private lateinit var btnRequestAgain: Button
    private lateinit var btnDeleteAcc: Button
    private lateinit var btnLogout: Button

    private var businessId: String? = null
    private var userId: String? = null
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_denied)

        btnDeleteAcc = findViewById(R.id.btnDeleteAccountDenied)
        btnRequestAgain = findViewById(R.id.btnRequestAccess)
        btnLogout = findViewById(R.id.btnLogout)

        // Fetch businessId and userId from Firebase
        fetchUserData()

        btnRequestAgain.setOnClickListener {
            updateStatus()
        }

        btnDeleteAcc.setOnClickListener {
            deleteAccount()
        }

        btnLogout.setOnClickListener(){
           logoutUser()
        }
    }

    private fun fetchUserData() {
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        val usersRef = FirebaseDatabase.getInstance().getReference("Users")


        // First, check in Employees
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var userFound = false

                for (businessSnapshot in snapshot.children) {
                    val employeeSnapshot = businessSnapshot.child("Employees").child(currentUserId!!)

                    if (employeeSnapshot.exists()) {

                        businessId = businessSnapshot.key
                        userId = currentUserId
                        userFound = true
                        break
                    }
                }

                if (!userFound) {

                    checkPendingStatus() // Call checkPendingStatus if user not found in Employees
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun checkPendingStatus() {
        val usersRef = FirebaseDatabase.getInstance().getReference("Users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var userFound = false

                for (businessSnapshot in snapshot.children) {
                    val pendingSnapshot = businessSnapshot.child("Pending").child(currentUserId!!)

                    if (pendingSnapshot.exists()) {

                        businessId = businessSnapshot.key
                        userId = currentUserId
                        userFound = true
                        break
                    }
                }

                if (!userFound) {
                    Toast.makeText(this@DeniedActivity, "User not found in Employees or Pending.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    fun logoutUser() {
        // Get the Firebase Auth instance
        val auth = FirebaseAuth.getInstance()


        auth.signOut()
        startActivity(Intent(this@DeniedActivity, Login::class.java))
        finish()
    }


    private fun deleteAccount() {
        if (businessId == null || userId == null) {
            Toast.makeText(this, "Business ID or User ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        // Get current user
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        // Reference to user's data in Realtime Database
        val dbRef = FirebaseDatabase.getInstance().getReference("Users")
            .child(businessId!!)
            .child("Pending")
            .child(userId!!)

        // Delete data from Realtime Database first
        dbRef.removeValue().addOnCompleteListener { dbTask ->
            if (dbTask.isSuccessful) {


                // Then delete user from Firebase Authentication
                user.delete().addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        Toast.makeText(this, "Account deleted successfully from Authentication and Database.", Toast.LENGTH_SHORT).show()
                        finish() // Close the activity
                    } else {
                        Toast.makeText(this, "Failed to delete account from Authentication.", Toast.LENGTH_SHORT).show()

                    }
                }
            } else {
                Toast.makeText(this, "Failed to delete data from Realtime Database.", Toast.LENGTH_SHORT).show()

            }
        }
    }


    private fun updateStatus() {
        if (businessId == null || userId == null) {
            Toast.makeText(this, "Business ID or User ID is missing.", Toast.LENGTH_SHORT).show()
            return
        }

        // Reference to the request counter
        val dbRef = FirebaseDatabase.getInstance().getReference("Users")
            .child(businessId!!).child("Pending").child(userId!!)

        dbRef.child("requestCount").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get the current count of requests, defaulting to 0 if it doesn't exist
                val currentCount = snapshot.getValue(Int::class.java) ?: 0

                if (currentCount < 3) {
                    // Increment the counter and update the approval status
                    dbRef.child("requestCount").setValue(currentCount + 1).addOnSuccessListener {
                        dbRef.child("approval").setValue("pending").addOnSuccessListener {
                            Toast.makeText(this@DeniedActivity, "Access request sent again.", Toast.LENGTH_SHORT).show()
                            finish() // Close the activity
                        }.addOnFailureListener {
                            Toast.makeText(this@DeniedActivity, "Failed to request access.", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this@DeniedActivity, "Failed to update request count.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Inform the user that they have reached the limit
                    Toast.makeText(this@DeniedActivity, "You have reached the maximum number of requests.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DeniedActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


//    private fun updateStatus() {
//        if (businessId == null || userId == null) {
//            Toast.makeText(this, "Business ID or User ID is missing.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(businessId!!).child("Pending").child(userId!!)
//        dbRef.child("approval").setValue("pending").addOnSuccessListener {
//            Toast.makeText(this, "Access request sent again.", Toast.LENGTH_SHORT).show()
//            finish() // Close the activity
//        }.addOnFailureListener {
//            Toast.makeText(this, "Failed to request access.", Toast.LENGTH_SHORT).show()
//        }
//    }
}
