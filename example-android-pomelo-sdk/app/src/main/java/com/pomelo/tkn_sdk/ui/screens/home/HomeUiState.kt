package com.pomelo.tkn_sdk.ui.screens.home

import com.pomelo.tkn_sdk.data.model.CardDto

sealed interface HomeUiState {
  data object Loading : HomeUiState

  data class Success(val cards: List<CardDto>) : HomeUiState

  data class Error(val message: String) : HomeUiState
}
