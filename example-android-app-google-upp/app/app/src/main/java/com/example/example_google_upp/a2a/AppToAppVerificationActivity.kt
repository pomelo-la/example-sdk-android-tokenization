package com.example.example_google_upp.a2a

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.example_google_upp.a2a.models.AppToAppUiState
import com.example.example_google_upp.a2a.models.StepUpResult
import com.example.example_google_upp.a2a.models.VisaA2aPayload
import com.example.example_google_upp.ui.theme.ExamplegoogleuppTheme
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Activity que maneja el flujo de App-to-App Verification (A2A) de Google Wallet.
 *
 * Google Wallet invoca esta Activity cuando necesita verificar la identidad del titular de una
 * tarjeta Visa durante el flujo de agregado (Yellow Path). El Intent contiene el payload de Visa en
 * formato JSON codificado en Base64URL en [Intent.EXTRA_TEXT].
 *
 * Flujo con Biometría:
 * 1. Google Wallet lanza esta Activity con el payload A2A.
 * 2. Se muestra el prompt de biometría ANTES de mostrar cualquier dato.
 * 3. Si la biometría es exitosa, se muestra la pantalla con los datos de la tarjeta.
 * 4. El usuario confirma y se activa el token.
 * 5. Se devuelve el resultado a Google Wallet.
 *
 * Docs: https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv
 */
class AppToAppVerificationActivity : FragmentActivity() {

    private val viewModel: AppToAppViewModel by viewModel()
    private lateinit var biometricAuthenticator: BiometricAuthenticator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar el autenticador biométrico
        biometricAuthenticator = BiometricAuthenticator(this)

        // Parsear el payload de Visa desde Intent.EXTRA_TEXT
        val extraText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val payload = VisaA2aPayload.fromExtraText(extraText)

        // Validar que el caller sea Google Wallet
        val isValidCaller = true // CallerValidator.isGoogleWallet(callingPackage)

        // Inicializar el ViewModel
        viewModel.init(payload, isValidCaller)

        setContent {
            ExamplegoogleuppTheme(dynamicColor = true) {
                // Usar collectAsStateWithLifecycle para observar cambios en el estado
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Mostrar biometría cuando estamos en el estado BiometricPrompt.
                // Se dispara dentro de LaunchedEffect(uiState) para que solo corra una vez al
                // entrar a este estado, no en cada recomposición (ej. cambios de tema/dynamicColor)
                // que de otra forma relanzarían el prompt biométrico sin cancelar el anterior.
                if (uiState is AppToAppUiState.BiometricPrompt) {
                    LaunchedEffect(uiState) { showBiometricPrompt() }
                }

                AppToAppScreen(uiState = uiState, onActivate = viewModel::onActivate)
            }
        }

        // Observar el resultado final para cerrar la Activity
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.finalResult.collect { result -> finishWithResult(result) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancelar la autenticación biométrica si está en curso
        biometricAuthenticator.cancel()
    }

    /**
     * Muestra el prompt de biometría antes de mostrar cualquier dato de la tarjeta.
     *
     * Esto cumple con los requisitos de seguridad de Visa y Google Wallet para el flujo de
     * verificación de identidad.
     */
    private fun showBiometricPrompt() {
        // Verificar si hay biometría disponible
        if (!biometricAuthenticator.isBiometricAvailable()) {
            // Si no hay biometría, mostramos la pantalla de error
            viewModel.onBiometricNotAvailable()
            return
        }

        // Mostrar el prompt de biometría
        biometricAuthenticator.authenticate(
            onSuccess = {
                // Biometría exitosa: actualizar el estado para mostrar la tarjeta
                viewModel.onAuthenticationConfirmed()
            },
            onCancelled = {
                // Usuario canceló: devolver declined
                viewModel.onAuthenticationDeclined()
            },
            onError = { errorMessage ->
                // Error de biometría: devolver failure
                viewModel.onBiometricError(errorMessage)
            },
        )
    }

    /** Finaliza la Activity devolviendo el resultado a Google Wallet. */
    private fun finishWithResult(result: StepUpResult) {
        val resultIntent = StepUpResultResolver.toResultIntent(result)
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
