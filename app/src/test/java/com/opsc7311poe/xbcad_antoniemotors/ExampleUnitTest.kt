package com.opsc7311poe.xbcad_antoniemotors


import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class LoginUnitTest {

    private lateinit var loginActivity: Login
    private lateinit var mAuthMock: FirebaseAuth

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        loginActivity = Mockito.mock(Login::class.java)
        mAuthMock = Mockito.mock(FirebaseAuth::class.java)
    }

    @Test
    fun `test valid email and password`() {
        val email = "testuser@example.com"
        val password = "validPassword"

        Mockito.`when`(loginActivity.validateLogin(email, password)).thenReturn(true)
        assertTrue(loginActivity.validateLogin(email, password))
    }

    @Test
    fun `test empty email`() {
        val email = ""
        val password = "validPassword"

        Mockito.`when`(loginActivity.validateLogin(email, password)).thenReturn(false)
        assertFalse(loginActivity.validateLogin(email, password))
    }

    @Test
    fun `test empty password`() {
        val email = "testuser@example.com"
        val password = ""

        Mockito.`when`(loginActivity.validateLogin(email, password)).thenReturn(false)
        assertFalse(loginActivity.validateLogin(email, password))
    }

    @Test
    fun `test short password`() {
        val email = "testuser@example.com"
        val password = "123"

        Mockito.`when`(loginActivity.validateLogin(email, password)).thenReturn(false)
        assertFalse(loginActivity.validateLogin(email, password))
    }
}