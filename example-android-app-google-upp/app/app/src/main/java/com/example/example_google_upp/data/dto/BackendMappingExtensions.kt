package com.example.example_google_upp.data.dto

import com.example.example_google_upp.model.Brand

internal fun String?.optionalField(): String? = this?.trim().takeUnless { it.isNullOrEmpty() }

internal fun String?.requireField(fieldName: String): String =
    optionalField() ?: throw IllegalStateException("Backend response is missing $fieldName")

internal fun String?.toBrand(): Brand =
    this?.trim()?.uppercase()?.let { normalized ->
        Brand.entries.firstOrNull { it.name == normalized }
    } ?: throw IllegalStateException("Backend response has unsupported brand: $this")
