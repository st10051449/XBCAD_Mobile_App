package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import android.content.Context
import android.graphics.Color
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
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

private lateinit var vehicleDemoBarChart: BarChart
private lateinit var vehicleModelBarChart: BarChart
private lateinit var vehicleLineChart: LineChart
private lateinit var txtAllVehicles: TextView
private lateinit var btnBack: ImageView

private var businessId : String? = null


class VehicleAnalyticsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_vehicle_analytics, container, false)

        vehicleDemoBarChart = view.findViewById(R.id.vhlBarChart)
        vehicleModelBarChart = view.findViewById(R.id.vhlModelBarChart)
        vehicleLineChart = view.findViewById(R.id.vhlLineChart)
        txtAllVehicles = view.findViewById(R.id.txtTotalVehicles)
        btnBack = view.findViewById(R.id.ivBackButton)

        // Call method to get the businessId of the logged-in user
        getBusinessIdAndFetchVehicleCount()

        btnBack.setOnClickListener {
            replaceFragment(VehicleMenuFragment())
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

                            // Fetch vehicle count once businessId is retrieved
                            fetchVehicleCount()
                            fetchVehicleRegistrationData()
                            fetchVehicleModelData()
                            fetchVehicleDemographicData()
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

        private fun fetchVehicleCount() {
            val vehiclesReference = FirebaseDatabase.getInstance().getReference("Users/$businessId/Vehicles")
            vehiclesReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val totalVehicles = snapshot.childrenCount
                    txtAllVehicles.text = "-$totalVehicles-"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error fetching vehicle count: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

    private fun fetchVehicleRegistrationData() {
        val vehiclesReference = FirebaseDatabase.getInstance().getReference("Users/$businessId/Vehicles")

        vehiclesReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Generate past 10 days
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val calendar = Calendar.getInstance()
                val past10Days = mutableListOf<String>()
                val registrationCounts = mutableMapOf<String, Int>()

                // Generate list of dates for the past 10 days and initialize counts
                for (i in 0 until 10) {
                    val dateStr = dateFormat.format(calendar.time)
                    past10Days.add(0, dateStr)  // Insert at the beginning for ascending order
                    registrationCounts[dateStr] = 0  // Initialize with 0
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }

                // Count vehicles registered on each date
                for (vehicleSnapshot in snapshot.children) {
                    val registrationDate = vehicleSnapshot.child("registrationDate").getValue(String::class.java)
                    if (registrationDate != null && registrationCounts.containsKey(registrationDate)) {
                        registrationCounts[registrationDate] = registrationCounts[registrationDate]!! + 1
                    }
                }

                // Prepare data for the chart
                val entries = ArrayList<Entry>()
                for (i in past10Days.indices) {
                    val date = past10Days[i]
                    val count = registrationCounts[date] ?: 0
                    entries.add(Entry(i.toFloat(), count.toFloat()))
                }

                // Configure and display the line chart
                val lineDataSet = LineDataSet(entries, "")
                lineDataSet.color = resources.getColor(R.color.red)
                lineDataSet.valueTextSize = 10f

                val lineData = LineData(lineDataSet)
                vehicleLineChart.data = lineData


                val xAxis = vehicleLineChart.xAxis
                vehicleLineChart.setExtraBottomOffset(20f)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.valueFormatter = IndexAxisValueFormatter(past10Days)
                xAxis.labelRotationAngle = -45f
                xAxis.granularity = 1f


                vehicleLineChart.axisRight.isEnabled = false
                vehicleLineChart.invalidate()  // Refresh the chart
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching vehicle data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun fetchVehicleModelData() {
        val vehiclesReference = FirebaseDatabase.getInstance().getReference("Users/$businessId/Vehicles")
        val modelCounts = mutableMapOf<String, Int>()

        vehiclesReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (vehicleSnapshot in snapshot.children) {
                    val vehicleModel = vehicleSnapshot.child("vehicleModel").getValue(String::class.java)
                    if (vehicleModel != null) {
                        modelCounts[vehicleModel] = modelCounts.getOrDefault(vehicleModel, 0) + 1
                    }
                }

                // Get top 5 most registered vehicle models
                val top5Models = modelCounts.entries.sortedByDescending { it.value }.take(5)

                // Prepare data for the chart
                val entries = ArrayList<BarEntry>()
                val modelNames = ArrayList<String>()

                for ((index, model) in top5Models.withIndex()) {
                    entries.add(BarEntry(index.toFloat(), model.value.toFloat()))
                    modelNames.add(model.key)
                }

                // Configure the bar chart
                val barDataSet = BarDataSet(entries, "")
                barDataSet.color = resources.getColor(R.color.cherryred)
                barDataSet.valueTextSize = 10f

                val barData = BarData(barDataSet)
                vehicleModelBarChart.data = barData


                val xAxis = vehicleModelBarChart.xAxis
                vehicleModelBarChart.setExtraBottomOffset(40f)
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.valueFormatter = IndexAxisValueFormatter(modelNames)
                xAxis.granularity = 1f
                xAxis.labelRotationAngle = -45f


                vehicleModelBarChart.axisRight.isEnabled = false
                vehicleModelBarChart.invalidate()  // Refresh the chart
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching vehicle data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun fetchVehicleDemographicData() {
        val vehiclesReference = FirebaseDatabase.getInstance().getReference("Users/$businessId/Vehicles")
        val vehiclePORCount = mutableMapOf<String, Int>()
        val areaCodeToAreaName = mutableMapOf<String, String>()

        // Step 1: Fetch area data from VehiclePOR node
        val vehiclePORReference = FirebaseDatabase.getInstance().getReference("VehiclePOR")
        vehiclePORReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(vehiclePORSnapshot: DataSnapshot) {
                for (provinceSnapshot in vehiclePORSnapshot.children) {
                    for (areaSnapshot in provinceSnapshot.children) {
                        val areaCode = areaSnapshot.child("areaCode").getValue(String::class.java)
                        val areaName = areaSnapshot.key // The name of the area

                        if (areaCode != null && areaName != null) {
                            areaCodeToAreaName[areaCode] = areaName
                        }
                    }
                }

                // Step 2: Fetch all vehicles and match vehiclePOR to areas
                vehiclesReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (vehicleSnapshot in snapshot.children) {
                            val vehiclePOR = vehicleSnapshot.child("vehiclePOR").getValue(String::class.java)
                            if (vehiclePOR != null && areaCodeToAreaName.containsKey(vehiclePOR)) {
                                val areaName = areaCodeToAreaName[vehiclePOR]!!
                                vehiclePORCount[areaName] = vehiclePORCount.getOrDefault(areaName, 0) + 1
                            }
                        }

                        // Step 3: Get top 7 areas
                        val top7Areas = vehiclePORCount.entries.sortedByDescending { it.value }.take(7)

                        // Step 4: Prepare data for the chart
                        val entries = ArrayList<BarEntry>()
                        val areaNames = ArrayList<String>()

                        for ((index, area) in top7Areas.withIndex()) {
                            entries.add(BarEntry(index.toFloat(), area.value.toFloat()))
                            areaNames.add(area.key)
                        }

                        // Configure the bar chart
                        val barDataSet = BarDataSet(entries, "")
                        barDataSet.color = resources.getColor(R.color.green)
                        barDataSet.valueTextSize = 10f

                        val barData = BarData(barDataSet)
                        vehicleDemoBarChart.data = barData



                        val xAxis = vehicleDemoBarChart.xAxis
                        vehicleDemoBarChart.setExtraBottomOffset(40f)
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.valueFormatter = IndexAxisValueFormatter(areaNames)
                        xAxis.granularity = 1f
                        xAxis.labelRotationAngle = -45f


                        vehicleDemoBarChart.axisRight.isEnabled = false
                        vehicleDemoBarChart.invalidate()  // Refresh the chart
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Error fetching vehicle data: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching VehiclePOR data data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .commit()
    }


}


