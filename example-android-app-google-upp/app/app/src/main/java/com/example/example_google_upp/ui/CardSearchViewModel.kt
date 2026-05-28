package com.example.example_google_upp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.example_google_upp.data.BackendService
import com.example.example_google_upp.wallet.models.PushProvisioningResult
import com.example.example_google_upp.wallet.models.WalletProvisioningGateway
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CardSearchViewModel(
    private val backendService: BackendService,
    private val walletProvisioningGateway: WalletProvisioningGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CardSearchUiState())
    val uiState: StateFlow<CardSearchUiState> = _uiState.asStateFlow()

    private var walletEligibilityJob: Job? = null

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    suspend fun search(query: String): Result<Unit> {
        val currentQuery = query.trim()
        clearSearchResultState()
        if (currentQuery.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a card ID"))
        }

        _uiState.update { it.copy(isLoading = true) }

        return runCatching {
            withContext(Dispatchers.IO) { backendService.getCardById(currentQuery) }
        }
            .fold(
                onSuccess = { card ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedCard = card,
                            walletButtonState = WalletButtonState.UNAVAILABLE,
                        )
                    }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    if (error is CancellationException) {
                        throw error
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    Result.failure(error)
                },
            )
    }

    fun onProvisioningResult(result: PushProvisioningResult) {
        when (result) {
            PushProvisioningResult.Success -> {
                _uiState.update { it.copy(snackbarMessage = "Card added to Google Wallet") }
                refreshWalletEligibility()
            }

            is PushProvisioningResult.Cancelled -> {
                _uiState.update { it.copy(snackbarMessage = "Google Wallet provisioning canceled") }
            }

            is PushProvisioningResult.Error -> {
                _uiState.update { it.copy(snackbarMessage = result.message) }
            }
        }
    }

    /**
     * Refreshes the Google Wallet button state for the currently selected card.
     *
     * This is called after wallet-relevant events such as Activity resume, DataChangedListener
     * callbacks, and successful provisioning. It cancels any previous eligibility check, asks Tap
     * And Pay whether the card is already tokenized, and maps that answer to the sample UI:
     * tokenized cards are ALREADY_ADDED, non-tokenized cards are READY_TO_ADD. If the selected card
     * changes while the check is running, the stale result is ignored.
     *
     * This sample is the most basic integration, so it does not take wearables into account. That
     * means it only validates whether the card already exists in the current device's wallet,
     * regardless of any paired wearable devices. To evaluate tokenization targets across the phone
     * and wearables, use `hasEligibleTokenizationTarget` instead.
     *
     * Reference:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#haseligibletokenizationtarget
     */
    fun refreshWalletEligibility() {
        val selectedCard = uiState.value.selectedCard ?: return

        walletEligibilityJob?.cancel()
        walletEligibilityJob = viewModelScope.launch {
            runCatching { walletProvisioningGateway.isTokenized(selectedCard) }
                .onSuccess { isTokenized ->
                    _uiState.update {
                        it.copy(
                            walletButtonState =
                                if (isTokenized) {
                                    WalletButtonState.ALREADY_ADDED
                                } else {
                                    WalletButtonState.READY_TO_ADD
                                }
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure

                    _uiState.update {
                        it.copy(
                            walletButtonState = WalletButtonState.UNAVAILABLE,
                            snackbarMessage =
                                error.message ?: "Unable to check Google Wallet eligibility",
                        )
                    }
                }
        }
    }

    private fun clearSearchResultState() {
        walletEligibilityJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = false,
                selectedCard = null,
                walletButtonState = WalletButtonState.UNAVAILABLE,
            )
        }
    }
}
