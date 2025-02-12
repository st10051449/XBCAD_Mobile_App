package com.opsc7311poe.xbcad_antoniemotors

import QuoteData
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class PastReceiptsFragment : Fragment() {

    private lateinit var searchInput: EditText
    private lateinit var linLay: LinearLayout
    private lateinit var btnBack: ImageView
    private val receiptsList = mutableListOf<ReceiptData>()  // Updated to hold receipts
    private lateinit var businessId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_past_receipts, container, false)

        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!

        searchInput = view.findViewById(R.id.txtSearch)
        btnBack = view.findViewById(R.id.ivBackButton)

        // Initialize the LinearLayout to display the cards
        linLay = view.findViewById(R.id.linlayServiceCards)

        // Load receipts initially
        loadReceipts()

        // Set a listener to filter receipts as the user types
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchReceipts(s.toString()) // Perform search when text changes
            }
        })

        btnBack.setOnClickListener {
            replaceFragment(DocumentationFragment())
        }

        return view
    }

    private fun loadReceipts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val database = Firebase.database
            val receiptsRef = database.getReference("Users/$businessId").child("Receipts")

            receiptsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    context?.let { ctx ->
                        linLay.removeAllViews()  // Clear previous views
                        receiptsList.clear()  // Clear list before adding new data

                        if (!snapshot.exists()) {
                            // No receipts available, show a message
                            val noReceiptsMessage = TextView(ctx).apply {
                                text = "No receipts have been made, please add one."
                                textSize = 16f
                                setPadding(16, 16, 16, 16)
                            }
                            linLay.addView(noReceiptsMessage)
                        } else {
                            // Receipts exist, process them
                            for (receiptSnapshot in snapshot.children) {
                                val receipt = receiptSnapshot.getValue(ReceiptData::class.java)

                                if (receipt != null) {
                                    receiptsList.add(receipt)  // Add each receipt to the list

                                    // Create and populate the card view
                                    val cardView = LayoutInflater.from(ctx)
                                        .inflate(R.layout.documentationlayout, linLay, false) as CardView

                                    cardView.findViewById<TextView>(R.id.txtCustName).text = receipt.customerName ?: "Unknown"
                                    cardView.findViewById<TextView>(R.id.txtPrice).text = "R ${receipt.totalCost ?: "0"}"
                                    cardView.findViewById<TextView>(R.id.txtDate).text = "${receipt.dateCreated ?: "0"}"

                                    // Set OnClickListener for card view to navigate to receipt details
                                    cardView.setOnClickListener {
                                        val receiptOverviewFragment = ReceiptOverviewFragment()
                                        val bundle = Bundle()
                                        bundle.putString("receiptId", receiptSnapshot.key)
                                        receiptOverviewFragment.arguments = bundle
                                        replaceFragment(receiptOverviewFragment)
                                    }

                                    linLay.addView(cardView)  // Add the card to the container
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle any database errors
                }
            })
        }
    }

    private fun searchReceipts(query: String) {
        val filteredReceipts = receiptsList.filter {
            it.customerName?.contains(query, ignoreCase = true) == true
        }
        displayReceipts(filteredReceipts)
    }

    private fun displayReceipts(receipts: List<ReceiptData>) {
        linLay.removeAllViews()  // Clear previous views

        for (receipt in receipts) {
            val cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.documentationlayout, linLay, false) as CardView

            cardView.findViewById<TextView>(R.id.txtCustName).text = receipt.customerName ?: "Unknown"
            cardView.findViewById<TextView>(R.id.txtPrice).text = "R ${receipt.totalCost ?: "0"}"

            // Set OnClickListener for card view to navigate to receipt details
            cardView.setOnClickListener {
                val receiptOverviewFragment = ReceiptOverviewFragment()
                val bundle = Bundle()
                bundle.putString("receiptId", receipt.id)  // Pass the receipt ID
                receiptOverviewFragment.arguments = bundle
                replaceFragment(receiptOverviewFragment)
            }

            linLay.addView(cardView)  // Add the card to the container
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
