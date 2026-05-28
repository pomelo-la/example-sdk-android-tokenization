package com.example.example_google_upp.data.dto

import com.example.example_google_upp.model.ProvisioningData
import com.google.gson.annotations.SerializedName

internal data class ProvisioningCredentialsDto(
    @SerializedName("opc") val opaquePaymentCard: String,
    @SerializedName("google_opc") val googleOpaquePaymentCard: String,
) {
    fun toDomain(): ProvisioningData =
        ProvisioningData(
            opaquePaymentCard = opaquePaymentCard,
            googleOpaquePaymentCard = googleOpaquePaymentCard,
        )
}
