package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.ktx.Firebase
import android.widget.ArrayAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddTaskFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var txtTask: EditText
    private lateinit var txtTaskName: EditText
    private lateinit var btnSubmit: Button
    private lateinit var charCount: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var carSelected: Spinner
    private lateinit var businessId: String

    private val MAX_CHAR_LIMIT = 400

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add_task, container, false)

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!

        btnBack = view.findViewById(R.id.ivBackButton)
        txtTask = view.findViewById(R.id.txtTaskDescriptions)
        btnSubmit = view.findViewById(R.id.btnAddTask)
        charCount = view.findViewById(R.id.charCount)
        carSelected = view.findViewById(R.id.spChooseVehicle)
        txtTaskName = view.findViewById(R.id.txtTaskName)


        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""


        txtTask.filters = arrayOf(InputFilter.LengthFilter(MAX_CHAR_LIMIT))
        txtTask.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val remainingChars = MAX_CHAR_LIMIT - (s?.length ?: 0)
                charCount.text = "$remainingChars characters left"
            }

            override fun afterTextChanged(s: Editable?) {}
        })


        btnBack.setOnClickListener {
            replaceFragment(HomeFragment())
        }


        btnSubmit.setOnClickListener {
            submitTask()
        }

        loadVehicles()
        return view
    }

    private fun submitTask() {

        val tName = txtTaskName.text.toString().trim()

        if (tName.isBlank()) {
            Toast.makeText(requireContext(), "Please give your task a name!", Toast.LENGTH_SHORT).show()
            return
        }
      
        val taskDescription = txtTask.text.toString().trim()

        if (taskDescription.isBlank()) {
            Toast.makeText(requireContext(), "Please fill in the task description.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedVehicle = carSelected.selectedItem?.toString() ?: ""

        if (userId != null) {
            val database = Firebase.database
            val taskId = database.getReference("Users/$businessId/Employees/$userId").child("Tasks").push().key

            taskId?.let { tId ->
                val task = if (selectedVehicle == "No Vehicle" || selectedVehicle.isBlank()) {
                    Tasks(
                        taskID = tId,
                        taskName = tName,
                        taskDescription = taskDescription,
                        vehicleNumberPlate = null, // No vehicle selected
                        creationDate = System.currentTimeMillis(),
                        completedDate = null
                    )
                } else {
                    Tasks(
                        taskID = tId,
                        taskName = tName,
                        taskDescription = taskDescription,
                        vehicleNumberPlate = selectedVehicle,
                        creationDate = System.currentTimeMillis(),
                        completedDate = null
                    )
                }

                // Save the task directly under "Tasks" node if no vehicle is selected
                val taskRef = database.getReference("Users/$businessId/Employees/$userId").child("Tasks").child(tId)


                taskRef.setValue(task)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Task successfully added!", Toast.LENGTH_LONG).show()
                        replaceFragment(HomeFragment())
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error saving task: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } ?: run {
                Toast.makeText(requireContext(), "Failed to generate task ID.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "User ID is null.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun loadVehicles() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val vehicleRef = FirebaseDatabase.getInstance().getReference("Users").child(businessId).child("Vehicles")

            vehicleRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val vehicleNumbers = mutableListOf("No Vehicle") // To store vehicle number plates for the spinner


                    if (!snapshot.exists()) {
                        Toast.makeText(requireContext(), "No vehicles found for this user", Toast.LENGTH_SHORT).show()
                        return
                    }

                    for (vehicleSnapshot in snapshot.children) {
                        val numberPlate = vehicleSnapshot.child("vehicleNumPlate").getValue(String::class.java)
                        if (numberPlate != null) {
                            vehicleNumbers.add(numberPlate)
                        }
                    }

                    if (vehicleNumbers.isEmpty()) {
                        Toast.makeText(requireContext(), "No vehicles found", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, vehicleNumbers)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    carSelected.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load vehicles: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "Please log in to access vehicles.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }

}



