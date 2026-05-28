package com.example.example_google_upp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.example_google_upp.ui.theme.PomeloMagenta
import com.example.example_google_upp.ui.theme.PomeloWhite

@Composable
fun CardSearchRoute(
    viewModel: CardSearchViewModel,
    walletButtonContent: @Composable (CardSearchUiState) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSearchSheetVisible by rememberSaveable { mutableStateOf(uiState.selectedCard == null) }

    LaunchedEffect(uiState.selectedCard?.cardId, uiState.isLoading) {
        if (uiState.selectedCard != null && !uiState.isLoading) {
            isSearchSheetVisible = false
        }
    }

    LaunchedEffect(uiState.selectedCard?.cardId) {
        if (uiState.selectedCard != null) {
            viewModel.refreshWalletEligibility()
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSnackbarShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isSearchSheetVisible = true },
                containerColor = PomeloMagenta,
                contentColor = PomeloWhite,
            ) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Search card")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CardSearchScreen(
                uiState = uiState,
                isSearchSheetVisible = isSearchSheetVisible,
                onSearch = viewModel::search,
                onDismissSearch = { isSearchSheetVisible = false },
            )

            walletButtonContent(uiState)
        }
    }
}
