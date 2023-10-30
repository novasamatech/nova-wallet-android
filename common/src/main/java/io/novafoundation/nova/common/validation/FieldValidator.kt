package io.novafoundation.nova.common.validation

import io.novafoundation.nova.common.view.ValidatableInputField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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

fun ValidatableInputField.observeErrors(
    flow: Flow<FieldValidationResult>,
    scope: CoroutineScope,
) {
    scope.launch {
        flow.collect { validationResult ->
            when (validationResult) {
                is FieldValidationResult.Ok,
                is FieldValidationResult.Warning -> {
                    hideError()
                }

                is FieldValidationResult.Error -> {
                    showError(validationResult.reason)
                }
            }
        }
    }
}
