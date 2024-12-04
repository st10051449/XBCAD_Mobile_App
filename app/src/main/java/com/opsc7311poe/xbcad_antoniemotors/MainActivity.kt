package com.opsc7311poe.xbcad_antoniemotors

import Leave
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.android.material.badge.BadgeDrawable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
//Group Name : Tech Fusion
//Members:
/*Perla Jbara – ST10022447
 Gabriella Janssen – ST10034968
 Mauro Coelho – ST10080441
 Lee Knowles -ST10051449
 Daniel Antonie – ST10186731*/


class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavView: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var businessId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        businessId = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("business_id", null)!!

        bottomNavView = findViewById(R.id.bottom_navigation)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Check if there's a redirect flag
        val redirectToHome = intent.getBooleanExtra("redirectToHome", false)

        // Check if the user is logged in
        val user = auth.currentUser
        if (user != null) {
            // User is logged in, fetch the role and approval status
            getUserDetails(user.uid)
            checkPendingRequests() //this is for the notification thingie

            // Redirect to HomeFragment if flagged from SuccessOwnerActivity
            if (redirectToHome) {
                replaceFragment(HomeFragment())
            }
        } else {
            Toast.makeText(this, "You're not logged in!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@MainActivity, Login::class.java)
            startActivity(intent)
            finish()
        }

        //old method
       /* } else {
            Toast.makeText(this,"You're not logged in!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@MainActivity, Login ::class.java)
            startActivity(intent)
            finish()
        }*/


        // Set up the bottom navigation listener
        bottomNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navHome -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.navDocument -> {
                    replaceFragment(DocumentationFragment())
                    true
                }
                R.id.navEmployees -> {
                 //   replaceFragment(EmployeeFragment())
                 //replaceFragment(AdminApproveRegistrationFragment())
                 replaceFragment(AdminEmpFragment())
                    true
                }
                R.id.navVehicles -> {
                    replaceFragment(VehicleMenuFragment())
                    true
                }
                R.id.navCustomers -> {
                    replaceFragment(CustomerMenuFragment())
                    true
                }
                R.id.navTasks -> {
                    replaceFragment(TasksFragment()) // For employee role
                    true
                }
                R.id.navEmpHome -> {
                    replaceFragment(EmployeeHomeFragment()) // For employee role
                    true
                }
                R.id.navLeave -> {
                    replaceFragment(EMPLeavemenu()) // For employee role
                    true
                }
                R.id.navMessages -> {
                    replaceFragment(EmpProfileFragment()) // For employee role
                    true
                }
                R.id.navLeaderboard -> {
                    replaceFragment(LeaderboardFragment()) // For employee role
                    true
                }
                else -> false
            }
        }

    }

    private fun showBadge(count: Int) {
        val badgeDrawable = bottomNavView.getOrCreateBadge(R.id.navEmployees)
        badgeDrawable.isVisible = true
        badgeDrawable.number = count
    }

    private fun removeBadge() {
        val badgeDrawable = bottomNavView.getBadge(R.id.navEmployees)
        badgeDrawable?.isVisible = false
    }
    private fun checkPendingRequests() {
        // Use the businessId already fetched from SharedPreferences
        val pendingRef = database.child("Users").child(businessId).child("Pending")

        // Fetch and count the pending requests
        pendingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pendingCount = snapshot.childrenCount.toInt()
                if (pendingCount > 0) {

                    showBadge(pendingCount)
                } else {
                    // Remove the badge if there are no pending requests
                    removeBadge()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }



    private fun getUserDetails(userId: String) {
        // Get the role and approval status from Firebase Realtime Database
        val userRef = database.child("Users").child(businessId).child("Employees").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userRole = snapshot.child("role").getValue(String::class.java)
                val approvalStatus = snapshot.child("approval").getValue(String::class.java)

                if (userRole != null && approvalStatus != null) {
                    when (approvalStatus) {
                        "approved" -> {
                            // Load the appropriate navigation bar based on the role
                            loadNavigationMenu(userRole)

                            // Set the default fragment
                            when (userRole.lowercase()) {
                                "admin", "owner" -> replaceFragment(HomeFragment()) // For admin/owner
                                "employee" -> replaceFragment(EmployeeHomeFragment()) // For employee
                            }
                        }
                        "pending" -> {
                            // Redirect to WaitingActivity
                            val intent = Intent(this@MainActivity, WaitingActivity::class.java)
                            startActivity(intent)
                            finish() // Close the MainActivity
                        }
                        "denied" -> {
                            // Redirect to DeniedActivity
                            val intent = Intent(this@MainActivity, DeniedActivity::class.java)
                            startActivity(intent)
                            finish() // Close the MainActivity
                        }
                        else -> {
                        }
                    }
                } else {
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun loadNavigationMenu(userRole: String) {
        // Load the appropriate menu based on the role
        when (userRole.lowercase()) {
            "admin", "owner" -> {
                bottomNavView.menu.clear()
                bottomNavView.inflateMenu(R.menu.menu) // Load admin/owner menu
            }
            "employee" -> {
                bottomNavView.menu.clear()
                bottomNavView.inflateMenu(R.menu.empmenu) // Load employee menu
                resetCompletedTasks()
                assignAndCheckLeave()
            }
            else -> {
                bottomNavView.menu.clear()
                bottomNavView.inflateMenu(R.menu.empmenu) // Load a default menu for unknown roles
            }
        }
    }

    fun assignAndCheckLeave() {
        val userId = auth.currentUser?.uid ?: return
        val leaveRef = FirebaseDatabase.getInstance()
            .reference
            .child("Users")
            .child(businessId)
            .child("Employees")
            .child(userId)
            .child("Leave")

        // Check if Leave node already exists
        leaveRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                // Assign default leave types if the node does not exist
                leaveRef.setValue(defaultLeaveTypes()).addOnSuccessListener {
                    checkAndRenewLeave(userId, businessId)
                }.addOnFailureListener { e ->
                    // Handle failure
                }
            } else {
                // Only check and renew leave if the node exists
                checkAndRenewLeave(userId, businessId)
            }
        }.addOnFailureListener {
            // Handle error if needed
        }
    }


    fun checkAndRenewLeave(userId: String, businessId: String) {
        val database = FirebaseDatabase.getInstance().reference
        val leaveRef = database.child("Users").child(businessId).child("Employees").child(userId).child("Leave")

        leaveRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                snapshot.children.forEach { leaveType ->
                    val leave = leaveType.getValue(Leave::class.java)
                    leave?.let {
                        val expiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.expiryDate)
                        val expiryYear = Calendar.getInstance().apply { time = expiryDate }.get(Calendar.YEAR)

                        // If it's a new year and leave has expired
                        if (currentYear > expiryYear) {
                            val newLeaveDays =
                                (it.leaveDays + defaultLeaveTypes()[leaveType.key]?.leaveDays!!)
                                    ?: 0

                            // Update with renewed leave days and set new expiry date
                            leaveRef.child(leaveType.key!!).setValue(
                                Leave(
                                    leaveDays = newLeaveDays,
                                    dateAssigned = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    expiryDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                        calendar.add(Calendar.YEAR, 1)
                                    }.format(Date())
                                )
                            ).addOnSuccessListener {
                            }.addOnFailureListener { e ->
                            }
                        }
                    }
                }
            }
        }.addOnFailureListener { e ->
        }
    }


//300 commits yay!
    fun resetCompletedTasks() {
        // Get today's date
        val today = Calendar.getInstance()
        val dayOfMonth = today.get(Calendar.DAY_OF_MONTH)


        if (dayOfMonth == 1) {
            val database = FirebaseDatabase.getInstance()
            val employeesRef = database.getReference("Users/$businessId/Employees")

            // Retrieve employees and update completedTasks
            employeesRef.get().addOnSuccessListener { snapshot ->
                for (employeeSnapshot in snapshot.children) {
                    // Check if the employee has a completedTasks field
                    val completedTasks = employeeSnapshot.child("completedTasks").value as? Long
                    if (completedTasks != null) {
                        // Set completedTasks to 0
                        employeeSnapshot.ref.child("completedTasks").setValue(0)
                    }
                }
            }.addOnFailureListener {
                // Handle any errors here
            }
        }
    }


    fun defaultLeaveTypes(): Map<String, Leave> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val calendar = Calendar.getInstance()

        return mapOf(
            // Sick Leave expires in 36 months (3 years)
            "Sick Leave" to Leave(
                leaveDays = 30,
                dateAssigned = today,
                expiryDate = calendar.apply {
                    time = Date()
                    add(Calendar.MONTH, 36)
                }.time.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) }
            ),

            // Parental Leave expires in 12 months (1 year)
            "Parental Leave" to Leave(
                leaveDays = 10,
                dateAssigned = today,
                expiryDate = calendar.apply {
                    time = Date()
                    add(Calendar.YEAR, 1)
                }.time.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) }
            ),

            // Study Leave expires in 12 months (1 year)
            "Study Leave" to Leave(
                leaveDays = 5,
                dateAssigned = today,
                expiryDate = calendar.apply {
                    time = Date()
                    add(Calendar.YEAR, 1)
                }.time.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) }
            ),

            // Bereavement Leave expires in 6 months
            "Bereavement Leave" to Leave(
                leaveDays = 3,
                dateAssigned = today,
                expiryDate = calendar.apply {
                    time = Date()
                    add(Calendar.MONTH, 6)
                }.time.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) }
            ),

            // Annual Leave expires in 18 months
            "Annual Leave" to Leave(
                leaveDays = 15,
                dateAssigned = today,
                expiryDate = calendar.apply {
                    time = Date()
                    add(Calendar.MONTH, 18)
                }.time.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) }
            ),

            // Maternity Leave expires in 12 months
            "Maternity Leave" to Leave(
                leaveDays = 120,
                dateAssigned = today,
                expiryDate = calendar.apply {
                    time = Date()
                    add(Calendar.YEAR, 1)
                }.time.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) }
            ),

            // Unpaid Leave (no expiration, using an arbitrary far future date)
            "Unpaid Leave" to Leave(
                leaveDays = 0,
                dateAssigned = today,
                expiryDate = "2099-12-31"
            ),

            // Family Responsibility Leave expires in 12 months
            "Family Responsibility Leave" to Leave(
                leaveDays = 3,
                dateAssigned = today,
                expiryDate = calendar.apply {
                    time = Date()
                    add(Calendar.YEAR, 1)
                }.time.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) }
            ),

            // Religious Leave expires in 12 months
            "Religious Leave" to Leave(
                leaveDays = 5,
                dateAssigned = today,
                expiryDate = calendar.apply {
                    time = Date()
                    add(Calendar.YEAR, 1)
                }.time.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) }
            )
        )
    }



    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_slide_in_right,   // Enter animation
                R.anim.fragment_slide_out_left,   // Exit animation
                R.anim.fragment_slide_in_left,    // Pop enter animation
                R.anim.fragment_slide_out_right   // Pop exit animation
            )
            .replace(R.id.frame_container, fragment)
            .commit()
    }

    /*private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .addToBackStack(null)
            .commit()
    }*/
}
