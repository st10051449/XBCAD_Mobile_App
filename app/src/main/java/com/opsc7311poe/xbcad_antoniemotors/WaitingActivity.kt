package com.opsc7311poe.xbcad_antoniemotors

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class WaitingActivity : AppCompatActivity() {

    private lateinit var btnLogout: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_waiting)

        btnLogout = findViewById(R.id.btnLogout)

        btnLogout.setOnClickListener(){
            logoutUser()
        }

    }

    fun logoutUser() {
        // Get the Firebase Auth instance
        val auth = FirebaseAuth.getInstance()
        auth.signOut()
        startActivity(Intent(this@WaitingActivity, Login::class.java))
        finish()
    }

}