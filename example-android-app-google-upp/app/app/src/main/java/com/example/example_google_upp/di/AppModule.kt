package com.example.example_google_upp.di

import com.example.example_google_upp.BuildConfig
import com.example.example_google_upp.data.BackendService
import com.example.example_google_upp.ui.CardSearchViewModel
import com.example.example_google_upp.ui.VisaAppToAppViewModel
import com.example.example_google_upp.wallet.TapAndPayService
import com.example.example_google_upp.wallet.models.WalletProvisioningGateway
import com.google.android.gms.tapandpay.TapAndPay
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { BackendService.create(BuildConfig.BACKEND_BASE_URL) }
    single { TapAndPay.getClient(androidContext()) }
    single<WalletProvisioningGateway> { TapAndPayService(get(), get()) }
    viewModelOf(::CardSearchViewModel)
    viewModelOf(::VisaAppToAppViewModel)
}
