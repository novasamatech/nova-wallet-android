package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.isSelfSufficientAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface AssetInAdditionalSwapDeductionUseCase {

    suspend fun invoke(assetIn: Chain.Asset, assetOut: Chain.Asset): Balance
}

class RealAssetInAdditionalSwapDeductionUseCase(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry
) : AssetInAdditionalSwapDeductionUseCase {

    override suspend fun invoke(assetIn: Chain.Asset, assetOut: Chain.Asset): Balance {
        val assetInBalanceCanDropBelowEd = assetSourceRegistry.sourceFor(assetIn)
            .transfers
            .totalCanDropBelowMinimumBalance(assetIn)

        val sameChain = assetIn.chainId == assetOut.chainId

        val assetOutCanProvideSufficiency = sameChain && assetSourceRegistry.isSelfSufficientAsset(assetOut)

        val canDustAssetIn = assetInBalanceCanDropBelowEd || assetOutCanProvideSufficiency
        val shouldKeepEdForAssetIn = !canDustAssetIn

        return if (shouldKeepEdForAssetIn) {
            val chainIn = chainRegistry.getChain(assetIn.chainId)
            assetSourceRegistry.existentialDepositInPlanks(chainIn, assetIn)
        } else {
            Balance.ZERO
        }
    }
}
