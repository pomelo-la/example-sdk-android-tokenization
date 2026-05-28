package com.example.example_google_upp.data.dto

import com.example.example_google_upp.model.Card
import com.example.example_google_upp.model.Brand
import com.google.gson.annotations.SerializedName

internal data class ProvisioningRequestDto(
    @SerializedName("card_id") val cardId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("server_session_id") val serverSessionId: String,
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("wallet_account_id") val walletAccountId: String? = null,
) {
    companion object {
        fun from(
            card: Card,
            serverSessionId: String,
            deviceId: String,
            walletAccountId: String,
        ): ProvisioningRequestDto {
            val isVisa = card.brand == Brand.VISA

            return ProvisioningRequestDto(
                cardId = card.cardId,
                userId = card.userId,
                serverSessionId =
                    if (isVisa) serverSessionId.requiredVisaValue("serverSessionId")
                    else serverSessionId,
                deviceId = if (isVisa) deviceId.requiredVisaValue("deviceId") else null,
                walletAccountId =
                    if (isVisa) walletAccountId.requiredVisaValue("walletAccountId") else null,
            )
        }

        private fun String.requiredVisaValue(fieldName: String): String =
            takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Visa provisioning requires $fieldName")
    }
}
