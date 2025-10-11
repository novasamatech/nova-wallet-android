package io.novafoundation.nova.feature_account_impl.data.fee.types.assetHub

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.feature_account_api.data.conversion.assethub.assetConversionOrNull
import io.novafoundation.nova.feature_account_api.data.conversion.assethub.pools
import io.novafoundation.nova.feature_xcm_api.converter.asset.ChainAssetLocationConverter
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Inject
import javax.inject.Named

interface AssetHubFeePaymentAssetsFetcher {

    suspend fun fetchAvailablePaymentAssets(): Set<ChainAssetId>
}

@FeatureScope
class AssetHubFeePaymentAssetsFetcherFactory @Inject constructor(
    @Named(REMOTE_STORAGE_SOURCE) private val remoteStorageSource: StorageDataSource,
) {

    fun create(chain: Chain, multiLocationConverter: ChainAssetLocationConverter): AssetHubFeePaymentAssetsFetcher {
        return RealAssetHubFeePaymentAssetsFetcher(remoteStorageSource, multiLocationConverter, chain)
    }
}

private class RealAssetHubFeePaymentAssetsFetcher(
    private val remoteStorageSource: StorageDataSource,
    private val multiLocationConverter: ChainAssetLocationConverter,
    private val chain: Chain,
) : AssetHubFeePaymentAssetsFetcher {

    override suspend fun fetchAvailablePaymentAssets(): Set<ChainAssetId> {
        return remoteStorageSource.query(chain.id) {
            val allPools = metadata.assetConversionOrNull?.pools?.keys().orEmpty()

            constructAvailableCustomFeeAssets(allPools)
        }
    }

    private suspend fun constructAvailableCustomFeeAssets(pools: List<Pair<RelativeMultiLocation, RelativeMultiLocation>>): Set<Int> {
        return pools.mapNotNullToSet { (firstLocation, secondLocation) ->
            val firstAsset = multiLocationConverter.chainAssetFromRelativeLocation(firstLocation, pointOfView = chain) ?: return@mapNotNullToSet null
            if (!firstAsset.isUtilityAsset) return@mapNotNullToSet null

            val secondAsset = multiLocationConverter.chainAssetFromRelativeLocation(secondLocation, pointOfView = chain) ?: return@mapNotNullToSet null

            secondAsset.id
        }
    }
}
