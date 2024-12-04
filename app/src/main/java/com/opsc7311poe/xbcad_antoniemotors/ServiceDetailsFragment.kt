package com.opsc7311poe.xbcad_antoniemotors

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.ktx.database
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class ServiceDetailsFragment : Fragment() {
   private lateinit var btnBack: ImageView
   private lateinit var txtName: TextView
   private lateinit var btnCust: Button
   private lateinit var btnSave: Button
   private lateinit var btnDelete: Button
   private lateinit var imgStatus: ImageView
   private lateinit var imgPayStatus: ImageView
   private lateinit var imgChangeStatus: ImageView
   private lateinit var imgChangePayStatus: ImageView
   private lateinit var txtDateCarReceived: TextView
   private lateinit var txtDateCarReturned: TextView
   private lateinit var txtParts: TextView
   private lateinit var txtLabourCost: TextView
   private lateinit var txtVehicleModel: TextView
   private lateinit var txtVin: TextView
   private lateinit var txtNumPlate: TextView
   private lateinit var businessId: String

   private lateinit var currentStatus: String
   private var currentPaymentStatus: Boolean = false
   private lateinit var fetchedService: ServiceData

    val database = Firebase.database
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var vehicleID: String = ""
    var custID: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_service_details, container, false)

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!

        //back button functionality
        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener(){
            replaceFragment(ServicesFragment())
        }

        //connecting all UI elements
        txtName = view.findViewById(R.id.txtName)
        btnCust = view.findViewById(R.id.btnCustName)
        imgStatus = view.findViewById(R.id.imgStatus)
        imgPayStatus = view.findViewById(R.id.imgPayStatus)
        txtDateCarReceived = view.findViewById(R.id.txtDateCarReceived)
        txtDateCarReturned = view.findViewById(R.id.txtDateCarReturned)
        txtParts = view.findViewById(R.id.txtParts)
        txtLabourCost = view.findViewById(R.id.txtLabourCost)
        txtVehicleModel = view.findViewById(R.id.txtVehicleModel)
        txtVin = view.findViewById(R.id.txtVin)
        txtNumPlate = view.findViewById(R.id.txtNumPlate)

        val serRef = database.getReference("Users/$businessId").child("Services")
        val serviceID = arguments?.getString("serviceID")
        //fetching service data from DB
        if (userId != null && serviceID != null) {

            // Query the database to find the service with the matching ID
            val query = serRef.child(serviceID)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Directly fetch the service object without looping
                    fetchedService = dataSnapshot.getValue(ServiceData::class.java)!!

                    if (fetchedService != null) {
                        // Assign fetched service info to text views
                        txtName.text = fetchedService.name
                        txtLabourCost.text = "${fetchedService.labourCost}"

                        // Convert dates to string values
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        txtDateCarReceived.text = formatter.format(fetchedService.dateReceived)
                        if (fetchedService.dateReturned != null){
                            txtDateCarReturned.text =formatter.format(fetchedService.dateReturned)
                        }

                        // Populate parts textbox
                        var allPartsString = ""
                        fetchedService.parts?.forEach { part ->
                            allPartsString += "${part.name}             R${String.format(Locale.getDefault(), "%.2f", part.cost)}\n"
                        }
                        txtParts.text = allPartsString

                        // Handle status display
                        when (fetchedService.status) {
                            "Completed" -> {
                                imgStatus.setImageResource(R.drawable.vectorstatuscompleted)
                                currentStatus = "Completed"
                            }
                            "Busy" -> {
                                imgStatus.setImageResource(R.drawable.vectorstatusbusy)
                                currentStatus = "Busy"
                            }
                            "Not Started" -> {
                                imgStatus.setImageResource(R.drawable.vectorstatusnotstrarted)
                                currentStatus = "Not Started"
                            }
                        }

                        //Handle payment status display
                        if (fetchedService.paid!!){
                            imgPayStatus.setImageResource(R.drawable.vectorpaid)
                            currentPaymentStatus = true
                        } else {
                            imgPayStatus.setImageResource(R.drawable.vectornotpaid)
                            currentPaymentStatus = false
                        }

                        //populating vehicle info
                        vehicleID = fetchedService.vehicleID!!
                        getVehInfo(vehicleID)

                        //populating cust info
                        custID = fetchedService.custID!!
                        getCustName(custID)

                    } else {
                        Toast.makeText(requireContext(), "Service not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(requireContext(), "Error reading from the database: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        //change status button functionality
        imgChangeStatus = view.findViewById(R.id.imgChangeStatus)
        imgChangeStatus.setOnClickListener(){
            //status changes when button is tapped
            if (currentStatus == "Completed")
            {
                imgStatus.setImageResource(R.drawable.vectorstatusnotstrarted)
                currentStatus = "Not Started"
            }
            else if (currentStatus == "Busy")
            {
                imgStatus.setImageResource(R.drawable.vectorstatuscompleted)
                currentStatus = "Completed"
            }
            else if(currentStatus == "Not Started")
            {
                imgStatus.setImageResource(R.drawable.vectorstatusbusy)
                currentStatus = "Busy"
            }
        }

        imgChangePayStatus = view.findViewById(R.id.imgChangePayStatus)
        imgChangePayStatus.setOnClickListener(){
            //status changes when button is tapped
            if (currentPaymentStatus)
            {
                imgPayStatus.setImageResource(R.drawable.vectornotpaid)
                currentPaymentStatus = false
            }
            else if(!currentPaymentStatus)
            {
                imgPayStatus.setImageResource(R.drawable.vectorpaid)
                currentPaymentStatus = true
            }
        }

        //date pickers functionality
        txtDateCarReceived = view.findViewById(R.id.txtDateCarReceived)
        txtDateCarReturned = view.findViewById(R.id.txtDateCarReturned)

        txtDateCarReceived.setOnClickListener{
            pickDate(txtDateCarReceived)
        }
        txtDateCarReturned.setOnClickListener{
            pickDate(txtDateCarReturned)
        }

        //save button functionality
        btnSave = view.findViewById(R.id.btnSave)
        btnSave.setOnClickListener(){
            lateinit var serviceEntered: ServiceData

            //checking all fields are filled
            if(txtName.text.toString().isBlank() ||
                txtDateCarReceived.text.toString().isBlank() ||
                txtLabourCost.text.toString().isBlank() )
            {
                Toast.makeText( requireContext(), "Please ensure all service information is filled correctly.", Toast.LENGTH_SHORT).show()
            }
            else
            {
                serviceEntered = ServiceData()

                //converting date texts to date values
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dateReceived: Date? = dateFormatter.parse(txtDateCarReceived.text.toString())
                if(txtDateCarReturned.text.toString() != "-"){
                    val dateReturned: Date? = dateFormatter.parse(txtDateCarReturned.text.toString())
                    serviceEntered.dateReturned = dateReturned
                }

                //totalling cost in order to save
                //totalling parts
                var totalCost: Double = 0.0
                for(part in fetchedService.parts!!)
                {
                    totalCost += part.cost!!
                }
                //adding labour cost
                totalCost += txtLabourCost.text.toString().toDouble()

                //making service object
                serviceEntered.name = txtName.text.toString()
                serviceEntered.custID = custID
                serviceEntered.vehicleID = vehicleID
                serviceEntered.status = currentStatus
                serviceEntered.dateReceived = dateReceived
                serviceEntered.parts = fetchedService.parts
                serviceEntered.labourCost = txtLabourCost.text.toString().toDouble()
                serviceEntered.totalCost = totalCost
                serviceEntered.paid = currentPaymentStatus

                //adding service object to db
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null  && serviceID != null)
                {
                    var database = com.google.firebase.ktx.Firebase.database
                    val empRef = database.getReference("Users/$businessId").child("Services").child(serviceID)

                    empRef.setValue(serviceEntered)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Service successfully updated", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "An error occurred while adding a updated:" + it.toString() , Toast.LENGTH_LONG).show()
                        }
                }


                //go back to service landing page
                it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                replaceFragment(ServicesFragment())
            }
        }

        //delete button functionality
        btnDelete = view.findViewById(R.id.btnDelete)
        btnDelete.setOnClickListener(){

            //making alert dialog to check if user is sure they want to delete service
            val dialogConfirm = AlertDialog.Builder(requireContext())
            dialogConfirm.setTitle("Confirm Delete")
            dialogConfirm.setMessage("Are you sure you want to permanently delete this service.")

            //if user taps yes
            dialogConfirm.setPositiveButton("Yes") { dialog, _ ->
                //deleting service
                val database = Firebase.database
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                val serRef = database.getReference("Users/$businessId").child("Services")

                serRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //deleting service
                        serRef.child(serviceID!!).removeValue()
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Service Successfully Removed", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to Remove Service", Toast.LENGTH_SHORT).show()
                            }

                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(requireContext(), "Error reading from the database: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                    }
                })
                dialog.dismiss()
                //returning to services overview page
                replaceFragment(ServicesFragment())
            }

            //if user taps no
            dialogConfirm.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

            val alert = dialogConfirm.create()
            alert.show()
        }

        return view
    }

    private fun getCustName(custID: String){
        val vehRef = database.getReference("Users/$businessId").child("Customers/$custID")

        vehRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //fetching and assigning vehicle info
                var custFName = dataSnapshot.child("CustomerName").getValue(String::class.java)
                var custSurname = dataSnapshot.child("CustomerSurname").getValue(String::class.java)
                btnCust.text = "$custFName $custSurname"
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Error reading from the database: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getVehInfo(vehID: String) {
        val vehRef = database.getReference("Users/$businessId").child("Vehicles/$vehID")

        vehRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //fetching and assigning vehicle info
                txtVehicleModel.text = dataSnapshot.child("vehicleModel").getValue(String::class.java)
                txtNumPlate.text = dataSnapshot.child("vehicleNumPlate").getValue(String::class.java)
                txtVin.text = dataSnapshot.child("vinNumber").getValue(String::class.java)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Error reading from the database: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
}