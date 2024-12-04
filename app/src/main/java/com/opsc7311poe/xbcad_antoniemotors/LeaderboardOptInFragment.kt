package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LeaderboardOptInFragment : Fragment() {

    private lateinit var switchOptIn: Switch
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var businessId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_leaderboard_opt_in, container, false)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        if (userId == null) {

            return view
        }

        // Retrieve the business ID from SharedPreferences
        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("business_id", null)

        if (businessId == null) {

            return view
        }

        switchOptIn = view.findViewById(R.id.switchSettingsOptIn)
        database = FirebaseDatabase.getInstance().getReference("Users/$businessId/Employees/$userId")

        // Fetch current leaderboard status and set switch accordingly
        database.child("leaderboard").get().addOnSuccessListener { snapshot ->
            val isOptedIn = snapshot.getValue(Boolean::class.java) ?: false
            switchOptIn.isChecked = isOptedIn
        }.addOnFailureListener {

        }

        // Listen for switch toggle to update leaderboard status in Firebase
        switchOptIn.setOnCheckedChangeListener { _, isChecked ->
            database.child("leaderboard").setValue(isChecked).addOnFailureListener {

            }
        }

        return view
    }
}
