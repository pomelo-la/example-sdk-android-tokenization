package com.example.example_google_upp.ui

import com.example.example_google_upp.model.Card

enum class WalletButtonState {
    UNAVAILABLE,
    READY_TO_ADD,
    ALREADY_ADDED,
}

data class CardSearchUiState(
    val isLoading: Boolean = false,
    val selectedCard: Card? = null,
    val walletButtonState: WalletButtonState = WalletButtonState.UNAVAILABLE,
    val snackbarMessage: String? = null,
)
