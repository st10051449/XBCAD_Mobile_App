package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class CustomerFragment : Fragment() {


    private lateinit var btnBack: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var database: DatabaseReference
    private lateinit var customerAdapter: CustomerAdapter
    //private val customerList = mutableListOf<CustomerData>()
    private lateinit var customerList: ArrayList<CustomerData>
    private lateinit var searchCustomers: SearchView
    private lateinit var usersReference: DatabaseReference


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_customer, container, false)


        btnBack = view.findViewById(R.id.ivBackButton)
        searchCustomers = view.findViewById(R.id.customerSearch)
        recyclerView = view.findViewById(R.id.recyclerViewCustomers)



        // Set click listener for the "Back" button
        btnBack.setOnClickListener {
            replaceFragment(CustomerMenuFragment())
        }

        customerList = ArrayList()
        //customerAdapter = CustomerAdapter(customerList)
        customerAdapter = CustomerAdapter(customerList) { selectedCustomer ->
            openCustomerDetailsFragment(selectedCustomer)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = customerAdapter


        usersReference = FirebaseDatabase.getInstance().getReference("Users")

        searchCustomers.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                fetchCustomers(newText)
                return false
            }
        })

        // Initial fetch to load all customers
        fetchCustomers(null)

        return view
    }


    private fun fetchCustomers(searchQuery: String?) {
        val adminId = FirebaseAuth.getInstance().currentUser?.uid
        if (adminId != null) {
            usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(usersSnapshot: DataSnapshot) {
                    var businessID: String? = null
                    for (businessSnapshot in usersSnapshot.children) {
                        val employeeSnapshot = businessSnapshot.child("Employees").child(adminId)
                        if (employeeSnapshot.exists()) {
                            businessID = employeeSnapshot.child("businessID").getValue(String::class.java)
                                ?: employeeSnapshot.child("businessId").getValue(String::class.java)
                            break
                        }
                    }

                    if (businessID != null) {
                        val customerReference = usersReference.child(businessID).child("Customers")
                        customerReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                customerList.clear()

                                for (customerSnapshot in snapshot.children) {
                                    val customer = customerSnapshot.getValue(CustomerData::class.java)
                                    if (customer != null) {
                                        // If searchQuery is not null, filter by the query
                                        if (searchQuery.isNullOrBlank() ||
                                            customer.CustomerName.contains(searchQuery, true) ||
                                            customer.CustomerSurname.contains(searchQuery, true)) {
                                            customerList.add(customer)
                                        }
                                    }
                                }
                                customerAdapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(requireContext(), "Error fetching customers.", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(requireContext(), "Unable to find associated business.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error fetching business information.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }




    private fun openCustomerDetailsFragment(customer: CustomerData) {
        val fragment = CustomerDetailsFragment().apply {
            arguments = Bundle().apply {
                putString("customerID", customer.CustomerID)
                putString("customerName", customer.CustomerName)
                putString("customerSurname", customer.CustomerSurname)
                putString("customerEmail", customer.CustomerEmail)
                putString("customerAddress", customer.CustomerAddress)
                putString("customerMobile", customer.CustomerMobileNum)
                putString("customerType", customer.CustomerType)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }




    // Replaces the current fragment with the specified fragment
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
