package com.opsc7311poe.xbcad_antoniemotors

    import android.content.Intent
    import android.os.Bundle
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.Button
    import android.widget.EditText
    import android.widget.ImageView
    import android.widget.Toast
    import androidx.fragment.app.Fragment
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.auth.EmailAuthProvider

    class EmpDeleteAccountFragment : Fragment() {

        private lateinit var txtConfirmDelete: EditText
        private lateinit var btnConfirm: Button
        private lateinit var btnCancel: Button
        private lateinit var ivBackButton: ImageView
        private lateinit var auth: FirebaseAuth

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance()
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            // Inflate the layout for this fragment
            val view = inflater.inflate(R.layout.fragment_emp_delete_account, container, false)

            // Find views
            txtConfirmDelete = view.findViewById(R.id.txtconfirmdelete)
            btnConfirm = view.findViewById(R.id.btnConfirm)
            btnCancel = view.findViewById(R.id.btnCancel)
            ivBackButton = view.findViewById(R.id.ivBackButton)

            // Set up listeners
            btnConfirm.setOnClickListener {
                val password = txtConfirmDelete.text.toString().trim()
                if (password.isNotEmpty()) {
                    reauthenticateAndDeleteAccount(password)
                } else {
                    Toast.makeText(requireContext(), "Please enter your password", Toast.LENGTH_SHORT).show()
                }
            }

            btnCancel.setOnClickListener {
                navigateToEmpSettingsFragment()
            }

            ivBackButton.setOnClickListener {
                navigateToEmpSettingsFragment()
            }

            return view
        }

        private fun reauthenticateAndDeleteAccount(password: String) {
            val user = auth.currentUser
            if (user != null && user.email != null) {
                // Re-authenticate the user
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Delete the account
                            deleteAccount()
                        } else {
                            Toast.makeText(requireContext(), "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        private fun deleteAccount() {
            val user = auth.currentUser
            user?.delete()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show()
                    navigateToLoginActivity()
                } else {
                    Toast.makeText(requireContext(), "Failed to delete account. Please try again later.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun navigateToEmpSettingsFragment() {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_container, EmpSettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        private fun navigateToLoginActivity() {
            val intent = Intent(requireContext(), Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish() //Closes the DeleteAcc fragment's activity as well
        }
    }

