package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import java.math.BigDecimal

class PositiveAmountValidation<P, E>(
    val amountExtractor: (P) -> BigDecimal,
    val errorProvider: () -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        return if (amountExtractor(value) > BigDecimal.ZERO) {
            ValidationStatus.Valid()
        } else {
            ValidationStatus.NotValid(DefaultFailureLevel.ERROR, errorProvider())
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.positiveAmount(
    amount: (P) -> BigDecimal,
    error: () -> E
) = validate(
    PositiveAmountValidation(
        amountExtractor = amount,
        errorProvider = error
    )
)
