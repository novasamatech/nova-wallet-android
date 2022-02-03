package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrError

class EnoughTotalToStayAboveEDValidation<P, E>(
    private val fee: AmountProducer<P>,
    private val totalBalance: AmountProducer<P>,
    private val existentialDeposit: AmountProducer<P>,
    private val error: (P) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        return validOrError(totalBalance(value) - fee(value) >= existentialDeposit(value)) {
            error(value)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.enoughTotalToStayAboveED(
    fee: AmountProducer<P>,
    total: AmountProducer<P>,
    existentialDeposit: AmountProducer<P>,
    error: (P) -> E
) = validate(
    EnoughTotalToStayAboveEDValidation(
        fee = fee,
        existentialDeposit = existentialDeposit,
        totalBalance = total,
        error = error
    )
)
