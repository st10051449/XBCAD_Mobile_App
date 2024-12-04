package com.opsc7311poe.xbcad_antoniemotors

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageAdapter(
    private val imageUris: List<Uri>,
    private val onRemoveImage: (Uri) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgVehicle: ImageView = itemView.findViewById(R.id.imgVehicle)
        val btnRemoveImage: ImageView = itemView.findViewById(R.id.btnRemoveImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.regvehicle_image_item, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = imageUris[position]

        // Load the image into ImageView using Glide or any other library
        Glide.with(holder.itemView.context)
            .load(uri)
            .centerCrop()
            .into(holder.imgVehicle)

        // Set up the remove button click listener
        holder.btnRemoveImage.setOnClickListener {
            onRemoveImage(uri) // Call the callback to remove the image
        }
    }

    override fun getItemCount(): Int {
        return imageUris.size
    }
}
