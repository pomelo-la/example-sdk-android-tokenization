package com.pomelo.tkn_sdk.ui.screens.home

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pomelo.sdk.pushprovisioning.ui.Brand
import com.pomelo.sdk.pushprovisioning.ui.GWalletEffect
import com.pomelo.sdk.pushprovisioning.ui.GoogleWalletButtonComposable
import com.pomelo.tkn_sdk.ui.composables.CardCarousel
import com.pomelo.tkn_sdk.ui.composables.Loader
import kotlinx.coroutines.launch

@Composable
fun HomeComposable(viewModel: HomeViewModel = viewModel()) {
  var isGPayLoading by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  val uiState by viewModel.uiState.collectAsState()

  when (val state = uiState) {
    is HomeUiState.Loading -> Loader()

    is HomeUiState.Error -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = state.message, color = Color.Red)
      }
    }

    is HomeUiState.Success -> {
      val cards = state.cards
      val pagerState = rememberPagerState(pageCount = { cards.size })
      val selectedCard = cards.getOrNull(pagerState.currentPage)

      Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.padding(horizontal = 16.dp),
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValue ->
          Column(
              modifier = Modifier.padding(paddingValues = paddingValue),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            CardCarousel(cards = cards, pagerState = pagerState)

            GoogleWalletButtonComposable(
                cardId = selectedCard!!.cardId,
                lastFour = selectedCard.lastFour,
                brand = selectedCard.brand,
                asBadge = false,
                authTokenProvider = { viewModel.getAuthToken(selectedCard.userId) },
                onEffect = { effect ->
                  when (effect) {
                    GWalletEffect.Loading -> {
                      Log.i("CardComposeScreen", "GPay loading...")
                      isGPayLoading = true
                    }

                    is GWalletEffect.Error -> {
                      Log.i("CardComposeScreen", "GPay error: ${effect.message}")
                      isGPayLoading = false
                      coroutineScope.launch {
                        snackbarHostState.showSnackbar("Error en GPay: ${effect.message}")
                      }
                    }

                    GWalletEffect.TokenizationCompleted -> {
                      Log.i("CardComposeScreen", "GPay tokenization completed")
                      isGPayLoading = false
                      coroutineScope.launch {
                        snackbarHostState.showSnackbar("Tokenización completada")
                      }
                    }
                  }
                },
            )
          }
        }

        if (isGPayLoading) {
          Loader()
        }
      }
    }
  }
}
