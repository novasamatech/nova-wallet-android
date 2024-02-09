package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validOrWarning
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.networkFeeByRequestedAccountOrZero
import java.math.BigDecimal

typealias ExistentialDepositError<E, P> = (remainingAmount: BigDecimal, payload: P) -> E

class ExistentialDepositValidation<P, E, F : GenericFee>(
    private val countableTowardsEdBalance: AmountProducer<P>,
    private val feeProducer: GenericFeeListProducer<F, P>,
    private val extraAmountProducer: AmountProducer<P>,
    private val errorProducer: ExistentialDepositError<E, P>,
    private val existentialDeposit: AmountProducer<P>
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val existentialDeposit = existentialDeposit(value)

        val countableTowardsEd = countableTowardsEdBalance(value)
        val fee = feeProducer(value).sumOf { it.networkFeeByRequestedAccountOrZero }
        val extraAmount = extraAmountProducer(value)

        val remainingAmount = countableTowardsEd - fee - extraAmount

        return validOrWarning(remainingAmount >= existentialDeposit) {
            errorProducer(remainingAmount, value)
        }
    }
}

fun <P, E, F : GenericFee> ValidationSystemBuilder<P, E>.doNotCrossExistentialDepositMultyFee(
    countableTowardsEdBalance: AmountProducer<P>,
    fee: GenericFeeListProducer<F, P> = { emptyList() },
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

fun <P, E> ValidationSystemBuilder<P, E>.doNotCrossExistentialDepositInUsedAsset(
    countableTowardsEdBalance: AmountProducer<P>,
    fee: FeeProducer<P> = { null },
    extraAmount: AmountProducer<P> = { BigDecimal.ZERO },
    existentialDeposit: AmountProducer<P>,
    error: ExistentialDepositError<E, P>,
) = validate(
    ExistentialDepositValidation(
        countableTowardsEdBalance = countableTowardsEdBalance,
        feeProducer = { listOfNotNull(fee(it)) },
        extraAmountProducer = extraAmount,
        errorProducer = error,
        existentialDeposit = existentialDeposit
    )
)
