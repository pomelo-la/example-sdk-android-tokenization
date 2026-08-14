package com.example.example_google_upp.data.dto

import com.example.example_google_upp.model.AppToAppActivationResult
import com.google.gson.annotations.SerializedName

internal data class AppToAppActivationResponseDto(
    @SerializedName("external_token_id") val externalTokenId: String?,
    @SerializedName("activation_result") val activationResult: String?,
) {
    fun toDomain(): AppToAppActivationResult =
        when (activationResult?.trim()?.uppercase()) {
            "APPROVED" -> AppToAppActivationResult.Approved
            "DECLINED" -> AppToAppActivationResult.Declined
            else ->
                AppToAppActivationResult.Failed(
                    "Backend returned an unexpected activation_result: $activationResult"
                )
        }
}
