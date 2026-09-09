package com.example.example_google_upp.a2a

import com.example.example_google_upp.a2a.models.AppToAppUiState
import com.example.example_google_upp.a2a.models.StepUpResult
import com.example.example_google_upp.a2a.models.VisaA2aPayload
import com.example.example_google_upp.data.BackendService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppToAppViewModelTest {

    private val validPayload = VisaA2aPayload(
        panReferenceID = "V-123",
        tokenRequestorID = "TR-456",
        tokenReferenceID = "TOKEN-789",
        panLast4 = "1234",
        deviceID = "device-123",
        walletAccountID = "wallet-456"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `payload invalido al iniciar emite Failure sin pasar por autenticacion`() = runTest {
        val viewModel = AppToAppViewModel(createService())

        viewModel.init(payload = null, isValidCaller = true)

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AppToAppUiState.InvalidRequest)
        assertEquals("Invalid or missing payload", (uiState as AppToAppUiState.InvalidRequest).reason)

        val finalResult = viewModel.finalResult.first()
        assertTrue(finalResult is StepUpResult.Failure)
        assertEquals("Invalid or missing payload", (finalResult as StepUpResult.Failure).message)
    }

    @Test
    fun `caller invalido al iniciar emite Failure sin pasar por autenticacion`() = runTest {
        val viewModel = AppToAppViewModel(createService())

        viewModel.init(payload = validPayload, isValidCaller = false)

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AppToAppUiState.InvalidRequest)
        assertEquals("Invalid caller: not Google Wallet", (uiState as AppToAppUiState.InvalidRequest).reason)

        val finalResult = viewModel.finalResult.first()
        assertTrue(finalResult is StepUpResult.Failure)
        assertEquals("Invalid caller: not Google Wallet", (finalResult as StepUpResult.Failure).message)
    }

    @Test
    fun `payload y caller validos inicializan en AwaitingAuthentication`() = runTest {
        val viewModel = AppToAppViewModel(createService())

        viewModel.init(payload = validPayload, isValidCaller = true)

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AppToAppUiState.AwaitingAuthentication)
        assertEquals(validPayload, (uiState as AppToAppUiState.AwaitingAuthentication).payload)
    }

    @Test
    fun `onSimulatedAuthenticationConfirmed pasa a Authenticated`() = runTest {
        val viewModel = AppToAppViewModel(createService())

        viewModel.init(payload = validPayload, isValidCaller = true)
        viewModel.onSimulatedAuthenticationConfirmed()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AppToAppUiState.Authenticated)
        assertEquals(validPayload, (uiState as AppToAppUiState.Authenticated).payload)
    }

    @Test
    fun `onAuthenticationDeclined emite Declined sin llamar a backend`() = runTest {
        val service = createService()
        val viewModel = AppToAppViewModel(service)

        viewModel.init(payload = validPayload, isValidCaller = true)
        viewModel.onAuthenticationDeclined()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AppToAppUiState.Finished)
        assertEquals(StepUpResult.Declined, (uiState as AppToAppUiState.Finished).result)

        val finalResult = viewModel.finalResult.first()
        assertEquals(StepUpResult.Declined, finalResult)
    }

    @Test
    fun `flujo feliz autenticacion exitosa y activacion exitosa emite Approved`() = runTest {
        val service = createService(activateTokenSuccess = true)
        val viewModel = AppToAppViewModel(service)

        viewModel.init(payload = validPayload, isValidCaller = true)
        viewModel.onSimulatedAuthenticationConfirmed()
        viewModel.onActivate()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AppToAppUiState.Finished)
        assertEquals(StepUpResult.Approved, (uiState as AppToAppUiState.Finished).result)

        val finalResult = viewModel.finalResult.first()
        assertEquals(StepUpResult.Approved, finalResult)
    }

    @Test
    fun `onActivate con error del backend emite Failure`() = runTest {
        val service = createService(activateTokenSuccess = false)
        val viewModel = AppToAppViewModel(service)

        viewModel.init(payload = validPayload, isValidCaller = true)
        viewModel.onSimulatedAuthenticationConfirmed()
        viewModel.onActivate()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AppToAppUiState.Finished)
        val result = (uiState as AppToAppUiState.Finished).result
        assertTrue(result is StepUpResult.Failure)
    }

    @Test
    fun `estado Activating durante la llamada al backend`() = runTest {
        // Crear un mock engine que no responde inmediatamente
        val mockEngine = MockEngine {
            // No respondemos inmediatamente
            respond(content = "", status = HttpStatusCode.Accepted)
        }
        val service = BackendService.createForTest(mockEngine)
        val viewModel = AppToAppViewModel(service)

        viewModel.init(payload = validPayload, isValidCaller = true)
        viewModel.onSimulatedAuthenticationConfirmed()
        viewModel.onActivate()

        // Estado debería ser Activating inmediatamente después de llamar onActivate
        val uiState = viewModel.uiState.value
        assertTrue(uiState is AppToAppUiState.Activating)
    }

    private fun createService(activateTokenSuccess: Boolean = true): BackendService {
        val mockEngine = MockEngine { request ->
            when {
                request.url.encodedPath.contains("/activate") -> {
                    if (activateTokenSuccess) {
                        respond(content = "{}", status = HttpStatusCode.Accepted)
                    } else {
                        respond(content = "Error", status = HttpStatusCode.InternalServerError)
                    }
                }
                else -> respond(content = "", status = HttpStatusCode.NotFound)
            }
        }
        return BackendService.createForTest(mockEngine)
    }
}

// Extensión para crear un BackendService con un mock engine para testing
private fun BackendService.Companion.createForTest(mockEngine: MockEngine): BackendService {
    return BackendService(
        baseUrl = "https://backend.test",
        client = HttpClient(mockEngine)
    )
}
