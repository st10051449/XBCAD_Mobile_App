package com.opsc7311poe.xbcad_antoniemotors

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.widget.*
import com.google.firebase.auth.*
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.database.FirebaseDatabase

class Login : AppCompatActivity() {

    private lateinit var pbLoad: ProgressBar
    private lateinit var btnLogin: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var email: TextView
    private lateinit var password: TextView
    private lateinit var signUp: TextView
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        pbLoad = findViewById(R.id.progressBar)
        btnLogin = findViewById(R.id.btnNextPage)
        email = findViewById(R.id.txtUsername)
        password = findViewById(R.id.txtPassword)
        signUp = findViewById(R.id.txtSignUp)

        signUp.setOnClickListener {
            val intent = Intent(this@Login, SelectRoleReg::class.java)
            startActivity(intent)
            finish()
        }

        btnLogin.setOnClickListener {
            val userEmail = email.text.toString().trim()
            val userPassword = password.text.toString().trim()

            if (userEmail.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            } else {
                pbLoad.visibility = View.VISIBLE
                btnLogin.visibility = View.GONE
                signIn(userEmail, userPassword)
            }
        }

        checkForBiometricSupport()
    }

//    private fun checkForBiometricSupport() {
//        val biometricManager = BiometricManager.from(this)
//        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
//            BiometricManager.BIOMETRIC_SUCCESS -> {
//                showBiometricPromptIfNotFirstLogin()
//            }
//            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
//            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
//            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
//            }
//        }
//    }

    private fun checkForBiometricSupport() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPromptIfNotFirstLogin()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Biometric is not supported or available
            }
        }
    }

    private fun showBiometricPromptIfNotFirstLogin() {
        val user = auth.currentUser
        user?.let {
            val metadata = it.metadata
            if (metadata != null && metadata.creationTimestamp != metadata.lastSignInTimestamp) {
                showBiometricPrompt()
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val user = auth.currentUser
                if (user != null) {
                    checkUserStatus(user.uid)
                } else {
                    Toast.makeText(this@Login, "No user logged in.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@Login, "Use your account details to login.", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@Login, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
        })

        // Build the prompt info without the NegativeButtonText when DEVICE_CREDENTIAL is allowed
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your fingerprint or face ID")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }


    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        checkUserStatus(user.uid)
                    }
                } else {
                    Toast.makeText(this, "Please enter correct credentials.", Toast.LENGTH_SHORT).show()
                    pbLoad.visibility = View.GONE
                    btnLogin.visibility = View.VISIBLE
                }
            }
    }


    private fun checkUserStatus(userId: String) {
        val usersRef = database.child("Users")


        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {


                    //saving business id to be used later in app

                    val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

                    val editor = sharedPref.edit()

                    editor.putString("business_id", userSnapshot.key)

                    editor.apply()

                    val employeeSnapshot = userSnapshot.child("Employees").child(userId)
                    if (employeeSnapshot.exists()) {
               val approvalStatus = employeeSnapshot.child("approval").getValue(String::class.java)
                        val role = employeeSnapshot.child("role").getValue(String::class.java)
                        redirectToMainActivity(role)
                        return
                    }
                }

                checkPendingStatus(userId)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Login, "Oh no! Something went wrong.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkPendingStatus(userId: String) {
        val usersRef = database.child("Users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val pendingSnapshot = userSnapshot.child("Pending").child(userId)
                    if (pendingSnapshot.exists()) {
                        val approvalStatus = pendingSnapshot.child("approval").getValue(String::class.java)
                        val role = pendingSnapshot.child("role").getValue(String::class.java)

                        when (approvalStatus) {
                            "pending" -> {
                                val intent = Intent(this@Login, WaitingActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            "denied" -> {
                                val intent = Intent(this@Login, DeniedActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            else -> {
                                Toast.makeText(this@Login, "Unknown approval status.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        return
                    }
                }
                Toast.makeText(this@Login, "User not found in database.", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
    fun validateLogin(email: String, password: String): Boolean {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this@Login, "Please enter an email", Toast.LENGTH_SHORT).show()
            return false
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this@Login, "Please enter a password", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }


    private fun redirectToMainActivity(role: String?) {
        val intent = Intent(this@Login, MainActivity::class.java)
        intent.putExtra("role", role)
        startActivity(intent)
        finish()
    }
}
