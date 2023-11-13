package io.novafoundation.nova.common.validation

import io.novafoundation.nova.common.utils.combine
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

class CompoundFieldValidator(
    private val validators: List<FieldValidator>
) : FieldValidator {

    constructor(vararg validators: FieldValidator) : this(validators.toList())

    override fun observe(inputStream: Flow<String>): Flow<FieldValidationResult> {
        return validators.map { it.observe(inputStream) }
            .combine()
            .map {
                it.firstOrNull { it is FieldValidationResult.Error }
                    ?: FieldValidationResult.Ok
            }
    }
}

sealed class FieldValidationResult {
    object Ok : FieldValidationResult()

    class Error(val reason: String) : FieldValidationResult()
}

fun FieldValidationResult.getReasonOrNull(): String? {
    return when (this) {
        is FieldValidationResult.Error -> reason
        else -> null
    }
}

fun ValidatableInputField.observeErrors(
    flow: Flow<FieldValidationResult>,
    scope: CoroutineScope,
) {
    scope.launch {
        flow.collect { validationResult ->
            when (validationResult) {
                is FieldValidationResult.Ok -> {
                    hideError()
                }

                is FieldValidationResult.Error -> {
                    showError(validationResult.reason)
                }
            }
        }
    }
}
