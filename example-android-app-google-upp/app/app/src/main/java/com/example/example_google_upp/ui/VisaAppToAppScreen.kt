package com.example.example_google_upp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Confirmation screen shown by VisaAppToAppVerificationActivity while Google Wallet waits for IDV. */
@Composable
fun VisaAppToAppScreen(uiState: VisaAppToAppUiState, onActivate: () -> Unit, onCancel: () -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Confirm this card activation", style = MaterialTheme.typography.titleLarge)

            uiState.panLast4?.let { lastFour ->
                Text(
                    text = "•••• •••• •••• $lastFour",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            uiState.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            when {
                uiState.isActivating -> CircularProgressIndicator()
                uiState.errorMessage == null -> {
                    Button(onClick = onActivate) { Text("Activar") }
                    OutlinedButton(onClick = onCancel) { Text("Cancelar") }
                }
                else -> OutlinedButton(onClick = onCancel) { Text("Cerrar") }
            }
        }
    }
}
