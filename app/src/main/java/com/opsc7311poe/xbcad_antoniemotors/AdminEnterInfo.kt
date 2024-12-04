package com.opsc7311poe.xbcad_antoniemotors

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class AdminEnterInfo : AppCompatActivity() {

    private lateinit var txtBusinessName: EditText
    private lateinit var txtAdminFirstName: EditText
    private lateinit var txtAdminLastName: EditText
    private lateinit var txtAdminEmail: EditText
    private lateinit var txtAdminPassword: EditText
    private lateinit var ivProfileImage: ImageView
    private lateinit var txtSelectProfileImage: TextView
    private lateinit var txtAdminPhone: EditText
    private lateinit var txtAdminAddress: EditText
    private var selectedImageUri: Uri? = null
    private lateinit var btnRegisterAdmin: Button
    private lateinit var progressBar: ProgressBar

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            displayImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_admin_enter_info)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        // Initialize views
        txtBusinessName = findViewById(R.id.txtAdminBusinessName)
        txtAdminFirstName = findViewById(R.id.txtAdminFirstName)
        txtAdminLastName = findViewById(R.id.txtAdminLastName)
        txtAdminEmail = findViewById(R.id.txtAdminEmail)
        txtAdminPassword = findViewById(R.id.txtPassword)
        ivProfileImage = findViewById(R.id.ivAdminProfilePicture)
        txtSelectProfileImage = findViewById(R.id.txtSelectImage)
        btnRegisterAdmin = findViewById(R.id.btnRegisterAdmin)
        txtAdminAddress = findViewById(R.id.txtAdminPAddress)
        txtAdminPhone = findViewById(R.id.txtAdminPhone)
        progressBar = findViewById(R.id.progressBar)

        txtSelectProfileImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Retrieve the business name from the previous intent
        val businessName = intent.getStringExtra("selectedBusinessName")
        txtBusinessName.setText(businessName)

        btnRegisterAdmin.setOnClickListener {

            if (!validateData()) { //if amy of the validations fail over here

                return@setOnClickListener
            }

            val firstName = txtAdminFirstName.text.toString()
            val lastName = txtAdminLastName.text.toString()
            val email = txtAdminEmail.text.toString()
            val password = txtAdminPassword.text.toString()
            val phone = txtAdminPhone.text.toString()
            val address = txtAdminAddress.text.toString()

            if (businessName.isNullOrEmpty() || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Hide register button and show progress bar
            btnRegisterAdmin.visibility = Button.INVISIBLE
            progressBar.visibility = ProgressBar.VISIBLE

            // Create the admin account in Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        auth.currentUser?.sendEmailVerification()
                        retrieveBusinessIdAndOwnerId(businessName) { ownerId, businessId ->
                            if (businessId != null) {
                                saveAdminInfoToFirebase(userId, ownerId, businessId, businessName, firstName, lastName, email, phone, address)
                            } else {
                                Toast.makeText(this, "Failed to retrieve business or owner ID", Toast.LENGTH_SHORT).show()
                                btnRegisterAdmin.visibility = Button.VISIBLE
                                progressBar.visibility = ProgressBar.GONE
                            }
                        }
                    } else {
                        btnRegisterAdmin.visibility = Button.VISIBLE
                        progressBar.visibility = ProgressBar.GONE
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun validateData(): Boolean {
        var isValid = true

        // Validate each field and show a Toast message if any field is empty or invalid
        if (txtBusinessName.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter the name of your business!", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (txtAdminFirstName.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter your first name!", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (txtAdminLastName.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter your last name!", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (txtAdminEmail.text.toString().trim().isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(txtAdminEmail.text.toString()).matches()) {
            Toast.makeText(this, "Please enter a valid email address!", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (txtAdminPassword.text.toString().trim().isEmpty() || txtAdminPassword.text.toString().length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long!", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (txtAdminPhone.text.toString().trim().isEmpty() || !txtAdminPhone.text.toString().matches(Regex("\\d{10,}"))) {
            Toast.makeText(this, "Please enter a valid phone number!", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (txtAdminAddress.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter your address!", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select a profile image!", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }


    private fun displayImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .into(ivProfileImage)
    }

    private fun retrieveBusinessIdAndOwnerId(businessName: String, callback: (String?, String?) -> Unit) {
        database.child("Users").orderByChild("BusinessInfo/businessName").equalTo(businessName)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val businessId = userSnapshot.key

                        // Look for the employee with the role "owner"
                        val employeesSnapshot = userSnapshot.child("Employees").children
                        for (employee in employeesSnapshot) {
                            val role = employee.child("role").getValue(String::class.java)
                            if (role == "owner") {
                                val ownerId = employee.key
                                callback(ownerId, businessId)
                                return@addOnSuccessListener
                            }
                        }
                        callback(null, businessId) // No owner found, pass null for ownerId
                        return@addOnSuccessListener
                    }
                } else {
                    Log.d("FirebaseDebug", "No business found with the name '$businessName'.")
                    callback(null, null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDebug", "Error retrieving business data: ${e.message}")
                callback(null, null)
            }
    }

    private fun saveAdminInfoToFirebase(userId: String?, ownerId: String?, businessId: String?, businessName: String, firstName: String, lastName: String, email: String, phone: String, address: String) {
        if (userId != null && businessId != null) {
            val adminInfo = hashMapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "phone" to phone,
                "address" to address,
                "role" to "admin",
                "approval" to "pending",
                "companyName" to businessName,
                "businessID" to businessId,
                "managerID" to ownerId // Add the owner ID if available
            )

            database.child("Users").child(businessId).child("Pending").child(userId).setValue(adminInfo)
                .addOnSuccessListener {
                    Toast.makeText(this, "Admin registered successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@AdminEnterInfo, SuccessActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    btnRegisterAdmin.visibility = Button.VISIBLE
                    progressBar.visibility = ProgressBar.GONE
                    Toast.makeText(this, "Failed to save admin info: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            btnRegisterAdmin.visibility = Button.VISIBLE
            progressBar.visibility = ProgressBar.GONE
            Toast.makeText(this, "Failed to retrieve business ID", Toast.LENGTH_SHORT).show()
        }
    }
}