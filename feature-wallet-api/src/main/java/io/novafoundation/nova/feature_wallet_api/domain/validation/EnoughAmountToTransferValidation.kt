package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

interface NotEnoughToPayFeesError {
    val chainAsset: Chain.Asset
    val availableToPayFees: BigDecimal
    val fee: BigDecimal
}

class EnoughAmountToTransferValidation<P, E>(
    private val feeExtractor: AmountProducer<P>,
    private val availableBalanceProducer: AmountProducer<P>,
    private val errorProducer: (P, availableToPayFees: BigDecimal) -> E,
    private val skippable: Boolean = false,
    private val extraAmountExtractor: AmountProducer<P> = { BigDecimal.ZERO },
) : Validation<P, E> {

    companion object;

    override suspend fun validate(value: P): ValidationStatus<E> {
        val fee = feeExtractor(value)
        val available = availableBalanceProducer(value)
        val amount = extraAmountExtractor(value)

        return if (fee + amount <= available) {
            ValidationStatus.Valid()
        } else {
            val availableToPayFees = (available - amount).coerceAtLeast(BigDecimal.ZERO)

            val failureLevel = if (skippable) DefaultFailureLevel.WARNING else DefaultFailureLevel.ERROR

            ValidationStatus.NotValid(failureLevel, errorProducer(value, availableToPayFees))
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.sufficientBalance(
    fee: AmountProducer<P> = { BigDecimal.ZERO },
    amount: AmountProducer<P> = { BigDecimal.ZERO },
    available: AmountProducer<P>,
    error: (P, availableToPayFees: BigDecimal) -> E,
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

fun ResourceManager.notSufficientBalanceToPayFeeErrorMessage() = getString(R.string.common_not_enough_funds_title) to
    getString(R.string.common_not_enough_funds_message)

fun ResourceManager.amountIsTooBig() = getString(R.string.common_not_enough_funds_title) to
    getString(R.string.choose_amount_error_too_big)

fun ResourceManager.zeroAmount() = getString(R.string.common_amount_low) to
    getString(R.string.common_zero_amount_error)

fun handleNotEnoughFeeError(error: NotEnoughToPayFeesError, resourceManager: ResourceManager): TitleAndMessage {
    val title = resourceManager.getString(R.string.common_cannot_pay_network_fee_title)

    val availableToPayFees = error.availableToPayFees.formatTokenAmount(error.chainAsset)
    val fee = error.fee.formatTokenAmount(error.chainAsset)
    val message = resourceManager.getString(R.string.common_cannot_pay_network_fee_message, fee, availableToPayFees)

    return title to message
}
