package com.opsc7311poe.xbcad_antoniemotors

import android.app.DatePickerDialog
import android.content.Context
import android.media.Image
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class  HistoricalTasks : Fragment() {

    private lateinit var svTasksOld: ScrollView // Reference to the ScrollView
    private lateinit var taskContainer: LinearLayout // Reference to the LinearLayout inside ScrollView
    private lateinit var btnBack: ImageView // Reference to the LinearLayout inside ScrollView

    private var completedTasks: List<Tasks>? = null // List to store completed tasks
    private lateinit var dpStartDate: TextView
    private lateinit var dpEndDate: TextView

    private var startDate: Long? = null
    private var endDate: Long? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private lateinit var businessId: String
    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String

    companion object {
        private const val ARG_COMPLETED_TASKS = "completed_tasks"

        fun newInstance(completedTasks: List<Tasks>): HistoricalTasks {
            val fragment = HistoricalTasks()
            val args = Bundle()
            args.putParcelableArrayList(ARG_COMPLETED_TASKS, ArrayList(completedTasks))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            completedTasks = it.getParcelableArrayList(ARG_COMPLETED_TASKS)


        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_historical_tasks, container, false)

        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        businessId = sharedPreferences.getString("business_id", null) ?: ""
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        svTasksOld = view.findViewById(R.id.svTasksOld)
        taskContainer = view.findViewById(R.id.svlinlay)

        dpStartDate = view.findViewById(R.id.dpStartDate)
        dpEndDate = view.findViewById(R.id.dpEndDate)

        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener {
          replaceFragment(HomeFragment())
        }

        dpStartDate.setOnClickListener {
            showDatePickerDialog(isStartDate = true)
            fetchCompletedTasks()
        }
        dpEndDate.setOnClickListener {
            showDatePickerDialog(isStartDate = false)
            fetchCompletedTasks()
        }

        fetchCompletedTasks()

        return view
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .commit()
    }

    private fun displayCompletedTasks() {
        taskContainer.removeAllViews() // Clear previous task views

        // Filter and display tasks that have a completed date
        val tasksToDisplay = completedTasks?.filter { it.completedDate != null } ?: emptyList()

        tasksToDisplay.forEach { task ->
            // Inflate the new card layout for each task
            val taskView = layoutInflater.inflate(R.layout.card_historical_task, taskContainer, false)

            // Reference TextViews and Button in the card layout
            val txtTaskName = taskView.findViewById<TextView>(R.id.txtTaskName)
            val txtTaskDesc = taskView.findViewById<TextView>(R.id.txtTaskDesc)
            val txtVehNumPlate = taskView.findViewById<TextView>(R.id.txtVehNumPlate)
            val txtDueDate = taskView.findViewById<TextView>(R.id.txtDueDate)
            val txtDateCompleted = taskView.findViewById<TextView>(R.id.txtDateCompleted)
            val btnRestoreTask = taskView.findViewById<Button>(R.id.btnRestoreTask)

            // Set data from the task
            txtTaskName.text = task.taskName ?: "No Task Name"
            txtTaskDesc.text = task.taskDescription ?: "No Description"
            txtVehNumPlate.text = task.vehicleNumberPlate ?: "No Number Plate"
            txtDueDate.text = dateFormat.format(Date(task.creationDate ?: 0))
            txtDateCompleted.text = dateFormat.format(Date(task.completedDate ?: 0))

            // Set click listener for the "Restore Task" button
            btnRestoreTask.setOnClickListener {
                val taskId = task.taskID // Make sure you have a unique ID for each task
                val taskRef = taskId?.let { it1 ->
                    Firebase.database.reference
                        .child("Users")
                        .child(businessId)
                        .child("Employees")
                        .child(userId)
                        .child("Tasks")
                        .child(it1)
                }

                // Remove the `completedDate` field
                taskRef?.updateChildren(
                    mapOf(
                        "taskCompletedDate" to null,
                        "completedDate" to null
                    )
                )?.addOnSuccessListener {
                    Toast.makeText(context, "Task restored successfully", Toast.LENGTH_SHORT).show()
                    fetchCompletedTasks() // Refreshes tasks list
                }?.addOnFailureListener { error ->
                    Toast.makeText(context, "Failed to restore task: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }

            // Add the task view to the LinearLayout container inside ScrollView
            taskContainer.addView(taskView)
        }
    }

    private fun filterTasksByDate() {
        val filteredTasks = completedTasks?.filter { task ->
            val taskCompletedDate = task.completedDate
            val isWithinRange = taskCompletedDate != null &&
                    (startDate?.let { taskCompletedDate >= it } ?: true) &&
                    (endDate?.let { taskCompletedDate <= it } ?: true)

            isWithinRange
        }


        taskContainer.removeAllViews() // Clear current views

        filteredTasks?.forEach { task ->
            val taskView = layoutInflater.inflate(R.layout.card_historical_task, taskContainer, false)

            val txtTaskName = taskView.findViewById<TextView>(R.id.txtTaskName)
            val txtTaskDesc = taskView.findViewById<TextView>(R.id.txtTaskDesc)
            val txtVehNumPlate = taskView.findViewById<TextView>(R.id.txtVehNumPlate)
            val txtDueDate = taskView.findViewById<TextView>(R.id.txtDueDate)
            val txtDateCompleted = taskView.findViewById<TextView>(R.id.txtDateCompleted)
            val btnRestoreTask = taskView.findViewById<Button>(R.id.btnRestoreTask)

            txtTaskName.text = task.taskName ?: "No Task Name"
            txtTaskDesc.text = task.taskDescription ?: "No Description"
            txtVehNumPlate.text = task.vehicleNumberPlate ?: "No Vehicle Selected"
            txtDueDate.text = dateFormat.format(Date(task.creationDate ?: 0))
            txtDateCompleted.text = dateFormat.format(Date(task.completedDate ?: 0))

            btnRestoreTask.setOnClickListener {
                // Handle the restore task action here
            }

            taskContainer.addView(taskView)
        }
    }



    private fun showTaskPopup(task: Tasks) {
        val popupView = layoutInflater.inflate(R.layout.popup_task_details, null)

        val popupDialog = AlertDialog.Builder(requireContext())
            .setView(popupView)
            .create()

        // Populate the popup with task details
        val numberPlateText = popupView.findViewById<TextView>(R.id.txtNumberPlate)
        val taskDescriptionText = popupView.findViewById<TextView>(R.id.txtTaskDescription)
        val taskCreatedDateText = popupView.findViewById<TextView>(R.id.txtTaskCreatedDate)
        val taskCompletedDateText = popupView.findViewById<TextView>(R.id.txtTaskCompletedDate)

        numberPlateText.text = task.vehicleNumberPlate
        taskDescriptionText.text = "Task Description: ${task.taskDescription}"
        taskCreatedDateText.text = "Date Created: ${dateFormat.format(Date(task.creationDate ?: 0))}"
        taskCompletedDateText.text = "Date Completed: ${dateFormat.format(Date(task.completedDate ?: 0))}"

        popupDialog.show()
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()

        // Initialize with existing start or end date if already set
        if (isStartDate && startDate != null) {
            calendar.timeInMillis = startDate!!
        } else if (!isStartDate && endDate != null) {
            calendar.timeInMillis = endDate!!
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = calendar.time
                if (isStartDate) {
                    startDate = selectedDate.time
                    dpStartDate.text = dateFormat.format(selectedDate)
                } else {
                    endDate = selectedDate.time
                    dpEndDate.text = dateFormat.format(selectedDate)
                }
                filterTasksByDate() // Apply filtering after selecting dates
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun fetchCompletedTasks() {
        val database = Firebase.database.reference
            .child("Users")
            .child(businessId)
            .child("Employees")
            .child(userId)
            .child("Tasks")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Tasks>()
                for (taskSnapshot in snapshot.children) {
                    val task = taskSnapshot.getValue(Tasks::class.java)
                    task?.let {
                        // Set the task ID from the snapshot key
                        it.taskID = taskSnapshot.key // Ensure your Tasks class has a `taskId` property
                        // Only add tasks that have a non-null `completedDate`
                        if (it.completedDate != null) {
                            tasks.add(it)
                        }
                    }
                }
                completedTasks = tasks
                displayCompletedTasks()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load tasks", Toast.LENGTH_SHORT).show()
            }
        })
    }




    //THIS ONE WORKS
//    private fun fetchCompletedTasks() {
//        val database = Firebase.database.reference
//            .child("Users")
//            .child(businessId)
//            .child("Employees")
//            .child(userId)
//            .child("Tasks")
//
//        database.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val tasks = mutableListOf<Tasks>()
//                for (taskSnapshot in snapshot.children) {
//                    val task = taskSnapshot.getValue(Tasks::class.java)
//                    // Only add tasks that have a non-null `completedDate`
//                    if (task?.completedDate != null) {
//                        tasks.add(task)
//                        Log.d("HistoricalTasks", "Fetched task: ${task.taskName}, completed date: ${task.completedDate}")
//                    }
//                }
//                completedTasks = tasks
//                Log.d("HistoricalTasks", "Total completed tasks fetched: ${tasks.size}")
//                displayCompletedTasks()
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Toast.makeText(context, "Failed to load tasks", Toast.LENGTH_SHORT).show()
//                Log.e("HistoricalTasks", "Database error: ${error.message}")
//            }
//        })
//    }


}
