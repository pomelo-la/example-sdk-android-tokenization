package com.pomelo.tkn_sdk.data.repository

import com.pomelo.sdk.pushprovisioning.model.Brand
import com.pomelo.tkn_sdk.data.model.CardDto
import com.pomelo.tkn_sdk.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class CardsRepository {

  private val hardcodedCards =
      listOf(
          CardDto(
              cardId = "crd-123",
              userId = "usr-123",
              lastFour = "5987",
              cardholderName = "Juan Pérez",
              brand = Brand.VISA,
          ),
          CardDto(
              cardId = "crd-456",
              userId = "usr-002",
              lastFour = "5678",
              cardholderName = "María García",
              brand = Brand.MASTERCARD,
          ),
          CardDto(
              cardId = "crd-789",
              userId = "usr-003",
              lastFour = "9012",
              cardholderName = "Carlos López",
              brand = Brand.VISA,
          ),
      )

  suspend fun getCards(): NetworkResult<List<CardDto>> =
      withContext(Dispatchers.IO) {
        delay(500)
        NetworkResult.Success(hardcodedCards)
      }
}
