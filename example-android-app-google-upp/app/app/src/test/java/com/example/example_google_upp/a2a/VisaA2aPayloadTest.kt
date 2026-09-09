package com.example.example_google_upp.a2a.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class VisaA2aPayloadTest {

    private val gson = Gson()

    @Test
    fun `payload valido parsea todos los campos correctamente`() {
        val payload = VisaA2aPayload(
            panReferenceID = "V-3815023863409817870482",
            tokenRequestorID = "42301999123",
            tokenReferenceID = "DNITHE381502386342002358",
            panLast4 = "1234",
            deviceID = "device-123",
            walletAccountID = "wallet-456"
        )
        val json = gson.toJson(payload)
        val base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(
            json.toByteArray(Charsets.UTF_8)
        )

        val result = VisaA2aPayload.fromExtraText(base64, gson)

        assertNotNull(result)
        assertEquals("V-3815023863409817870482", result?.panReferenceID)
        assertEquals("42301999123", result?.tokenRequestorID)
        assertEquals("DNITHE381502386342002358", result?.tokenReferenceID)
        assertEquals("1234", result?.panLast4)
        assertEquals("device-123", result?.deviceID)
        assertEquals("wallet-456", result?.walletAccountID)
    }

    @Test
    fun `extraText nulo devuelve null`() {
        val result = VisaA2aPayload.fromExtraText(null, gson)
        assertNull(result)
    }

    @Test
    fun `extraText vacio devuelve null`() {
        val result = VisaA2aPayload.fromExtraText("", gson)
        assertNull(result)
    }

    @Test
    fun `extraText con solo espacios devuelve null`() {
        val result = VisaA2aPayload.fromExtraText("   ", gson)
        assertNull(result)
    }

    @Test
    fun `base64 corrupto no decodifica devuelve null`() {
        val result = VisaA2aPayload.fromExtraText("!!!invalid-base64!!!", gson)
        assertNull(result)
    }

    @Test
    fun `JSON valido en base64 con campos faltantes devuelve payload con esos campos en null`() {
        val partialJson = """{"panLast4":"9999","tokenReferenceID":"TOKEN-123"}"""
        val base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(
            partialJson.toByteArray(Charsets.UTF_8)
        )

        val result = VisaA2aPayload.fromExtraText(base64, gson)

        assertNotNull(result)
        assertNull(result?.panReferenceID)
        assertNull(result?.tokenRequestorID)
        assertNull(result?.deviceID)
        assertNull(result?.walletAccountID)
        assertEquals("9999", result?.panLast4)
        assertEquals("TOKEN-123", result?.tokenReferenceID)
    }

    @Test
    fun `JSON con campos extra desconocidos no falla`() {
        val jsonWithExtra = """{"panLast4":"1234","extraField":"ignored","anotherUnknown":123}"""
        val base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(
            jsonWithExtra.toByteArray(Charsets.UTF_8)
        )

        val result = VisaA2aPayload.fromExtraText(base64, gson)

        assertNotNull(result)
        assertEquals("1234", result?.panLast4)
        // Gson ignora campos desconocidos por defecto
    }

    @Test
    fun `base64 con padding URL safe funciona correctamente`() {
        // El payload de ejemplo de la guía
        val json = """{"panReferenceID":"V-3815023863409817870482","tokenRequestorID":"42301999123","tokenReferenceID":"DNITHE381502386342002358","panLast4":"1234"}"""
        val base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(
            json.toByteArray(Charsets.UTF_8)
        )

        val result = VisaA2aPayload.fromExtraText(base64, gson)

        assertNotNull(result)
        assertEquals("1234", result?.panLast4)
    }

    @Test
    fun `base64 standard funciona igual que URL safe`() {
        val json = """{"panLast4":"5678"}"""
        val base64Standard = Base64.getEncoder().withoutPadding().encodeToString(
            json.toByteArray(Charsets.UTF_8)
        )

        val result = VisaA2aPayload.fromExtraText(base64Standard, gson)

        assertNotNull(result)
        assertEquals("5678", result?.panLast4)
    }
}
