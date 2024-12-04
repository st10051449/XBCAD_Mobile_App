package com.opsc7311poe.xbcad_antoniemotors

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment

class CustomerMenuFragment : Fragment() {

    private lateinit var btnViewCustomers: ImageView
    private lateinit var btnCAnalytics: ImageView
    private lateinit var btnRegNewCust: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_customer_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnCAnalytics = view.findViewById(R.id.btnCustomerAnalytics)
        btnViewCustomers = view.findViewById(R.id.btnCustomers)
        btnRegNewCust = view.findViewById(R.id.btnAddCustomer)

        // Set up button click listeners or other view logic here
        btnCAnalytics.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(CustomerAnalyticsFragment())

        }

        btnViewCustomers.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            replaceFragment(CustomerFragment())
        }


        //this is where we'll have to add the link to the web page when the website is hosted officially
        btnRegNewCust.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://antoniemotors-aqaxebguhdaeesfj.southafricanorth-01.azurewebsites.net")
            context?.startActivity(intent)
        }

        /*https://antoniemotors-aqaxebguhdaeesfj.southafricanorth-01.azurewebsites.net*/

    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
