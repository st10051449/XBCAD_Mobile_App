package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class AddPartFragment : Fragment() {

    private lateinit var partNameEditText: EditText
    private lateinit var partDescriptionEditText: EditText
    private lateinit var stockCountPicker: NumberPicker
    private lateinit var minStockPicker: NumberPicker
    private lateinit var costPriceEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var database: DatabaseReference
    private val auth = FirebaseAuth.getInstance()
    private lateinit var btnBack: ImageView
    private lateinit var businessId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_part, container, false)

        partNameEditText = view.findViewById(R.id.edtPartName)
        partDescriptionEditText = view.findViewById(R.id.edtPartDescription)
        stockCountPicker = view.findViewById(R.id.npStockCounter)
        minStockPicker = view.findViewById(R.id.npMinStockCounter)
        costPriceEditText = view.findViewById(R.id.edtCostPrice)
        saveButton = view.findViewById(R.id.btnAddPart)
        btnBack = view.findViewById(R.id.ivBackButton)

        stockCountPicker.minValue = 0
        stockCountPicker.maxValue = 100
        minStockPicker.minValue = 0
        minStockPicker.maxValue = 99

        database = Firebase.database.reference

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("business_id", null)!!

        saveButton.setOnClickListener {
            savePart()
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun savePart() {
        val partName = partNameEditText.text.toString().trim()
        val partDescription = partDescriptionEditText.text.toString().trim()
        val stockCount = stockCountPicker.value
        val minStock = minStockPicker.value
        val costPriceText = costPriceEditText.text.toString().trim()

        // Validate part name
        if (partName.isEmpty()) {
            Toast.makeText(context, "Part name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate cost price
        val costPrice = costPriceText.toDoubleOrNull()
        if (costPrice == null || costPrice <= 0) {
            Toast.makeText(context, "Please enter a valid cost price", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate stock counts
        if (stockCount < minStock) {
            Toast.makeText(context, "Minimum stock cannot be greater than stock count", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if user is authenticated
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid

        // Create a map for part data to save in the database
        val partData = mapOf(
            "partName" to partName,
            "partDescription" to partDescription,
            "stockCount" to stockCount,
            "minStock" to minStock,
            "costPrice" to costPrice
        )

        // Save part data to Firebase
        database.child("Users/$businessId").child("parts").push()
            .setValue(partData)
            .addOnSuccessListener {
                Toast.makeText(context, "Part added successfully", Toast.LENGTH_SHORT).show()
                clearFields()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to add part: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearFields() {
        partNameEditText.text.clear()
        partDescriptionEditText.text.clear()
        costPriceEditText.text.clear()
        stockCountPicker.value = 0
        minStockPicker.value = 0
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
