package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion

import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.mutableMultiMapOf
import io.novafoundation.nova.common.utils.put
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicHash
import io.novafoundation.nova.feature_swap_api.domain.model.SwapArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter.CompoundMultiLocationConverter
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter.LocalAssetsLocationConverter
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter.MultiLocationConverter
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter.NativeAssetLocationConverter
import io.novafoundation.nova.feature_swap_impl.data.network.blockhain.api.assetConversionOrNull
import io.novafoundation.nova.feature_swap_impl.data.network.blockhain.api.pools
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata

class AssetConversionExchangeFactory(
    private val chainRegistry: ChainRegistry,
    private val remoteStorageSource: StorageDataSource,
) : AssetExchange.Factory {

    override suspend fun create(chainId: ChainId): AssetExchange? {
        val chain = chainRegistry.getChainOrNull(chainId) ?: return null
        val runtime = chainRegistry.getRuntime(chainId)

        val converter = CompoundMultiLocationConverter(
            NativeAssetLocationConverter(chain),
            LocalAssetsLocationConverter(chain, runtime)
        )

        return AssetConversionExchange(chain, converter, remoteStorageSource)
    }
}

private class AssetConversionExchange(
    private val chain: Chain,
    private val multiLocationConverter: MultiLocationConverter,
    private val remoteStorageSource: StorageDataSource,
) : AssetExchange {

    override suspend fun availableSwapDirections(): MultiMap<FullChainAssetId, FullChainAssetId> {
        return remoteStorageSource.query(chain.id) {
            val allPools = metadata.assetConversionOrNull?.pools?.keys().orEmpty()

            constructAllAvailableDirections(allPools)
        }
    }

    override suspend fun quote(args: SwapArgs): Result<SwapQuote> {
        TODO("Not yet implemented")
    }

    override suspend fun swap(args: SwapArgs): Result<ExtrinsicHash> {
        TODO("Not yet implemented")
    }

    private suspend fun constructAllAvailableDirections(pools: List<Pair<MultiLocation, MultiLocation>>): MultiMap<FullChainAssetId, FullChainAssetId> {
        val multiMap = mutableMultiMapOf<FullChainAssetId, FullChainAssetId>()

        pools.forEach { (firstLocation, secondLocation) ->
            val firstAsset = multiLocationConverter.toChainAsset(firstLocation) ?: return@forEach
            val secondAsset = multiLocationConverter.toChainAsset(secondLocation) ?: return@forEach

            val firstAssetId = firstAsset.fullId
            val secondAssetId = secondAsset.fullId

            multiMap.put(firstAssetId, secondAssetId)
            multiMap.put(secondAssetId, firstAssetId)
        }

        return multiMap
    }
}
