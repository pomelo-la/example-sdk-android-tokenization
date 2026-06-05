package com.example.example_google_upp.wallet

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import com.example.example_google_upp.wallet.models.PushProvisioningResult
import com.google.android.gms.tapandpay.TapAndPayStatusCodes
import com.google.android.gms.tapandpay.issuer.PushTokenizeResult
import com.google.android.gms.tapandpay.issuer.TokenizationOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class PushProvisioningResultResolverTest {
    @Test
    fun `missing push tokenize payload is error for any activity result`() {
        listOf(Activity.RESULT_OK, Activity.RESULT_CANCELED, 12345).forEach { activityResultCode ->
            val result = PushProvisioningResultResolver.resolve(activityResultCode, null)

            assertEquals(
                PushProvisioningResult.Error(
                    statusCode = activityResultCode,
                    message = "Google Wallet provisioning did not return Tap And Pay details",
                ),
                result,
            )
        }
    }

    @Test
    fun `successful push tokenize payload is success for any activity result`() {
        assertPayloadResolvesForAnyActivityResult(
            pushTokenizeResult(outcomes = listOf(tokenizationOutcome(tokenStatus = 0))),
            PushProvisioningResult.Success,
        )
    }

    @Test
    fun `failed card result is error before checking successful tokenization outcome`() {
        val cardStatus = TapAndPayStatusCodes.TAP_AND_PAY_SAVE_CARD_ERROR

        assertPayloadResolvesForAnyActivityResult(
            pushTokenizeResult(cardStatus = cardStatus),
            PushProvisioningResult.Error(
                statusCode = cardStatus,
                message = "Google Wallet provisioning failed (status=$cardStatus)",
            ),
        )
    }

    @Test
    fun `cancelled card status is cancelled before checking successful tokenization outcome`() {
        val cardStatus = TapAndPayStatusCodes.TAP_AND_PAY_USER_CANCELED_FLOW

        assertPayloadResolvesForAnyActivityResult(
            pushTokenizeResult(cardStatus = cardStatus),
            PushProvisioningResult.Cancelled(cardStatus),
        )
    }

    @Test
    fun `failed tokenization outcome is error`() {
        val tokenStatus = TapAndPayStatusCodes.TAP_AND_PAY_TOKENIZE_ERROR

        assertPayloadResolvesForAnyActivityResult(
            pushTokenizeResult(outcomes = listOf(tokenizationOutcome(tokenStatus))),
            PushProvisioningResult.Error(
                statusCode = tokenStatus,
                message = "Google Wallet provisioning failed (status=$tokenStatus)",
            ),
        )
    }

    @Test
    fun `user cancelled tokenization outcome is cancelled`() {
        val tokenStatus = TapAndPayStatusCodes.TAP_AND_PAY_USER_CANCELED_FLOW

        assertPayloadResolvesForAnyActivityResult(
            pushTokenizeResult(outcomes = listOf(tokenizationOutcome(tokenStatus))),
            PushProvisioningResult.Cancelled(tokenStatus),
        )
    }

    @Test
    fun `missing tokenization outcomes is error`() {
        assertPayloadResolvesForAnyActivityResult(
            pushTokenizeResult(outcomes = emptyList()),
            PushProvisioningResult.Error(
                statusCode = null,
                message = "Google Wallet provisioning did not return a tokenization outcome",
            ),
        )
    }

    private fun assertPayloadResolvesForAnyActivityResult(
        pushTokenizeResult: PushTokenizeResult,
        expected: PushProvisioningResult,
    ) {
        listOf(Activity.RESULT_OK, Activity.RESULT_CANCELED, 12345).forEach { activityResultCode ->
            val result =
                PushProvisioningResultResolver.resolve(
                    activityResultCode,
                    PushTokenizeResultIntent(pushTokenizeResult),
                )

            assertEquals(expected, result)
        }
    }

    private fun pushTokenizeResult(
        cardStatus: Int = 0,
        outcomes: List<TokenizationOutcome> = listOf(tokenizationOutcome(tokenStatus = 0)),
    ): PushTokenizeResult {
        val constructor =
            PushTokenizeResult::class
                .java
                .getDeclaredConstructor(
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    List::class.java,
                )
        constructor.isAccessible = true
        return constructor.newInstance(cardStatus, 0, outcomes)
    }

    private fun tokenizationOutcome(tokenStatus: Int): TokenizationOutcome {
        val constructor =
            TokenizationOutcome::class
                .java
                .getDeclaredConstructor(
                    Int::class.javaPrimitiveType,
                    String::class.java,
                    String::class.java,
                    String::class.java,
                )
        constructor.isAccessible = true
        return constructor.newInstance(tokenStatus, "issuer-token-id", null, "wallet-id")
    }

    private class PushTokenizeResultIntent(
        private val pushTokenizeResult: PushTokenizeResult
    ) : Intent() {
        @Deprecated("Deprecated by Android SDK")
        @Suppress("UNCHECKED_CAST")
        override fun <T : Parcelable?> getParcelableExtra(name: String?): T? =
            pushTokenizeResult as T

        override fun <T : Any?> getParcelableExtra(name: String?, clazz: Class<T>): T? =
            clazz.cast(pushTokenizeResult)
    }
}
