package com.example.example_google_upp.a2a

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.example_google_upp.a2a.models.AppToAppUiState
import com.example.example_google_upp.a2a.models.StepUpResult
import com.example.example_google_upp.components.PomeloCardComposable
import com.example.example_google_upp.model.Brand
import com.example.example_google_upp.model.Card
import com.example.example_google_upp.ui.theme.PomeloBlack
import com.example.example_google_upp.ui.theme.PomeloMagenta
import com.example.example_google_upp.ui.theme.PomeloTextMuted
import com.example.example_google_upp.ui.theme.PomeloWhite

/**
 * Pantalla de App-to-App Verification con biometría.
 *
 * El flujo es:
 * 1. Loading: validando payload y caller
 * 2. BiometricPrompt: mostrando indicador de biometría (sin datos de tarjeta visibles)
 * 3. AwaitingConfirmation: biometría exitosa, mostrando tarjeta y botón para activar
 * 4. Activating: llamando al backend
 *
 * No hay una pantalla para el resultado final: ni la activación exitosa
 * ([StepUpResult.Approved]), ni que el usuario decline la biometría ([StepUpResult.Declined]), ni
 * los errores ([StepUpResult.Failure]: payload/caller inválido, sin biometría disponible, error de
 * red al activar) muestran nada acá. El ViewModel cede el control a Google Wallet directamente en
 * todos esos casos y la Activity cierra, sin pasar por ningún estado nuevo de este composable.
 *
 * @param uiState El estado actual de la UI.
 * @param onActivate Callback para activar el token.
 */
@Composable
fun AppToAppScreen(uiState: AppToAppUiState, onActivate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (uiState) {
            is AppToAppUiState.Loading -> {
                LoadingContent()
            }

            is AppToAppUiState.BiometricPrompt -> {
                BiometricPromptContent()
            }

            is AppToAppUiState.AwaitingConfirmation -> {
                AwaitingConfirmationContent(
                    panLast4 = uiState.payload.panLast4 ?: "••••",
                    onActivate = onActivate,
                )
            }

            is AppToAppUiState.Activating -> {
                ActivatingContent(panLast4 = uiState.payload.panLast4 ?: "••••")
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    CircularProgressIndicator(color = PomeloMagenta)
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Validando...",
        color = PomeloTextMuted,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun BiometricPromptContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Autenticación biométrica",
            modifier = Modifier.size(80.dp),
            tint = PomeloMagenta,
        )

        Text(
            text = "Verifica tu identidad",
            style = MaterialTheme.typography.headlineMedium,
            color = PomeloBlack,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AwaitingConfirmationContent(panLast4: String, onActivate: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "Activa tu tarjeta",
            style = MaterialTheme.typography.headlineMedium,
            color = PomeloBlack,
            textAlign = TextAlign.Center,
        )

        A2ACardDisplay(panLast4 = panLast4)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Presiona el botón para activar tu tarjeta en Google Wallet",
            color = PomeloBlack,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Button(
            onClick = onActivate,
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = PomeloMagenta,
                    contentColor = PomeloWhite,
                ),
        ) {
            Text("Activar tarjeta")
        }
    }
}

@Composable
private fun ActivatingContent(panLast4: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "Activando...",
            style = MaterialTheme.typography.headlineMedium,
            color = PomeloBlack,
            textAlign = TextAlign.Center,
        )

        A2ACardDisplay(panLast4 = panLast4)

        Spacer(modifier = Modifier.height(8.dp))

        CircularProgressIndicator(color = PomeloMagenta)

        Text(
            text = "Estamos activando tu tarjeta, por favor espera...",
            color = PomeloTextMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** Representación de la tarjeta usando PomeloCardComposable. */
@Composable
private fun A2ACardDisplay(panLast4: String) {
    PomeloCardComposable(
        card =
            Card(
                cardId = "a2a-card",
                userId = "a2a-user",
                lastFour = panLast4,
                cardholderName = "",
                brand = Brand.VISA,
            )
    )
}
