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

    class EmployeeChangePasswordFragment : Fragment() {

        private lateinit var btnChangeEmpPassword: Button
        private lateinit var edtCurrentEmpPassword: EditText
        private lateinit var edtNewEmpPassword: EditText
        private lateinit var edtConfirmEmpPassword: EditText

        private lateinit var btnBack: ImageView
        private lateinit var auth: FirebaseAuth

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            // Inflate the layout for this fragment
            val view = inflater.inflate(R.layout.fragment_employee_change_password, container, false)

            // Initialize FirebaseAuth
            auth = FirebaseAuth.getInstance()

            // Initialize views
            btnBack = view.findViewById(R.id.ivBackButton)
            btnChangeEmpPassword = view.findViewById(R.id.btnChangeEmpPassword)
            edtCurrentEmpPassword = view.findViewById(R.id.txtCurrentEmpPassword)
            edtNewEmpPassword = view.findViewById(R.id.txtNewEmpPassword)
            edtConfirmEmpPassword = view.findViewById(R.id.txtConfirmEmpPassword)

            // Back button listener
            btnBack.setOnClickListener {
                replaceFragment(EmpSettingsFragment())
            }

            // Change password button listener
            btnChangeEmpPassword.setOnClickListener {
                val currentPassword = edtCurrentEmpPassword.text.toString()
                val newPassword = edtNewEmpPassword.text.toString()
                val confirmPassword = edtConfirmEmpPassword.text.toString()

                if (validateInputs(currentPassword, newPassword, confirmPassword)) {
                    changeUserPassword(currentPassword, newPassword)
                }
            }

            return view
        }

        private fun validateInputs(currentPassword: String, newPassword: String, confirmPassword: String): Boolean {
            if (currentPassword.isEmpty()) {
                edtCurrentEmpPassword.error = "Current password is required"
                edtCurrentEmpPassword.requestFocus()
                return false
            }

            if (newPassword.isEmpty()) {
                edtNewEmpPassword.error = "New password is required"
                edtNewEmpPassword.requestFocus()
                return false
            }

            if (confirmPassword.isEmpty()) {
                edtConfirmEmpPassword.error = "Please confirm your new password"
                edtConfirmEmpPassword.requestFocus()
                return false
            }

            if (newPassword != confirmPassword) {
                edtConfirmEmpPassword.error = "Passwords do not match"
                edtConfirmEmpPassword.requestFocus()
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
                                    replaceFragment(EmpSettingsFragment())
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
