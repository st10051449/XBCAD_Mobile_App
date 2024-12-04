package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class LeaderboardFragment : Fragment() {

    private lateinit var leaderboardContainer: LinearLayout

    private lateinit var profilepic1: ImageView
    private lateinit var profilepic2: ImageView
    private lateinit var profilepic3: ImageView

    private lateinit var name1: TextView
    private lateinit var name2: TextView
    private lateinit var name3: TextView

    private lateinit var score1: TextView
    private lateinit var score2: TextView
    private lateinit var score3: TextView

    private lateinit var businessId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_leaderboard, container, false)

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!
        leaderboardContainer = view.findViewById(R.id.leaderboardContainer)

        profilepic1 = view.findViewById(R.id.player1pp)
        profilepic2 = view.findViewById(R.id.player2_pp)
        profilepic3 = view.findViewById(R.id.player3_pp)

        name1 = view.findViewById(R.id.player1_name)
        name2 = view.findViewById(R.id.player2_name)
        name3 = view.findViewById(R.id.player3_name)

        score1 = view.findViewById(R.id.player1_score)
        score2 = view.findViewById(R.id.player2_score)
        score3 = view.findViewById(R.id.player3_score)

        fetchOrder()

        return view
    }

    private fun fetchOrder() {

        // Firebase database reference
        val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(businessId).child("Employees")

        // Query employees where leaderboard is true
        dbRef.orderByChild("leaderboard").equalTo(true).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val employeesList = mutableListOf<EmployeeLeaderboard>()

                for (employeeSnapshot in snapshot.children) {
                    val name = employeeSnapshot.child("firstName").getValue(String::class.java) ?: "Unknown"
                    val surname = employeeSnapshot.child("lastName").getValue(String::class.java) ?: "Unknown"
                    val completedTasks = employeeSnapshot.child("completedTasks").getValue(Int::class.java) ?: 0
                    val profileImageURL = employeeSnapshot.child("profileImageUrl").getValue(String::class.java)
                        ?: employeeSnapshot.child("profilePicUrl").getValue(String::class.java) ?: ""


                    val fullName = "$name $surname"



                    // Create an EmployeeLeaderboard object
                    val empLeading = EmployeeLeaderboard(name = fullName, completedTasks = completedTasks, profileImageURL = profileImageURL)
                    employeesList.add(empLeading)
                }

                if (employeesList.isEmpty()) {
                }

                // Sort employees by completedTasks in descending order
                employeesList.sortByDescending { it.completedTasks }

                displayLeaderboard(employeesList)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun displayLeaderboard(employeesList: List<EmployeeLeaderboard>) {
        // Clear container if dynamically adding additional employees
        leaderboardContainer.removeAllViews()

        // Set top three employees in the preset views
        if (employeesList.isNotEmpty()) {
            name1.text = employeesList[0].name
            score1.text = employeesList[0].completedTasks.toString()
            Glide.with(this)
                .load(employeesList[0].profileImageURL)
                .placeholder(R.drawable.vector_myprofile)
                .error(R.drawable.vector_myprofile)
                .transform(CircleCrop())
                .into(profilepic1)
        }

        if (employeesList.size > 1) {
            name2.text = employeesList[1].name
            score2.text = employeesList[1].completedTasks.toString()
            Glide.with(this)
                .load(employeesList[1].profileImageURL)
                .placeholder(R.drawable.vector_myprofile)
                .error(R.drawable.vector_myprofile)
                .transform(CircleCrop())
                .into(profilepic2)
        }

        if (employeesList.size > 2) {
            name3.text = employeesList[2].name
            score3.text = employeesList[2].completedTasks.toString()
            Glide.with(this)
                .load(employeesList[2].profileImageURL)
                .placeholder(R.drawable.vector_myprofile)
                .error(R.drawable.vector_myprofile)
                .transform(CircleCrop())
                .into(profilepic3)
        }

        // Dynamically add remaining employees to leaderboard container
        for (index in 3 until employeesList.size) {
            val employee = employeesList[index]
            val employeeView = LayoutInflater.from(context).inflate(R.layout.leaderboard_item, leaderboardContainer, false)
            employeeView.findViewById<TextView>(R.id.rank).text = "${index + 1}"
            employeeView.findViewById<TextView>(R.id.playersname).text = employee.name
            employeeView.findViewById<TextView>(R.id.playerscore).text = "${employee.completedTasks}"

            // Load profile image using Glide
            val profileImageView = employeeView.findViewById<ImageView>(R.id.profpic)
            Glide.with(this)
                .load(employee.profileImageURL)
                .placeholder(R.drawable.vector_myprofile)
                .error(R.drawable.vector_myprofile)
                .transform(CircleCrop())
                .into(profileImageView)

            leaderboardContainer.addView(employeeView)
        }
    }

}

