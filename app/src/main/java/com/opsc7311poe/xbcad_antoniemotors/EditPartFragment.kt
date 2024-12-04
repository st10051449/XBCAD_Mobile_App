package com.opsc7311poe.xbcad_antoniemotors

import android.app.AlertDialog
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

class EditPartFragment : Fragment() {

    private lateinit var partNameEditText: EditText
    private lateinit var partDescriptionEditText: EditText
    private lateinit var stockCountPicker: NumberPicker
    private lateinit var minStockPicker: NumberPicker
    private lateinit var costPriceEditText: EditText
    private lateinit var btnBack: ImageView
    private lateinit var saveChangesButton: Button
    private lateinit var deleteButton: Button
    private lateinit var database: DatabaseReference
    private val auth = FirebaseAuth.getInstance()
    private lateinit var businessId: String

    private var partId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_part, container, false)

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("business_id", null)!!

        partNameEditText = view.findViewById(R.id.edtPartName)
        partDescriptionEditText = view.findViewById(R.id.edtPartDescription)
        stockCountPicker = view.findViewById(R.id.npStockCounter)
        minStockPicker = view.findViewById(R.id.npMinStockCounter)
        costPriceEditText = view.findViewById(R.id.edtCostPrice)
        saveChangesButton = view.findViewById(R.id.btnEditPart)
        deleteButton = view.findViewById(R.id.btnDeletePart)
        btnBack = view.findViewById(R.id.ivBackButton)

        saveChangesButton.text = "Save"

        stockCountPicker.minValue = 0
        stockCountPicker.maxValue = 100
        minStockPicker.minValue = 0
        minStockPicker.maxValue = 99

        database = Firebase.database.reference

        partId = arguments?.getString("partId")
        if (partId == null) {
            Toast.makeText(context, "Error: Part ID is missing", Toast.LENGTH_SHORT).show()
        } else {
            // Log the part ID for debugging
            println("Received partId in EditPartFragment: $partId")
        }

        populateFields()

        saveChangesButton.setOnClickListener {
            updatePart()
        }

        deleteButton.setOnClickListener {
            confirmDelete()
        }
        btnBack.setOnClickListener {
            replaceFragment(ViewInventoryFragment())
        }


        return view
    }

    private fun populateFields() {
        arguments?.let { bundle ->
            partNameEditText.setText(bundle.getString("partName", ""))
            partDescriptionEditText.setText(bundle.getString("partDescription", ""))
            stockCountPicker.value = bundle.getInt("stockCount", 0)
            minStockPicker.value = bundle.getInt("minStock", 0)
            costPriceEditText.setText(bundle.getDouble("costPrice", 0.0).toString())
        }
    }

    private fun updatePart() {
        val partName = partNameEditText.text.toString().trim()
        val partDescription = partDescriptionEditText.text.toString().trim()
        val stockCount = stockCountPicker.value
        val minStock = minStockPicker.value
        val costPrice = costPriceEditText.text.toString().toDoubleOrNull() ?: 0.0

        if (partName.isEmpty()) {
            Toast.makeText(context, "Please enter a valid part name", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "User is not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        if (partId == null) {
            Toast.makeText(context, "Part ID is missing", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid

        val partData = mapOf(
            "partName" to partName,
            "partDescription" to partDescription,
            "stockCount" to stockCount,
            "minStock" to minStock,
            "costPrice" to costPrice
        )

        database.child("Users").child(businessId).child("parts").child(partId!!)
            .updateChildren(partData)
            .addOnSuccessListener {
                Toast.makeText(context, "Part updated successfully", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update part: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Part")
            .setMessage("Are you sure you want to delete this part?")
            .setPositiveButton("Delete") { _, _ ->
                deletePart()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePart() {
        val currentUser = auth.currentUser
        if (partId == null) {
            Toast.makeText(context, "Part ID is missing", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentUser == null) {
            Toast.makeText(context, "User is not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid

        database.child("Users").child(businessId).child("parts").child(partId!!)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Part deleted successfully", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete part: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
