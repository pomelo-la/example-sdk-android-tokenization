package com.example.example_google_upp.wallet.models

import android.app.PendingIntent
import com.example.example_google_upp.model.Card
import com.google.android.gms.tapandpay.TapAndPay

interface WalletProvisioningGateway {
    suspend fun isTokenized(card: Card): Boolean

    /**
     * @param isBounceProvisioned Whether this push-tokenize call was triggered by Google Wallet's
     *   Bounce Provisioning flow (the app was launched via `ACTION_INITIATE_PROVISIONING`). Forwarded
     *   to Tap And Pay as `PushTokenizeExtraOptions` so Google can track the flow's origin.
     */
    suspend fun createPushTokenizePendingIntent(
        card: Card,
        isBounceProvisioned: Boolean = false,
    ): PendingIntent

    fun registerDataChangedListener(listener: TapAndPay.DataChangedListener)

    fun removeDataChangedListener(listener: TapAndPay.DataChangedListener)
}
