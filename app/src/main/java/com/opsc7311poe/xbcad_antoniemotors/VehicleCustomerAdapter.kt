package com.opsc7311poe.xbcad_antoniemotors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VehicleCustomerAdapter(
    private var customers: List<CustomerData>,
    private val onCustomerSelected: (CustomerData) -> Unit
) : RecyclerView.Adapter<VehicleCustomerAdapter.CustomerViewHolder>() {

    private var selectedCustomer: CustomerData? = null

    inner class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val customerName: TextView = itemView.findViewById(R.id.txtCustomerName)

        fun bind(customer: CustomerData) {
            customerName.text = "${customer.CustomerName} ${customer.CustomerSurname}"
            itemView.setOnClickListener {
                selectedCustomer = customer
                onCustomerSelected(customer)
                notifyDataSetChanged() // Refresh to visually update selected item if needed
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(customers[position])

    }

    override fun getItemCount(): Int = customers.size

    fun updateCustomers(newCustomers: List<CustomerData>) {
        customers = newCustomers
        selectedCustomer = null // Reset selected customer if the list changes
        notifyDataSetChanged()
    }
}

