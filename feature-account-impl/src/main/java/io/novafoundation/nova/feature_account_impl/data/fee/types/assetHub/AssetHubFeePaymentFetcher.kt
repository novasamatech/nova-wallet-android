package io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub

import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.feature_account_api.data.conversion.assethub.assetConversionOrNull
import io.novafoundation.nova.feature_account_api.data.conversion.assethub.pools
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverter
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.common.utils.metadata

interface AssetHubFeePaymentAssetsFetcher {

    suspend fun fetchAvailablePaymentAssets(): Set<ChainAssetId>
}

class AssetHubFeePaymentAssetsFetcherFactory(
    private val remoteStorageSource: StorageDataSource,
    private val multiLocationConverterFactory: MultiLocationConverterFactory
) {

    suspend fun create(chain: Chain): AssetHubFeePaymentAssetsFetcher {
        val multiLocationConverter = multiLocationConverterFactory.defaultSync(chain)

        return RealAssetHubFeePaymentAssetsFetcher(remoteStorageSource, multiLocationConverter, chain)
    }

    fun create(chain: Chain, multiLocationConverter: MultiLocationConverter): AssetHubFeePaymentAssetsFetcher {
        return RealAssetHubFeePaymentAssetsFetcher(remoteStorageSource, multiLocationConverter, chain)
    }
}

private class RealAssetHubFeePaymentAssetsFetcher(
    private val remoteStorageSource: StorageDataSource,
    private val multiLocationConverter: MultiLocationConverter,
    private val chain: Chain,
) : AssetHubFeePaymentAssetsFetcher {

    override suspend fun fetchAvailablePaymentAssets(): Set<ChainAssetId> {
        return remoteStorageSource.query(chain.id) {
            val allPools = metadata.assetConversionOrNull?.pools?.keys().orEmpty()

            constructAvailableCustomFeeAssets(allPools)
        }
    }

    private suspend fun constructAvailableCustomFeeAssets(pools: List<Pair<MultiLocation, MultiLocation>>): Set<Int> {
        return pools.mapNotNullToSet { (firstLocation, secondLocation) ->
            val firstAsset = multiLocationConverter.toChainAsset(firstLocation) ?: return@mapNotNullToSet null
            if (!firstAsset.isUtilityAsset) return@mapNotNullToSet null

            val secondAsset = multiLocationConverter.toChainAsset(secondLocation) ?: return@mapNotNullToSet null

            secondAsset.id
        }
    }
}
