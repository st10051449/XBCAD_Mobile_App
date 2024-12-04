package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class Register_Employee_Page : Fragment() {

    private lateinit var btnSubmit: Button
    private lateinit var txtName: TextView
    private lateinit var txtSurname: TextView
    private lateinit var txtSal: TextView
    private lateinit var txtTotalLeave: TextView
    private lateinit var txtLeaveLeft: TextView
    private lateinit var txtNum: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtPasswordInput: TextView
    private lateinit var txtAddress: TextView
    private lateinit var btnBack: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register__employee__page, container, false)

        btnSubmit = view.findViewById(R.id.btnregEmp)
        btnBack = view.findViewById(R.id.ivBackButton)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        txtName = view.findViewById(R.id.txtEmpname)
        txtSurname = view.findViewById(R.id.txtEmpsurname)
        txtSal = view.findViewById(R.id.txtEmpsalary)
        txtTotalLeave = view.findViewById(R.id.txtTotal)
        txtLeaveLeft = view.findViewById(R.id.txtleft)
        txtNum = view.findViewById(R.id.txtnumber)
        txtEmail = view.findViewById(R.id.txtEmailInput)
        txtPasswordInput = view.findViewById(R.id.txtPasswordInput)
        txtAddress = view.findViewById(R.id.txtAddressInput)

        btnSubmit.setOnClickListener {
            // Get the user input


            btnBack.setOnClickListener {
                replaceFragment(EmployeeFragment())
            }

            if (txtName.text.isBlank() || txtSurname.text.isBlank() || txtSal.text.isBlank() ||
                txtTotalLeave.text.isBlank() || txtLeaveLeft.text.isBlank() || txtNum.text.isBlank() ||
                txtEmail.text.isBlank() || txtPasswordInput.text.isBlank() || txtAddress.text.isBlank()) {
                Toast.makeText(requireContext(), "Please enter all employee information.", Toast.LENGTH_SHORT).show()
            } else {
                // Directly save the employee under the admin
                saveEmployeeToDatabase()
                registerEmployeeAccount()
            }
        }
        return view
    }

    private fun registerEmployeeAccount() {

        // Get input data
        val email = txtEmail?.text.toString().trim()
        val password = txtPasswordInput?.text.toString().trim()
        val firstName = txtName?.text.toString().trim()
        val lastName = txtSurname?.text.toString().trim()
        val businessName = txtAddress?.text.toString().trim()
        val role = "employee" // Role is hardcoded as "employee"

        // Validate fields
        if (email.isEmpty() || password.isEmpty() || businessName.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Register user with Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: ""
                // No image for now, so passing null or a placeholder for profileImageUrl
                saveEmpInfo(userId, businessName, firstName, lastName, email, null)
            } else {
                // Registration failed
                Toast.makeText(requireContext(), "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                // Optionally, handle retries or log errors
            }
        }
    }


    private fun saveEmpInfo(userId: String, businessName: String, firstName: String, lastName: String, email: String, profileImageUrl: String?) {
        // Get the admin details first
        val adminId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        getAdminDetails(adminId) { adminName, companyName ->
            // Create an employee object with the given details
            val emp = Employee(
                name = firstName,
                surname = lastName,
                salary = txtSal.text.toString(),
                totalLeave = txtTotalLeave.text.toString(),
                leaveLeft = txtLeaveLeft.text.toString(),
                number = txtNum.text.toString(),
                email = email,
                password = txtPasswordInput.text.toString(),
                address = txtAddress.text.toString(),
                role = "employee",
                businessName = companyName,  // Use the admin's business name
                registeredBy = adminName,    // Use the admin's name
                profileImage = profileImageUrl
            )

            // Save the employee object under the admin's "Employees" node
            val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(adminId).child("Employees").child(userId)
            databaseRef.setValue(emp)
                .addOnSuccessListener {
                    // Save the employee details under their own node
                    saveEmployeeDetailsToOwnNode(userId, adminName)

                    Toast.makeText(requireContext(), "Employee added successfully!", Toast.LENGTH_SHORT).show()

                    // Step 1: Sign out the admin
                    auth.signOut()

                    // Store admin's credentials temporarily
                    val adminEmail = FirebaseAuth.getInstance().currentUser?.email
                    val adminPassword = txtPasswordInput.text.toString()

                    // Step 2: Reauthenticate the admin
                    reauthenticateAdmin(adminEmail ?: "", adminPassword)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error saving employee info: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveEmployeeDetailsToOwnNode(userId: String, adminId: String) {
        // Save the employee details under their own "Details" node
        val employeeDetailsRef =
            FirebaseDatabase.getInstance().getReference("Users").child(userId).child("Details")

        // Fetch admin details
        getAdminDetails(adminId) { adminName, companyName ->
            // Create an employee object with the given details
            val emp = Employee(
                name = txtName.text.toString(),
                surname = txtSurname.text.toString(),
                salary = txtSal.text.toString(),
                totalLeave = txtTotalLeave.text.toString(),
                leaveLeft = txtLeaveLeft.text.toString(),
                number = txtNum.text.toString(),
                email = txtEmail.text.toString(),
                password = txtPasswordInput.text.toString(),
                address = txtAddress.text.toString(),
                role = "employee",
                businessName = companyName,  // Use the admin's business name
                registeredBy = adminName,    // Use the admin's name
                profileImage = null // Assuming profileImageUrl is a global or accessible variable
            )

            // Save employee details to Firebase
            employeeDetailsRef.setValue(emp)
                .addOnSuccessListener {

                    Toast.makeText(requireContext(), "Employee details saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error saving employee details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }



    private fun reauthenticateAdmin(email: String, password: String) {
        val credential = EmailAuthProvider.getCredential(email, password)

        // Reauthenticate the admin
        FirebaseAuth.getInstance().currentUser?.reauthenticate(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Successful reauthentication
                Toast.makeText(requireContext(), "Reauthenticated successfully. You are back in your account.", Toast.LENGTH_SHORT).show()
                // Navigate back to admin's account interface, e.g., EmployeeFragment or admin dashboard
                replaceFragment(EmployeeFragment())  // Replace with your desired fragment for admin
            } else {
                Toast.makeText(requireContext(), "Reauthentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }?.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to reauthenticate: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getAdminDetails(adminId: String, callback: (String, String) -> Unit) {
        val database = Firebase.database
        val adminRef = database.getReference("Users").child(adminId).child("Details")

        adminRef.get().addOnSuccessListener { snapshot ->
            val adminName = "${snapshot.child("firstName").value} ${snapshot.child("lastName").value}"
            val companyName = snapshot.child("businessName").value.toString()
            callback(adminName, companyName)
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to retrieve admin details.", Toast.LENGTH_LONG).show()
        }
    }


    private fun saveEmployeeToDatabase() {
        val adminId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val currentAdminId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Create a new employee reference using push() to generate a unique key
        val employeeRef = FirebaseDatabase.getInstance().getReference("Users").child(currentAdminId).child("Employees").push()
        val userId = employeeRef.key // Get the newly created user ID for the employee

        getAdminDetails(adminId) { adminName, companyName ->
            // Employee data with admin's details
            val emp = Employee(
                name = txtName.text.toString(),
                surname = txtSurname.text.toString(),
                salary = txtSal.text.toString(),
                totalLeave = txtTotalLeave.text.toString(),
                leaveLeft = txtLeaveLeft.text.toString(),
                number = txtNum.text.toString(),
                email = txtEmail.text.toString(),
                password = txtPasswordInput.text.toString(),
                address = txtAddress.text.toString(),
                role = "employee",
                businessName = "", // Initially, we won't use it here
                registeredBy = "", // Initially, we won't use it here
                profileImage = null // Assuming profileImageUrl is a global or accessible variable
            )

            // Save the employee object under the admin's "Employees" node
            employeeRef.setValue(emp)
                .addOnSuccessListener {
                    // After saving to Employees, also save the employee details to their own node
                    saveEmployeeDetailsToOwnNode(userId ?: "", currentAdminId)
                    Toast.makeText(
                        requireContext(),
                        "Employee successfully added under admin.",
                        Toast.LENGTH_LONG
                    ).show()
                    replaceFragment(EmployeeFragment())  // Go back to the EmployeeFragment after saving
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Error adding employee: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }


    private fun replaceFragment(fragment: Fragment) {

        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}

// Employee data class with the registeredBy field
data class Employee(
    var name: String?,
    var surname: String?,
    var salary: String?,
    var totalLeave: String?,
    var leaveLeft: String?,
    var number: String?,
    var email: String?,
    var password: String?,
    var address: String?,
    var role: String?,
    var businessName: String?,
    var profileImage: String? = null,
    var registeredBy: String? = null // Admin's name who registered the employee
) {
    constructor() : this(null, null, null, null, null, null, null, null, null, null, null)
}