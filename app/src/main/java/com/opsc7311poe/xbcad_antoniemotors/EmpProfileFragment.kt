package com.opsc7311poe.xbcad_antoniemotors

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.InputStream
import com.bumptech.glide.request.RequestOptions

class EmpProfileFragment : Fragment() {

    private lateinit var txtAppName: TextView
    private lateinit var txtAppSurname: TextView
    private lateinit var txtAppEmail: TextView
    private lateinit var txtAppPhone: TextView
    private lateinit var txtAppAddress: TextView
    private lateinit var txtUpdatePic: TextView
    private lateinit var txtAppRole: TextView
    private lateinit var ivEmpPic: ImageView
    private lateinit var businessId: String

    private lateinit var btnBack: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private val PICK_IMAGE_REQUEST = 71
    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_emp_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve business ID from SharedPreferences
        businessId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("business_id", null) ?: ""

        // Initialize views
        txtAppName = view.findViewById(R.id.txtAppName)
        txtAppSurname = view.findViewById(R.id.txtAppSurname)
        txtAppEmail = view.findViewById(R.id.txtAppEmail)
        txtAppPhone = view.findViewById(R.id.txtAppPhone)
        txtAppAddress = view.findViewById(R.id.txtAppAddress)
        txtAppRole = view.findViewById(R.id.txtAppRole)
        ivEmpPic = view.findViewById(R.id.ivEmpPic)
        txtUpdatePic = view.findViewById(R.id.txtUpdatePic)
        btnBack = view.findViewById(R.id.ivBackButton)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        // Load Employee Data
        loadEmployeeData()

        // Profile picture upload button (removed listener)
        ivEmpPic.setOnClickListener {
            openImageChooser()
        }

        txtUpdatePic.setOnClickListener(){
             openImageChooser()
        }

        btnBack.setOnClickListener {
            replaceFragment(EmployeeHomeFragment())
        }
    }

    private fun loadEmployeeData() {
        val userId = auth.currentUser?.uid ?: return

        database.child("Users").child(businessId).child("Employees").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Retrieve employee details
                        val firstName = snapshot.child("firstName").value.toString()
                        val lastName = snapshot.child("lastName").value.toString()
                        val email = snapshot.child("email").value.toString()
                        val phone = snapshot.child("phone").value.toString()
                        val address = snapshot.child("address").value.toString()
                        val role = snapshot.child("role").value.toString()
                        val profilePicUrl = snapshot.child("profileImageUrl").value.toString()

                        // Set data to UI
                        txtAppName.text = firstName
                        txtAppSurname.text = lastName
                        txtAppEmail.text = email
                        txtAppPhone.text = phone
                        txtAppAddress.text = address
                        txtAppRole.text = role

                        // Load Profile Picture
                        if (profilePicUrl.isNotEmpty()) {
                            displayProfilePicture(profilePicUrl)
                        }
                    } else {
                        Toast.makeText(context, "Employee data not found.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load profile data.", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun openImageChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data


            // Load and display the selected image as a preview
            val inputStream: InputStream? = context?.contentResolver?.openInputStream(imageUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            ivEmpPic.setImageBitmap(bitmap) // Preview selected image

            // Automatically upload the selected image
            uploadProfilePicture()
        }
    }

    private fun uploadProfilePicture() {
        val userId = auth.currentUser?.uid ?: return


        imageUri?.let { newImageUri ->
            val ref = storage.reference.child("employee_profile_images/$userId.jpg")


            // Step 1: Check for existing image URL
            database.child("Users").child(businessId).child("Employees").child(userId)
                .child("profileImageUrl")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val oldImageUrl = snapshot.value as? String
                        if (oldImageUrl != null) {
                            // Step 2: Delete the old image from Firebase Storage
                            FirebaseStorage.getInstance().getReferenceFromUrl(oldImageUrl).delete()
                                .addOnSuccessListener {

                                    // Proceed with uploading new image
                                    uploadNewProfileImage(ref, newImageUri)
                                }
                                .addOnFailureListener { e ->

                                    // Even if deletion fails, proceed with uploading new image
                                    uploadNewProfileImage(ref, newImageUri)
                                }
                        } else {

                            uploadNewProfileImage(ref, newImageUri)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                        Toast.makeText(context, "Failed to check old image URL.", Toast.LENGTH_SHORT).show()
                    }
                })
        } ?: run {

            Toast.makeText(context, "No image selected.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadNewProfileImage(ref: StorageReference, newImageUri: Uri) {

        ref.putFile(newImageUri)
            .addOnSuccessListener {

                ref.downloadUrl.addOnSuccessListener { uri ->


                    // Step 3: Update the database with the new image URL
                    val dbRef = database.child("Users").child(businessId)
                        .child("Employees").child(auth.currentUser?.uid ?: "")
                        .child("profileImageUrl")
                    dbRef.setValue(uri.toString())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {

                                Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                            } else {

                                Toast.makeText(context, "Failed to update profile picture in database", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->

                Toast.makeText(context, "Failed to upload new image.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayProfilePicture(profilePicUrl: String) {
        Glide.with(this)
            .load(profilePicUrl)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.vector_myprofile)
            .error(R.drawable.vector_myprofile)
            .into(ivEmpPic)
    }


    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }

}
