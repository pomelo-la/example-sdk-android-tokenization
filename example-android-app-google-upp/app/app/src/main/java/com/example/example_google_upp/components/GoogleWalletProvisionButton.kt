package com.example.example_google_upp.components

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.tapandpay.ExperimentalTapAndPayApi
import com.google.android.gms.tapandpay.TapAndPay
import com.google.android.gms.tapandpay.issuer.ProvisionButton

const val GOOGLE_WALLET_BUTTON_TAG = "google-wallet-button"

/**
 * Compose entry point for the official "Add to Google Wallet" button
 *
 * Uses the Provision Button API with programmatic integration.
 *
 * The dynamic button keeps Google Wallet branding current and handles localization and scaling for
 * the issuer app. The SDK supports primary and condensed display modes; this sample uses primary.
 *
 * Official docs:
 * https://developers.google.com/pay/issuers/apis/push-provisioning/android/provision-button-api
 */
@OptIn(ExperimentalTapAndPayApi::class)
@Composable
fun GoogleWalletProvisionButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    AndroidView(
        modifier = modifier.testTag(GOOGLE_WALLET_BUTTON_TAG),
        factory = { context ->
            ProvisionButton(context).apply {
                setDisplayMode(TapAndPay.PROVISION_BUTTON_DISPLAY_MODE_PRIMARY)
                setOnClickListener { onClick() }
            }
        },
        update = { provisionButton -> provisionButton.setOnClickListener { onClick() } },
    )
}
