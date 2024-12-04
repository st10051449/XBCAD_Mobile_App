package com.opsc7311poe.xbcad_antoniemotors

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*



class AddVehicleMakeModelPOR : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var btnBack: ImageView
    private lateinit var edtNewVehicleMake: EditText
    private lateinit var edtSelectedVehicleMake: EditText
    private lateinit var edtNewModel: EditText
    private lateinit var edtNewAreaName: EditText
    private lateinit var provinceSpinner: Spinner
    private lateinit var edtNewPOR: EditText
    private lateinit var txtHelp: TextView
    private lateinit var btnAddNewVMake: Button
    private lateinit var btnAddNewVModel: Button
    private lateinit var btnAddNewPOR: Button
    private lateinit var rbBNumPlate: RadioButton
    private lateinit var rbENumPlate: RadioButton
    private val vehicleMakesList = mutableListOf<String>()
    private lateinit var databaseVRef: DatabaseReference


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_vehicle_make_model_p_o_r, container, false)

        // Initialize views
        edtNewVehicleMake = view.findViewById(R.id.edttextNewVehicleMake)
        edtSelectedVehicleMake = view.findViewById(R.id.edttxtSelectedVehicleMake)
        edtNewModel = view.findViewById(R.id.edttxtNewVehicleModel)
        edtNewAreaName = view.findViewById(R.id.edttxtArea)
        edtNewPOR = view.findViewById(R.id.edttxtAddPOR)
        btnAddNewVMake = view.findViewById(R.id.btnAddNewVehicleMake)
        btnAddNewVModel = view.findViewById(R.id.btnAddVehicleModel)
        btnAddNewPOR = view.findViewById(R.id.btnAddPOR)
        btnBack = view.findViewById(R.id.ivBackButton)
        rbBNumPlate = view.findViewById(R.id.rbBeginning)
        rbENumPlate = view.findViewById(R.id.rbEnd)
        provinceSpinner = view.findViewById(R.id.spnProvince)
        txtHelp = view.findViewById(R.id.txtPORHelp)
        txtHelp.text = "Click here for more information\n about POR codes and areas"

        txtHelp.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://en.wikipedia.org/wiki/Vehicle_registration_plates_of_South_Africa")
            context?.startActivity(intent)
        }


        databaseVRef = FirebaseDatabase.getInstance().getReference("VehicleMake")

        // Fetch vehicle makes when fragment starts
        fetchVehicleMakes()

        // Handle adding a new vehicle make
        btnAddNewVMake.setOnClickListener {
            val newVehicleMake = edtNewVehicleMake.text.toString().trim()
            if (newVehicleMake.isNotEmpty() && newVehicleMake.matches(Regex("^[a-zA-Z0-9 ]*$"))) {
                checkAndAddVehicleMake(newVehicleMake)
            } else {
                Toast.makeText(requireContext(), "Invalid Vehicle Make. No special characters allowed.", Toast.LENGTH_SHORT).show()
            }
        }


        // **Set OnClickListener for edtSelectedVehicleMake to show dialog**
        edtSelectedVehicleMake.setOnClickListener {
            showVehicleMakeDialog()  // Call the function to show the vehicle make selection dialog
        }

        // Handle adding a new vehicle model
        btnAddNewVModel.setOnClickListener {
            val selectedMake = edtSelectedVehicleMake.text.toString().trim()
            val newModel = edtNewModel.text.toString().trim()
            if (selectedMake.isNotEmpty() && newModel.isNotEmpty() && newModel.matches(Regex("^[a-zA-Z0-9 ]*$"))) {
                checkAndAddVehicleModel(selectedMake, newModel)
            } else {
                Toast.makeText(requireContext(), "Please fill in both fields. No special characters allowed in the model.", Toast.LENGTH_SHORT).show()
            }
        }

        // Load provinces into the spinner
        loadProvinces()

        // Handle adding a new  area, and area registration code
        btnAddNewPOR.setOnClickListener {
            addPOR()
        }

        // Back button
        btnBack.setOnClickListener {
            replaceFragment(VehicleMenuFragment())
        }

        rbBNumPlate.setOnClickListener {
            if (rbBNumPlate.isChecked) {
                // If `rbBNumPlate` is already checked, uncheck it
                rbBNumPlate.isChecked = true
            } else {
                // Otherwise, check it and uncheck `rbENumPlate`
                rbBNumPlate.isChecked = true
                rbENumPlate.isChecked = false
            }
        }

        rbENumPlate.setOnClickListener {
            if (rbENumPlate.isChecked) {
                // If `rbENumPlate` is already checked, uncheck it
                rbENumPlate.isChecked = true
            } else {
                // Otherwise, check it and uncheck `rbBNumPlate`
                rbENumPlate.isChecked = true
                rbBNumPlate.isChecked = false
            }
        }


        return view
    }

    // Check and add vehicle make if it doesn't already exist
    private fun checkAndAddVehicleMake(make: String) {
        databaseVRef.child(make).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(requireContext(), "Vehicle Make already exists.", Toast.LENGTH_SHORT).show()
                } else {
                    databaseVRef.child(make).setValue(VehicleMakeData(make))
                    Toast.makeText(requireContext(), "Vehicle Make added.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Check and add vehicle model under the selected vehicle make
    private fun checkAndAddVehicleModel(make: String, model: String) {
        val makeRef = databaseVRef.child(make).child("Models")
        makeRef.orderByValue().equalTo(model).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(requireContext(), "Model already exists under $make.", Toast.LENGTH_SHORT).show()
                } else {
                    val newModelKey = makeRef.push().key ?: return
                    makeRef.child(newModelKey).setValue(model)
                    Toast.makeText(requireContext(), "Model added under $make.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun checkAndAddPOR(province: String, areaName: String, porCode: String, layout: Int) {
        if (layout == 0) {
            Toast.makeText(requireContext(), "Please select Beginning or End layout.", Toast.LENGTH_SHORT).show()
            return
        }

        val porRef = FirebaseDatabase.getInstance().getReference("VehiclePOR")
        porRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var isPORCodeDuplicate = false
                var isAreaNameDuplicate = false

                for (provinceSnapshot in snapshot.children) {
                    for (areaSnapshot in provinceSnapshot.children) {
                        val existingPOR = areaSnapshot.child("areaCode").value.toString().uppercase()
                        val existingAreaName = areaSnapshot.key.toString().uppercase()

                        if (existingPOR == porCode) {
                            isPORCodeDuplicate = true
                        }
                        if (provinceSnapshot.key == province && existingAreaName == areaName.uppercase()) {
                            isAreaNameDuplicate = true
                        }
                    }
                }

                if (isPORCodeDuplicate) {
                    Toast.makeText(requireContext(), "POR Code already exists in the system.", Toast.LENGTH_SHORT).show()
                } else if (isAreaNameDuplicate) {
                    Toast.makeText(requireContext(), "Area Name already exists in this province.", Toast.LENGTH_SHORT).show()
                } else {
                    val newAreaData = AreaData(porCode, layout)
                    porRef.child(province).child(areaName).setValue(newAreaData)
                    Toast.makeText(requireContext(), "Area and POR Code added successfully.", Toast.LENGTH_SHORT).show()
                    clearPORFields()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // Fetch vehicle makes to display in the dialog
    private fun fetchVehicleMakes() {
        databaseVRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                vehicleMakesList.clear()
                for (childSnapshot in snapshot.children) {
                    val vehicleMake = childSnapshot.key
                    vehicleMake?.let { vehicleMakesList.add(it) }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun showVehicleMakeDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_vehicle_make, null)
        val searchView = dialogView.findViewById<SearchView>(R.id.searchVehicleMake)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerVehicleMake)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Select Vehicle Make")
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .create()

        val adapter = VehicleMakeAdapter(vehicleMakesList) { selectedMake ->
            edtSelectedVehicleMake.setText(selectedMake)
            alertDialog.dismiss()
        }

        recyclerView.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return false
            }
        })

        alertDialog.show()
    }

    private fun addPOR() {
        val selectedProvince = provinceSpinner.selectedItem.toString()
        val newAreaName = edtNewAreaName.text.toString().trim()
        val newPOR = edtNewPOR.text.toString().trim().uppercase()
        val layout = if (rbBNumPlate.isChecked) 1 else if (rbENumPlate.isChecked) 2 else 0

        if (selectedProvince.isNotEmpty() && newAreaName.isNotEmpty() && newPOR.isNotEmpty() && newPOR.length <= 3 && newPOR.matches(Regex("^[A-Z]*$"))) {
            checkAndAddPOR(selectedProvince, newAreaName, newPOR, layout)
        } else {
            Toast.makeText(requireContext(), "Invalid Area or POR Code. POR Code must be letters only, max 3 characters.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun loadProvinces() {
        val provinceList = mutableListOf<String>()
        val databaseRef = FirebaseDatabase.getInstance().getReference("VehiclePOR")

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (provinceSnapshot in snapshot.children) {
                    provinceList.add(provinceSnapshot.key ?: "")
                }
                // Set data to spinner
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, provinceList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                provinceSpinner.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun clearPORFields() {
        edtNewAreaName.text?.clear()
        edtNewPOR.text?.clear()
        rbBNumPlate.isChecked = false
        rbENumPlate.isChecked = false
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .commit()
    }
}