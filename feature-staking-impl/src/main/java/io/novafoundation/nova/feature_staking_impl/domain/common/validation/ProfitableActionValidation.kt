package io.novafoundation.nova.feature_staking_impl.domain.common.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrWarning
import io.novafoundation.nova.feature_wallet_api.domain.validation.FeeProducer
import io.novafoundation.nova.feature_wallet_api.presentation.model.networkFeeDecimalAmount
import java.math.BigDecimal

class ProfitableActionValidation<P, E>(
    val amount: P.() -> BigDecimal,
    val fee: FeeProducer<P>,
    val error: (P) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        // No matter who paid the fee we check that it is profitable
        val isProfitable = fee(value).networkFeeDecimalAmount < value.amount()

        return isProfitable isTrueOrWarning {
            error(value)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.profitableAction(
    amount: P.() -> BigDecimal,
    fee: FeeProducer<P>,
    error: (P) -> E
) {
    validate(ProfitableActionValidation(amount, fee, error))
}
