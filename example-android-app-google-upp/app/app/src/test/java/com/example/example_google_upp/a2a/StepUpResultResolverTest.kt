package com.example.example_google_upp.a2a

import android.content.Intent
import com.example.example_google_upp.a2a.models.StepUpResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StepUpResultResolverTest {

    @Test
    fun `Approved devuelve extra STEP_UP_RESPONSE igual a approved`() {
        val intent = StepUpResultResolver.toResultIntent(StepUpResult.Approved)

        assertEquals("approved", intent.getStringExtra("STEP_UP_RESPONSE"))
    }

    @Test
    fun `Declined devuelve extra STEP_UP_RESPONSE igual a declined`() {
        val intent = StepUpResultResolver.toResultIntent(StepUpResult.Declined)

        assertEquals("declined", intent.getStringExtra("STEP_UP_RESPONSE"))
    }

    @Test
    fun `Failure devuelve extra STEP_UP_RESPONSE igual a failure`() {
        val intent = StepUpResultResolver.toResultIntent(StepUpResult.Failure("Network error"))

        assertEquals("failure", intent.getStringExtra("STEP_UP_RESPONSE"))
    }

    @Test
    fun `Failure con mensaje nulo devuelve failure`() {
        val intent = StepUpResultResolver.toResultIntent(StepUpResult.Failure(null))

        assertEquals("failure", intent.getStringExtra("STEP_UP_RESPONSE"))
    }

    @Test
    fun `Failure con mensaje vacio devuelve failure`() {
        val intent = StepUpResultResolver.toResultIntent(StepUpResult.Failure(""))

        assertEquals("failure", intent.getStringExtra("STEP_UP_RESPONSE"))
    }
}
