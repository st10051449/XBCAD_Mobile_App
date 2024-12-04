package com.opsc7311poe.xbcad_antoniemotors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList

class CustomerAdapter(private var customerList: List<CustomerData>,
                      private val onItemClick: (CustomerData) -> Unit // Callback for item clicks
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>(), Filterable {

    // Original full list of customers for filtering
    private var customerListFull: List<CustomerData> = ArrayList(customerList)

    // ViewHolder for Customer items
    inner class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val customerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        val customerMobile: TextView = itemView.findViewById(R.id.tvCustomerMobile)
        val customerAddress: TextView = itemView.findViewById(R.id.tvCustomerAddress)

        init {
            itemView.setOnClickListener {
                onItemClick(customerList[adapterPosition]) // Pass selected customer to callback
            }
        }
    }

    // Creating the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.customer_snippet, parent, false)
        return CustomerViewHolder(itemView)
    }

    // Binding customer data to the ViewHolder
    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val currentCustomer = customerList[position]
        holder.customerName.text = "${currentCustomer.CustomerName} ${currentCustomer.CustomerSurname}"
        holder.customerMobile.text = "Mobile Number: ${currentCustomer.CustomerMobileNum}"
        holder.customerAddress.text = "Address: ${currentCustomer.CustomerAddress}"
    }

    // Get the number of items in the list
    override fun getItemCount(): Int {
        return customerList.size
    }

    // Providing the filter implementation
    override fun getFilter(): Filter {
        return customerFilter
    }

    private val customerFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = ArrayList<CustomerData>()

            if (constraint == null || constraint.isEmpty()) {
                filteredList.addAll(customerListFull)
            } else {
                val filterPattern = constraint.toString().lowercase().trim()

                for (customer in customerListFull) {
                    if (customer.CustomerName.lowercase().contains(filterPattern) ||
                        customer.CustomerSurname.lowercase().contains(filterPattern)) {
                        filteredList.add(customer)
                    }
                }
            }

            return FilterResults().apply { values = filteredList }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            customerList = results?.values as List<CustomerData>
            notifyDataSetChanged()
        }
    }


}
