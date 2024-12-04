package com.opsc7311poe.xbcad_antoniemotors

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class RegisterVehicleFragment : Fragment() {

    private lateinit var edtCustomer: EditText
    private lateinit var edtVehicleNoPlate: EditText
    private lateinit var edtVehicleMake: EditText
    private lateinit var spnVehiclePOR: Spinner
    private lateinit var edtVehicleModel: EditText
    private lateinit var edtVinNumber: EditText
    private lateinit var edtVehicleKms: EditText
    private lateinit var btnSubmitRegVehicle: Button

    //side specific vehicle camera buttons
    private lateinit var imgFront: ImageView
    private lateinit var imgRight: ImageView
    private lateinit var imgRear: ImageView
    private lateinit var imgLeft: ImageView

    // Separate lists for each side
    private val frontImageUris = mutableListOf<Uri>()
    private val rightImageUris = mutableListOf<Uri>()
    private val rearImageUris = mutableListOf<Uri>()
    private val leftImageUris = mutableListOf<Uri>()
    private var currentSide: String? = null



    private lateinit var displayFront: RecyclerView
    private lateinit var displayRight: RecyclerView
    private lateinit var displayRear: RecyclerView
    private lateinit var displayLeft: RecyclerView
    private var selectedCustomerId: String = ""
    private val imageUris = mutableListOf<Uri>()
    private lateinit var btnBack: ImageView
    private lateinit var vehiclePORList: ArrayList<String> // To store the values (e.g., "CA")
    private lateinit var vehiclePORAdapter: ArrayAdapter<String>
    private lateinit var ynpYearPicker : NumberPicker


    // Firebase references
    // Reference to the Customers node in your database
    val databaseRef = FirebaseDatabase.getInstance().getReference("Customers")
    val databaseVRef = FirebaseDatabase.getInstance().getReference("VehicleMake")

    // List to hold vehicle makes
    val vehicleMakesList = mutableListOf<String>()

    private val database = FirebaseDatabase.getInstance().getReference("Vehicles")
    private val storage = FirebaseStorage.getInstance().getReference("VehicleImages")

    private val PICK_IMAGES_REQUEST = 100

    //accessing the camera
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val CAMERA_REQUEST_CODE = 101
    private val REQUEST_IMAGE_CAPTURE = 102


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register_vehicle, container, false)

        // Initialize views
        edtCustomer = view.findViewById(R.id.edtCustomerNames)
        edtVehicleNoPlate = view.findViewById(R.id.edttxtVehicleNoPlate)
        edtVehicleModel = view.findViewById(R.id.edttxtVehicleModel)
        edtVehicleMake = view.findViewById(R.id.edttxtVehicleMake)
        spnVehiclePOR = view.findViewById(R.id.spinnerPOR)
        edtVinNumber = view.findViewById(R.id.edttxtVinNumber)
        edtVehicleKms = view.findViewById(R.id.edttxtVehicleKms)
        btnSubmitRegVehicle = view.findViewById(R.id.btnSubmitRegVehicle)
        btnBack = view.findViewById(R.id.ivBackButton)

        displayFront = view.findViewById(R.id.rvVehicleFront)
        displayRight = view.findViewById(R.id.rvVehicleRightSide)
        displayRear = view.findViewById(R.id.rvVehicleLeftSide)
        displayLeft = view.findViewById(R.id.rvVehicleRear)

        imgFront = view.findViewById(R.id.imgVehicleFront)
        imgRight = view.findViewById(R.id.imgVehicleRightSide)
        imgRear = view.findViewById(R.id.imgVehicleRear)
        imgLeft = view.findViewById(R.id.imgVehicleLeftSide)
        ynpYearPicker = view.findViewById(R.id.npYearPicker)


        edtCustomer.setOnClickListener {
            showCustomerSelectionDialog()
        }


        vehiclePORList = ArrayList()

        // Set up adapter for Spinner
        vehiclePORAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, vehiclePORList)
        vehiclePORAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnVehiclePOR.adapter = vehiclePORAdapter

        fetchVehiclePORData()

        fetchVehicleMakes()

        val frontAdapter = ImageAdapter(frontImageUris) { uri -> removeImage(uri, frontImageUris) }
        val rightAdapter = ImageAdapter(rightImageUris) { uri -> removeImage(uri, rightImageUris) }
        val rearAdapter = ImageAdapter(rearImageUris) { uri -> removeImage(uri, rearImageUris) }
        val leftAdapter = ImageAdapter(leftImageUris) { uri -> removeImage(uri, leftImageUris) }

        displayFront.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        displayFront.adapter = frontAdapter

        displayRight.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        displayRight.adapter = rightAdapter

        displayRear.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        displayRear.adapter = rearAdapter

        displayLeft.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        displayLeft.adapter = leftAdapter


        // Set click listeners for capturing images
        imgFront.setOnClickListener { handleCameraPermission("front") }
        imgRight.setOnClickListener { handleCameraPermission("right") }
        imgRear.setOnClickListener { handleCameraPermission("rear") }
        imgLeft.setOnClickListener { handleCameraPermission("left") }

        edtVehicleMake.setOnClickListener {
            showVehicleMakeDialog()
        }

        setupYearPicker()

        setupEditorActions()


        edtVehicleModel.setOnClickListener {
            val selectedMake = edtVehicleMake.text.toString()
            if (selectedMake.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a vehicle make first.", Toast.LENGTH_SHORT).show()
            } else {
                // Call the function to show the pop-up for model selection
                showVehicleModelDialog(selectedMake)
            }
        }

        // Submit vehicle registration
        btnSubmitRegVehicle.setOnClickListener {
            registerVehicle()
        }

        btnBack.setOnClickListener {
            replaceFragment(SearchVehiclesFragment())
        }


        return view
    }

    fun showVehicleMakeDialog() {
        // Inflate the dialog view layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_vehicle_make, null)
        val searchView = dialogView.findViewById<SearchView>(R.id.searchVehicleMake)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerVehicleMake)

        // Set up RecyclerView with a LinearLayoutManager
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Create the AlertDialog before setting up the adapter
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Select Vehicle Make")
            .setNegativeButton("Cancel", null)
            .setCancelable(true)
            .create()

        // Adapter for displaying vehicle makes
        val adapter = VehicleMakeAdapter(vehicleMakesList) { selectedMake ->
            // Handle the vehicle make selection
            edtVehicleMake.setText(selectedMake)
            // Dismiss the dialog when an item is selected
            alertDialog.dismiss()
        }

        recyclerView.adapter = adapter

        // Filter the vehicle makes based on search query
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return false
            }
        })

        // Show the dialog
        alertDialog.show()
    }


    private fun showVehicleModelDialog(selectedMake: String) {
        // Create a dialog
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_vehicle_model) // Custom layout for the model selection

        // Reference UI components inside the dialog
        val listViewModels = dialog.findViewById<ListView>(R.id.listViewModels)
        val searchViewModels = dialog.findViewById<SearchView>(R.id.searchViewModels)

        val modelList = ArrayList<String>() // List to hold models for the selected make
        val modelAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, modelList)
        listViewModels.adapter = modelAdapter

        // Fetch models from Firebase for the selected make
        fetchVehicleModels(selectedMake, modelList, modelAdapter)

        // Handle search functionality
        searchViewModels.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                modelAdapter.filter.filter(newText)
                return false
            }
        })

        // Handle model selection
        listViewModels.setOnItemClickListener { _, _, position, _ ->
            // Get the filtered item directly from the adapter
            val selectedModel = modelAdapter.getItem(position) ?: ""
            edtVehicleModel.setText(selectedModel) // Set the selected model in the edit text
            dialog.dismiss() // Close the dialog
        }

        dialog.show() // Show the dialog
    }


    private fun fetchVehicleMakes(){
        // Fetch vehicle makes from Firebase
        databaseVRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                vehicleMakesList.clear()  // Clear list before adding
                for (snapshot in dataSnapshot.children) {
                    val vehicleMake = snapshot.key.toString()
                    vehicleMakesList.add(vehicleMake)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors.
                Toast.makeText(requireContext(), "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchVehicleModels(selectedMake: String, modelList: ArrayList<String>, modelAdapter: ArrayAdapter<String>) {
        // Reference to VehicleData/VehicleMake/selectedMake/Models in Firebase
        val modelsRef = FirebaseDatabase.getInstance().getReference("VehicleMake/$selectedMake/Models")

        modelsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                modelList.clear() // Clear the list first
                for (modelSnapshot in snapshot.children) {
                    val model = modelSnapshot.getValue(String::class.java)
                    if (model != null) {
                        modelList.add(model)
                    }
                }
                // Notify adapter about data change
                modelAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching vehicle models: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun fetchVehiclePORData() {
        val vehiclePORRef = FirebaseDatabase.getInstance().getReference("VehiclePOR")

        vehiclePORRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                vehiclePORList.clear()

                // Loop through each province under VehiclePOR
                for (provinceSnapshot in snapshot.children) {
                    // Loop through each area under the province
                    for (areaSnapshot in provinceSnapshot.children) {
                        val areaCode = areaSnapshot.child("areaCode").getValue(String::class.java)
                        if (areaCode != null) {
                            // Add each area code to the list
                            vehiclePORList.add(areaCode)
                        }
                    }
                }

                // Sort area codes alphabetically
                vehiclePORList.sort()

                // Notify adapter about the data change
                vehiclePORAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun setupYearPicker() {

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val startYear = 1900 // Define the starting year
        val endYear = currentYear // Define the end year as the current year

        ynpYearPicker?.minValue = startYear
        ynpYearPicker?.maxValue = endYear
        ynpYearPicker?.value = currentYear // Set the current year as default

        // Optional: Listener to handle year selection
        ynpYearPicker?.setOnValueChangedListener { picker, oldVal, newVal ->
            if (newVal > currentYear) {
                Toast.makeText(requireContext(), "A car model year cannot be greater than the current year", Toast.LENGTH_SHORT).show()
                ynpYearPicker.value = currentYear
            }
        }
    }



    private fun setupEditorActions() {
        edtVehicleNoPlate.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                edtVehicleModel.requestFocus()
                true
            } else {
                false
            }
        }

        edtVehicleModel.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                edtVinNumber.requestFocus()
                true
            } else {
                false
            }
        }

        edtVinNumber.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                edtVehicleKms.requestFocus()
                true
            } else {
                false
            }
        }

        edtVehicleKms.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                imgFront.requestFocus()
                true
            } else {
                false
            }
        }
    }


    private fun showCustomerSelectionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_customer_names, null)
        val searchView = dialogView.findViewById<SearchView>(R.id.searchCustomerName)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerCustomerName)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Select Customer")
            .setNegativeButton("Cancel", null)
            .create()

        populateCustomerList { customers ->
            var filteredCustomers = customers.toList()
            val adapter = VehicleCustomerAdapter(filteredCustomers) { selectedCustomer ->
                // Set both customer name and ID when a customer is selected
                edtCustomer.setText("${selectedCustomer.CustomerName} ${selectedCustomer.CustomerSurname}")
                selectedCustomerId = selectedCustomer.CustomerID  // Save the selected customer ID
                dialog.dismiss()  // Dismiss the dialog on selection
            }

            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    val searchText = newText?.lowercase()?.trim()
                    filteredCustomers = if (searchText.isNullOrEmpty()) {
                        customers
                    } else {
                        customers.filter {
                            "${it.CustomerName} ${it.CustomerSurname}".lowercase().contains(searchText)
                        }
                    }
                    adapter.updateCustomers(filteredCustomers)
                    return true
                }
            })

            dialog.show()
        }
    }



    private fun populateCustomerList(onCustomersFetched: (List<CustomerData>) -> Unit) {
        val adminId = FirebaseAuth.getInstance().currentUser?.uid
        if (adminId == null) {
            Toast.makeText(requireContext(), "User is not logged in or adminId is null.", Toast.LENGTH_SHORT).show()
            return
        }

        // Reference to the "Users" node in Firebase
        val usersReference = FirebaseDatabase.getInstance().getReference("Users")

        usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(usersSnapshot: DataSnapshot) {
                var businessID: String? = null

                // Iterate over each business node to find where adminId exists under Employees
                for (businessSnapshot in usersSnapshot.children) {
                    val employeeSnapshot = businessSnapshot.child("Employees").child(adminId)

                    if (employeeSnapshot.exists()) {
                        // Found the employee record; extract the associated BusinessID
                        //businessID = employeeSnapshot.child("businessID").getValue(String::class.java)
                        businessID = employeeSnapshot.child("businessID").getValue(String::class.java)
                            ?: employeeSnapshot.child("businessId").getValue(String::class.java)

                        break
                    }
                }

                if (businessID != null) {
                    // Now use BusinessID to retrieve customers under the correct business
                    val customerReference = usersReference.child(businessID).child("Customers")

                    customerReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val customerList = mutableListOf<CustomerData>()

                            if (!snapshot.exists()) {
                                onCustomersFetched(customerList) // Return an empty list if no customers found
                                return
                            }

                            // Retrieve each customer and add to the list
                            for (customerSnapshot in snapshot.children) {
                                val customer = customerSnapshot.getValue(CustomerData::class.java)
                                customer?.let {
                                    customerList.add(it)
                                }
                            }

                            // Pass the list of CustomerData to the callback function
                            onCustomersFetched(customerList)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(requireContext(), "Error fetching customers : ${error.message}", Toast.LENGTH_SHORT).show()

                        }
                    })
                } else {
                    Toast.makeText(requireContext(), "BusinessID not found for the current admin.", Toast.LENGTH_SHORT).show()

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching business information: ${error.message}", Toast.LENGTH_SHORT).show()

            }
        })
    }


    private fun registerVehicle() {
        // Retrieve and format inputs
        val vehicleNoPlate = edtVehicleNoPlate.text.toString().trim().uppercase()
        val vehicleModel = edtVehicleModel.text.toString().trim()
        val vinNumber = edtVinNumber.text.toString().trim().uppercase()
        val vehicleKms = edtVehicleKms.text.toString().trim()
        val vehiclePOR = spnVehiclePOR.selectedItem.toString()  // This represents the selected area code (e.g., "GP")
        val vehicleOwner = edtCustomer.text.toString()

        // Validate inputs
        if (vehicleNoPlate.isEmpty() || vehicleModel.isEmpty() || vehicleKms.isEmpty()) {
            Toast.makeText(context, "Please fill in all mandatory fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (!vehicleNoPlate.matches(Regex("^[a-zA-Z0-9]{1,7}$"))) {
            Toast.makeText(context, "Invalid number plate. It must be alphanumeric and 1-7 characters long.", Toast.LENGTH_SHORT).show()
            return
        }
        if (vinNumber.isNotEmpty() && !vinNumber.matches(Regex("^[A-Z0-9]{17}$"))) {
            Toast.makeText(context, "Invalid VIN number. It must be exactly 17 alphanumeric characters.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!vehicleKms.matches(Regex("^[0-9]+$"))) {
            Toast.makeText(context, "Invalid kilometers. It must be a number.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedCustomerId.isEmpty()) {
            Toast.makeText(context, "Please select a customer", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if images are taken for all sides
        if (frontImageUris.isEmpty() || rightImageUris.isEmpty() || rearImageUris.isEmpty() || leftImageUris.isEmpty()) {
            Toast.makeText(context, "Please capture at least one image for each side of the vehicle", Toast.LENGTH_SHORT).show()
            return
        }

        // Reference to the VehiclePOR node in Firebase to find the layout for the selected area code
        val vehiclePORRef = FirebaseDatabase.getInstance().getReference("VehiclePOR")
        vehiclePORRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var layout: Int? = null
                var fullVehicleNumPlate: String? = null

                // Iterate through each province to find the area with the matching area code
                for (provinceSnapshot in snapshot.children) {
                    for (areaSnapshot in provinceSnapshot.children) {
                        val areaCode = areaSnapshot.child("areaCode").getValue(String::class.java)
                        if (areaCode == vehiclePOR) {
                            layout = areaSnapshot.child("layout").getValue(Int::class.java) ?: 1
                            fullVehicleNumPlate = if (layout == 1) "$vehiclePOR $vehicleNoPlate" else "$vehicleNoPlate $vehiclePOR"
                            break
                        }
                    }
                    if (layout != null) break
                }

                if (layout == null || fullVehicleNumPlate == null) {
                    Toast.makeText(context, "Error: Layout not found for selected area code.", Toast.LENGTH_SHORT).show()
                    return
                }

                // Proceed with the rest of the registration
                val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val selectedYear = ynpYearPicker.value.toString()
                val currentUser = FirebaseAuth.getInstance().currentUser
                val adminId = currentUser?.uid ?: return

                // Retrieve business ID and full name
                val usersReference = FirebaseDatabase.getInstance().getReference("Users")
                usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(usersSnapshot: DataSnapshot) {
                        var businessId: String? = null
                        var adminFullName: String? = null

                        for (businessSnapshot in usersSnapshot.children) {
                            val employeeSnapshot = businessSnapshot.child("Employees").child(adminId)
                            if (employeeSnapshot.exists()) {
                                businessId = businessSnapshot.key
                                val firstName = employeeSnapshot.child("firstName").getValue(String::class.java)
                                val lastName = employeeSnapshot.child("lastName").getValue(String::class.java)
                                val name = employeeSnapshot.child("name").getValue(String::class.java)
                                val surname = employeeSnapshot.child("surname").getValue(String::class.java)
                                adminFullName = when {
                                    !firstName.isNullOrEmpty() && !lastName.isNullOrEmpty() -> "$firstName $lastName"
                                    !name.isNullOrEmpty() && !surname.isNullOrEmpty() -> "$name $surname"
                                    else -> "Unknown Admin"
                                }
                                break
                            }
                        }

                        if (businessId != null && adminFullName != null) {
                            // Check for duplicates
                            val vehicleReference = usersReference.child(businessId).child("Vehicles")
                            vehicleReference.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(vehicleSnapshot: DataSnapshot) {
                                    var isDuplicate = false
                                    for (vehicleSnap in vehicleSnapshot.children) {
                                        val existingPlate = vehicleSnap.child("vehicleNumPlate").getValue(String::class.java)
                                        val existingVin = vehicleSnap.child("vinNumber").getValue(String::class.java)

                                        if (existingPlate == fullVehicleNumPlate) {
                                            Toast.makeText(context, "A vehicle with this number plate already exists.", Toast.LENGTH_SHORT).show()
                                            isDuplicate = true
                                            break
                                        }
                                        if (vinNumber.isNotEmpty() && existingVin == vinNumber) {
                                            Toast.makeText(context, "A vehicle with this VIN number already exists.", Toast.LENGTH_SHORT).show()
                                            isDuplicate = true
                                            break
                                        }
                                    }

                                    if (!isDuplicate) {
                                        // No duplicates found, proceed with registration
                                        val vehicle = VehicleData(
                                            VehicleOwner = vehicleOwner,
                                            customerID = selectedCustomerId,
                                            VehicleNumPlate = fullVehicleNumPlate,
                                            VehiclePOR = vehiclePOR,
                                            VehicleModel = vehicleModel,
                                            VehicleMake = edtVehicleMake.text.toString(),
                                            VehicleYear = selectedYear,
                                            VinNumber = if (vinNumber.isEmpty()) "N/A" else vinNumber,
                                            VehicleKms = vehicleKms,
                                            registrationDate = currentDate,
                                            AdminID = adminId,
                                            AdminFullName = adminFullName
                                        )

                                        val vehicleRef = vehicleReference.push()
                                        vehicle.vehicleId = vehicleRef.key ?: ""

                                        vehicleRef.setValue(vehicle).addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                uploadVehicleImages(businessId, vehicleRef.key!!)
                                                Toast.makeText(context, "Vehicle registered successfully", Toast.LENGTH_SHORT).show()
                                                clearInputFields()
                                                clearAllImages()
                                            } else {
                                                Toast.makeText(context, "Failed to register vehicle", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(requireContext(), "Database error ${error.message}", Toast.LENGTH_SHORT).show()
                                    Toast.makeText(context, "Error checking for duplicates.", Toast.LENGTH_SHORT).show()
                                }
                            })
                        } else {
                            Toast.makeText(context, "Unable to register vehicle. Business not found.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Error fetching business information.", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error retrieving layout for POR.", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun clearInputFields() {
        edtVehicleNoPlate.text.clear()  // Clear the vehicle number plate input
        edtVehicleModel.text.clear()  // Clear the vehicle model input
        edtVehicleMake.text.clear()  // Clear the vehicle make input
        edtVinNumber.text.clear()  // Clear the VIN number input
        edtVehicleKms.text.clear()  // Clear the vehicle kilometers input
        edtCustomer.text.clear()  // Clear the selected customer input
        imageUris.clear()  // Clear the image URIs
        spnVehiclePOR.setSelection(0)  // Reset the vehicle POR spinner to the first option
        ynpYearPicker.value = ynpYearPicker.minValue  // Reset the year picker to the minimum value
    }

    private fun clearAllImages() {
        frontImageUris.clear()
        rightImageUris.clear()
        rearImageUris.clear()
        leftImageUris.clear()

        displayFront.adapter?.notifyDataSetChanged()
        displayRight.adapter?.notifyDataSetChanged()
        displayRear.adapter?.notifyDataSetChanged()
        displayLeft.adapter?.notifyDataSetChanged()
    }



    private fun uploadVehicleImages(businessId: String, vehicleId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            uploadImagesForSide(businessId, vehicleId, "front", frontImageUris)
            uploadImagesForSide(businessId, vehicleId, "right", rightImageUris)
            uploadImagesForSide(businessId, vehicleId, "rear", rearImageUris)
            uploadImagesForSide(businessId, vehicleId, "left", leftImageUris)
        }
    }


    private fun uploadImagesForSide(businessId: String, vehicleId: String, side: String, imageUris: List<Uri>) {
        if (imageUris.isNotEmpty()) {
            for (uri in imageUris) {
                val uniqueImageId = UUID.randomUUID().toString()
                val storageRef = storage.child("$businessId/Vehicles/$vehicleId/$side/$uniqueImageId.jpg")

                storageRef.putFile(uri)
                    .addOnSuccessListener { taskSnapshot ->
                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            val imageRef = FirebaseDatabase.getInstance().getReference("Users")
                                .child(businessId)
                                .child("Vehicles")
                                .child(vehicleId)
                                .child("images")
                                .child(side)
                                .child(uniqueImageId)

                            imageRef.setValue(downloadUri.toString()).addOnSuccessListener {
                                Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener {
                                Toast.makeText(context, "Failed to save image URL in database", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }




    private fun handleCameraPermission(side: String) {
        currentSide = side // Set the current side for later use
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            captureImageWithCamera(side)
        }
    }


    // Capture image and add it to the respective list
    private fun captureImageWithCamera(side: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            // Set the current side being captured
            currentSide = side

            // Start CameraCaptureActivity instead of the camera intent directly
            val intent = Intent(requireContext(), CameraCaptureActivity::class.java)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

  /*  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    // Handle the captured image returned from CameraCaptureActivity
                    handleCameraImage(data)
                }
            }
        }
    }*/

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    // Get the image URI from CameraCaptureActivity
                    val imageUri = data?.getStringExtra("imageUri")?.let { Uri.parse(it) }
                    imageUri?.let {
                        try {
                            // Load the image as a bitmap
                            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)

                            // Pass the bitmap for further handling (e.g., displaying or saving)
                            handleCameraImage(bitmap)

                            // Optionally save the image back with higher quality
                            val newUri = getImageUriFromBitmap(requireContext(), bitmap)
                            newUri?.let { savedUri ->
                                // Log or display the new saved URI if needed
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }



    private fun getImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri? {
        val resolver = context.contentResolver
        val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        // Create a unique file name by appending the current timestamp
        val imageName = "Title_${System.currentTimeMillis()}.jpg"

        // Configure the file metadata
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/")
        }

        // Insert the new image record into the MediaStore
        val imageUri = resolver.insert(imageCollection, contentValues)

        return try {
            if (imageUri != null) {
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }
            imageUri
        } catch (e: Exception) {
            // Log the error and remove the image entry from MediaStore if insertion fails
            e.printStackTrace()
            imageUri?.let { resolver.delete(it, null, null) }
            null
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                currentSide?.let { captureImageWithCamera(it) } // Use currentSide as the side parameter
            }
        }
    }


    private fun removeImage(uri: Uri, imageList: MutableList<Uri>) {
        imageList.remove(uri)
        // Notify the appropriate adapter based on the list being passed
        when (imageList) {
            frontImageUris -> displayFront.adapter?.notifyDataSetChanged()
            rightImageUris -> displayRight.adapter?.notifyDataSetChanged()
            rearImageUris -> displayRear.adapter?.notifyDataSetChanged()
            leftImageUris -> displayLeft.adapter?.notifyDataSetChanged()
        }
    }


    private fun handleCameraImage(bitmap: Bitmap) {
        val tempUri = getImageUriFromBitmap(requireContext(), bitmap)

        if (tempUri != null) {
            when (currentSide) {
                "front" -> {
                    frontImageUris.add(tempUri)
                    displayFront.adapter?.notifyDataSetChanged()
                }
                "right" -> {
                    rightImageUris.add(tempUri)
                    displayRight.adapter?.notifyDataSetChanged()
                }
                "rear" -> {
                    rearImageUris.add(tempUri)
                    displayRear.adapter?.notifyDataSetChanged()
                }
                "left" -> {
                    leftImageUris.add(tempUri)
                    displayLeft.adapter?.notifyDataSetChanged()
                }
                else -> {
                    Toast.makeText(requireContext(), "Unknown side: $currentSide", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Failed to capture image. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }



   /* private fun handleCameraImage(data: Intent?) {
        val photoBitmap = data?.extras?.get("data") as? Bitmap
        if (photoBitmap == null) {
            Toast.makeText(requireContext(), "Failed to capture image. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        val tempUri = getImageUriFromBitmap(requireContext(), photoBitmap)

        if (tempUri != null) {
            when (currentSide) {
                "front" -> {
                    frontImageUris.add(tempUri)
                    displayFront.adapter?.notifyDataSetChanged()
                }
                "right" -> {
                    rightImageUris.add(tempUri)
                    displayRight.adapter?.notifyDataSetChanged()
                }
                "rear" -> {
                    rearImageUris.add(tempUri)
                    displayRear.adapter?.notifyDataSetChanged()
                }
                "left" -> {
                    leftImageUris.add(tempUri)
                    displayLeft.adapter?.notifyDataSetChanged()
                }
                else -> {
                    Toast.makeText(requireContext(), "Unknown side: $currentSide", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Failed to capture image. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }*/


    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }

}