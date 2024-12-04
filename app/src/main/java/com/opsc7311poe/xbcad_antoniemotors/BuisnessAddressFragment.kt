package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.concurrent.Executor
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt


class BusinessAddressFragment : Fragment() {

    private lateinit var edtBusinessName: EditText
    private lateinit var edtStreet: EditText
    private lateinit var edtSuburb: EditText
    private lateinit var edtCity: EditText
    private lateinit var edtPost: EditText
    private lateinit var btnAddAddress: Button
    private lateinit var database: DatabaseReference
    private val auth = FirebaseAuth.getInstance()
    private lateinit var businessId: String
    private lateinit var btnBack: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_buisness_address, container, false)

        // Initialize views
        edtBusinessName = view.findViewById(R.id.edtBusinessName)
        edtStreet = view.findViewById(R.id.edtStreet)
        edtSuburb = view.findViewById(R.id.edtSuburb)
        edtCity = view.findViewById(R.id.edtCity)
        edtPost = view.findViewById(R.id.edtPost)
        btnAddAddress = view.findViewById(R.id.btnAddAddress)
        btnBack = view.findViewById(R.id.ivBackButton)

        database = Firebase.database.reference

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("business_id", null)!!

        // Set click listener for the Save button
        btnAddAddress.setOnClickListener {
            saveBusinessAddress()
        }
        btnBack.setOnClickListener {
            replaceFragment(DocumentationFragment())
        }

        return view
    }

    private fun saveBusinessAddress() {
        // Get data from EditText fields
        val businessName = edtBusinessName.text.toString().trim()
        val streetName = edtStreet.text.toString().trim()
        val suburb = edtSuburb.text.toString().trim()
        val city = edtCity.text.toString().trim()
        val postCodeText = edtPost.text.toString().trim()

        // Validate fields
        if (businessName.isEmpty()) {
            Toast.makeText(context, "Business Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (streetName.isEmpty()) {
            Toast.makeText(context, "Street Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (suburb.isEmpty()) {
            Toast.makeText(context, "Suburb cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (city.isEmpty()) {
            Toast.makeText(context, "City cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (postCodeText.isEmpty()) {
            Toast.makeText(context, "Post Code cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate Post Code to be numeric (if necessary)
        if (postCodeText.isEmpty()) {
            Toast.makeText(context, "Post Code cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate Post Code to be exactly 4 digits
        if (postCodeText.length != 4 || !postCodeText.all { it.isDigit() }) {
            Toast.makeText(context, "Please enter a valid 4-digit Post Code", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert Post Code to integer
        val postCode = postCodeText.toInt()

        // Check if user is authenticated
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid

        // Create a map for address data to save in Firebase
        val addressData = mapOf(
            "businessName" to businessName,
            "streetName" to streetName,
            "suburb" to suburb,
            "city" to city,
            "postCode" to postCode
        )

        // Save address data to Firebase under the user's business ID
        database.child("Users/$businessId").child("BusinessAddress").push().setValue(addressData)
            .addOnSuccessListener {
                Toast.makeText(context, "Business Address saved successfully", Toast.LENGTH_SHORT)
                    .show()
                clearFields() // Optionally clear the fields after successful saving
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save address: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    // Function to clear the EditText fields after saving
    private fun clearFields() {
        edtBusinessName.text.clear()
        edtStreet.text.clear()
        edtSuburb.text.clear()
        edtCity.text.clear()
        edtPost.text.clear()
    }
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
