package com.example.example_google_upp.a2a

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

/**
 * Maneja la autenticación biométrica para el flujo de App-to-App Verification.
 *
 * Esta clase envuelve BiometricPrompt y proporciona callbacks simples para
 * éxito, cancelación y error.
 *
 * @property activity La Activity que muestra el prompt (debe ser FragmentActivity).
 */
class BiometricAuthenticator(private val activity: FragmentActivity) {

    private val executor: Executor = ContextCompat.getMainExecutor(activity)
    private var biometricPrompt: BiometricPrompt? = null

    /**
     * Muestra el prompt de autenticación biométrica.
     *
     * @param onSuccess Callback cuando la autenticación es exitosa.
     * @param onCancelled Callback cuando el usuario cancela o no hay biometría disponible.
     * @param onError Callback cuando hay un error técnico.
     */
    fun authenticate(
        onSuccess: () -> Unit,
        onCancelled: () -> Unit,
        onError: (String) -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verificar identidad")
            .setSubtitle("Usa tu huella o rostro para continuar")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    biometricPrompt = null
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // No llamamos onError aquí, el sistema reintentará automáticamente
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    biometricPrompt = null
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onCancelled()
                        else -> onError(errString.toString())
                    }
                }
            }
        )

        biometricPrompt?.authenticate(promptInfo)
    }

    /**
     * Cancela la autenticación biométrica si está en curso.
     *
     * Debe llamarse cuando el usuario cancela el flujo desde la UI de la app
     * (no desde el botón negativo del prompt nativo).
     */
    fun cancel() {
        biometricPrompt?.cancelAuthentication()
        biometricPrompt = null
    }

    /**
     * Verifica si el dispositivo tiene biometría disponible.
     *
     * @return true si hay hardware biométrico disponible y registrado.
     */
    fun isBiometricAvailable(): Boolean {
        return try {
            val biometricManager = androidx.biometric.BiometricManager.from(activity)
            biometricManager.canAuthenticate(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }
    }
}
