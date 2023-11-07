package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.totalCanBeDroppedBelowMinimumBalance
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.hash.isPositive

class SufficientBalanceConsideringConsumersValidation<P, E>(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainExtractor: (P) -> Chain,
    private val assetExtractor: (P) -> Chain.Asset,
    private val totalBalanceExtractor: (P) -> BigInteger,
    private val feeExtractor: (P) -> BigInteger,
    private val amountExtractor: (P) -> BigInteger,
    private val error: (P, existentialDeposit: BigInteger) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val chain = chainExtractor(value)
        val asset = assetExtractor(value)

        val totalCanDropBelowMinimumBalance = assetSourceRegistry.totalCanBeDroppedBelowMinimumBalance(asset)

        if (asset.isCommissionAsset && totalCanDropBelowMinimumBalance) {
            val totalBalance = totalBalanceExtractor(value)
            val amount = amountExtractor(value)
            val fee = feeExtractor(value)

            val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(chain, asset)
            return validOrError(totalBalance - existentialDeposit >= amount + fee) { error(value, existentialDeposit) }
        }

        return valid()
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.sufficientBalanceConsideringConsumersValidation(
    assetSourceRegistry: AssetSourceRegistry,
    chainExtractor: (P) -> Chain,
    assetExtractor: (P) -> Chain.Asset,
    totalBalanceExtractor: (P) -> BigInteger,
    feeExtractor: (P) -> BigInteger,
    amountExtractor: (P) -> BigInteger,
    error: (P, existentialDeposit: BigInteger) -> E
) = validate(
    SufficientBalanceConsideringConsumersValidation(
        assetSourceRegistry,
        chainExtractor,
        assetExtractor,
        totalBalanceExtractor,
        feeExtractor,
        amountExtractor,
        error,
    )
)
