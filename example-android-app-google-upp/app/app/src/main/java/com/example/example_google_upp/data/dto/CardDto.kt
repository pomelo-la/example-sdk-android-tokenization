package com.example.example_google_upp.data.dto

import com.example.example_google_upp.model.Card
import com.google.gson.annotations.SerializedName

internal data class CardDto(
    @SerializedName("cardId") val cardId: String?,
    @SerializedName("userId") val userId: String?,
    @SerializedName("lastFour") val lastFour: String?,
    @SerializedName("cardholderName") val cardholderName: String?,
    @SerializedName("brand") val brand: String?,
) {
    fun toDomain(): Card =
        Card(
            cardId = cardId.requireField("cardId"),
            userId = userId.requireField("userId"),
            lastFour = lastFour.requireField("lastFour"),
            cardholderName =
                cardholderName?.trim().takeUnless { it.isNullOrEmpty() } ?: DEFAULT_CARDHOLDER_NAME,
            brand = brand.toBrand(),
        )

    private companion object {
        const val DEFAULT_CARDHOLDER_NAME = "Pomelo Card"
    }
}
