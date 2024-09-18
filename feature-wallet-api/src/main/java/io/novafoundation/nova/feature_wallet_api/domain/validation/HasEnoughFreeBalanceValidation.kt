package io.novafoundation.nova.feature_wallet_api.domain.validation

import androidx.annotation.StringRes
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.networkFeeByRequestedAccountOrZero
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

typealias HasEnoughFreeBalanceErrorProducer<E> = (chainAsset: Chain.Asset, freeBalanceAfterFees: BigDecimal) -> E

interface NotEnoughFreeBalanceError {
    val chainAsset: Chain.Asset
    val freeAfterFees: BigDecimal
}

class HasEnoughFreeBalanceValidation<P, E>(
    private val asset: (P) -> Asset,
    private val fee: FeeProducer<P>,
    private val requestedAmount: AmountProducer<P>,
    private val error: HasEnoughFreeBalanceErrorProducer<E>
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val freeBalanceAfterFees = asset(value).free - fee(value).networkFeeByRequestedAccountOrZero

        return (freeBalanceAfterFees >= requestedAmount(value)) isTrueOrError {
            error(asset(value).token.configuration, freeBalanceAfterFees.atLeastZero())
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.hasEnoughFreeBalance(
    asset: (P) -> Asset,
    fee: FeeProducer<P>,
    requestedAmount: AmountProducer<P>,
    error: HasEnoughFreeBalanceErrorProducer<E>
) {
    validate(HasEnoughFreeBalanceValidation(asset, fee, requestedAmount, error))
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
