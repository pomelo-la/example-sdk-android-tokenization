package com.example.example_google_upp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.lifecycle.lifecycleScope
import com.example.example_google_upp.components.GoogleWalletProvisionButton
import com.example.example_google_upp.model.Card
import com.example.example_google_upp.ui.CardSearchRoute
import com.example.example_google_upp.ui.CardSearchViewModel
import com.example.example_google_upp.ui.WalletButtonState
import com.example.example_google_upp.ui.theme.ExamplegoogleuppTheme
import com.example.example_google_upp.wallet.PushProvisioningResultResolver
import com.example.example_google_upp.wallet.models.PushProvisioningResult
import com.example.example_google_upp.wallet.models.WalletProvisioningGateway
import com.google.android.gms.tapandpay.TapAndPay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Sample host Activity for the Tap And Pay flow.
 *
 * Its responsibilities are to keep wallet state synchronized with `DataChangedListener`, register
 * the Activity result callback for push tokenization, and launch the Google Wallet intent returned
 * by the provisioning gateway.
 *
 * It also handles Google Wallet's Bounce Provisioning flow: when this Activity is launched via
 * `ACTION_INITIATE_PROVISIONING` (declared in the manifest), the card search screen is shown as
 * usual -- there is no extra navigation step, no overlay, and no popover before it, per Google's
 * UX requirement that the user land directly on the eligible-cards screen -- and the resulting
 * `pushTokenize` call is flagged as bounce-provisioned. There is no requirement to close this
 * Activity or navigate back to Google Wallet once provisioning finishes: the confirmation shown to
 * the user (see [PushProvisioningResultResolver]) is enough, matching Google's own sample apps.
 *
 * Docs: https://developers.google.com/pay/issuers/apis/push-provisioning/android/bounce-provisioning
 */
class MainActivity : ComponentActivity() {
    private val viewModel: CardSearchViewModel by viewModel()
    private val walletProvisioningGateway: WalletProvisioningGateway by inject()
    private lateinit var pushProvisioningLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var walletDataChangedListener: TapAndPay.DataChangedListener? = null
    private var isBounceProvisioning = false

    /**
     * Uses registerForActivityResult to listen for Google Wallet's push-tokenize Activity result.
     *
     * Docs:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#handling_result_callbacks
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isBounceProvisioning = intent.isBounceProvisioningIntent()

        val pushProvisioningContract = ActivityResultContracts.StartIntentSenderForResult()
        pushProvisioningLauncher =
            registerForActivityResult(pushProvisioningContract) { result ->
                handlePushTokenizeResult(result.resultCode, result.data)
            }

        setContent {
            ExamplegoogleuppTheme(dynamicColor = true) {
                CardSearchRoute(viewModel = viewModel) { uiState ->
                    val selectedCard = uiState.selectedCard

                    if (selectedCard != null) {
                        when (uiState.walletButtonState) {
                            WalletButtonState.UNAVAILABLE -> Text("Google Wallet unavailable")
                            WalletButtonState.ALREADY_ADDED ->
                                Text("Card already added to Google Wallet")
                            WalletButtonState.READY_TO_ADD ->
                                GoogleWalletProvisionButton(
                                    onClick = { launchProvisioningIntent(selectedCard) }
                                )
                        }
                    }
                }
            }
        }
    }

    /**
     * Registers wallet-change callbacks to keep local eligibility in sync with Google Wallet.
     *
     * Docs:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#data_change_callbacks
     */
    override fun onStart() {
        super.onStart()
        if (walletDataChangedListener == null) {
            walletDataChangedListener =
                TapAndPay.DataChangedListener { viewModel.refreshWalletEligibility() }
                    .also(walletProvisioningGateway::registerDataChangedListener)
        }
    }

    /** Rechecks eligibility because wallet state can change while the app is backgrounded. */
    override fun onResume() {
        super.onResume()
        viewModel.refreshWalletEligibility()
    }

    /** Removes the wallet-change callback when this Activity is no longer visible. */
    override fun onStop() {
        walletDataChangedListener?.let(walletProvisioningGateway::removeDataChangedListener)
        walletDataChangedListener = null
        super.onStop()
    }

    /** Requests the push-tokenize PendingIntent and launches the Google Wallet flow. */
    private fun launchProvisioningIntent(card: Card) {
        lifecycleScope.launch {
            try {
                val pendingIntent =
                    walletProvisioningGateway.createPushTokenizePendingIntent(
                        card = card,
                        isBounceProvisioned = isBounceProvisioning,
                    )
                val request = IntentSenderRequest.Builder(pendingIntent).build()
                pushProvisioningLauncher.launch(request)
            } catch (error: Exception) {
                viewModel.onProvisioningResult(
                    PushProvisioningResult.Error(
                        statusCode = null,
                        message = error.message ?: "Unable to start Google Wallet provisioning",
                    )
                )
            }
        }
    }

    /**
     * Handles the Activity result returned by Google Wallet after push tokenization finishes.
     *
     * Docs:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#handling_result_callbacks
     */
    private fun handlePushTokenizeResult(resultCode: Int, data: Intent?) {
        viewModel.onProvisioningResult(PushProvisioningResultResolver.resolve(resultCode, data))
    }
}

/**
 * Whether this Intent launched the Activity through Google Wallet's Bounce Provisioning flow.
 *
 * Docs: https://developers.google.com/pay/issuers/apis/push-provisioning/android/bounce-provisioning
 */
private fun Intent.isBounceProvisioningIntent(): Boolean =
    action == "com.google.android.gms.tapandpay.issuer.ACTION_INITIATE_PROVISIONING"
