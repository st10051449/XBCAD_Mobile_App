package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class Employee_Info_Page : Fragment() {

    lateinit var txtName: TextView
    lateinit var txtNum: TextView
    lateinit var txtEmail: TextView
    lateinit var txtAddress: TextView

    private lateinit var btnBack: ImageView
    private lateinit var btnDeleteEmployee: Button
    private lateinit var btnEditEmp: Button
    private lateinit var btnSaveup: Button

    // New fields for editing
    lateinit var txtnewNum: EditText
    lateinit var txtnewEmail: EditText
    lateinit var txtnewAddress: EditText
    lateinit var txtnewSal: EditText

    private lateinit var businessID: String

    var isEditing = false
    var employeeName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_employee__info__page, container, false)

        businessID = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!
        employeeName = arguments?.getString("employeeId")

        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener {
            replaceFragment(EmployeeFragment()) // Going back to menu
        }

        // Initialize views
        txtName = view.findViewById(R.id.txtheaderempname)
        txtNum = view.findViewById(R.id.txtempcell_view)
        txtEmail = view.findViewById(R.id.txtempEmail_view)
        txtAddress = view.findViewById(R.id.txtempAddress_view)

        btnBack = view.findViewById(R.id.ivBackButton)
        btnDeleteEmployee = view.findViewById(R.id.btnDeleteEmp)
        btnEditEmp = view.findViewById(R.id.btnEditEmployeeInfo)
        btnSaveup = view.findViewById(R.id.btnSaveEmployeeInfo)

        // New fields for editing
        txtnewNum = view.findViewById(R.id.txtupdatecell)
        txtnewEmail = view.findViewById(R.id.txtupdateEmail)
        txtnewAddress = view.findViewById(R.id.txtupdateAddress)
        txtnewSal = view.findViewById(R.id.txtupdateMonthly)

        displayDetails()


        btnEditEmp.setOnClickListener {
            toggleEditing(true)
        }

        btnSaveup.setOnClickListener {
            saveEmployeeData(employeeName)
        }

        // Delete employee
        btnDeleteEmployee.setOnClickListener {
            employeeName?.let { empId ->
                deleteEmployeeData(empId)
            }
        }

        // Fetch and display employee data
        if (employeeName != null) {
            displayDetails()
        }

        return view
    }

    private fun displayDetails() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && employeeName != null) {
            val database = Firebase.database
            val empRef = database.getReference("Users").child(businessID).child("Employees")

            if (employeeName != null) {
                fetchEmployeeDetails(employeeName!!)
            }
        }
    }

    private fun fetchEmployeeDetails(employeeId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val database = Firebase.database
            val empRef = database.getReference("Users").child(businessID).child("Employees")

            val employeeRef = empRef.child(employeeId)

            employeeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val fetchedEmp = dataSnapshot.getValue(EmployeeInfo::class.java)
                    if (fetchedEmp != null) {
                        updateEmployeeUI(fetchedEmp)
                    } else {
                        Toast.makeText(requireContext(), "Employee not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(requireContext(), "Error fetching employee data: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun updateEmployeeUI(employee: EmployeeInfo?) {
        if (employee != null) {
            txtName.text = "${employee.firstName} ${employee.lastName}"
            txtNum.text = employee.phone
            txtEmail.text = employee.email
            txtAddress.text = employee.address

            // Fill in the editable fields with existing employee info
            txtnewNum.setText(employee.phone)
            txtnewEmail.setText(employee.email)
            txtnewAddress.setText(employee.address)
            txtnewSal.setText(employee.salary)
        }
    }

    private fun saveEmployeeData(employeeId: String?) {
        if (employeeId == null) {
            Toast.makeText(requireContext(), "Employee ID is missing", Toast.LENGTH_SHORT).show()
            return
        }

        val newNumber = txtnewNum.text.toString().trim()
        val newEmail = txtnewEmail.text.toString().trim()
        val newAddress = txtnewAddress.text.toString().trim()

        if (newNumber.isEmpty() || newEmail.isEmpty() || newAddress.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedData = mapOf(
            "phone" to newNumber,
            "email" to newEmail,
            "address" to newAddress
        )

        val database = Firebase.database
        val empRef = database.getReference("Users").child(businessID).child("Employees").child(employeeId)

        empRef.updateChildren(updatedData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Employee data updated", Toast.LENGTH_SHORT).show()
                toggleEditing(false)
                displayDetails()
            } else {
                Toast.makeText(requireContext(), "Failed to update employee data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleEditing(isEditing: Boolean) {
        this.isEditing = isEditing
        if (isEditing) {
            txtName.visibility = View.GONE
            txtNum.visibility = View.GONE
            txtEmail.visibility = View.GONE
            txtAddress.visibility = View.GONE

            txtnewNum.visibility = View.VISIBLE
            txtnewEmail.visibility = View.VISIBLE
            txtnewAddress.visibility = View.VISIBLE

            btnSaveup.visibility = View.VISIBLE
        } else {
            txtName.visibility = View.VISIBLE
            txtNum.visibility = View.VISIBLE
            txtEmail.visibility = View.VISIBLE
            txtAddress.visibility = View.VISIBLE

            txtnewNum.visibility = View.GONE
            txtnewEmail.visibility = View.GONE
            txtnewAddress.visibility = View.GONE

            btnSaveup.visibility = View.GONE
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.frame_container, fragment)?.commit()
    }

    private fun deleteEmployeeData(employeeId: String) {
        val database = Firebase.database

        val employeeRef = database.getReference("Users").child(businessID).child("Employees").child(employeeId)

        // Delete employee data from the database
        employeeRef.removeValue().addOnSuccessListener {
            Toast.makeText(requireContext(), "Employee deleted successfully.", Toast.LENGTH_SHORT).show()

            // Go back to the previous fragment in the back stack
            parentFragmentManager.popBackStack()
        }.addOnFailureListener { dbError ->
            Toast.makeText(requireContext(), "Failed to delete employee data: ${dbError.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
