package io.novafoundation.nova.feature_staking_impl.domain.common.validation

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrWarning
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.SimpleFeeProducer
import java.math.BigDecimal

class ProfitableActionValidation<P, E>(
    val amount: P.() -> BigDecimal,
    val fee: SimpleFeeProducer<P>,
    val error: (P) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        // No matter who paid the fee we check that it is profitable
        val isProfitable = fee(value)?.decimalAmount.orZero() < value.amount()

        return isProfitable isTrueOrWarning {
            error(value)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.profitableAction(
    amount: P.() -> BigDecimal,
    fee: SimpleFeeProducer<P>,
    error: (P) -> E
) {
    validate(ProfitableActionValidation(amount, fee, error))
}
