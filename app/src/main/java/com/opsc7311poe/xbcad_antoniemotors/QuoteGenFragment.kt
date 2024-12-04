package com.opsc7311poe.xbcad_antoniemotors

import QuoteData
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import java.util.*

class QuoteGenFragment : Fragment() {

    private lateinit var spinCustomer: Spinner
    private lateinit var spinCompanyName: Spinner
    private lateinit var edtLabour: EditText
    private lateinit var txtRvalue: TextView
    private lateinit var btnFinalReview: Button
    private lateinit var btnBack: ImageView
    private lateinit var txtPartsList: TextView
    private lateinit var btnAddNewPart: ImageView
    private lateinit var btnAddPart: Button
    private lateinit var SpinPartName: Spinner  // Changed from EditText
    private lateinit var edtPartCost: EditText
    private lateinit var npStockCounter: NumberPicker  // New NumberPicker

    private lateinit var partsList: MutableList<Map<String, String>>  // List to store parts and their costs
    private var totalPartsCost: Double = 0.0
    private var totalLabourCost: Double = 0.0

    private var selectedCustomerName: String = ""
    private var selectedCompanyName: String = ""
    private var selectedServiceTypeName: String = ""

    private lateinit var auth: FirebaseAuth
    private lateinit var databases: DatabaseReference
    private lateinit var businessId: String

    // Firebase database reference
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_quote_gen, container, false)

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("business_id", null)!!

        // Initialize views
        spinCompanyName = view.findViewById(R.id.spinCompanyName)
        spinCustomer = view.findViewById(R.id.spinCustomer)
        SpinPartName = view.findViewById(R.id.spinPartName)
        edtPartCost = view.findViewById(R.id.edtPartCost)
        txtPartsList = view.findViewById(R.id.txtPartsList)
        btnAddPart = view.findViewById(R.id.btnAddPart)
        edtLabour = view.findViewById(R.id.edttxtLabour)
        txtRvalue = view.findViewById(R.id.txtRvalue)
        btnBack = view.findViewById(R.id.ivBackButton)
        btnFinalReview = view.findViewById(R.id.btnFinalReview)
        npStockCounter = view.findViewById(R.id.npStockCounter)
        btnAddNewPart = view.findViewById(R.id.btnPlus)

        partsList = mutableListOf()

        npStockCounter.minValue = 1
        npStockCounter.maxValue = 15
        npStockCounter.wrapSelectorWheel = true

        SpinPartName.setSelection(0)
        edtPartCost.text.clear()
        npStockCounter.value = 1
        txtPartsList.text = ""
        edtLabour.text.clear()
        txtRvalue.text = "R0.00"
        spinCustomer.setSelection(0)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            populateCustomerSpinner()
            populatePartSpinner()
            populateCompanySpinner()
        }

        // Handle back button press
        btnBack.setOnClickListener {
            replaceFragment(DocumentationFragment())
        }

        //directs the user to the add parts/ inventory page
        btnAddNewPart.setOnClickListener {
            replaceFragment(AddPartFragment())
        }

        // Set up add part button
        btnAddPart.setOnClickListener {
            if (validateAddPartInput()) addPart()
        }


        // Add TextWatcher to update total when labor changes
        edtLabour.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                totalLabourCost = s.toString().toDoubleOrNull() ?: 0.0
                updateTotalQuote()
            }
        })

        btnFinalReview.setOnClickListener {
            // Check if labor cost is entered
            val laborCost = edtLabour.text.toString().trim()

            if (laborCost.isEmpty()) {
                // If the labor cost is empty, show a toast and do not proceed
                Toast.makeText(
                    requireContext(),
                    "Please enter the labor price to proceed.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // If the labor cost is valid, generate the quote and proceed
                val quoteId = generateReceipt()
                val bundle = Bundle()
                bundle.putString("quoteId", quoteId)
                val quoteOverviewFragment = QuoteOverviewFragment()
                quoteOverviewFragment.arguments = bundle
                replaceFragment(quoteOverviewFragment)
            }
        }
        return view
    }

    private fun validateAddPartInput(): Boolean {
        val selectedPartName = SpinPartName.selectedItem?.toString()
        val partCost = edtPartCost.text.toString().trim()

        if (selectedPartName.isNullOrEmpty() || partCost.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all part details.", Toast.LENGTH_SHORT)
                .show()
            return false
        }
        return true
    }

    private fun populateCompanySpinner() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val businessNames = mutableListOf<String>()
        val partsReference = database.reference.child("Users/$businessId").child("BusinessAddress")

        partsReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (partSnapshot in snapshot.children) {
                    val businessName = partSnapshot.child("businessName").getValue(String::class.java)
                    businessName?.let { businessNames.add(it) }
                }

                if (businessNames.isEmpty()) {
                    // Display a message if no business addresses are found
                    Toast.makeText(
                        context,
                        "You need to add a business address first.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        businessNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinCompanyName.adapter = adapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    context,
                    "Failed to load business names: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun populateCustomerSpinner() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val customerRef = FirebaseDatabase.getInstance().getReference("Users").child(businessId)
            .child("Customers")

        customerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customerMap = mutableMapOf<String, String>()
                val customerNames = mutableListOf<String>()

                if (!snapshot.exists()) {
                    Toast.makeText(
                        requireContext(),
                        "No customers found for this user",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                for (customerSnapshot in snapshot.children) {
                    val customerId = customerSnapshot.key
                    val firstName =
                        customerSnapshot.child("CustomerName").getValue(String::class.java)
                    val lastName =
                        customerSnapshot.child("CustomerSurname").getValue(String::class.java)

                    if (!firstName.isNullOrEmpty() && !lastName.isNullOrEmpty() && customerId != null) {
                        val fullName = "$firstName $lastName"
                        customerMap[customerId] = fullName
                        customerNames.add(fullName)
                    }
                }

                if (customerNames.isEmpty()) {
                    Toast.makeText(requireContext(), "No customers found", Toast.LENGTH_SHORT)
                        .show()
                    return
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    customerNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinCustomer.adapter = adapter

                spinCustomer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedCustomerName = parent.getItemAtPosition(position).toString()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load customers", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun populatePartSpinner() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val partNames = mutableListOf<String>()
        val partsReference = database.reference.child("Users/$businessId").child("parts")

        partsReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (partSnapshot in snapshot.children) {
                    val partName = partSnapshot.child("partName").getValue(String::class.java)
                    partName?.let { partNames.add(it) }
                }
                val adapter =
                    ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, partNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                SpinPartName.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    context,
                    "Failed to load parts: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // Method to clear fields after adding a part
    private fun clearFields() {
        SpinPartName.setSelection(0) // Reset the spinner to the first item
        edtPartCost.text.clear()     // Clear the cost input
        npStockCounter.value = 1     // Reset the quantity picker to its minimum value (e.g., 1)
    }

    private fun addPart() {
        val selectedPartName = SpinPartName.selectedItem?.toString()
        val partCost = edtPartCost.text.toString().trim()
        val selectedQuantity = npStockCounter.value

        if (selectedPartName != null && partCost.isNotEmpty()) {
            val partData = mapOf(
                "name" to selectedPartName,
                "cost" to partCost,
                "quantity" to selectedQuantity.toString()
            )
            partsList.add(partData)

            totalPartsCost += partCost.toDouble() * selectedQuantity
            txtPartsList.text = formatPartsList()
            updateTotalQuote()

            clearFields() // Reset fields after adding the part
        } else {
            Toast.makeText(requireContext(), "Please provide part details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatPartsList(): String {
        var partsListText = ""
        for (part in partsList) {
            val name = part["name"]
            val cost = part["cost"]
            val quantity = part["quantity"]
            partsListText += "$name x$quantity - R$cost\n"
        }
        return partsListText
    }

    private fun updateTotalQuote() {
        val totalQuote = totalLabourCost + totalPartsCost
        txtRvalue.text = "R$totalQuote"
    }

    private fun generateReceipt(): String {
        val userID = FirebaseAuth.getInstance().currentUser?.uid
        if (userID == null) {
            Toast.makeText(
                requireContext(),
                "User not authenticated. Cannot save quote.",
                Toast.LENGTH_SHORT
            ).show()
            return ""
        }
        val quoteId = UUID.randomUUID().toString()
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val selectedCompanyName = spinCompanyName.selectedItem?.toString() ?: ""
        if (selectedCompanyName.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a company name.", Toast.LENGTH_SHORT)
                .show()
            return ""
        }

        val businessRef = database.reference.child("Users/$businessId/BusinessAddress")

        businessRef.orderByChild("businessName").equalTo(selectedCompanyName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (businessSnapshot in snapshot.children) {
                        val streetName =
                            businessSnapshot.child("streetName").getValue(String::class.java)
                        val city = businessSnapshot.child("city").getValue(String::class.java)
                        val postCode = businessSnapshot.child("postCode").getValue(Int::class.java)
                        val suburb = businessSnapshot.child("suburb").getValue(String::class.java)

                        val quoteData = QuoteData(
                            id = quoteId,
                            companyName = selectedCompanyName,
                            customerName = selectedCustomerName,
                            parts = partsList,
                            labourCost = totalLabourCost.toString(),
                            totalCost = (totalPartsCost + totalLabourCost).toString(),
                            dateCreated = currentDate,
                            streetName = streetName,
                            city = city,
                            postCode = postCode,
                            suburb = suburb
                        )

                        val quoteRef =
                            database.reference.child("Users/$businessId/Quotes").child(quoteId)
                        quoteRef.setValue(quoteData)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Quote saved!", Toast.LENGTH_SHORT)
                                    .show()

                                val bundle = Bundle().apply {
                                    putString("quoteId", quoteId)
                                }
                                val quoteOverviewFragment = QuoteOverviewFragment().apply {
                                    arguments = bundle
                                }
                                replaceFragment(quoteOverviewFragment)
                            }
                            .addOnFailureListener { error ->
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to save quote data: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch business address details: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        return quoteId
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
