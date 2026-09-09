package com.example.example_google_upp.a2a

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.example_google_upp.a2a.models.AppToAppUiState
import com.example.example_google_upp.ui.theme.PomeloBlack
import com.example.example_google_upp.ui.theme.PomeloMagenta
import com.example.example_google_upp.ui.theme.PomeloMagentaDeep
import com.example.example_google_upp.ui.theme.PomeloTextMuted
import com.example.example_google_upp.ui.theme.PomeloViolet
import com.example.example_google_upp.ui.theme.PomeloWhite

/**
 * Pantalla de App-to-App Verification.
 *
 * Muestra los diferentes estados del flujo A2A:
 * - Cargando/validando
 * - Esperando autenticación (muestra los últimos 4 dígitos de la tarjeta)
 * - Autenticado, listo para activar
 * - Activando (llamando al backend)
 * - Finalizado (éxito/error)
 *
 * @param uiState El estado actual de la UI.
 * @param onSimulatedAuthenticationConfirmed Callback cuando el usuario confirma la autenticación simulada.
 * @param onAuthenticationDeclined Callback cuando el usuario cancela la autenticación.
 * @param onActivate Callback para activar el token.
 */
@Composable
fun AppToAppScreen(
    uiState: AppToAppUiState,
    onSimulatedAuthenticationConfirmed: () -> Unit,
    onAuthenticationDeclined: () -> Unit,
    onActivate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Activa tu tarjeta",
            style = MaterialTheme.typography.headlineMedium,
            color = PomeloBlack,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (uiState) {
            is AppToAppUiState.Loading -> {
                CircularProgressIndicator(color = PomeloMagenta)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Validando...",
                    color = PomeloTextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            is AppToAppUiState.InvalidRequest -> {
                Text(
                    text = "Error: ${uiState.reason}",
                    color = PomeloMagentaDeep,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            is AppToAppUiState.AwaitingAuthentication -> {
                AwaitingAuthenticationContent(
                    panLast4 = uiState.payload.panLast4,
                    onSimulatedAuthenticationConfirmed = onSimulatedAuthenticationConfirmed,
                    onAuthenticationDeclined = onAuthenticationDeclined,
                )
            }

            is AppToAppUiState.Authenticated -> {
                AuthenticatedContent(
                    panLast4 = uiState.payload.panLast4,
                    onActivate = onActivate,
                    isActivating = false,
                )
            }

            is AppToAppUiState.Activating -> {
                AuthenticatedContent(
                    panLast4 = uiState.payload.panLast4,
                    onActivate = {},
                    isActivating = true,
                )
            }

            is AppToAppUiState.Finished -> {
                val message = when (uiState.result) {
                    is com.example.example_google_upp.a2a.models.StepUpResult.Approved -> "Tarjeta activada exitosamente"
                    is com.example.example_google_upp.a2a.models.StepUpResult.Declined -> "Activación cancelada"
                    is com.example.example_google_upp.a2a.models.StepUpResult.Failure -> "Error: ${uiState.result.message}"
                }
                Text(
                    text = message,
                    color = when (uiState.result) {
                        is com.example.example_google_upp.a2a.models.StepUpResult.Approved -> PomeloViolet
                        is com.example.example_google_upp.a2a.models.StepUpResult.Declined -> PomeloTextMuted
                        is com.example.example_google_upp.a2a.models.StepUpResult.Failure -> PomeloMagentaDeep
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AwaitingAuthenticationContent(
    panLast4: String?,
    onSimulatedAuthenticationConfirmed: () -> Unit,
    onAuthenticationDeclined: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SimplifiedCard(panLast4 = panLast4)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Para continuar, confirma tu identidad",
            color = PomeloBlack,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSimulatedAuthenticationConfirmed,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PomeloMagenta,
                contentColor = PomeloWhite,
            ),
        ) {
            Text("Simular autenticación")
        }

        OutlinedButton(
            onClick = onAuthenticationDeclined,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancelar", color = PomeloTextMuted)
        }
    }
}

@Composable
private fun AuthenticatedContent(
    panLast4: String?,
    onActivate: () -> Unit,
    isActivating: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SimplifiedCard(panLast4 = panLast4)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Listo para activar tu tarjeta",
            color = PomeloBlack,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onActivate,
            enabled = !isActivating,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PomeloMagenta,
                contentColor = PomeloWhite,
                disabledContainerColor = PomeloMagentaDeep,
                disabledContentColor = PomeloWhite,
            ),
        ) {
            if (isActivating) {
                CircularProgressIndicator(
                    color = PomeloWhite,
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Activando...")
            } else {
                Text("Activar")
            }
        }
    }
}

/**
 * Representación simplificada de la tarjeta, mostrando solo los últimos 4 dígitos.
 *
 * Nota: El payload A2A no incluye nombre del titular ni marca, por lo que
 * no se puede reusar PomeloCardComposable completo.
 */
@Composable
private fun SimplifiedCard(panLast4: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PomeloViolet,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = "•••• •••• •••• ${panLast4 ?: "••••"}",
                color = PomeloWhite,
                fontSize = 24.sp,
                letterSpacing = 2.sp,
            )
        }
    }
}
