package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar

class VehicleDetailsFragment : Fragment() {

    private lateinit var txtVehicleNumberPlate: TextView
    private lateinit var txtVehicleOwner: TextView
    private lateinit var txtVehicleModel: TextView
    private lateinit var txtVehicleKms: TextView
    private lateinit var txtMYear: TextView
    private lateinit var txtlabelMYear: TextView
    private lateinit var txtVMake: TextView
    private lateinit var txtVinNumber: EditText
    private lateinit var txtVehicleReg: TextView
    private lateinit var txtAdminFN: TextView // New TextView for Admin Full Name
    private lateinit var rvFront: RecyclerView
    private lateinit var rvRight: RecyclerView
    private lateinit var rvRear: RecyclerView
    private lateinit var rvLeft: RecyclerView
    private lateinit var ynpYearPicker: NumberPicker
    private lateinit var btnEditVehicleDetails: Button
    private lateinit var btnDeleteVehicle: Button
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private var vehicleId: String? = null
    private var businessId: String? = null
    private var isEditable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vehicleId = arguments?.getString("vehicleId")
        businessId = arguments?.getString("businessId")
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_vehicle_details, container, false)

        txtVehicleNumberPlate = view.findViewById(R.id.txtNumberPlate)
        txtVehicleOwner = view.findViewById(R.id.txtVehicleOwner)
        txtVehicleModel = view.findViewById(R.id.txtVehicleModel)
        txtVehicleKms = view.findViewById(R.id.txtVehicleKms)
        txtMYear = view.findViewById(R.id.txtModelYear)
        txtlabelMYear = view.findViewById(R.id.labelEditModelYear)
        txtVMake = view.findViewById(R.id.txtVehicleMake)
        txtVinNumber = view.findViewById(R.id.txtVinNumber)
        txtVehicleReg = view.findViewById(R.id.txtVehicleRegDate)
        txtAdminFN = view.findViewById(R.id.txtAdminFullName)
        btnEditVehicleDetails = view.findViewById(R.id.btnEditVehicle)
        btnSave = view.findViewById(R.id.btnSaveChanges)
        btnDeleteVehicle = view.findViewById(R.id.btnDeleteVehicle)
        ynpYearPicker = view.findViewById(R.id.npYearPicker)
        btnBack = view.findViewById(R.id.ivBackButton)


        btnSave.visibility = View.INVISIBLE
        btnDeleteVehicle.visibility = View.INVISIBLE
        ynpYearPicker.visibility = View.GONE
        txtlabelMYear.visibility = View.GONE

        rvFront = view.findViewById(R.id.rvFrontSideImages)
        rvFront.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        rvRear = view.findViewById(R.id.rvRearSideImages)
        rvRear.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        rvRight = view.findViewById(R.id.rvRightSideImages)
        rvRight.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        rvLeft = view.findViewById(R.id.rvLeftSideImages)
        rvLeft.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        checkUserRole()
        setupYearPicker()

        btnEditVehicleDetails.setOnClickListener { enableEditing(true) }
        btnSave.setOnClickListener { showSaveConfirmationDialog() }
        // Set up the delete button listener
        btnDeleteVehicle.setOnClickListener { showDeleteConfirmationDialog() }

        searchVehicleAcrossBusinesses()

        btnBack.setOnClickListener {
            replaceFragment(SearchVehiclesFragment())
        }

        return view
    }

    private fun searchVehicleAcrossBusinesses() {
        val usersReference = FirebaseDatabase.getInstance().getReference("Users")

        usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(usersSnapshot: DataSnapshot) {
                for (businessSnapshot in usersSnapshot.children) {
                    val vehiclesSnapshot = businessSnapshot.child("Vehicles").child(vehicleId ?: "")

                    if (vehiclesSnapshot.exists()) {
                        val vehicleData = vehiclesSnapshot.getValue(VehicleData::class.java)
                        vehicleData?.let {
                            displayVehicleDetails(it)
                        }
                        return  // Exit loop once vehicle is found
                    }
                }
                Toast.makeText(requireContext(), "Vehicle not found.", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error searching vehicle.", Toast.LENGTH_SHORT).show()

            }
        })
    }

    private fun enableEditing(enable: Boolean) {
        isEditable = enable
        btnEditVehicleDetails.visibility = if (enable) View.INVISIBLE else View.VISIBLE
        btnSave.visibility = if (enable) View.VISIBLE else View.INVISIBLE
        ynpYearPicker.visibility = if (enable) View.VISIBLE else View.GONE
        txtlabelMYear.visibility = if (enable) View.VISIBLE else View.GONE
        txtVinNumber.isEnabled = enable  // Allow editing for VIN number when enabled
    }

    private fun saveChanges() {
        val newYear = txtMYear.text.toString()
        val currentVin = txtVinNumber.text.toString()

        // Validate VIN format if it has changed
        if (currentVin.isNotEmpty() && !currentVin.matches(Regex("^[0-9]{17}$"))) {
            Toast.makeText(context, "Invalid VIN number. It must be exactly 17 digits.", Toast.LENGTH_SHORT).show()
            return
        }

        val vehicleRef = FirebaseDatabase.getInstance()
            .getReference("Users/$businessId/Vehicles/$vehicleId")

        vehicleRef.child("vehicleYear").setValue(newYear)
        vehicleRef.child("vinNumber").setValue(currentVin).addOnSuccessListener {
            Toast.makeText(requireContext(), "Vehicle details updated.", Toast.LENGTH_SHORT).show()
            enableEditing(false)
            ynpYearPicker.visibility = View.GONE
            txtlabelMYear.visibility = View.GONE

        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to update vehicle.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun checkUserRole() {

        val userId = auth.currentUser?.uid ?: return
        if (businessId == null) {

            return
        }

        val userRef = FirebaseDatabase.getInstance().getReference("Users/$businessId/Employees/$userId")

        userRef.child("role").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val role = snapshot.getValue(String::class.java)
                if (role == "owner") {
                    btnDeleteVehicle.visibility = View.VISIBLE
                } else {
                    btnDeleteVehicle.visibility = View.INVISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun showSaveConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Save Changes")
            .setMessage("Are you sure you want to save changes?")
            .setPositiveButton("Save") { _, _ -> saveChanges() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun displayVehicleDetails(vehicle: VehicleData) {
        txtVehicleNumberPlate.text = vehicle.VehicleNumPlate
        txtVehicleOwner.text = vehicle.VehicleOwner
        txtVehicleModel.text = vehicle.VehicleModel
        txtVehicleKms.text = vehicle.VehicleKms
        txtVinNumber.setText(vehicle.VinNumber)
        txtVehicleReg.text = vehicle.registrationDate
        txtAdminFN.text = vehicle.AdminFullName
        txtMYear.text = vehicle.VehicleYear
        txtVMake.text = vehicle.VehicleMake

        loadVehicleImages(vehicle.images)
    }

    private fun setupYearPicker() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        ynpYearPicker.minValue = 1900
        ynpYearPicker.maxValue = currentYear
        ynpYearPicker.value = currentYear
        ynpYearPicker.setOnValueChangedListener { _, _, newVal ->
            txtMYear.text = newVal.toString()
        }
    }

    private fun loadVehicleImages(images: Map<String, Map<String, String>>) {
        val frontImages = images["front"]?.values?.toList() ?: emptyList()
        val rearImages = images["rear"]?.values?.toList() ?: emptyList()
        val rightImages = images["right"]?.values?.toList() ?: emptyList()
        val leftImages = images["left"]?.values?.toList() ?: emptyList()

        rvFront.adapter = VehicleImagesAdapter(frontImages)
        rvRear.adapter = VehicleImagesAdapter(rearImages)
        rvRight.adapter = VehicleImagesAdapter(rightImages)
        rvLeft.adapter = VehicleImagesAdapter(leftImages)
    }

    companion object {
        fun newInstance(vehicleId: String) =
            VehicleDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString("vehicleId", vehicleId)
                }
            }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Vehicle")
            .setMessage("Are you sure you want to delete this vehicle?")
            .setPositiveButton("Delete") { _, _ -> deleteVehicle() }
            .setNegativeButton("Cancel", null)
            .show()
    }




    private fun deleteVehicle() {
        if (businessId == null || vehicleId == null) {
            Toast.makeText(requireContext(), "Error: Missing business or vehicle ID.", Toast.LENGTH_SHORT).show()
            return
        }

        val vehicleRef = FirebaseDatabase.getInstance()
            .getReference("Users/$businessId/Vehicles/$vehicleId")

        vehicleRef.child("images").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Iterate through the categories (front, rear, left, right)
                for (categorySnapshot in snapshot.children) {
                    // Each category contains multiple images
                    for (imageSnapshot in categorySnapshot.children) {
                        val imageUrl = imageSnapshot.value.toString()
                        deleteImageFromStorage(imageUrl) // Delete each image from Storage
                    }
                }

                // After deleting images, delete the vehicle data
                vehicleRef.removeValue().addOnSuccessListener {
                    Toast.makeText(requireContext(), "Vehicle successfully deleted.", Toast.LENGTH_SHORT).show()
                    // Navigate back or update UI as needed
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to delete vehicle.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error deleting vehicle images.", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // Delete vehicle and associated images from Firebase
    /*private fun deleteVehicle() {
        if (businessId == null || vehicleId == null) {
            Toast.makeText(requireContext(), "Error: Missing business or vehicle ID.", Toast.LENGTH_SHORT).show()
            return
        }

        val vehicleRef = FirebaseDatabase.getInstance()
            .getReference("Users/$businessId/Vehicles/$vehicleId")

        vehicleRef.child("images").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Delete each image from Firebase Storage
                for (imageSnapshot in snapshot.children) {
                    val imageUrl = imageSnapshot.value.toString()
                    deleteImageFromStorage(imageUrl)
                }

                // Now delete the vehicle data
                vehicleRef.removeValue().addOnSuccessListener {
                    Toast.makeText(requireContext(), "Vehicle successfully deleted.", Toast.LENGTH_SHORT).show()
                    // Navigate back or update UI as needed
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to delete vehicle.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error deleting vehicle images.", Toast.LENGTH_SHORT).show()
            }
        })
    }*/

    // Function to delete an image from Firebase Storage
    private fun deleteImageFromStorage(imageUrl: String) {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
        storageRef.delete().addOnSuccessListener {

        }.addOnFailureListener {

        }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .commit()
    }
}





//VehicleImagesAdapter
class VehicleImagesAdapter(private val imageUrls: List<String>) : RecyclerView.Adapter<VehicleImagesAdapter.VehicleImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vehicle_images, parent, false)
        return VehicleImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleImageViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(imageUrls[position])
            .into(holder.vehicleImageView)
    }

    override fun getItemCount(): Int = imageUrls.size

    class VehicleImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleImageView: ImageView = itemView.findViewById(R.id.vehicleImageView)
    }
}



