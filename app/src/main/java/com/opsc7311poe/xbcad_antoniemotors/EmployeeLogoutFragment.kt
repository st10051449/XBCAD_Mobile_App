package com.opsc7311poe.xbcad_antoniemotors

    import android.content.Context
    import android.content.Intent
    import android.net.ConnectivityManager
    import android.net.NetworkInfo
    import android.os.Bundle
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.Button
    import android.widget.ImageView
    import com.google.firebase.auth.FirebaseAuth
    import androidx.fragment.app.Fragment

    class EmployeeLogoutFragment : Fragment() {

        private lateinit var btnBack: ImageView
        private lateinit var btnYes: Button
        private lateinit var btnNo: Button

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
            val view = inflater.inflate(R.layout.fragment_employee_logout, container, false)

            btnBack = view.findViewById(R.id.ivBackButton)
            btnYes = view.findViewById(R.id.btnLogout)
            btnNo = view.findViewById(R.id.btnStayLoggedIn)

            btnBack.setOnClickListener {
                replaceFragment(EmpSettingsFragment())
            }

            btnYes.setOnClickListener {
                logoutUser()
            }

            btnNo.setOnClickListener {
                replaceFragment(EmployeeHomeFragment())
            }

            return view
        }

        private fun logoutUser() {
            auth.signOut()
            val intent = Intent(activity, Login::class.java)
            startActivity(intent)
            activity?.finish()
        }
        fun isOnline(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
            return activeNetwork?.isConnected == true
        }
        private fun replaceFragment(fragment: Fragment) {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_container, fragment)
                .commit()
        }
    }
