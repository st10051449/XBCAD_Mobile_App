package com.opsc7311poe.xbcad_antoniemotors

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SuccessOwnerActivity : AppCompatActivity() {
    private lateinit var lottieAnimationView: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_success)

        lottieAnimationView = findViewById(R.id.lottieAnimationView)

        // Add a listener for when the animation ends
        lottieAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                //val intent = Intent(this@SuccessOwnerActivity, MainActivity::class.java)

                // Pass a flag to MainActivity to indicate redirection to HomeFragment
                val intent = Intent(this@SuccessOwnerActivity, MainActivity::class.java).apply {
                    putExtra("redirectToHome", true) // Add a flag
                }
                startActivity(intent)
                finish()
            }
        })
    }



}
