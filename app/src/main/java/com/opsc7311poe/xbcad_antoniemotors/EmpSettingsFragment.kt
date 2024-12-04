package com.opsc7311poe.xbcad_antoniemotors

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

class EmpSettingsFragment : Fragment() {
    private lateinit var btnChangePass: TextView
    private lateinit var btnDelAcc: TextView
    private lateinit var btnLogout: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnLeaderboards: TextView


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
        val view = inflater.inflate(R.layout.fragment_emp_settings, container, false)

        btnChangePass = view.findViewById(R.id.txtChangePassword)
        btnDelAcc = view.findViewById(R.id.txtDeleteAccount)
        btnLogout = view.findViewById(R.id.txtLogout)
        btnLeaderboards = view.findViewById(R.id.txtLeaderboardOption)


        btnChangePass.setOnClickListener {
            // Replace the current fragment with CustomerFragment
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(EmployeeChangePasswordFragment())
        }

        btnDelAcc.setOnClickListener {
            // Replace the current fragment with CustomerFragment
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(EmpDeleteAccountFragment())
        }

        btnLogout.setOnClickListener {
            // Replace the current fragment with CustomerFragment
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(EmployeeLogoutFragment())
        }

        btnLeaderboards.setOnClickListener(){
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(LeaderboardOptInFragment())
        }

        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener(){
            replaceFragment(HomeFragment())
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