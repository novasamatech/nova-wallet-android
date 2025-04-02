package io.novafoundation.nova.feature_wallet_api.domain.validation

import androidx.annotation.StringRes
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_account_api.data.model.decimalAmountByExecutingAccount
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

typealias HasEnoughFreeBalanceErrorProducer<E> = (chainAsset: Chain.Asset, freeBalanceAfterFees: BigDecimal) -> E

interface NotEnoughFreeBalanceError {
    val chainAsset: Chain.Asset
    val freeAfterFees: BigDecimal
}

class HasEnoughBalanceValidation<P, E>(
    private val chainAsset: (P) -> Chain.Asset,
    private val availableBalance: AmountProducer<P>,
    private val requestedAmount: AmountProducer<P>,
    private val error: HasEnoughFreeBalanceErrorProducer<E>
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val balanceAfterFees = availableBalance(value)

        return (balanceAfterFees >= requestedAmount(value)) isTrueOrError {
            error(chainAsset(value), balanceAfterFees.atLeastZero())
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.hasEnoughFreeBalance(
    asset: (P) -> Asset,
    fee: SimpleFeeProducer<P>,
    requestedAmount: AmountProducer<P>,
    error: HasEnoughFreeBalanceErrorProducer<E>
) {
    hasEnoughBalance(
        chainAsset = { asset(it).token.configuration },
        requestedAmount = requestedAmount,
        error = error,
        availableBalance = { asset(it).free - fee(it)?.decimalAmountByExecutingAccount.orZero() }
    )
}

fun <P, E> ValidationSystemBuilder<P, E>.hasEnoughBalance(
    chainAsset: (P) -> Chain.Asset,
    availableBalance: AmountProducer<P>,
    requestedAmount: AmountProducer<P>,
    error: HasEnoughFreeBalanceErrorProducer<E>
) {
    validate(
        HasEnoughBalanceValidation(
            chainAsset = chainAsset,
            availableBalance = availableBalance,
            requestedAmount = requestedAmount,
            error = error
        )
    )
}

fun handleNotEnoughFreeBalanceError(
    error: NotEnoughFreeBalanceError,
    resourceManager: ResourceManager,
    @StringRes descriptionFormat: Int
): TitleAndMessage {
    val feeAfterFees = error.freeAfterFees.formatTokenAmount(error.chainAsset)

    return resourceManager.getString(R.string.common_amount_too_big) to
        resourceManager.getString(descriptionFormat, feeAfterFees)
}
