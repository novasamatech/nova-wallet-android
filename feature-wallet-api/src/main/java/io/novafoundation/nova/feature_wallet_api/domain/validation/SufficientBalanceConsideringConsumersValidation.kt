package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.getExistentialDeposit
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SufficientBalanceConsideringConsumersValidation<P, E>(
    private val assetsValidationContext: AssetsValidationContext,
    private val assetExtractor: (P) -> Chain.Asset,
    private val canDropBelowEd: suspend (P, Chain.Asset) -> Boolean,
    private val feeExtractor: (P) -> Balance,
    private val amountExtractor: (P) -> Balance,
    private val error: (P, existentialDeposit: Balance, asset: Asset) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val chainAsset = assetExtractor(value)

        val totalCanDropBelowMinimumBalance = canDropBelowEd(value, chainAsset)
        if (totalCanDropBelowMinimumBalance) return valid()

        val asset = assetsValidationContext.getAsset(chainAsset)
        val amount = amountExtractor(value)
        val fee = feeExtractor(value)

        val existentialDeposit = assetsValidationContext.getExistentialDeposit(chainAsset)
        return validOrError(asset.balanceCountedTowardsEDInPlanks - existentialDeposit >= amount + fee) {
            error(value, existentialDeposit, asset)
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.sufficientBalanceConsideringConsumersValidation(
    assetsValidationContext: AssetsValidationContext,
    assetExtractor: (P) -> Chain.Asset,
    canDropBelowEd: suspend (P, Chain.Asset) -> Boolean,
    feeExtractor: (P) -> Balance,
    amountExtractor: (P) -> Balance,
    error: (P, existentialDeposit: Balance, asset: Asset) -> E
) = validate(
    SufficientBalanceConsideringConsumersValidation(
        assetsValidationContext = assetsValidationContext,
        assetExtractor = assetExtractor,
        canDropBelowEd = canDropBelowEd,
        feeExtractor = feeExtractor,
        amountExtractor = amountExtractor,
        error = error
    )
)
