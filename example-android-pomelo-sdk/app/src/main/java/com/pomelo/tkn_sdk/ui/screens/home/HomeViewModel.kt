package com.pomelo.tkn_sdk.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomelo.tkn_sdk.data.repository.AuthRepository
import com.pomelo.tkn_sdk.data.repository.CardsRepository
import com.pomelo.tkn_sdk.util.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class HomeViewModel(
    private val cardsRepository: CardsRepository = CardsRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
) : ViewModel() {

  private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init {
    loadCards()
  }

  fun loadCards() {
    viewModelScope.launch {
      _uiState.value = HomeUiState.Loading
      _uiState.value =
          when (val result = cardsRepository.getCards()) {
            is NetworkResult.Success -> HomeUiState.Success(result.data)
            is NetworkResult.Error -> HomeUiState.Error(result.message ?: "An error occurred")
            is NetworkResult.Loading -> HomeUiState.Loading
          }
    }
  }

  fun getAuthToken(userId: String): String = runBlocking {
    when (val result = authRepository.getUserEndToken(userId)) {
      is NetworkResult.Success -> result.data
      else -> ""
    }
  }
}
