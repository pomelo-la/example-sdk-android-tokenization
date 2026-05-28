package com.example.example_google_upp.wallet.models

import com.example.example_google_upp.model.Brand
import com.example.example_google_upp.model.User
import com.google.android.gms.tapandpay.TapAndPay
import com.google.android.gms.tapandpay.issuer.UserAddress

internal fun Brand.toTapAndPayNetwork(): Int =
    when (this) {
        Brand.VISA -> TapAndPay.CARD_NETWORK_VISA
        Brand.MASTERCARD -> TapAndPay.CARD_NETWORK_MASTERCARD
    }

internal fun Brand.toTapAndPayProvider(): Int =
    when (this) {
        Brand.VISA -> TapAndPay.TOKEN_PROVIDER_VISA
        Brand.MASTERCARD -> TapAndPay.TOKEN_PROVIDER_MASTERCARD
    }

internal fun User.toTapAndPayUserAddress(): UserAddress =
    UserAddress.newBuilder()
        .setName(listOf(name, surname).joinToString(" ").trim())
        .setAddress1(addressLine1)
        .apply { addressLine2?.takeIf { it.isNotBlank() }?.let(::setAddress2) }
        .setLocality(locality)
        .setAdministrativeArea(administrativeArea)
        .setPostalCode(postalCode)
        .setCountryCode(countryCode)
        .build()
