package io.novafoundation.nova.feature_wallet_impl.domain.validaiton.context

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.isSelfSufficientAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.totalCanBeDroppedBelowMinimumBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AssetValidationContextFactory(
    private val arbitraryAssetUseCase: ArbitraryAssetUseCase,
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
) : AssetsValidationContext.Factory {

    override fun create(): AssetsValidationContext {
        return RealAssetValidationContext(arbitraryAssetUseCase, chainRegistry, assetSourceRegistry)
    }
}

private class RealAssetValidationContext(
    private val arbitraryAssetUseCase: ArbitraryAssetUseCase,
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry,
) : AssetsValidationContext {

    private val balanceCache = mutableMapOf<FullChainAssetId, Asset>()
    private val balanceMutex = Mutex()

    private val edCache = mutableMapOf<FullChainAssetId, Balance>()
    private val edMutex = Mutex()

    override suspend fun getAsset(chainAsset: Chain.Asset): Asset {
        return balanceMutex.withLock {
            balanceCache.getOrPut(chainAsset.fullId) {
                arbitraryAssetUseCase.getAsset(chainAsset)!!
            }
        }
    }

    override suspend fun getAsset(chainAssetId: FullChainAssetId): Asset {
        val chainAsset = chainRegistry.asset(chainAssetId)
        return getAsset(chainAsset)
    }

    override suspend fun getExistentialDeposit(chainAssetId: FullChainAssetId): Balance {
        return edMutex.withLock {
            edCache.getOrPut(chainAssetId) {
                val (chain, asset) = chainRegistry.chainWithAsset(chainAssetId)
                assetSourceRegistry.existentialDepositInPlanks(chain, asset)
            }
        }
    }

    override suspend fun isAssetSufficient(chainAsset: Chain.Asset): Boolean {
        return assetSourceRegistry.isSelfSufficientAsset(chainAsset)
    }

    override suspend fun canTotalDropBelowEd(chainAsset: Chain.Asset): Boolean {
        return assetSourceRegistry.totalCanBeDroppedBelowMinimumBalance(chainAsset)
    }
}
