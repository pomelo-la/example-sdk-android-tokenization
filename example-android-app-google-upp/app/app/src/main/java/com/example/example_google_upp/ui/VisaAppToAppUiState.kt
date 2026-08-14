package com.example.example_google_upp.ui

import com.example.example_google_upp.model.AppToAppActivationResult

data class VisaAppToAppUiState(
    val panLast4: String? = null,
    val isActivating: Boolean = false,
    val activationResult: AppToAppActivationResult? = null,
    val errorMessage: String? = null,
)
