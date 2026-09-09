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
 * ViewModel para el flujo de App-to-App Verification.
 *
 * Responsabilidades:
 * - Validar el payload y el caller al iniciar.
 * - Gestionar el estado de UI a través de [AppToAppUiState].
 * - Coordinar la autenticación simulada del cliente.
 * - Llamar al backend para activar el token.
 * - Emitir el [StepUpResult] final para que la Activity lo devuelva a Google Wallet.
 *
 * Docs:
 * https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv
 *
 * @property backendService Servicio para llamar a la API de activación de tokens.
 */
class AppToAppViewModel(
    private val backendService: BackendService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppToAppUiState>(AppToAppUiState.Loading)
    val uiState: StateFlow<AppToAppUiState> = _uiState.asStateFlow()

    private val _finalResult = MutableSharedFlow<StepUpResult>(extraBufferCapacity = 1)
    val finalResult: SharedFlow<StepUpResult> = _finalResult.asSharedFlow()

    /**
     * Inicializa el ViewModel con el payload y la validación del caller.
     *
     * Si el payload es nulo o el caller no es válido, emite [StepUpResult.Failure]
     * y finaliza el flujo sin mostrar la UI de autenticación.
     *
     * @param payload El payload de Visa parseado desde Intent.EXTRA_TEXT, o null si hubo error.
     * @param isValidCaller true si el caller es Google Wallet (com.google.android.gms).
     */
    fun init(payload: VisaA2aPayload?, isValidCaller: Boolean) {
        when {
            payload == null -> {
                _uiState.value = AppToAppUiState.InvalidRequest("Invalid or missing payload")
                _finalResult.tryEmit(StepUpResult.Failure("Invalid or missing payload"))
            }
            !isValidCaller -> {
                _uiState.value = AppToAppUiState.InvalidRequest("Invalid caller: not Google Wallet")
                _finalResult.tryEmit(StepUpResult.Failure("Invalid caller: not Google Wallet"))
            }
            else -> {
                _uiState.value = AppToAppUiState.AwaitingAuthentication(payload)
            }
        }
    }

    /**
     * Simula la confirmación de autenticación del cliente.
     *
     * En producción, aquí iría el flujo real de autenticación (login, biometría, PIN).
     * Este ejemplo usa una simulación para mantener el foco en el cableado de A2A.
     *
     * Docs: ver guía interna google-pay-a2a.es.md
     */
    fun onSimulatedAuthenticationConfirmed() {
        val currentState = _uiState.value
        if (currentState is AppToAppUiState.AwaitingAuthentication) {
            _uiState.value = AppToAppUiState.Authenticated(currentState.payload)
        }
    }

    /**
     * El cliente canceló la autenticación o eligió no continuar.
     *
     * No se llama a la API de activación y se devuelve [StepUpResult.Declined] a Google Wallet.
     */
    fun onAuthenticationDeclined() {
        _uiState.value = AppToAppUiState.Finished(StepUpResult.Declined)
        _finalResult.tryEmit(StepUpResult.Declined)
    }

    /**
     * Activa el token llamando al backend de Pomelo.
     *
     * En éxito devuelve [StepUpResult.Approved] a Google Wallet.
     * En error devuelve [StepUpResult.Failure].
     */
    fun onActivate() {
        val currentState = _uiState.value
        if (currentState !is AppToAppUiState.Authenticated) return

        val payload = currentState.payload
        _uiState.value = AppToAppUiState.Activating(payload)

        viewModelScope.launch {
            try {
                backendService.activateToken(
                    tokenId = payload.tokenReferenceID.orEmpty(),
                    motive = "APP_TO_APP_ACTIVATION",
                )
                _uiState.value = AppToAppUiState.Finished(StepUpResult.Approved)
                _finalResult.tryEmit(StepUpResult.Approved)
            } catch (e: Exception) {
                val message = e.message ?: "Failed to activate token"
                _uiState.value = AppToAppUiState.Finished(StepUpResult.Failure(message))
                _finalResult.tryEmit(StepUpResult.Failure(message))
            }
        }
    }
}
