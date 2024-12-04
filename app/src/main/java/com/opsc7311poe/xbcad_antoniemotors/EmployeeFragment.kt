package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class EmployeeFragment : Fragment() {

    private lateinit var svEmpList: ScrollView
    private lateinit var linLay: LinearLayout
    private lateinit var searchBar: EditText
    private lateinit var btnSearch: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var businessId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_employee, container, false)

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!

        searchBar = view.findViewById(R.id.txtEmpSearched)
        btnSearch = view.findViewById(R.id.searchimage)
        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener {
            replaceFragment(AdminEmpFragment()) // Going back to menu
        }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                if (charSequence.isNullOrEmpty()) {
                    displayEmployees() // Display all employees if the search bar is empty
                } else {
                    filterEmployees(charSequence.toString()) // Perform the search
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        svEmpList = view.findViewById(R.id.svEmployeeList)
        linLay = view.findViewById(R.id.linlayEmployees)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val database = Firebase.database
            val empRef = database.getReference("Users").child(businessId).child("Employees")

            empRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    linLay.removeAllViews()
                    for (pulledOrder in snapshot.children) {
                        val managerId: String? = pulledOrder.child("managerID").getValue(String::class.java)
                        val fName: String? = pulledOrder.child("firstName").getValue(String::class.java)
                        val lName: String? = pulledOrder.child("lastName").getValue(String::class.java)

                        if (managerId == userId) {
                            val empName = "$fName $lName"
                            createEmployeeButton(empName, pulledOrder.key)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error fetching employees: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        return view
    }

    // Helper method to create and add an employee button
    private fun createEmployeeButton(empName: String, employeeId: String?) {
        val employeeButton = Button(requireContext()).apply {
            text = empName
            textSize = 20f
            setBackgroundColor(Color.parseColor("#038a39"))
            setTextColor(Color.WHITE)
            typeface = ResourcesCompat.getFont(requireContext(), R.font.fontpoppinsregular)
            setPadding(20, 20, 20, 20)
            background = ResourcesCompat.getDrawable(resources, R.drawable.gbutton_round, null)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 20, 0, 20) }
        }

        employeeButton.setOnClickListener {
            val employeeInfoFragment = Employee_Info_Page()
            val bundle = Bundle()
            bundle.putString("employeeId", employeeId)
            employeeInfoFragment.arguments = bundle
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(employeeInfoFragment)
        }

        linLay.addView(employeeButton)
    }

    // Method to display all employees
    private fun displayEmployees() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val empRef = Firebase.database.getReference("Users").child(businessId).child("Employees")

        empRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                linLay.removeAllViews()

                for (pulledOrder in snapshot.children) {
                    val managerId: String? = pulledOrder.child("managerID").getValue(String::class.java)
                    val fName: String? = pulledOrder.child("firstName").getValue(String::class.java)
                    val lName: String? = pulledOrder.child("lastName").getValue(String::class.java)

                    if (managerId == userId) {
                        val empName = "$fName $lName"
                        createEmployeeButton(empName, pulledOrder.key)
                    }
                }

                if (linLay.childCount == 0) {
                    Toast.makeText(requireContext(), "No employees found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching employees: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Method to search for employees
    private fun filterEmployees(query: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val empRef = Firebase.database.getReference("Users").child(businessId).child("Employees")

        empRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                linLay.removeAllViews()

                for (pulledOrder in snapshot.children) {
                    val managerId: String? = pulledOrder.child("managerID").getValue(String::class.java)
                    val fName: String? = pulledOrder.child("firstName").getValue(String::class.java)
                    val lName: String? = pulledOrder.child("lastName").getValue(String::class.java)

                    val empName = "$fName $lName".trim()

                    if (managerId == userId && empName.lowercase().contains(query.lowercase())) {
                        createEmployeeButton(empName, pulledOrder.key)
                    }
                }

                if (linLay.childCount == 0) {
                    Toast.makeText(requireContext(), "No employees found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching employees: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Method to replace fragments
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
