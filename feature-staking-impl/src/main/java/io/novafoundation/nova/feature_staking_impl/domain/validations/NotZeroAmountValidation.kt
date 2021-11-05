package io.novafoundation.nova.feature_staking_impl.domain.validations

import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import java.math.BigDecimal

class NotZeroAmountValidation <P, E>(
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
