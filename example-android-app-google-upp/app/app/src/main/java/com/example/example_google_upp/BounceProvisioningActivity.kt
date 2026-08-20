package com.example.example_google_upp

/**
 * Dedicated entry point for Google Wallet's Bounce Provisioning flow, declared in the manifest with
 * `android:launchMode="singleTask"` and `android:taskAffinity=""` so it always runs isolated in its
 * own task, regardless of whether [MainActivity] is already open (hot start) or the app process
 * isn't running at all (cold start). That isolation is what makes `finish()` in
 * [WalletProvisioningActivity] reliably return the user to Google Wallet instead of surfacing an
 * existing [MainActivity] instance from the app's regular back stack.
 *
 * Docs: https://developers.google.com/pay/issuers/apis/push-provisioning/android/bounce-provisioning
 */
class BounceProvisioningActivity : WalletProvisioningActivity() {
    override val isBounceProvisioned: Boolean = true
}
