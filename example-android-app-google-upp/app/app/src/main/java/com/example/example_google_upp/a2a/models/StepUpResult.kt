package com.example.example_google_upp.a2a.models

/**
 * Resultado del flujo de App-to-App Verification que se devuelve a Google Wallet.
 *
 * - [Approved]: Se autenticó al cliente y se activó el token exitosamente.
 * - [Declined]: El cliente canceló la autenticación o eligió no continuar.
 * - [Failure]: Error técnico (autenticación fallida, error de red, timeout).
 *
 * Docs:
 * https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv
 */
sealed interface StepUpResult {
    data object Approved : StepUpResult
    data object Declined : StepUpResult
    data class Failure(val message: String?) : StepUpResult
}
