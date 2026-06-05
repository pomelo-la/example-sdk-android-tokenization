package com.example.example_google_upp.data.dto

import com.example.example_google_upp.model.User
import com.google.gson.annotations.SerializedName

internal data class UserDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("surname") val surname: String?,
    @SerializedName("legal_address") val legalAddress: LegalAddressDto?,
) {
    fun toDomain(): User {
        val address =
            legalAddress ?: throw IllegalStateException("Backend response is missing legal_address")

        return User(
            id = id.requireField("id"),
            name = name.requireField("name"),
            surname = surname.requireField("surname"),
            addressLine1 = address.addressLine1(),
            addressLine2 = address.addressLine2(),
            locality = address.locality(),
            administrativeArea = address.administrativeArea(),
            postalCode = address.postalCode(),
            countryCode = address.countryCode(),
        )
    }
}

internal data class LegalAddressDto(
    @SerializedName("street_name") val streetName: String?,
    @SerializedName("street_number") val streetNumber: String?,
    @SerializedName("zip_code") val zipCode: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("region") val region: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("additional_info") val additionalInfo: String?,
    @SerializedName("floor") val floor: String?,
    @SerializedName("apartment") val apartment: String?,
) {
    fun addressLine1(): String =
        listOfNotNull(
                streetName.requireField("legal_address.street_name"),
                streetNumber.optionalField(),
            )
            .joinToString(" ")

    fun addressLine2(): String? =
        listOfNotNull(
                additionalInfo.optionalField(),
                floor.optionalField()?.let { "floor $it" },
                apartment.optionalField()?.let { "apartment $it" },
            )
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")

    fun locality(): String = city.requireField("legal_address.city")

    fun administrativeArea(): String = region.requireField("legal_address.region")

    fun postalCode(): String = zipCode.requireField("legal_address.zip_code")

    fun countryCode(): String = country.requireField("legal_address.country")
}
