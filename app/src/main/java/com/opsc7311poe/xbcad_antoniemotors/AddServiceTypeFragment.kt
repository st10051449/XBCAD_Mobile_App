package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Locale

class AddServiceTypeFragment : Fragment() {

    private lateinit var btnBack: ImageView
    private lateinit var btnAdd: Button
    private lateinit var btnAddPart: Button
    private lateinit var txtPartName: EditText
    private lateinit var txtPartCost: EditText
    private lateinit var txtAllParts: TextView
    private lateinit var txtName: TextView
    private lateinit var txtLabourCost: TextView

    private var partsEntered: MutableList<Part> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_add_service_type, container, false)

        val businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)

        //back btn functionality
        //handling back button
        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener() {
            replaceFragment(ServicesFragment())
        }

        //functionality for adding a part
        btnAddPart = view.findViewById(R.id.btnAddPart)

        btnAddPart.setOnClickListener{

            //adding part to list of parts
            txtPartName = view.findViewById(R.id.txtPartName)
            txtPartCost = view.findViewById(R.id.txtPartCost)
            txtAllParts = view.findViewById(R.id.txtAllParts)

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

        //save button functionality
        btnAdd = view.findViewById(R.id.btnAdd)

        btnAdd.setOnClickListener() {
            lateinit var serviceTypeEntered: ServiceTypeData

            txtName = view.findViewById(R.id.txtServiceName)
            txtLabourCost = view.findViewById(R.id.txtLabourCost)

            //checking all fields are filled
            if(txtName.text.toString().isBlank() ||
                txtAllParts.text.toString().isBlank() ||
                txtLabourCost.text.toString().isBlank() )
            {
                Toast.makeText( requireContext(), "Please ensure all service information is filled correctly.", Toast.LENGTH_SHORT).show()
            }
            else{
                //making service type object
                serviceTypeEntered = ServiceTypeData( txtName.text.toString(), partsEntered, txtLabourCost.text.toString().toDouble())

                //adding to DB
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null)
                {
                    val database = Firebase.database


                    val empRef = database.getReference("Users/$businessId").child("ServiceTypes")

                    empRef.push().setValue(serviceTypeEntered)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Service Type successfully added", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "An error occurred while adding a service type:" + it.toString() , Toast.LENGTH_LONG).show()
                        }
                }

                //go back to service types page
                it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                replaceFragment(AddServiceFragment())
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
}