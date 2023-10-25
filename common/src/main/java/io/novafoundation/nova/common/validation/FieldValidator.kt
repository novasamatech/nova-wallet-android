package io.novafoundation.nova.common.validation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FieldValidator {
    fun observe(inputStream: Flow<String>): Flow<FieldValidationResult>

    companion object
}

abstract class MapFieldValidator : FieldValidator {

    abstract suspend fun validate(input: String): FieldValidationResult

    override fun observe(inputStream: Flow<String>) = inputStream.map(::validate)
}

sealed class FieldValidationResult {
    object Ok : FieldValidationResult()

    class Error(val reason: String) : FieldValidationResult()

    class Warning(val reason: String) : FieldValidationResult()
}
