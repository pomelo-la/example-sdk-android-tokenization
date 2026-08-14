package com.example.example_google_upp.model

/**
 * Result of activating a token through Visa's App2App (IDV / "yellow path") step-up flow.
 *
 * The backend maps Pomelo's `activation_result` (APPROVED/DECLINED/FAILURE) to this domain type,
 * which the app then maps to the `STEP_UP_RESPONSE` value Google Wallet expects back.
 */
sealed interface AppToAppActivationResult {
    data object Approved : AppToAppActivationResult

    data object Declined : AppToAppActivationResult

    data class Failed(val message: String) : AppToAppActivationResult
}
