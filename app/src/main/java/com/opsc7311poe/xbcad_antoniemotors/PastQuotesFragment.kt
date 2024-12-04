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

class PastQuotesFragment : Fragment() {

    private lateinit var searchInput: EditText
    private lateinit var linLay: LinearLayout
    private lateinit var btnBack: ImageView
    private val receiptsList = mutableListOf<ReceiptData>()  // Updated to hold receipts
    private lateinit var businessId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_past_quotes, container, false)

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
            val receiptsRef = database.getReference("Users/$businessId").child("Quotes")

            receiptsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded) return

                    linLay.removeAllViews()
                    receiptsList.clear()

                    // Check if there are any quotes
                    if (!snapshot.exists()) {
                        // If no quotes, display a message
                        val noQuotesMessage = TextView(context).apply {
                            text = "No quotes have been made, please add one."
                            setTextSize(16f)
                            setPadding(16, 16, 16, 16)
                        }
                        linLay.addView(noQuotesMessage)  // Add the message to the layout
                    } else {
                        // If there are quotes, populate the list and display them
                        for (receiptSnapshot in snapshot.children) {
                            val quote = receiptSnapshot.getValue(ReceiptData::class.java)

                            if (quote != null) {
                                receiptsList.add(quote)  // Add each receipt to the list

                                // Create and populate the card view
                                val cardView = LayoutInflater.from(context)
                                    .inflate(R.layout.documentationlayout, linLay, false) as CardView

                                cardView.findViewById<TextView>(R.id.txtCustName).text = quote.customerName ?: "Unknown"
                                cardView.findViewById<TextView>(R.id.txtPrice).text = "R ${quote.totalCost ?: "0"}"
                                cardView.findViewById<TextView>(R.id.txtDate).text = quote.dateCreated ?: "Unknown"

                                // Set OnClickListener for card view to navigate to receipt details
                                cardView.setOnClickListener {
                                    val quoteOverviewFragment = QuoteOverviewFragment()
                                    val bundle = Bundle()
                                    bundle.putString("quoteId", receiptSnapshot.key)
                                    quoteOverviewFragment.arguments = bundle
                                    replaceFragment(quoteOverviewFragment)
                                }

                                linLay.addView(cardView)  // Add the card to the container
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

        for (quote in receipts) {
            val cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.documentationlayout, linLay, false) as CardView

            cardView.findViewById<TextView>(R.id.txtCustName).text = quote.customerName ?: "Unknown"
            cardView.findViewById<TextView>(R.id.txtPrice).text = "R ${quote.totalCost ?: "0"}"

            // Set OnClickListener for card view to navigate to receipt details
            cardView.setOnClickListener {
                val receiptOverviewFragment = QuoteOverviewFragment()
                val bundle = Bundle()
                bundle.putString("quoteId", quote.id)  // Pass the receipt ID
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
