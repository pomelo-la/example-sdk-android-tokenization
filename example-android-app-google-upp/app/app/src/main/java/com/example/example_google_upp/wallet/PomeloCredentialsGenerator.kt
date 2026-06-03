package com.example.example_google_upp.wallet

import com.example.example_google_upp.data.BackendService
import com.example.example_google_upp.model.Card
import com.google.android.gms.tapandpay.issuer.GeneratePaymentCredentialsRequest
import com.google.android.gms.tapandpay.issuer.GeneratePaymentCredentialsResponse
import com.google.android.gms.tapandpay.issuer.PaymentCredentialsGenerator
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import kotlin.concurrent.thread

/**
 * Pomelo implementation of Tap And Pay's PaymentCredentialsGenerator interface.
 *
 * Its responsibility is to fetch issuer payment credentials when Google Wallet reaches the
 * push-tokenize step that needs OPC data.
 *
 * Docs:
 * https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#paymentcredentialsgenerator_interface
 */
class PomeloCredentialsGenerator(
    private val card: Card,
    private val backendService: BackendService,
) : PaymentCredentialsGenerator {
    /**
     * Receives Google's GeneratePaymentCredentialsRequest and returns a credentials response.
     *
     * The request provides the session and device context (`serverSessionId`, `stableHardwareId`,
     * and `walletId`). This sample sends those values to Pomelo through BackendService and uses the
     * returned Google OPC to build GeneratePaymentCredentialsResponse.
     *
     * Request docs:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#generatepaymentcredentialsrequest_interface
     *
     * Response docs:
     * https://developers.google.com/pay/issuers/apis/push-provisioning/android/wallet-operations#generatepaymentcredentialsresponse_interface
     */
    override fun generate(
        request: GeneratePaymentCredentialsRequest
    ): Future<GeneratePaymentCredentialsResponse> {
        val futureTask = FutureTask {
            val provisioningData = runBlocking {
                backendService.getProvisioningData(
                    card = card,
                    serverSessionId = request.serverSessionId,
                    deviceId = request.stableHardwareId,
                    walletAccountId = request.walletId,
                )
            }

            val (opaquePaymentCard, googleOpaquePaymentCard) = provisioningData

            GeneratePaymentCredentialsResponse.Builder()
                .setGoogleOpaquePaymentCard(googleOpaquePaymentCard.toByteArray())
                .setOpaquePaymentCard(opaquePaymentCard.toByteArray())
                .build()
        }

        thread(start = true, isDaemon = true, name = "pomelo-payment-credentials") {
            futureTask.run()
        }

        return futureTask
    }

    override fun getAuxiliaryOpaquePaymentCardSupported() = false

    /** Signals that this sample provides a Google OPC for saving the FPAN to the Google Account. */
    override fun getGoogleOpaquePaymentCardSupported(): Boolean = true
}
