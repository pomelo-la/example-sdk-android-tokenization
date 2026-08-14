package com.example.example_google_upp.wallet.models

import com.example.example_google_upp.model.AppToAppActivationResult
import org.junit.Assert.assertEquals
import org.junit.Test

class VisaAppToAppStepUpResponseTest {
    @Test
    fun `approved maps to approved`() {
        assertEquals(
            "approved",
            VisaAppToAppStepUpResponse.from(AppToAppActivationResult.Approved),
        )
    }

    @Test
    fun `declined maps to declined`() {
        assertEquals(
            "declined",
            VisaAppToAppStepUpResponse.from(AppToAppActivationResult.Declined),
        )
    }

    @Test
    fun `failed maps to failure regardless of message`() {
        assertEquals(
            "failure",
            VisaAppToAppStepUpResponse.from(AppToAppActivationResult.Failed("boom")),
        )
    }
}
