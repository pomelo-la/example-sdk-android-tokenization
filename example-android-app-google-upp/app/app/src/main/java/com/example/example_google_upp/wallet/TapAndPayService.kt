package com.example.example_google_upp.wallet

import android.app.PendingIntent
import com.example.example_google_upp.data.BackendService
import com.example.example_google_upp.model.Card
import com.example.example_google_upp.wallet.models.WalletProvisioningGateway
import com.example.example_google_upp.wallet.models.toTapAndPayNetwork
import com.example.example_google_upp.wallet.models.toTapAndPayProvider
import com.example.example_google_upp.wallet.models.toTapAndPayUserAddress
import com.google.android.gms.tapandpay.TapAndPay
import com.google.android.gms.tapandpay.TapAndPayClient
import com.google.android.gms.tapandpay.issuer.IsTokenizedRequest
import com.google.android.gms.tapandpay.issuer.PushTokenizeRequest
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Gateway around TapAndPayClient plus the issuer backend data needed for provisioning. */
class TapAndPayService(
    private val tapAndPayClient: TapAndPayClient,
    private val backendService: BackendService,
) : WalletProvisioningGateway {
    /**
     * Returns whether this card is already tokenized in the current device's Google Wallet.
     *
     * The lookup uses the card last four digits, network, and token service provider required by
     * `isTokenized`.
     *
     * Docs:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet#istokenized
     */
    override suspend fun isTokenized(card: Card): Boolean {
        val request =
            IsTokenizedRequest.Builder()
                .setIdentifier(card.lastFour)
                .setNetwork(card.brand.toTapAndPayNetwork())
                .setTokenServiceProvider(card.brand.toTapAndPayProvider())
                .build()

        return tapAndPayClient.isTokenized(request).awaitTask()
    }

    /**
     * Builds the UserAddress and PushTokenizeRequest used to launch Google Wallet provisioning.
     *
     * PomeloCredentialsGenerator is passed as the PaymentCredentialsGenerator so Tap And Pay can
     * ask the issuer backend for the Google OPC during the push-tokenize flow.
     *
     * Docs:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#pushtokenize
     */
    override suspend fun createPushTokenizePendingIntent(card: Card): PendingIntent {
        val user = backendService.getUser(card.userId)

        val request =
            PushTokenizeRequest.Builder()
                .setNetwork(card.brand.toTapAndPayNetwork())
                .setTokenServiceProvider(card.brand.toTapAndPayProvider())
                .setLastDigits(card.lastFour)
                .setDisplayName("${card.cardholderName} ${card.lastFour}")
                .setUserAddress(user.toTapAndPayUserAddress())
                .setPaymentCredentialsGenerator(
                    PomeloCredentialsGenerator(
                        card = card,
                        backendService = backendService,
                    )
                )
                .build()

        return tapAndPayClient.pushTokenize(request).awaitTask()
    }

    /** Registers Tap And Pay wallet-change callbacks used to refresh local eligibility. */
    override fun registerDataChangedListener(listener: TapAndPay.DataChangedListener) {
        tapAndPayClient.registerDataChangedListener(listener)
    }

    /** Removes the wallet-change callback when the Activity leaves the foreground. */
    override fun removeDataChangedListener(listener: TapAndPay.DataChangedListener) {
        tapAndPayClient.removeDataChangedListener(listener)
    }

    /** Bridges Google Play services Task callbacks into cancellable coroutines. */
    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWithException(error)
            }
        }
        addOnCanceledListener {
            if (continuation.isActive) {
                continuation.resumeWithException(
                    CancellationException("Tap And Pay task was cancelled")
                )
            }
        }
    }
}
