package com.opsc7311poe.xbcad_antoniemotors

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class EMPLeaveListFragment : Fragment() {

    private lateinit var btnBackButton: ImageView
    private lateinit var leaveListContainer: LinearLayout
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var businessID: String
    private lateinit var datepicks: TextView
    private lateinit var datepicke: TextView
    private lateinit var btnsubmit: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnBackButton = view.findViewById(R.id.ivBackButton)
        leaveListContainer = view.findViewById(R.id.leaveListContainer)

        datepicks = view.findViewById(R.id.txtstartofleave)
        datepicke = view.findViewById(R.id.txtend)
        btnsubmit = view.findViewById(R.id.btnSubmit)

        businessID = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("business_id", null) ?: return

        btnBackButton.setOnClickListener {
            replaceFragment(AdminLeaveMenuFragment()) // Replace with your actual fragment class
        }

        // Set up date pickers
        setupDatePicker(datepicks)
        setupDatePicker(datepicke)

        // Display all leaves initially
        displayAllLeaves()

        // Display leaves within the selected date range when the submit button is clicked
        btnsubmit.setOnClickListener {
            val startDate = datepicks.text.toString()
            val endDate = datepicke.text.toString()
            if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                fetchLeavesWithinRange(startDate, endDate)
            } else {
                Toast.makeText(requireContext(), "Please select both start and end dates.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_e_m_p_leave_list, container, false)
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    // Set up a DatePickerDialog for the specified TextView
    private fun setupDatePicker(textView: TextView) {
        textView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
                textView.text = selectedDate
            }, year, month, day)
            datePicker.show()
        }
    }

    // Fetch leaves within the specified date range
    private fun fetchLeavesWithinRange(startDate: String, endDate: String) {
        leaveListContainer.removeAllViews()  // Clear the view

        val employeesRef = database.child("Users").child(businessID).child("Employees")

        employeesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var activeLeaveFound = false  // Flag to check if any leave is displayed

                for (employeeSnapshot in snapshot.children) {
                    val employeeId = employeeSnapshot.key ?: continue
                    val approvedLeaves = employeeSnapshot.child("ApprovedLeave")

                    for (leaveSnapshot in approvedLeaves.children) {
                        val leaveStart = leaveSnapshot.child("startDate").getValue(String::class.java) ?: continue
                        val leaveEnd = leaveSnapshot.child("endDate").getValue(String::class.java) ?: continue

                        // Only display leaves within the selected date range
                        if (isWithinRange(startDate, endDate, leaveStart, leaveEnd)) {
                            val employeeName = employeeSnapshot.child("firstName").getValue(String::class.java) + " " +
                                    employeeSnapshot.child("lastName").getValue(String::class.java)
                            addLeaveToList(employeeName ?: "Unknown", leaveStart, leaveEnd)
                            activeLeaveFound = true
                        }
                    }
                }

                if (!activeLeaveFound) {
                    Toast.makeText(requireContext(), "No leave requests found within the selected range", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load leave requests", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Display all approved leaves initially
    private fun displayAllLeaves() {
        leaveListContainer.removeAllViews()  // Clear the view

        val employeesRef = database.child("Users").child(businessID).child("Employees")

        employeesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (employeeSnapshot in snapshot.children) {
                    val employeeId = employeeSnapshot.key ?: continue
                    val approvedLeaves = employeeSnapshot.child("ApprovedLeave")

                    for (leaveSnapshot in approvedLeaves.children) {
                        val leaveStart = leaveSnapshot.child("startDate").getValue(String::class.java) ?: continue
                        val leaveEnd = leaveSnapshot.child("endDate").getValue(String::class.java) ?: continue

                        val employeeName = employeeSnapshot.child("firstName").getValue(String::class.java) + " " +
                                employeeSnapshot.child("lastName").getValue(String::class.java)
                        addLeaveToList(employeeName ?: "Unknown", leaveStart, leaveEnd)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load leave requests", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Add leave details to the UI
    private fun addLeaveToList(employeeName: String, startDate: String, endDate: String) {
        val leaveItem = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

        }

        val employeeTextView = TextView(requireContext()).apply {
            text = "Employee: $employeeName"
            textSize = 16f
        }

        val leaveDatesTextView = TextView(requireContext()).apply {
            text = "Leave Dates: $startDate to $endDate"
            textSize = 16f
        }

        leaveItem.addView(employeeTextView)
        leaveItem.addView(leaveDatesTextView)
        leaveListContainer.addView(leaveItem)
    }

    // Check if a leave period is within the selected range
    private fun isWithinRange(startRange: String, endRange: String, leaveStart: String, leaveEnd: String): Boolean {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            val startRangeDate = dateFormat.parse(startRange)
            val endRangeDate = dateFormat.parse(endRange)
            val leaveStartDate = dateFormat.parse(leaveStart)
            val leaveEndDate = dateFormat.parse(leaveEnd)

            startRangeDate != null && endRangeDate != null && leaveStartDate != null && leaveEndDate != null &&
                    !leaveEndDate.before(startRangeDate) && !leaveStartDate.after(endRangeDate)
        } catch (e: Exception) {

            false
        }
    }
}
