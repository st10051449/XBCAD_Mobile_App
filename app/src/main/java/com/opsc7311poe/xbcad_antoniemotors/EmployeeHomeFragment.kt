package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath

class EmployeeHomeFragment : Fragment() {

    private lateinit var btnSettings: ImageView
    private lateinit var btnGoToTask: ImageView
    private lateinit var taskContainer: LinearLayout
    private lateinit var noTasksMessage: TextView


    private lateinit var lottieAnimationView: LottieAnimationView

    private lateinit var userId: String
    private lateinit var businessId: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_employee_home, container, false)

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!

        //changing colour of animation
        lottieAnimationView = view.findViewById(R.id.lottieAnimationView)
        lottieAnimationView.addValueCallback(
            KeyPath("**"), // This applies to all layers; use specific layer names if you want more control
            LottieProperty.COLOR_FILTER
        ) { PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP) }


        // Ensure all IDs are correct and present in fragment_home.xml
        btnSettings = view.findViewById(R.id.ivSettings)
        taskContainer = view.findViewById(R.id.taskContainer)
        noTasksMessage = view.findViewById(R.id.txtNotasksToDisplay)





        //val ivFilter = view.findViewById<ImageView>(R.id.ivFilter)

        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""


        btnSettings.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(EmpSettingsFragment())
        }

        /*ivFilter.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            // Show a dialog with filter options
            val filterOptions = arrayOf("Recently Added", "Earliest Added", "Vehicle Number Plate")
            AlertDialog.Builder(requireContext())
                .setTitle("Filter Tasks By")
                .setItems(filterOptions) { _, which ->
                    when (which) {
                        0 -> applyTaskFilter("recent")
                        1 -> applyTaskFilter("earliest")
                        2 -> applyTaskFilter("plate")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }*/


        loadTasks()

        return view
    }


    private fun loadTasks() {
        val database = Firebase.database.reference.child("Users/$businessId/EmployeeTasks")
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || currentUserID == null) return

                taskContainer.removeAllViews()

                if (!snapshot.exists()) {
                    noTasksMessage.visibility = View.VISIBLE
                    noTasksMessage.text = "No tasks available at the moment."
                    return
                }

                noTasksMessage.visibility = View.GONE

                val plateColorMap = mutableMapOf<String, Int>()
                var colorIndex = 0
                val colors = listOf(Color.GRAY)

                for (taskSnapshot in snapshot.children) {
                    val taskId = taskSnapshot.key
                    val taskDescription = taskSnapshot.child("taskName").getValue(String::class.java)
                    val serviceID = taskSnapshot.child("serviceID").getValue(String::class.java)
                    val status = taskSnapshot.child("status").getValue(String::class.java)
                    val employeeID = taskSnapshot.child("employeeID").getValue(String::class.java)

                    // Only show tasks assigned to the current user and where the status is not "Completed"
                    if (taskId != null && taskDescription != null && status != "Completed" && employeeID == currentUserID) {
                        // Fetch the vehicle ID from the corresponding service
                        val serviceRef = Firebase.database.reference.child("Users/$businessId/Services/$serviceID")
                        serviceRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(serviceSnapshot: DataSnapshot) {
                                val vehicleID = serviceSnapshot.child("vehicleID").getValue(String::class.java)

                                // Fetch the vehicle number plate from the Vehicles node
                                if (vehicleID != null) {
                                    val vehicleRef = Firebase.database.reference.child("Users/$businessId/Vehicles/$vehicleID")
                                    vehicleRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(vehicleSnapshot: DataSnapshot) {
                                            val numberPlate = vehicleSnapshot.child("vehicleNumPlate").getValue(String::class.java)
                                            val vehicleModel = vehicleSnapshot.child("vehicleModel").getValue(String::class.java)

                                            // Inflate the task item layout
                                            val taskView = layoutInflater.inflate(R.layout.emp_task_item, taskContainer, false)
                                            val numberPlateText = taskView.findViewById<TextView>(R.id.numberPlateText)
                                            val taskDescriptionText = taskView.findViewById<TextView>(R.id.taskDescriptionText)

                                            taskDescriptionText.text = taskDescription

                                            // Assign colors to different vehicles based on number plate
                                            if (!plateColorMap.containsKey(numberPlate)) {
                                                plateColorMap[numberPlate ?: ""] = colors[colorIndex % colors.size]
                                                colorIndex++
                                            }

                                            val displayNumberPlate = numberPlate ?: "No vehicle assigned"
                                            val displayModel = vehicleModel ?: "Unknown model"
                                            numberPlateText.text = "$displayNumberPlate ($displayModel)"
                                            numberPlateText.setBackgroundColor(Color.GRAY)

                                            // Add the task view to the container
                                            taskContainer.addView(taskView)
                                        }

                                        override fun onCancelled(error: DatabaseError) {

                                        }
                                    })
                                } else {
                                    // If no vehicle is assigned, create a task view without vehicle info
                                    val taskView = layoutInflater.inflate(R.layout.emp_task_item, taskContainer, false)
                                    val taskDescriptionText = taskView.findViewById<TextView>(R.id.taskDescriptionText)
                                    val numberPlateText = taskView.findViewById<TextView>(R.id.numberPlateText)

                                    taskDescriptionText.text = taskDescription
                                    numberPlateText.text = "No vehicle assigned"
                                    taskContainer.addView(taskView)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }



    private fun applyTaskFilter(filterType: String) {
        val database = Firebase.database.reference.child(userId).child("Vehicles")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                // Clear existing views from taskContainer
                taskContainer.removeAllViews()

                if (!snapshot.exists()) {
                    noTasksMessage.visibility = View.VISIBLE
                    noTasksMessage.text = "No tasks available at the moment."
                    return
                }

                noTasksMessage.visibility = View.GONE

                val tasksList = mutableListOf<Tasks>() // List to hold the tasks

                // Extract and add tasks data to the list
                for (vehicleSnapshot in snapshot.children) {
                    val vehicleModel = vehicleSnapshot.child("vehicleModel").getValue(String::class.java)
                    val numberPlate = vehicleSnapshot.child("vehicleNumPlate").getValue(String::class.java)
                    val vehicleID = vehicleSnapshot.key

                    if (vehicleID != null && vehicleModel != null && numberPlate != null) {
                        val tasksSnapshot = vehicleSnapshot.child("Tasks")
                        for (taskSnapshot in tasksSnapshot.children) {
                            val taskID = taskSnapshot.key
                            val taskName = taskSnapshot.child("taskDescription").getValue(String::class.java)
                            val taskDescription = taskSnapshot.child("taskDescription").getValue(String::class.java)
                            val creationDate = taskSnapshot.child("taskCreatedDate").getValue(Long::class.java)
                            val completedDate = taskSnapshot.child("taskCompletedDate").getValue(Long::class.java)

                            // Include only tasks that are not completed, as required in HomeFragment
                            if (taskID != null && taskDescription != null && completedDate == null) {
                                val task = Tasks(
                                    taskID = taskID,
                                    taskName = taskName,
                                    taskDescription = taskDescription,
                                    vehicleNumberPlate = numberPlate,
                                    creationDate = creationDate,
                                    completedDate = null
                                )
                                tasksList.add(task)
                            }
                        }
                    }
                }

                // Sort or filter tasks based on the selected filter type
                when (filterType) {
                    "recent" -> tasksList.sortByDescending { it.creationDate }
                    "earliest" -> tasksList.sortBy { it.creationDate }
                    "plate" -> tasksList.sortBy { it.vehicleNumberPlate }
                }

                // Display the filtered/sorted tasks back in the taskContainer with the same formatting
                tasksList.forEach { task ->
                    val taskView = layoutInflater.inflate(R.layout.task_item, taskContainer, false)

                    // Get references to the TextViews in each task item layout
                    val taskCheckBox = taskView.findViewById<CheckBox>(R.id.taskCheckBox)
                    val numberPlateText = taskView.findViewById<TextView>(R.id.numberPlateText)
                    val taskDescriptionText = taskView.findViewById<TextView>(R.id.taskDescriptionText)

                    // Set data from the task
                    numberPlateText.text = "${task.vehicleNumberPlate}"
                    taskDescriptionText.text = task.taskDescription ?: "No Description"

                    // Handle task completion checkbox
                    taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            markTaskAsCompleted(
                                vehicleID = task.vehicleNumberPlate ?: "",
                                taskId = task.taskID ?: "",
                                taskView = taskView,
                                creationDate = task.creationDate ?: System.currentTimeMillis()
                            )
                        }
                    }

                    // Add the task view to the container
                    taskContainer.addView(taskView)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to retrieve tasks", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun markTaskAsCompleted(vehicleID: String, taskId: String, taskView: View, creationDate: Long) {
        val database = Firebase.database.reference.child(userId).child("Vehicles").child(vehicleID).child("Tasks").child(taskId)

        val taskUpdate = mapOf(
            "taskCompletedDate" to System.currentTimeMillis(), // Add completed date
            "taskCreatedDate" to creationDate // Ensure original creation date is stored
        )

        database.updateChildren(taskUpdate)
            .addOnSuccessListener {
                taskContainer.removeView(taskView) // Remove the task view from the container
                Toast.makeText(requireContext(), "Task completed!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to mark task as completed", Toast.LENGTH_SHORT).show()
            }
    }


    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}