package com.example.example_google_upp.wallet.models

import com.example.example_google_upp.model.AppToAppActivationResult

/**
 * Maps an [AppToAppActivationResult] to the `STEP_UP_RESPONSE` extra Visa's wallet provider
 * expects back in the Activity result Intent.
 *
 * This sample implements Visa's "Option 1" activation (the backend calls Visa's Token Lifecycle
 * API directly), so it only reports approved/declined/failure and never returns an authentication
 * code (TAV) — that would be "Option 2", documented separately by Visa.
 */
object VisaAppToAppStepUpResponse {
    const val EXTRA_STEP_UP_RESPONSE = "STEP_UP_RESPONSE"

    private const val RESULT_APPROVED = "approved"
    private const val RESULT_DECLINED = "declined"
    private const val RESULT_FAILURE = "failure"

    fun from(result: AppToAppActivationResult): String =
        when (result) {
            AppToAppActivationResult.Approved -> RESULT_APPROVED
            AppToAppActivationResult.Declined -> RESULT_DECLINED
            is AppToAppActivationResult.Failed -> RESULT_FAILURE
        }
}
