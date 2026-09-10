package com.example.example_google_upp.a2a.models

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.Base64

/**
 * Payload de Visa para App-to-App Verification (A2A).
 *
 * Google Wallet invoca esta Activity con un Intent que contiene el payload de Visa
 * en formato JSON codificado en Base64URL en el campo [android.content.Intent.EXTRA_TEXT].
 *
 * Docs:
 * https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv
 */
data class VisaA2aPayload(
    @SerializedName("panReferenceID")
    val panReferenceID: String? = null,
    @SerializedName("tokenRequestorID")
    val tokenRequestorID: String? = null,
    @SerializedName("tokenReferenceID")
    val tokenReferenceID: String? = null,
    @SerializedName("panLast4")
    val panLast4: String? = null,
    @SerializedName("deviceID")
    val deviceID: String? = null,
    @SerializedName("walletAccountID")
    val walletAccountID: String? = null,
) {
    companion object {
        /**
         * Parsea el payload de Visa desde el string Base64URL recibido en Intent.EXTRA_TEXT.
         *
         * Usa Java Base64 que está disponible tanto en Android (API 26+) como en JVM de tests.
         *
         * @param extraText El valor de Intent.EXTRA_TEXT (JSON en Base64URL), o null si no está presente.
         * @param gson Instancia de Gson para el parseo (por defecto crea una nueva).
         * @return El [VisaA2aPayload] parseado, o null si el input es inválido o nulo.
         */
        fun fromExtraText(extraText: String?, gson: Gson = Gson()): VisaA2aPayload? {
            if (extraText.isNullOrBlank()) {
                return null
            }

            return try {
                // Usar Java Base64 URL decoder (disponible en Android API 26+ y JVM)
                val decoded = Base64.getUrlDecoder().decode(extraText)
                val json = String(decoded, Charsets.UTF_8)
                gson.fromJson(json, VisaA2aPayload::class.java)
            } catch (e: IllegalArgumentException) {
                // Base64 inválido
                null
            } catch (e: Exception) {
                // JSON malformado u otros errores
                null
            }
        }
    }
}
