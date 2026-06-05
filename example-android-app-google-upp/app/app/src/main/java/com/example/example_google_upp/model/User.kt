package com.example.example_google_upp.model

data class User(
    val id: String,
    val name: String,
    val surname: String,
    val addressLine1: String,
    val addressLine2: String?,
    val locality: String,
    val administrativeArea: String,
    val postalCode: String,
    val countryCode: String,
)
