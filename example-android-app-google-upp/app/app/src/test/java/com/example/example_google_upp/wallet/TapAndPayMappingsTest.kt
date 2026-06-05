package com.example.example_google_upp.wallet

import com.example.example_google_upp.model.User
import com.example.example_google_upp.wallet.models.toTapAndPayUserAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TapAndPayMappingsTest {
    @Test
    fun `toTapAndPayUserAddress maps user name and address fields`() {
        val user =
            User(
                id = "usr-123",
                name = "Oscar",
                surname = "Odon",
                addressLine1 = "Rua Amadeo Alvarez Gandara 226",
                addressLine2 = "Casa G5",
                locality = "Marilia",
                administrativeArea = "SP",
                postalCode = "17516-636",
                countryCode = "BRA",
            )

        val result = user.toTapAndPayUserAddress()

        assertEquals("Oscar Odon", result.name)
        assertEquals("Rua Amadeo Alvarez Gandara 226", result.address1)
        assertEquals("Casa G5", result.address2)
        assertEquals("Marilia", result.locality)
        assertEquals("SP", result.administrativeArea)
        assertEquals("17516-636", result.postalCode)
        assertEquals("BRA", result.countryCode)
    }

    @Test
    fun `toTapAndPayUserAddress skips blank optional address line`() {
        val user =
            User(
                id = "usr-456",
                name = "Lisa",
                surname = "Simpson",
                addressLine1 = "12 Main St",
                addressLine2 = " ",
                locality = "Springfield",
                administrativeArea = "IL",
                postalCode = "62701",
                countryCode = "US",
            )

        val result = user.toTapAndPayUserAddress()

        assertEquals("Lisa Simpson", result.name)
        assertNull(result.address2)
    }
}
