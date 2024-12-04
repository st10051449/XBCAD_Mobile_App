package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

class SettingsFragment : Fragment() {

    private lateinit var btnChangePass: TextView
    private lateinit var btnDelAcc: TextView
    private lateinit var btnLogout: TextView

    private lateinit var btnBack: ImageView

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
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

       btnChangePass = view.findViewById(R.id.txtChangePassword)
       btnDelAcc = view.findViewById(R.id.txtDeleteAccount)
       btnLogout = view.findViewById(R.id.txtLogout)

        btnChangePass.setOnClickListener {
            // Replace the current fragment with CustomerFragment
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(ChnagepasswordFragment())
        }

        btnDelAcc.setOnClickListener {
            // Replace the current fragment with CustomerFragment
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(deleteAccFragment())
        }

        btnLogout.setOnClickListener {
            // Replace the current fragment with CustomerFragment
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(LogoutFragment())
        }

        btnBack = view.findViewById(R.id.ivBackButton)

        btnBack.setOnClickListener(){
            replaceFragment(HomeFragment())
        }

        return view
    }
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }

}