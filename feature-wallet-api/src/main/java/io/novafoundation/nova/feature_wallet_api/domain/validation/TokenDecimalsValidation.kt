package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError

class TokenDecimalsValidation<P, E>(
    private val decimals: (P) -> Int,
    private val error: (P) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        return validOrError(decimals(value) in 0..36) {
            error(value)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.validTokenDecimals(
    decimals: (P) -> Int,
    error: (P) -> E
) = validate(
    TokenDecimalsValidation(decimals, error)
)
