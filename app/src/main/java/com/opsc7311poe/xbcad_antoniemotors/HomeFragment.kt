package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.appcompat.app.AlertDialog
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale


class HomeFragment : Fragment() {

    private lateinit var btnSettings: ImageView
    private lateinit var btnGoToTask: ImageView
    private lateinit var taskContainer: LinearLayout
    private lateinit var noTasksMessage: TextView

    private lateinit var txtBusy: TextView
    private lateinit var txtToStart: TextView
    private lateinit var txtCompleted: TextView

    private lateinit var lottieAnimationView: LottieAnimationView

    private lateinit var userId: String
    private lateinit var businessId: String


    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)


        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""


        //changing colour of animation
        lottieAnimationView = view.findViewById(R.id.lottieAnimationView)
        lottieAnimationView.addValueCallback(
            KeyPath(""), // This applies to all layers; use specific layer names if you want more control
            LottieProperty.COLOR_FILTER
        ) { PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP) }

        // Ensure all IDs are correct and present in fragment_home.xml
        btnSettings = view.findViewById(R.id.ivSettings)
        btnGoToTask = view.findViewById(R.id.ivGoToAddTask)
        taskContainer = view.findViewById(R.id.taskContainer)
        noTasksMessage = view.findViewById(R.id.txtNotasksToDisplay)

        txtBusy = view.findViewById(R.id.txtCarsInProgress)
        txtToStart = view.findViewById(R.id.txtCarsToBeDone)
        txtCompleted = view.findViewById(R.id.txtCarsCompleted)

        val ivHistory = view.findViewById<ImageView>(R.id.ivHistory)
        val ivFilter = view.findViewById<ImageView>(R.id.ivFilter)

        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Attach click listeners
        btnGoToTask.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(AddTaskFragment())
        }

        btnSettings.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(SettingsFragment())
        }

        /*btnRegVehicle.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(RegisterVehicleFragment())
        }*/

        // Attach methods to ImageViews

        ivHistory.setOnClickListener {
            loadCompletedTasksForHistoricalFragment()
        }

        ivFilter.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            // Show a dialog with filter options
            val filterOptions = arrayOf("Recently Added", "Earliest Added", "Vehicle Number Plate")
            AlertDialog.Builder(requireContext())
                .setTitle("Filter Tasks By")
                .setItems(filterOptions) { _, which ->
                    when (which) {
                        0 -> {

                            applyTaskFilter("recent")
                        }
                        1 -> {

                            applyTaskFilter("earliest")
                        }
                        2 -> {

                            applyTaskFilter("plate")
                        }
                    }
                }
                .setNegativeButton("Cancel", null)  // Moved outside the setItems block
                .show()
        }

        loadTasks()
        loadServiceStatuses()

        return view
    }

    private fun displayHistoricalTasksFragment(completedTasks: List<Tasks>) {
        val historicalTasksFragment = HistoricalTasks.newInstance(completedTasks)
        replaceFragment(historicalTasksFragment)
    }

    private fun loadCompletedTasksForHistoricalFragment() {
        val database = Firebase.database.reference.child("Users/$businessId/Employees/$userId")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val completedTasks = mutableListOf<Tasks>()

                for (vehicleSnapshot in snapshot.children) {
                    val tasksSnapshot = vehicleSnapshot.child("Tasks")
                    for (taskSnapshot in tasksSnapshot.children) {
                        val taskID = taskSnapshot.key
                        val taskDescription = taskSnapshot.child("taskDescription").getValue(String::class.java)
                        val vehicleNumberPlate = taskSnapshot.child("vehicleNumberPlate").getValue(String::class.java)
                        val taskCompletedDate = taskSnapshot.child("taskCompletedDate").getValue(Long::class.java)

                        // Add the task to the list if it has been completed
                        if (taskCompletedDate != null && taskID != null && taskDescription != null ) {
                            // Use the correct constructor for the Tasks class
                            val task = Tasks(
                                taskID = taskID,
                                taskDescription = taskDescription,
                                vehicleNumberPlate = vehicleNumberPlate,
                                creationDate = null, // If creationDate is available, pass it here; otherwise, use null
                                completedDate = taskCompletedDate
                            )
                            completedTasks.add(task)
                        }
                    }
                }

                // Pass the completed tasks to the HistoricalTasksFragment
                displayHistoricalTasksFragment(completedTasks)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun loadTasks() {
        val database = Firebase.database.reference.child("Users/$businessId/Employees/$userId/Tasks")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                taskContainer.removeAllViews()

                if (!snapshot.exists()) {
                    noTasksMessage.visibility = View.VISIBLE
                    noTasksMessage.text = "No tasks available at the moment."
                    return
                }

                noTasksMessage.visibility = View.GONE

                val plateColorMap = mutableMapOf<String, Int>()
                var colorIndex = 0
                val colors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA)

                for (taskSnapshot in snapshot.children) {
                    val taskDescription = taskSnapshot.child("taskName").getValue(String::class.java)
                    val taskId = taskSnapshot.key
                    val creationDate = taskSnapshot.child("taskCreatedDate").getValue(Long::class.java)
                    val completedDate = taskSnapshot.child("taskCompletedDate").getValue(Long::class.java)
                    val vehicleNumberPlate = taskSnapshot.child("vehicleNumberPlate").getValue(String::class.java)

                    // Only show tasks that do not have a completed date
                    if (taskId != null && taskDescription != null && completedDate == null) {
                        // Inflate the task item layout
                        val taskView = layoutInflater.inflate(R.layout.task_item, taskContainer, false)
                        val taskCheckBox = taskView.findViewById<CheckBox>(R.id.taskCheckBox)
                        val numberPlateText = taskView.findViewById<TextView>(R.id.numberPlateText)
                        val taskDescriptionText = taskView.findViewById<TextView>(R.id.taskDescriptionText)

                        taskDescriptionText.text = taskDescription

                        // Set vehicle details or "No vehicle assigned"
                        if (!vehicleNumberPlate.isNullOrEmpty()) {
                            numberPlateText.text = vehicleNumberPlate

                            // Assign colors to different vehicles based on number plate
                            if (!plateColorMap.containsKey(vehicleNumberPlate)) {
                                plateColorMap[vehicleNumberPlate] = colors[colorIndex % colors.size]
                                colorIndex++
                            }
                            numberPlateText.setBackgroundColor(plateColorMap[vehicleNumberPlate] ?: Color.GRAY)
                        } else {
                            numberPlateText.text = "No vehicle assigned"
                            numberPlateText.setBackgroundColor(Color.GRAY)
                        }

                        // Handle task completion
                        taskCheckBox.setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                markTaskAsCompleted(taskId, taskView, creationDate ?: System.currentTimeMillis())
                            }
                        }

                        // Add the task view to the container
                        taskContainer.addView(taskView)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun markTaskAsCompleted(taskId: String, taskView: View, creationDate: Long) {
        val db1 =
            Firebase.database.reference.child("Users/$businessId/Employees/$userId").child("Tasks")
                .child(taskId)

        if (taskId.isBlank()) {
            return
        }
        val db = Firebase.database.reference
            .child("Users")
            .child(businessId)
            .child("Employees")
            .child(userId)
            .child("Tasks")
            .child(taskId)
        // Prepare data for completion
        val taskUpdate = mapOf(
            "completedDate" to System.currentTimeMillis(), // Set completed date to the current time
            "creationDate" to creationDate, // Keep the original creation date
            "taskCompletedDate" to System.currentTimeMillis(), // Add completed date
            "taskCreatedDate" to creationDate // Ensure original creation date is stored
        )

        // Update the Firebase node with the completed date
        db.updateChildren(taskUpdate)
            .addOnSuccessListener {
                taskContainer.removeView(taskView) // Remove the task view from the container
                Toast.makeText(requireContext(), "Task completed!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to mark task as completed",
                    Toast.LENGTH_SHORT
                ).show()
            }


    }
        private fun applyTaskFilter(filterType: String) {
        // Get the current user id dynamically (use FirebaseAuth to get the UID)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            // Handle the case where no user is logged in
            return
        }

        // Build the path to the current user's tasks under their employee node
        val database = Firebase.database.reference.child("Users/$businessId/Employees/$currentUserId/Tasks")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) {

                    return
                }

                // Clear existing views from taskContainer
                taskContainer.removeAllViews()


                if (!snapshot.exists()) {
                    noTasksMessage.visibility = View.VISIBLE
                    noTasksMessage.text = "No tasks available at the moment."
                    return
                }

                noTasksMessage.visibility = View.GONE
                val tasksList = mutableListOf<Tasks>() // List to hold tasks

                // Extract and add tasks data to the list
                for (taskSnapshot in snapshot.children) {
                    val taskId = taskSnapshot.key
                    val taskDescription = taskSnapshot.child("taskName").getValue(String::class.java)
                    val creationDate = taskSnapshot.child("creationDate").getValue(Long::class.java)
                    val completedDate = taskSnapshot.child("taskCompletedDate").getValue(Long::class.java)
                    val numberPlate = taskSnapshot.child("vehicleNumberPlate").getValue(String::class.java)

                    // Include only tasks that are not completed
                    if (taskId != null && taskDescription != null && completedDate == null) {
                        val task = Tasks(
                            taskID = taskId,
                            taskDescription = taskDescription,
                            vehicleNumberPlate = numberPlate,
                            creationDate = creationDate,
                            completedDate = null
                        )
                        tasksList.add(task)
                    }
                }

                // Sort or filter tasks based on the selected filter type
                when (filterType) {
                    "recent" -> tasksList.sortByDescending { it.creationDate }
                    "earliest" -> tasksList.sortBy { it.creationDate }
                    "plate" -> tasksList.sortBy { it.vehicleNumberPlate }
                }

                // Map to hold colors for each vehicle number plate
                val plateColorMap = mutableMapOf<String, Int>()
                val colors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA)
                var colorIndex = 0

                // Display the filtered/sorted tasks back in the taskContainer
                tasksList.forEach { task ->
                    val taskView = layoutInflater.inflate(R.layout.task_item, taskContainer, false)

                    // Get references to the TextViews in each task item layout
                    val numberPlateText = taskView.findViewById<TextView>(R.id.numberPlateText)
                    val taskDescriptionText = taskView.findViewById<TextView>(R.id.taskDescriptionText)

                    // Set data from the task
                    taskDescriptionText.text = task.taskDescription

                    // Set vehicle details or "No vehicle assigned"
                    val numberPlate = task.vehicleNumberPlate ?: "No vehicle assigned"
                    numberPlateText.text = numberPlate

                    // Assign colors to different vehicles based on number plate
                    if (!plateColorMap.containsKey(numberPlate)) {
                        plateColorMap[numberPlate] = colors[colorIndex % colors.size]
                        colorIndex++
                    }
                    numberPlateText.setBackgroundColor(plateColorMap[numberPlate] ?: Color.GRAY)

                    // Update UI on the main thread
                    requireActivity().runOnUiThread {
                        taskContainer.addView(taskView)
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }




    private fun loadServiceStatuses() {
        val database = Firebase.database.reference.child("Users/$businessId/Services")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var busyCount = 0
                var completedCount = 0
                var notStartedCount = 0

                if (!snapshot.exists()) {
                    if (isAdded) {
                        txtToStart.text = "Nothing To Be Started"
                        txtBusy.text = "Nothing Is In Progress"
                        txtCompleted.text = "Nothing Is Completed"
                    }
                    return
                }

                // Iterate through each service and count statuses
                for (serviceSnapshot in snapshot.children) {
                    val status = serviceSnapshot.child("status").getValue(String::class.java)

                    when (status) {
                        "Busy" -> busyCount++
                        "Completed" -> completedCount++
                        "Not Started" -> notStartedCount++
                    }
                }

                if (isAdded) {
                    // Update UI elements with the counts
                    txtToStart.text = if (notStartedCount == 0) "Nothing To Be Started" else "$notStartedCount Cars To Start"
                    txtBusy.text = if (busyCount == 0) "Nothing Is In Progress" else "$busyCount Cars In Progress"
                    txtCompleted.text = if (completedCount == 0) "Nothing Is Completed" else "$completedCount Cars Completed"
                }
            }

            override fun onCancelled(error: DatabaseError) {
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