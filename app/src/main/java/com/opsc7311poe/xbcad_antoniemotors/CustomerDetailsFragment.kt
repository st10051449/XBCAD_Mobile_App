package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.widget.ArrayAdapter
import android.widget.LinearLayout

class CustomerDetailsFragment : Fragment() {
    private lateinit var btnBack: ImageView
    private lateinit var spnCustType: Spinner
    private lateinit var btnEditCust: Button
    private lateinit var btnDeleteCust: Button
    private lateinit var btnSaveCust: Button
    private lateinit var txtCustomerName: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtAddress: EditText
    private lateinit var txtCellNumber: EditText
    private lateinit var txtCustType: TextView
    private lateinit var rviewCustomerVehicles: RecyclerView
    private lateinit var vehicleAdapter: VehicleAdapter
    private lateinit var vehicleList: ArrayList<VehicleData>
    private var businessId: String? = null  // Store business ID for reuse
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private var isEditable = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_customer_details, container, false)

        // Initialize UI elements
        btnBack = view.findViewById(R.id.ivBackButton)
        txtCustomerName = view.findViewById(R.id.txtCustomerName)
        txtEmail = view.findViewById(R.id.txtEmail)
        txtAddress = view.findViewById(R.id.txtAddress)
        txtCellNumber = view.findViewById(R.id.txtCellNumber)
        txtCustType = view.findViewById(R.id.txtCustomerType)
        rviewCustomerVehicles = view.findViewById(R.id.rvCustomerVehicles)
        btnEditCust = view.findViewById(R.id.btnEditCustomer)
        btnDeleteCust = view.findViewById(R.id.btnDeleteCustomer)
        btnSaveCust = view.findViewById(R.id.btnSaveChanges)
        spnCustType = view.findViewById(R.id.spnCustomerType)
        auth = FirebaseAuth.getInstance()

        // Retrieve customer details from arguments
        val customerID = arguments?.getString("customerID") ?: ""
        val customerName = arguments?.getString("customerName") ?: ""
        val customerSurname = arguments?.getString("customerSurname") ?: ""
        val customerEmail = arguments?.getString("customerEmail") ?: ""
        val customerAddress = arguments?.getString("customerAddress") ?: ""
        val customerMobile = arguments?.getString("customerMobile") ?: ""
        val customerType = arguments?.getString("customerType") ?: ""



        // Set customer information in TextViews
        txtCustomerName.text = "$customerName $customerSurname"
        txtEmail.text = customerEmail
        txtAddress.setText(customerAddress)
        txtCellNumber.setText(customerMobile)
        //txtCustType.setText(customerType)
        txtCustType.text = customerType


        val customerTypes = arrayOf("business", "private")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, customerTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnCustType.adapter = adapter

        // Back button click listener
        btnBack.setOnClickListener {
            replaceFragment(CustomerFragment())
        }

        vehicleList = ArrayList()
        vehicleAdapter = VehicleAdapter(vehicleList) { selectedVehicle ->
            businessId?.let { openVehicleDetailsFragment(selectedVehicle, it) }
        }

        checkUserRole()

        rviewCustomerVehicles.layoutManager = LinearLayoutManager(requireContext())
        rviewCustomerVehicles.adapter = vehicleAdapter

        btnEditCust.setOnClickListener {
            enableEditingMode(true)
        }

        btnSaveCust.setOnClickListener {
            if (isValidInput()) {
                showSaveConfirmationDialog()
            }
        }

        btnDeleteCust.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Customer")
                .setMessage("Are you sure you want to delete this customer?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteCustomer()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }


        // Fetch vehicles belonging to the selected customer
        fetchCustomerVehicles(customerID)

        return view
    }

    private fun fetchCustomerVehicles(customerID: String) {
        val adminId = FirebaseAuth.getInstance().currentUser?.uid

        if (adminId != null) {
            val usersReference = FirebaseDatabase.getInstance().getReference("Users")

            usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(usersSnapshot: DataSnapshot) {
                    // Find the business ID associated with this admin
                    for (businessSnapshot in usersSnapshot.children) {
                        val employeeSnapshot = businessSnapshot.child("Employees").child(adminId)
                        if (employeeSnapshot.exists()) {
                            businessId = businessSnapshot.key  // Save businessId for use in other functions
                            break
                        }
                    }

                    if (businessId != null) {
                        // Reference to the Vehicles under the business
                        val vehicleReference = usersReference.child(businessId!!).child("Vehicles")

                        vehicleReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val customerVehicles = ArrayList<VehicleData>()

                                for (vehicleSnapshot in snapshot.children) {
                                    val vehicle = vehicleSnapshot.getValue(VehicleData::class.java)
                                    vehicle?.let {
                                        // Match the vehicle's customerID with the selected customer's ID
                                        if (it.customerID == customerID) {
                                            it.vehicleId = vehicleSnapshot.key ?: ""
                                            customerVehicles.add(it)
                                        }
                                    }
                                }

                                if (customerVehicles.isEmpty()) {
                                    Toast.makeText(requireContext(), "No vehicles found for this customer.", Toast.LENGTH_SHORT).show()
                                } else {
                                    vehicleAdapter.updateList(customerVehicles)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(requireContext(), "Error fetching vehicles.", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(requireContext(), "Unable to find associated business.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error fetching business information.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun openVehicleDetailsFragment(vehicle: VehicleData, businessId: String) {
        if (vehicle.vehicleId.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid Vehicle ID.", Toast.LENGTH_SHORT).show()
            return
        }

        val fragment = VehicleDetailsFragment().apply {
            arguments = Bundle().apply {
                putString("vehicleId", vehicle.vehicleId)
                putString("businessId", businessId)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun enableEditingMode(enable: Boolean) {
        isEditable = enable
        txtAddress.isEnabled = enable
        txtCellNumber.isEnabled = enable
        spnCustType.visibility = if (enable) View.VISIBLE else View.INVISIBLE
        txtCustType.isEnabled = enable  // Hide the non-editable customer type
        btnSaveCust.visibility = if (enable) View.VISIBLE else View.INVISIBLE
    }




    private fun isValidInput(): Boolean {
        val addressPattern = Regex("^[a-zA-Z0-9,' ]+\$")
        val phonePattern = Regex("^[0-9]+\$")

        val isAddressValid = txtAddress.text.toString().matches(addressPattern)
        val isPhoneValid = txtCellNumber.text.toString().matches(phonePattern)

        if (!isAddressValid) {
            Toast.makeText(context, "Invalid address format.", Toast.LENGTH_SHORT).show()
        }
        if (!isPhoneValid) {
            Toast.makeText(context, "Invalid phone number format.", Toast.LENGTH_SHORT).show()
        }

        return isAddressValid && isPhoneValid
    }

    private fun showSaveConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Changes")
            .setMessage("Are you sure you want to save these changes?")
            .setPositiveButton("Save") { _, _ ->
                saveCustomerChanges()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveCustomerChanges() {
        val customerID = arguments?.getString("customerID") ?: return
        val updatedAddress = txtAddress.text.toString()
        val updatedCellNumber = txtCellNumber.text.toString()
        val updatedCustomerType = spnCustType.selectedItem.toString()

        // Reference to the customer in Firebase
        val customerRef = FirebaseDatabase.getInstance().getReference("Users/$businessId/Customers/$customerID")

        customerRef.child("CustomerAddress").setValue(updatedAddress)
        customerRef.child("CustomerMobileNum").setValue(updatedCellNumber)
        customerRef.child("CustomerType").setValue(updatedCustomerType).addOnSuccessListener {
            Toast.makeText(requireContext(), "Customer details updated successfully.", Toast.LENGTH_SHORT).show()
            enableEditingMode(false)
            btnSaveCust.visibility = View.GONE
            btnEditCust.visibility = View.VISIBLE

        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to update vehicle.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun deleteCustomer() {
        val customerID = arguments?.getString("customerID") ?: return

        // Reference to the customer in Firebase
        val customerRef = FirebaseDatabase.getInstance().getReference("Users/$businessId/Customers/$customerID")

        customerRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Customer deleted successfully.", Toast.LENGTH_SHORT).show()
                // Navigate back to the previous fragment
                replaceFragment(CustomerFragment())
            } else {
                Toast.makeText(context, "Failed to delete customer.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun checkUserRole() {
        val userId = auth.currentUser?.uid ?: return

        fetchBusinessId(userId) { retrievedBusinessId ->
            businessId = retrievedBusinessId

            if (businessId == null) {
                Toast.makeText(requireContext(), "Business ID is null", Toast.LENGTH_SHORT).show()
                return@fetchBusinessId
            }

            val userRef = FirebaseDatabase.getInstance().getReference("Users/$businessId/Employees/$userId")

            userRef.child("role").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val role = snapshot.getValue(String::class.java)
                    if (role == "owner") {
                        btnDeleteCust.visibility = View.VISIBLE
                    } else {
                        btnDeleteCust.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error fetching user role: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }


    private fun fetchBusinessId(userId: String, onResult: (String?) -> Unit) {
        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (businessSnapshot in snapshot.children) {
                    val employeeSnapshot = businessSnapshot.child("Employees").child(userId)
                    if (employeeSnapshot.exists()) {
                        val foundBusinessId = businessSnapshot.key ?: return
                        onResult(foundBusinessId)  // Pass back the businessId
                        return  // Exit loop once business ID is found
                    }
                }
                onResult(null)  // If not found, return null
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching business ID: ${error.message}", Toast.LENGTH_SHORT).show()
                onResult(null)
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
