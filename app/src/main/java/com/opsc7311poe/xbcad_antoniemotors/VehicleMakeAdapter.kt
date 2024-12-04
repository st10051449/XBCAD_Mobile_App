package com.opsc7311poe.xbcad_antoniemotors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class VehicleMakeAdapter(
    private var vehicleMakes: List<String>,
    private val onMakeSelected: (String) -> Unit
) : RecyclerView.Adapter<VehicleMakeAdapter.VehicleMakeViewHolder>(), Filterable {

    private var filteredVehicleMakes: List<String> = vehicleMakes

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleMakeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return VehicleMakeViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleMakeViewHolder, position: Int) {
        val vehicleMake = filteredVehicleMakes[position]
        holder.bind(vehicleMake)
    }

    override fun getItemCount(): Int = filteredVehicleMakes.size

    inner class VehicleMakeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(vehicleMake: String) {
            textView.text = vehicleMake
            itemView.setOnClickListener {
                onMakeSelected(vehicleMake)
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase() ?: ""

                val filteredList = if (query.isEmpty()) {
                    vehicleMakes
                } else {
                    vehicleMakes.filter { it.lowercase().contains(query) }
                }

                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredVehicleMakes = results?.values as List<String>
                notifyDataSetChanged()
            }
        }
    }
}

