package com.example.example_google_upp.wallet

import android.content.Intent
import android.util.Log
import androidx.core.content.IntentCompat
import com.example.example_google_upp.wallet.models.PushProvisioningResult
import com.google.android.gms.tapandpay.TapAndPay
import com.google.android.gms.tapandpay.TapAndPayStatusCodes
import com.google.android.gms.tapandpay.issuer.PushTokenizeResult
import com.google.android.gms.tapandpay.issuer.TokenizationOutcome

/**
 * Translates Google Wallet push-tokenize callbacks into the app's provisioning result model.
 *
 * MainActivity delegates the Activity result to this resolver, and the returned
 * PushProvisioningResult is then consumed by the ViewModel to update snackbars and wallet state.
 * Keeping this mapping isolated makes the sample easier to read: the Activity handles Android
 * lifecycle concerns, while this object handles Tap And Pay result interpretation.
 */
object PushProvisioningResultResolver {
    private const val TAG = "PushProvisioningResultResolver"

    /**
     * Basic push-tokenize result handler for success, cancellation, and error cases.
     *
     * This sample intentionally keeps the result surface small: successful tokenization becomes
     * Success. Cancellation is reserved for Tap And Pay cancellation statuses
     * (`TAP_AND_PAY_USER_CANCELED_FLOW` and `CANCELED`). Missing payloads, missing tokenization
     * outcomes, card-save failures, and all other Tap And Pay statuses are treated as Error. The
     * implementation is based on the migration-to-UPP sample under "Sample code for
     * pushTokenize(...)".
     *
     * Docs:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/upgrade_to_upp#sample_code_for_pushtokenize
     */
    fun resolve(activityResultCode: Int, data: Intent?): PushProvisioningResult {
        val pushTokenizeResult = data?.let {
            IntentCompat.getParcelableExtra(
                it,
                TapAndPay.EXTRA_PUSH_TOKENIZE_RESULT,
                PushTokenizeResult::class.java,
            )
        }
        logPushTokenizeResult(activityResultCode, pushTokenizeResult)

        if (pushTokenizeResult == null) {
            return PushProvisioningResult.Error(
                "Google Wallet provisioning did not return Tap And Pay details",
                statusCode = null,
            )
        }

        return resolveOutcomes(pushTokenizeResult.tokenizationOutcomes)
    }

    /** Reads the tokenization outcomes that reports whether the card token was provisioned. */
    private fun resolveOutcomes(outcomes: List<TokenizationOutcome>): PushProvisioningResult {
        val outcome =
            outcomes.firstOrNull()
                ?: return PushProvisioningResult.Error(
                    message = "Google Wallet provisioning failed",
                    statusCode = null,
                )

        if (outcome.tokenResult) return PushProvisioningResult.Success

        return when (outcome.tokenStatus) {
            TapAndPayStatusCodes.TAP_AND_PAY_USER_CANCELED_FLOW,
            TapAndPayStatusCodes.CANCELED -> PushProvisioningResult.Cancelled(outcome.tokenStatus)

            else ->
                PushProvisioningResult.Error(
                    statusCode = outcome.tokenStatus,
                    message = "Google Wallet provisioning failed (status=$outcome.tokenStatus)",
                )
        }
    }

    private fun logPushTokenizeResult(
        activityResultCode: Int,
        pushTokenizeResult: PushTokenizeResult?,
    ) {
        if (pushTokenizeResult == null) {
            logDebug(
                "PushTokenizeResult activityResultCode=$activityResultCode. pushTokenizeResult is null"
            )
            return
        }

        val outcome = pushTokenizeResult.tokenizationOutcomes.firstOrNull()
        logDebug(
            "PushTokenizeResult activityResultCode=$activityResultCode cardResult=${pushTokenizeResult.cardResult} cardStatus=${pushTokenizeResult.cardStatus} outcomes=${pushTokenizeResult.tokenizationOutcomes.size} firstTokenResult=${outcome?.tokenResult} firstTokenStatus=${outcome?.tokenStatus}"
        )
    }

    private fun logDebug(message: String) {
        runCatching { Log.d(TAG, message) }
    }
}
