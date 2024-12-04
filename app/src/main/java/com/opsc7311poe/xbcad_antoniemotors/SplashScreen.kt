package com.opsc7311poe.xbcad_antoniemotors

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        splashScreen.setKeepOnScreenCondition {
            true // The system will handle the duration based on the theme's animation duration
        }

        // Ensure the splash screen stays on for 3500 ms
        Handler(Looper.getMainLooper()).postDelayed({
            // After 3500 ms, start the MainActivity
            startActivity(Intent(this, Login::class.java))
            finish() // Finish SplashScreen activity
        }, 3500) // Duration of the splash screen animation




    }
}