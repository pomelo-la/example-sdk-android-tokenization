package com.example.example_google_upp.wallet.models

import android.app.PendingIntent
import com.example.example_google_upp.model.Card
import com.google.android.gms.tapandpay.TapAndPay

interface WalletProvisioningGateway {
    suspend fun isTokenized(card: Card): Boolean

    suspend fun createPushTokenizePendingIntent(card: Card): PendingIntent

    fun registerDataChangedListener(listener: TapAndPay.DataChangedListener)

    fun removeDataChangedListener(listener: TapAndPay.DataChangedListener)
}
