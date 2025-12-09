package com.pomelo.tkn_sdk.data.repository

import com.pomelo.tkn_sdk.util.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AuthRepository {

  /**
   * Example implementation - Replace with your actual backend call.
   *
   * This method currently returns a dummy token for demonstration purposes.
   * You must replace this implementation with a real API call to your backend
   * that returns a valid end-user token for the tokenization process.
   *
   * @param userId The user identifier to obtain the token for. Note: This parameter
   *               is used for demonstration purposes only. Your actual implementation
   *               may not require this parameter or may use different parameters
   *               depending on your backend authentication requirements.
   * @return NetworkResult containing the end-user token or an error.
   */
  suspend fun getUserEndToken(userId: String): NetworkResult<String> =
    withContext(Dispatchers.IO) {
      delay(300)
      NetworkResult.Success("DUMMY_TOKEN")
    }
}
