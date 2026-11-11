package com.shieldguard.app.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpGuardTest {

    @Test
    fun `classic numeric otp is protected`() {
        assertTrue(OtpGuard.isOtpOrTransactional("Your OTP is 849201. Do not share with anyone."))
    }

    @Test
    fun `alphanumeric verification code is protected`() {
        assertTrue(OtpGuard.isOtpOrTransactional("Verification code: A1B2C3"))
    }

    @Test
    fun `bank transaction alert is protected`() {
        assertTrue(OtpGuard.isOtpOrTransactional("Rs.5000 debited by transfer to acct ending 4412"))
    }

    @Test
    fun `plain order confirmation is not treated as otp`() {
        assertFalse(OtpGuard.isOtpOrTransactional("Your order 12345 has shipped"))
    }

    @Test
    fun `promotional message with digits is not treated as otp`() {
        assertFalse(OtpGuard.isOtpOrTransactional("Flat 50% off on all orders this weekend"))
    }

    @Test
    fun `security wording without a code is not treated as otp`() {
        assertFalse(OtpGuard.isOtpOrTransactional("Your OTP is invalid, please try again"))
    }

    @Test
    fun `blank text is not otp`() {
        assertFalse(OtpGuard.isOtpOrTransactional("   "))
    }

    @Test
    fun `nine digit number is not mistaken for a code`() {
        assertFalse(OtpGuard.isOtpOrTransactional("Call me back on 123456789 about the OTP"))
    }
}
