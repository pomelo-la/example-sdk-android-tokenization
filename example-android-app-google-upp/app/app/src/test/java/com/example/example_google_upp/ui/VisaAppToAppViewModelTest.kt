package com.example.example_google_upp.ui

import com.example.example_google_upp.data.BackendService
import com.example.example_google_upp.model.AppToAppActivationResult
import com.example.example_google_upp.model.Brand
import com.example.example_google_upp.wallet.models.VisaAppToAppPayload
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VisaAppToAppViewModelTest {
    @Test
    fun `payload without token reference id surfaces an error`() = runTest {
        val viewModel = VisaAppToAppViewModel(unusedBackendService())

        viewModel.onPayloadParsed(VisaAppToAppPayload(panLast4 = "4242"))

        val state = viewModel.uiState.value
        assertEquals("Google Wallet sent an invalid App2App payload", state.errorMessage)
        assertNull(state.card)
    }

    @Test
    fun `null payload surfaces an error`() = runTest {
        val viewModel = VisaAppToAppViewModel(unusedBackendService())

        viewModel.onPayloadParsed(null)

        assertEquals(
            "Google Wallet sent an invalid App2App payload",
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun `valid payload is reflected in the ui state`() = runTest {
        val viewModel = VisaAppToAppViewModel(unusedBackendService())

        viewModel.onPayloadParsed(
            VisaAppToAppPayload(
                tokenReferenceId = "token-123",
                panLast4 = "4242",
                walletAccountId = "wallet-123",
            )
        )

        val state = viewModel.uiState.value
        assertEquals("4242", state.card?.lastFour)
        assertEquals("token-123", state.card?.cardId)
        assertEquals("wallet-123", state.card?.userId)
        assertEquals("Pomelo Card", state.card?.cardholderName)
        assertEquals(Brand.VISA, state.card?.brand)
        assertNull(state.errorMessage)
    }

    @Test
    fun `activate stores the backend activation result`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val service =
                createService { request ->
                    assertEquals(
                        "/tokens/token-123/app-to-app-activation",
                        request.url.encodedPath,
                    )
                    respondJson(
                        """{ "external_token_id": "token-123", "activation_result": "APPROVED" }"""
                    )
                }
            val viewModel = VisaAppToAppViewModel(service)

            viewModel.activate(
                VisaAppToAppPayload(tokenReferenceId = "token-123", deviceId = "device-123")
            )
            awaitState { viewModel.uiState.value.activationResult != null }

            val state = viewModel.uiState.value
            assertEquals(AppToAppActivationResult.Approved, state.activationResult)
            assertEquals(false, state.isActivating)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `activate surfaces backend failures as a failed activation result`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val service = createService { error("Pomelo unavailable") }
            val viewModel = VisaAppToAppViewModel(service)

            viewModel.activate(VisaAppToAppPayload(tokenReferenceId = "token-123"))
            awaitState { viewModel.uiState.value.activationResult != null }

            assertEquals(
                AppToAppActivationResult.Failed("Pomelo unavailable"),
                viewModel.uiState.value.activationResult,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createService(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): BackendService =
        BackendService(baseUrl = "https://backend.test", client = HttpClient(MockEngine(handler)))

    private fun unusedBackendService(): BackendService = createService { request ->
        error("Unexpected backend request: ${request.url.encodedPath}")
    }

    private fun MockRequestHandleScope.respondJson(body: String): HttpResponseData =
        respond(
            content = body,
            status = HttpStatusCode.OK,
            headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
        )

    private suspend fun awaitState(condition: () -> Boolean) {
        val deadlineNanos = System.nanoTime() + 2_000_000_000L
        while (!condition()) {
            if (System.nanoTime() >= deadlineNanos) {
                throw AssertionError("Timed out waiting for view model state")
            }
            yield()
            delay(1)
        }
    }
}
