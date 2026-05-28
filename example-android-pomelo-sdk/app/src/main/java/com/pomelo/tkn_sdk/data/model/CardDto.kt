package com.pomelo.tkn_sdk.data.model

import com.pomelo.sdk.pushprovisioning.ui.Brand

data class CardDto(
    val cardId: String,
    val userId: String,
    val lastFour: String,
    val cardholderName: String,
    val brand: Brand,
)
