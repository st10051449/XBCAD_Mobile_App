package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView


class AdminEmpFragment : Fragment() {

    private lateinit var imgLeaveman: ImageView
    private lateinit var imgSearchAndReg: ImageView
    private lateinit var imgTask: ImageView
    private lateinit var imganalytic: ImageView
    private lateinit var imgapprovereg : ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_admin_emp, container, false)

        // Initialize ImageViews
        imgLeaveman = view.findViewById(R.id.imgleavemang)
        imgSearchAndReg = view.findViewById(R.id.imgsearchandreg)
        imgTask = view.findViewById(R.id.imgtask)
        imganalytic = view.findViewById(R.id.imganalytics)
        imgapprovereg= view.findViewById(R.id.imgadminapprove)


        // Set click listeners for the image views

        imgLeaveman.setOnClickListener {
            replaceFragment(AdminLeaveMenuFragment()) // Replace with your actual fragment class
        }

        imgSearchAndReg.setOnClickListener {
            replaceFragment(EmployeeFragment()) // Replace with your actual fragment class
        }

        imgTask.setOnClickListener {
            replaceFragment(AdminTasksMenuFragment())
        }



        //add fragment
        imganalytic.setOnClickListener {
            replaceFragment(AdminAnalyticsFragment()) // Replace with your actual fragment class
        }

        imgapprovereg.setOnClickListener {
            replaceFragment(AdminApproveRegistrationFragment()) // Replace with your actual fragment class
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
