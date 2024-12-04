package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

class AdminTasksMenuFragment : Fragment() {

    private lateinit var imgbtnassigntask: ImageView
    private lateinit var imgbtnchecktask: ImageView
    private lateinit var btnBack: ImageView

    //private lateinit var imgbtnApprovetask: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_admin_tasks_menu, container, false)

        btnBack = view.findViewById(R.id.ivBackButton)
        //handling naviagtion for each button
        imgbtnassigntask = view.findViewById(R.id.imgbtnassigntask)
        imgbtnassigntask.setOnClickListener {
            replaceFragment(AssignEmployeeTask())
        }

        imgbtnchecktask = view.findViewById(R.id.imgbtnchecktask)
        imgbtnchecktask.setOnClickListener {
            replaceFragment(CheckTaskStatus())
        }

        btnBack.setOnClickListener {
            replaceFragment(AdminEmpFragment())
        }

        /*imgbtnApprovetask = view.findViewById(R.id.imgbtnApprovetask)
        imgbtnApprovetask.setOnClickListener {
            replaceFragment(AdminEmpFragment())
        }*/

        return view
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }

}