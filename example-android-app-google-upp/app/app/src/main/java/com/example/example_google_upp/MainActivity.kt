package com.example.example_google_upp

/**
 * Regular launcher entry point of the sample app. See [WalletProvisioningActivity] for the shared
 * Tap And Pay flow implementation.
 */
class MainActivity : WalletProvisioningActivity() {
    override val isBounceProvisioned: Boolean = false
}
