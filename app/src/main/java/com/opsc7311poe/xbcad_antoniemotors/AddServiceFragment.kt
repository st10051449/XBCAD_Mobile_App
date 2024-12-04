package com.opsc7311poe.xbcad_antoniemotors

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
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


class AddServiceFragment : Fragment() {
    private lateinit var spinStatus: Spinner
    private lateinit var btnBack: ImageView
    private lateinit var spinCust: Spinner
    private lateinit var spinVeh: Spinner
    private lateinit var spinServiceType: Spinner
    private lateinit var txtName: TextView
    private lateinit var txtDateReceived: TextView
    private lateinit var txtDateReturned: TextView
    private lateinit var txtAllParts: TextView
    private lateinit var txtPartName: EditText
    private lateinit var txtPartCost: EditText
    private lateinit var txtLabourCost: TextView
    private lateinit var btnAddPart: Button
    private lateinit var btnAdd: Button
    private lateinit var btnManageServiceTypes: Button
    private lateinit var businessId: String

    //list of parts entered
    private var partsEntered: MutableList<Part> = mutableListOf()
    private var selectedCustomerId: String = ""
    private var selectedVehicleId: String = ""
    private var selectedServiceTypeId: String = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_add_service, container, false)
        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!

        txtName = view.findViewById(R.id.txtServiceName)
        txtLabourCost = view.findViewById(R.id.txtLabourCost)
        txtPartName = view.findViewById(R.id.txtPartName)
        txtPartCost = view.findViewById(R.id.txtPartCost)
        txtAllParts = view.findViewById(R.id.txtAllParts)

        //handling back button
        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener() {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(ServicesFragment())
        }

        //handling manage service types button
        btnManageServiceTypes = view.findViewById(R.id.btnManageServiceTypes)

        btnManageServiceTypes.setOnClickListener() {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(ManageServiceTypesFragment())
        }


        //populating spinners
        //status spinner
        spinStatus = view.findViewById(R.id.spinStatus)

        val statuses = arrayOf("Not Started", "Busy", "Completed")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinStatus.adapter = adapter

        //customer spinner
        spinCust = view.findViewById(R.id.spinCustomer)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { populateCustomerSpinner() }

        //vehicle spinner
        spinVeh = view.findViewById(R.id.spinVeh)

        userId?.let { populateVehicleSpinner(selectedCustomerId) }

        //Service type spinner
        spinServiceType = view.findViewById(R.id.spinDefaultServiceType)

        userId?.let { populateServiceTypeSpinner() }


        //date picking functionality to date-based textviews
        txtDateReceived = view.findViewById(R.id.txtDateCarReceived)
        txtDateReturned = view.findViewById(R.id.txtDateCarReturned)

        txtDateReceived.setOnClickListener{
            pickDate(txtDateReceived)
        }
        txtDateReturned.setOnClickListener{
            pickDate(txtDateReturned)
        }

        //functionality to add a part
        btnAddPart = view.findViewById(R.id.btnAddPart)

        btnAddPart.setOnClickListener{

            //adding part to list of parts

            //checking all fields are filled
            if(txtPartName.text.toString().isBlank() || txtPartCost.text.toString().isBlank())
            {
                Toast.makeText( requireContext(), "Please enter part name and cost.", Toast.LENGTH_SHORT).show()
            }
            else
            {
                partsEntered.add(Part(txtPartName.text.toString(), txtPartCost.text.toString().toDouble()))

                //displaying updated list to user
                var allPartsString = ""
                for(part in partsEntered)
                {
                    allPartsString += "${part.name}             R${String.format(Locale.getDefault(), "%.2f", part.cost)}"
                    allPartsString += "\n"
                }

                txtAllParts.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                txtAllParts.text = allPartsString

                //erasing content just entered in txt fields
                txtPartName.text.clear()
                txtPartCost.text.clear()
            }

        }

        //adding the service to the DB when add button is clicked
        btnAdd = view.findViewById(R.id.btnAdd)

        btnAdd.setOnClickListener{
            lateinit var serviceEntered: ServiceData

            //checking all fields are filled
            if(txtName.text.toString().isBlank() ||
                txtDateReceived.text.toString().isBlank() ||
                txtAllParts.text.toString().isBlank() ||
                txtLabourCost.text.toString().isBlank() )
            {
                Toast.makeText( requireContext(), "Please ensure all service information is filled correctly.", Toast.LENGTH_SHORT).show()
            }
            else
            {
                serviceEntered = ServiceData()

                //converting date texts to date values
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dateReceived: Date? = dateFormatter.parse(txtDateReceived.text.toString())
                if(!txtDateReturned.text.toString().isBlank()){
                val dateReturned: Date? = dateFormatter.parse(txtDateReturned.text.toString())
                    serviceEntered.dateReturned = dateReturned
                }

                //totalling cost in order to save
                //totalling parts
                var totalCost = 0.0
                for(part in partsEntered)
                {
                    totalCost += part.cost!!
                }
                //adding labour cost
                totalCost += txtLabourCost.text.toString().toDouble()

                //making service object

                serviceEntered.name = txtName.text.toString()
                serviceEntered.custID = selectedCustomerId
                serviceEntered.vehicleID = selectedVehicleId
                serviceEntered.status = spinStatus.selectedItem.toString()
                serviceEntered.dateReceived = dateReceived
                serviceEntered.parts = partsEntered
                serviceEntered.labourCost = txtLabourCost.text.toString().toDouble()
                serviceEntered.totalCost = totalCost
                serviceEntered.paid = false


                //adding service object to db
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null)
                {
                    val database = Firebase.database
                    val empRef = database.getReference("Users/$businessId").child("Services")

                    empRef.push().setValue(serviceEntered)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Service successfully added", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "An error occurred while adding a service:" + it.toString() , Toast.LENGTH_LONG).show()
                        }
                }


                //go back to service landing page
                it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                replaceFragment(ServicesFragment())
            }
        }

        return view


    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    //method for picking dates
    fun pickDate(edittxt: TextView)
    {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val datePickDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->

                val formattedDay = String.format("%02d", selectedDay)
                val formattedMonth = String.format("%02d", selectedMonth + 1)

                val selectedDate = "$formattedDay/${formattedMonth}/$selectedYear"
                edittxt.setText(selectedDate)
            }, year, month, day
        )

        datePickDialog.show()
    }

    private fun populateCustomerSpinner() {
        // Get the current logged-in user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Query the database for customers directly under the current user's ID
        val customerRef = FirebaseDatabase.getInstance().getReference("Users/$businessId").child("Customers")

        customerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customerMap = mutableMapOf<String, String>()
                val customerNames = mutableListOf<String>()

                // Check if there are customers associated with the user
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "No customers found for this user", Toast.LENGTH_SHORT).show()
                    return
                }

                // Loop through all customers associated with this user
                for (customerSnapshot in snapshot.children) {
                    val customerId = customerSnapshot.key // Get customer ID
                    val firstName = customerSnapshot.child("CustomerName").getValue(String::class.java)
                    val lastName = customerSnapshot.child("CustomerSurname").getValue(String::class.java)

                    // Log data to check if it's being fetched correctly


                    // Only add customer if names are not null or empty
                    if (!firstName.isNullOrEmpty() && !lastName.isNullOrEmpty() && customerId != null) {
                        val fullName = "$firstName $lastName"
                        customerMap[customerId] = fullName
                        customerNames.add(fullName) // Add full name to spinner options
                    }
                }

                // Check if the list is empty
                if (customerNames.isEmpty()) {
                    Toast.makeText(requireContext(), "No customers found", Toast.LENGTH_SHORT).show()
                    return
                }

                // Set up the spinner with the list of customer names
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, customerNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinCust.adapter = adapter

                // Handle customer selection from the spinner
                spinCust.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        val selectedCustomerName = parent.getItemAtPosition(position).toString()
                        selectedCustomerId = customerMap.filterValues { it == selectedCustomerName }.keys.firstOrNull().orEmpty()
                        //populate vehicle spinner when customer is selected
                        populateVehicleSpinner(selectedCustomerId)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        // Handle case where nothing is selected if necessary
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

                Toast.makeText(requireContext(), "Error loading customer data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun populateVehicleSpinner(selectedCust: String) {
        // Get the current logged-in user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Query the database for vehicles directly under the current user's ID
        val vehRef = FirebaseDatabase.getInstance().getReference("Users/$businessId").child("Vehicles")

        vehRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val vehicleMap = mutableMapOf<String, String>()
                val vehicleNames = mutableListOf<String>()

                // Check if there are vehicles associated with the user
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "No vehicles found for this user", Toast.LENGTH_SHORT).show()
                    return
                }

                // Loop through all vehicles
                for (vehSnapshot in snapshot.children) {
                    val foundCustomerId = vehSnapshot.child("customerID").getValue(String::class.java)
                    val vehNumPlate = vehSnapshot.child("vehicleNumPlate").getValue(String::class.java)
                    val vehModel = vehSnapshot.child("vehicleModel").getValue(String::class.java)

                    // Check if the vehicle belongs to the desired customer

                    if (foundCustomerId == selectedCust && !vehNumPlate.isNullOrEmpty() && !vehModel.isNullOrEmpty()) {
                        val vehicleDisplay = "$vehNumPlate ($vehModel)"

                        // Use vehSnapshot.key as the unique ID for the vehicle
                        val vehicleID = vehSnapshot.key ?: ""
                        vehicleMap[vehicleID] = vehicleDisplay
                        vehicleNames.add(vehicleDisplay)
                    }
                }

                // Check if the list is empty
                if (vehicleNames.isEmpty()) {
                    Toast.makeText(requireContext(), "No vehicles found for this customer", Toast.LENGTH_SHORT).show()
                    return
                }

                // Set up the spinner with the list of vehicle names
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, vehicleNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinVeh.adapter = adapter

                // Handle vehicle selection from the spinner
                spinVeh.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        val selectedVehicleName = parent.getItemAtPosition(position).toString()

                        // Get the selected vehicle ID based on the vehicle name
                        selectedVehicleId = vehicleMap.filterValues { it == selectedVehicleName }.keys.firstOrNull().orEmpty()


                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        // Handle case where nothing is selected if necessary
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error here
            }
        })
    }

    private fun populateServiceTypeSpinner() {
        // Get the current logged-in user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Query the database for service types under the current user's ID
        val serviceTypeRef = FirebaseDatabase.getInstance().getReference("Users/$businessId").child("ServiceTypes")

        serviceTypeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val serviceTypeMap = mutableMapOf<String, String>()
                val serviceTypeNames = mutableListOf("Select a service type") // Add placeholder at the first position

                // Check if there are service types associated with the user
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "No service types found for this user", Toast.LENGTH_SHORT).show()
                    return
                }

                // Loop through all service types associated with this user
                for (serviceTypeSnapshot in snapshot.children) {
                    val serviceTypeId = serviceTypeSnapshot.key
                    val serviceTypeName = serviceTypeSnapshot.child("name").getValue(String::class.java)



                    // Only add service type if names are not null or empty
                    if (!serviceTypeName.isNullOrEmpty() && serviceTypeId != null) {
                        serviceTypeMap[serviceTypeId] = serviceTypeName
                        serviceTypeNames.add(serviceTypeName) // Add service type name to spinner options
                    }
                }

                // Check if the list is empty after fetching service types
                if (serviceTypeNames.size == 1) { // Only placeholder exists
                    Toast.makeText(requireContext(), "No service types found", Toast.LENGTH_SHORT).show()
                    return
                }

                // Set up the spinner with the list of service type names
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, serviceTypeNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinServiceType.adapter = adapter

                // Set spinner to show the placeholder by default
                spinServiceType.setSelection(0)

                // Handle service type selection from the spinner
                spinServiceType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        if (position != 0) {
                            // Ignore the first item (placeholder) and proceed with service type selection
                            val selectedServiceTypeName = parent.getItemAtPosition(position).toString()
                            selectedServiceTypeId = serviceTypeMap.filterValues { it == selectedServiceTypeName }.keys.firstOrNull().orEmpty()
                            // Populate other fields when a service type is selected
                            populateFieldsWithServiceTypeData(selectedServiceTypeId)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        // Handle if nothing is selected
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

                Toast.makeText(requireContext(), "Error loading service type data", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun populateFieldsWithServiceTypeData(selectedServiceTypeId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val serviceTypeRef = FirebaseDatabase.getInstance().getReference("Users/$businessId").child("ServiceTypes/$selectedServiceTypeId")

        serviceTypeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val fetchedSerType = snapshot.getValue(ServiceTypeData::class.java)

                txtName.text = fetchedSerType!!.name

                //displaying part list to user
                var allPartsString = ""
                for(part in fetchedSerType.parts!!)
                {
                    allPartsString += "${part.name}             R${String.format(Locale.getDefault(), "%.2f", part.cost)}"
                    allPartsString += "\n"
                }

                partsEntered = fetchedSerType.parts!!.toMutableList()

                txtAllParts.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                txtAllParts.text = allPartsString

                txtLabourCost.text = fetchedSerType.labourCost.toString()

            }
            override fun onCancelled(error: DatabaseError) {

                Toast.makeText(requireContext(), "Error loading service type data", Toast.LENGTH_SHORT).show()
            }
        })

    }


}