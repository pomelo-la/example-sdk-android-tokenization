package com.example.example_google_upp.model

enum class Brand {
    VISA,
    MASTERCARD,
}

data class Card(
    val cardId: String,
    val userId: String,
    val lastFour: String,
    val cardholderName: String,
    val brand: Brand,
)
