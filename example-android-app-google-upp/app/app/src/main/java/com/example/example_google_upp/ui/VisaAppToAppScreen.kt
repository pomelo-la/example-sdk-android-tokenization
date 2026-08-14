package com.example.example_google_upp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.example_google_upp.components.PomeloCardComposable
import com.example.example_google_upp.ui.theme.PomeloMagenta
import com.example.example_google_upp.ui.theme.PomeloWhite

/**
 * Confirmation screen shown by VisaAppToAppVerificationActivity while Google Wallet waits for IDV.
 *
 * Layout mirrors Google's own App2App reference mockup ("Issuer app UI"): brand title, a short
 * instruction, the card being activated, and a single prominent action pinned to the bottom.
 */
@Composable
fun VisaAppToAppScreen(uiState: VisaAppToAppUiState, onActivate: () -> Unit, onCancel: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                when {
                    uiState.isActivating -> CircularProgressIndicator()
                    uiState.errorMessage != null -> TextButton(onClick = onCancel) { Text("Cerrar") }
                    else -> {
                        TextButton(onClick = onCancel) { Text("Cancelar") }
                        Button(
                            onClick = onActivate,
                            shape = RoundedCornerShape(50),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = PomeloMagenta,
                                    contentColor = PomeloWhite,
                                ),
                        ) {
                            Text("Activar")
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "POMELO",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = PomeloMagenta,
            )

            Text(
                text = "Tocá \"Activar\" para activar tu tarjeta",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )

            uiState.card?.let { card -> PomeloCardComposable(card) }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}
