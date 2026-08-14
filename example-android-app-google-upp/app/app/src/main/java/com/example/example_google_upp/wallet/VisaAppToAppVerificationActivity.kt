package com.example.example_google_upp.wallet

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.example_google_upp.BuildConfig
import com.example.example_google_upp.model.AppToAppActivationResult
import com.example.example_google_upp.ui.VisaAppToAppScreen
import com.example.example_google_upp.ui.VisaAppToAppViewModel
import com.example.example_google_upp.ui.theme.ExamplegoogleuppTheme
import com.example.example_google_upp.wallet.models.VisaAppToAppPayload
import com.example.example_google_upp.wallet.models.VisaAppToAppStepUpResponse
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Handles Visa's App2App (IDV / "yellow path") step-up flow.
 *
 * Google Wallet launches this Activity with an explicit Intent (action `<applicationId>.a2a`, the
 * fixed format Visa requires: `{banking app identifier}.{service name}`) when a token needs
 * cardholder verification before it can be activated. Google Wallet passes an opaque,
 * Base64URL-encoded JSON payload in `Intent.EXTRA_TEXT` (see VisaAppToAppPayload) — never
 * PCI/auth data, per Google's App2App verification guidance.
 *
 * This is a different flow from the manual push-tokenize one started by MainActivity: here
 * Google Wallet initiates the Intent, not the app, and it does not call TapAndPay APIs at all —
 * it is a plain Intent handoff plus a call to the issuer backend.
 *
 * This sample implements Visa's "Option 1" activation: it authenticates the cardholder, asks the
 * backend to call Visa's Token Lifecycle API directly, and reports back only whether the
 * activation was approved/declined/failed — it does not return an authentication code (TAV),
 * which would be Visa's "Option 2".
 *
 * Extends FragmentActivity (instead of plain ComponentActivity, like MainActivity) because
 * BiometricPrompt requires one.
 */
class VisaAppToAppVerificationActivity : FragmentActivity() {
    private val viewModel: VisaAppToAppViewModel by viewModel()
    private var payload: VisaAppToAppPayload? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isCalledByGoogleWallet()) {
            Log.e(TAG, "Ignoring Intent not started by Google Wallet (callingPackage=$callingPackage)")
            finishWithResult(AppToAppActivationResult.Failed("Untrusted caller"))
            return
        }

        val parsedPayload = VisaAppToAppPayload.fromExtraText(intent.getStringExtra(Intent.EXTRA_TEXT))
        payload = parsedPayload
        viewModel.onPayloadParsed(parsedPayload)

        promptCardholderAuthentication(
            onSuccess = { showConfirmationScreen(parsedPayload) },
            onFailure = { finishWithResult(AppToAppActivationResult.Declined) },
        )
    }

    private fun showConfirmationScreen(parsedPayload: VisaAppToAppPayload?) {
        setContent {
            ExamplegoogleuppTheme(dynamicColor = true) {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.activationResult) {
                    uiState.activationResult?.let(::finishWithResult)
                }

                VisaAppToAppScreen(
                    uiState = uiState,
                    onActivate = { parsedPayload?.let(viewModel::activate) },
                    onCancel = { finishWithResult(AppToAppActivationResult.Declined) },
                )
            }
        }
    }

    /**
     * Gates the confirmation screen behind cardholder authentication, per Google's App2App
     * guidance that the issuer app must authenticate the cardholder before activating the token.
     *
     * BiometricPrompt is used here only as a runnable example — it is NOT a recommendation. Each
     * issuer should pick whatever authentication strategy fits their own app (biometrics, face
     * recognition, an already-active logged-in session, a PIN, etc.) instead of assuming this one.
     */
    private fun promptCardholderAuthentication(onSuccess: () -> Unit, onFailure: () -> Unit) {
        // DEVICE_CREDENTIAL can only be combined with BIOMETRIC_STRONG from API 30 onward; below
        // that, BiometricPrompt requires a negative button instead since there's no combined
        // fallback.
        val authenticators =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            } else {
                BIOMETRIC_STRONG
            }

        if (BiometricManager.from(this).canAuthenticate(authenticators) !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            Log.e(TAG, "Cardholder authentication is not available on this device")
            onFailure()
            return
        }

        val promptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm it's you")
                .setSubtitle("Authenticate to activate your Pomelo card")
                .setAllowedAuthenticators(authenticators)
                .apply {
                    if (authenticators == BIOMETRIC_STRONG) setNegativeButtonText("Cancel")
                }
                .build()

        BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        Log.e(TAG, "Cardholder authentication error: $errorCode $errString")
                        onFailure()
                    }
                },
            )
            .authenticate(promptInfo)
    }

    /**
     * Verifies the Intent was actually started by Google Wallet, not another app impersonating it.
     *
     * `callingPackage` is only set when the caller used `startActivityForResult` (what Google
     * Wallet does); it is null when this Activity is launched any other way, e.g. `adb shell am
     * start`. Debug builds accept a null caller so this flow can be exercised manually with a
     * mocked payload, as Google's own App2App verification troubleshooting guide suggests testing
     * with `adb`. Release builds keep the check strict.
     *
     * Docs: Google App2App verification, "Mobile app security".
     */
    private fun isCalledByGoogleWallet(): Boolean =
        callingPackage == GOOGLE_WALLET_PACKAGE || (BuildConfig.DEBUG && callingPackage == null)

    /** Reports the activation result back to Google Wallet via the `STEP_UP_RESPONSE` extra. */
    private fun finishWithResult(result: AppToAppActivationResult) {
        val data =
            Intent()
                .putExtra(
                    VisaAppToAppStepUpResponse.EXTRA_STEP_UP_RESPONSE,
                    VisaAppToAppStepUpResponse.from(result),
                )
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    companion object {
        private const val TAG = "AppToAppVerification"
        private const val GOOGLE_WALLET_PACKAGE = "com.google.android.gms"
    }
}
