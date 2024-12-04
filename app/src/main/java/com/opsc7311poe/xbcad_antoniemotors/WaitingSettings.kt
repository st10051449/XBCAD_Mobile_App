package com.opsc7311poe.xbcad_antoniemotors

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class WaitingSettings : AppCompatActivity() {

    private lateinit var btnLogout: TextView
    private lateinit var btnDelAccount: TextView
    private lateinit var btnBack: ImageView

    private var businessId: String? = null
    private var userId: String? = null
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_waiting_settings)

        btnLogout = findViewById(R.id.txtLogout)
        btnDelAccount = findViewById(R.id.txtDeleteAccount)
        btnBack = findViewById(R.id.ivBackButton)

        btnLogout.setOnClickListener {
            showConfirmationDialog("Logout", "Are you sure you want to log out?", isDelete = false)
        }

        btnDelAccount.setOnClickListener {
            showConfirmationDialog("Delete Account", "Are you sure you want to delete your account?", isDelete = true)
        }

        btnBack.setOnClickListener {
            // Navigate back or handle as required
            finish()
        }
    }

    private fun showConfirmationDialog(title: String, message: String, isDelete: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)

        // Set up the dialog buttons
        builder.setPositiveButton("Yes") { dialog, _ ->
            if (isDelete) {
                handleDeleteAccount()  // Delete account logic
            } else {
                handleLogout()  // Logout logic
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        // Customize dialog appearance
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.green))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.red))
        }
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialog_background)
        dialog.show()
    }

    private fun handleLogout() {
        FirebaseAuth.getInstance().signOut()  // Sign out from Firebase

        // Redirect to login screen
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun handleDeleteAccount() {
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
                Log.d("WaitingSettings", "User data deleted from Realtime Database.")

                // Then delete user from Firebase Authentication
                user.delete().addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                        // Redirect to login or exit activity
                        val intent = Intent(this, Login::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Failed to delete account from Authentication.", Toast.LENGTH_SHORT).show()
                        Log.e("WaitingSettings", "Authentication deletion failed: ${authTask.exception?.message}")
                    }
                }
            } else {
                Toast.makeText(this, "Failed to delete data from Realtime Database.", Toast.LENGTH_SHORT).show()
                Log.e("WaitingSettings", "Database deletion failed: ${dbTask.exception?.message}")
            }
        }
    }
}
