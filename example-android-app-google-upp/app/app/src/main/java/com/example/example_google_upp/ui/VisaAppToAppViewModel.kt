package com.example.example_google_upp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.example_google_upp.data.BackendService
import com.example.example_google_upp.model.AppToAppActivationResult
import com.example.example_google_upp.model.Brand
import com.example.example_google_upp.model.Card
import com.example.example_google_upp.wallet.models.VisaAppToAppPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the App2App (IDV) verification screen shown by VisaAppToAppVerificationActivity: displays
 * the card being activated and, once the cardholder confirms, asks the backend to activate the
 * token against Visa.
 *
 * Cardholder authentication itself already happened before this screen is shown (see
 * VisaAppToAppVerificationActivity.promptCardholderAuthentication) — the tap on "Activar" here is
 * just the confirmation to proceed with the activation call.
 */
class VisaAppToAppViewModel(private val backendService: BackendService) : ViewModel() {
    private val _uiState = MutableStateFlow(VisaAppToAppUiState())
    val uiState: StateFlow<VisaAppToAppUiState> = _uiState.asStateFlow()

    /** Reflects the decoded Visa payload in the UI, or surfaces an error if it's missing/invalid. */
    fun onPayloadParsed(payload: VisaAppToAppPayload?) {
        val tokenId = payload?.tokenReferenceId
        if (tokenId == null) {
            _uiState.update {
                it.copy(errorMessage = "Google Wallet sent an invalid App2App payload")
            }
            return
        }

        // The Visa payload never includes the cardholder name, only card/token identifiers, so
        // PomeloCardComposable is shown with the same "Pomelo Card" placeholder BackendService
        // falls back to when the backend doesn't provide one either.
        val card =
            Card(
                cardId = tokenId,
                userId = payload.walletAccountId.orEmpty(),
                lastFour = payload.panLast4 ?: "????",
                cardholderName = "Pomelo Card",
                brand = Brand.VISA,
            )
        _uiState.update { it.copy(card = card) }
    }

    /** Calls the backend to activate the token; the result is later reported back to Google Wallet. */
    fun activate(payload: VisaAppToAppPayload) {
        val tokenId = payload.tokenReferenceId ?: return

        _uiState.update { it.copy(isActivating = true) }
        viewModelScope.launch {
            runCatching {
                    backendService.activateAppToAppToken(
                        tokenId = tokenId,
                        deviceId = payload.deviceId,
                    )
                }
                .onSuccess { result ->
                    _uiState.update { it.copy(isActivating = false, activationResult = result) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isActivating = false,
                            activationResult =
                                AppToAppActivationResult.Failed(
                                    error.message ?: "Unable to activate the token"
                                ),
                        )
                    }
                }
        }
    }
}
