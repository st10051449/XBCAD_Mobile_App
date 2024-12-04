package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale

class ServicesFragment : Fragment() {

    private lateinit var imgPlus: ImageView
    private lateinit var imgFilter: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var linLay: LinearLayout
    private lateinit var svServices: SearchView
    private var listOfAllServices = mutableListOf<ServiceData>()
    private lateinit var businessId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_services, container, false)

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!

        //handling back button
        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener() {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(VehicleMenuFragment())
        }

        imgPlus = view.findViewById(R.id.imgPlus)

        imgPlus.setOnClickListener(){
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(AddServiceFragment())
        }

        //loading services in database
        linLay = view.findViewById(R.id.linlayServiceCards)
        loadServices()

        //handling search functionality

        // Listen to SearchView input
        svServices = view.findViewById(R.id.svServices)
        svServices.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchList(newText)
                return true
            }
        })

        //filter functionality
        imgFilter = view.findViewById(R.id.imgFilter)

        imgFilter.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            // Show a dialog with filter options
            val filterOptions = arrayOf("Not Started", "Busy", "Completed", "Paid", "Unpaid", "View All Services")
            AlertDialog.Builder(requireContext())
                .setTitle("Filter Tasks By")
                .setItems(filterOptions) { _, which ->
                    when (which) {
                        0 -> filterByStatus("Not Started")
                        1 -> filterByStatus("Busy")
                        2 -> filterByStatus("Completed")
                        3 -> filterByPayStatus(true)
                        4 -> filterByPayStatus(false)
                        5 -> replaceFragment(ServicesFragment())
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        return view
    }

    private fun loadServices() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val database = Firebase.database
            val servicesRef = database.getReference("Users/$businessId").child("Services")

            servicesRef.addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    linLay.removeAllViews()

                    for (pulledOrder in snapshot.children) {

                        val service = pulledOrder.getValue(ServiceData::class.java)
                        service!!.serviceID = pulledOrder.key
                        //adding service to list to filter later
                        listOfAllServices.add(service);

                        val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.card_service, linLay, false) as CardView
                        // Populate the card with service data
                        //populate the customer data
                        fetchCustNameAndSurname(service!!.custID!!, cardView)
                        //populate vehicle data
                        fetchVehicleNameAndModel(service.vehicleID!!, cardView)

                        cardView.findViewById<TextView>(R.id.txtServiceName).text = service.name
                        cardView.findViewById<TextView>(R.id.txtCost).text = "R ${service.totalCost.toString()}"

                        //changing date values to string
                        val formatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

                        cardView.findViewById<TextView>(R.id.txtDateTakenIn).text = formatter.format(service?.dateReceived)
                        if (service.dateReturned != null){
                            cardView.findViewById<TextView>(R.id.txtDateGivenBack).text =
                                formatter.format(service.dateReturned)
                        }
                        // Set the status ImageView based on status
                        val statusImageView = cardView.findViewById<ImageView>(R.id.imgStatus)
                        when (service.status) {
                            "Completed" -> statusImageView.setImageResource(R.drawable.vectorstatuscompleted)
                            "Busy" -> statusImageView.setImageResource(R.drawable.vectorstatusbusy)
                            "Not Started" -> statusImageView.setImageResource(R.drawable.vectorstatusnotstrarted)
                        }

                        // Set the payment status ImageView based on payment status
                        val payStatusImageView = cardView.findViewById<ImageView>(R.id.imgPayStatus)
                        if(service.paid!!) {
                            payStatusImageView.setImageResource(R.drawable.vectorpaid)
                        }

                        //loading progress
                        loadProgressBar(service.serviceID,cardView)

                        //functionality to go details page when card is tapped
                        cardView.setOnClickListener {

                            val serviceInfoFragment = ServiceDetailsFragment()
                            //transferring service info using a bundle
                            val bundle = Bundle()
                            bundle.putString("serviceID", pulledOrder.key)
                            serviceInfoFragment.arguments = bundle
                            //changing to service info fragment
                            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            replaceFragment(serviceInfoFragment)
                        }




                        // Add the card to the container
                        linLay.addView(cardView)

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error here
                }
            })
        }
    }

    private fun loadServicesFromList(inputList: MutableList<ServiceData>) {

        linLay.removeAllViews()

        for (service in inputList) {

            val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.card_service, linLay, false) as CardView
            // Populate the card with service data
            //populate the customer data
            fetchCustNameAndSurname(service.custID!!, cardView)
            //populate vehicle data
            fetchVehicleNameAndModel(service.vehicleID!!, cardView)

            cardView.findViewById<TextView>(R.id.txtServiceName).text = service.name
            cardView.findViewById<TextView>(R.id.txtCost).text = "R ${service.totalCost.toString()}"

            //changing date values to string
            val formatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

            cardView.findViewById<TextView>(R.id.txtDateTakenIn).text = formatter.format(service?.dateReceived)
            if (service.dateReturned != null){
                cardView.findViewById<TextView>(R.id.txtDateGivenBack).text =
                    formatter.format(service.dateReturned)
            }
            // Set the status ImageView based on status
            val statusImageView = cardView.findViewById<ImageView>(R.id.imgStatus)
            when (service.status) {
                "Completed" -> statusImageView.setImageResource(R.drawable.vectorstatuscompleted)
                "Busy" -> statusImageView.setImageResource(R.drawable.vectorstatusbusy)
                "Not Started" -> statusImageView.setImageResource(R.drawable.vectorstatusnotstrarted)
            }

            val payStatusImageView = cardView.findViewById<ImageView>(R.id.imgPayStatus)
            if(service.paid!!) {
                payStatusImageView.setImageResource(R.drawable.vectorpaid)
            }

            //loading progress
            loadProgressBar(service.serviceID,cardView)

            //functionality to go details page when card is tapped
            /*cardView.setOnClickListener {

                val serviceInfoFragment = ServiceDetailsFragment()
                //transferring service info using a bundle
                val bundle = Bundle()
                bundle.putString("serviceID", pulledOrder.key)
                serviceInfoFragment.arguments = bundle
                //changing to service info fragment
                it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                replaceFragment(serviceInfoFragment)
            }*/


            // Add the card to the container
            linLay.addView(cardView)

        }

    }

    private fun loadProgressBar(serviceID: String?, cardView: CardView) {
        if (serviceID == null) {
            return
        }

        val progressBar = cardView.findViewById<ProgressBar>(R.id.progBarService)

        // Reference to the Firebase database for EmployeeTasks
        val taskRef = Firebase.database.reference.child("Users/$businessId/EmployeeTasks")

        taskRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    progressBar.progress = 0 // Set progress to 0 if no tasks
                    return
                }

                var totalTasks = 0
                var completedTasks = 0

                // Iterate through tasks and check if they belong to the service
                for (taskSnapshot in snapshot.children) {
                    val taskServiceID = taskSnapshot.child("serviceID").getValue(String::class.java)
                    val taskStatus = taskSnapshot.child("status").getValue(String::class.java)

                    if (taskServiceID == serviceID) {
                        totalTasks++ // Increment total tasks for the service
                        if (taskStatus == "Completed") {
                            completedTasks++ // Increment completed tasks
                        }
                    }

                }

                if (totalTasks > 0) {
                    val progress = (completedTasks.toFloat() / totalTasks.toFloat() * 100).toInt()
                    progressBar.progress = progress // Set the progress bar value
                } else {
                    progressBar.progress = 0 // No tasks, set progress to 0
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun searchList(query: String?) {
        var tempList = mutableListOf<ServiceData>()


        if (query.isNullOrEmpty()) {
            tempList.addAll(listOfAllServices)
        } else {
            val filter = query.lowercase()
            tempList.addAll(listOfAllServices.filter {
                it.name!!.lowercase().contains(filter)
            })
        }
        loadServicesFromList(tempList)
    }

    private fun filterByStatus(statusEntered: String) {
        var tempList = mutableListOf<ServiceData>()

       tempList.addAll(listOfAllServices.filter {
           it.status!!.equals(statusEntered)
       })

        loadServicesFromList(tempList)
    }
    private fun filterByPayStatus(payStatusEntered: Boolean) {
        var tempList = mutableListOf<ServiceData>()

       tempList.addAll(listOfAllServices.filter {
           it.paid!! == payStatusEntered
       })

        loadServicesFromList(tempList)
    }

    private fun fetchCustNameAndSurname(custID: String, cv: CardView )
    {
        //fetching name and surname of cust using cust ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val database = Firebase.database
            val custRef = database.getReference("Users/$businessId").child("Customers")

            custRef.addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (customerSnapshot in snapshot.children) {
                        if(customerSnapshot.key == custID){
                            val custName = customerSnapshot.child("CustomerName").getValue(String::class.java)
                            val custSurname = customerSnapshot.child("CustomerSurname").getValue(String::class.java)
                            cv.findViewById<TextView>(R.id.txtCustName).text = "$custName $custSurname"
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error here
                }
            })
        }
    }

    private fun fetchVehicleNameAndModel(vehID: String, cv: CardView){
        //fetching name and surname of cust using cust ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val database = Firebase.database
            val vehRef = database.getReference("Users/$businessId").child("Vehicles")

            vehRef.addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (vehSnapshot in snapshot.children) {
                        if(vehSnapshot.key == vehID){
                            val vehNumPlate = vehSnapshot.child("vehicleNumPlate").getValue(String::class.java)
                            val vehicleModel = vehSnapshot.child("vehicleModel").getValue(String::class.java)
                            val vehicleMake = vehSnapshot.child("vehicleMake").getValue(String::class.java)
                            cv.findViewById<TextView>(R.id.txtVehicleName).text = "$vehNumPlate ($vehicleMake $vehicleModel)"
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error here
                }
            })
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}