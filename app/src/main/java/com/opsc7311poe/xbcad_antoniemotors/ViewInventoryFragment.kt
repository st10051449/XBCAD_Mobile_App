package com.opsc7311poe.xbcad_antoniemotors

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ViewInventoryFragment : Fragment() {

    private lateinit var addImage: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PartAdapter
    private lateinit var database: DatabaseReference
    private val auth = FirebaseAuth.getInstance()
    private lateinit var btnBack: ImageView
    private lateinit var businessId: String
    private lateinit var txtNoData: TextView

    private val REQUEST_CODE_NOTIFICATION_PERMISSION = 1001

    private lateinit var searchEditText: EditText
    private var allPartsList = listOf<PartsData>() // To store the full list of parts

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_inventory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("business_id", null)!!

        addImage = view.findViewById(R.id.imgPlus)
        recyclerView = view.findViewById(R.id.recyclerViewInventory)
        searchEditText = view.findViewById(R.id.txtSearch)
        btnBack = view.findViewById(R.id.ivBackButton)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PartAdapter { selectedPart ->
            if (selectedPart.id == null) {
                Toast.makeText(context, "Part ID is missing", Toast.LENGTH_SHORT).show()
                return@PartAdapter
            }
            // Continue with creating the fragment if ID is not null
            val fragment = EditPartFragment().apply {
                arguments = Bundle().apply {
                    putString("partId", selectedPart.id)
                    putString("partName", selectedPart.partName)
                    putString("partDescription", selectedPart.partDescription)
                    putInt("stockCount", selectedPart.stockCount ?: 0)
                    putInt("minStock", selectedPart.minStock ?: 0)
                    putDouble("costPrice", selectedPart.costPrice ?: 0.0)
                }
            }
            replaceFragment(fragment)
        }

        btnBack.setOnClickListener {
            replaceFragment(VehicleMenuFragment())
        }

        recyclerView.adapter = adapter

        // Set up Firebase database reference
        database = Firebase.database.reference

        // Fetch parts data and display it
        fetchParts()

        // Set up button to navigate to AddPartFragment in add mode
        addImage.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            val fragment = AddPartFragment().apply {
                arguments = Bundle().apply {
                    putBoolean("isEditMode", false)
                }
            }
            replaceFragment(fragment)
        }

        // Set up search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterParts(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun fetchParts() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid

        database.child("Users/$businessId").child("parts")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val partsList = mutableListOf<PartsData>()
                    snapshot.children.forEach { partSnapshot ->
                        val part = partSnapshot.getValue(PartsData::class.java)
                        part?.let {
                            it.id = partSnapshot.key
                            partsList.add(it)

                            // Check if stock is less than or equal to minimum stock
                            if (it.stockCount != null && it.minStock != null && it.stockCount <= it.minStock) {
                                showStockWarningNotification(it)
                            }
                        }
                    }

                    // Show or hide the no-data message
                    val noDataMessage = view?.findViewById<TextView>(R.id.txtNoData)
                    if (partsList.isEmpty()) {
                        noDataMessage?.visibility = View.VISIBLE
                    } else {
                        noDataMessage?.visibility = View.GONE
                    }

                    // Sort and display the parts list
                    allPartsList = partsList.sortedBy { it.partName?.toLowerCase() ?: "" }
                    adapter.submitList(allPartsList) // Display full list initially
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to fetch parts data", Toast.LENGTH_SHORT).show()
                }
            })
    }



    private fun filterParts(query: String) {
        val filteredList = allPartsList.filter { part ->
            part.partName?.contains(query, ignoreCase = true) == true
        }
        adapter.submitList(filteredList)
    }
    private fun showStockWarningNotification(part: PartsData) {
        val notificationManager = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel (required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "stock_warning_channel"
            val channelName = "Stock Warning"
            val channelDescription = "Notifications for parts with low stock"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create the notification
        val notification = NotificationCompat.Builder(requireContext(), "stock_warning_channel")
            .setContentTitle("Low Stock Warning")
            .setContentText("The stock for ${part.partName} is low. Current stock: ${part.stockCount}, Minimum stock: ${part.minStock}")
            .setSmallIcon(R.drawable.vector_deny)  // Use an appropriate icon here
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Show the notification
        notificationManager.notify(1, notification)
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
