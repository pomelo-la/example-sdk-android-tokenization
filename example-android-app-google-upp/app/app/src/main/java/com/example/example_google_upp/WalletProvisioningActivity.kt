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
 * Shared host Activity for the Tap And Pay flow, reused by [MainActivity] and
 * [BounceProvisioningActivity].
 *
 * Its responsibilities are to keep wallet state synchronized with `DataChangedListener`, register
 * the Activity result callback for push tokenization, and launch the Google Wallet intent returned
 * by the provisioning gateway.
 *
 * [isBounceProvisioned] is fixed per subclass instead of being inferred from the launching Intent.
 * Bounce Provisioning is only ever entered through [BounceProvisioningActivity], which runs in its
 * own task (see its manifest declaration) precisely so that cold start (app not running) and hot
 * start (app already open) behave the same way: `finish()` always tears down that isolated task and
 * returns the user to Google Wallet, instead of falling back onto an existing [MainActivity]
 * instance sitting underneath in the same back stack.
 *
 * Docs: https://developers.google.com/pay/issuers/apis/push-provisioning/android/bounce-provisioning
 */
abstract class WalletProvisioningActivity : ComponentActivity() {
    private val viewModel: CardSearchViewModel by viewModel()
    private val walletProvisioningGateway: WalletProvisioningGateway by inject()
    private lateinit var pushProvisioningLauncher: ActivityResultLauncher<IntentSenderRequest>
    private var walletDataChangedListener: TapAndPay.DataChangedListener? = null

    /** Whether this Activity instance is the Bounce Provisioning entry point. */
    protected abstract val isBounceProvisioned: Boolean

    /**
     * Uses registerForActivityResult to listen for Google Wallet's push-tokenize Activity result.
     *
     * Docs:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#handling_result_callbacks
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        isBounceProvisioned = isBounceProvisioned,
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
     * When this Activity is the Bounce Provisioning entry point, it closes itself once the result is
     * handled so the user is returned to Google Wallet instead of staying on the issuer app.
     *
     * Docs:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#handling_result_callbacks
     */
    private fun handlePushTokenizeResult(resultCode: Int, data: Intent?) {
        viewModel.onProvisioningResult(PushProvisioningResultResolver.resolve(resultCode, data))
        if (isBounceProvisioned) {
            finish()
        }
    }
}
