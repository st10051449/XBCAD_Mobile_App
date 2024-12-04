package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment

class VehicleMenuFragment : Fragment() {

    private lateinit var btnVehicleReg: ImageView
    private lateinit var btnAddVehicleMakes: ImageView
    private lateinit var btnPartsInventory: ImageView
    private lateinit var btnServices: ImageView
    private lateinit var btnManageServiceTypes: ImageView
    private lateinit var btnVAnalytics: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vehicle_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views here, where view is guaranteed to be non-null
        btnVehicleReg = view.findViewById(R.id.btnRegVehicle)
        btnAddVehicleMakes = view.findViewById(R.id.btnAddVehicleMake)
        btnServices = view.findViewById(R.id.btnServices)
        btnManageServiceTypes = view.findViewById(R.id.btnManageServiceTypes)
        btnPartsInventory = view.findViewById(R.id.btnManageInventory) // Updated ID
        btnVAnalytics = view.findViewById(R.id.btnVehicleAnalytics)

        // Set up button click listeners or other view logic here
        btnVehicleReg.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(SearchVehiclesFragment())
            //replaceFragment(RegisterVehicleFragment())
        }

        btnAddVehicleMakes.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(AddVehicleMakeModelPOR())
        }

        btnPartsInventory.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(ViewInventoryFragment())
        }

        btnServices.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(ServicesFragment())
        }

        btnManageServiceTypes.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(ManageServiceTypesFragment())
        }

        btnVAnalytics.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(VehicleAnalyticsFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
