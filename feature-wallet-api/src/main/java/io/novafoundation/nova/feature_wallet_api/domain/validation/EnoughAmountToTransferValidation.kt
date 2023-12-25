package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.networkFeeByRequestedAccountOrZero
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

interface NotEnoughToPayFeesError {
    val chainAsset: Chain.Asset
    val maxUsable: BigDecimal
    val fee: BigDecimal
}

typealias EnoughAmountToTransferValidation<P, E> = EnoughAmountToTransferValidationGeneric<P, E, SimpleFee>

class EnoughAmountToTransferValidationGeneric<P, E, F : GenericFee>(
    private val feeExtractor: GenericFeeProducer<F, P>,
    private val availableBalanceProducer: AmountProducer<P>,
    private val errorProducer: (ErrorContext<P>) -> E,
    private val skippable: Boolean = false,
    private val extraAmountExtractor: AmountProducer<P> = { BigDecimal.ZERO },
) : Validation<P, E> {

    class ErrorContext<P>(

        val payload: P,

        val availableToPayFees: BigDecimal,

        val fee: BigDecimal,
    )

    companion object;

    override suspend fun validate(value: P): ValidationStatus<E> {
        val fee = feeExtractor(value).networkFeeByRequestedAccountOrZero
        val available = availableBalanceProducer(value)
        val amount = extraAmountExtractor(value)

        return if (fee + amount <= available) {
            ValidationStatus.Valid()
        } else {
            val maxUsable = (available - fee).coerceAtLeast(BigDecimal.ZERO)

            val failureLevel = if (skippable) DefaultFailureLevel.WARNING else DefaultFailureLevel.ERROR

            ValidationStatus.NotValid(failureLevel, errorProducer(ErrorContext(value, maxUsable, fee)))
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.sufficientBalance(
    fee: FeeProducer<P> = { null },
    amount: AmountProducer<P> = { BigDecimal.ZERO },
    available: AmountProducer<P>,
    error: (EnoughAmountToTransferValidationGeneric.ErrorContext<P>) -> E,
    skippable: Boolean = false
) = validate(
    EnoughAmountToTransferValidation(
        feeExtractor = fee,
        extraAmountExtractor = amount,
        errorProducer = error,
        skippable = skippable,
        availableBalanceProducer = available
    )
)

fun <P, E, F : GenericFee> ValidationSystemBuilder<P, E>.sufficientBalanceGeneric(
    fee: GenericFeeProducer<F, P> = { null },
    amount: AmountProducer<P> = { BigDecimal.ZERO },
    available: AmountProducer<P>,
    error: (EnoughAmountToTransferValidationGeneric.ErrorContext<P>) -> E,
    skippable: Boolean = false
) = validate(
    EnoughAmountToTransferValidationGeneric(
        feeExtractor = fee,
        extraAmountExtractor = amount,
        errorProducer = error,
        skippable = skippable,
        availableBalanceProducer = available
    )
)

fun ResourceManager.notSufficientBalanceToPayFeeErrorMessage() = getString(R.string.common_not_enough_funds_title) to
    getString(R.string.common_not_enough_funds_message)

fun ResourceManager.amountIsTooBig() = getString(R.string.common_not_enough_funds_title) to
    getString(R.string.choose_amount_error_too_big)

fun ResourceManager.zeroAmount() = getString(R.string.common_amount_low) to
    getString(R.string.common_zero_amount_error)

fun handleNotEnoughFeeError(error: NotEnoughToPayFeesError, resourceManager: ResourceManager): TitleAndMessage {
    val title = resourceManager.getString(R.string.common_not_enough_funds_title)

    val maxUsable = error.maxUsable.formatTokenAmount(error.chainAsset)
    val fee = error.fee.formatTokenAmount(error.chainAsset)
    val message = resourceManager.getString(R.string.common_cannot_pay_network_fee_message, maxUsable, fee)

    return title to message
}
