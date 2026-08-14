package com.example.example_google_upp.wallet.models

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Payload Visa sends in the App2App (IDV) Intent's `EXTRA_TEXT`: an opaque, Base64URL-encoded JSON
 * blob (never PCI/auth data, per Google's App2App verification guidance).
 *
 * Fields mirror Visa's App2App documentation (Table 2-1): panReferenceID, tokenRequestorID,
 * tokenReferenceID, panLast4, deviceID and walletAccountID. All are nullable because Google Wallet
 * does not guarantee every field is always present, and a malformed/missing payload should surface
 * as an error in the UI rather than crash the Activity.
 */
data class VisaAppToAppPayload(
    @SerializedName("panReferenceID") val panReferenceId: String? = null,
    @SerializedName("tokenRequestorID") val tokenRequestorId: String? = null,
    @SerializedName("tokenReferenceID") val tokenReferenceId: String? = null,
    @SerializedName("panLast4") val panLast4: String? = null,
    @SerializedName("deviceID") val deviceId: String? = null,
    @SerializedName("walletAccountID") val walletAccountId: String? = null,
) {
    companion object {
        /** Decodes Base64URL + JSON from `Intent.EXTRA_TEXT`, or null if it's missing/malformed. */
        fun fromExtraText(extraText: String?, gson: Gson = Gson()): VisaAppToAppPayload? {
            if (extraText.isNullOrBlank()) return null

            return runCatching {
                    val decodedJson =
                        String(
                            Base64.decode(extraText, Base64.URL_SAFE or Base64.NO_WRAP),
                            Charsets.UTF_8,
                        )
                    gson.fromJson(decodedJson, VisaAppToAppPayload::class.java)
                }
                .getOrNull()
        }
    }
}
