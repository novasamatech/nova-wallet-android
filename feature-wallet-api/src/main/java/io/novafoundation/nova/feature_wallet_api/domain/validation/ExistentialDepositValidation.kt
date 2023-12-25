package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_wallet_api.presentation.model.networkFeeByRequestedAccountOrZero
import java.math.BigDecimal

typealias ExistentialDepositError<E, P> = (remainingAmount: BigDecimal, payload: P) -> E

class ExistentialDepositValidation<P, E>(
    private val countableTowardsEdBalance: AmountProducer<P>,
    private val feeProducer: FeeProducer<P>,
    private val extraAmountProducer: AmountProducer<P>,
    private val errorProducer: ExistentialDepositError<E, P>,
    private val existentialDeposit: AmountProducer<P>
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val existentialDeposit = existentialDeposit(value)

        val countableTowardsEd = countableTowardsEdBalance(value)
        val fee = feeProducer(value)
        val extraAmount = extraAmountProducer(value)

        val remainingAmount = countableTowardsEd - fee.networkFeeByRequestedAccountOrZero - extraAmount

        return validOrWarning(remainingAmount >= existentialDeposit) {
            errorProducer(remainingAmount, value)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.doNotCrossExistentialDeposit(
    countableTowardsEdBalance: AmountProducer<P>,
    fee: FeeProducer<P> = { null },
    extraAmount: AmountProducer<P> = { BigDecimal.ZERO },
    existentialDeposit: AmountProducer<P>,
    error: ExistentialDepositError<E, P>,
) = validate(
    ExistentialDepositValidation(
        countableTowardsEdBalance = countableTowardsEdBalance,
        feeProducer = fee,
        extraAmountProducer = extraAmount,
        errorProducer = error,
        existentialDeposit = existentialDeposit
    )
)
