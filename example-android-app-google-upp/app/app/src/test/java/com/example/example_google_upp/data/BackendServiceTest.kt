package com.example.example_google_upp.data

import com.example.example_google_upp.data.dto.ProvisioningRequestDto
import com.example.example_google_upp.model.Brand
import com.example.example_google_upp.model.Card
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackendServiceTest {
    private val gson = Gson()

    @Test
    fun `getCardById returns mapped card`() = runTest {
        val service = createService { request: HttpRequestData ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/cards/crd-123", request.url.encodedPath)

            respondJson(
                """
                {
                  "cardId": "crd-123",
                  "userId": "usr-123",
                  "lastFour": "1573",
                  "cardholderName": "Dieguito",
                  "brand": "MASTERCARD"
                }
                """
                    .trimIndent()
            )
        }

        val result = service.getCardById("crd-123")

        assertEquals(
            Card(
                cardId = "crd-123",
                userId = "usr-123",
                lastFour = "1573",
                cardholderName = "Dieguito",
                brand = Brand.MASTERCARD,
            ),
            result,
        )
    }

    @Test
    fun `getCardById falls back when cardholder name is null`() = runTest {
        val service = createService { request: HttpRequestData ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/cards/crd-999", request.url.encodedPath)

            respondJson(
                """
                {
                  "cardId": "crd-999",
                  "userId": "usr-999",
                  "lastFour": "9999",
                  "cardholderName": null,
                  "brand": "VISA"
                }
                """
                    .trimIndent()
            )
        }

        val result = service.getCardById("crd-999")

        assertEquals(
            Card(
                cardId = "crd-999",
                userId = "usr-999",
                lastFour = "9999",
                cardholderName = "Pomelo Card",
                brand = Brand.VISA,
            ),
            result,
        )
    }

    @Test
    fun `getCardById fails with a clear error when required fields are missing`() = runTest {
        val service = createService { request: HttpRequestData ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/cards/crd-bad", request.url.encodedPath)

            respondJson(
                """
                {
                  "cardId": "crd-bad",
                  "userId": "usr-bad",
                  "lastFour": null,
                  "cardholderName": "Pomelo User",
                  "brand": "VISA"
                }
                """
                    .trimIndent()
            )
        }

        val error = captureFailure { service.getCardById("crd-bad") }

        assertNotNull(error)
        assertEquals("Backend response is missing lastFour", error?.message)
    }

    @Test
    fun `getProvisioningData forwards server session id for mastercard`() = runTest {
        val card =
            Card(
                cardId = "crd-123",
                userId = "usr-123",
                lastFour = "1573",
                cardholderName = "Dieguito",
                brand = Brand.MASTERCARD,
            )
        val service = createService { request: HttpRequestData ->
            when (request.url.encodedPath) {
                "/push-provisioning/mastercard/google-pay" -> {
                    assertEquals(HttpMethod.Post, request.method)
                    val payload = requestBodyDto(request)
                    assertEquals("crd-123", payload.cardId)
                    assertEquals("usr-123", payload.userId)
                    assertEquals("session-123", payload.serverSessionId)
                    assertEquals(null, payload.deviceId)
                    assertEquals(null, payload.walletAccountId)
                    respondJson(
                        """
                        {
                          "opc": "opaque-mastercard",
                          "google_opc": "google-opaque-mastercard"
                        }
                        """
                            .trimIndent()
                    )
                }

                else -> error("Unexpected path: ${request.url.encodedPath}")
            }
        }

        val result =
            service.getProvisioningData(
                card = card,
                serverSessionId = "session-123",
                deviceId = "device-123",
                walletAccountId = "wallet-123",
            )

        assertEquals("opaque-mastercard", result.opaquePaymentCard)
        assertEquals("google-opaque-mastercard", result.googleOpaquePaymentCard)
    }

    @Test
    fun `getUser maps legal address from backend user response`() = runTest {
        val card =
            Card(
                cardId = "crd-789",
                userId = "usr-2YXKNZe2epgtv1hSsaaiovpGpcf",
                lastFour = "1234",
                cardholderName = "Oscar Odon",
                brand = Brand.MASTERCARD,
            )
        val service = createService { request: HttpRequestData ->
            when (request.url.encodedPath) {
                "/users/usr-2YXKNZe2epgtv1hSsaaiovpGpcf" -> {
                    assertEquals(HttpMethod.Get, request.method)
                    respondJson(
                        """
                        {
                          "id": "usr-2YXKNZe2epgtv1hSsaaiovpGpcf",
                          "name": "Oscar",
                          "surname": "Odon",
                          "legal_address": {
                            "street_name": "Rua Amadeo Alvarez Gandara",
                            "street_number": "226",
                            "zip_code": "17516-636",
                            "neighborhood": "Marília (3529005)",
                            "city": "Marília",
                            "region": "SP",
                            "municipality": "3529005",
                            "country": "BRA",
                            "additional_info": "Casa G5",
                            "floor": "1",
                            "apartment": "A"
                          }
                        }
                        """
                            .trimIndent()
                    )
                }

                else -> error("Unexpected path: ${request.url.encodedPath}")
            }
        }

        val result = service.getUser(card.userId)

        assertEquals("usr-2YXKNZe2epgtv1hSsaaiovpGpcf", result.id)
        assertEquals("Oscar", result.name)
        assertEquals("Odon", result.surname)
        assertEquals("Rua Amadeo Alvarez Gandara 226", result.addressLine1)
        assertEquals("Casa G5, floor 1, apartment A", result.addressLine2)
        assertEquals("Marília", result.locality)
        assertEquals("SP", result.administrativeArea)
        assertEquals("17516-636", result.postalCode)
        assertEquals("BRA", result.countryCode)
    }

    @Test
    fun `getProvisioningData forwards session id for visa`() = runTest {
        val card =
            Card(
                cardId = "crd-456",
                userId = "usr-456",
                lastFour = "4242",
                cardholderName = "Pomelo User",
                brand = Brand.VISA,
            )
        val service = createService { request: HttpRequestData ->
            when (request.url.encodedPath) {
                "/push-provisioning/visa/google-pay" -> {
                    assertEquals(HttpMethod.Post, request.method)
                    val payload = requestBodyDto(request)
                    assertEquals("crd-456", payload.cardId)
                    assertEquals("usr-456", payload.userId)
                    assertEquals("session-123", payload.serverSessionId)
                    assertEquals("device-456", payload.deviceId)
                    assertEquals("wallet-456", payload.walletAccountId)
                    respondJson(
                        """
                        {
                          "opc": "opaque-visa",
                          "google_opc": "google-opaque-visa"
                        }
                        """
                            .trimIndent()
                    )
                }

                else -> error("Unexpected path: ${request.url.encodedPath}")
            }
        }

        val result =
            service.getProvisioningData(
                card = card,
                serverSessionId = "session-123",
                deviceId = "device-456",
                walletAccountId = "wallet-456",
            )

        assertEquals("opaque-visa", result.opaquePaymentCard)
        assertEquals("google-opaque-visa", result.googleOpaquePaymentCard)
    }

    @Test
    fun `getProvisioningData rejects visa without device id`() = runTest {
        val card =
            Card(
                cardId = "crd-456",
                userId = "usr-456",
                lastFour = "4242",
                cardholderName = "Pomelo User",
                brand = Brand.VISA,
            )
        val service = createService { error("Unexpected request: ${it.url.encodedPath}") }

        val error =
            captureFailure {
                service.getProvisioningData(
                    card = card,
                    serverSessionId = "session-123",
                    deviceId = "",
                    walletAccountId = "wallet-456",
                )
            }

        assertNotNull(error)
        assertEquals("Visa provisioning requires deviceId", error?.message)
    }

    @Test
    fun `getUser fails with a clear error when backend user address is incomplete`() =
        runTest {
            val card =
                Card(
                    cardId = "crd-456",
                    userId = "usr-456",
                    lastFour = "4242",
                    cardholderName = "Pomelo User",
                    brand = Brand.VISA,
                )
            val service = createService { request: HttpRequestData ->
                when (request.url.encodedPath) {
                    "/users/usr-456" -> {
                        assertEquals(HttpMethod.Get, request.method)
                        respondJson(
                            """
                            {
                              "id": "usr-456",
                              "name": "Lisa",
                              "surname": "Simpson",
                              "legal_address": {
                                "street_name": "12 Main St",
                                "street_number": null,
                                "zip_code": "49007",
                                "city": "Springfield",
                                "region": null,
                                "country": "US"
                              }
                            }
                            """
                                .trimIndent()
                        )
                    }

                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            }

            val error = captureFailure { service.getUser(card.userId) }

            assertNotNull(error)
            assertEquals("Backend response is missing legal_address.region", error?.message)
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

    private fun MockRequestHandleScope.respondJson(body: String): HttpResponseData =
        respond(
            content = body,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )

    private suspend fun captureFailure(block: suspend () -> Unit): Throwable? =
        try {
            block()
            null
        } catch (error: Throwable) {
            error
        }
}
