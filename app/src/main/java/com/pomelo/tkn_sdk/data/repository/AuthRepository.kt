package com.pomelo.tkn_sdk.data.repository

import com.pomelo.tkn_sdk.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AuthRepository {

    suspend fun getUserEndToken(userId: String): NetworkResult<String> =
        withContext(Dispatchers.IO) {
            delay(300)
            NetworkResult.Success("DUMMY_TOKEN_$userId")
        }
}
