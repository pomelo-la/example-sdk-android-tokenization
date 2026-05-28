package com.example.example_google_upp.wallet

import com.example.example_google_upp.data.BackendService
import com.example.example_google_upp.data.dto.ProvisioningRequestDto
import com.example.example_google_upp.model.Brand
import com.example.example_google_upp.model.Card
import com.google.android.gms.tapandpay.issuer.GeneratePaymentCredentialsRequest
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class PomeloCredentialsGeneratorTest {
    private val gson = Gson()

    @Test
    fun `generate forwards Google-provided Visa context to backend`() = runTest {
        val card =
            Card(
                cardId = "crd-456",
                userId = "usr-456",
                lastFour = "4242",
                cardholderName = "Pomelo User",
                brand = Brand.VISA,
            )
        var capturedRequest: ProvisioningRequestDto? = null
        val backendService = createService { request ->
            capturedRequest = requestBodyDto(request)
            assertEquals("/push-provisioning/visa/google-pay", request.url.encodedPath)
            respondJson("""{"opc":"tsp-opc","google_opc":"google-opc"}""")
        }
        val generator =
            PomeloCredentialsGenerator(card = card, backendService = backendService)
        val request =
            GeneratePaymentCredentialsRequest.Builder()
                .setServerSessionId("session-from-google")
                .setStableHardwareId("device-from-google")
                .setWalletId("wallet-from-google")
                .setGoogleOpaquePaymentCardRequested(true)
                .build()

        val future = generator.generate(request)
        future.get(1, TimeUnit.SECONDS)

        assertTrue(future.isDone)
        assertEquals("session-from-google", capturedRequest?.serverSessionId)
        assertEquals("device-from-google", capturedRequest?.deviceId)
        assertEquals("wallet-from-google", capturedRequest?.walletAccountId)
    }

    private fun createService(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): BackendService =
        BackendService(baseUrl = "https://example.test/", client = HttpClient(MockEngine(handler)))

    private fun requestBody(request: HttpRequestData): String {
        val body = request.body
        require(body is TextContent) { "Expected TextContent but was ${body::class.simpleName}" }

        assertEquals(ContentType.Application.Json, body.contentType.withoutParameters())
        return body.text
    }

    private fun requestBodyDto(request: HttpRequestData): ProvisioningRequestDto =
        gson.fromJson(requestBody(request), ProvisioningRequestDto::class.java)

    private fun MockRequestHandleScope.respondJson(body: String) =
        respond(
            content = body,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
}
