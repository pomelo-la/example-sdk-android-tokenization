package com.example.example_google_upp.a2a

import android.content.Intent
import com.example.example_google_upp.a2a.models.StepUpResult

/**
 * Traduce el resultado del flujo de App-to-App Verification en un Intent con el extra
 * `STEP_UP_RESPONSE` que Google Wallet espera recibir.
 *
 * Docs:
 * https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv
 */
object StepUpResultResolver {
    private const val STEP_UP_RESPONSE_EXTRA = "STEP_UP_RESPONSE"

    /**
     * Convierte un [StepUpResult] en un Intent con el extra `STEP_UP_RESPONSE`.
     *
     * @param result El resultado del flujo A2A.
     * @return Un Intent con el extra `STEP_UP_RESPONSE` configurado.
     */
    fun toResultIntent(result: StepUpResult): Intent = Intent().apply {
        putExtra(STEP_UP_RESPONSE_EXTRA, result.toStepUpResponseValue())
    }

    private fun StepUpResult.toStepUpResponseValue(): String = when (this) {
        StepUpResult.Approved -> "approved"
        StepUpResult.Declined -> "declined"
        is StepUpResult.Failure -> "failure"
    }
}
