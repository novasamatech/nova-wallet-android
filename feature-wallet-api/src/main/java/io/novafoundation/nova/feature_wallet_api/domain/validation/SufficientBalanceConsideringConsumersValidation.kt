package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.totalCanBeDroppedBelowMinimumBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SufficientBalanceConsideringConsumersValidation<P, E>(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainExtractor: (P) -> Chain,
    private val assetExtractor: (P) -> Chain.Asset,
    private val balanceExtractor: (P) -> Balance,
    private val feeExtractor: (P) -> Balance,
    private val amountExtractor: (P) -> Balance,
    private val error: (P, existentialDeposit: Balance) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val chain = chainExtractor(value)
        val asset = assetExtractor(value)

        val totalCanDropBelowMinimumBalance = assetSourceRegistry.totalCanBeDroppedBelowMinimumBalance(asset)

        return if (totalCanDropBelowMinimumBalance) {
            valid()
        } else {
            val balance = balanceExtractor(value)
            val amount = amountExtractor(value)
            val fee = feeExtractor(value)

            val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(chain, asset)
            validOrError(balance - existentialDeposit >= amount + fee) { error(value, existentialDeposit) }
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.sufficientBalanceConsideringConsumersValidation(
    assetSourceRegistry: AssetSourceRegistry,
    chainExtractor: (P) -> Chain,
    assetExtractor: (P) -> Chain.Asset,
    balanceExtractor: (P) -> Balance,
    feeExtractor: (P) -> Balance,
    amountExtractor: (P) -> Balance,
    error: (P, existentialDeposit: Balance) -> E
) = validate(
    SufficientBalanceConsideringConsumersValidation(
        assetSourceRegistry,
        chainExtractor,
        assetExtractor,
        balanceExtractor,
        feeExtractor,
        amountExtractor,
        error,
    )
)
