package com.opsc7311poe.xbcad_antoniemotors

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TasksFragment : Fragment() {

    private lateinit var svlinlay: LinearLayout
    private lateinit var svTasks: ScrollView
    private lateinit var imgFilter: ImageView
    private lateinit var searchTasks: SearchView
    private var listOfAllTasks = mutableListOf<EmpTask>()
    private lateinit var businessId: String
    private lateinit var userId: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        userId = FirebaseAuth.getInstance().currentUser?.uid!!
        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!

        //connecting elements
        svlinlay = view.findViewById(R.id.svlinlay)
        svTasks = view.findViewById(R.id.svTasks)


        //filter functionality
        imgFilter = view.findViewById(R.id.imgFilter)

        imgFilter.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            // Show a dialog with filter options
            val filterOptions = arrayOf("Not Started", "Busy", "Completed", "View All Services")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Filter Tasks By")
                .setItems(filterOptions) { _, which ->
                    when (which) {
                        0 -> filterByStatus("Not Started")
                        1 -> filterByStatus("Busy")
                        2 -> filterByStatus("Completed")
                        3 -> replaceFragment(TasksFragment())
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        //search fucnitonality
        searchTasks = view.findViewById(R.id.searchTasks)
        searchTasks.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchList(newText)
                return true
            }
        })



        //populating scrollview with all employee tasks in DB
        populateTaskScrollView()

        return view
    }

    private fun populateTaskScrollView() {
        // Reference to Firebase database for fetching tasks
        val database = Firebase.database.reference.child("Users/$businessId/EmployeeTasks")

        // Find the scroll view container in your layout

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "No tasks available", Toast.LENGTH_SHORT).show()
                    return
                }

                svlinlay.removeAllViews()  // Clear any existing tasks

                for (taskSnapshot in snapshot.children) {
                    //only displaying tasks assigned to this employee
                    val task = taskSnapshot.getValue(EmpTask::class.java)
                    task!!.taskID = taskSnapshot.key

                    if (task.employeeID == userId){

                        //adding service to list to filter later
                        listOfAllTasks.add(task);

                        if (task != null) {
                            // Inflate the card_task layout for each task
                            val taskCardView = LayoutInflater.from(requireContext())
                                .inflate(R.layout.card_task_emp, svlinlay, false) as CardView

                            // Set task details in the card
                            val txtTaskName = taskCardView.findViewById<TextView>(R.id.txtTaskName)
                            val txtTaskDesc = taskCardView.findViewById<TextView>(R.id.txtTaskDesc)
                            val txtVehNumPlate =
                                taskCardView.findViewById<TextView>(R.id.txtVehNumPlate)
                            val txtDueDate = taskCardView.findViewById<TextView>(R.id.txtDueDate)
                            val txtCompletedDate =
                                taskCardView.findViewById<TextView>(R.id.txtDateCompleted)
                            val imgStatus = taskCardView.findViewById<ImageView>(R.id.imgStatus)
                            val imgChangeStatus = taskCardView.findViewById<ImageView>(R.id.imgChangeStatus)
                            val imgRemindMe = taskCardView.findViewById<ImageView>(R.id.imgRemindMe)
                            val btnSaveChanges = taskCardView.findViewById<Button>(R.id.btnSave)

                            //temp status for saving changes
                            var tempStatus = task.status

                            // Set up the data format
                            val dateFormatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

                            // Populate data fields
                            txtTaskName.text = task.taskName ?: "Unnamed Task"
                            txtTaskDesc.text = task.taskDescription ?: "No Description"
                            getAdminName(task.adminID.toString(), taskCardView)
                            getVehicleNumPlate(task.vehicleID.toString(), taskCardView)
                            txtDueDate.text =
                                task.dueDate?.let { dateFormatter.format(Date(it)) } ?: "N/A"
                            txtCompletedDate.text =
                                task.completedDate?.let { dateFormatter.format(Date(it)) } ?: "N/A"

                            // Update status image according to task status
                            when (task.status) {
                                "Completed" -> imgStatus.setImageResource(R.drawable.vectorstatuscompleted)
                                "Busy" -> imgStatus.setImageResource(R.drawable.vectorstatusbusy)
                                "Not Started" -> imgStatus.setImageResource(R.drawable.vectorstatusnotstrarted)
                            }

                            //functionality to all the buttons on card
                            imgChangeStatus.setOnClickListener {
                                when (tempStatus) {
                                    "Completed" -> {
                                        imgStatus.setImageResource(R.drawable.vectorstatusnotstrarted)
                                        tempStatus = "Not Started"
                                    }

                                    "Busy" -> {
                                        imgStatus.setImageResource(R.drawable.vectorstatuscompleted)
                                        tempStatus = "Completed"
                                    }

                                    "Not Started" -> {
                                        imgStatus.setImageResource(R.drawable.vectorstatusbusy)
                                        tempStatus = "Busy"
                                    }
                                }
                            }

                            btnSaveChanges.setOnClickListener {
                                it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                saveStatus(task.taskID, tempStatus)
                            }

                            imgRemindMe.setOnClickListener {
                                val alarmOptions = arrayOf("5 Minutes", "30 Minutes", "1 Hour")
                                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                    .setTitle("Remind me about task in...")
                                    .setItems(alarmOptions) { _, which ->
                                        when (which) {
                                            0 -> if (checkExactAlarmPermission()){setAlarm(5, task.taskName ?: "Unnamed Task")}
                                            1 -> if (checkExactAlarmPermission()){setAlarm(30, task.taskName ?: "Unnamed Task")}
                                            2 -> if (checkExactAlarmPermission()){setAlarm(60, task.taskName ?: "Unnamed Task")}
                                        }
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            }


                            // Add the populated task card to the scroll view container
                            svlinlay.addView(taskCardView)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load tasks", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setAlarm(time: Int, taskName: String) {

        val context = requireContext()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Create an Intent to send the notification
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("taskName", taskName) // Pass the taskName to the receiver
        }

        // Use a unique request code for the pending intent
        val requestCode = System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Calculate the trigger time in milliseconds
        val triggerTimeMillis = System.currentTimeMillis() + (time * 60 * 1000L)

        // Set the alarm
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )

        Toast.makeText(context, "Notification set for $time minutes from now", Toast.LENGTH_SHORT).show()

    }

    private fun checkExactAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                requestExactAlarmPermission()
                return false
            }
        }
        return true
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
        }
    }

    private fun loadTasksFromList(inputList: MutableList<EmpTask>) {

        svlinlay.removeAllViews()  // Clear any existing tasks

        for (task in inputList) {

            if (task != null) {
                // Inflate the card_task layout for each task
                val taskCardView = LayoutInflater.from(requireContext()).inflate(R.layout.card_task_emp, svlinlay, false) as CardView

                // Set task details in the card
                val txtTaskName = taskCardView.findViewById<TextView>(R.id.txtTaskName)
                val txtTaskDesc = taskCardView.findViewById<TextView>(R.id.txtTaskDesc)
                val txtVehNumPlate = taskCardView.findViewById<TextView>(R.id.txtVehNumPlate)
                val txtDueDate = taskCardView.findViewById<TextView>(R.id.txtDueDate)
                val txtCompletedDate = taskCardView.findViewById<TextView>(R.id.txtDateCompleted)
                val imgStatus = taskCardView.findViewById<ImageView>(R.id.imgStatus)
                val imgChangeStatus = taskCardView.findViewById<ImageView>(R.id.imgChangeStatus)
                val btnSaveChanges = taskCardView.findViewById<Button>(R.id.btnSave)

                //temp status for saving changes
                var tempStatus = task.status

                // Set up the data format
                val dateFormatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

                // Populate data fields
                txtTaskName.text = task.taskName ?: "Unnamed Task"
                txtTaskDesc.text = task.taskDescription ?: "No Description"
                getAdminName(task.employeeID.toString(), taskCardView)
                getVehicleNumPlate(task.vehicleID.toString(), taskCardView)
                txtDueDate.text = task.dueDate?.let { dateFormatter.format(Date(it)) } ?: "N/A"
                txtCompletedDate.text = task.completedDate?.let { dateFormatter.format(Date(it)) } ?: "N/A"

                // Update status image according to task status
                when (task.status) {
                    "Completed" -> imgStatus.setImageResource(R.drawable.vectorstatuscompleted)
                    "Busy" -> imgStatus.setImageResource(R.drawable.vectorstatusbusy)
                    "Not Started" -> imgStatus.setImageResource(R.drawable.vectorstatusnotstrarted)
                }

                //functionality to all the buttons on card
                imgChangeStatus.setOnClickListener{
                    when (tempStatus) {
                        "Completed" -> {
                            imgStatus.setImageResource(R.drawable.vectorstatusnotstrarted)
                            tempStatus = "Not Started"
                        }
                        "Busy" -> {
                            imgStatus.setImageResource(R.drawable.vectorstatuscompleted)
                            tempStatus = "Completed"
                        }
                        "Not Started" -> {
                            imgStatus.setImageResource(R.drawable.vectorstatusbusy)
                            tempStatus = "Busy"
                        }
                    }
                }

                btnSaveChanges.setOnClickListener{
                    it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    saveStatus(task.taskID, tempStatus)
                }


                // Add the populated task card to the scroll view container
                svlinlay.addView(taskCardView)
            }
        }

    }

    private fun searchList(query: String?) {
        var tempList = mutableListOf<EmpTask>()


        if (query.isNullOrEmpty()) {
            tempList.addAll(listOfAllTasks)
        } else {
            val filter = query.lowercase()
            tempList.addAll(listOfAllTasks.filter {
                it.taskName!!.lowercase().contains(filter)
            })
        }
        loadTasksFromList(tempList)
    }

    private fun filterByStatus(statusEntered: String) {
        var tempList = mutableListOf<EmpTask>()

        tempList.addAll(listOfAllTasks.filter {
            it.status!!.equals(statusEntered)
        })

        loadTasksFromList(tempList)
    }

    private fun saveStatus(taskID: String?, tempStatus: String?) {

        if (taskID == null || tempStatus == null) return

        val taskRef = Firebase.database.reference.child("Users/$businessId/EmployeeTasks").child(taskID)

        // Fetch the employee ID for the task
        taskRef.child("employeeID").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val employeeID = snapshot.getValue(String::class.java)
                if (employeeID == null) {
                    return
                }

                val empRef = Firebase.database.reference.child("Users/$businessId/Employees").child(employeeID).child("completedTasks")

                if (tempStatus == "Completed") {
                    // Save the current time as completedDate
                    taskRef.child("completedDate").setValue(System.currentTimeMillis())
                        .addOnSuccessListener {

                            // Increment completedTasks count
                            empRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
                                override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                                    val currentCount = mutableData.getValue(Int::class.java) ?: 0
                                    mutableData.value = currentCount + 1
                                    return com.google.firebase.database.Transaction.success(mutableData)
                                }

                                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                                    if (committed) {
                                    } else {
                                    }
                                }
                            })
                        }
                        .addOnFailureListener {
                        }
                } else {
                    // Remove the completedDate value if the status is not "Completed"
                    taskRef.child("completedDate").removeValue()
                        .addOnSuccessListener {

                            // Decrement completedTasks count
                            empRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
                                override fun doTransaction(mutableData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                                    val currentCount = mutableData.getValue(Int::class.java) ?: 0
                                    mutableData.value = if (currentCount > 0) currentCount - 1 else 0
                                    return com.google.firebase.database.Transaction.success(mutableData)
                                }

                                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                                    if (committed) {
                                    } else {
                                    }
                                }
                            })
                        }
                        .addOnFailureListener {
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        // Update the task's status
        taskRef.child("status").setValue(tempStatus).addOnSuccessListener {
            Toast.makeText(requireContext(), "Task status updated", Toast.LENGTH_LONG).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error updating status: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getAdminName(employeeID: String?, cv: CardView) {

        val empRef = Firebase.database.reference.child("Users/$businessId/Employees")

        empRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot){
                for (empSnapshot in snapshot.children) {
                    val employeeId = empSnapshot.key // Get employee ID
                    val firstName = empSnapshot.child("firstName").getValue(String::class.java)
                    val lastName = empSnapshot.child("lastName").getValue(String::class.java)
                    val role = empSnapshot.child("role").getValue(String::class.java)

                    // Only add employees with the role "employee" and valid name details
                    if (role != "employee" && !firstName.isNullOrEmpty() && !lastName.isNullOrEmpty() && employeeId == employeeID) {
                        val fullName = "$firstName $lastName"

                        cv.findViewById<TextView>(R.id.txtAssignedEmp).text = fullName
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading employee data", Toast.LENGTH_SHORT).show()
            }
        })



    }

    private fun getVehicleNumPlate(serviceID: String?, cv: CardView) {
        if (serviceID == null) {
            cv.findViewById<TextView>(R.id.txtVehNumPlate).text = "No vehicle assigned"
            return
        }

        val serRef = Firebase.database.reference.child("Users/$businessId/Services/$serviceID/vehicleID")

        // Fetch the vehicle ID
        serRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val vehicleID = snapshot.getValue(String::class.java)

                if (!vehicleID.isNullOrEmpty()) {
                    // Fetch the vehicle details using the vehicle ID
                    val empRef = Firebase.database.reference.child("Users/$businessId/Vehicles/$vehicleID")

                    empRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val vehNumPlate = snapshot.child("vehicleNumPlate").getValue(String::class.java)
                            if (!vehNumPlate.isNullOrEmpty()) {
                                cv.findViewById<TextView>(R.id.txtVehNumPlate).text = vehNumPlate
                            } else {
                                cv.findViewById<TextView>(R.id.txtVehNumPlate).text = "No vehicle assigned"
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                requireContext(),
                                "Error loading vehicle data",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    cv.findViewById<TextView>(R.id.txtVehNumPlate).text = "No vehicle assigned"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Error loading service data",
                    Toast.LENGTH_SHORT
                ).show()
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