package com.example.example_google_upp.wallet.models

sealed interface PushProvisioningResult {
    data object Success : PushProvisioningResult

    data class Cancelled(val statusCode: Int?) : PushProvisioningResult

    data class Error(val statusCode: Int?, val message: String) : PushProvisioningResult
}
