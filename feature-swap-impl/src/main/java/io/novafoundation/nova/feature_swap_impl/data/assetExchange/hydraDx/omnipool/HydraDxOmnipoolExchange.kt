package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.filterToSet
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeFee
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeQuote
import io.novafoundation.nova.runtime.ext.decodeOrNull
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.ormlOrNull
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigInteger

private val SYSTEM_ON_CHAIN_ASSET_ID = BigInteger.ZERO

class HydraDxOmnipoolExchangeFactory(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : AssetExchange.Factory {

    override suspend fun create(chain: Chain, coroutineScope: CoroutineScope): AssetExchange {
        return HydraDxOmnipoolExchange(remoteStorageSource, chainRegistry, chain)
    }
}

private class HydraDxOmnipoolExchange(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val chain: Chain,
) : AssetExchange {

    private var pooledOnChainAssetIdsState: MutableSharedFlow<List<BigInteger>> = MutableSharedFlow()

    override suspend fun canPayFeeInNonUtilityToken(asset: Chain.Asset): Boolean {
        // TODO HydraDx supports custom token fee payment
        return false
    }

    override suspend fun availableSwapDirections(): MultiMap<FullChainAssetId, FullChainAssetId> {
        val pooledOnChainAssetIds = getPooledOnChainAssetIds()
        pooledOnChainAssetIdsState.emit(pooledOnChainAssetIds)

        val pooledChainAssetsIds = matchKnownChainAssetIds(pooledOnChainAssetIds)

        return pooledChainAssetsIds.associateWith { currentId ->
            // In OmniPool, each asset is tradable with any other except itself
            pooledChainAssetsIds.filterToSet { otherId -> currentId != otherId }
        }
    }

    override suspend fun quote(args: SwapQuoteArgs): AssetExchangeQuote {
        TODO("Not yet implemented")
    }

    override suspend fun estimateFee(args: SwapExecuteArgs): AssetExchangeFee {
        TODO("Not yet implemented")
    }

    override suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicSubmission> {
        TODO("Not yet implemented")
    }

    override suspend fun slippageConfig(): SlippageConfig {
        TODO("Not yet implemented")
    }

    private suspend fun getPooledOnChainAssetIds(): List<BigInteger> {
        return remoteStorageSource.query(chain.id) {
            runtime.metadata.omnipoolOrNull?.assets?.keys().orEmpty()
        }
    }

    private suspend fun matchKnownChainAssetIds(onChainIds: List<BigInteger>): List<FullChainAssetId> {
        val runtime = chainRegistry.getRuntime(chain.id)
        val ormlIds = chain.assets.mapNotNull {

            val onChainId = bindNumberOrNull(it.ormlOrNull()?.decodeOrNull(runtime)) ?: return@mapNotNull null

            onChainId to it
        }.toMap()

        return onChainIds.mapNotNull { onChainId ->
            if (onChainId == SYSTEM_ON_CHAIN_ASSET_ID) {
                chain.utilityAsset.fullId
            } else {
                ormlIds[onChainId]?.fullId
            }
        }
    }
}
