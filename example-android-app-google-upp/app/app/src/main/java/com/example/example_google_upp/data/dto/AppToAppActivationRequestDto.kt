package com.example.example_google_upp.data.dto

import com.google.gson.annotations.SerializedName

/**
 * `device_id` comes from the Visa App2App payload (`deviceID`) and is nullable because only Visa
 * validates the device on activation; other TSPs can call this same endpoint without it.
 */
internal data class AppToAppActivationRequestDto(
    @SerializedName("device_id") val deviceId: String?
)
