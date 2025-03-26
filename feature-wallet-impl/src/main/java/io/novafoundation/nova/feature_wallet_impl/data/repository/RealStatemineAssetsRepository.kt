package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.StatemineAssetDetails
import io.novafoundation.nova.feature_wallet_api.data.repository.StatemineAssetsRepository
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.api.asset
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.api.assets
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.prepareIdForEncoding
import io.novafoundation.nova.runtime.storage.cache.StorageCachingContext
import io.novafoundation.nova.runtime.storage.cache.cacheValues
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.queryNonNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject
import javax.inject.Named

@FeatureScope
class RealStatemineAssetsRepository @Inject constructor(
    @Named(LOCAL_STORAGE_SOURCE)
    private val localStorageSource: StorageDataSource,

    @Named(REMOTE_STORAGE_SOURCE)
    private val remoteStorageSource: StorageDataSource,

    override val storageCache: StorageCache,
) : StatemineAssetsRepository,
    StorageCachingContext by StorageCachingContext(storageCache) {

    override suspend fun getAssetDetails(chainId: ChainId, assetType: Chain.Asset.Type.Statemine): StatemineAssetDetails {
        return localStorageSource.query(chainId) {
            val encodableAssetId = assetType.prepareIdForEncoding(runtime)
            metadata.assets(assetType.palletNameOrDefault()).asset.queryNonNull(encodableAssetId)
        }
    }

    override suspend fun subscribeAndSyncAssetDetails(
        chainId: ChainId,
        assetType: Chain.Asset.Type.Statemine,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<StatemineAssetDetails> {
        return remoteStorageSource.subscribe(chainId, subscriptionBuilder) {
            val encodableAssetId = assetType.prepareIdForEncoding(runtime)

            metadata.assets(assetType.palletNameOrDefault()).asset.observeWithRaw(encodableAssetId)
                .cacheValues()
                .filterNotNull()
        }
    }
}
