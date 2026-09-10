package com.example.example_google_upp.a2a.models

/**
 * Estados de UI para el flujo de App-to-App Verification con biometría.
 *
 * El flujo es:
 * 1. Loading (validando payload y caller)
 * 2. BiometricPrompt (mostrando biometría - ANTES de mostrar datos)
 * 3. AwaitingConfirmation (mostrando tarjeta, esperando confirmación para activar)
 * 4. Activating (llamando al backend)
 *
 * No hay un estado final de UI: ni la activación exitosa, ni que el usuario cancele/decline, ni
 * los errores (payload/caller inválido, biometría no disponible, error de red) tienen una pantalla
 * dedicada. En todos esos casos no hay ninguna acción que el usuario deba tomar, así que el
 * ViewModel cede el control a Google Wallet directamente (ver `AppToAppViewModel.finalResult`) y
 * la Activity cierra sin transicionar [AppToAppUiState] a nada nuevo.
 */
sealed interface AppToAppUiState {
    /**
     * Estado inicial: validando payload y caller.
     */
    data object Loading : AppToAppUiState

    /**
     * Mostrando prompt de biometría. No se muestra ningún dato de la tarjeta aún.
     */
    data object BiometricPrompt : AppToAppUiState

    /**
     * Biometría exitosa, mostrando los datos de la tarjeta y esperando
     * confirmación para activar.
     */
    data class AwaitingConfirmation(val payload: VisaA2aPayload) : AppToAppUiState

    /**
     * Activando el token (llamando al backend).
     */
    data class Activating(val payload: VisaA2aPayload) : AppToAppUiState
}
