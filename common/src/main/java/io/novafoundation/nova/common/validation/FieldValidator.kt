package io.novafoundation.nova.common.validation

import kotlinx.coroutines.flow.Flow

interface FieldValidator {
    fun observe(inputStream: Flow<String>): Flow<FieldValidationResult>

    companion object
}

sealed class FieldValidationResult {
    object Ok : FieldValidationResult()

    class Error(val reason: String) : FieldValidationResult()

    class Warning(val reason: String) : FieldValidationResult()
}
