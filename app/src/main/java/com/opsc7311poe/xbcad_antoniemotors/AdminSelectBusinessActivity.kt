package com.opsc7311poe.xbcad_antoniemotors

import BusinessAdapter
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SearchView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class AdminSelectBusinessActivity : AppCompatActivity() {

    //private lateinit var spBusinessNames: Spinner
    private lateinit var txtBusinessNames: EditText
    private lateinit var btnAdminSelBusiness: Button
    private lateinit var businessNamesList: MutableList<String>
    private lateinit var businessIds: MutableList<String>
    private lateinit var databaseRef: DatabaseReference
    private lateinit var selectedBusiness: String
    private lateinit var selectedBusinessId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_admin_select_business)

        //spBusinessNames = findViewById(R.id.spAllBusinessNames)
        txtBusinessNames = findViewById(R.id.edtBusinessNames)
        btnAdminSelBusiness = findViewById(R.id.btnAdminBusChoice)
        businessNamesList = mutableListOf()
        businessIds = mutableListOf()
        databaseRef = FirebaseDatabase.getInstance().getReference("Users")

        // Load business names into spinner
        loadBusinessNames()

        txtBusinessNames.setOnClickListener {
            showBusinessNameDialog()
        }

        // Button click listener to carry the selected business name to the next page
        btnAdminSelBusiness.setOnClickListener {
            if (selectedBusiness.isNotEmpty()) {
                val intent = Intent(this, AdminEnterInfo::class.java)
                intent.putExtra("selectedBusinessName", selectedBusiness)
                intent.putExtra("selectedBusinessId", selectedBusinessId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a business.", Toast.LENGTH_SHORT).show()
            }
        }

    }



    // Function to load business names from Firebase
    private fun loadBusinessNames() {
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                businessNamesList.clear()
                businessIds.clear()

                for (userSnapshot in snapshot.children) {
                    val businessName = userSnapshot.child("BusinessInfo").child("businessName").getValue(String::class.java)
                    val businessId = userSnapshot.key
                    if (businessName != null && businessId != null) {
                        businessNamesList.add(businessName)
                        businessIds.add(businessId)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminSelectBusinessActivity, "Failed to load business names: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showBusinessNameDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_business_names)
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerBusinessName)
        val searchView = dialog.findViewById<SearchView>(R.id.searchBusinessName)

        val adapter = BusinessAdapter(businessNamesList.toMutableList()) { selectedBusinessName ->
            // Find the correct business ID using the selected name
            val index = businessNamesList.indexOf(selectedBusinessName)
            if (index != -1) {
                selectedBusiness = selectedBusinessName
                selectedBusinessId = businessIds[index]
                txtBusinessNames.setText(selectedBusiness)
            }
            dialog.dismiss()
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                adapter.filterList(newText)
                return true
            }
            override fun onQueryTextSubmit(query: String): Boolean {
                adapter.filterList(query)
                return true
            }
        })

        dialog.show()
    }


}
