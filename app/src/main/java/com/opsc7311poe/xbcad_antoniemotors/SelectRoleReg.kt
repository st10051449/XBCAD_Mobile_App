package com.opsc7311poe.xbcad_antoniemotors

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SelectRoleReg : AppCompatActivity() {
    private lateinit var ivAdmin : ImageView
    private lateinit var ivEmployee : ImageView
    private lateinit var ivBusiness: ImageView

    private lateinit var txtAdmin : TextView
    private lateinit var txtEmployee : TextView
    private lateinit var txtBusiness: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_role_reg)

        ivAdmin = findViewById(R.id.ivAdminRole)
        ivEmployee = findViewById(R.id.ivEmployeeLogo)
        ivBusiness = findViewById(R.id.ivNewBusiness)
        txtAdmin = findViewById(R.id.txtAdminRole)
        txtEmployee = findViewById(R.id.txtemployeelogo)
        txtBusiness = findViewById(R.id.txtGoToRegBusiness)

        ivAdmin.setOnClickListener(){
            val intent = Intent(this@SelectRoleReg, AdminSelectBusinessActivity::class.java)
            startActivity(intent)
            finish()
        }

        txtAdmin.setOnClickListener(){
            val intent = Intent(this@SelectRoleReg, AdminSelectBusinessActivity::class.java)
            startActivity(intent)
            finish()

        }

        ivEmployee.setOnClickListener(){
            val intent = Intent(this@SelectRoleReg, EmpSelectBusinessAdminActivity::class.java)
            startActivity(intent)
            finish()
        }

        txtEmployee.setOnClickListener(){
            val intent = Intent(this@SelectRoleReg, EmpSelectBusinessAdminActivity::class.java)
            startActivity(intent)
            finish()
        }

        ivBusiness.setOnClickListener(){
            val intent = Intent(this@SelectRoleReg, RegisterBusinessActivity::class.java)
            startActivity(intent)
            finish()
        }

        txtBusiness.setOnClickListener(){
            val intent = Intent(this@SelectRoleReg, RegisterBusinessActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun navigateToAdminEnterInfoPage(role: String) {
        val intent: Intent = Intent(
            this@SelectRoleReg,
            AdminEnterInfo::class.java
        )
        intent.putExtra("role", role)
        startActivity(intent)
    }
}