package io.novafoundation.nova.feature_staking_impl.domain.common.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrWarning
import java.math.BigDecimal

class ProfitableActionValidation<P, E>(
    val amount: P.() -> BigDecimal,
    val fee: P.() -> BigDecimal,
    val error: (P) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val isProfitable = value.fee() < value.amount()

        return isProfitable isTrueOrWarning {
            error(value)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.profitableAction(
    amount: P.() -> BigDecimal,
    fee: P.() -> BigDecimal,
    error: (P) -> E
) {
    validate(ProfitableActionValidation(amount, fee, error))
}
