package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.decimalAmountByExecutingAccount
import java.math.BigDecimal

typealias ExistentialDepositError<E, P> = (remainingAmount: BigDecimal, payload: P) -> E

class ExistentialDepositValidation<P, E, F : Fee>(
    private val countableTowardsEdBalance: AmountProducer<P>,
    private val feeProducer: FeeListProducer<F, P>,
    private val extraAmountProducer: AmountProducer<P>,
    private val errorProducer: ExistentialDepositError<E, P>,
    private val existentialDeposit: AmountProducer<P>
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val existentialDeposit = existentialDeposit(value)

        val countableTowardsEd = countableTowardsEdBalance(value)
        val fee = feeProducer(value).sumOf { it.decimalAmountByExecutingAccount }
        val extraAmount = extraAmountProducer(value)

        val remainingAmount = countableTowardsEd - fee - extraAmount

        return validOrWarning(remainingAmount >= existentialDeposit) {
            errorProducer(remainingAmount, value)
        }
    }
}

fun <P, E, F : Fee> ValidationSystemBuilder<P, E>.doNotCrossExistentialDepositMultiFee(
    countableTowardsEdBalance: AmountProducer<P>,
    fee: FeeListProducer<F, P> = { emptyList() },
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
