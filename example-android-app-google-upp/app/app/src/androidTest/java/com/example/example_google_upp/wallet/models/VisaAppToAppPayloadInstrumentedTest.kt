package com.example.example_google_upp.wallet.models

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real Android runtime because decoding relies on `android.util.Base64`, which is
 * native-backed and not mocked in plain JVM unit tests.
 */
@RunWith(AndroidJUnit4::class)
class VisaAppToAppPayloadInstrumentedTest {
    @Test
    fun decodesValidBase64UrlPayload() {
        val json =
            """
            {
              "panReferenceID": "pan-ref-123",
              "tokenRequestorID": "trid-123",
              "tokenReferenceID": "token-ref-123",
              "panLast4": "4242",
              "deviceID": "device-123",
              "walletAccountID": "wallet-123"
            }
            """
                .trimIndent()
        val extraText = Base64.encodeToString(json.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)

        val payload = VisaAppToAppPayload.fromExtraText(extraText, Gson())

        assertEquals("pan-ref-123", payload?.panReferenceId)
        assertEquals("trid-123", payload?.tokenRequestorId)
        assertEquals("token-ref-123", payload?.tokenReferenceId)
        assertEquals("4242", payload?.panLast4)
        assertEquals("device-123", payload?.deviceId)
        assertEquals("wallet-123", payload?.walletAccountId)
    }
}
