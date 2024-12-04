package com.opsc7311poe.xbcad_antoniemotors

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class RegisterBusinessActivity : AppCompatActivity() {

    private lateinit var businessname: EditText
    private lateinit var ownername: EditText
    private lateinit var ownersurname: EditText
    private lateinit var btnRegisterBusiness: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_register_business)

        businessname = findViewById(R.id.txtRegBusinessName)
        ownername = findViewById(R.id.txtOwnerName)
        ownersurname = findViewById(R.id.txtOwnerSurname)
        btnRegisterBusiness = findViewById(R.id.btnRegisterBusiness)
        progressBar = findViewById(R.id.progressBar)

        btnRegisterBusiness.setOnClickListener {
            val businessName = businessname.text.toString().trim()
            val ownerName = ownername.text.toString().trim()
            val ownerSurname = ownersurname.text.toString().trim()

            if (businessName.isNotEmpty() && ownerName.isNotEmpty() && ownerSurname.isNotEmpty()) {
                checkBusinessName(businessName, ownerName, ownerSurname)


            } else {
                Toast.makeText(this, "Please fill in all fields!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Function to check if business name exists
    private fun checkBusinessName(businessName: String, ownerName: String, ownerSurname: String) {
        // Show progress bar
        progressBar.visibility = ProgressBar.VISIBLE
        btnRegisterBusiness.visibility = View.GONE

        // Get Firebase database reference
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("Users")

        // Query to check if the business name already exists
        usersRef.orderByChild("BusinessInfo/businessName").equalTo(businessName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Business name already exists
                        progressBar.visibility = ProgressBar.GONE
                        btnRegisterBusiness.visibility = View.VISIBLE
                        Toast.makeText(this@RegisterBusinessActivity, "Business name already exists. Please choose a different name.", Toast.LENGTH_SHORT).show()
                    } else {
                        // If no businesses exist, or business name is unique, proceed with registration
                        saveBusinessInfo(businessName, ownerName, ownerSurname)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Hide progress bar and show error message
                    progressBar.visibility = ProgressBar.GONE
                    btnRegisterBusiness.visibility = View.VISIBLE
                    Toast.makeText(this@RegisterBusinessActivity, "Error checking business name. Please try again.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Function to save business info
    private fun saveBusinessInfo(businessName: String, ownerName: String, ownerSurname: String) {
        // Get Firebase database reference
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("Users")

        // Generate a unique ID without dashes
        val businessId = UUID.randomUUID().toString().replace("-", "")

        // Create a BusinessInfo object with the provided details
        val businessInfo = BusinessInfo(
            businessName = businessName,
            ownerName = ownerName,
            ownerSurname = ownerSurname,
            ownerId = null // Initially set to null
        )

        // Set the data in the database under the generated key
        usersRef.child(businessId).child("BusinessInfo").setValue(businessInfo)
            .addOnCompleteListener { task ->
                // Hide progress bar
                progressBar.visibility = ProgressBar.GONE

                if (task.isSuccessful) {
                    // Data saved successfully
                    Toast.makeText(this, "Business registered successfully!", Toast.LENGTH_SHORT).show()

                    // Navigate to the next activity and pass the data
                    val intent = Intent(this, OwnerEnterInfoActivity::class.java)
                    intent.putExtra("businessName", businessName)
                    intent.putExtra("ownerName", ownerName)
                    intent.putExtra("ownerSurname", ownerSurname)
                    startActivity(intent)
                } else {
                    // Failed to save data
                    Toast.makeText(this, "Failed to register business!", Toast.LENGTH_SHORT).show()
                }
            }
    }


}
