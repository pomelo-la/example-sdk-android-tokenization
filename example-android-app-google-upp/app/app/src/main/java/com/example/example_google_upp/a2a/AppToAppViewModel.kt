package com.example.example_google_upp.a2a

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.example_google_upp.a2a.models.AppToAppUiState
import com.example.example_google_upp.a2a.models.StepUpResult
import com.example.example_google_upp.a2a.models.VisaA2aPayload
import com.example.example_google_upp.data.BackendService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para el flujo de App-to-App Verification con biometría.
 *
 * Responsabilidades:
 * - Validar el payload y el caller al iniciar.
 * - Gestionar el estado de UI a través de [AppToAppUiState].
 * - Coordinar la autenticación biométrica (antes de mostrar datos).
 * - Llamar al backend para activar el token.
 * - Emitir el [StepUpResult] final para que la Activity lo devuelva a Google Wallet.
 *
 * Flujo:
 * 1. Loading (validación inicial)
 * 2. BiometricPrompt (biometría antes de mostrar datos)
 * 3. AwaitingConfirmation (mostrando tarjeta, listo para activar)
 * 4. Activating (llamando al backend)
 *
 * No hay un estado de UI para el resultado final: ni la activación exitosa, ni que el usuario
 * cancele/decline la biometría, ni ningún [StepUpResult.Failure] (payload/caller inválido,
 * biometría no disponible, error de red al activar) transicionan [uiState] a nada nuevo. En
 * ninguno de esos casos hay una acción que el usuario deba tomar, así que el control se cede a
 * Google Wallet directamente vía [finalResult] y la Activity cierra sola.
 *
 * Docs:
 * https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv
 */
class AppToAppViewModel(
    private val backendService: BackendService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppToAppUiState>(AppToAppUiState.Loading)
    val uiState: StateFlow<AppToAppUiState> = _uiState.asStateFlow()

    private val _finalResult = MutableSharedFlow<StepUpResult>(replay = 1, extraBufferCapacity = 0)
    val finalResult: SharedFlow<StepUpResult> = _finalResult.asSharedFlow()

    private var currentPayload: VisaA2aPayload? = null

    /**
     * Inicializa el ViewModel con el payload y la validación del caller.
     *
     * Si el payload es nulo o el caller no es válido, devuelve el control a Google Wallet
     * directamente emitiendo [StepUpResult.Failure] por [finalResult], sin pasar por ningún
     * estado de UI dedicado: no hay nada que el usuario pueda hacer ante un request inválido, así
     * que no tiene sentido mostrarle una pantalla de error a la que nunca hay que reaccionar.
     * Si todo es válido, pasa al estado BiometricPrompt para solicitar biometría.
     *
     * @param payload El payload de Visa parseado desde Intent.EXTRA_TEXT.
     * @param isValidCaller true si el caller es Google Wallet.
     */
    fun init(payload: VisaA2aPayload?, isValidCaller: Boolean) {
        when {
            payload == null -> {
                _finalResult.tryEmit(StepUpResult.Failure("Invalid or missing payload"))
            }
            !isValidCaller -> {
                _finalResult.tryEmit(StepUpResult.Failure("Invalid caller: not Google Wallet"))
            }
            else -> {
                currentPayload = payload
                // Ir a estado de biometría - ANTES de mostrar cualquier dato
                _uiState.value = AppToAppUiState.BiometricPrompt
            }
        }
    }

    /**
     * Llamado cuando la autenticación biométrica es exitosa.
     * Ahora podemos mostrar los datos de la tarjeta.
     */
    fun onAuthenticationConfirmed() {
        currentPayload?.let { payload ->
            _uiState.value = AppToAppUiState.AwaitingConfirmation(payload)
        }
    }

    /**
     * Llamado cuando el usuario cancela la biometría desde el propio prompt nativo (el sistema ya
     * ofrece su forma de cancelar, así que no hace falta un botón propio en la screen para esto).
     *
     * No se llama a la API de activación. El control se cede a Google Wallet directamente vía
     * [finalResult], sin pasar por ningún estado de UI dedicado.
     */
    fun onAuthenticationDeclined() {
        _finalResult.tryEmit(StepUpResult.Declined)
    }

    /**
     * Llamado cuando no hay biometría disponible en el dispositivo.
     *
     * Esto es un error técnico, no algo que el usuario pueda resolver desde esta pantalla: no
     * cambiamos [uiState] a un estado de error dedicado, simplemente devolvemos el control a
     * Google Wallet emitiendo [StepUpResult.Failure] por [finalResult].
     */
    fun onBiometricNotAvailable() {
        _finalResult.tryEmit(StepUpResult.Failure("Biometric authentication not available"))
    }

    /**
     * Llamado cuando hay un error técnico en la biometría. Ver [onBiometricNotAvailable].
     */
    fun onBiometricError(message: String) {
        _finalResult.tryEmit(StepUpResult.Failure(message))
    }

    /**
     * Activa el token llamando al backend de Pomelo.
     *
     * Tanto en éxito como en error, el resultado se cede directamente a Google Wallet vía
     * [finalResult] sin pasar por un estado de UI dedicado: no hay ninguna confirmación ni acción
     * que el usuario deba tomar después de activar, así que no tiene sentido una pantalla
     * intermedia entre "Activando..." y que la Activity cierre.
     */
    fun onActivate() {
        val payload = currentPayload
        if (payload == null || _uiState.value !is AppToAppUiState.AwaitingConfirmation) return

        _uiState.value = AppToAppUiState.Activating(payload)

        viewModelScope.launch {
            try {
                backendService.activateToken(
                    tokenId = payload.tokenReferenceID.orEmpty(),
                    motive = "APP_TO_APP_ACTIVATION",
                )
                _finalResult.tryEmit(StepUpResult.Approved)
            } catch (e: Exception) {
                val message = e.message ?: "Failed to activate token"
                _finalResult.tryEmit(StepUpResult.Failure(message))
            }
        }
    }
}
