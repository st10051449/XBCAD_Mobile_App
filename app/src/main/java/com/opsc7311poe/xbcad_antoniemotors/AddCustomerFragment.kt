package com.opsc7311poe.xbcad_antoniemotors

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.widget.LinearLayout
import android.widget.ImageView
import java.util.Date
import java.util.Locale



class AddCustomerFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var nameField: EditText
    private lateinit var surnameField: EditText
    private lateinit var mobileNumField: EditText
    private lateinit var emailField: EditText
    private lateinit var addressField: EditText
    private lateinit var submitButton: Button
    private lateinit var btnBack: ImageView
    private lateinit var spinnerCustType: Spinner
    private lateinit var edtCompany: EditText
    private lateinit var nameLayout: LinearLayout
    private lateinit var surnameLayout: LinearLayout
    private lateinit var companyLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_customer, container, false)
        // Initialize Firebase Database and Views
        database = FirebaseDatabase.getInstance().reference
        nameField = view.findViewById(R.id.edttxtRegName)
        surnameField = view.findViewById(R.id.edttxtRegSurname)
        mobileNumField = view.findViewById(R.id.edttxtRegMobNumber)
        emailField = view.findViewById(R.id.edttxtEmail)
        addressField = view.findViewById(R.id.edttxtAddress)
        submitButton = view.findViewById(R.id.btnSubmitRegCustomer)
        btnBack = view.findViewById(R.id.ivBackButton)
        spinnerCustType = view.findViewById(R.id.spnCustomerType)
        edtCompany = view.findViewById(R.id.edttxtCompanyName)
        nameLayout = view.findViewById(R.id.nameLayout)
        surnameLayout = view.findViewById(R.id.surnameLayout)
        companyLayout = view.findViewById(R.id.companyLayout)

        // Set onClickListener for the submit button
        submitButton.setOnClickListener {
            addCustomer()
        }

        // Handle back button click
        btnBack.setOnClickListener() {
            replaceFragment(HomeFragment())
        }

        // Listen for changes in the spinner selection to toggle name/company fields
        spinnerCustType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedType = parent.getItemAtPosition(position).toString()
                if (selectedType == "Business") {
                    // Show company name field and hide name and surname fields
                    nameLayout.visibility = View.GONE
                    surnameLayout.visibility = View.GONE
                    companyLayout.visibility = View.VISIBLE
                } else {
                    // Show name and surname fields, hide company name field
                    nameLayout.visibility = View.VISIBLE
                    surnameLayout.visibility = View.VISIBLE
                    companyLayout.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed here
            }
        }

        return view
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun addCustomer() {
        val customerType = spinnerCustType.selectedItem.toString()

        // If customer type is Private, get name and surname
        if (customerType == "Private") {
            val name = nameField.text.toString().trim()
            val surname = surnameField.text.toString().trim()
            val mobileNum = mobileNumField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val address = addressField.text.toString().trim()

            if (validatePrivateCustomer(name, surname, mobileNum, email, address)) {
                saveCustomerToDatabase(name, surname, mobileNum, email, address, customerType)
            }
        }

        // If customer type is Business, get company name
        else if (customerType == "Business") {
            val companyName = edtCompany.text.toString().trim()
            val mobileNum = mobileNumField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val address = addressField.text.toString().trim()

            if (validateBusinessCustomer(companyName, mobileNum, email, address)) {
                saveCustomerToDatabase(companyName, "", mobileNum, email, address, customerType)
            }
        }
    }

    private fun validatePrivateCustomer(
        name: String, surname: String, mobileNum: String, email: String, address: String
    ): Boolean {
        val namePattern = Regex("^[a-zA-Z]+\$")
        if (!name.matches(namePattern)) {
            nameField.error = "Name cannot contain numbers or special characters"
            return false
        }
        if (!surname.matches(namePattern)) {
            surnameField.error = "Surname cannot contain numbers or special characters"
            return false
        }
        if (mobileNum.length != 10 || !mobileNum.all { it.isDigit() }) {
            mobileNumField.error = "Mobile number must be 10 digits and contain only numbers"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.error = "Invalid email format"
            return false
        }
        if (TextUtils.isEmpty(address)) {
            addressField.error = "Please enter a physical address"
            return false
        }
        return true
    }


    private fun validateBusinessCustomer(
        companyName: String, mobileNum: String, email: String, address: String
    ): Boolean {
        if (TextUtils.isEmpty(companyName)) {
            edtCompany.error = "Please enter a company name"
            return false
        }
        if (mobileNum.length != 10 || !mobileNum.all { it.isDigit() }) {
            mobileNumField.error = "Mobile number must be 10 digits and contain only numbers"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.error = "Invalid email format"
            return false
        }
        if (TextUtils.isEmpty(address)) {
            addressField.error = "Please enter a physical address"
            return false
        }
        return true
    }

    private fun saveCustomerToDatabase(
        name: String, surname: String, mobileNum: String, email: String, address: String, customerType: String
    ) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserUid != null) {
            val userDatabaseRef = database.child("Users").child(currentUserUid).child("Customers")
            val customerId = userDatabaseRef.push().key
            // Get the current date and time
            val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            val customer = customerId?.let {
                CustomerData(
                    CustomerID = it,
                    CustomerName = name, // For Business, this will be the company name
                    CustomerSurname = surname, // Empty for Business customers
                    CustomerMobileNum = mobileNum,
                    CustomerEmail = email,
                    CustomerAddress = address,
                    CustomerType = customerType,
                    CustomerAddedDate = currentDate
                )
            }

            if (customerId != null) {
                userDatabaseRef.child(customerId).setValue(customer).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Customer added successfully", Toast.LENGTH_SHORT).show()

                        // Clear input fields after successful submission
                        nameField.text.clear()
                        surnameField.text.clear()
                        mobileNumField.text.clear()
                        emailField.text.clear()
                        addressField.text.clear()
                        edtCompany.text.clear()
                    } else {
                        Toast.makeText(context, "Failed to add customer. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}