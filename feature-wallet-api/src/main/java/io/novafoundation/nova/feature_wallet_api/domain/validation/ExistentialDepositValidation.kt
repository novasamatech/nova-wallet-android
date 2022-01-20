package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrWarning
import java.math.BigDecimal

class ExistentialDepositValidation<P, E>(
    private val totalBalanceProducer: AmountProducer<P>,
    private val feeProducer: AmountProducer<P>,
    private val extraAmountProducer: AmountProducer<P>,
    private val errorProducer: (remainingAmount: BigDecimal) -> E,
    private val existentialDeposit: AmountProducer<P>
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val existentialDeposit = existentialDeposit(value)

        val totalBalance = totalBalanceProducer(value)
        val fee = feeProducer(value)
        val extraAmount = extraAmountProducer(value)

        val remainingAmount = totalBalance - fee - extraAmount

        return validOrWarning(remainingAmount >= existentialDeposit) {
            errorProducer(remainingAmount)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.doNotCrossExistentialDeposit(
    totalBalance: AmountProducer<P>,
    fee: AmountProducer<P> = { BigDecimal.ZERO },
    extraAmount: AmountProducer<P> = { BigDecimal.ZERO },
    existentialDeposit: AmountProducer<P>,
    error: (remainingAmount: BigDecimal) -> E,
) = validate(
    ExistentialDepositValidation(
        totalBalanceProducer = totalBalance,
        feeProducer = fee,
        extraAmountProducer = extraAmount,
        errorProducer = error,
        existentialDeposit = existentialDeposit
    )
)
