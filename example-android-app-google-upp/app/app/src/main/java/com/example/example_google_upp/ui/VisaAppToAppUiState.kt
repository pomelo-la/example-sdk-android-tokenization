package com.example.example_google_upp.ui

import com.example.example_google_upp.model.AppToAppActivationResult
import com.example.example_google_upp.model.Card

data class VisaAppToAppUiState(
    val card: Card? = null,
    val isActivating: Boolean = false,
    val activationResult: AppToAppActivationResult? = null,
    val errorMessage: String? = null,
)
