package com.example.example_google_upp.a2a.models

/**
 * Estados de UI para el flujo de App-to-App Verification.
 *
 * Representa las distintas fases del flujo desde que Google Wallet invoca la Activity
 * hasta que se devuelve el resultado.
 */
sealed interface AppToAppUiState {
    /**
     * Estado inicial: validando payload y caller.
     */
    data object Loading : AppToAppUiState

    /**
     * Request inválido: payload ausente/inválido o caller no es Google Wallet.
     * La Activity finalizará con [StepUpResult.Failure].
     */
    data class InvalidRequest(val reason: String) : AppToAppUiState

    /**
     * Esperando autenticación del cliente. Muestra los últimos 4 dígitos de la tarjeta.
     */
    data class AwaitingAuthentication(val payload: VisaA2aPayload) : AppToAppUiState

    /**
     * Cliente autenticado, listo para activar el token.
     */
    data class Authenticated(val payload: VisaA2aPayload) : AppToAppUiState

    /**
     * Activando el token (llamando al backend).
     */
    data class Activating(val payload: VisaA2aPayload) : AppToAppUiState

    /**
     * Flujo finalizado, listo para cerrar la Activity y devolver el resultado a Google Wallet.
     */
    data class Finished(val result: StepUpResult) : AppToAppUiState
}
