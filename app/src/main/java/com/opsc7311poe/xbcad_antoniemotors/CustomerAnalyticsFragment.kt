package com.opsc7311poe.xbcad_antoniemotors

import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

import java.util.Locale


private lateinit var customerTypeBarChart: BarChart
private lateinit var customerLineGraph: LineChart
private lateinit var txtAllCustomers: TextView
private lateinit var btnBack: ImageView

private var businessId : String? = null


class CustomerAnalyticsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_customer_analytics, container, false)

        customerTypeBarChart = view.findViewById(R.id.custTypeBarChart)
        customerLineGraph = view.findViewById(R.id.custLineChart)
        txtAllCustomers = view.findViewById(R.id.txtTotalCustomers)
        btnBack = view.findViewById(R.id.ivBackButton)
        //get the businessId of the logged-in user
        getBusinessIdAndFetchVehicleCount()

        btnBack.setOnClickListener {
            replaceFragment(CustomerMenuFragment())
        }

        return view
    }

    private fun getBusinessIdAndFetchVehicleCount() {
        val adminId = FirebaseAuth.getInstance().currentUser?.uid
        if (adminId != null) {
            val usersReference = FirebaseDatabase.getInstance().getReference("Users")
            usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(usersSnapshot: DataSnapshot) {
                    for (businessSnapshot in usersSnapshot.children) {
                        val employeeSnapshot = businessSnapshot.child("Employees").child(adminId)
                        if (employeeSnapshot.exists()) {
                            businessId = employeeSnapshot.child("businessID").getValue(String::class.java)
                                ?: employeeSnapshot.child("businessId").getValue(String::class.java)

                            // Fetch customer count once businessId is retrieved
                            fetchCustomerCount()
                            fetchCustomerRegistrationData()
                            fetchCustomerTypeData()

                            break
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error fetching business ID: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun fetchCustomerCount() {
        val vehiclesReference = FirebaseDatabase.getInstance().getReference("Users/$businessId/Customers")
        vehiclesReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val totalCustomers = snapshot.childrenCount
                txtAllCustomers.text = "-$totalCustomers-"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching customer count: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun fetchCustomerRegistrationData() {
        val customersReference = FirebaseDatabase.getInstance().getReference("Users/$businessId/Customers")

        customersReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Set up date format and calendar for past 10 days
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                val past10Days = mutableListOf<String>()
                val registrationCounts = mutableMapOf<String, Int>()

                // Generate list of dates for the past 10 days and initialize counts
                for (i in 0 until 10) {
                    val dateStr = dateFormat.format(calendar.time)
                    past10Days.add(0, dateStr) // Insert at the beginning for ascending order
                    registrationCounts[dateStr] = 0 // Initialize count for each date
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }

                // Count customers registered on each date
                for (customerSnapshot in snapshot.children) {
                    val customerAddedDate = customerSnapshot.child("CustomerAddedDate").getValue(String::class.java)
                    if (customerAddedDate != null && registrationCounts.containsKey(customerAddedDate)) {
                        registrationCounts[customerAddedDate] = registrationCounts[customerAddedDate]!! + 1
                    }
                }

                // Prepare data entries for the line chart
                val entries = ArrayList<Entry>()
                for (i in past10Days.indices) {
                    val date = past10Days[i]
                    val count = registrationCounts[date] ?: 0
                    entries.add(Entry(i.toFloat(), count.toFloat()))
                }

                // Configure and display the line chart
                val lineDataSet = LineDataSet(entries, "")
                lineDataSet.color = resources.getColor(R.color.red) // Change color as needed
                lineDataSet.valueTextSize = 10f

                val lineData = LineData(lineDataSet)
                customerLineGraph.data = lineData

                // Customize the X-axis to show date labels
                val xAxis = customerLineGraph.xAxis
                customerLineGraph.setExtraBottomOffset(20f)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.valueFormatter = IndexAxisValueFormatter(past10Days)
                xAxis.labelRotationAngle = -45f // Optional: rotate labels for readability
                xAxis.granularity = 1f


                // Disable the right Y-axis and refresh the chart
                customerLineGraph.axisRight.isEnabled = false
                customerLineGraph.description.isEnabled = false
                customerLineGraph.invalidate() // Refresh the chart with new data
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching customer data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun fetchCustomerTypeData() {
        val customersReference = FirebaseDatabase.getInstance().getReference("Users/$businessId/Customers")

        customersReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Initialize counts for business and private types
                var businessCount = 0
                var privateCount = 0

                // Count the types of customers
                for (customerSnapshot in snapshot.children) {
                    val customerType = customerSnapshot.child("CustomerType").getValue(String::class.java)
                    when (customerType) {
                        "business" -> businessCount++
                        "private" -> privateCount++
                    }
                }

                // Prepare data entries for the bar chart
                val entries = ArrayList<BarEntry>()
                entries.add(BarEntry(0f, businessCount.toFloat())) // X=0 for "business"
                entries.add(BarEntry(1f, privateCount.toFloat()))  // X=1 for "private"

                // Set up the BarDataSet and customize it
                val barDataSet = BarDataSet(entries, "")
                barDataSet.color = resources.getColor(R.color.green) // Customize color as needed
                barDataSet.valueTextSize = 10f

                // Create BarData with the dataset and assign it to the chart
                val barData = BarData(barDataSet)
                customerTypeBarChart.data = barData

                // Configure x-axis to show "business" and "private" labels
                val xAxis = customerTypeBarChart.xAxis
                customerTypeBarChart.setExtraBottomOffset(20f)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.valueFormatter = IndexAxisValueFormatter(listOf("business", "private"))
                xAxis.granularity = 1f
                xAxis.labelRotationAngle = -45f // Optional: rotate labels for readability

                // Customize chart appearance
                customerTypeBarChart.axisRight.isEnabled = false // Hide right Y-axis
                customerTypeBarChart.description.isEnabled = false // Remove description text
                customerTypeBarChart.invalidate() // Refresh chart with updated data
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching customer type data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .commit()
    }


}

