package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminAnalyticsFragment : Fragment() {

    private lateinit var empBarChart: BarChart
    private lateinit var taskLineChart: LineChart
    private lateinit var txtMostProdDay: TextView
    private lateinit var txtAverageTasks: TextView
    private lateinit var ivBackButton: ImageView

    private lateinit var businessId: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_admin_analytics, container, false)

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!

        //loading bar chart
        empBarChart = view.findViewById(R.id.barChart)
        loadBarChart(empBarChart)

        //loading line chart
        taskLineChart = view.findViewById(R.id.lineChart)
        loadLineChart(taskLineChart)

        //populating most productive day data
        txtMostProdDay = view.findViewById(R.id.txtMostProdDay)
        txtAverageTasks = view.findViewById(R.id.txtAverageTasks)
        loadMostProdDay(txtMostProdDay, txtAverageTasks)

        ivBackButton = view.findViewById(R.id.ivBackButton)
        //handling back button
        ivBackButton.setOnClickListener {
            replaceFragment(AdminEmpFragment())
        }


        return view
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadMostProdDay(txtMostProdDay: TextView, txtAverageTasks: TextView) {
        val database = FirebaseDatabase.getInstance().reference.child("Users/$businessId/EmployeeTasks")
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        val thirtyDaysAgo = today - (30L * 24 * 60 * 60 * 1000) // 30 days in milliseconds

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    txtMostProdDay.text = "Not enough data available"
                    txtAverageTasks.text = ""
                    return
                }

                // Map to store total tasks for each day of the week
                val dayOfWeekCounts = mutableMapOf<Int, Int>() // Day of the week -> Task count
                val dayOfWeekOccurrences = mutableMapOf<Int, Int>() // Day of the week -> Number of occurrences

                // Count the number of tasks completed on each day
                for (taskSnapshot in snapshot.children) {
                    val completedDate = taskSnapshot.child("completedDate").getValue(Long::class.java)

                    if (completedDate != null && completedDate in thirtyDaysAgo..today) {
                        calendar.timeInMillis = completedDate
                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Get the day of the week (1 = Sunday, 7 = Saturday)

                        // Increment task count for the day
                        dayOfWeekCounts[dayOfWeek] = dayOfWeekCounts.getOrDefault(dayOfWeek, 0) + 1
                    }
                }

                // Calculate occurrences of each day of the week in the past 30 days
                calendar.timeInMillis = thirtyDaysAgo
                while (calendar.timeInMillis <= today) {
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    dayOfWeekOccurrences[dayOfWeek] = dayOfWeekOccurrences.getOrDefault(dayOfWeek, 0) + 1
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                // Calculate averages
                val dayOfWeekAverages = dayOfWeekCounts.mapValues { (dayOfWeek, taskCount) ->
                    val occurrences = dayOfWeekOccurrences[dayOfWeek] ?: 1 // Avoid division by zero
                    taskCount.toDouble() / occurrences
                }

                // Find the most productive day
                val mostProductiveDay = dayOfWeekAverages.maxByOrNull { it.value }
                if (mostProductiveDay != null) {
                    val dayOfWeekName = getDayOfWeekName(mostProductiveDay.key)
                    val averageTasks = mostProductiveDay.value

                    txtMostProdDay.text = dayOfWeekName
                    txtAverageTasks.text = "Average Tasks Completed: %.2f".format(averageTasks)
                } else {
                    txtMostProdDay.text = "Not enough data available"
                    txtAverageTasks.text = ""
                }
            }

            override fun onCancelled(error: DatabaseError) {
                txtMostProdDay.text = "Error loading data"
                txtAverageTasks.text = ""
            }
        })
    }

    private fun getDayOfWeekName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Unknown"
        }
    }




    //function to load bar chart
    private fun loadBarChart(barChart: BarChart) {
        val database = Firebase.database.reference.child("Users/$businessId/EmployeeTasks")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "No tasks available", Toast.LENGTH_SHORT).show()
                    return
                }

                val taskCounts = mutableMapOf<String, Int>() // Map to store task counts per employee
                val employeeIDs = mutableSetOf<String>()     // Set to collect unique employee IDs

                // Loop through tasks and count tasks for each employee
                for (taskSnapshot in snapshot.children) {
                    val employeeID = taskSnapshot.child("employeeID").getValue(String::class.java)
                    if (employeeID != null) {
                        taskCounts[employeeID] = taskCounts.getOrDefault(employeeID, 0) + 1
                        employeeIDs.add(employeeID)
                    }
                }

                // Fetch employee names in bulk and update the chart once all names are retrieved
                fetchEmployeeNames(employeeIDs) { employeeNamesMap ->
                    val barEntries = mutableListOf<BarEntry>()
                    val employeeNames = mutableListOf<String>() // For x-axis labels
                    var index = 0f

                    for ((employeeID, count) in taskCounts) {
                        barEntries.add(BarEntry(index, count.toFloat()))
                        employeeNames.add(employeeNamesMap[employeeID] ?: "Unknown")
                        index++
                    }

                    // Set up the BarDataSet
                    val dataSet = BarDataSet(barEntries, "Tasks Assigned").apply {
                        colors = generateColors(barEntries.size)
                        valueTextSize = 12f
                    }

                    // Set up the BarData
                    val barData = BarData(dataSet)
                    barData.barWidth = 0.9f

                    // Set up the BarChart
                    barChart.apply {
                        data = barData
                        description.isEnabled = false
                        setFitBars(true)
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            valueFormatter = IndexAxisValueFormatter(employeeNames)
                            granularity = 1f
                            setDrawGridLines(false)
                        }
                        axisLeft.apply {
                            axisMinimum = 0f
                            granularity = 1f
                        }
                        axisRight.isEnabled = false
                        animateY(1000)
                        invalidate() // Refresh the chart
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load task data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Function to fetch all employee names
    private fun fetchEmployeeNames(
        employeeIDs: Set<String>,
        callback: (Map<String, String?>) -> Unit) {
        val empRef = Firebase.database.reference.child("Users/$businessId/Employees")
        empRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val employeeNamesMap = mutableMapOf<String, String?>()
                for (employeeID in employeeIDs) {
                    val employeeSnapshot = snapshot.child(employeeID)
                    val firstName = employeeSnapshot.child("firstName").getValue(String::class.java)
                    val lastName = employeeSnapshot.child("lastName").getValue(String::class.java)
                    employeeNamesMap[employeeID] = "$firstName $lastName"
                }
                callback(employeeNamesMap)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyMap())
            }
        })
    }

    // Function to generate colors for the bars
    fun generateColors(size: Int): List<Int> {
        val baseColors = listOf(
            "#CA2F2E",
            "#506c7a",
            "#038A39",
            "#506c7a"
        )
        // Repeat colors if the dataset is larger than the base color list
        return List(size) { index -> Color.parseColor(baseColors[index % baseColors.size]) }
    }

    //line chart function
    private fun loadLineChart(lineChart: LineChart) {
        val database = Firebase.database.reference.child("Users/$businessId/EmployeeTasks")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "No tasks available", Toast.LENGTH_SHORT).show()
                    return
                }

                // Map to store task counts per day
                val taskCountsByDate = mutableMapOf<String, Int>()
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                // Loop through tasks and count completed tasks by date
                for (taskSnapshot in snapshot.children) {
                    val completedDate = taskSnapshot.child("completedDate").getValue(Long::class.java)

                    if (completedDate != null) {
                        val formattedDate = dateFormatter.format(Date(completedDate))
                        taskCountsByDate[formattedDate] = taskCountsByDate.getOrDefault(formattedDate, 0) + 1
                    }
                }

                if (taskCountsByDate.isEmpty()) {
                    Toast.makeText(requireContext(), "No completed tasks found.", Toast.LENGTH_SHORT).show()
                    return
                }

                // Prepare data for the line chart
                val lineEntries = mutableListOf<Entry>()
                val sortedDates = taskCountsByDate.keys.sortedBy { dateFormatter.parse(it) }
                sortedDates.forEachIndexed { index, date ->
                    val count = taskCountsByDate[date] ?: 0
                    lineEntries.add(Entry(index.toFloat(), count.toFloat()))
                }

                // Set up the LineDataSet
                val dataSet = LineDataSet(lineEntries, "Tasks Completed Per Day").apply {
                    color = R.color.red
                    valueTextSize = 12f
                    lineWidth = 2f
                    setCircleColor(R.color.red)
                    circleRadius = 4f
                    setDrawCircleHole(false)
                }

                // Set up the LineData
                val lineData = LineData(dataSet)

                // Set up the LineChart
                lineChart.apply {
                    data = lineData
                    description.isEnabled = false
                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        valueFormatter = IndexAxisValueFormatter(sortedDates)
                        granularity = 1f
                        setDrawGridLines(false)
                    }
                    axisLeft.apply {
                        axisMinimum = 0f
                        granularity = 1f
                    }
                    axisRight.isEnabled = false
                    animateX(1000)
                    invalidate() // Refresh the chart
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load completed tasks", Toast.LENGTH_SHORT).show()
            }
        })
    }


}
