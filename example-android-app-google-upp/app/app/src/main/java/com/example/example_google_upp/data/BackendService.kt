package com.example.example_google_upp.data

import android.util.Log
import com.example.example_google_upp.data.dto.CardDto
import com.example.example_google_upp.data.dto.ProvisioningCredentialsDto
import com.example.example_google_upp.data.dto.ProvisioningRequestDto
import com.example.example_google_upp.data.dto.UserDto
import com.example.example_google_upp.model.Brand
import com.example.example_google_upp.model.Card
import com.example.example_google_upp.model.ProvisioningData
import com.example.example_google_upp.model.User
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent

class BackendService
internal constructor(
    private val baseUrl: String,
    private val client: HttpClient,
    private val gson: Gson = Gson(),
) {

    suspend fun getProvisioningData(
        card: Card,
        serverSessionId: String,
        deviceId: String,
        walletAccountId: String,
    ): ProvisioningData {
        val request = ProvisioningRequestDto.from(card, serverSessionId, deviceId, walletAccountId)
        val endpoint =
            when (card.brand) {
                Brand.VISA -> "push-provisioning/visa/google-pay"
                Brand.MASTERCARD -> "push-provisioning/mastercard/google-pay"
            }
        val credentials = post<ProvisioningCredentialsDto>(endpoint, request)

        return credentials.toDomain()
    }

    suspend fun getUser(userId: String): User = get<UserDto>("users/$userId").toDomain()

    suspend fun getCardById(cardId: String): Card = get<CardDto>("cards/$cardId").toDomain()

    /**
     * Activa un token de tarjeta tokenizada.
     *
     * Este endpoint se usa en el flujo de App-to-App Verification (A2A) cuando Google Wallet
     * necesita confirmar la identidad del titular antes de activar el token (Yellow Path).
     *
     * @param tokenId El ID del token a activar (generalmente el tokenReferenceID del payload A2A).
     * @param motive El motivo de la activación. Por defecto "APP_TO_APP_ACTIVATION".
     *
     * Docs:
     * https://developers.google.com/pay/issuers/tsp-integration/app-to-app-idv
     */
    suspend fun activateToken(tokenId: String, motive: String = "APP_TO_APP_ACTIVATION") {
        post<Unit>("tokens/$tokenId/activate", mapOf("motive" to motive))
    }

    private suspend inline fun <reified T> get(path: String): T =
        gson.fromJson(client.get(url(path)).bodyAsText(), T::class.java)

    private suspend inline fun <reified T> post(path: String, payload: Any): T {
        val response = client.post(url(path)) {
            setBody(
                TextContent(
                    text = gson.toJson(payload),
                    contentType = ContentType.Application.Json,
                )
            )
        }

        // Verificar que la respuesta sea exitosa (2xx)
        if (response.status.value !in 200..299) {
            throw Exception("HTTP ${response.status.value}: ${response.bodyAsText()}")
        }

        return gson.fromJson(response.bodyAsText(), T::class.java)
    }

    private fun url(path: String): String = "${baseUrl.trimEnd('/')}/$path"

    companion object {
        fun create(baseUrl: String): BackendService =
            BackendService(
                baseUrl = baseUrl,
                client =
                    HttpClient(OkHttp) {
                        install(Logging) {
                            logger =
                                object : Logger {
                                    override fun log(message: String) {
                                        Log.d("BackendHttp", message)
                                    }
                                }
                            level = LogLevel.ALL
                        }
                    },
            )
    }
}
