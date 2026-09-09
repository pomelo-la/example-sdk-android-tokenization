package com.example.example_google_upp.a2a

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.example_google_upp.a2a.models.StepUpResult
import com.example.example_google_upp.a2a.models.VisaA2aPayload
import com.example.example_google_upp.ui.theme.ExamplegoogleuppTheme
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Activity que maneja el flujo de App-to-App Verification (A2A) de Google Wallet.
 *
 * Google Wallet invoca esta Activity cuando necesita verificar la identidad del titular
 * de una tarjeta Visa durante el flujo de agregado (Yellow Path). El Intent contiene
 * el payload de Visa en formato JSON codificado en Base64URL en [Intent.EXTRA_TEXT].
 *
 * Responsabilidades:
 * - Parsear el payload de Visa desde [Intent.EXTRA_TEXT].
 * - Validar que el caller sea Google Wallet ([com.google.android.gms]).
 * - Inicializar el [AppToAppViewModel] con el payload y la validación del caller.
 * - Renderizar la UI con [AppToAppScreen] observando el estado del ViewModel.
 * - Escuchar el resultado final del flujo y devolverlo a Google Wallet vía
 *   [setResult] con el extra `STEP_UP_RESPONSE`.
 *
 * Flujo:
 * 1. Google Wallet lanza esta Activity con el payload A2A.
 * 2. La Activity valida el payload y el caller.
 * 3. Se muestra la UI con los últimos 4 dígitos de la tarjeta.
 * 4. El usuario se autentica (simulado en este ejemplo).
 * 5. Se llama a la API de Pomelo para activar el token.
 * 6. Se devuelve el resultado a Google Wallet.
 *
 * Docs oficiales de Google:
 * https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv
 *
 * Guía interna de Pomelo:
 * pomelo-docs/docs/modules/tokenization/google-pay-a2a.es.md
 */
class AppToAppVerificationActivity : ComponentActivity() {

    private val viewModel: AppToAppViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Parsear el payload de Visa desde Intent.EXTRA_TEXT
        val extraText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val payload = VisaA2aPayload.fromExtraText(extraText)

        // Validar que el caller sea Google Wallet
        val isValidCaller = CallerValidator.isGoogleWallet(callingPackage)

        // Inicializar el ViewModel
        viewModel.init(payload, isValidCaller)

        setContent {
            ExamplegoogleuppTheme(dynamicColor = true) {
                AppToAppScreen(
                    uiState = viewModel.uiState.value,
                    onSimulatedAuthenticationConfirmed = viewModel::onSimulatedAuthenticationConfirmed,
                    onAuthenticationDeclined = viewModel::onAuthenticationDeclined,
                    onActivate = viewModel::onActivate,
                )
            }
        }

        // Observar el resultado final para cerrar la Activity
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.finalResult.collect { result ->
                    finishWithResult(result)
                }
            }
        }
    }

    /**
     * Finaliza la Activity devolviendo el resultado a Google Wallet.
     *
     * El Intent resultante contiene el extra `STEP_UP_RESPONSE` con uno de los valores:
     * - `"approved"`: autenticación y activación exitosas.
     * - `"declined"`: el usuario canceló o declinó.
     * - `"failure"`: error técnico.
     *
     * Docs:
     * https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv
     */
    private fun finishWithResult(result: StepUpResult) {
        val resultIntent = StepUpResultResolver.toResultIntent(result)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
