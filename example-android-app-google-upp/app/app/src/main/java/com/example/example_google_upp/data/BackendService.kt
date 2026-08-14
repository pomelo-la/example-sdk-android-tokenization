package com.example.example_google_upp.data

import android.util.Log
import com.example.example_google_upp.data.dto.AppToAppActivationRequestDto
import com.example.example_google_upp.data.dto.AppToAppActivationResponseDto
import com.example.example_google_upp.data.dto.CardDto
import com.example.example_google_upp.data.dto.ProvisioningCredentialsDto
import com.example.example_google_upp.data.dto.ProvisioningRequestDto
import com.example.example_google_upp.data.dto.UserDto
import com.example.example_google_upp.model.AppToAppActivationResult
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
     * Activates a token during Visa's App2App (IDV) step-up flow.
     *
     * `deviceId` comes from the Visa payload Google Wallet sends to
     * `VisaAppToAppVerificationActivity` and is only required for TSPs (currently Visa) that
     * validate the device on activation.
     */
    suspend fun activateAppToAppToken(tokenId: String, deviceId: String?): AppToAppActivationResult =
        post<AppToAppActivationResponseDto>(
                "tokens/$tokenId/app-to-app-activation",
                AppToAppActivationRequestDto(deviceId = deviceId),
            )
            .toDomain()

    private suspend inline fun <reified T> get(path: String): T =
        gson.fromJson(client.get(url(path)).bodyAsText(), T::class.java)

    private suspend inline fun <reified T> post(path: String, payload: Any): T =
        gson.fromJson(
            client
                .post(url(path)) {
                    setBody(
                        TextContent(
                            text = gson.toJson(payload),
                            contentType = ContentType.Application.Json,
                        )
                    )
                }
                .bodyAsText(),
            T::class.java,
        )

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
