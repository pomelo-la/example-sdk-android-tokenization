package com.example.example_google_upp.wallet.models

import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Only covers the guard clauses here: decoding a real Base64URL payload needs `android.util.Base64`,
 * which isn't mocked in plain JVM unit tests. The happy path is covered by the instrumented test
 * `VisaAppToAppPayloadInstrumentedTest` (androidTest), which runs on a real Android runtime.
 */
class VisaAppToAppPayloadTest {
    @Test
    fun `null extra text returns null`() {
        assertNull(VisaAppToAppPayload.fromExtraText(null))
    }

    @Test
    fun `blank extra text returns null`() {
        assertNull(VisaAppToAppPayload.fromExtraText("   "))
    }

    @Test
    fun `malformed extra text returns null instead of throwing`() {
        assertNull(VisaAppToAppPayload.fromExtraText("not-a-valid-payload"))
    }
}
