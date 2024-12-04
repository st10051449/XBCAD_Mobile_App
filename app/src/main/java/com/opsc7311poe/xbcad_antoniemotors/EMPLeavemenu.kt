package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView


class EMPLeavemenu : Fragment() {


    private lateinit var imgReq: ImageView
    private lateinit var imghis: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_e_m_p_leavemenu, container, false)

        // Initialize ImageViews
        imgReq = view.findViewById(R.id.imgRequests)
        imghis = view.findViewById(R.id.imghistory)



        // Set click listeners for the image views
        imgReq.setOnClickListener {
            replaceFragment(EmpLeaveFragment()) // Replace with your actual fragment class
        }

        imghis.setOnClickListener {

            replaceFragment(Employeeleavehis()) // Replace with your actual fragment class


        }



        return view
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }


}