package com.example.example_google_upp.ui

import android.app.PendingIntent
import com.example.example_google_upp.data.BackendService
import com.example.example_google_upp.model.Brand
import com.example.example_google_upp.model.Card
import com.example.example_google_upp.wallet.models.PushProvisioningResult
import com.example.example_google_upp.wallet.models.WalletProvisioningGateway
import com.google.android.gms.tapandpay.TapAndPay
import com.google.gson.Gson
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CardSearchViewModelTest {
    private val gson = Gson()

    @Test
    fun `initial state is empty`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel =
                CardSearchViewModel(
                    backendService = unusedBackendService(),
                    walletProvisioningGateway = FakeWalletProvisioningGateway(),
                )

            val state = viewModel.uiState.value

            assertFalse(state.isLoading)
            assertNull(state.selectedCard)
            assertEquals(WalletButtonState.UNAVAILABLE, state.walletButtonState)
            assertNull(state.snackbarMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `refresh wallet eligibility marks card ready to add when card is not tokenized`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                val card = sampleCard()
                val gateway = FakeWalletProvisioningGateway(isTokenized = false)
                val viewModel =
                    CardSearchViewModel(
                        backendService =
                            createService { request ->
                                when (requestCardId(request)) {
                                    card.cardId -> respondCard(card)
                                    else -> error("Unexpected path: ${request.url.encodedPath}")
                                }
                            },
                        walletProvisioningGateway = gateway,
                    )

                viewModel.search(card.cardId).getOrThrow()
                awaitState { viewModel.uiState.value.selectedCard?.cardId == card.cardId }

                viewModel.refreshWalletEligibility()
                awaitState {
                    viewModel.uiState.value.walletButtonState == WalletButtonState.READY_TO_ADD
                }

                val state = viewModel.uiState.value

                assertEquals(WalletButtonState.READY_TO_ADD, state.walletButtonState)
                assertEquals(listOf(card), gateway.requestedCards)
                assertNull(state.snackbarMessage)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `refresh wallet eligibility marks card already added when card is tokenized`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                val card = sampleCard()
                val viewModel =
                    CardSearchViewModel(
                        backendService =
                            createService { request ->
                                when (requestCardId(request)) {
                                    card.cardId -> respondCard(card)
                                    else -> error("Unexpected path: ${request.url.encodedPath}")
                                }
                        },
                    walletProvisioningGateway =
                            FakeWalletProvisioningGateway(isTokenized = true),
                    )

                viewModel.search(card.cardId).getOrThrow()
                awaitState { viewModel.uiState.value.selectedCard?.cardId == card.cardId }

                viewModel.refreshWalletEligibility()
                awaitState {
                    viewModel.uiState.value.walletButtonState == WalletButtonState.ALREADY_ADDED
                }

                assertEquals(
                    WalletButtonState.ALREADY_ADDED,
                    viewModel.uiState.value.walletButtonState,
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `refresh wallet eligibility exposes gateway error`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val card = sampleCard()
            val viewModel =
                CardSearchViewModel(
                    backendService =
                        createService { request ->
                            when (requestCardId(request)) {
                                card.cardId -> respondCard(card)
                                else -> error("Unexpected path: ${request.url.encodedPath}")
                            }
                        },
                    walletProvisioningGateway =
                        FakeWalletProvisioningGateway(
                            eligibilityError = IllegalStateException("TapAndPay unavailable")
                        ),
                )

            viewModel.search(card.cardId).getOrThrow()
            awaitState { viewModel.uiState.value.selectedCard?.cardId == card.cardId }

            viewModel.refreshWalletEligibility()
            awaitState { viewModel.uiState.value.snackbarMessage == "TapAndPay unavailable" }

            val state = viewModel.uiState.value

            assertEquals(WalletButtonState.UNAVAILABLE, state.walletButtonState)
            assertEquals("TapAndPay unavailable", state.snackbarMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `search failure returns backend error`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel =
                CardSearchViewModel(
                    backendService =
                        createService { request ->
                            when (requestCardId(request)) {
                                "crd-missing" -> throw IllegalStateException("Card not found")
                                else -> error("Unexpected path: ${request.url.encodedPath}")
                            }
                        },
                    walletProvisioningGateway = FakeWalletProvisioningGateway(),
                )

            val result = viewModel.search("crd-missing")

            val state = viewModel.uiState.value

            assertEquals("Card not found", result.exceptionOrNull()?.message)
            assertFalse(state.isLoading)
            assertNull(state.snackbarMessage)
            assertNull(state.selectedCard)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `failed search clears previously selected and provisionable card`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val loadedCard =
                Card(
                    cardId = "crd-123",
                    userId = "usr-123",
                    lastFour = "1573",
                    cardholderName = "Dieguito",
                    brand = Brand.MASTERCARD,
                )
            val viewModel =
                CardSearchViewModel(
                    backendService =
                        createService { request ->
                            when (requestCardId(request)) {
                                "crd-123" -> respondCard(loadedCard)
                                "crd-missing" -> throw IllegalStateException("Card not found")
                                else -> error("Unexpected path: ${request.url.encodedPath}")
                            }
                        },
                    walletProvisioningGateway =
                        FakeWalletProvisioningGateway(isTokenized = false),
                )

            viewModel.search("crd-123").getOrThrow()
            awaitState {
                !viewModel.uiState.value.isLoading &&
                    viewModel.uiState.value.selectedCard?.cardId == "crd-123"
            }
            viewModel.refreshWalletEligibility()
            awaitState {
                viewModel.uiState.value.walletButtonState == WalletButtonState.READY_TO_ADD
            }

            val result = viewModel.search("crd-missing")

            val state = viewModel.uiState.value

            assertEquals("Card not found", result.exceptionOrNull()?.message)
            assertNull(state.selectedCard)
            assertEquals(WalletButtonState.UNAVAILABLE, state.walletButtonState)
            assertNull(state.snackbarMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `blank search query returns validation error without calling backend`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel =
                CardSearchViewModel(
                    backendService = unusedBackendService(),
                    walletProvisioningGateway = FakeWalletProvisioningGateway(),
                )

            val result = viewModel.search("   ")

            val state = viewModel.uiState.value

            assertEquals("Enter a card ID", result.exceptionOrNull()?.message)
            assertFalse(state.isLoading)
            assertNull(state.snackbarMessage)
            assertNull(state.selectedCard)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `provisioning success stores snackbar message and refreshes wallet eligibility`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                val card = sampleCard()
                val gateway = FakeWalletProvisioningGateway(isTokenized = false)
                val viewModel =
                    CardSearchViewModel(
                        backendService =
                            createService { request ->
                                when (requestCardId(request)) {
                                    card.cardId -> respondCard(card)
                                    else -> error("Unexpected path: ${request.url.encodedPath}")
                                }
                            },
                        walletProvisioningGateway = gateway,
                    )

                viewModel.search(card.cardId).getOrThrow()
                awaitState { viewModel.uiState.value.selectedCard?.cardId == card.cardId }
                viewModel.onProvisioningResult(PushProvisioningResult.Success)
                awaitState { gateway.requestedCards == listOf(card) }

                assertEquals("Card added to Google Wallet", viewModel.uiState.value.snackbarMessage)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `provisioning cancellation stores snackbar message until shown`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel =
                CardSearchViewModel(
                    backendService = unusedBackendService(),
                    walletProvisioningGateway = FakeWalletProvisioningGateway(),
                )

            viewModel.onProvisioningResult(PushProvisioningResult.Cancelled(statusCode = 15027))

            assertEquals(
                "Google Wallet provisioning canceled",
                viewModel.uiState.value.snackbarMessage,
            )

            viewModel.onSnackbarShown()

            assertNull(viewModel.uiState.value.snackbarMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `provisioning failure stores snackbar message`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel =
                CardSearchViewModel(
                    backendService = unusedBackendService(),
                    walletProvisioningGateway = FakeWalletProvisioningGateway(),
                )

            viewModel.onProvisioningResult(
                PushProvisioningResult.Error(statusCode = null, message = "Provisioning failed")
            )

            val state = viewModel.uiState.value

            assertEquals("Provisioning failed", state.snackbarMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createService(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): BackendService =
        BackendService(
            baseUrl = "https://backend.test",
            client = HttpClient(MockEngine(handler)),
            gson = gson,
        )

    private fun unusedBackendService(): BackendService = createService { request ->
        error("Unexpected backend request: ${request.url.encodedPath}")
    }

    private fun sampleCard(): Card =
        Card(
            cardId = "crd-123",
            userId = "usr-123",
            lastFour = "1573",
            cardholderName = "Dieguito",
            brand = Brand.MASTERCARD,
        )

    private fun requestCardId(request: HttpRequestData): String =
        request.url.encodedPath.trimStart('/').substringAfter("cards/")

    private fun MockRequestHandleScope.respondCard(card: Card): HttpResponseData =
        respondJson(
            gson.toJson(
                mapOf(
                    "cardId" to card.cardId,
                    "userId" to card.userId,
                    "lastFour" to card.lastFour,
                    "cardholderName" to card.cardholderName,
                    "brand" to card.brand.name,
                )
            )
        )

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

    private class FakeWalletProvisioningGateway(
        private val isTokenized: Boolean = false,
        private val eligibilityError: Throwable? = null,
    ) : WalletProvisioningGateway {
        val requestedCards = mutableListOf<Card>()

        override suspend fun isTokenized(card: Card): Boolean {
            requestedCards += card
            eligibilityError?.let { throw it }
            return isTokenized
        }

        override suspend fun createPushTokenizePendingIntent(card: Card): PendingIntent =
            error("Unexpected push tokenize request for ${card.cardId}")

        override fun registerDataChangedListener(listener: TapAndPay.DataChangedListener) = Unit

        override fun removeDataChangedListener(listener: TapAndPay.DataChangedListener) = Unit
    }
}
