package com.example.example_google_upp.a2a

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallerValidatorTest {

    @Test
    fun `google wallet package devuelve true`() {
        assertTrue(CallerValidator.isGoogleWallet("com.google.android.gms"))
    }

    @Test
    fun `null devuelve false`() {
        assertFalse(CallerValidator.isGoogleWallet(null))
    }

    @Test
    fun `string vacio devuelve false`() {
        assertFalse(CallerValidator.isGoogleWallet(""))
    }

    @Test
    fun `otro package devuelve false`() {
        assertFalse(CallerValidator.isGoogleWallet("com.example.otra.app"))
    }

    @Test
    fun `package similar pero diferente devuelve false`() {
        assertFalse(CallerValidator.isGoogleWallet("com.google.android.gms.pay"))
        assertFalse(CallerValidator.isGoogleWallet("com.google.android.gm"))
        assertFalse(CallerValidator.isGoogleWallet("com.google.android"))
    }

    @Test
    fun `package malicioso devuelve false`() {
        assertFalse(CallerValidator.isGoogleWallet("com.malicious.app"))
        assertFalse(CallerValidator.isGoogleWallet("com.google.android.gms.fake"))
    }
}
