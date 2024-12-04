package com.opsc7311poe.xbcad_antoniemotors

import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity

class CameraCaptureActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE = 102
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Create a file to store the image
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Captured_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraImages/")
        }
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        // Launch the camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        }
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                // Return the URI of the captured image
                val resultIntent = Intent().apply {
                    putExtra("imageUri", imageUri.toString())
                }
                setResult(RESULT_OK, resultIntent)
            } else {
                // Remove the file entry if capture failed or was canceled
                imageUri?.let { contentResolver.delete(it, null, null) }
                setResult(RESULT_CANCELED)
            }
            finish()
        }
    }

    override fun onBackPressed() {
        // Handle back press
        imageUri?.let { contentResolver.delete(it, null, null) }
        setResult(RESULT_CANCELED)
        super.onBackPressed()
        finish()
    }
}




/*
package com.opsc7311poe.xbcad_antoniemotors

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity

class CameraCaptureActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK, data) // Return captured image data
            } else {
                setResult(resultCode) // Return any error or cancellation code as is
            }
            finish() // Close CameraCaptureActivity immediately
        }
    }

    override fun onBackPressed() {
        // Ensure proper handling of back press to avoid camera re-invoking
        setResult(RESULT_CANCELED)
        super.onBackPressed()
        finish()
    }
}

*/





