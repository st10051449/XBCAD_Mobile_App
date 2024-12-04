package com.opsc7311poe.xbcad_antoniemotors

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Locale


class ManageServiceTypesFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var imgPlus: ImageView
    private lateinit var linLay: LinearLayout
    private lateinit var businessId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_manage_service_types, container, false)

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!

        //handle back btn functionality
        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener(){
            replaceFragment(AddServiceFragment())
        }

        //handle plus button functionality
        imgPlus = view.findViewById(R.id.imgPlus)

        imgPlus.setOnClickListener(){
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(AddServiceTypeFragment())
        }

        //loading in service types
        linLay = view.findViewById(R.id.linlayServiceTypeCards)
        loadServiceTypes()


        return view
    }

    private fun loadServiceTypes() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val database = Firebase.database
            val servicesRef = database.getReference("Users/$businessId").child("ServiceTypes")

            servicesRef.addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    linLay.removeAllViews()

                    for (pulledOrder in snapshot.children) {

                        val serviceType = pulledOrder.getValue(ServiceData::class.java)

                        val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.card_service_type, linLay, false) as CardView
                        // Populate the card with service type data
                        cardView.findViewById<TextView>(R.id.txtServiceName).text = serviceType!!.name
                        cardView.findViewById<TextView>(R.id.txtLabourCost).text = "R ${serviceType.labourCost.toString()}"

                        // Populate parts textbox
                        var allPartsString = ""
                        serviceType.parts?.forEach { part ->
                            allPartsString += "${part.name}             R${String.format(Locale.getDefault(), "%.2f", part.cost)}\n"
                        }
                        cardView.findViewById<TextView>(R.id.txtParts).text = allPartsString


                        //delete button functionality
                        cardView.findViewById<TextView>(R.id.btnDelete).setOnClickListener {

                            val serviceTypeID = pulledOrder.key

                            deleteServiceType(serviceTypeID!!)

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

    private fun deleteServiceType(serviceTypeID: String) {
        //making alert dialog to check if user is sure they want to delete service
        val dialogConfirm = AlertDialog.Builder(requireContext())
        dialogConfirm.setTitle("Confirm Delete")
        dialogConfirm.setMessage("Are you sure you want to permanently delete this service type.")

        //if user taps yes
        dialogConfirm.setPositiveButton("Yes") { dialog, _ ->
            //deleting service
            val database = com.google.firebase.Firebase.database
            val userId = FirebaseAuth.getInstance().currentUser?.uid


            val serRef = database.getReference("Users/$businessId").child("ServiceTypes")

            serRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //deleting service
                    serRef.child(serviceTypeID).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Service Type Successfully Removed", Toast.LENGTH_SHORT).show()
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
            replaceFragment(AddServiceFragment())
        }

        //if user taps no
        dialogConfirm.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        val alert = dialogConfirm.create()
        alert.show()
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }


}