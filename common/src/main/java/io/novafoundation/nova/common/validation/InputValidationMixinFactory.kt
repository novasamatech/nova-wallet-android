package io.novafoundation.nova.common.validation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InputValidationMixinFactory<P, E>(
    private val validationSystem: ValidationSystem<P, E>,
) {
    fun create(inputFlow: Flow<String>, payloadFormatter: (String) -> P): InputValidationMixin<P, E> {
        return InputValidationMixin(validationSystem, inputFlow, payloadFormatter)
    }
}

class InputValidationMixin<P, E>(
    private val validationSystem: ValidationSystem<P, E>,
    private val inputFlow: Flow<String>,
    private val payloadFormatter: (String) -> P
) {
    fun observeStatus(): Flow<Result<ValidationStatus<E>>> {
        return inputFlow.map {
            val payload = payloadFormatter(it)
            validationSystem.validate(payload)
        }
    }
}
