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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
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

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `payload invalido al iniciar emite Failure sin pasar por biometria ni por un estado de UI dedicado`() =
        runTest(testDispatcher) {
            val viewModel = AppToAppViewModel(createService())

            viewModel.init(payload = null, isValidCaller = true)

            // Avanzar hasta que todas las coroutines terminen
            advanceUntilIdle()

            // No hay un estado de error dedicado: el control vuelve a Google Wallet directamente
            // por finalResult, sin que uiState transicione a nada distinto de Loading.
            assertTrue(viewModel.uiState.value is AppToAppUiState.Loading)

            val finalResult = viewModel.finalResult.first()
            assertTrue(finalResult is StepUpResult.Failure)
            assertEquals("Invalid or missing payload", (finalResult as StepUpResult.Failure).message)
        }

    @Test
    fun `caller invalido al iniciar emite Failure sin pasar por biometria ni por un estado de UI dedicado`() =
        runTest(testDispatcher) {
            val viewModel = AppToAppViewModel(createService())

            viewModel.init(payload = validPayload, isValidCaller = false)

            // Avanzar hasta que todas las coroutines terminen
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is AppToAppUiState.Loading)

            val finalResult = viewModel.finalResult.first()
            assertTrue(finalResult is StepUpResult.Failure)
            assertEquals("Invalid caller: not Google Wallet", (finalResult as StepUpResult.Failure).message)
        }

    @Test
    fun `payload y caller validos inicializan en BiometricPrompt`() = runTest(testDispatcher) {
        val viewModel = AppToAppViewModel(createService())

        viewModel.init(payload = validPayload, isValidCaller = true)

        // Avanzar hasta que todas las coroutines terminen
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AppToAppUiState.BiometricPrompt)
    }

    @Test
    fun `onAuthenticationConfirmed despues de biometria exitosa pasa a AwaitingConfirmation`() = runTest(testDispatcher) {
        val viewModel = AppToAppViewModel(createService())

        viewModel.init(payload = validPayload, isValidCaller = true)

        // Avanzar hasta que todas las coroutines terminen
        advanceUntilIdle()

        viewModel.onAuthenticationConfirmed()

        val uiState = viewModel.uiState.value
        assertTrue(uiState is AppToAppUiState.AwaitingConfirmation)
        assertEquals(validPayload, (uiState as AppToAppUiState.AwaitingConfirmation).payload)
    }

    @Test
    fun `onAuthenticationDeclined emite Declined sin pasar por un estado de UI dedicado`() = runTest(testDispatcher) {
        val viewModel = AppToAppViewModel(createService())

        viewModel.init(payload = validPayload, isValidCaller = true)

        // Avanzar hasta que todas las coroutines terminen
        advanceUntilIdle()

        viewModel.onAuthenticationDeclined()

        // Avanzar nuevamente después de onAuthenticationDeclined
        advanceUntilIdle()

        // uiState se queda en BiometricPrompt (nunca transiciona a nada nuevo): el control vuelve
        // a Google Wallet directamente por finalResult.
        assertTrue(viewModel.uiState.value is AppToAppUiState.BiometricPrompt)

        val finalResult = viewModel.finalResult.first()
        assertEquals(StepUpResult.Declined, finalResult)
    }

    @Test
    fun `onBiometricNotAvailable emite Failure sin pasar por un estado de UI dedicado`() = runTest(testDispatcher) {
        val viewModel = AppToAppViewModel(createService())

        viewModel.init(payload = validPayload, isValidCaller = true)

        // Avanzar hasta que todas las coroutines terminen
        advanceUntilIdle()

        viewModel.onBiometricNotAvailable()

        // Avanzar nuevamente después del método
        advanceUntilIdle()

        // uiState se queda en BiometricPrompt (nunca transiciona a Finished): el control vuelve a
        // Google Wallet directamente por finalResult.
        assertTrue(viewModel.uiState.value is AppToAppUiState.BiometricPrompt)

        val result = viewModel.finalResult.first()
        assertTrue(result is StepUpResult.Failure)
        assertEquals("Biometric authentication not available", (result as StepUpResult.Failure).message)
    }

    @Test
    fun `onBiometricError emite Failure con mensaje sin pasar por un estado de UI dedicado`() =
        runTest(testDispatcher) {
            val viewModel = AppToAppViewModel(createService())

            viewModel.init(payload = validPayload, isValidCaller = true)

            // Avanzar hasta que todas las coroutines terminen
            advanceUntilIdle()

            viewModel.onBiometricError("Hardware unavailable")

            // Avanzar nuevamente después del método
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is AppToAppUiState.BiometricPrompt)

            val result = viewModel.finalResult.first()
            assertTrue(result is StepUpResult.Failure)
            assertEquals("Hardware unavailable", (result as StepUpResult.Failure).message)
        }

    @Test
    fun `flujo feliz biometria exitosa y activacion exitosa emite Approved sin pasar por un estado de UI dedicado`() =
        runTest(testDispatcher) {
            val service = createService(activateTokenSuccess = true)
            val viewModel = AppToAppViewModel(service)

            viewModel.init(payload = validPayload, isValidCaller = true)

            // Avanzar hasta que todas las coroutines terminen
            advanceUntilIdle()

            viewModel.onAuthenticationConfirmed()
            viewModel.onActivate()

            // El MockEngine de Ktor ejecuta la request en Dispatchers.IO (un hilo real), no en
            // testDispatcher, así que advanceUntilIdle() no alcanza para esperarlo de forma
            // determinística. finalResult.first() sí suspende hasta que la corrutina real termine.
            val finalResult = viewModel.finalResult.first()
            assertEquals(StepUpResult.Approved, finalResult)

            // uiState se queda en Activating (nunca transiciona a nada nuevo): el control vuelve a
            // Google Wallet directamente por finalResult.
            assertTrue(viewModel.uiState.value is AppToAppUiState.Activating)
        }

    @Test
    fun `onActivate con error del backend emite Failure sin pasar por un estado de UI dedicado`() =
        runTest(testDispatcher) {
            val service = createService(activateTokenSuccess = false)
            val viewModel = AppToAppViewModel(service)

            viewModel.init(payload = validPayload, isValidCaller = true)

            // Avanzar hasta que todas las coroutines terminen
            advanceUntilIdle()

            viewModel.onAuthenticationConfirmed()
            viewModel.onActivate()

            // Ver comentario en el test anterior: hay que esperar la señal real, no virtual time.
            val finalResult = viewModel.finalResult.first()
            assertTrue(finalResult is StepUpResult.Failure)

            // uiState se queda en Activating (nunca transiciona a Finished): el control vuelve a
            // Google Wallet directamente por finalResult.
            assertTrue(viewModel.uiState.value is AppToAppUiState.Activating)
        }

    @Test
    fun `estado Activating durante la llamada al backend`() = runTest(testDispatcher) {
        val mockEngine = MockEngine {
            respond(content = "", status = HttpStatusCode.Accepted)
        }
        val service = BackendService.createForTest(mockEngine)
        val viewModel = AppToAppViewModel(service)

        viewModel.init(payload = validPayload, isValidCaller = true)

        // Avanzar hasta que todas las coroutines terminen
        advanceUntilIdle()

        viewModel.onAuthenticationConfirmed()
        viewModel.onActivate()

        // Verificar el estado inmediatamente después de onActivate (no esperamos que complete)
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

    // Test para verificar que el backend es llamado correctamente
    @Test
    fun `onActivate llama al backend con el tokenId correcto`() = runTest(testDispatcher) {
        val mockEngine = MockEngine { request ->
            // Verificar que se llama al endpoint correcto
            assertTrue(request.url.encodedPath.contains("/tokens/TOKEN-789/activate"))
            respond(content = "{}", status = HttpStatusCode.Accepted)
        }
        val service = BackendService.createForTest(mockEngine)
        val viewModel = AppToAppViewModel(service)

        viewModel.init(payload = validPayload, isValidCaller = true)
        advanceUntilIdle()

        viewModel.onAuthenticationConfirmed()
        viewModel.onActivate()

        // Ver comentario en "flujo feliz...": hay que esperar la señal real, no virtual time.
        val finalResult = viewModel.finalResult.first()
        assertEquals(StepUpResult.Approved, finalResult)

        // uiState se queda en Activating (nunca transiciona a nada nuevo): el control vuelve a
        // Google Wallet directamente por finalResult.
        assertTrue(viewModel.uiState.value is AppToAppUiState.Activating)
    }
}

// Extensión para crear un BackendService con un mock engine para testing
private fun BackendService.Companion.createForTest(mockEngine: MockEngine): BackendService {
    return BackendService(
        baseUrl = "https://backend.test",
        client = HttpClient(mockEngine)
    )
}
