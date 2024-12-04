package com.opsc7311poe.xbcad_antoniemotors

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChnagepasswordFragment : Fragment() {

    private lateinit var btnChangePassword: Button
    private lateinit var edtCurrentPassword: EditText
    private lateinit var edtNewPassword: EditText
    private lateinit var edtConfirmPassword: EditText

    private lateinit var btnBack: ImageView
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chnagepassword, container, false)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        btnBack = view.findViewById(R.id.ivBackButton)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        edtCurrentPassword = view.findViewById(R.id.txtCurrentPassword)
        edtNewPassword = view.findViewById(R.id.txtNewPassword)
        edtConfirmPassword = view.findViewById(R.id.txtConfirmPassword)

        // Back button listener
        btnBack.setOnClickListener {
            replaceFragment(SettingsFragment())
        }

        // Change password button listener
        btnChangePassword.setOnClickListener {
            val currentPassword = edtCurrentPassword.text.toString()
            val newPassword = edtNewPassword.text.toString()
            val confirmPassword = edtConfirmPassword.text.toString()

            if (validateInputs(currentPassword, newPassword, confirmPassword)) {
                changeUserPassword(currentPassword, newPassword)
            }
        }

        return view
    }

    private fun validateInputs(currentPassword: String, newPassword: String, confirmPassword: String): Boolean {
        if (currentPassword.isEmpty()) {
            edtCurrentPassword.error = "Current password is required"
            edtCurrentPassword.requestFocus()
            return false
        }

        if (newPassword.isEmpty()) {
            edtNewPassword.error = "New password is required"
            edtNewPassword.requestFocus()
            return false
        }

        if (confirmPassword.isEmpty()) {
            edtConfirmPassword.error = "Please confirm your new password"
            edtConfirmPassword.requestFocus()
            return false
        }

        if (newPassword != confirmPassword) {
            edtConfirmPassword.error = "Passwords do not match"
            edtConfirmPassword.requestFocus()
            return false
        }

        return true
    }

    private fun changeUserPassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser
        user?.let {
            val email = it.email
            if (email != null) {
                // Get credentials from the user
                val credential = EmailAuthProvider.getCredential(email, currentPassword)

                // Re-authenticate the user
                user.reauthenticate(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Change the password
                        user.updatePassword(newPassword).addOnCompleteListener { passwordChangeTask ->
                            if (passwordChangeTask.isSuccessful) {
                                Toast.makeText(requireContext(), "Password successfully changed", Toast.LENGTH_SHORT).show()
                                replaceFragment(SettingsFragment())
                            } else {
                                Toast.makeText(requireContext(), "Error changing password: ${passwordChangeTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Re-authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
