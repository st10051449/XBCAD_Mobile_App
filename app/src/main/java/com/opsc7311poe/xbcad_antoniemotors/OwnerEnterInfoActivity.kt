package com.opsc7311poe.xbcad_antoniemotors

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class OwnerEnterInfoActivity : AppCompatActivity() {

    private lateinit var ivProfilePic: ImageView
    private lateinit var txtSelectPic: TextView
    private lateinit var ownersName: EditText
    private lateinit var ownersSurname: EditText
    private lateinit var ownersEmail: EditText
    private lateinit var ownersPhone: EditText
    private lateinit var ownersAddress: EditText
    private lateinit var ownersPassword: EditText
    private lateinit var btnRegisterOwnerAccount: Button
    private lateinit var progressBar: ProgressBar

    private var selectedProfilePicUri: Uri? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_owner_enter_info)

        // Initializing views
        ivProfilePic = findViewById(R.id.ivOwnerProfilePic)
        txtSelectPic = findViewById(R.id.txtSelectImageOwner)
        ownersName = findViewById(R.id.txtOwnerFirstName)
        ownersSurname = findViewById(R.id.txtOwnerLastName)
        ownersEmail = findViewById(R.id.txtOwnerEmail)
        ownersPhone = findViewById(R.id.txtOwnerPhone)
        ownersAddress = findViewById(R.id.txtOwnerAddress)
        ownersPassword = findViewById(R.id.txtOwnerPassword)
        btnRegisterOwnerAccount = findViewById(R.id.btnRegisterOwner)
        progressBar = findViewById(R.id.progressBar)

        // Initialize FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()
        auth = FirebaseAuth.getInstance()

        // Getting data from the intent
        val businessName = intent.getStringExtra("businessName")
        val ownerName = intent.getStringExtra("ownerName")
        val ownerSurname = intent.getStringExtra("ownerSurname")

        // Populating fields with intent data
        ownersName.setText(ownerName)
        ownersSurname.setText(ownerSurname)

        // Selecting profile picture
        txtSelectPic.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 1)
        }

        // Registering owner account
        btnRegisterOwnerAccount.setOnClickListener {
            if (validateInputs()) {
                // Create user account in Firebase Auth
                registerOwnerAccount(
                    ownersEmail.text.toString().trim(),
                    ownersPassword.text.toString().trim(),
                    ownersName.text.toString().trim(),
                    ownersSurname.text.toString().trim(),
                    businessName!!,
                    ownersPhone.text.toString().trim(),
                    ownersAddress.text.toString().trim()
                )
            }

            else if (!validateInputs()){
                return@setOnClickListener
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            selectedProfilePicUri = data?.data
            ivProfilePic.setImageURI(selectedProfilePicUri)
        }
    }

    // Function to validate all input fields
    private fun validateInputs(): Boolean {
        // Validate profile picture
        if (selectedProfilePicUri == null) {
            Toast.makeText(this, "Please select a profile picture.", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate name
        if (ownersName.text.toString().trim().isEmpty()) {
            ownersName.error = "Name is required."
            return false
        }

        // Validate surname
        if (ownersSurname.text.toString().trim().isEmpty()) {
            ownersSurname.error = "Surname is required."
            return false
        }

        // Validate email
        if (ownersEmail.text.toString().trim().isEmpty() ||
            !Patterns.EMAIL_ADDRESS.matcher(ownersEmail.text.toString().trim()).matches()
        ) {
            ownersEmail.error = "Valid email is required."
            return false
        }

        // Validate phone
        if (ownersPhone.text.toString().trim().isEmpty() ||
            !ownersPhone.text.toString().trim().matches(Regex("^\\d{10,15}$"))
        ) {
            ownersPhone.error = "Valid phone number is required."
            return false
        }

        // Validate address
        if (ownersAddress.text.toString().trim().isEmpty()) {
            ownersAddress.error = "Address is required."
            return false
        }

        // Validate password
        val password = ownersPassword.text.toString().trim()
        if (password.isEmpty() || password.length < 6) {
            ownersPassword.error = "Password must be at least 6 characters."
            return false
        }

        return true
    }

    // Function to register owner account with Firebase Authentication
    private fun registerOwnerAccount(email: String, password: String, name: String, surname: String, businessName: String, phone: String, address: String) {
        // Show progress bar
        progressBar.visibility = ProgressBar.VISIBLE
        btnRegisterOwnerAccount.visibility = Button.GONE

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // User registered successfully, now save owner info to Firebase Database
                    val ownerId = firebaseAuth.currentUser?.uid // Get the UID of the registered user
                    auth.currentUser?.sendEmailVerification()
                    saveOwnerInfo(ownerId!!, name, surname, businessName, email, phone, address)
                } else {
                    // Failed to register user
                    progressBar.visibility = ProgressBar.GONE
                    btnRegisterOwnerAccount.visibility = Button.VISIBLE
                    Toast.makeText(this, "Failed to register owner account: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Function to save owner info to Firebase Database
    private fun saveOwnerInfo(ownerId: String, name: String, surname: String, businessName: String, email: String, phone: String, address: String) {
        // Get Firebase database reference
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("Users")

        // Query to find the business ID based on business name
        usersRef.orderByChild("BusinessInfo/businessName").equalTo(businessName)
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(dataSnapshot: com.google.firebase.database.DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Get the business ID
                        val businessId = dataSnapshot.children.first().key!!

                        // Upload the profile picture to Firebase Storage
                        val storageRef = FirebaseStorage.getInstance().getReference("profile_pics/$ownerId.jpg")
                        storageRef.putFile(selectedProfilePicUri!!).addOnSuccessListener {
                            storageRef.downloadUrl.addOnSuccessListener { uri ->
                                // Create a map for the owner data
                                val ownerData = mapOf(
                                    "name" to name,
                                    "surname" to surname,
                                    "profilePicUrl" to uri.toString(),
                                    "phone" to phone,
                                    "address" to address,
                                    "companyName" to businessName,
                                    "businessId" to businessId, // Added businessId field
                                    "email" to email,
                                    "role" to "owner",
                                    "approval" to "approved"
                                )

                                // Save the owner data under the business ID in the Employees node
                                usersRef.child(businessId).child("Employees").child(ownerId).setValue(ownerData)
                                    .addOnCompleteListener { task ->
                                        // Hide progress bar
                                        progressBar.visibility = ProgressBar.GONE
                                        btnRegisterOwnerAccount.visibility = Button.VISIBLE

                                        if (task.isSuccessful) {
                                            // Data saved successfully
                                            Toast.makeText(this@OwnerEnterInfoActivity, "Owner account created successfully!", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this@OwnerEnterInfoActivity, SuccessOwnerActivity::class.java))
                                        } else {
                                            // Failed to save data
                                            Toast.makeText(this@OwnerEnterInfoActivity, "Failed to create owner account!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        }.addOnFailureListener {
                            // Hide progress bar and show error message
                            progressBar.visibility = ProgressBar.GONE
                            btnRegisterOwnerAccount.visibility = Button.VISIBLE

                            Toast.makeText(this@OwnerEnterInfoActivity, "Failed to upload profile picture!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Hide progress bar and show error message
                        progressBar.visibility = ProgressBar.GONE
                        btnRegisterOwnerAccount.visibility = Button.VISIBLE
                        Toast.makeText(this@OwnerEnterInfoActivity, "Business not found!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: com.google.firebase.database.DatabaseError) {
                    // Hide progress bar and show error message
                    progressBar.visibility = ProgressBar.GONE
                    btnRegisterOwnerAccount.visibility = Button.VISIBLE
                    Toast.makeText(this@OwnerEnterInfoActivity, "Error finding business. Please try again.", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
