package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.opsc7311poe.xbcad_antoniemotors.VehicleAdapter.VehicleViewHolder

class SearchVehiclesFragment : Fragment() {

    private lateinit var vectorPlusButton: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var vehicleAdapter: VehicleAdapter
    private lateinit var vehicleList: ArrayList<VehicleData>
    private lateinit var searchVehicle: SearchView
    private var businessId: String? = null // Store businessID here

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_vehicles, container, false)

        vectorPlusButton = view.findViewById(R.id.btnPlus)
        btnBack = view.findViewById(R.id.ivBackButton)
        searchVehicle = view.findViewById(R.id.vehicleSearch)
        recyclerView = view.findViewById(R.id.recyclerViewVehicle)

        vectorPlusButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(RegisterVehicleFragment())
        }

        btnBack.setOnClickListener {
            replaceFragment(VehicleMenuFragment())
        }

        vehicleList = ArrayList()
        vehicleAdapter = VehicleAdapter(vehicleList) { selectedVehicle ->
            businessId?.let { openVehicleDetailsFragment(selectedVehicle, it) }
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = vehicleAdapter


        searchVehicle.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterVehicles(newText)
                return false
            }
        })

        fetchVehicles()

        return view
    }


    private fun fetchVehicles() {
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
                            break
                        }
                    }

                    if (businessId != null) {
                        val vehicleReference = usersReference.child(businessId!!).child("Vehicles")

                        vehicleReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                vehicleList.clear()

                                if (!snapshot.exists()) {
                                    Toast.makeText(requireContext(), "No saved vehicles found.", Toast.LENGTH_SHORT).show()
                                    return
                                }

                                for (vehicleSnapshot in snapshot.children) {
                                    val vehicle = vehicleSnapshot.getValue(VehicleData::class.java)
                                    vehicle?.let {
                                        it.vehicleId = vehicleSnapshot.key ?: ""
                                        vehicleList.add(it)
                                    }
                                }

                                if (vehicleList.isEmpty()) {
                                    Toast.makeText(requireContext(), "No saved vehicles found.", Toast.LENGTH_SHORT).show()
                                } else {
                                    vehicleAdapter.notifyDataSetChanged()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(requireContext(), "Error fetching vehicles.", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {

                        Toast.makeText(requireContext(), "Unable to find associated business.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                    Toast.makeText(requireContext(), "Error fetching business information.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun filterVehicles(query: String?) {
        val filteredList = ArrayList<VehicleData>()
        if (query.isNullOrEmpty()) {
            filteredList.addAll(vehicleList)
            vehicleAdapter.updateList(filteredList)
            return
        }

        val searchQuery = query.lowercase().trim()
        val vehicleReference = FirebaseDatabase.getInstance().getReference("Users")
            .child(businessId ?: return)
            .child("Vehicles")

        vehicleReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                filteredList.clear()
                for (vehicleSnapshot in snapshot.children) {
                    val vehicle = vehicleSnapshot.getValue(VehicleData::class.java)
                    if (vehicle != null) {
                        if (vehicle.VehicleNumPlate.lowercase().contains(searchQuery) ||
                            vehicle.VehicleModel.lowercase().contains(searchQuery)) {
                            vehicle.vehicleId = vehicleSnapshot.key ?: ""
                            filteredList.add(vehicle)
                        }
                    }
                }
                vehicleAdapter.updateList(filteredList)
            }

            override fun onCancelled(error: DatabaseError) {

                Toast.makeText(requireContext(), "Error searching vehicles.", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun openVehicleDetailsFragment(vehicle: VehicleData, businessId: String) {
        if (vehicle.vehicleId.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid Vehicle ID.", Toast.LENGTH_SHORT).show()
            return
        }

        val fragment = VehicleDetailsFragment().apply {
            arguments = Bundle().apply {
                putString("vehicleId", vehicle.vehicleId) // Pass vehicleId
                putString("businessId", businessId) // Pass businessID
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}

