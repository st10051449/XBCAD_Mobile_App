package com.opsc7311poe.xbcad_antoniemotors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class PartAdapter(private val onItemClick: (PartsData) -> Unit) : ListAdapter<PartsData, PartAdapter.PartViewHolder>(PartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.stockviewlayout, parent, false)
        return PartViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartViewHolder, position: Int) {
        val part = getItem(position)
        holder.bind(part)
        // Pass the clicked item to the listener to open EditPartFragment
        holder.itemView.setOnClickListener {
            onItemClick(part)
        }
    }

    class PartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val partNameTextView: TextView = itemView.findViewById(R.id.tvPartName)
        private val partDescriptionTextView: TextView = itemView.findViewById(R.id.tvPartDescription)
        private val partCostTextView: TextView = itemView.findViewById(R.id.tvPartCost)
        private val partStockTextView: TextView = itemView.findViewById(R.id.txtStock)

        fun bind(part: PartsData) {
            partNameTextView.text = part.partName
            partDescriptionTextView.text = part.partDescription
            partCostTextView.text = "R ${String.format("%.2f", part.costPrice ?: 0.0)}"
            partStockTextView.text = "Stock: ${part.stockCount}"
        }
    }

    class PartDiffCallback : DiffUtil.ItemCallback<PartsData>() {
        override fun areItemsTheSame(oldItem: PartsData, newItem: PartsData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PartsData, newItem: PartsData): Boolean {
            return oldItem == newItem
        }
    }
}
