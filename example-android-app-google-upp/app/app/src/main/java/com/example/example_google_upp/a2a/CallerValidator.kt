package com.example.example_google_upp.a2a

/**
 * Valida que el caller de la Activity sea Google Wallet.
 *
 * Google Wallet siempre invoca la app emisora con un Intent explícito desde
 * `com.google.android.gms` (Google Play Services). Antes de mostrar cualquier
 * dato sensible, la app debe verificar que el caller sea el esperado.
 *
 * Docs:
 * https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv
 */
object CallerValidator {
    private const val GOOGLE_WALLET_PACKAGE = "com.google.android.gms"

    /**
     * Verifica si el paquete que invocó la Activity es Google Wallet.
     *
     * @param callingPackage El nombre del paquete del caller (obtenido via Activity.callingPackage).
     * @return `true` si el caller es Google Wallet, `false` en caso contrario.
     */
    fun isGoogleWallet(callingPackage: String?): Boolean = callingPackage == GOOGLE_WALLET_PACKAGE
}
