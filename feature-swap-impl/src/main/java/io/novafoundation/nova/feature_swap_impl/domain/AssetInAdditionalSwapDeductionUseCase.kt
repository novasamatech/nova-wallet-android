package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.isSelfSufficientAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface AssetInAdditionalSwapDeductionUseCase {

    suspend fun invoke(assetIn: Chain.Asset, assetOut: Chain.Asset, keepAlive: Boolean = false): Balance
}

class RealAssetInAdditionalSwapDeductionUseCase(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry
) : AssetInAdditionalSwapDeductionUseCase {

    override suspend fun invoke(assetIn: Chain.Asset, assetOut: Chain.Asset, keepAlive: Boolean): Balance {
        // A following call in the same atomic swap batch can still need the source account.
        // Current consumer counts or a sufficient output asset do not make reaping safe for that batch.
        if (keepAlive) return assetSourceRegistry.existentialDepositInPlanks(assetIn)

        val assetInBalanceCanDropBelowEd = assetSourceRegistry.sourceFor(assetIn)
            .transfers
            .totalCanDropBelowMinimumBalance(assetIn)

        val sameChain = assetIn.chainId == assetOut.chainId

        val assetOutCanProvideSufficiency = sameChain && assetSourceRegistry.isSelfSufficientAsset(assetOut)

        val canDustAssetIn = assetInBalanceCanDropBelowEd || assetOutCanProvideSufficiency
        val shouldKeepEdForAssetIn = !canDustAssetIn

        return if (shouldKeepEdForAssetIn) {
            assetSourceRegistry.existentialDepositInPlanks(assetIn)
        } else {
            Balance.ZERO
        }
    }
}
