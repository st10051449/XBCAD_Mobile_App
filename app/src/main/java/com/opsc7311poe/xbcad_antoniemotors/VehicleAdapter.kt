package com.opsc7311poe.xbcad_antoniemotors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class VehicleAdapter(
    private var vehicleList: List<VehicleData>,
    private val onVehicleClick: (VehicleData) -> Unit
) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    private var filteredList: List<VehicleData> = vehicleList

    // Method to update the vehicle list dynamically
    fun updateList(newVehicleList: List<VehicleData>) {
        vehicleList = newVehicleList
        filteredList = newVehicleList
        notifyDataSetChanged() // Refresh the RecyclerView
    }

    fun filterList(query: String?) {
        filteredList = if (!query.isNullOrEmpty()) {
            vehicleList.filter {
                it.VehicleNumPlate.contains(query, ignoreCase = true) ||
                        it.VehicleModel.contains(query, ignoreCase = true) ||
                        it.VehicleOwner.contains(query, ignoreCase = true)
            }
        } else {
            vehicleList
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = filteredList.size

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.vehicle_snippet, parent, false)
        return VehicleViewHolder(view)
    }

    inner class VehicleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtVehicleNumPlate: TextView = view.findViewById(R.id.txtVehicleNumPlate)
        private val txtVehicleModel: TextView = view.findViewById(R.id.txtVehicleModel)
        private val txtVehicleOwner: TextView = view.findViewById(R.id.txtVehicleOwner)
        private val imgVehicle: ImageView = view.findViewById(R.id.imgVehicle)

        fun bind(vehicle: VehicleData) {
            txtVehicleNumPlate.text = vehicle.VehicleNumPlate
            txtVehicleModel.text = vehicle.VehicleModel
            txtVehicleOwner.text = vehicle.VehicleOwner

            if (vehicle.images.isNotEmpty()) {
                val frontImages = vehicle.images["front"]
                val firstImageUrl = frontImages?.values?.firstOrNull() // Get the first image URL for the front view if available
                if (firstImageUrl != null) {
                    Glide.with(imgVehicle.context)
                        .load(firstImageUrl)
                        .into(imgVehicle)
                } else {
                    imgVehicle.setImageResource(R.drawable.vehicledetails)
                }
            } else {
                imgVehicle.setImageResource(R.drawable.vehicledetails) // Fallback image
            }

            // Set up click listener for the entire item view
            itemView.setOnClickListener {
                onVehicleClick(vehicle)
            }
        }
    }
}
